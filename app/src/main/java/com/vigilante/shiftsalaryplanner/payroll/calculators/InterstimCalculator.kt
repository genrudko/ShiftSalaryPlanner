package com.vigilante.shiftsalaryplanner.payroll.calculators

import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import com.vigilante.shiftsalaryplanner.payroll.models.CalculationMode

/**
 * Расчёт интегральной надбавки (1010) и индексации (1024)
 * Оба начисления работают пропорционально часам
 */
object InterstimCalculator {

    data class InterstimResult(
        val gross: Double,
        val taxable: Double,
        val hoursWorked: Double,
        val excludedDays: Int  // Дни, за которые не начислено (отпуск/больничный)
    )

    fun calculate(
        shifts: List<ShiftData>,
        settings: AccrualSettings
    ): InterstimResult {
        if (!settings.isEnabled) return InterstimResult(0.0, 0.0, 0.0, 0)

        // Фильтруем смены с учётом исключений
        val eligibleShifts = shifts.filter { shift ->
            when {
                settings.excludedInVacation && shift.isVacation -> false
                settings.excludedInSick && shift.isSick -> false
                settings.excludedInHoliday && shift.isHoliday -> false
                shift.isDayOff -> false
                else -> true
            }
        }

        val excludedShifts = shifts.filter { shift ->
            (settings.excludedInVacation && shift.isVacation) ||
                    (settings.excludedInSick && shift.isSick)
        }

        val hoursWorked = eligibleShifts.sumOf { it.hours }
        val excludedDays = excludedShifts.size

        val amount = when (settings.calculationMode) {
            CalculationMode.HOURLY_PROPORTIONAL -> {
                val hourlyRate = settings.hourlyRate
                    ?: (settings.baseAmount / 165.0)
                hoursWorked * hourlyRate
            }

            CalculationMode.FIXED_MONTHLY -> {
                settings.baseAmount
            }

            else -> 0.0
        }

        val taxable = if (settings.taxable) amount else 0.0

        return InterstimResult(
            gross = amount,
            taxable = taxable,
            hoursWorked = hoursWorked,
            excludedDays = excludedDays
        )
    }
}