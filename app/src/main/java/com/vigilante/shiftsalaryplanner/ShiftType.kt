@file:Suppress("unused")

package com.vigilante.shiftsalaryplanner

enum class ShiftType(
    val code: String,
    val title: String,
    val workHours: Double,
    val nightHours: Double = 0.0
) {
    DAY("Д", "Дневная", 11.5),
    NIGHT("Н", "Ночная", 11.5, 8.0),
    WEEKEND_DAY("РВД", "Работа в выходной день", 11.5),
    WEEKEND_NIGHT("РВН", "Работа в выходной день (ночь)", 11.5, 8.0),
    EIGHT("8", "Восьмичасовая", 8.0),
    VACATION("ОТ", "Отпуск", 0.0),
    SICK("Б", "Больничный", 0.0),
    OFF("ВЫХ", "Выходной", 0.0)
}