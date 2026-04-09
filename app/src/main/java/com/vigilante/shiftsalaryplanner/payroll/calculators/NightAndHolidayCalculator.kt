package com.vigilante.shiftsalaryplanner.payroll.calculators

import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import com.vigilante.shiftsalaryplanner.payroll.models.CalculationMode

/**
 * Расчёт доплат за ночные (0212) и выходные/праздники (0233)
 */
object NightAndHolidayCalculator {

    data class NightHolidayResult(
        val nightAmount: Double,
        val holidayAmount: Double,
        val nightHours: Double,
        val holidayHours: Double,
        val description: String
    )

    fun calculate(
        shifts: List<ShiftData>,
        nightSettings: AccrualSettings?,
        holidaySettings: AccrualSettings?
    ): NightHolidayResult {

        var nightHours = 0.0
        var holidayHours = 0.0

        shifts.filter { !it.isVacation && !it.isSick && !it.isDayOff }.forEach { shift ->
            nightHours += shift.nightHours
            if (shift.isHoliday) {
                holidayHours += shift.hours
            }
        }

        // Ночные (коэффициент обычно 40% к ставке или фиксированная сумма за час)
        val nightAmount = nightSettings?.let { settings ->
            when (settings.calculationMode) {
                CalculationMode.HOURLY_PROPORTIONAL,
                CalculationMode.FIXED_HOURLY -> {
                    val rate = settings.hourlyRate
                        ?: (settings.baseAmount / 165.0)
                    nightHours * rate
                }
                else -> 0.0
            }
        } ?: 0.0

        // Выходные/праздники (коэффициент 100% доплаты к окладу или двойная ставка)
        val holidayAmount = holidaySettings?.let { settings ->
            when (settings.calculationMode) {
                CalculationMode.HOURLY_PROPORTIONAL,
                CalculationMode.FIXED_HOURLY -> {
                    val rate = settings.hourlyRate
                        ?: (settings.baseAmount / 165.0)
                    holidayHours * rate
                }
                else -> 0.0
            }
        } ?: 0.0

        return NightHolidayResult(
            nightAmount = nightAmount,
            holidayAmount = holidayAmount,
            nightHours = nightHours,
            holidayHours = holidayHours,
            description = "Ночные: ${nightHours}ч × ставка, Праздники: ${holidayHours}ч × ставка"
        )
    }
}