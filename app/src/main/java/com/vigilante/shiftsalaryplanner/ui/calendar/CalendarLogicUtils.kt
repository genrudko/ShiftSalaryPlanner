package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import com.vigilante.shiftsalaryplanner.data.ShiftDayDao
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun fixedFederalHolidayMap(year: Int): Map<LocalDate, HolidayEntity> {
    val items = mutableListOf<HolidayEntity>()

    for (day in 1..8) {
        val date = LocalDate.of(year, 1, day)
        val title = if (day == 7) "Рождество Христово" else "Новогодние каникулы"
        items += HolidayEntity(
            id = "RU-FED-FIXED|${date}|${HolidayKinds.HOLIDAY}",
            date = date.toString(),
            title = title,
            scopeCode = "RU-FED",
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )
    }

    listOf(
        LocalDate.of(year, 2, 23) to "День защитника Отечества",
        LocalDate.of(year, 3, 8) to "Международный женский день",
        LocalDate.of(year, 5, 1) to "Праздник Весны и Труда",
        LocalDate.of(year, 5, 9) to "День Победы",
        LocalDate.of(year, 6, 12) to "День России",
        LocalDate.of(year, 11, 4) to "День народного единства"
    ).forEach { (date, title) ->
        items += HolidayEntity(
            id = "RU-FED-FIXED|${date}|${HolidayKinds.HOLIDAY}",
            date = date.toString(),
            title = title,
            scopeCode = "RU-FED",
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )
    }

    return items.associateBy { LocalDate.parse(it.date) }
}

fun isCalendarDayOff(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>
): Boolean {
    return isWeekendDay(date) || holidayMap.containsKey(date)
}

fun isWeekendDay(date: LocalDate): Boolean {
    return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
}

suspend fun applyPatternToMonth(
    shiftDayDao: ShiftDayDao,
    pattern: PatternTemplate,
    cycleStartDate: LocalDate,
    month: YearMonth,
    validShiftCodes: Set<String>
) {
    val cycle = pattern.normalizedSteps().take(pattern.usedLength())

    if (cycle.isEmpty()) return

    var date = month.atDay(1)
    val endDate = month.atEndOfMonth()

    while (!date.isAfter(endDate)) {
        val diffDays = ChronoUnit.DAYS.between(cycleStartDate, date).toInt()
        val cycleIndex = ((diffDays % cycle.size) + cycle.size) % cycle.size
        val code = cycle[cycleIndex]

        if (code.isBlank()) {
            shiftDayDao.deleteByDate(date.toString())
        } else if (validShiftCodes.contains(code)) {
            shiftDayDao.upsert(
                ShiftDayEntity(
                    date = date.toString(),
                    shiftCode = code
                )
            )
        }

        date = date.plusDays(1)
    }
}
suspend fun applyPatternToRange(
    shiftDayDao: ShiftDayDao,
    pattern: PatternTemplate,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    validShiftCodes: Set<String>,
    phaseOffset: Int = 0
) {
    val cycle = pattern.normalizedSteps().take(pattern.usedLength())
    if (cycle.isEmpty()) return

    var date = rangeStart
    while (!date.isAfter(rangeEnd)) {
        val diffDays = ChronoUnit.DAYS.between(rangeStart, date).toInt()
        val rawIndex = diffDays + phaseOffset
        val cycleIndex = ((rawIndex % cycle.size) + cycle.size) % cycle.size
        val code = cycle[cycleIndex]

        if (code.isBlank()) {
            shiftDayDao.deleteByDate(date.toString())
        } else if (validShiftCodes.contains(code)) {
            shiftDayDao.upsert(
                ShiftDayEntity(
                    date = date.toString(),
                    shiftCode = code
                )
            )
        }

        date = date.plusDays(1)
    }
}
fun buildPatternPreviewRows(
    pattern: PatternTemplate,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    phaseOffset: Int,
    maxItems: Int
): List<Pair<LocalDate, String>> {
    val cycle = pattern.normalizedSteps().take(pattern.usedLength())
    if (cycle.isEmpty()) return emptyList()

    val result = mutableListOf<Pair<LocalDate, String>>()
    var date = rangeStart

    while (!date.isAfter(rangeEnd) && result.size < maxItems) {
        val diffDays = ChronoUnit.DAYS.between(rangeStart, date).toInt()
        val rawIndex = diffDays + phaseOffset
        val cycleIndex = ((rawIndex % cycle.size) + cycle.size) % cycle.size
        val code = cycle[cycleIndex]

        result.add(date to code)
        date = date.plusDays(1)
    }

    return result
}