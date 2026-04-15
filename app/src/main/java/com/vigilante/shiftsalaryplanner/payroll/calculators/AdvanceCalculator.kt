package com.vigilante.shiftsalaryplanner.payroll.calculators

import com.vigilante.shiftsalaryplanner.currentCurrencySymbol
import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import com.vigilante.shiftsalaryplanner.payroll.models.CalculationMode
import java.time.YearMonth

/**
 * Расчёт аванса по фактическим сменам с 1 по 15 число
 * Как в реальных расчётных листах ОП АО "РосатомВэ"
 */
object AdvanceCalculator {

    data class AdvanceResult(
        val gross: Double,                    // Всего начислено
        val taxable: Double,                  // Облагаемая база
        val ndfl: Double,                     // НДФЛ 13%
        val net: Double,                      // На руки (gross - ndfl)
        val details: List<AdvanceDetail>,     // Детализация по начислениям
        val shiftsCount: Int,                 // Количество смен в аванс
        val hoursTotal: Double                 // Всего часов в аванс
    )

    data class AdvanceDetail(
        val accrualName: String,              // Название начисления
        val accrualCode: String,              // Код (0010, 1010 и т.д.)
        val amount: Double,                   // Сумма
        val isTaxable: Boolean,               // Облагается ли НДФЛ
        val calculationDescription: String    // Описание расчёта для отладки
    )

    /**
     * Расчёт аванса за первую половину месяца (1-15 числа)
     *
     * @param allShifts Все смены месяца
     * @param accrualSettings Список настроек начислений (только те, что с withAdvance=true)
     * @param targetMonth Целевой месяц для определения 1-15 чисел
     */
    fun calculateAdvance(
        allShifts: List<ShiftData>,
        accrualSettings: List<AccrualSettings>,
        targetMonth: YearMonth
    ): AdvanceResult {

        // Фильтруем смены с 1 по 15 число
        val firstHalfShifts = allShifts.filter { shift ->
            val day = shift.date.dayOfMonth
            day in 1..15 && !shift.isVacation && !shift.isSick && !shift.isDayOff
        }

        val hoursTotal = firstHalfShifts.sumOf { it.hours }
        val shiftsCount = firstHalfShifts.size

        val details = mutableListOf<AdvanceDetail>()
        var gross = 0.0
        var taxable = 0.0

        // Расчёт только тех начислений, что входят в аванс
        accrualSettings.filter { it.withAdvance && it.isEnabled }.forEach { settings ->

            val amount = when (settings.calculationMode) {
                CalculationMode.HOURLY_PROPORTIONAL -> {
                    // Пропорционально отработанным часам: ставка × часы
                    val rate = settings.hourlyRate ?: (settings.baseAmount / 165.0) // 165 - типовая норма
                    val calculated = hoursTotal * rate
                    calculated
                }

                CalculationMode.SHIFT_PROPORTIONAL -> {
                    // Пропорционально сменам: сумма за смену × количество смен
                    val perShift = settings.baseAmount / 15.0 // примерно 15 смен в месяц
                    shiftsCount * perShift
                }

                CalculationMode.FIXED_MONTHLY -> {
                    // Фиксированная сумма делится пополам (1-15 и 16-31)
                    settings.baseAmount / 2.0
                }

                CalculationMode.FIXED_HOURLY -> {
                    // Почасовая ставка × часы
                    val rate = settings.hourlyRate ?: settings.baseAmount
                    hoursTotal * rate
                }

                else -> 0.0 // Квартальные и годовые в аванс не идут
            }

            if (amount > 0) {
                val isTaxable = settings.taxable
                val currencySymbol = currentCurrencySymbol()
                val description = when (settings.calculationMode) {
                    CalculationMode.HOURLY_PROPORTIONAL -> "${settings.hourlyRate?.let { "%.2f".format(it) } ?: "ставка"} $currencySymbol × $hoursTotal ч"
                    CalculationMode.SHIFT_PROPORTIONAL -> "$shiftsCount смен × ${(settings.baseAmount / 15.0).let { "%.2f".format(it) }} $currencySymbol"
                    CalculationMode.FIXED_MONTHLY -> "${settings.baseAmount} $currencySymbol / 2"
                    else -> "Фиксированная сумма"
                }

                details.add(AdvanceDetail(
                    accrualName = settings.displayName,
                    accrualCode = settings.code.code,
                    amount = amount,
                    isTaxable = isTaxable,
                    calculationDescription = description
                ))

                gross += amount
                if (isTaxable) taxable += amount
            }
        }

        // НДФЛ с аванса (13% от облагаемой базы аванса)
        // Важно: НДФЛ считается отдельно для аванса, не общий за месяц!
        val ndfl = taxable * 0.13
        val net = gross - ndfl

        return AdvanceResult(
            gross = gross,
            taxable = taxable,
            ndfl = ndfl,
            net = net,
            details = details,
            shiftsCount = shiftsCount,
            hoursTotal = hoursTotal
        )
    }
}
