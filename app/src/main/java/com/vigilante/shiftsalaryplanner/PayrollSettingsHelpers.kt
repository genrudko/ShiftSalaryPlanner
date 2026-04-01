package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AdvanceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.ExtraSalaryMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayMode

fun advanceModeLabel(advanceModeName: String): String {
    return when (runCatching { AdvanceMode.valueOf(advanceModeName) }.getOrElse { AdvanceMode.ACTUAL_EARNINGS }) {
        AdvanceMode.ACTUAL_EARNINGS -> "По фактически начисленному"
        AdvanceMode.FIXED_PERCENT -> "Фиксированный процент"
    }
}

fun payModeLabel(payModeName: String): String {
    return when (runCatching { PayMode.valueOf(payModeName) }.getOrElse { PayMode.HOURLY }) {
        PayMode.HOURLY -> "Почасовая"
        PayMode.MONTHLY_SALARY -> "Помесячная по окладу"
    }
}


fun normModeLabel(normModeName: String): String {
    return when (runCatching { NormMode.valueOf(normModeName) }.getOrElse { NormMode.MANUAL }) {
        NormMode.MANUAL -> "Ручная"
        NormMode.PRODUCTION_CALENDAR -> "По производственному календарю"
        NormMode.AVERAGE_ANNUAL -> "Среднегодовая"
        NormMode.AVERAGE_QUARTERLY -> "Среднеквартальная"
    }
}

fun annualNormSourceModeLabel(modeName: String): String {
    return when (
        runCatching { AnnualNormSourceMode.valueOf(modeName) }
            .getOrElse { AnnualNormSourceMode.WORKDAY_HOURS }
    ) {
        AnnualNormSourceMode.WORKDAY_HOURS -> "По часам в рабочем дне"
        AnnualNormSourceMode.YEAR_TOTAL_HOURS -> "По общему количеству часов в году"
    }
}
fun extraSalaryModeLabel(extraSalaryModeName: String): String {
    return when (runCatching { ExtraSalaryMode.valueOf(extraSalaryModeName) }.getOrElse { ExtraSalaryMode.INCLUDED_IN_RATE }) {
        ExtraSalaryMode.INCLUDED_IN_RATE -> "Включена в часовую ставку"
        ExtraSalaryMode.FIXED_MONTHLY -> "Фиксированная месячная"
    }
}
fun overtimePeriodLabel(overtimePeriodName: String): String {
    return when (runCatching { OvertimePeriod.valueOf(overtimePeriodName) }.getOrElse { OvertimePeriod.YEAR }) {
        OvertimePeriod.MONTH -> "Месяц"
        OvertimePeriod.QUARTER -> "Квартал"
        OvertimePeriod.HALF_YEAR -> "Полугодие"
        OvertimePeriod.YEAR -> "Год"
    }
}
