package com.vigilante.shiftsalaryplanner.payroll

import com.vigilante.shiftsalaryplanner.ResolvedAdditionalPaymentBreakdown
import java.time.YearMonth

object PayrollSheetDraftFactory {

    fun build(
        month: YearMonth,
        summary: PayrollResult,
        housingPaymentLabel: String = "Выплата на квартиру",
        resolvedAdditionalPaymentBreakdown: List<ResolvedAdditionalPaymentBreakdown> = emptyList()
    ): PayrollDetailedResult {
        val headerItems = buildHeader(
            month = month,
            summary = summary,
            housingPaymentLabel = housingPaymentLabel,
            resolvedAdditionalPaymentBreakdown = resolvedAdditionalPaymentBreakdown
        )

        val accrualItems = buildAccruals(
            summary = summary,
            housingPaymentLabel = housingPaymentLabel,
            resolvedAdditionalPaymentBreakdown = resolvedAdditionalPaymentBreakdown
        )

        val deductionItems = buildDeductions(summary)
        val priorPaymentItems = buildPriorPayments(summary)
        val payoutItems = buildPayouts(summary)
        val referenceItems = buildReference(summary)

        return PayrollSheetBuilder.build(
            month = month,
            summary = summary,
            headerItems = headerItems,
            accrualItems = accrualItems,
            deductionItems = deductionItems,
            priorPaymentItems = priorPaymentItems,
            payoutItems = payoutItems,
            referenceItems = referenceItems
        )
    }

    private fun resolvedPaymentKind(item: ResolvedAdditionalPaymentBreakdown): PayrollLineKind {
        val name = item.payment.displayName.lowercase()

        return when {
            "интерстим" in name -> PayrollLineKind.INTERSTIM_ALLOWANCE
            "индекс" in name -> PayrollLineKind.INDEXATION_PAYMENT
            "арен" in name || "жиль" in name || "квартир" in name -> PayrollLineKind.HOUSING_COMPENSATION
            "прем" in name -> PayrollLineKind.OPERATION_PREMIUM
            else -> PayrollLineKind.OTHER
        }
    }

    private fun resolvedPaymentSortOrder(item: ResolvedAdditionalPaymentBreakdown): Int {
        return when (resolvedPaymentKind(item)) {
            PayrollLineKind.INTERSTIM_ALLOWANCE -> 65
            PayrollLineKind.INDEXATION_PAYMENT -> 66
            PayrollLineKind.HOUSING_COMPENSATION -> 67
            PayrollLineKind.OPERATION_PREMIUM -> 68
            PayrollLineKind.OTHER -> 69
            else -> 69
        }
    }

    private fun buildHeader(
        month: YearMonth,
        summary: PayrollResult,
        housingPaymentLabel: String,
        resolvedAdditionalPaymentBreakdown: List<ResolvedAdditionalPaymentBreakdown>
    ): List<PayrollLineItem> {
        return buildList {
            if (summary.basePay > 0.0 && summary.workedHours > 0.0) {
                val estimatedMonthlyBase = if (summary.hourlyRate > 0.0) {
                    summary.hourlyRate * summary.workedHours
                } else {
                    summary.basePay
                }

                add(
                    PayrollSheetBuilder.headerLine(
                        title = "Оклад / тарифная ставка",
                        amount = estimatedMonthlyBase,
                        sortOrder = 10,
                        note = month.toString()
                    )
                )
            }

            resolvedAdditionalPaymentBreakdown
                .sortedBy { resolvedPaymentSortOrder(it) }
                .forEach { item ->
                    add(
                        PayrollSheetBuilder.headerLine(
                            title = item.payment.displayName,
                            amount = item.grossAmount,
                            sortOrder = resolvedPaymentSortOrder(item),
                            note = if (item.payment.withAdvance) "В аванс" else null
                        )
                    )
                }

            if (summary.housingPayment > 0.0) {
                add(
                    PayrollSheetBuilder.headerLine(
                        title = housingPaymentLabel,
                        amount = summary.housingPayment,
                        sortOrder = 80
                    )
                )
            }

            if (summary.workedHours > 0.0) {
                add(
                    PayrollSheetBuilder.referenceLine(
                        title = "Отработано часов",
                        amount = summary.workedHours,
                        sortOrder = 90,
                        unit = PayrollQuantityUnit.HOURS
                    )
                )
            }

            if (summary.nightHours > 0.0) {
                add(
                    PayrollSheetBuilder.referenceLine(
                        title = "Ночных часов",
                        amount = summary.nightHours,
                        sortOrder = 100,
                        unit = PayrollQuantityUnit.HOURS
                    )
                )
            }
        }
    }

    private fun buildAccruals(
        summary: PayrollResult,
        housingPaymentLabel: String,
        resolvedAdditionalPaymentBreakdown: List<ResolvedAdditionalPaymentBreakdown>
    ): List<PayrollLineItem> {
        return buildList {
            if (summary.basePay != 0.0) {
                add(
                    PayrollSheetBuilder.accrualLine(
                        kind = PayrollLineKind.BASE_SALARY,
                        title = "Оплата по окладу",
                        code = "0010",
                        quantity = summary.workedHours,
                        unit = PayrollQuantityUnit.HOURS,
                        amount = summary.basePay,
                        sortOrder = 10
                    )
                )
            }

            if (summary.nightExtra != 0.0) {
                add(
                    PayrollSheetBuilder.accrualLine(
                        kind = PayrollLineKind.NIGHT_EXTRA,
                        title = "Доплата за работу ночью",
                        code = "0212",
                        quantity = summary.nightHours,
                        unit = PayrollQuantityUnit.HOURS,
                        amount = summary.nightExtra,
                        sortOrder = 20
                    )
                )
            }

            if (summary.holidayExtra != 0.0) {
                add(
                    PayrollSheetBuilder.accrualLine(
                        kind = PayrollLineKind.SPECIAL_DAY_EXTRA,
                        title = "Доплата за выходные / праздники",
                        quantity = summary.holidayHours,
                        unit = PayrollQuantityUnit.HOURS,
                        amount = summary.holidayExtra,
                        sortOrder = 30
                    )
                )
            }

            if (summary.vacationPay != 0.0) {
                add(
                    PayrollSheetBuilder.accrualLine(
                        kind = PayrollLineKind.VACATION_PAY,
                        title = "Отпуск",
                        code = "3000",
                        quantity = summary.vacationDays.toDouble(),
                        unit = PayrollQuantityUnit.DAYS,
                        amount = summary.vacationPay,
                        sortOrder = 40
                    )
                )
            }

            if (summary.sickPay != 0.0) {
                add(
                    PayrollSheetBuilder.accrualLine(
                        kind = PayrollLineKind.SICK_PAY_EMPLOYER,
                        title = "Больничный",
                        code = "5000",
                        quantity = summary.sickDays.toDouble(),
                        unit = PayrollQuantityUnit.DAYS,
                        amount = summary.sickPay,
                        sortOrder = 50
                    )
                )
            }

            if (summary.housingPayment != 0.0) {
                add(
                    PayrollSheetBuilder.accrualLine(
                        kind = PayrollLineKind.HOUSING_COMPENSATION,
                        title = housingPaymentLabel,
                        code = "4433",
                        quantity = 1.0,
                        unit = PayrollQuantityUnit.MONTHS,
                        amount = summary.housingPayment,
                        sortOrder = 60
                    )
                )
            }

            resolvedAdditionalPaymentBreakdown
                .sortedBy { resolvedPaymentSortOrder(it) }
                .forEach { item ->
                    add(
                        PayrollSheetBuilder.accrualLine(
                            kind = resolvedPaymentKind(item),
                            title = item.payment.displayName,
                            amount = item.grossAmount,
                            sortOrder = resolvedPaymentSortOrder(item),
                            note = if (item.payment.withAdvance) "В аванс" else null
                        )
                    )
                }
        }
    }

    private fun buildDeductions(summary: PayrollResult): List<PayrollLineItem> {
        return buildList {
            if (summary.ndfl != 0.0) {
                add(
                    PayrollSheetBuilder.deductionLine(
                        kind = PayrollLineKind.NDFL,
                        title = "НДФЛ",
                        amount = summary.ndfl,
                        sortOrder = 10
                    )
                )
            }

            if (summary.deductionsTotal != 0.0) {
                add(
                    PayrollSheetBuilder.deductionLine(
                        kind = PayrollLineKind.EXECUTIVE_DEDUCTION,
                        title = "Удержания",
                        amount = summary.deductionsTotal,
                        sortOrder = 20,
                        note = "Позже разобьём по видам удержаний"
                    )
                )
            }
        }
    }

    private fun buildPriorPayments(summary: PayrollResult): List<PayrollLineItem> {
        return buildList {
            if (summary.advanceAmount != 0.0) {
                add(
                    PayrollSheetBuilder.priorPaymentLine(
                        kind = PayrollLineKind.ADVANCE_PAID,
                        title = "Аванс / первая половина месяца",
                        code = "8700",
                        amount = summary.advanceAmount,
                        sortOrder = 10
                    )
                )
            }
        }
    }

    private fun buildPayouts(summary: PayrollResult): List<PayrollLineItem> {
        return buildList {
            if (summary.salaryPaymentAmount != 0.0) {
                add(
                    PayrollSheetBuilder.payoutLine(
                        title = "К выплате за вторую половину месяца",
                        amount = summary.salaryPaymentAmount,
                        sortOrder = 10
                    )
                )
            }

            add(
                PayrollSheetBuilder.payoutLine(
                    title = "Итого к выплате",
                    amount = summary.netAfterDeductions,
                    sortOrder = 20
                )
            )
        }
    }

    private fun buildReference(summary: PayrollResult): List<PayrollLineItem> {
        return buildList {
            add(
                PayrollSheetBuilder.referenceLine(
                    title = "Налогооблагаемый доход",
                    amount = summary.taxableGrossTotal,
                    sortOrder = 10
                )
            )

            add(
                PayrollSheetBuilder.referenceLine(
                    title = "Неналогооблагаемые выплаты",
                    amount = summary.nonTaxableTotal,
                    sortOrder = 20
                )
            )

            add(
                PayrollSheetBuilder.referenceLine(
                    title = "Чистая сумма после удержаний",
                    amount = summary.netAfterDeductions,
                    sortOrder = 30
                )
            )

            add(
                PayrollSheetBuilder.referenceLine(
                    title = "Налоговая база YTD до месяца",
                    amount = summary.taxableIncomeYtdBeforeCurrentMonth,
                    sortOrder = 40
                )
            )

            add(
                PayrollSheetBuilder.referenceLine(
                    title = "Налоговая база YTD после месяца",
                    amount = summary.taxableIncomeYtdAfterCurrentMonth,
                    sortOrder = 50
                )
            )
        }
    }
}