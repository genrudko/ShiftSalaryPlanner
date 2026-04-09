package com.vigilante.shiftsalaryplanner.payroll.calculators

import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import com.vigilante.shiftsalaryplanner.payroll.models.CalculationMode

/**
 * Расчёт основного оклада (код 0010)
 * Пропорционально отработанным часам или фиксированно
 */
object BaseSalaryCalculator {

    fun calculate(
        shifts: List<ShiftData>,
        settings: AccrualSettings
    ): Double {
        if (!settings.isEnabled) return 0.0

        val workedShifts = shifts.filter { !it.isVacation && !it.isSick && !it.isDayOff }
        val hoursWorked = workedShifts.sumOf { it.hours }

        return when (settings.calculationMode) {
            CalculationMode.HOURLY_PROPORTIONAL -> {
                val hourlyRate = settings.hourlyRate
                    ?: (settings.baseAmount / 165.0) // норма часов в месяц
                hoursWorked * hourlyRate
            }

            CalculationMode.FIXED_MONTHLY -> {
                settings.baseAmount // Полная сумма независимо от часов
            }

            CalculationMode.FIXED_HOURLY -> {
                val rate = settings.hourlyRate ?: settings.baseAmount
                hoursWorked * rate
            }

            else -> 0.0
        }
    }
}