package com.vigilante.shiftsalaryplanner.payroll

import com.vigilante.shiftsalaryplanner.DetailedShiftStats
import com.vigilante.shiftsalaryplanner.ResolvedAdditionalPaymentBreakdown
import com.vigilante.shiftsalaryplanner.formatHours
import java.time.YearMonth
import kotlin.math.round

object PayrollSheetDraftFactory {

    private data class SickPaySplit(
        val employerDays: Int,
        val employerAmount: Double,
        val externalDays: Int,
        val externalAmount: Double
    )

    fun build(
        month: YearMonth,
        periodLabel: String = month.toString(),
        periodSummarySuffix: String = "за месяц",
        periodNormHours: Double? = null,
        housingPaymentMonthsQuantity: Double = 1.0,
        summary: PayrollResult,
        detailedShiftStats: DetailedShiftStats,
        payrollSettings: PayrollSettings,
        housingPaymentLabel: String = "Выплата на квартиру",
        housingPaymentTaxable: Boolean = true,
        resolvedAdditionalPaymentBreakdown: List<ResolvedAdditionalPaymentBreakdown> = emptyList()
    ): PayrollDetailedResult {
        val headerItems = buildHeader(summary, detailedShiftStats, payrollSettings, periodNormHours)
        val accrualItems = buildAccruals(
            month = month,
            periodLabel = periodLabel,
            periodSummarySuffix = periodSummarySuffix,
            summary = summary,
            payrollSettings = payrollSettings,
            housingPaymentMonthsQuantity = housingPaymentMonthsQuantity,
            housingPaymentLabel = housingPaymentLabel,
            housingPaymentTaxable = housingPaymentTaxable,
            resolvedAdditionalPaymentBreakdown = resolvedAdditionalPaymentBreakdown
        )
        val deductionItems = buildDeductions(summary)
        val priorPaymentItems = buildPriorPayments(
            summary = summary,
            detailedShiftStats = detailedShiftStats,
            payrollSettings = payrollSettings,
            housingPaymentLabel = housingPaymentLabel,
            housingPaymentTaxable = housingPaymentTaxable,
            resolvedAdditionalPaymentBreakdown = resolvedAdditionalPaymentBreakdown
        )
        val payoutItems = buildPayouts(
            summary = summary,
            detailedShiftStats = detailedShiftStats,
            payrollSettings = payrollSettings,
            housingPaymentLabel = housingPaymentLabel,
            housingPaymentTaxable = housingPaymentTaxable,
            resolvedAdditionalPaymentBreakdown = resolvedAdditionalPaymentBreakdown
        )
        val referenceItems = buildReference(summary, periodSummarySuffix)

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

    private fun roundMoneyForSheet(value: Double): Double = round(value * 100.0) / 100.0

    private fun proportionalNdflForAmount(grossAmount: Double, summary: PayrollResult): Double {
        if (grossAmount <= 0.0) return 0.0
        if (summary.taxableGrossTotal <= 0.0) return 0.0
        if (summary.ndfl <= 0.0) return 0.0
        return roundMoneyForSheet(summary.ndfl * grossAmount / summary.taxableGrossTotal)
    }

    private fun proportionalNetForAmount(grossAmount: Double, summary: PayrollResult): Double {
        val ndfl = proportionalNdflForAmount(grossAmount, summary)
        return roundMoneyForSheet((grossAmount - ndfl).coerceAtLeast(0.0))
    }

    private fun resolvedExtraSalaryMode(settings: PayrollSettings): ExtraSalaryMode {
        return runCatching { ExtraSalaryMode.valueOf(settings.extraSalaryMode) }
            .getOrElse { ExtraSalaryMode.INCLUDED_IN_RATE }
    }

    private fun resolvedWorkRatio(summary: PayrollResult, settings: PayrollSettings): Double {
        return when {
            settings.monthlyNormHours > 0.0 -> (summary.workedHours / settings.monthlyNormHours).coerceIn(0.0, 1.0)
            summary.workedHours > 0.0 -> 1.0
            else -> 0.0
        }
    }

    private fun resolvedFixedExtraPay(summary: PayrollResult, settings: PayrollSettings): Double {
        if (resolvedExtraSalaryMode(settings) != ExtraSalaryMode.FIXED_MONTHLY) return 0.0
        val ratio = resolvedWorkRatio(summary, settings)
        return roundMoneyForSheet(settings.extraSalary.coerceAtLeast(0.0) * ratio)
    }

    private fun resolvedBaseHourlyRate(summary: PayrollResult, payrollSettings: PayrollSettings): Double {
        if (resolvedExtraSalaryMode(payrollSettings) == ExtraSalaryMode.FIXED_MONTHLY) {
            return roundMoneyForSheet(summary.hourlyRate)
        }
        val configuredBase = payrollSettings.baseSalary.coerceAtLeast(0.0)
        val configuredExtra = payrollSettings.extraSalary.coerceAtLeast(0.0)
        val configuredTotal = configuredBase + configuredExtra
        if (summary.hourlyRate <= 0.0) return 0.0
        if (configuredTotal <= 0.0) return summary.hourlyRate
        return roundMoneyForSheet(summary.hourlyRate * configuredBase / configuredTotal)
    }

    private fun resolvedExtraHourlyRate(summary: PayrollResult, payrollSettings: PayrollSettings): Double {
        if (resolvedExtraSalaryMode(payrollSettings) == ExtraSalaryMode.FIXED_MONTHLY) {
            return 0.0
        }
        val configuredBase = payrollSettings.baseSalary.coerceAtLeast(0.0)
        val configuredExtra = payrollSettings.extraSalary.coerceAtLeast(0.0)
        val configuredTotal = configuredBase + configuredExtra
        if (summary.hourlyRate <= 0.0) return 0.0
        if (configuredExtra <= 0.0) return 0.0
        if (configuredTotal <= 0.0) return 0.0
        return roundMoneyForSheet(summary.hourlyRate * configuredExtra / configuredTotal)
    }

    private fun splitSickPay(
        summary: PayrollResult,
        payrollSettings: PayrollSettings
    ): SickPaySplit {
        if (summary.sickDays <= 0 || summary.sickPay <= 0.0) {
            return SickPaySplit(
                employerDays = 0,
                employerAmount = 0.0,
                externalDays = 0,
                externalAmount = 0.0
            )
        }

        val employerDays = minOf(3, summary.sickDays)
        val externalDays = (summary.sickDays - employerDays).coerceAtLeast(0)
        if (externalDays == 0) {
            return SickPaySplit(
                employerDays = employerDays,
                employerAmount = summary.sickPay,
                externalDays = 0,
                externalAmount = 0.0
            )
        }

        val sickDailyAmount = minOf(
            payrollSettings.sickAverageDaily.coerceAtLeast(0.0) *
                payrollSettings.sickPayPercent.coerceAtLeast(0.0),
            payrollSettings.sickMaxDailyAmount.coerceAtLeast(0.0)
        )

        val employerAmountByFormula = roundMoneyForSheet(employerDays * sickDailyAmount)
        val employerAmount = employerAmountByFormula.coerceIn(0.0, summary.sickPay)
        val externalAmount = roundMoneyForSheet((summary.sickPay - employerAmount).coerceAtLeast(0.0))

        return SickPaySplit(
            employerDays = employerDays,
            employerAmount = employerAmount,
            externalDays = externalDays,
            externalAmount = externalAmount
        )
    }

    private fun isInterstimPayment(item: ResolvedAdditionalPaymentBreakdown): Boolean {
        val name = item.payment.displayName.lowercase()
        return "интерстим" in name
    }

    private fun isIndexationPayment(item: ResolvedAdditionalPaymentBreakdown): Boolean {
        val name = item.payment.displayName.lowercase()
        return "индекс" in name
    }

    private fun isHousingPayment(item: ResolvedAdditionalPaymentBreakdown): Boolean {
        val name = item.payment.displayName.lowercase()
        return "арен" in name || "жиль" in name || "квартир" in name
    }

    private fun isPremiumPayment(item: ResolvedAdditionalPaymentBreakdown): Boolean {
        val name = item.payment.displayName.lowercase()
        return "прем" in name
    }

    private fun resolvedPaymentKind(item: ResolvedAdditionalPaymentBreakdown): PayrollLineKind = when {
        isInterstimPayment(item) -> PayrollLineKind.INTERSTIM_ALLOWANCE
        isIndexationPayment(item) -> PayrollLineKind.INDEXATION_PAYMENT
        isHousingPayment(item) -> PayrollLineKind.HOUSING_COMPENSATION
        isPremiumPayment(item) -> PayrollLineKind.OPERATION_PREMIUM
        else -> PayrollLineKind.OTHER
    }

    private fun resolvedPaymentSortOrder(item: ResolvedAdditionalPaymentBreakdown): Int = when (resolvedPaymentKind(item)) {
        PayrollLineKind.INTERSTIM_ALLOWANCE -> 65
        PayrollLineKind.INDEXATION_PAYMENT -> 66
        PayrollLineKind.HOUSING_COMPENSATION -> 67
        PayrollLineKind.OPERATION_PREMIUM -> 68
        PayrollLineKind.OTHER -> 69
        else -> 69
    }

    private fun resolvedPaymentTitle(item: ResolvedAdditionalPaymentBreakdown): String = when (resolvedPaymentKind(item)) {
        PayrollLineKind.INTERSTIM_ALLOWANCE -> "Стимулирующая надбавка"
        PayrollLineKind.INDEXATION_PAYMENT -> "Индексирующая выплата"
        PayrollLineKind.HOUSING_COMPENSATION -> "Компенсация аренды / жилья"
        PayrollLineKind.OPERATION_PREMIUM -> "Премия"
        PayrollLineKind.OTHER -> item.payment.displayName
        else -> item.payment.displayName
    }

    private fun buildHeader(
        summary: PayrollResult,
        detailedShiftStats: DetailedShiftStats,
        payrollSettings: PayrollSettings,
        periodNormHours: Double?
    ): List<PayrollLineItem> {
        val baseHourlyRate = resolvedBaseHourlyRate(summary, payrollSettings)
        val extraHourlyRate = resolvedExtraHourlyRate(summary, payrollSettings)
        val resolvedPeriodNormHours = (periodNormHours ?: payrollSettings.monthlyNormHours).coerceAtLeast(0.0)
        val workedHoursNormText = if (resolvedPeriodNormHours > 0.0) {
            "Норма ${formatHours(resolvedPeriodNormHours)} ч"
        } else {
            null
        }
        return buildList {
            if (summary.hourlyRate > 0.0) {
                add(PayrollSheetBuilder.headerLine("Часовая ставка", summary.hourlyRate, 10))
            }
            if (baseHourlyRate > 0.0) {
                add(PayrollSheetBuilder.headerLine("Окладная ставка в час", baseHourlyRate, 20))
            }
            if (extraHourlyRate > 0.0) {
                add(PayrollSheetBuilder.headerLine("Стимулирующая в час", extraHourlyRate, 30))
            }
            if (summary.workedHours > 0.0 || resolvedPeriodNormHours > 0.0) {
                add(
                    PayrollSheetBuilder.headerLine(
                        "Отработано часов",
                        summary.workedHours,
                        40,
                        unit = PayrollQuantityUnit.HOURS,
                        amountTextOverride = workedHoursNormText
                    )
                )
            }
            if (summary.nightHours > 0.0) {
                add(PayrollSheetBuilder.headerLine("Ночных часов", summary.nightHours, 50, unit = PayrollQuantityUnit.HOURS))
            }
            if (detailedShiftStats.workedShiftCount > 0) {
                add(PayrollSheetBuilder.headerLine("Рабочих смен", detailedShiftStats.workedShiftCount.toDouble(), 60, unit = PayrollQuantityUnit.TIMES))
            }
        }
    }

    private fun buildAccruals(
        month: YearMonth,
        periodLabel: String,
        periodSummarySuffix: String,
        summary: PayrollResult,
        payrollSettings: PayrollSettings,
        housingPaymentMonthsQuantity: Double,
        housingPaymentLabel: String,
        housingPaymentTaxable: Boolean,
        resolvedAdditionalPaymentBreakdown: List<ResolvedAdditionalPaymentBreakdown>
    ): List<PayrollLineItem> = buildList {
        val hasHousingInBreakdown = resolvedAdditionalPaymentBreakdown.any(::isHousingPayment)
        val extraMode = resolvedExtraSalaryMode(payrollSettings)
        val baseHourlyRate = resolvedBaseHourlyRate(summary, payrollSettings)
        val extraHourlyRate = resolvedExtraHourlyRate(summary, payrollSettings)
        val fixedExtraPay = resolvedFixedExtraPay(summary, payrollSettings)
        val resolvedBaseGross = if (extraMode == ExtraSalaryMode.FIXED_MONTHLY) {
            (summary.basePay - fixedExtraPay).coerceAtLeast(0.0)
        } else {
            roundMoneyForSheet(baseHourlyRate * summary.workedHours)
        }
        val fixedExtraQuantity = resolvedWorkRatio(summary, payrollSettings)

        if (resolvedBaseGross > 0.0 && summary.workedHours > 0.0) {
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.BASE_SALARY,
                title = "Оклад",
                amount = resolvedBaseGross,
                sortOrder = 10,
                periodLabel = periodLabel,
                quantity = summary.workedHours,
                unit = PayrollQuantityUnit.HOURS,
                ndflAmount = proportionalNdflForAmount(resolvedBaseGross, summary),
                netAmount = proportionalNetForAmount(resolvedBaseGross, summary),
                expandableDetails = true
            ))
        }

        if (extraMode == ExtraSalaryMode.FIXED_MONTHLY && fixedExtraPay > 0.0) {
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.INTERSTIM_ALLOWANCE,
                title = "Стимулирующая надбавка",
                amount = fixedExtraPay,
                sortOrder = 20,
                quantity = fixedExtraQuantity,
                unit = PayrollQuantityUnit.MONTHS,
                note = "Пропорционально отработке",
                ndflAmount = proportionalNdflForAmount(fixedExtraPay, summary),
                netAmount = proportionalNetForAmount(fixedExtraPay, summary),
                expandableDetails = true
            ))
        } else if (extraHourlyRate > 0.0 && summary.workedHours > 0.0) {
            val gross = roundMoneyForSheet(extraHourlyRate * summary.workedHours)
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.INTERSTIM_ALLOWANCE,
                title = "Стимулирующая надбавка",
                amount = gross,
                sortOrder = 20,
                quantity = summary.workedHours,
                unit = PayrollQuantityUnit.HOURS,
                ndflAmount = proportionalNdflForAmount(gross, summary),
                netAmount = proportionalNetForAmount(gross, summary),
                expandableDetails = true
            ))
        }

        if (summary.nightExtra > 0.0) {
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.NIGHT_EXTRA,
                title = "Ночные",
                amount = summary.nightExtra,
                sortOrder = 30,
                quantity = summary.nightHours,
                unit = PayrollQuantityUnit.HOURS,
                ndflAmount = proportionalNdflForAmount(summary.nightExtra, summary),
                netAmount = proportionalNetForAmount(summary.nightExtra, summary),
                expandableDetails = true
            ))
        }

        if (summary.holidayExtra > 0.0) {
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.SPECIAL_DAY_EXTRA,
                title = "Выходные / праздники",
                amount = summary.holidayExtra,
                sortOrder = 40,
                quantity = summary.holidayHours,
                unit = PayrollQuantityUnit.HOURS,
                ndflAmount = proportionalNdflForAmount(summary.holidayExtra, summary),
                netAmount = proportionalNetForAmount(summary.holidayExtra, summary),
                expandableDetails = true
            ))
        }

        if (summary.vacationPay > 0.0) {
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.VACATION_PAY,
                title = "Отпуск",
                amount = summary.vacationPay,
                sortOrder = 50,
                quantity = summary.vacationDays.toDouble(),
                unit = PayrollQuantityUnit.DAYS,
                ndflAmount = proportionalNdflForAmount(summary.vacationPay, summary),
                netAmount = proportionalNetForAmount(summary.vacationPay, summary),
                expandableDetails = true
            ))
        }

        val sickPaySplit = splitSickPay(summary, payrollSettings)
        if (sickPaySplit.employerAmount > 0.0) {
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.SICK_PAY_EMPLOYER,
                title = "Больничный (работодатель)",
                amount = sickPaySplit.employerAmount,
                sortOrder = 60,
                quantity = sickPaySplit.employerDays.toDouble(),
                unit = PayrollQuantityUnit.DAYS,
                note = "Первые 3 дня",
                ndflAmount = proportionalNdflForAmount(sickPaySplit.employerAmount, summary),
                netAmount = proportionalNetForAmount(sickPaySplit.employerAmount, summary),
                expandableDetails = true
            ))
        }

        if (sickPaySplit.externalAmount > 0.0) {
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.SICK_PAY_EXTERNAL,
                title = "Больничный (СФР)",
                amount = sickPaySplit.externalAmount,
                sortOrder = 61,
                quantity = sickPaySplit.externalDays.toDouble(),
                unit = PayrollQuantityUnit.DAYS,
                note = "С 4-го дня",
                ndflAmount = proportionalNdflForAmount(sickPaySplit.externalAmount, summary),
                netAmount = proportionalNetForAmount(sickPaySplit.externalAmount, summary),
                expandableDetails = true
            ))
        }

        if (summary.housingPayment > 0.0 && !hasHousingInBreakdown) {
            val ndfl = if (housingPaymentTaxable) proportionalNdflForAmount(summary.housingPayment, summary) else 0.0
            val net = if (housingPaymentTaxable) proportionalNetForAmount(summary.housingPayment, summary) else summary.housingPayment
            add(PayrollSheetBuilder.accrualLine(
                kind = PayrollLineKind.HOUSING_COMPENSATION,
                title = housingPaymentLabel,
                amount = summary.housingPayment,
                sortOrder = 70,
                quantity = housingPaymentMonthsQuantity.coerceAtLeast(0.0),
                unit = PayrollQuantityUnit.MONTHS,
                ndflAmount = ndfl,
                netAmount = net,
                expandableDetails = true
            ))
        }

        resolvedAdditionalPaymentBreakdown
            .filterNot(::isInterstimPayment)
            .sortedBy { resolvedPaymentSortOrder(it) }
            .forEachIndexed { index, item ->
                add(PayrollSheetBuilder.accrualLine(
                    kind = resolvedPaymentKind(item),
                    title = resolvedPaymentTitle(item),
                    amount = item.grossAmount,
                    sortOrder = 80 + index,
                    note = if (item.payment.withAdvance) "Часть уходит в аванс" else "Учитывается во второй выплате",
                    ndflAmount = item.ndflAmount,
                    netAmount = item.netAmount,
                    expandableDetails = true
                ))
            }

        if (summary.grossTotal > 0.0) {
            add(
                PayrollSheetBuilder.accrualLine(
                    kind = PayrollLineKind.REFERENCE_VALUE,
                    title = "Итого начислено $periodSummarySuffix",
                    amount = summary.grossTotal,
                    sortOrder = 9_999,
                    ndflAmount = summary.ndfl,
                    netAmount = summary.netTotal,
                    expandableDetails = true
                )
            )
        }
    }

    private fun buildDeductions(summary: PayrollResult): List<PayrollLineItem> = buildList {
        if (summary.ndfl != 0.0) {
            add(PayrollSheetBuilder.deductionLine(PayrollLineKind.NDFL, "НДФЛ", summary.ndfl, 10))
        }
        if (summary.deductionsTotal != 0.0) {
            add(PayrollSheetBuilder.deductionLine(
                kind = PayrollLineKind.EXECUTIVE_DEDUCTION,
                title = "Прочие удержания",
                amount = summary.deductionsTotal,
                sortOrder = 20
            ))
        }
    }

    private fun buildShiftAdvanceDetails(
        summary: PayrollResult,
        detailedShiftStats: DetailedShiftStats,
        payrollSettings: PayrollSettings
    ): List<PayrollLineBreakdownItem> = buildShiftHalfDetails(
        summary = summary,
        workedHours = detailedShiftStats.firstHalfWorkedHours,
        nightHours = detailedShiftStats.firstHalfNightHours,
        payrollSettings = payrollSettings
    )

    private fun buildShiftSalaryDetails(
        summary: PayrollResult,
        detailedShiftStats: DetailedShiftStats,
        payrollSettings: PayrollSettings
    ): List<PayrollLineBreakdownItem> = buildShiftHalfDetails(
        summary = summary,
        workedHours = detailedShiftStats.secondHalfWorkedHours,
        nightHours = detailedShiftStats.secondHalfNightHours,
        payrollSettings = payrollSettings
    )

    private fun buildShiftHalfDetails(
        summary: PayrollResult,
        workedHours: Double,
        nightHours: Double,
        payrollSettings: PayrollSettings
    ): List<PayrollLineBreakdownItem> {
        val details = mutableListOf<PayrollLineBreakdownItem>()
        val extraMode = resolvedExtraSalaryMode(payrollSettings)
        val baseHourlyRate = resolvedBaseHourlyRate(summary, payrollSettings)
        val extraHourlyRate = resolvedExtraHourlyRate(summary, payrollSettings)
        val fixedExtraTotal = resolvedFixedExtraPay(summary, payrollSettings)
        val baseWithoutFixedExtraTotal = (summary.basePay - fixedExtraTotal).coerceAtLeast(0.0)
        val halfRatio = if (summary.workedHours > 0.0) {
            (workedHours / summary.workedHours).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        if (baseHourlyRate > 0.0 && workedHours > 0.0) {
            val gross = if (extraMode == ExtraSalaryMode.FIXED_MONTHLY) {
                roundMoneyForSheet(baseWithoutFixedExtraTotal * halfRatio)
            } else {
                roundMoneyForSheet(baseHourlyRate * workedHours)
            }
            val ndfl = proportionalNdflForAmount(gross, summary)
            val net = proportionalNetForAmount(gross, summary)
            details += PayrollLineBreakdownItem("Оклад", gross, workedHours, PayrollQuantityUnit.HOURS, ndfl, net)
        }

        if (extraMode == ExtraSalaryMode.FIXED_MONTHLY && fixedExtraTotal > 0.0 && halfRatio > 0.0) {
            val gross = roundMoneyForSheet(fixedExtraTotal * halfRatio)
            val ndfl = proportionalNdflForAmount(gross, summary)
            val net = proportionalNetForAmount(gross, summary)
            details += PayrollLineBreakdownItem(
                title = "Стимулирующая надбавка",
                amount = gross,
                quantity = roundMoneyForSheet(resolvedWorkRatio(summary, payrollSettings) * halfRatio),
                unit = PayrollQuantityUnit.MONTHS,
                ndflAmount = ndfl,
                netAmount = net,
                note = "Пропорционально части месяца"
            )
        } else if (extraHourlyRate > 0.0 && workedHours > 0.0) {
            val gross = roundMoneyForSheet(extraHourlyRate * workedHours)
            val ndfl = proportionalNdflForAmount(gross, summary)
            val net = proportionalNetForAmount(gross, summary)
            details += PayrollLineBreakdownItem("Стимулирующая надбавка", gross, workedHours, PayrollQuantityUnit.HOURS, ndfl, net)
        }

        if (summary.nightExtra > 0.0 && summary.nightHours > 0.0 && nightHours > 0.0) {
            val nightRate = summary.nightExtra / summary.nightHours
            val gross = roundMoneyForSheet(nightRate * nightHours)
            val ndfl = proportionalNdflForAmount(gross, summary)
            val net = proportionalNetForAmount(gross, summary)
            details += PayrollLineBreakdownItem("Ночные", gross, nightHours, PayrollQuantityUnit.HOURS, ndfl, net)
        }

        if (summary.holidayExtra > 0.0 && summary.workedHours > 0.0 && workedHours > 0.0) {
            val ratio = workedHours / summary.workedHours
            val gross = roundMoneyForSheet(summary.holidayExtra * ratio)
            if (gross > 0.0) {
                val ndfl = proportionalNdflForAmount(gross, summary)
                val net = proportionalNetForAmount(gross, summary)
                details += PayrollLineBreakdownItem("Выходные / праздники", gross, ndflAmount = ndfl, netAmount = net)
            }
        }

        return details.filter { it.amount != 0.0 }
    }

    private fun buildPriorPayments(
        summary: PayrollResult,
        detailedShiftStats: DetailedShiftStats,
        payrollSettings: PayrollSettings,
        housingPaymentLabel: String,
        housingPaymentTaxable: Boolean,
        resolvedAdditionalPaymentBreakdown: List<ResolvedAdditionalPaymentBreakdown>
    ): List<PayrollLineItem> = buildList {
        val shiftDetails = buildShiftAdvanceDetails(summary, detailedShiftStats, payrollSettings)
        val advanceSummaryDetails = mutableListOf<PayrollLineBreakdownItem>()

        if (summary.shiftOnlyAdvanceNetAmount > 0.0) {
            advanceSummaryDetails += PayrollLineBreakdownItem(
                title = "Сменная часть аванса",
                amount = summary.shiftOnlyAdvanceNetAmount,
                details = shiftDetails
            )
        }

        resolvedAdditionalPaymentBreakdown
            .filter { it.payment.withAdvance && !isInterstimPayment(it) && !isHousingPayment(it) }
            .sortedBy { resolvedPaymentSortOrder(it) }
            .forEachIndexed { index, item ->
                add(PayrollSheetBuilder.priorPaymentLine(
                    kind = resolvedPaymentKind(item),
                    title = "${resolvedPaymentTitle(item)} (в аванс)",
                    amount = item.netAmount,
                    sortOrder = 30 + index,
                    note = "На руки"
                ))
            }

        val hasHousingInAdvanceBreakdown = resolvedAdditionalPaymentBreakdown.any { it.payment.withAdvance && isHousingPayment(it) }
        if (summary.housingAdvancePart > 0.0 && !hasHousingInAdvanceBreakdown) {
            val net = if (housingPaymentTaxable) proportionalNetForAmount(summary.housingAdvancePart, summary) else summary.housingAdvancePart
            add(PayrollSheetBuilder.priorPaymentLine(
                kind = PayrollLineKind.HOUSING_COMPENSATION,
                title = "$housingPaymentLabel (в аванс)",
                amount = net,
                sortOrder = 50,
                note = "На руки"
            ))
        }

        add(PayrollSheetBuilder.priorPaymentLine(
            kind = PayrollLineKind.ADVANCE_PAID,
            title = "Аванс к выплате",
            amount = summary.advanceAmount,
            sortOrder = 10,
            details = advanceSummaryDetails,
            expandableDetails = advanceSummaryDetails.isNotEmpty()
        ))
    }

    private fun buildPayouts(
        summary: PayrollResult,
        detailedShiftStats: DetailedShiftStats,
        payrollSettings: PayrollSettings,
        housingPaymentLabel: String,
        housingPaymentTaxable: Boolean,
        resolvedAdditionalPaymentBreakdown: List<ResolvedAdditionalPaymentBreakdown>
    ): List<PayrollLineItem> = buildList {
        val shiftDetails = buildShiftSalaryDetails(summary, detailedShiftStats, payrollSettings)
        val salarySummaryDetails = mutableListOf<PayrollLineBreakdownItem>()

        if (summary.shiftOnlySalaryNetAmount > 0.0) {
            salarySummaryDetails += PayrollLineBreakdownItem(
                title = "Сменная часть зарплаты",
                amount = summary.shiftOnlySalaryNetAmount,
                details = shiftDetails
            )
        }

        resolvedAdditionalPaymentBreakdown
            .filter { !it.payment.withAdvance && !isInterstimPayment(it) && !isHousingPayment(it) }
            .sortedBy { resolvedPaymentSortOrder(it) }
            .forEachIndexed { index, item ->
                add(PayrollSheetBuilder.payoutLine(
                    title = "${resolvedPaymentTitle(item)} (во вторую половину)",
                    amount = item.netAmount,
                    sortOrder = 30 + index,
                    note = "На руки"
                ))
            }

        val hasHousingInSalaryBreakdown = resolvedAdditionalPaymentBreakdown.any { !it.payment.withAdvance && isHousingPayment(it) }
        if (summary.housingSalaryPart > 0.0 && !hasHousingInSalaryBreakdown) {
            val net = if (housingPaymentTaxable) proportionalNetForAmount(summary.housingSalaryPart, summary) else summary.housingSalaryPart
            add(PayrollSheetBuilder.payoutLine(
                title = "$housingPaymentLabel (во вторую половину)",
                amount = net,
                sortOrder = 50,
                note = "На руки"
            ))
        }

        add(PayrollSheetBuilder.payoutLine(
            title = "Зарплата к выплате",
            amount = summary.salaryPaymentAmount,
            sortOrder = 10,
            details = salarySummaryDetails,
            expandableDetails = salarySummaryDetails.isNotEmpty()
        ))
    }

    private fun buildReference(summary: PayrollResult, periodSummarySuffix: String): List<PayrollLineItem> = buildList {
        add(PayrollSheetBuilder.referenceLine("Всего начислено", summary.grossTotal, 10))
        add(PayrollSheetBuilder.referenceLine("Облагаемая база", summary.taxableGrossTotal, 20))
        if (summary.nonTaxableTotal > 0.0) {
            add(PayrollSheetBuilder.referenceLine("Необлагаемые выплаты", summary.nonTaxableTotal, 30))
        }
        add(PayrollSheetBuilder.referenceLine("НДФЛ $periodSummarySuffix", summary.ndfl, 40))
        add(PayrollSheetBuilder.referenceLine("На руки $periodSummarySuffix", summary.netTotal, 50))
    }
}
