package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AdvanceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.ExtraSalaryMode
import com.vigilante.shiftsalaryplanner.payroll.NightHoursBaseMode
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

fun nightHoursBaseModeLabel(modeName: String): String {
    return when (
        runCatching { NightHoursBaseMode.valueOf(modeName) }
            .getOrElse { NightHoursBaseMode.FOLLOW_HOURLY_RATE }
    ) {
        NightHoursBaseMode.FOLLOW_HOURLY_RATE -> "Как для часовой ставки"
        NightHoursBaseMode.BASE_ONLY -> "Только оклад"
        NightHoursBaseMode.BASE_PLUS_EXTRA -> "Оклад + надбавка"
        NightHoursBaseMode.BASE_PLUS_EXTRA_PLUS_MANUAL -> "Оклад + надбавка + ручные"
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

fun ratioToPercentUiValue(
    ratio: Double,
    coefficientUpperBound: Double
): Double {
    val safe = ratio.coerceAtLeast(0.0)
    return if (safe <= coefficientUpperBound) safe * 100.0 else safe
}

fun parsePercentUiToRatio(
    text: String,
    fallbackRatio: Double,
    coefficientUpperBound: Double
): Double {
    val fallbackPercent = ratioToPercentUiValue(fallbackRatio, coefficientUpperBound)
    val percentValue = parseDouble(text, fallbackPercent).coerceAtLeast(0.0)
    return percentValue / 100.0
}
