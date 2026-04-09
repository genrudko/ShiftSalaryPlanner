package com.vigilante.shiftsalaryplanner.payroll.models

import kotlinx.serialization.Serializable

/**
 * Режимы расчёта начислений
 */
@Serializable
enum class CalculationMode(val displayName: String) {
    HOURLY_PROPORTIONAL("Пропорционально часам"),
    DAILY_PROPORTIONAL("Пропорционально дням"),
    SHIFT_PROPORTIONAL("Пропорционально сменам"),
    FIXED_MONTHLY("Фиксированная сумма ежемесячно"),
    FIXED_HOURLY("Почасовая ставка"),
    QUARTERLY_FIXED("Квартальная фиксированная"),
    ANNUAL_FIXED("Годовая фиксированная (1/12 в месяц");
}