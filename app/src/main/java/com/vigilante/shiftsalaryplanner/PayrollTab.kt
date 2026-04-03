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
import com.vigilante.shiftsalaryplanner.payroll.PayrollQuantityUnit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import java.time.YearMonth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetSection

private enum class PayrollViewMode {
    SUMMARY,
    SHEET
}

@Composable
fun PayrollTab(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit,
    summary: MonthSummary,
    payroll: PayrollResult,
    payrollDetailedResult: PayrollDetailedResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    detailedShiftStats: DetailedShiftStats,
    isSummaryExpanded: Boolean,
    onToggleSummary: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by rememberSaveable { mutableStateOf(PayrollViewMode.SUMMARY) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        MonthHeader(
            currentMonth = currentMonth,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onPickMonth = onPickMonth
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayrollStatTile(
                title = "Часы",
                value = formatDouble(summary.workedHours),
                subtitle = "оплачиваемые",
                modifier = Modifier.weight(1f)
            )
            PayrollStatTile(
                title = "Смены",
                value = detailedShiftStats.workedShiftCount.toString(),
                subtitle = "рабочие",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayrollStatTile(
                title = "Аванс",
                value = formatMoney(payroll.advanceAmount),
                subtitle = formatDate(paymentDates.advanceDate),
                modifier = Modifier.weight(1f)
            )
            PayrollStatTile(
                title = "К зарплате",
                value = formatMoney(payroll.salaryPaymentAmount),
                subtitle = formatDate(paymentDates.salaryDate),
                modifier = Modifier.weight(1f),
                emphasize = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PayrollModeSwitcher(
            viewMode = viewMode,
            onModeChange = { viewMode = it }
        )

        Spacer(modifier = Modifier.height(12.dp))
        if (viewMode == PayrollViewMode.SUMMARY) {
            SummaryCard(
                summary = summary,
                payroll = payroll,
                annualOvertime = annualOvertime,
                paymentDates = paymentDates,
                housingPaymentLabel = housingPaymentLabel,
                detailedShiftStats = detailedShiftStats,
                isExpanded = isSummaryExpanded,
                onToggle = onToggleSummary,
                onOpenSettings = onOpenSettings
            )
        } else {
            PayrollSheetCard(
                payrollDetailedResult = payrollDetailedResult,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
fun SummaryCard(
    summary: MonthSummary,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    detailedShiftStats: DetailedShiftStats,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onToggle)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Сводка за месяц",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isExpanded) "Развернутая детализация" else "Краткий итог",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onOpenSettings) {
                    Text("Настройки")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded) {
                PayrollSummarySectionTitle("Смены и часы")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Рабочих дней: ${summary.workedDays}")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Оплачиваемые часы: ${formatDouble(summary.workedHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Ночные часы: ${formatDouble(summary.nightHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Праздничные/выходные: ${formatDouble(payroll.holidayHours)} ч")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Отпуск: ${payroll.vacationDays} дн. • Больничный: ${payroll.sickDays} дн.")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Сверхурочка (${annualOvertime.periodLabel}): ${formatDouble(annualOvertime.payableOvertimeHours)} ч")
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
                    PaymentInfoRow("На руки за месяц", formatMoney(payroll.netTotal), bold = true)
                }

                Spacer(modifier = Modifier.height(10.dp))

                PayrollSummarySectionTitle("Выплаты")
                Spacer(modifier = Modifier.height(6.dp))

                SummaryPanelCard(title = "По датам") {
                    PaymentInfoRow("Аванс", formatMoney(payroll.advanceAmount))
                    PaymentInfoRow("Аванс только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
                    PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
                    CompactSummaryDivider()
                    PaymentInfoRow("К зарплате", formatMoney(payroll.salaryPaymentAmount), bold = true)
                    PaymentInfoRow("Зарплата только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
                    PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
                }
            } else {
                SummaryCollapsedPill(text = "Часы: ${formatDouble(summary.workedHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "Смены: ${detailedShiftStats.workedShiftCount} • Д ${detailedShiftStats.dayShiftCount} • Н ${detailedShiftStats.nightShiftCount}")
                if (detailedShiftStats.workedShiftCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "Средняя смена: ${formatMoney(detailedShiftStats.shiftCostAverageGross)} / ${formatMoney(detailedShiftStats.shiftCostAverageNet)}")
                }
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "Аванс: ${formatMoney(payroll.advanceAmount)}")
                if (payroll.vacationPay > 0.0 || payroll.sickPay > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "Отпуск/больничный: ${formatMoney(payroll.vacationPay + payroll.sickPay)}")
                }
                if (annualOvertime.payableOvertimeHours > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "Сверхурочка: ${formatDouble(annualOvertime.payableOvertimeHours)} ч")
                }
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(
                    text = "К зарплате: ${formatMoney(payroll.salaryPaymentAmount)}",
                    emphasize = true
                )
            }
        }
    }
}

@Composable
private fun PayrollStatTile(
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
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
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
private fun PayrollSummarySectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SummaryPanelCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
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
private fun SummaryCollapsedPill(
    text: String,
    emphasize: Boolean = false
) {
    val containerColor = if (emphasize) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
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
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
    @Composable
    private fun PayrollModeSwitcher(
        viewMode: PayrollViewMode,
        onModeChange: (PayrollViewMode) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayrollModeChip(
                text = "Сводка",
                selected = viewMode == PayrollViewMode.SUMMARY,
                onClick = { onModeChange(PayrollViewMode.SUMMARY) },
                modifier = Modifier.weight(1f)
            )
            PayrollModeChip(
                text = "Лист",
                selected = viewMode == PayrollViewMode.SHEET,
                onClick = { onModeChange(PayrollViewMode.SHEET) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun PayrollModeChip(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val containerColor = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        }

        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = BorderStroke(1.dp, appPanelBorderColor())
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    @Composable
    private fun PayrollSheetCard(
        payrollDetailedResult: PayrollDetailedResult,
        onOpenSettings: () -> Unit
    ) {
        val items = payrollDetailedResult.lineItems

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, appPanelBorderColor())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Расчётный лист",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Черновой подробный вывод",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = onOpenSettings) {
                        Text("Настройки")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PayrollSheetSectionBlock(
                    title = "Общая информация",
                    items = items.filter { it.section == PayrollSheetSection.HEADER }
                )
                PayrollSheetSectionBlock(
                    title = "Начисления",
                    items = items.filter { it.section == PayrollSheetSection.ACCRUAL }
                )
                PayrollSheetSectionBlock(
                    title = "Удержания",
                    items = items.filter { it.section == PayrollSheetSection.DEDUCTION }
                )
                PayrollSheetSectionBlock(
                    title = "Ранее выплачено",
                    items = items.filter { it.section == PayrollSheetSection.PRIOR_PAYMENT }
                )
                PayrollSheetSectionBlock(
                    title = "К выплате",
                    items = items.filter { it.section == PayrollSheetSection.PAYOUT }
                )
                PayrollSheetSectionBlock(
                    title = "Справочно",
                    items = items.filter { it.section == PayrollSheetSection.REFERENCE }
                )
            }
        }
    }

    @Composable
    private fun PayrollSheetSectionBlock(
        title: String,
        items: List<PayrollLineItem>
    ) {
        if (items.isEmpty()) return

        Spacer(modifier = Modifier.height(6.dp))
        PayrollSummarySectionTitle(title)
        Spacer(modifier = Modifier.height(6.dp))

        SummaryPanelCard(title = title) {
            items.forEachIndexed { index, item ->
                PayrollSheetRow(item)
                if (index != items.lastIndex) {
                    CompactSummaryDivider()
                }
            }
        }
    }

@Composable
private fun PayrollSheetRow(item: PayrollLineItem) {
    val quantityText = when (item.unit) {
        PayrollQuantityUnit.HOURS -> item.quantity?.let { "${formatDouble(it)} ч" }
        PayrollQuantityUnit.DAYS -> item.quantity?.let { "${formatDouble(it)} дн" }
        PayrollQuantityUnit.MONTHS -> item.quantity?.let { "${formatDouble(it)} мес" }
        PayrollQuantityUnit.TIMES -> item.quantity?.let { "${formatDouble(it)} раз" }
        PayrollQuantityUnit.NONE -> null
    }

    val title = item.title

    val valueText = when (item.unit) {
        PayrollQuantityUnit.HOURS -> "${formatDouble(item.amount)} ч"
        PayrollQuantityUnit.DAYS -> "${formatDouble(item.amount)} дн"
        PayrollQuantityUnit.MONTHS -> "${formatDouble(item.amount)} мес"
        PayrollQuantityUnit.TIMES -> "${formatDouble(item.amount)} раз"
        PayrollQuantityUnit.NONE -> formatMoney(item.amount)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PaymentInfoRow(title, valueText, bold = true)

        if (!item.periodLabel.isNullOrBlank()) {
            PaymentInfoRow("Период", item.periodLabel)
        }

        if (!quantityText.isNullOrBlank()) {
            PaymentInfoRow("Количество", quantityText)
        }

        if (!item.note.isNullOrBlank()) {
            PaymentInfoRow("Примечание", item.note)
        }
    }
}