package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AdvanceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.ExtraSalaryMode
import com.vigilante.shiftsalaryplanner.payroll.LegislationProfile
import com.vigilante.shiftsalaryplanner.payroll.NightHoursBaseMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePaymentMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode

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
        PayMode.PER_SHIFT -> "За смену"
    }
}

fun paymentScheduleModeLabel(modeName: String): String {
    return when (
        runCatching { PaymentScheduleMode.valueOf(modeName) }
            .getOrElse { PaymentScheduleMode.TWICE_MONTHLY }
    ) {
        PaymentScheduleMode.ONCE_MONTHLY -> "Раз в месяц"
        PaymentScheduleMode.TWICE_MONTHLY -> "Аванс + зарплата"
        PaymentScheduleMode.PER_SHIFT -> "После каждой смены"
    }
}

fun legislationProfileLabel(profileName: String): String {
    return when (
        runCatching { LegislationProfile.valueOf(profileName) }
            .getOrElse { LegislationProfile.RUSSIA }
    ) {
        LegislationProfile.RUSSIA -> "РФ"
        LegislationProfile.UNIVERSAL -> "Универсально"
        LegislationProfile.CUSTOM -> "Свои правила"
    }
}

fun legislationProfileDescription(profileName: String): String {
    return when (
        runCatching { LegislationProfile.valueOf(profileName) }
            .getOrElse { LegislationProfile.RUSSIA }
    ) {
        LegislationProfile.RUSSIA -> "Базовый профиль под РФ: НДФЛ, праздники, РВД и сверхурочка настроены ближе к текущей логике приложения."
        LegislationProfile.UNIVERSAL -> "Нейтральный профиль без привязки к стране: ставки, налоги и сверхурочку задаёшь вручную."
        LegislationProfile.CUSTOM -> "Ручной профиль для организации или страны со своими правилами расчёта."
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

fun overtimePaymentModeLabel(modeName: String): String {
    return when (
        runCatching { OvertimePaymentMode.valueOf(modeName) }
            .getOrElse { OvertimePaymentMode.RF_LIKE }
    ) {
        OvertimePaymentMode.RF_LIKE -> "По правилам"
        OvertimePaymentMode.PERCENT_OF_HOURLY -> "% от часовки"
        OvertimePaymentMode.CUSTOM_MULTIPLIER -> "Гибкая"
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
