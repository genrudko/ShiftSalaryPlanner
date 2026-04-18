package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdvanceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import java.time.LocalDate

@Composable
fun CurrentParametersScreen(
    payrollSettings: PayrollSettings,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FixedScreenHeader(
                title = "Текущие параметры",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                InfoCard(title = "Оплата") {
                    PaymentInfoRow("Режим оплаты", payModeLabel(payrollSettings.payMode))
                    PaymentInfoRow("Режим надбавки", extraSalaryModeLabel(payrollSettings.extraSalaryMode))
                    PaymentInfoRow("Оклад", formatMoney(payrollSettings.baseSalary))
                    PaymentInfoRow("Надбавка", formatMoney(payrollSettings.extraSalary))
                    PaymentInfoRow(displayHousingPaymentLabel(payrollSettings.housingPaymentLabel), formatMoney(payrollSettings.housingPayment))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Расчёт") {
                    PaymentInfoRow("Режим нормы", normModeLabel(payrollSettings.normMode))
                    PaymentInfoRow("Норма часов", formatDouble(payrollSettings.monthlyNormHours))
                    if (runCatching { NormMode.valueOf(payrollSettings.normMode) }.getOrElse { NormMode.MANUAL } != NormMode.MANUAL) {
                        PaymentInfoRow("Часов в рабочем дне", formatDouble(payrollSettings.workdayHours))
                    }
                    if (runCatching { NormMode.valueOf(payrollSettings.normMode) }.getOrElse { NormMode.MANUAL } == NormMode.AVERAGE_ANNUAL) {
                        PaymentInfoRow("Источник среднегодовой нормы", annualNormSourceModeLabel(payrollSettings.annualNormSourceMode))
                        if (runCatching { AnnualNormSourceMode.valueOf(payrollSettings.annualNormSourceMode) }.getOrElse { AnnualNormSourceMode.WORKDAY_HOURS } == AnnualNormSourceMode.YEAR_TOTAL_HOURS) {
                            PaymentInfoRow("Часов в году", formatDouble(payrollSettings.annualNormHours))
                        }
                    }
                    PaymentInfoRow(
                        "Сокращённый день",
                        if (payrollSettings.applyShortDayReduction) "Учитывается" else "Не учитывается"
                    )
                    PaymentInfoRow(
                        "Сверхурочка",
                        if (payrollSettings.overtimeEnabled) "Включена" else "Отключена"
                    )
                    PaymentInfoRow("Период сверхурочки", overtimePeriodLabel(payrollSettings.overtimePeriod))
                    PaymentInfoRow(
                        "Искл. выходные / праздничные",
                        if (payrollSettings.excludeWeekendHolidayFromOvertime) "Да" else "Нет"
                    )
                    PaymentInfoRow(
                        "Искл. РВД двойная",
                        if (payrollSettings.excludeRvdDoublePayFromOvertime) "Да" else "Нет"
                    )
                    PaymentInfoRow(
                        "Искл. РВД с отгулом",
                        if (payrollSettings.excludeRvdSingleWithDayOffFromOvertime) "Да" else "Нет"
                    )
                    PaymentInfoRow(
                        "Ночные",
                        "${formatDouble(ratioToPercentUiValue(payrollSettings.nightPercent, coefficientUpperBound = 3.0))}%"
                    )
                    PaymentInfoRow("База для ночных", nightHoursBaseModeLabel(payrollSettings.nightHoursBaseMode))
                    PaymentInfoRow("РВД/РВН", payrollSettings.holidayRateMultiplier.toPlainString())
                    PaymentInfoRow(
                        "НДФЛ",
                        "${formatDouble(ratioToPercentUiValue(payrollSettings.ndflPercent, coefficientUpperBound = 1.0))}%"
                    )
                    PaymentInfoRow("Начисления за 12 мес. (отпуск)", formatMoney(payrollSettings.vacationAccruals12Months))
                    PaymentInfoRow("Отпуск (ср. день)", formatMoney(payrollSettings.vacationAverageDaily))
                    PaymentInfoRow("Больничный: доход за ${LocalDate.now().year - 2}", formatMoney(payrollSettings.sickIncomeYear1))
                    PaymentInfoRow("Больничный: доход за ${LocalDate.now().year - 1}", formatMoney(payrollSettings.sickIncomeYear2))
                    PaymentInfoRow("Больничный: лимит ${LocalDate.now().year - 2}", formatMoney(payrollSettings.sickLimitYear1))
                    PaymentInfoRow("Больничный: лимит ${LocalDate.now().year - 1}", formatMoney(payrollSettings.sickLimitYear2))
                    PaymentInfoRow("Больничный: дней периода", payrollSettings.sickCalculationPeriodDays.toString())
                    PaymentInfoRow("Больничный: исключаемые дни", payrollSettings.sickExcludedDays.toString())
                    PaymentInfoRow("Больничный (ср. день)", formatMoney(payrollSettings.sickAverageDaily))
                    PaymentInfoRow("Больничный коэффициент", payrollSettings.sickPayPercent.toPlainString())
                    PaymentInfoRow("Макс. больничный в день", formatMoney(payrollSettings.sickMaxDailyAmount))
                    PaymentInfoRow("Прогрессивный НДФЛ", if (payrollSettings.progressiveNdflEnabled) "Включён" else "Выключен")
                    if (payrollSettings.progressiveNdflEnabled) {
                        PaymentInfoRow("Доход с начала года", formatMoney(payrollSettings.taxableIncomeYtdBeforeCurrentMonth))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Выплаты") {
                    PaymentInfoRow("Режим аванса", advanceModeLabel(payrollSettings.advanceMode))
                    if (runCatching { AdvanceMode.valueOf(payrollSettings.advanceMode) }.getOrElse { AdvanceMode.ACTUAL_EARNINGS } == AdvanceMode.FIXED_PERCENT) {
                        PaymentInfoRow("Процент аванса", formatDouble(payrollSettings.advancePercent) + "%")
                    }
                    PaymentInfoRow("День аванса", payrollSettings.advanceDay.toString())
                    PaymentInfoRow("День зарплаты", payrollSettings.salaryDay.toString())
                    PaymentInfoRow(
                        "Сдвиг выплат",
                        if (payrollSettings.movePaymentsToPreviousWorkday) "На предыдущий рабочий день" else "Без сдвига"
                    )
                    PaymentInfoRow(
                        "${displayHousingPaymentLabel(payrollSettings.housingPaymentLabel)} в аванс",
                        if (payrollSettings.housingPaymentWithAdvance) "Да" else "Нет"
                    )
                    PaymentInfoRow(
                        "${displayHousingPaymentLabel(payrollSettings.housingPaymentLabel)} облагается НДФЛ",
                        if (payrollSettings.housingPaymentTaxable) "Да" else "Нет"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
