package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import java.time.YearMonth

@Suppress("unused")
@Composable
fun PaymentsTab(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    payMode: String,
    paymentScheduleMode: String,
    housingPaymentLabel: String,
    additionalPayments: List<AdditionalPayment>,
    resolvedAdditionalPaymentsBreakdown: List<ResolvedAdditionalPaymentBreakdown>,
    detailedShiftStats: DetailedShiftStats,
    onAddPayment: () -> Unit,
    onEditPayment: (AdditionalPayment) -> Unit,
    onDeletePayment: (AdditionalPayment) -> Unit,
    onOpenMonthlyReport: () -> Unit,
    onOpenVisibilitySettings: () -> Unit,
    visibilitySettings: ReportVisibilitySettings,
    actualAdvanceNet: Double = 0.0,
    actualSalaryNet: Double = 0.0,
    paymentDifferenceToleranceRub: Double = 100.0,
    onSaveActualPayments: (Double, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val activeConfiguredPayments = remember(additionalPayments) { additionalPayments.filter { it.active } }
    val isPerShiftPayment = remember(payMode, paymentScheduleMode) {
        runCatching { PayMode.valueOf(payMode) }.getOrElse { PayMode.HOURLY } == PayMode.PER_SHIFT ||
            runCatching { PaymentScheduleMode.valueOf(paymentScheduleMode) }
                .getOrElse { PaymentScheduleMode.TWICE_MONTHLY } == PaymentScheduleMode.PER_SHIFT
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        MonthHeader(
            currentMonth = currentMonth,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onPickMonth = onPickMonth,
            useEvolution = true
        )

        Spacer(modifier = Modifier.height(appSectionSpacing()))
        PaymentsFactVsPlanCard(
            payroll = payroll,
            isPerShiftPayment = isPerShiftPayment,
            actualAdvanceNet = actualAdvanceNet,
            actualSalaryNet = actualSalaryNet,
            paymentDifferenceToleranceRub = paymentDifferenceToleranceRub,
            onSaveActualPayments = onSaveActualPayments
        )

        if (visibilitySettings.showPaymentsActionTiles) {
            Spacer(modifier = Modifier.height(appSectionSpacing()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                PaymentsReportTile(
                    onClick = onOpenMonthlyReport,
                    modifier = Modifier.weight(1f)
                )
                PaymentsVisibilityTile(
                    onClick = onOpenVisibilitySettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        if (!visibilitySettings.hasVisiblePaymentsBlocks()) {
            AppEmptyCard(
                title = "Все блоки скрыты",
                message = "Нажми «Видимость строк», чтобы включить нужные разделы вкладки «Выплаты»."
            )
        } else {
            val showMainSummaryContent = visibilitySettings.showPaymentsMainSummary &&
                (visibilitySettings.showPaymentsMainSummaryTopRow || visibilitySettings.showPaymentsMainSummaryBottomRow)
            if (showMainSummaryContent) {
                PaymentsSectionTitle("Главное за месяц")
                Spacer(modifier = Modifier.height(appBlockSpacing()))

                if (visibilitySettings.showPaymentsMainSummaryTopRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
                    ) {
                        if (isPerShiftPayment) {
                            PaymentsStatTile(
                                title = "За смены",
                                value = formatFinanceMoney(payroll.netAfterDeductions),
                                subtitle = "к выплате за месяц",
                                modifier = Modifier.weight(1f),
                                emphasize = true
                            )
                            PaymentsStatTile(
                                title = "Смены",
                                value = detailedShiftStats.workedShiftCount.toString(),
                                subtitle = "по шаблонам",
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            PaymentsStatTile(
                                title = "Аванс",
                                value = formatFinanceMoney(payroll.netAdvanceAfterDeductions),
                                subtitle = formatDate(paymentDates.advanceDate),
                                modifier = Modifier.weight(1f)
                            )
                            PaymentsStatTile(
                                title = "К зарплате",
                                value = formatFinanceMoney(payroll.netSalaryAfterDeductions),
                                subtitle = formatDate(paymentDates.salaryDate),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (visibilitySettings.showPaymentsMainSummaryTopRow && visibilitySettings.showPaymentsMainSummaryBottomRow) {
                    Spacer(modifier = Modifier.height(appBlockSpacing()))
                }

                if (visibilitySettings.showPaymentsMainSummaryBottomRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
                    ) {
                        PaymentsStatTile(
                            title = "На руки",
                            value = formatFinanceMoney(payroll.netTotal),
                            subtitle = "за месяц",
                            modifier = Modifier.weight(1f),
                            emphasize = true
                        )
                        PaymentsStatTile(
                            title = "Смен",
                            value = detailedShiftStats.workedShiftCount.toString(),
                            subtitle = "рабочих",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(appSectionSpacing()))
            }

            val showPayoutAndTotalsContent = visibilitySettings.showPaymentsPayoutAndTotals &&
                (visibilitySettings.showPaymentsPayoutCard || visibilitySettings.showPaymentsTotalsCard)
            if (showPayoutAndTotalsContent) {
                PaymentsSectionTitle("Выплаты и итог")
                Spacer(modifier = Modifier.height(appBlockSpacing()))

                if (visibilitySettings.showPaymentsPayoutCard) {
                    PaymentsPanelCard(title = "Выплаты") {
                        if (isPerShiftPayment) {
                            PaymentInfoRow("Режим", "после каждой смены", bold = true)
                            PaymentInfoRow("К выплате за смены", formatFinanceMoney(payroll.netAfterDeductions), bold = payroll.netAfterDeductions > 0.0)
                            PaymentInfoRow("Смен оплачено", detailedShiftStats.workedShiftCount.toString())
                        } else {
                            PaymentInfoRow("Аванс", formatFinanceMoney(payroll.netAdvanceAfterDeductions), bold = payroll.netAdvanceAfterDeductions > 0.0)
                            PaymentInfoRow("Только по сменам", formatFinanceMoney(payroll.shiftOnlyAdvanceNetAmount))
                            PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
                            CompactDivider()
                            PaymentInfoRow("К зарплате", formatFinanceMoney(payroll.netSalaryAfterDeductions), bold = payroll.netSalaryAfterDeductions > 0.0)
                            PaymentInfoRow("Только по сменам", formatFinanceMoney(payroll.shiftOnlySalaryNetAmount))
                            PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
                        }
                    }
                }

                if (visibilitySettings.showPaymentsPayoutCard && visibilitySettings.showPaymentsTotalsCard) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (visibilitySettings.showPaymentsTotalsCard) {
                    PaymentsPanelCard(title = "Итоги начисления") {
                        PaymentInfoRow("Допвыплаты всего", formatFinanceMoney(payroll.additionalPaymentsTotal))
                        PaymentInfoRow("В аванс", formatFinanceMoney(payroll.additionalPaymentsAdvancePart))
                        PaymentInfoRow("В зарплату", formatFinanceMoney(payroll.additionalPaymentsSalaryPart))
                        CompactDivider()
                        PaymentInfoRow("Облагаемая база", formatFinanceMoney(payroll.taxableGrossTotal))
                        PaymentInfoRow("Необлагаемые выплаты", formatFinanceMoney(payroll.nonTaxableTotal))
                        PaymentInfoRow("Всего начислено", formatFinanceMoney(payroll.grossTotal))
                        PaymentInfoRow(
                            "НДФЛ",
                            if (isPerShiftPayment && payroll.ndfl == 0.0) "не удерживается с суммы за смену" else formatFinanceMoney(payroll.ndfl)
                        )
                        PaymentInfoRow("На руки", formatFinanceMoney(payroll.netTotal), bold = true)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            val showShiftCostsContent = visibilitySettings.showPaymentsShiftCosts &&
                (visibilitySettings.showPaymentsShiftStatsCard || visibilitySettings.showPaymentsShiftCostCard)
            if (showShiftCostsContent) {
                PaymentsSectionTitle("Смены и стоимость")
                Spacer(modifier = Modifier.height(8.dp))

                if (visibilitySettings.showPaymentsShiftStatsCard) {
                    PaymentsPanelCard(title = "Статистика смен") {
                        PaymentInfoRow("Всего отмеченных дней", detailedShiftStats.totalAssignedDays.toString())
                        PaymentInfoRow("Рабочих смен", detailedShiftStats.workedShiftCount.toString(), bold = detailedShiftStats.workedShiftCount > 0)
                        PaymentInfoRow("Дневных", detailedShiftStats.dayShiftCount.toString())
                        PaymentInfoRow("Ночных", detailedShiftStats.nightShiftCount.toString())
                        PaymentInfoRow("Выходных/праздничных", detailedShiftStats.weekendHolidayShiftCount.toString())
                        PaymentInfoRow("Восьмичасовой раб.день", detailedShiftStats.eightHourShiftCount.toString())
                        PaymentInfoRow("Отпуск", detailedShiftStats.vacationShiftCount.toString())
                        PaymentInfoRow("Больничный", detailedShiftStats.sickShiftCount.toString())
                        PaymentInfoRow("Смен в 1-й половине", detailedShiftStats.firstHalfWorkedShifts.toString())
                        PaymentInfoRow("Смен во 2-й половине", detailedShiftStats.secondHalfWorkedShifts.toString())
                    }
                }

                if (visibilitySettings.showPaymentsShiftStatsCard && visibilitySettings.showPaymentsShiftCostCard) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (visibilitySettings.showPaymentsShiftCostCard) {
                    PaymentsPanelCard(title = "Стоимость смены") {
                        PaymentInfoRow("База расчёта", formatFinanceMoney(detailedShiftStats.shiftCostBaseTotal))
                        PaymentInfoRow("Учтено доплат", formatFinanceMoney(detailedShiftStats.shiftCostIncludedPayments))
                        PaymentInfoRow("Рабочих смен", detailedShiftStats.workedShiftCount.toString())
                        CompactDivider()
                        PaymentInfoRow("Средняя (до НДФЛ)", formatFinanceMoney(detailedShiftStats.shiftCostAverageGross), bold = detailedShiftStats.shiftCostAverageGross > 0.0)
                        PaymentInfoRow("Средняя (на руки)", formatFinanceMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
                        PaymentInfoRow("Дневная (на руки)", formatFinanceMoney(detailedShiftStats.dayShiftCostAverageNet), bold = detailedShiftStats.dayShiftCostAverageNet > 0.0)
                        PaymentInfoRow("Ночная (на руки)", formatFinanceMoney(detailedShiftStats.nightShiftCostAverageNet), bold = detailedShiftStats.nightShiftCostAverageNet > 0.0)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            val showAdditionalContent = visibilitySettings.showPaymentsAdditionalPayments &&
                (visibilitySettings.showPaymentsBaseAllowanceCard || visibilitySettings.showPaymentsMonthAdditionalCard || visibilitySettings.showPaymentsConfiguredAdditionalCard)
            if (showAdditionalContent) {
                PaymentsSectionTitle("Доплаты и премии")
                Spacer(modifier = Modifier.height(8.dp))

                if (visibilitySettings.showPaymentsBaseAllowanceCard) {
                    PaymentsPanelCard(title = "Основные доплаты") {
                        PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatFinanceMoney(payroll.housingPayment))
                        PaymentInfoRow("В аванс", formatFinanceMoney(payroll.housingAdvancePart))
                        PaymentInfoRow("В зарплату", formatFinanceMoney(payroll.housingSalaryPart))
                        PaymentInfoRow(
                            "Налогообложение",
                            if (payroll.housingPaymentTaxable) "Облагается НДФЛ" else "Не облагается"
                        )
                    }
                }

                if (
                    visibilitySettings.showPaymentsBaseAllowanceCard &&
                    (visibilitySettings.showPaymentsMonthAdditionalCard || visibilitySettings.showPaymentsConfiguredAdditionalCard)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (visibilitySettings.showPaymentsMonthAdditionalCard) {
                    PaymentsPanelCard(title = "Доплаты месяца") {
                        if (resolvedAdditionalPaymentsBreakdown.isEmpty()) {
                            Text("В этом месяце активных начислений по доплатам и премиям нет.")
                        } else {
                            resolvedAdditionalPaymentsBreakdown.forEachIndexed { index, item ->
                                PaymentInfoRow(item.payment.displayName, additionalPaymentTypeLabel(item.payment.sourceTypeName), bold = true)
                                PaymentInfoRow("До НДФЛ", formatFinanceMoney(item.grossAmount), bold = item.grossAmount != 0.0)
                                PaymentInfoRow("НДФЛ", formatFinanceMoney(item.ndflAmount))
                                PaymentInfoRow("На руки", formatFinanceMoney(item.netAmount), bold = item.netAmount != 0.0)
                                PaymentInfoRow(
                                    "Параметры",
                                    buildString {
                                        append(if (item.payment.withAdvance) "в аванс" else "в зарплату")
                                        append(" • ")
                                        append(if (item.payment.taxable) "облагается" else "не облагается")
                                    }
                                )
                                if (index != resolvedAdditionalPaymentsBreakdown.lastIndex) {
                                    CompactDivider()
                                }
                            }
                        }
                    }
                }

                if (
                    visibilitySettings.showPaymentsConfiguredAdditionalCard &&
                    (visibilitySettings.showPaymentsBaseAllowanceCard || visibilitySettings.showPaymentsMonthAdditionalCard)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (visibilitySettings.showPaymentsConfiguredAdditionalCard) {
                    PaymentsPanelCard(title = "Настроенные начисления") {
                        if (activeConfiguredPayments.isEmpty()) {
                            Text("Нет активных доплат и премий.")
                        } else {
                            activeConfiguredPayments.forEachIndexed { index, payment ->
                                PaymentInfoRow(payment.name.ifBlank { "Без названия" }, additionalPaymentTypeLabel(payment.type), bold = true)
                                PaymentInfoRow("Параметры", additionalPaymentDetailsLabel(payment))
                                PaymentInfoRow("Начисление", paymentDistributionLabel(payment.distribution))
                                if (index != activeConfiguredPayments.lastIndex) {
                                    CompactDivider()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            val showAbsenceAndOvertimeContent = visibilitySettings.showPaymentsAbsenceAndOvertime &&
                (visibilitySettings.showPaymentsAbsenceCard || visibilitySettings.showPaymentsOvertimeCard)
            if (showAbsenceAndOvertimeContent) {
                PaymentsSectionTitle("Отсутствия и переработка")
                Spacer(modifier = Modifier.height(8.dp))

                if (visibilitySettings.showPaymentsAbsenceCard) {
                    PaymentsPanelCard(title = "Отпуск и больничный") {
                        PaymentInfoRow("Дней отпуска", payroll.vacationDays.toString())
                        PaymentInfoRow("Отпускные", formatFinanceMoney(payroll.vacationPay))
                        PaymentInfoRow("Дней больничного", payroll.sickDays.toString())
                        PaymentInfoRow("Больничный", formatFinanceMoney(payroll.sickPay))
                    }
                }

                if (visibilitySettings.showPaymentsAbsenceCard && visibilitySettings.showPaymentsOvertimeCard) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (visibilitySettings.showPaymentsOvertimeCard) {
                    PaymentsPanelCard(title = "Сверхурочка: ${annualOvertime.periodLabel}") {
                        PaymentInfoRow("Статус", if (annualOvertime.enabled) "Включена" else "Отключена")
                        PaymentInfoRow("Норма периода", formatDouble(annualOvertime.annualNormHours))
                        PaymentInfoRow("Отработано", formatDouble(annualOvertime.workedHours))
                        PaymentInfoRow("К оплате", formatDouble(annualOvertime.payableOvertimeHours), bold = annualOvertime.payableOvertimeHours > 0.0)
                        PaymentInfoRow("Первые 2 часа", formatDouble(annualOvertime.firstTwoHours))
                        PaymentInfoRow("Остальные часы", formatDouble(annualOvertime.remainingHours))
                        PaymentInfoRow("Часовая ставка", formatFinanceMoney(annualOvertime.hourlyRate))
                        PaymentInfoRow("Доплата", formatFinanceMoney(annualOvertime.overtimePremiumAmount), bold = annualOvertime.overtimePremiumAmount > 0.0)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(24.dp)))
    }
}

@Composable
private fun PaymentsFactVsPlanCard(
    payroll: PayrollResult,
    isPerShiftPayment: Boolean,
    actualAdvanceNet: Double,
    actualSalaryNet: Double,
    paymentDifferenceToleranceRub: Double,
    onSaveActualPayments: (Double, Double) -> Unit
) {
    var advanceText by rememberSaveable(actualAdvanceNet) {
        mutableStateOf(if (actualAdvanceNet > 0.0) formatDouble(actualAdvanceNet) else "")
    }
    var salaryText by rememberSaveable(actualSalaryNet) {
        mutableStateOf(if (actualSalaryNet > 0.0) formatDouble(actualSalaryNet) else "")
    }
    val actualAdvance = advanceText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val actualSalary = salaryText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val expectedTotal = if (isPerShiftPayment) {
        payroll.netAfterDeductions
    } else {
        payroll.netAdvanceAfterDeductions + payroll.netSalaryAfterDeductions
    }
    val actualTotal = if (isPerShiftPayment) actualAdvance else actualAdvance + actualSalary
    val hasActual = actualAdvance > 0.0 || actualSalary > 0.0
    val delta = actualTotal - expectedTotal
    val tolerance = paymentDifferenceToleranceRub.coerceAtLeast(0.0)
    val mismatch = hasActual && kotlin.math.abs(delta) > tolerance

    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = if (mismatch) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            Text(
                text = "Ожидалось / пришло",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (isPerShiftPayment) {
                PaymentsFactVsPlanRow("За смены", payroll.netAfterDeductions, actualAdvance)
            } else {
                PaymentsFactVsPlanRow("Аванс", payroll.netAdvanceAfterDeductions, actualAdvance)
                PaymentsFactVsPlanRow("Зарплата", payroll.netSalaryAfterDeductions, actualSalary)
            }
            if (hasActual) {
                PaymentsFactVsPlanRow("Итого", expectedTotal, actualTotal, emphasize = true)
                Text(
                    text = if (mismatch) {
                        "Разница ${formatFinanceMoney(delta)} превышает допуск ${formatFinanceMoney(tolerance)}"
                    } else {
                        "Разница ${formatFinanceMoney(delta)} в пределах допуска ${formatFinanceMoney(tolerance)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                CompactTextField(
                    label = if (isPerShiftPayment) "За смены пришло" else "Аванс пришёл",
                    value = advanceText,
                    onValueChange = { advanceText = it },
                    modifier = Modifier.weight(1f)
                )
                if (!isPerShiftPayment) {
                    CompactTextField(
                        label = "Зарплата пришла",
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            TextButton(
                onClick = appHapticAction { onSaveActualPayments(actualAdvance, actualSalary) },
                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
            ) {
                Text("Сохранить факт")
            }
        }
    }
}

@Composable
private fun PaymentsFactVsPlanRow(
    title: String,
    expected: Double,
    actual: Double,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${formatFinanceMoney(expected)} / ${if (actual > 0.0) formatFinanceMoney(actual) else "не указано"}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PaymentsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PaymentsPanelCard(
    title: String,
    content: @Composable () -> Unit
) {
    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appScaledSpacing(11.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun CompactDivider() {
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun PaymentsStatTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    EvolutionSurface(
        modifier = modifier,
        role = if (emphasize) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appScaledSpacing(11.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PaymentsReportTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EvolutionSurface(
        modifier = modifier
            .clickable(onClick = appHapticAction(onAction = onClick)),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appSectionSpacing()),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Подробный отчёт",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Расшифровка начислений за месяц и экспорт CSV/PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Открыть",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PaymentsVisibilityTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EvolutionSurface(
        modifier = modifier
            .clickable(onClick = appHapticAction(onAction = onClick)),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appSectionSpacing()),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Видимость блоков",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Скрывай лишние строки в «Расчёте» и «Выплатах».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Открыть",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
