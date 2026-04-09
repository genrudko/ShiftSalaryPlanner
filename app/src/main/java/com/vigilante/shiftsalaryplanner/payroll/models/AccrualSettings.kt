package com.vigilante.shiftsalaryplanner.payroll.models

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlinx.serialization.Transient

/**
 * Настройки конкретного начисления
 * Хранит все параметры для расчёта
 */
@Serializable
data class AccrualSettings(
    val id: String = java.util.UUID.randomUUID().toString(),
    val code: AccrualCode,
    val customName: String? = null,

    // Режим расчёта
    val calculationMode: CalculationMode,

    // Базовые суммы
    val baseAmount: Double = 0.0,
    val hourlyRate: Double? = null,
    val dailyRate: Double? = null,

    // Условия начисления
    val requiresWorkedHours: Boolean = true,
    val excludedInVacation: Boolean = false,
    val excludedInSick: Boolean = false,
    val excludedInHoliday: Boolean = false,

    // Периодичность (для премий)
    val periodMonths: Int = 1,
    val paymentDelayMonths: Int = 0,
    val annualAmount: Double? = null,

    // Выплата и налоги
    val withAdvance: Boolean = false,
    val taxable: Boolean = true,

    // Дополнительные параметры
    val affectsOvertime: Boolean = false,
    val isEnabled: Boolean = true
) {
    // Отображаемое название (не сериализуется)
    val displayName: String
        get() = if (!customName.isNullOrBlank()) customName else code.defaultName

    /**
     * Получает сумму начисления за конкретный период
     */
    fun getAmountForPeriod(month: Int): Double {
        return when (periodMonths) {
            3 -> (annualAmount ?: baseAmount * 4) / 4.0
            12 -> (annualAmount ?: baseAmount * 12) / 12.0
            else -> baseAmount
        }
    }

    /**
     * Проверяет, выплачивается ли это начисление в указанном месяце
     */
    fun isPaymentMonth(currentMonth: Int): Boolean {
        if (periodMonths == 1 && paymentDelayMonths == 0) return true

        val quarterEndMonth = ((currentMonth - 1) / 3) * 3 + 3
        val paymentMonth = quarterEndMonth + paymentDelayMonths

        return currentMonth == paymentMonth
    }
}

/**
 * Результат расчёта конкретного начисления
 */
@Serializable
data class AccrualResult(
    val settings: AccrualSettings,
    val amount: Double,
    val hoursWorked: Double,
    val calculationDetails: String = ""
)