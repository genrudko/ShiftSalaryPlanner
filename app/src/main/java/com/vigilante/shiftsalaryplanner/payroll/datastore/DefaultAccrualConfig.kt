package com.vigilante.shiftsalaryplanner.payroll.datastore

import com.vigilante.shiftsalaryplanner.payroll.models.AccrualCode
import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import com.vigilante.shiftsalaryplanner.payroll.models.CalculationMode

/**
 * Начальные настройки по умолчанию
 */
object DefaultAccrualConfig {

    fun getDefaultSettings(): List<AccrualSettings> {
        return listOf(
            // 1. ОКЛАД (0010)
            AccrualSettings(
                id = "default-0010",  // ← ДОБАВИЛ id
                code = AccrualCode.BASE_SALARY_0010,
                calculationMode = CalculationMode.HOURLY_PROPORTIONAL,
                baseAmount = 102050.0,
                hourlyRate = 102050.0 / 165.0,
                requiresWorkedHours = true,
                excludedInVacation = false,
                excludedInSick = false,
                withAdvance = true,
                taxable = true
            ),

            // 2. ИНТЕГРАЛЬНАЯ НАДБАВКА (1010)
            AccrualSettings(
                id = "default-1010",  // ← ДОБАВИЛ id
                code = AccrualCode.INTERSTIM_1010,
                calculationMode = CalculationMode.HOURLY_PROPORTIONAL,
                baseAmount = 49733.0,
                hourlyRate = 49733.0 / 165.0,
                requiresWorkedHours = true,
                excludedInVacation = false,
                excludedInSick = false,
                withAdvance = true,
                taxable = true
            ),

            // 3. ИНДЕКСИРУЮЩАЯ ВЫПЛАТА (1024)
            AccrualSettings(
                id = "default-1024",  // ← ДОБАВИЛ id
                code = AccrualCode.INDEXATION_1024,
                calculationMode = CalculationMode.HOURLY_PROPORTIONAL,
                baseAmount = 19404.0,
                hourlyRate = 19404.0 / 165.0,
                requiresWorkedHours = true,
                excludedInVacation = false,
                excludedInSick = false,
                withAdvance = true,
                taxable = true
            ),

            // 4. ОПЕРАЦИОННАЯ ПРЕМИЯ (1273)
            AccrualSettings(
                id = "default-1273",  // ← ДОБАВИЛ id
                code = AccrualCode.PREMIUM_1273,
                calculationMode = CalculationMode.QUARTERLY_FIXED,
                baseAmount = 105000.0,
                annualAmount = 420000.0,
                periodMonths = 3,
                paymentDelayMonths = 1,
                requiresWorkedHours = true,
                excludedInVacation = true,
                excludedInSick = true,
                withAdvance = false,
                taxable = true
            ),

            // 5. КОМПЕНСАЦИЯ ЖИЛЬЯ (4433)
            AccrualSettings(
                id = "default-4433",  // ← ДОБАВИЛ id
                code = AccrualCode.HOUSING_4433,
                calculationMode = CalculationMode.FIXED_MONTHLY,
                baseAmount = 20000.0,
                requiresWorkedHours = false,
                excludedInVacation = false,
                excludedInSick = false,
                withAdvance = false,
                taxable = true
            ),

            // 6. НОЧНЫЕ (0212)
            AccrualSettings(
                id = "default-0212",  // ← ДОБАВИЛ id
                code = AccrualCode.NIGHT_EXTRA_0212,
                calculationMode = CalculationMode.HOURLY_PROPORTIONAL,
                baseAmount = 0.0,
                hourlyRate = 0.0,
                requiresWorkedHours = true,
                withAdvance = true,
                taxable = true,
                isEnabled = true
            ),

            // 7. ВЫХОДНЫЕ/ПРАЗДНИКИ (0233)
            AccrualSettings(
                id = "default-0233",  // ← ДОБАВИЛ id
                code = AccrualCode.HOLIDAY_EXTRA_0233,
                calculationMode = CalculationMode.HOURLY_PROPORTIONAL,
                baseAmount = 0.0,
                hourlyRate = 0.0,
                requiresWorkedHours = true,
                withAdvance = true,
                taxable = true,
                isEnabled = true
            )
        )
    }

    fun getSettingsWithoutHousing(): List<AccrualSettings> {
        return getDefaultSettings().map {
            if (it.code == AccrualCode.HOUSING_4433) {
                it.copy(isEnabled = false)
            } else it
        }
    }
}