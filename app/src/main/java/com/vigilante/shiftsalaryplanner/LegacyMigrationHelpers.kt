package com.vigilante.shiftsalaryplanner

import android.content.SharedPreferences
import com.vigilante.shiftsalaryplanner.payroll.AdvanceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.ExtraSalaryMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import kotlin.math.abs

const val PREFS_SHIFT_COLORS = "shift_colors"
const val PREFS_SHIFT_SPECIAL_RULES = "shift_special_rules"
const val PREFS_ONE_TIME_MIGRATIONS = "one_time_migrations"
const val KEY_MIGRATION_LEGACY_DEFAULTS_CLEANUP_V1 = "legacy_defaults_cleanup_v1"
const val LEGACY_EMBEDDED_BASE_SALARY = 102050.0
const val LEGACY_EMBEDDED_EXTRA_SALARY = 49733.0

fun neutralInitialPayrollSettings(): PayrollSettings = PayrollSettings(
    baseSalary = 0.0,
    extraSalary = 0.0
)
fun Double.nearlyEquals(other: Double, epsilon: Double = 0.0001): Boolean =
    abs(this - other) <= epsilon

fun readPayrollSettingsFromPrefs(prefs: SharedPreferences): PayrollSettings {
    return PayrollSettings(
        baseSalary = prefs.getFloat("base_salary", 0f).toDouble(),
        extraSalary = prefs.getFloat("extra_salary", 0f).toDouble(),
        housingPayment = prefs.getFloat("housing_payment", 0f).toDouble(),
        housingPaymentLabel = prefs.getString("housing_payment_label", "Выплата на квартиру") ?: "Выплата на квартиру",
        housingPaymentTaxable = prefs.getBoolean("housing_payment_taxable", true),
        housingPaymentWithAdvance = prefs.getBoolean("housing_payment_with_advance", false),
        monthlyNormHours = prefs.getFloat("monthly_norm_hours", 165f).toDouble(),
        workdayHours = prefs.getFloat("workday_hours", 8f).toDouble(),
        annualNormSourceMode = prefs.getString("annual_norm_source_mode", "WORKDAY_HOURS") ?: "WORKDAY_HOURS",
        annualNormHours = prefs.getFloat("annual_norm_hours", 1970f).toDouble(),
        normMode = prefs.getString("norm_mode", "MANUAL") ?: "MANUAL",
        payMode = prefs.getString("pay_mode", "HOURLY") ?: "HOURLY",
        extraSalaryMode = prefs.getString("extra_salary_mode", "INCLUDED_IN_RATE") ?: "INCLUDED_IN_RATE",
        advanceMode = prefs.getString("advance_mode", "ACTUAL_EARNINGS") ?: "ACTUAL_EARNINGS",
        advancePercent = prefs.getFloat("advance_percent", 50f).toDouble(),
        applyShortDayReduction = prefs.getBoolean("apply_short_day_reduction", true),
        nightPercent = prefs.getFloat("night_percent", 0.4f).toDouble(),
        holidayRateMultiplier = prefs.getFloat("holiday_rate_multiplier", 2f).toDouble(),
        ndflPercent = prefs.getFloat("ndfl_percent", 0.13f).toDouble(),
        vacationAverageDaily = prefs.getFloat("vacation_average_daily", 0f).toDouble(),
        vacationAccruals12Months = prefs.getFloat("vacation_accruals_12_months", 0f).toDouble(),
        sickAverageDaily = prefs.getFloat("sick_average_daily", 0f).toDouble(),
        sickIncomeYear1 = prefs.getFloat("sick_income_year1", 0f).toDouble(),
        sickIncomeYear2 = prefs.getFloat("sick_income_year2", 0f).toDouble(),
        sickLimitYear1 = prefs.getFloat("sick_limit_year1", 0f).toDouble(),
        sickLimitYear2 = prefs.getFloat("sick_limit_year2", 0f).toDouble(),
        sickCalculationPeriodDays = prefs.getInt("sick_calculation_period_days", 730),
        sickExcludedDays = prefs.getInt("sick_excluded_days", 0),
        sickPayPercent = prefs.getFloat("sick_pay_percent", 1f).toDouble(),
        sickMaxDailyAmount = prefs.getFloat("sick_max_daily_amount", 6827.40f).toDouble(),
        progressiveNdflEnabled = prefs.getBoolean("progressive_ndfl_enabled", false),
        taxableIncomeYtdBeforeCurrentMonth = prefs.getFloat("taxable_income_ytd_before_current_month", 0f).toDouble(),
        advanceDay = prefs.getInt("advance_day", 20),
        salaryDay = prefs.getInt("salary_day", 5),
        movePaymentsToPreviousWorkday = prefs.getBoolean("move_payments_to_previous_workday", true),
        overtimeEnabled = prefs.getBoolean("overtime_enabled", true),
        overtimePeriod = prefs.getString("overtime_period", "YEAR") ?: "YEAR",
        excludeWeekendHolidayFromOvertime = prefs.getBoolean("exclude_weekend_holiday_from_overtime", true),
        excludeRvdDoublePayFromOvertime = prefs.getBoolean("exclude_rvd_double_pay_from_overtime", true),
        excludeRvdSingleWithDayOffFromOvertime = prefs.getBoolean("exclude_rvd_single_with_day_off_from_overtime", false)
    )
}
fun PayrollSettings.matchesLikelyLegacyEmbeddedPayrollDefaults(): Boolean {
    return baseSalary.nearlyEquals(LEGACY_EMBEDDED_BASE_SALARY) &&
            extraSalary.nearlyEquals(LEGACY_EMBEDDED_EXTRA_SALARY) &&
            housingPayment.nearlyEquals(0.0) &&
            housingPaymentLabel == "Выплата на квартиру" &&
            housingPaymentTaxable &&
            !housingPaymentWithAdvance &&
            monthlyNormHours.nearlyEquals(165.0) &&
            workdayHours.nearlyEquals(8.0) &&
            annualNormSourceMode == AnnualNormSourceMode.WORKDAY_HOURS.name &&
            annualNormHours.nearlyEquals(1970.0) &&
            normMode == NormMode.MANUAL.name &&
            payMode == PayMode.HOURLY.name &&
            extraSalaryMode == ExtraSalaryMode.INCLUDED_IN_RATE.name &&
            advanceMode == AdvanceMode.ACTUAL_EARNINGS.name &&
            advancePercent.nearlyEquals(50.0) &&
            applyShortDayReduction &&
            nightPercent.nearlyEquals(0.4) &&
            holidayRateMultiplier.nearlyEquals(2.0) &&
            ndflPercent.nearlyEquals(0.13) &&
            vacationAverageDaily.nearlyEquals(0.0) &&
            vacationAccruals12Months.nearlyEquals(0.0) &&
            sickAverageDaily.nearlyEquals(0.0) &&
            sickIncomeYear1.nearlyEquals(0.0) &&
            sickIncomeYear2.nearlyEquals(0.0) &&
            sickLimitYear1.nearlyEquals(0.0) &&
            sickLimitYear2.nearlyEquals(0.0) &&
            sickCalculationPeriodDays == 730 &&
            sickExcludedDays == 0 &&
            sickPayPercent.nearlyEquals(1.0) &&
            sickMaxDailyAmount.nearlyEquals(6827.40) &&
            !progressiveNdflEnabled &&
            taxableIncomeYtdBeforeCurrentMonth.nearlyEquals(0.0) &&
            advanceDay == 20 &&
            salaryDay == 5 &&
            movePaymentsToPreviousWorkday &&
            overtimeEnabled &&
            overtimePeriod == OvertimePeriod.YEAR.name &&
            excludeWeekendHolidayFromOvertime &&
            excludeRvdDoublePayFromOvertime &&
            !excludeRvdSingleWithDayOffFromOvertime
}
