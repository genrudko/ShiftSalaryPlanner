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
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isGrossMode = amountViewMode == PayrollAmountViewMode.GROSS
    val advanceDisplayValue = if (isGrossMode) payroll.advanceGrossAmount else payroll.netAdvanceAfterDeductions
    val salaryDisplayValue = if (isGrossMode) payroll.salaryGrossAmount else payroll.netSalaryAfterDeductions
    val amountModeLabel = if (isGrossMode) "до НДФЛ" else "на руки"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
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
                        PaymentInfoRow("Средняя (до НДФЛ)", formatMoney(detailedShiftStats.shiftCostAverageGross), bold = detailedShiftStats.shiftCostAverageGross > 0.0)
                        PaymentInfoRow("Средняя (на руки)", formatMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
                        PaymentInfoRow("Дневная", "${formatMoney(detailedShiftStats.dayShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.dayShiftCostAverageNet)}")
                        PaymentInfoRow("Ночная", "${formatMoney(detailedShiftStats.nightShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.nightShiftCostAverageNet)}")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                PayrollSummarySectionTitle("Начисления")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryPanelCard(title = "Основные суммы") {
                    PaymentInfoRow("Часовая ставка", formatMoney(payroll.hourlyRate))
                    PaymentInfoRow("База", formatMoney(payroll.basePay))
                    PaymentInfoRow("Ночные", formatMoney(payroll.nightExtra))
                    PaymentInfoRow("Праздничные/выходные", formatMoney(payroll.holidayExtra))
                    PaymentInfoRow("Отпускные", formatMoney(payroll.vacationPay))
                    PaymentInfoRow("Больничный", formatMoney(payroll.sickPay))
                    CompactSummaryDivider()
                    PaymentInfoRow("Допвыплаты всего", formatMoney(payroll.additionalPaymentsTotal))
                    PaymentInfoRow("В аванс", formatMoney(payroll.additionalPaymentsAdvancePart))
                    PaymentInfoRow("В зарплату", formatMoney(payroll.additionalPaymentsSalaryPart))
                    CompactSummaryDivider()
                    PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
                    PaymentInfoRow("Из неё в аванс", formatMoney(payroll.housingAdvancePart))
                    PaymentInfoRow("Из неё в зарплату", formatMoney(payroll.housingSalaryPart))
                }

                Spacer(modifier = Modifier.height(10.dp))
                SummaryPanelCard(title = "Итог расчёта") {
                    PaymentInfoRow("Облагаемая база", formatMoney(payroll.taxableGrossTotal))
                    PaymentInfoRow("Необлагаемые выплаты", formatMoney(payroll.nonTaxableTotal))
                    PaymentInfoRow("Всего начислено", formatMoney(payroll.grossTotal))
                    PaymentInfoRow("НДФЛ", formatMoney(payroll.ndfl))
                    PaymentInfoRow("Доплата за переработку", formatMoney(annualOvertime.overtimePremiumAmount))
                    if (payroll.taxableIncomeYtdAfterCurrentMonth > 0.0) {
                        PaymentInfoRow("База с начала года до месяца", formatMoney(payroll.taxableIncomeYtdBeforeCurrentMonth))
                        PaymentInfoRow("База с начала года после месяца", formatMoney(payroll.taxableIncomeYtdAfterCurrentMonth))
                    }
                    PaymentInfoRow("На руки за период", formatMoney(payroll.netTotal), bold = true)
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
                    PaymentInfoRow("Аванс ($amountModeLabel)", formatMoney(advanceDisplayValue))
                    PaymentInfoRow("Аванс только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
                    if (periodMode == PayrollPeriodMode.MONTH) {
                        PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
                    }
                    CompactSummaryDivider()
                    PaymentInfoRow("К зарплате ($amountModeLabel)", formatMoney(salaryDisplayValue), bold = true)
                    PaymentInfoRow("Зарплата только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
                    if (periodMode == PayrollPeriodMode.MONTH) {
                        PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
                    }
                }
            } else {
            SummaryCollapsedPill(text = "Часы: ${formatHours(summary.workedHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "Смены: ${detailedShiftStats.workedShiftCount} • Д ${detailedShiftStats.dayShiftCount} • Н ${detailedShiftStats.nightShiftCount}")
                if (detailedShiftStats.workedShiftCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "Средняя смена: ${formatMoney(detailedShiftStats.shiftCostAverageGross)} / ${formatMoney(detailedShiftStats.shiftCostAverageNet)}")
                }
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "Аванс ($amountModeLabel): ${formatMoney(advanceDisplayValue)}")
                if (payroll.vacationPay > 0.0 || payroll.sickPay > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "Отпуск/больничный: ${formatMoney(payroll.vacationPay + payroll.sickPay)}")
                }
                if (annualOvertime.payableOvertimeHours > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "Сверхурочка: ${formatHours(annualOvertime.payableOvertimeHours)} ч")
                }
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "К зарплате ($amountModeLabel): ${formatMoney(salaryDisplayValue)}", emphasize = true)
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
    val containerColor = if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface

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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
private fun SummaryCollapsedPill(text: String, emphasize: Boolean = false) {
    val containerColor = if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    Surface(shape = RoundedCornerShape(999.dp), color = containerColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PayrollInfoPill(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
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

