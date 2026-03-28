package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PayrollSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("payroll_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadFromPrefs())

    val settingsFlow: Flow<PayrollSettings> = _settingsFlow.asStateFlow()

    private fun loadFromPrefs(): PayrollSettings {
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

    fun save(settings: PayrollSettings) {
        prefs.edit {
            putFloat("base_salary", settings.baseSalary.toFloat())
                .putFloat("extra_salary", settings.extraSalary.toFloat())
                .putFloat("housing_payment", settings.housingPayment.toFloat())
                .putString("housing_payment_label", settings.housingPaymentLabel)
                .putBoolean("housing_payment_taxable", settings.housingPaymentTaxable)
                .putBoolean("housing_payment_with_advance", settings.housingPaymentWithAdvance)
                .putFloat("monthly_norm_hours", settings.monthlyNormHours.toFloat())
                .putFloat("workday_hours", settings.workdayHours.toFloat())
                .putString("annual_norm_source_mode", settings.annualNormSourceMode)
                .putFloat("annual_norm_hours", settings.annualNormHours.toFloat())
                .putString("norm_mode", settings.normMode)
                .putString("pay_mode", settings.payMode)
                .putString("extra_salary_mode", settings.extraSalaryMode)
                .putString("advance_mode", settings.advanceMode)
                .putFloat("advance_percent", settings.advancePercent.toFloat())
                .putBoolean("apply_short_day_reduction", settings.applyShortDayReduction)
                .putFloat("night_percent", settings.nightPercent.toFloat())
                .putFloat("holiday_rate_multiplier", settings.holidayRateMultiplier.toFloat())
                .putFloat("ndfl_percent", settings.ndflPercent.toFloat())
                .putFloat("vacation_average_daily", settings.vacationAverageDaily.toFloat())
                .putFloat("vacation_accruals_12_months", settings.vacationAccruals12Months.toFloat())
                .putFloat("sick_average_daily", settings.sickAverageDaily.toFloat())
                .putFloat("sick_income_year1", settings.sickIncomeYear1.toFloat())
                .putFloat("sick_income_year2", settings.sickIncomeYear2.toFloat())
                .putFloat("sick_limit_year1", settings.sickLimitYear1.toFloat())
                .putFloat("sick_limit_year2", settings.sickLimitYear2.toFloat())
                .putInt("sick_calculation_period_days", settings.sickCalculationPeriodDays)
                .putInt("sick_excluded_days", settings.sickExcludedDays)
                .putFloat("sick_pay_percent", settings.sickPayPercent.toFloat())
                .putFloat("sick_max_daily_amount", settings.sickMaxDailyAmount.toFloat())
                .putBoolean("progressive_ndfl_enabled", settings.progressiveNdflEnabled)
                .putFloat("taxable_income_ytd_before_current_month", settings.taxableIncomeYtdBeforeCurrentMonth.toFloat())
                .putInt("advance_day", settings.advanceDay)
                .putInt("salary_day", settings.salaryDay)
                .putBoolean("move_payments_to_previous_workday", settings.movePaymentsToPreviousWorkday)
                .putBoolean("overtime_enabled", settings.overtimeEnabled)
                .putString("overtime_period", settings.overtimePeriod)
                .putBoolean("exclude_weekend_holiday_from_overtime", settings.excludeWeekendHolidayFromOvertime)
                .putBoolean("exclude_rvd_double_pay_from_overtime", settings.excludeRvdDoublePayFromOvertime)
                .putBoolean("exclude_rvd_single_with_day_off_from_overtime", settings.excludeRvdSingleWithDayOffFromOvertime)
        }

        _settingsFlow.value = loadFromPrefs()
    }
}
