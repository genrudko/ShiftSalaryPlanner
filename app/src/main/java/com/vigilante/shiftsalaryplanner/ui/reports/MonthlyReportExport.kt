package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import java.time.YearMonth

fun buildMonthlyReportCsv(
    currentMonth: YearMonth,
    payrollSettings: PayrollSettings,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    additionalPayments: List<AdditionalPayment>,
    resolvedAdditionalPaymentsBreakdown: List<ResolvedAdditionalPaymentBreakdown>,
    detailedShiftStats: DetailedShiftStats
): String {
    val rows = mutableListOf<List<String>>()
    val activePayments = additionalPayments.filter { it.active }

    rows += listOf("Показатель", "Значение")
    rows += listOf("Период", formatMonthYearTitle(currentMonth))
    rows += listOf("Режим оплаты", payModeLabel(payrollSettings.payMode))
    rows += listOf("Режим надбавки", extraSalaryModeLabel(payrollSettings.extraSalaryMode))
    rows += listOf("Режим аванса", advanceModeLabel(payrollSettings.advanceMode))
    rows += listOf("Режим нормы", normModeLabel(payrollSettings.normMode))
    rows += listOf("Норма часов", formatDouble(payrollSettings.monthlyNormHours))
    rows += listOf("Отработано часов", formatDouble(payroll.workedHours))
    rows += listOf("Ночных часов", formatDouble(payroll.nightHours))
    rows += listOf("Праздничных/выходных часов", formatDouble(payroll.holidayHours))
    rows += listOf("Всего отмеченных дней", detailedShiftStats.totalAssignedDays.toString())
    rows += listOf("Рабочих смен", detailedShiftStats.workedShiftCount.toString())
    rows += listOf("Дневных", detailedShiftStats.dayShiftCount.toString())
    rows += listOf("Ночных", detailedShiftStats.nightShiftCount.toString())
    rows += listOf("Выходных/праздничных", detailedShiftStats.weekendHolidayShiftCount.toString())
    rows += listOf("Восьмичасовой раб.день", detailedShiftStats.eightHourShiftCount.toString())
    rows += listOf("Отпуск", detailedShiftStats.vacationShiftCount.toString())
    rows += listOf("Больничный", detailedShiftStats.sickShiftCount.toString())
    rows += listOf("Часовая ставка", formatMoney(payroll.hourlyRate))
    rows += listOf("Базовая оплата", formatMoney(payroll.basePay))
    rows += listOf("Ночные", formatMoney(payroll.nightExtra))
    rows += listOf("РВД/праздничные", formatMoney(payroll.holidayExtra))
    rows += listOf(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
    rows += listOf("Допвыплаты всего", formatMoney(payroll.additionalPaymentsTotal))
    rows += listOf("Стоимость смены: база", formatMoney(detailedShiftStats.shiftCostBaseTotal))
    rows += listOf("Стоимость смены: из них доплаты/премии", formatMoney(detailedShiftStats.shiftCostIncludedPayments))
    rows += listOf("Стоимость смены: средняя (до НДФЛ)", formatMoney(detailedShiftStats.shiftCostAverageGross))
    rows += listOf("Стоимость смены: средняя (на руки)", formatMoney(detailedShiftStats.shiftCostAverageNet))
    rows += listOf("Стоимость смены: дневная (до НДФЛ)", formatMoney(detailedShiftStats.dayShiftCostAverageGross))
    rows += listOf("Стоимость смены: дневная (на руки)", formatMoney(detailedShiftStats.dayShiftCostAverageNet))
    rows += listOf("Стоимость смены: ночная (до НДФЛ)", formatMoney(detailedShiftStats.nightShiftCostAverageGross))
    rows += listOf("Стоимость смены: ночная (на руки)", formatMoney(detailedShiftStats.nightShiftCostAverageNet))
    rows += listOf("Дней отпуска", payroll.vacationDays.toString())
    rows += listOf("Отпускные", formatMoney(payroll.vacationPay))
    rows += listOf("Дней больничного", payroll.sickDays.toString())
    rows += listOf("Больничный", formatMoney(payroll.sickPay))
    rows += listOf("Облагаемая база", formatMoney(payroll.taxableGrossTotal))
    rows += listOf("Необлагаемые выплаты", formatMoney(payroll.nonTaxableTotal))
    rows += listOf("Всего начислено", formatMoney(payroll.grossTotal))
    rows += listOf("НДФЛ", formatMoney(payroll.ndfl))
    rows += listOf("На руки", formatMoney(payroll.netTotal))
    rows += listOf("Аванс", formatMoney(payroll.advanceAmount))
    rows += listOf("Аванс только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
    rows += listOf("Дата аванса", formatDate(paymentDates.advanceDate))
    rows += listOf("К зарплате", formatMoney(payroll.salaryPaymentAmount))
    rows += listOf("Зарплата только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
    rows += listOf("Дата зарплаты", formatDate(paymentDates.salaryDate))
    rows += listOf("Сверхурочка: период", annualOvertime.periodLabel)
    rows += listOf("Сверхурочка: статус", if (annualOvertime.enabled) "Включена" else "Отключена")
    rows += listOf("Сверхурочка: норма периода", formatDouble(annualOvertime.annualNormHours))
    rows += listOf("Сверхурочка: отработано", formatDouble(annualOvertime.workedHours))
    rows += listOf("Сверхурочка: переработка до исключений", formatDouble(annualOvertime.rawOvertimeHours))
    rows += listOf("Сверхурочка: исключено", formatDouble(annualOvertime.holidayExcludedHours))
    rows += listOf("Сверхурочка: к оплате", formatDouble(annualOvertime.payableOvertimeHours))
    rows += listOf("Сверхурочка: доплата", formatMoney(annualOvertime.overtimePremiumAmount))

    if (resolvedAdditionalPaymentsBreakdown.isNotEmpty()) {
        rows += listOf("", "")
        rows += listOf("Сработавшие доплаты и премии", "Значение")
        resolvedAdditionalPaymentsBreakdown.forEach { payment ->
            rows += listOf(payment.payment.displayName, additionalPaymentTypeLabel(payment.payment.sourceTypeName))
            rows += listOf("  До НДФЛ", formatMoney(payment.grossAmount))
            rows += listOf("  НДФЛ", formatMoney(payment.ndflAmount))
            rows += listOf("  На руки", formatMoney(payment.netAmount))
            rows += listOf(
                "  Параметры",
                buildString {
                    append(if (payment.payment.withAdvance) "в аванс" else "в зарплату")
                    append(" • ")
                    append(if (payment.payment.taxable) "облагается" else "не облагается")
                }
            )
        }
    }

    if (activePayments.isNotEmpty()) {
        rows += listOf("", "")
        rows += listOf("Активные настройки начислений", "Параметры")
        activePayments.forEach { payment ->
            rows += listOf(
                payment.name.ifBlank { "Без названия" },
                "${additionalPaymentTypeLabel(payment.type)} • ${additionalPaymentDetailsLabel(payment)} • ${paymentDistributionLabel(payment.distribution)}"
            )
        }
    }

    return rows.joinToString("\n") { row ->
        row.joinToString(";") { value -> csvEscape(value) }
    }
}

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}
