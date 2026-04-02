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
    modifier: Modifier = Modifier
) {
    val activeConfiguredPayments = remember(additionalPayments) { additionalPayments.filter { it.active } }

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

        PaymentsReportTile(onClick = onOpenMonthlyReport)

        Spacer(modifier = Modifier.height(12.dp))

        PaymentsSectionTitle("Главное за месяц")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaymentsStatTile(
                title = "Аванс",
                value = formatMoney(payroll.advanceAmount),
                subtitle = formatDate(paymentDates.advanceDate),
                modifier = Modifier.weight(1f)
            )
            PaymentsStatTile(
                title = "К зарплате",
                value = formatMoney(payroll.salaryPaymentAmount),
                subtitle = formatDate(paymentDates.salaryDate),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        Spacer(modifier = Modifier.height(12.dp))

        PaymentsSectionTitle("Выплаты и итог")
        Spacer(modifier = Modifier.height(8.dp))

        PaymentsPanelCard(title = "Выплаты") {
            PaymentInfoRow("Аванс", formatMoney(payroll.advanceAmount), bold = payroll.advanceAmount > 0.0)
            PaymentInfoRow("Только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
            PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
            CompactDivider()
            PaymentInfoRow("К зарплате", formatMoney(payroll.salaryPaymentAmount), bold = payroll.salaryPaymentAmount > 0.0)
            PaymentInfoRow("Только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
            PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        PaymentsSectionTitle("Смены и стоимость")
        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        PaymentsSectionTitle("Доплаты и премии")
        Spacer(modifier = Modifier.height(8.dp))

        PaymentsPanelCard(title = "Основные доплаты") {
            PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
            PaymentInfoRow("В аванс", formatMoney(payroll.housingAdvancePart))
            PaymentInfoRow("В зарплату", formatMoney(payroll.housingSalaryPart))
            PaymentInfoRow(
                "Налогообложение",
                if (payroll.housingPaymentTaxable) "Облагается НДФЛ" else "Не облагается"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        PaymentsSectionTitle("Отсутствия и переработка")
        Spacer(modifier = Modifier.height(8.dp))

        PaymentsPanelCard(title = "Отпуск и больничный") {
            PaymentInfoRow("Дней отпуска", payroll.vacationDays.toString())
            PaymentInfoRow("Отпускные", formatMoney(payroll.vacationPay))
            PaymentInfoRow("Дней больничного", payroll.sickDays.toString())
            PaymentInfoRow("Больничный", formatMoney(payroll.sickPay))
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
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
private fun PaymentsReportTile(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                    text = "Расшифровка начислений за месяц и экспорт CSV",
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
