package com.vigilante.shiftsalaryplanner

import java.time.LocalDate
import java.time.YearMonth

/** Fixed Monday-first calendar geometry; calculated once per displayed month. */
internal fun calendarVisibleWeeks(month: YearMonth): List<List<LocalDate>> {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val count = ((first.dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7) * 7
    return List(count) { offset -> start.plusDays(offset.toLong()) }.chunked(7)
}
