package com.vigilante.shiftsalaryplanner

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val russianDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("ru-RU"))

/** One complete spoken description for a calendar date with graphical indicators. */
internal fun calendarDayAccessibilityLabel(
    date: LocalDate,
    today: LocalDate,
    isCurrentMonthCell: Boolean,
    isSpecialDay: Boolean,
    isSelected: Boolean,
    isInPreviewRange: Boolean,
    shiftLabels: List<String>,
    hasNote: Boolean,
    hasShiftOverride: Boolean
): String = buildList {
    add(date.format(russianDate))
    if (!isCurrentMonthCell) add("Другой месяц")
    if (date == today) add("Сегодня")
    if (isSelected) add("Выбран")
    if (isSpecialDay) add("Нерабочий день")
    if (isInPreviewRange) add("В выделенном диапазоне")
    val shifts = shiftLabels.filter { it.isNotBlank() }
    if (shifts.size == 1) add("Смена: " + shifts.single())
    if (shifts.size > 1) add("Смены: " + shifts.joinToString(", "))
    if (hasNote) add("Есть заметка")
    if (hasShiftOverride) add("Индивидуальная правка смены")
}.joinToString(". ")
