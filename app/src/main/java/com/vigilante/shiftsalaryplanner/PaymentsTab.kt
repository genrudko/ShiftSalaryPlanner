package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import java.time.YearMonth

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

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Подробный отчёт") {
            Text(
                text = "Открой подробную расшифровку начислений за месяц и экспортируй CSV.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onOpenMonthlyReport) {
                    Text("Открыть отчёт")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Выплаты за месяц") {
            PaymentInfoRow("Аванс", formatMoney(payroll.advanceAmount))
            PaymentInfoRow("Аванс только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
            PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
            PaymentInfoRow("К зарплате", formatMoney(payroll.salaryPaymentAmount))
            PaymentInfoRow("Зарплата только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
            PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Статистика смен") {
            PaymentInfoRow("Всего отмеченных дней", detailedShiftStats.totalAssignedDays.toString())
            PaymentInfoRow("Рабочих смен", detailedShiftStats.workedShiftCount.toString(), bold = detailedShiftStats.workedShiftCount > 0)
            PaymentInfoRow("Дневных", detailedShiftStats.dayShiftCount.toString())
            PaymentInfoRow("Ночных", detailedShiftStats.nightShiftCount.toString())
            PaymentInfoRow("Выходных/праздничных", detailedShiftStats.weekendHolidayShiftCount.toString())
            PaymentInfoRow("Восьмичасовой раб.день", detailedShiftStats.eightHourShiftCount.toString())
            PaymentInfoRow("Отпуск", detailedShiftStats.vacationShiftCount.toString())
            PaymentInfoRow("Больничный", detailedShiftStats.sickShiftCount.toString())
            PaymentInfoRow("Смен в первой половине", detailedShiftStats.firstHalfWorkedShifts.toString())
            PaymentInfoRow("Смен во второй половине", detailedShiftStats.secondHalfWorkedShifts.toString())
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Стоимость одной смены") {
            PaymentInfoRow("База для расчёта", formatMoney(detailedShiftStats.shiftCostBaseTotal))
            PaymentInfoRow("Учтено доплат и премий", formatMoney(detailedShiftStats.shiftCostIncludedPayments))
            PaymentInfoRow("Рабочих смен", detailedShiftStats.workedShiftCount.toString())
            PaymentInfoRow("Средняя стоимость смены (до НДФЛ)", formatMoney(detailedShiftStats.shiftCostAverageGross), bold = detailedShiftStats.shiftCostAverageGross > 0.0)
            PaymentInfoRow("Средняя стоимость смены (на руки)", formatMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
            PaymentInfoRow("Дневная смена (до НДФЛ)", formatMoney(detailedShiftStats.dayShiftCostAverageGross), bold = detailedShiftStats.dayShiftCostAverageGross > 0.0)
            PaymentInfoRow("Дневная смена (на руки)", formatMoney(detailedShiftStats.dayShiftCostAverageNet), bold = detailedShiftStats.dayShiftCostAverageNet > 0.0)
            PaymentInfoRow("Ночная смена (до НДФЛ)", formatMoney(detailedShiftStats.nightShiftCostAverageGross), bold = detailedShiftStats.nightShiftCostAverageGross > 0.0)
            PaymentInfoRow("Ночная смена (на руки)", formatMoney(detailedShiftStats.nightShiftCostAverageNet), bold = detailedShiftStats.nightShiftCostAverageNet > 0.0)
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Основные доплаты") {
            PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
            PaymentInfoRow("В аванс", formatMoney(payroll.housingAdvancePart))
            PaymentInfoRow("В зарплату", formatMoney(payroll.housingSalaryPart))
            PaymentInfoRow(
                "Налогообложение",
                if (payroll.housingPaymentTaxable) "Облагается НДФЛ" else "Не облагается"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Доплаты и премии месяца") {
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
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Отпуск и больничный") {
            PaymentInfoRow("Дней отпуска", payroll.vacationDays.toString())
            PaymentInfoRow("Отпускные", formatMoney(payroll.vacationPay))
            PaymentInfoRow("Дней больничного", payroll.sickDays.toString())
            PaymentInfoRow("Больничный", formatMoney(payroll.sickPay))
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Сверхурочка: ${annualOvertime.periodLabel}") {
            PaymentInfoRow("Статус", if (annualOvertime.enabled) "Включена" else "Отключена")
            PaymentInfoRow("Норма периода", formatDouble(annualOvertime.annualNormHours))
            PaymentInfoRow("Отработано", formatDouble(annualOvertime.workedHours))
            PaymentInfoRow("Переработка до исключений", formatDouble(annualOvertime.rawOvertimeHours))
            PaymentInfoRow("Исключено из переработки", formatDouble(annualOvertime.holidayExcludedHours))
            PaymentInfoRow("К оплате как сверхурочные", formatDouble(annualOvertime.payableOvertimeHours), bold = annualOvertime.payableOvertimeHours > 0.0)
            PaymentInfoRow("Первые 2 часа", formatDouble(annualOvertime.firstTwoHours))
            PaymentInfoRow("Остальные часы", formatDouble(annualOvertime.remainingHours))
            PaymentInfoRow("Расчётная часовая ставка", formatMoney(annualOvertime.hourlyRate))
            PaymentInfoRow("Доплата за переработку", formatMoney(annualOvertime.overtimePremiumAmount), bold = annualOvertime.overtimePremiumAmount > 0.0)
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Настроенные начисления") {
            if (activeConfiguredPayments.isEmpty()) {
                Text("Нет активных доплат и премий.")
            } else {
                activeConfiguredPayments.forEachIndexed { index, payment ->
                    PaymentInfoRow(payment.name.ifBlank { "Без названия" }, additionalPaymentTypeLabel(payment.type), bold = true)
                    PaymentInfoRow("Параметры", additionalPaymentDetailsLabel(payment))
                    PaymentInfoRow("Начисление", paymentDistributionLabel(payment.distribution))
                    if (index != activeConfiguredPayments.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(title = "Итоги начисления") {
            PaymentInfoRow("Допвыплаты всего", formatMoney(payroll.additionalPaymentsTotal))
            PaymentInfoRow("Из них в аванс", formatMoney(payroll.additionalPaymentsAdvancePart))
            PaymentInfoRow("Из них в зарплату", formatMoney(payroll.additionalPaymentsSalaryPart))
            PaymentInfoRow("Облагаемая база", formatMoney(payroll.taxableGrossTotal))
            PaymentInfoRow("Необлагаемые выплаты", formatMoney(payroll.nonTaxableTotal))
            PaymentInfoRow("Всего начислено", formatMoney(payroll.grossTotal))
            PaymentInfoRow("НДФЛ", formatMoney(payroll.ndfl))
            PaymentInfoRow("На руки", formatMoney(payroll.netTotal), bold = true)
        }
    }
}
