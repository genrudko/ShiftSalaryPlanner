package com.vigilante.shiftsalaryplanner.payroll.models

import kotlinx.serialization.Serializable

/**
 * Коды начислений по 1С
 */
@Serializable
enum class AccrualCode(val code: String, val defaultName: String, val category: String) {
    BASE_SALARY_0010("0010", "Оклад/Тариф", "Основные"),
    NIGHT_EXTRA_0212("0212", "Доплата за ночные часы", "Доплаты"),
    HOLIDAY_EXTRA_0233("0233", "Доплата за работу в праздники", "Доплаты"),
    INTERSTIM_1010("1010", "Интегральная стимулирующая надбавка", "Стимулирующие"),
    INDEXATION_1024("1024", "Индексирующая выплата", "Индексация"),
    PREMIUM_1273("1273", "Операционная премия", "Премии"),
    HOUSING_4433("4433", "Компенсация аренды жилья", "Компенсации"),
    VACATION_3000("3000", "Основной отпуск", "Отпуска"),
    SICK_5000("5000", "Больничный", "Больничные"),
    CUSTOM_0000("0000", "Доплата (ручная)", "Доплаты");

    companion object {
        fun fromCode(code: String): AccrualCode? {
            return values().find { it.code == code }
        }
    }
}