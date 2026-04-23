package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
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
    modifier: Modifier = Modifier
) {
    val activeConfiguredPayments = remember(additionalPayments) { additionalPayments.filter { it.active } }

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
            onPickMonth = onPickMonth
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
                        PaymentsStatTile(
                            title = "Аванс",
                            value = formatMoney(payroll.netAdvanceAfterDeductions),
                            subtitle = formatDate(paymentDates.advanceDate),
                            modifier = Modifier.weight(1f)
                        )
                        PaymentsStatTile(
                            title = "К зарплате",
                            value = formatMoney(payroll.netSalaryAfterDeductions),
                            subtitle = formatDate(paymentDates.salaryDate),
                            modifier = Modifier.weight(1f)
                        )
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
                            value = formatMoney(payroll.netTotal),
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
                        PaymentInfoRow("Аванс", formatMoney(payroll.netAdvanceAfterDeductions), bold = payroll.netAdvanceAfterDeductions > 0.0)
                        PaymentInfoRow("Только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
                        PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
                        CompactDivider()
                        PaymentInfoRow("К зарплате", formatMoney(payroll.netSalaryAfterDeductions), bold = payroll.netSalaryAfterDeductions > 0.0)
                        PaymentInfoRow("Только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
                        PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
                    }
                }

                if (visibilitySettings.showPaymentsPayoutCard && visibilitySettings.showPaymentsTotalsCard) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (visibilitySettings.showPaymentsTotalsCard) {
                    PaymentsPanelCard(title = "Итоги начисления") {
                        PaymentInfoRow("Допвыплаты всего", formatMoney(payroll.additionalPaymentsTotal))
                        PaymentInfoRow("В аванс", formatMoney(payroll.additionalPaymentsAdvancePart))
                        PaymentInfoRow("В зарплату", formatMoney(payroll.additionalPaymentsSalaryPart))
                        CompactDivider()
                        PaymentInfoRow("Облагаемая база", formatMoney(payroll.taxableGrossTotal))
                        PaymentInfoRow("Необлагаемые выплаты", formatMoney(payroll.nonTaxableTotal))
                        PaymentInfoRow("Всего начислено", formatMoney(payroll.grossTotal))
                        PaymentInfoRow("НДФЛ", formatMoney(payroll.ndfl))
                        PaymentInfoRow("На руки", formatMoney(payroll.netTotal), bold = true)
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
                        PaymentInfoRow("База расчёта", formatMoney(detailedShiftStats.shiftCostBaseTotal))
                        PaymentInfoRow("Учтено доплат", formatMoney(detailedShiftStats.shiftCostIncludedPayments))
                        PaymentInfoRow("Рабочих смен", detailedShiftStats.workedShiftCount.toString())
                        CompactDivider()
                        PaymentInfoRow("Средняя (до НДФЛ)", formatMoney(detailedShiftStats.shiftCostAverageGross), bold = detailedShiftStats.shiftCostAverageGross > 0.0)
                        PaymentInfoRow("Средняя (на руки)", formatMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
                        PaymentInfoRow("Дневная (на руки)", formatMoney(detailedShiftStats.dayShiftCostAverageNet), bold = detailedShiftStats.dayShiftCostAverageNet > 0.0)
                        PaymentInfoRow("Ночная (на руки)", formatMoney(detailedShiftStats.nightShiftCostAverageNet), bold = detailedShiftStats.nightShiftCostAverageNet > 0.0)
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
                        PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
                        PaymentInfoRow("В аванс", formatMoney(payroll.housingAdvancePart))
                        PaymentInfoRow("В зарплату", formatMoney(payroll.housingSalaryPart))
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
                                PaymentInfoRow("До НДФЛ", formatMoney(item.grossAmount), bold = item.grossAmount != 0.0)
                                PaymentInfoRow("НДФЛ", formatMoney(item.ndflAmount))
                                PaymentInfoRow("На руки", formatMoney(item.netAmount), bold = item.netAmount != 0.0)
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
                        PaymentInfoRow("Отпускные", formatMoney(payroll.vacationPay))
                        PaymentInfoRow("Дней больничного", payroll.sickDays.toString())
                        PaymentInfoRow("Больничный", formatMoney(payroll.sickPay))
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
                        PaymentInfoRow("Часовая ставка", formatMoney(annualOvertime.hourlyRate))
                        PaymentInfoRow("Доплата", formatMoney(annualOvertime.overtimePremiumAmount), bold = annualOvertime.overtimePremiumAmount > 0.0)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(24.dp)))
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
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
    val containerColor = if (emphasize) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCardRadius()),
        color = containerColor,
        border = BorderStroke(1.dp, appPanelBorderColor())
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
    Surface(
        modifier = modifier
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
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
    Surface(
        modifier = modifier
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
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
