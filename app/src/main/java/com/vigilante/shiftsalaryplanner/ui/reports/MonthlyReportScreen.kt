package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import java.time.YearMonth

@Composable
fun MonthlyReportScreen(
    currentMonth: YearMonth,
    payrollSettings: PayrollSettings,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    additionalPayments: List<AdditionalPayment>,
    resolvedAdditionalPaymentsBreakdown: List<ResolvedAdditionalPaymentBreakdown>,
    detailedShiftStats: DetailedShiftStats,
    onBack: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    val activePayments = remember(additionalPayments) { additionalPayments.filter { it.active } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(
                title = "Отчёт за ${formatMonthYearTitle(currentMonth)}",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    Button(
                        onClick = appHapticAction(AppHapticKind.CONFIRM, onExportCsv),
                        modifier = Modifier
                            .weight(1f)
                            .appLargeButtonSizing()
                    ) {
                        Text("Экспорт CSV")
                    }
                    Button(
                        onClick = appHapticAction(AppHapticKind.CONFIRM, onExportPdf),
                        modifier = Modifier
                            .weight(1f)
                            .appLargeButtonSizing()
                    ) {
                        Text("Экспорт PDF")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Период и режимы") {
                    PaymentInfoRow("Период", formatMonthYearTitle(currentMonth))
                    PaymentInfoRow("Режим оплаты", payModeLabel(payrollSettings.payMode))
                    PaymentInfoRow("Режим надбавки", extraSalaryModeLabel(payrollSettings.extraSalaryMode))
                    PaymentInfoRow("Режим аванса", advanceModeLabel(payrollSettings.advanceMode))
                    PaymentInfoRow("Режим нормы", normModeLabel(payrollSettings.normMode))
                    PaymentInfoRow("Норма часов", formatDouble(payrollSettings.monthlyNormHours))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Отработанное время") {
                    PaymentInfoRow("Отработано часов", formatDouble(payroll.workedHours), bold = payroll.workedHours > 0.0)
                    PaymentInfoRow("Ночных часов", formatDouble(payroll.nightHours))
                    PaymentInfoRow("Праздничных/выходных часов", formatDouble(payroll.holidayHours))
                    PaymentInfoRow("Часовая ставка", formatMoney(payroll.hourlyRate))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Статистика смен") {
                    PaymentInfoRow("Всего отмеченных дней", detailedShiftStats.totalAssignedDays.toString())
                    PaymentInfoRow("Рабочих смен", detailedShiftStats.workedShiftCount.toString())
                    PaymentInfoRow("Дневных", detailedShiftStats.dayShiftCount.toString())
                    PaymentInfoRow("Ночных", detailedShiftStats.nightShiftCount.toString())
                    PaymentInfoRow("Выходных/праздничных", detailedShiftStats.weekendHolidayShiftCount.toString())
                    PaymentInfoRow("Восьмичасовой раб.день", detailedShiftStats.eightHourShiftCount.toString())
                    PaymentInfoRow("Отпуск", detailedShiftStats.vacationShiftCount.toString())
                    PaymentInfoRow("Больничный", detailedShiftStats.sickShiftCount.toString())
                    PaymentInfoRow("Смен в первой половине", detailedShiftStats.firstHalfWorkedShifts.toString())
                    PaymentInfoRow("Смен во 2-й половине", detailedShiftStats.secondHalfWorkedShifts.toString())
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Начисления") {
                    PaymentInfoRow("Базовая оплата", formatMoney(payroll.basePay))
                    PaymentInfoRow("Ночные", formatMoney(payroll.nightExtra))
                    PaymentInfoRow("РВД/праздничные", formatMoney(payroll.holidayExtra))
                    PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
                    PaymentInfoRow("Допвыплаты всего", formatMoney(payroll.additionalPaymentsTotal))
                    PaymentInfoRow("Всего начислено", formatMoney(payroll.grossTotal), bold = true)
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Стоимость смены") {
                    PaymentInfoRow("База для расчёта", formatMoney(detailedShiftStats.shiftCostBaseTotal))
                    PaymentInfoRow("Учтено доплат и премий", formatMoney(detailedShiftStats.shiftCostIncludedPayments))
                    PaymentInfoRow("Средняя стоимость смены (до НДФЛ)", formatMoney(detailedShiftStats.shiftCostAverageGross), bold = true)
                    PaymentInfoRow("Средняя стоимость смены (на руки)", formatMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
                    PaymentInfoRow("Дневная смена (до НДФЛ)", formatMoney(detailedShiftStats.dayShiftCostAverageGross), bold = detailedShiftStats.dayShiftCostAverageGross > 0.0)
                    PaymentInfoRow("Дневная смена (на руки)", formatMoney(detailedShiftStats.dayShiftCostAverageNet), bold = detailedShiftStats.dayShiftCostAverageNet > 0.0)
                    PaymentInfoRow("Ночная смена (до НДФЛ)", formatMoney(detailedShiftStats.nightShiftCostAverageGross), bold = detailedShiftStats.nightShiftCostAverageGross > 0.0)
                    PaymentInfoRow("Ночная смена (на руки)", formatMoney(detailedShiftStats.nightShiftCostAverageNet), bold = detailedShiftStats.nightShiftCostAverageNet > 0.0)
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Отпуск и больничный") {
                    PaymentInfoRow("Дней отпуска", payroll.vacationDays.toString())
                    PaymentInfoRow("Отпускные", formatMoney(payroll.vacationPay))
                    PaymentInfoRow("Дней больничного", payroll.sickDays.toString())
                    PaymentInfoRow("Больничный", formatMoney(payroll.sickPay))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Налоги и выплаты") {
                    PaymentInfoRow("Облагаемая база", formatMoney(payroll.taxableGrossTotal))
                    PaymentInfoRow("Необлагаемые выплаты", formatMoney(payroll.nonTaxableTotal))
                    PaymentInfoRow("НДФЛ", formatMoney(payroll.ndfl))
                    PaymentInfoRow("На руки", formatMoney(payroll.netTotal), bold = true)
                    PaymentInfoRow("Аванс", formatMoney(payroll.netAdvanceAfterDeductions))
                    PaymentInfoRow("Аванс только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
                    PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
                    PaymentInfoRow("К зарплате", formatMoney(payroll.netSalaryAfterDeductions))
                    PaymentInfoRow("Зарплата только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
                    PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Сверхурочка") {
                    PaymentInfoRow("Период", annualOvertime.periodLabel)
                    PaymentInfoRow("Статус", if (annualOvertime.enabled) "Включена" else "Отключена")
                    PaymentInfoRow("Норма периода", formatDouble(annualOvertime.annualNormHours))
                    PaymentInfoRow("Отработано", formatDouble(annualOvertime.workedHours))
                    PaymentInfoRow("Переработка до исключений", formatDouble(annualOvertime.rawOvertimeHours))
                    PaymentInfoRow("Исключено из переработки", formatDouble(annualOvertime.holidayExcludedHours))
                    PaymentInfoRow("К оплате как сверхурочные", formatDouble(annualOvertime.payableOvertimeHours), bold = annualOvertime.payableOvertimeHours > 0.0)
                    PaymentInfoRow("Доплата за переработку", formatMoney(annualOvertime.overtimePremiumAmount), bold = annualOvertime.overtimePremiumAmount > 0.0)
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Сработавшие доплаты и премии") {
                    if (resolvedAdditionalPaymentsBreakdown.isEmpty()) {
                        Text("В этом месяце дополнительных начислений нет.")
                    } else {
                        resolvedAdditionalPaymentsBreakdown.forEachIndexed { index, item ->
                            PaymentInfoRow(item.payment.displayName, additionalPaymentTypeLabel(item.payment.sourceTypeName), bold = true)
                            PaymentInfoRow("До НДФЛ", formatMoney(item.grossAmount), bold = item.grossAmount != 0.0)
                            PaymentInfoRow("НДФЛ", formatMoney(item.ndflAmount))
                            PaymentInfoRow("На руки", formatMoney(item.netAmount), bold = item.netAmount != 0.0)
                            PaymentInfoRow("Параметры", buildString {
                                append(if (item.payment.withAdvance) "в аванс" else "в зарплату")
                                append(" • ")
                                append(if (item.payment.taxable) "облагается" else "не облагается")
                            })
                            if (index != resolvedAdditionalPaymentsBreakdown.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                if (activePayments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    InfoCard(title = "Активные настройки начислений") {
                        activePayments.forEachIndexed { index, payment ->
                            PaymentInfoRow(payment.name.ifBlank { "Без названия" }, additionalPaymentTypeLabel(payment.type), bold = true)
                            PaymentInfoRow("Параметры", additionalPaymentDetailsLabel(payment))
                            PaymentInfoRow("Куда начислять", paymentDistributionLabel(payment.distribution))
                            if (index != activePayments.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
@Composable
fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}
@Composable
fun AdditionalPaymentsCard(
    payments: List<AdditionalPayment>,
    onAddPayment: () -> Unit,
    onEditPayment: (AdditionalPayment) -> Unit,
    onDeletePayment: (AdditionalPayment) -> Unit
) {
    InfoCard(title = "Доплаты, выплаты и премии") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onAddPayment,
                modifier = Modifier.appLargeButtonSizing(base = 44.dp)
            ) {
                Text("Добавить")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (payments.isEmpty()) {
            Text("Пока нет ни одного начисления.")
        } else {
            payments.forEach { payment ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onEditPayment(payment) }
                        .padding(12.dp)
                ) {
                    Text(
                        text = payment.name.ifBlank { "Без названия" },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(additionalPaymentTypeLabel(payment.type))
                    Text(additionalPaymentDetailsLabel(payment))
                    Text(
                        buildString {
                            append(paymentDistributionLabel(payment.distribution))
                            append(" • ")
                            append(if (payment.taxable) "облагается НДФЛ" else "не облагается")
                            append(" • ")
                            append(if (payment.includeInShiftCost) "входит в стоимость смены" else "не входит в стоимость смены")
                            append(" • ")
                            append(if (payment.active) "активна" else "неактивна")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onEditPayment(payment) }) {
                            Text("Изменить")
                        }
                        TextButton(onClick = { onDeletePayment(payment) }) {
                            Text("Удалить")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
