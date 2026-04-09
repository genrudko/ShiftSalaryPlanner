package com.vigilante.shiftsalaryplanner.payroll.calculators

import java.time.LocalDate

/**
 * Данные о смене для расчёта зарплаты
 * Упрощённая версия для калькуляторов
 */
data class ShiftData(
    val date: LocalDate,
    val shiftCode: String,           // Код смены (Д, Н, ОТ, Б и т.д.)
    val hours: Double,                // Всего часов
    val nightHours: Double = 0.0,     // Ночных часов
    val isHoliday: Boolean = false,     // Праздничный день
    val isVacation: Boolean = false,    // Отпуск
    val isSick: Boolean = false,        // Больничный
    val isDayOff: Boolean = false      // Выходной (не работал)
)