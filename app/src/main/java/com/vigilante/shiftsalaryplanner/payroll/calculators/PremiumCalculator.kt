package com.vigilante.shiftsalaryplanner.payroll.calculators

import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import com.vigilante.shiftsalaryplanner.payroll.models.CalculationMode
import java.time.YearMonth
import java.time.temporal.IsoFields

/**
 * Расчёт квартальной премии (код 1273/1275)
 * Условия: годовая сумма 420000, квартальная выплата, отсрочка 1 месяц,
 * не начисляется за дни отпуска и больничного
 */
object PremiumCalculator {

    data class PremiumResult(
        val gross: Double,                    // Начислено
        val taxable: Double,                  // Облагаемая база
        val workedDaysInQuarter: Int,         // Отработанные дни (без отпусков/больничных)
        val totalWorkingDaysInQuarter: Int,   // Всего рабочих дней в квартале
        val ratio: Double,                    // Коэффициент (worked/total)
        val isPaymentMonth: Boolean,          // Выплачивается ли в текущем месяце
        val quarterNumber: Int,               // Номер квартала (1-4)
        val calculationDescription: String    // Описание для отладки
    ) {
        val excludedDays: Int
            get() = totalWorkingDaysInQuarter - workedDaysInQuarter
    }

    /**
     * Расчёт квартальной премии
     *
     * @param currentMonth Текущий месяц (YearMonth.of(2026, 4) = апрель 2026)
     * @param allShiftsInQuarter Все смены за квартал (включая отпуска и больничные)
     * @param settings Настройки премии (должны быть periodMonths=3, annualAmount=420000)
     */
    fun calculate(
        currentMonth: YearMonth,
        allShiftsInQuarter: List<ShiftData>,
        settings: AccrualSettings
    ): PremiumResult {

        // Проверяем, что это квартальная премия
        if (settings.calculationMode != CalculationMode.QUARTERLY_FIXED &&
            settings.periodMonths != 3) {
            return emptyResult(currentMonth, false)
        }

        // Определяем текущий квартал и месяц выплаты
        val quarter = currentMonth.get(IsoFields.QUARTER_OF_YEAR)
        val quarterEndMonth = ((currentMonth.monthValue - 1) / 3) * 3 + 3
        val paymentMonth = quarterEndMonth + settings.paymentDelayMonths

        // Проверяем, является ли текущий месяц месяцем выплаты
        val isPaymentMonth = currentMonth.monthValue == paymentMonth

        if (!isPaymentMonth) {
            return emptyResult(currentMonth, false, quarter)
        }

        // Расчёт суммы квартальной премии
        // Годовая сумма / 4 квартала
        val annualAmount = settings.annualAmount ?: (settings.baseAmount * 4)
        val quarterlyAmount = annualAmount / 4.0

        // Подсчёт дней
        val totalDays = allShiftsInQuarter.count {
            !it.isDayOff // Рабочие дни в квартале (без выходных)
        }

        // Отработанные дни (исключая отпуски и больничные, если настроено)
        val workedDays = allShiftsInQuarter.count { shift ->
            when {
                settings.excludedInVacation && shift.isVacation -> false
                settings.excludedInSick && shift.isSick -> false
                shift.isDayOff -> false
                else -> true
            }
        }

        // Коэффициент пропорции
        val ratio = if (totalDays > 0) {
            workedDays.toDouble() / totalDays.toDouble()
        } else 1.0

        // Итоговая сумма с учётом пропорции
        val finalAmount = quarterlyAmount * ratio
        val taxableAmount = if (settings.taxable) finalAmount else 0.0

        val description = buildString {
            append("Квартал $quarter: ")
            append("$workedDays раб.дн из $totalDays, ")
            append("коэфф. ${"%.2f".format(ratio)}, ")
            append("выплата с отсрочкой в ${settings.paymentDelayMonths} мес")
        }

        return PremiumResult(
            gross = finalAmount,
            taxable = taxableAmount,
            workedDaysInQuarter = workedDays,
            totalWorkingDaysInQuarter = totalDays,
            ratio = ratio,
            isPaymentMonth = true,
            quarterNumber = quarter,
            calculationDescription = description
        )
    }

    private fun emptyResult(
        currentMonth: YearMonth,
        isPayment: Boolean,
        quarter: Int = 0
    ): PremiumResult {
        return PremiumResult(
            gross = 0.0,
            taxable = 0.0,
            workedDaysInQuarter = 0,
            totalWorkingDaysInQuarter = 0,
            ratio = 0.0,
            isPaymentMonth = isPayment,
            quarterNumber = quarter,
            calculationDescription = "Выплата в другом месяце"
        )
    }

    /**
     * Получает месяцы квартала по номеру
     */
    fun getQuarterMonths(quarter: Int, year: Int): List<YearMonth> {
        val startMonth = (quarter - 1) * 3 + 1
        return (0..2).map { YearMonth.of(year, startMonth + it) }
    }
}