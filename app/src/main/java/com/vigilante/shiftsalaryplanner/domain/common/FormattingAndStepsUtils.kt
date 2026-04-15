package com.vigilante.shiftsalaryplanner

import java.time.LocalDate
import java.util.Locale

private object CurrencyFormatterConfig {
    @Volatile
    var symbol: String = "₽"
}

fun Double.toPlainString(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else toString()
}

fun displayHousingPaymentLabel(rawLabel: String): String {
    return rawLabel.trim().ifBlank { "Выплата на квартиру" }
}

fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}

fun setCurrencySymbol(symbol: String) {
    CurrencyFormatterConfig.symbol = symbol.ifBlank { "₽" }
}

fun currentCurrencySymbol(): String = CurrencyFormatterConfig.symbol

fun formatMoney(value: Double): String = String.format(Locale.US, "%.2f %s", value, CurrencyFormatterConfig.symbol)

fun isDateInRange(
    date: LocalDate,
    start: LocalDate?,
    end: LocalDate?
): Boolean {
    if (start == null || end == null) return false
    return !date.isBefore(start) && !date.isAfter(end)
}

fun trimTrailingBlankSteps(steps: List<String>, minSize: Int = 35): List<String> {
    val lastUsedIndex = steps.indexOfLast { it.isNotBlank() }
    val trimmed = if (lastUsedIndex == -1) emptyList() else steps.take(lastUsedIndex + 1)
    return if (trimmed.size >= minSize) trimmed else trimmed + List(minSize - trimmed.size) { "" }
}

fun shiftStepsLeft(steps: List<String>): List<String> {
    if (steps.isEmpty()) return steps
    return steps.drop(1) + steps.first()
}

fun shiftStepsRight(steps: List<String>): List<String> {
    if (steps.isEmpty()) return steps
    return listOf(steps.last()) + steps.dropLast(1)
}

