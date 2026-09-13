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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineBreakdownItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollQuantityUnit
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetSection
import java.time.LocalDate

@Composable
fun SummaryCard(
    periodMode: PayrollPeriodMode,
    periodLabel: String,
    periodStartDate: LocalDate,
    periodEndDate: LocalDate,
    summary: MonthSummary,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    detailedShiftStats: DetailedShiftStats,
    amountViewMode: PayrollAmountViewMode,
    isPerShiftPayment: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isGrossMode = amountViewMode == PayrollAmountViewMode.GROSS
    val advanceDisplayValue = if (isGrossMode) payroll.advanceGrossAmount else payroll.netAdvanceAfterDeductions
    val salaryDisplayValue = if (isGrossMode) payroll.salaryGrossAmount else payroll.netSalaryAfterDeductions
    val amountModeLabel = if (isGrossMode) "до НДФЛ" else "на руки"

    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.PRIMARY,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(appCardRadius()))
                .clickable(onClick = appHapticAction(onAction = onToggle))
                .padding(appCardPadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Сводка за период",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isExpanded) periodLabel else "Краткий итог",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = appHapticAction(onAction = onOpenSettings)) {
                    Text("Настройки")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded) {
                PayrollSummarySectionTitle("Смены и часы")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Рабочих дней: ${summary.workedDays}")
                Spacer(modifier = Modifier.height(6.dp))
                        PayrollInfoPill(text = "Оплачиваемые часы: ${formatHours(summary.workedHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                        PayrollInfoPill(text = "Ночные часы: ${formatHours(summary.nightHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                        PayrollInfoPill(text = "Праздничные/выходные: ${formatHours(payroll.holidayHours)} ч")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Отпуск: ${payroll.vacationDays} дн. • Больничный: ${payroll.sickDays} дн.")
                Spacer(modifier = Modifier.height(6.dp))
                        PayrollInfoPill(text = "Сверхурочка (${annualOvertime.periodLabel}): ${formatHours(annualOvertime.payableOvertimeHours)} ч")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Смены: Д ${detailedShiftStats.dayShiftCount} • Н ${detailedShiftStats.nightShiftCount} • В/П ${detailedShiftStats.weekendHolidayShiftCount}")

                if (detailedShiftStats.workedShiftCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SummaryPanelCard(title = "Стоимость смены") {
                        PaymentInfoRow("Средняя (до НДФЛ)", formatFinanceMoney(detailedShiftStats.shiftCostAverageGross), bold = detailedShiftStats.shiftCostAverageGross > 0.0)
                        PaymentInfoRow("Средняя (на руки)", formatFinanceMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
                        PaymentInfoRow("Дневная", "${formatFinanceMoney(detailedShiftStats.dayShiftCostAverageGross)} / ${formatFinanceMoney(detailedShiftStats.dayShiftCostAverageNet)}")
                        PaymentInfoRow("Ночная", "${formatFinanceMoney(detailedShiftStats.nightShiftCostAverageGross)} / ${formatFinanceMoney(detailedShiftStats.nightShiftCostAverageNet)}")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                PayrollSummarySectionTitle("Начисления")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryPanelCard(title = "Основные суммы") {
                    PaymentInfoRow("Часовая ставка", formatFinanceMoney(payroll.hourlyRate))
                    PaymentInfoRow("База", formatFinanceMoney(payroll.basePay))
                    PaymentInfoRow("Ночные", formatFinanceMoney(payroll.nightExtra))
                    PaymentInfoRow("Праздничные/выходные", formatFinanceMoney(payroll.holidayExtra))
                    PaymentInfoRow("Отпускные", formatFinanceMoney(payroll.vacationPay))
                    PaymentInfoRow("Больничный", formatFinanceMoney(payroll.sickPay))
                    CompactSummaryDivider()
                    PaymentInfoRow("Допвыплаты всего", formatFinanceMoney(payroll.additionalPaymentsTotal))
                    PaymentInfoRow("В аванс", formatFinanceMoney(payroll.additionalPaymentsAdvancePart))
                    PaymentInfoRow("В зарплату", formatFinanceMoney(payroll.additionalPaymentsSalaryPart))
                    CompactSummaryDivider()
                    PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatFinanceMoney(payroll.housingPayment))
                    PaymentInfoRow("Из неё в аванс", formatFinanceMoney(payroll.housingAdvancePart))
                    PaymentInfoRow("Из неё в зарплату", formatFinanceMoney(payroll.housingSalaryPart))
                }

                Spacer(modifier = Modifier.height(10.dp))
                SummaryPanelCard(title = "Итог расчёта") {
                    PaymentInfoRow("Облагаемая база", formatFinanceMoney(payroll.taxableGrossTotal))
                    PaymentInfoRow("Необлагаемые выплаты", formatFinanceMoney(payroll.nonTaxableTotal))
                    PaymentInfoRow("Всего начислено", formatFinanceMoney(payroll.grossTotal))
                    PaymentInfoRow("НДФЛ", formatFinanceMoney(payroll.ndfl))
                    PaymentInfoRow("Доплата за переработку", formatFinanceMoney(annualOvertime.overtimePremiumAmount))
                    if (payroll.taxableIncomeYtdAfterCurrentMonth > 0.0) {
                        PaymentInfoRow("База с начала года до месяца", formatFinanceMoney(payroll.taxableIncomeYtdBeforeCurrentMonth))
                        PaymentInfoRow("База с начала года после месяца", formatFinanceMoney(payroll.taxableIncomeYtdAfterCurrentMonth))
                    }
                    PaymentInfoRow("На руки за период", formatFinanceMoney(payroll.netAfterDeductions), bold = true)
                }

                Spacer(modifier = Modifier.height(10.dp))
                PayrollSummarySectionTitle("Выплаты")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryPanelCard(title = "По датам") {
                    PaymentInfoRow(
                        "Период расчёта",
                        if (periodStartDate == periodEndDate) {
                            formatDate(periodStartDate)
                        } else {
                            "${formatDate(periodStartDate)} — ${formatDate(periodEndDate)}"
                        }
                    )
                    CompactSummaryDivider()
                    if (isPerShiftPayment) {
                        PaymentInfoRow("Режим", "после каждой смены", bold = true)
                        PaymentInfoRow("К выплате за смены", formatFinanceMoney(payroll.netAfterDeductions), bold = true)
                        PaymentInfoRow("Смен оплачено", detailedShiftStats.workedShiftCount.toString())
                    } else {
                        PaymentInfoRow("Аванс ($amountModeLabel)", formatFinanceMoney(advanceDisplayValue))
                        PaymentInfoRow("Аванс только по сменам", formatFinanceMoney(payroll.shiftOnlyAdvanceNetAmount))
                        if (periodMode == PayrollPeriodMode.MONTH) {
                            PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
                        }
                        CompactSummaryDivider()
                        PaymentInfoRow("К зарплате ($amountModeLabel)", formatFinanceMoney(salaryDisplayValue), bold = true)
                        PaymentInfoRow("Зарплата только по сменам", formatFinanceMoney(payroll.shiftOnlySalaryNetAmount))
                        if (periodMode == PayrollPeriodMode.MONTH) {
                            PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
                        }
                    }
                }
            } else {
                PayrollSummaryCollapsedOverview(
                    summary = summary,
                    payroll = payroll,
                    detailedShiftStats = detailedShiftStats,
                    amountModeLabel = amountModeLabel,
                    isPerShiftPayment = isPerShiftPayment,
                    salaryDisplayValue = salaryDisplayValue
                )
            }
        }
    }
}

@Composable
fun PayrollStatTile(
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
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PayrollSummarySectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SummaryPanelCard(title: String, content: @Composable () -> Unit) {
    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun PayrollSummaryCollapsedOverview(
    summary: MonthSummary,
    payroll: PayrollResult,
    detailedShiftStats: DetailedShiftStats,
    amountModeLabel: String,
    isPerShiftPayment: Boolean,
    salaryDisplayValue: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${detailedShiftStats.workedShiftCount} смен",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${formatDouble(summary.workedHours)} ч",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isPerShiftPayment) "К выплате за смены" else "К зарплате ($amountModeLabel)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (isPerShiftPayment) formatFinanceMoney(payroll.netAfterDeductions) else formatFinanceMoney(salaryDisplayValue),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            "Нажми карточку, чтобы раскрыть начисления, удержания и выплаты",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PayrollInfoPill(text: String) {
    EvolutionSurface(
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactSummaryDivider() {
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(6.dp))
}

