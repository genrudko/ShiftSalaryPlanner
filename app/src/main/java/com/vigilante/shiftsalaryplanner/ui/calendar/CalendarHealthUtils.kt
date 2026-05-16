package com.vigilante.shiftsalaryplanner

import java.time.LocalDate
import java.time.YearMonth

data class CalendarMonthAudit(
    val emptyDayCount: Int,
    val multiWorkDayCount: Int,
    val overlappingWorkDayCount: Int
)

fun auditCalendarMonth(
    month: YearMonth,
    dayAssignmentsByDate: Map<LocalDate, List<CalendarDayAssignment>>,
    templateAlarmConfigs: Map<String, ShiftTemplateAlarmConfig>
): CalendarMonthAudit {
    var emptyDays = 0
    var multiWorkDays = 0
    var overlappingDays = 0
    var date = month.atDay(1)
    val endDate = month.atEndOfMonth()

    while (!date.isAfter(endDate)) {
        val assignments = dayAssignmentsByDate[date].orEmpty()
        if (assignments.isEmpty()) {
            emptyDays += 1
        }
        if (assignments.size > 1) {
            multiWorkDays += 1
            if (hasOverlappingAssignments(assignments, templateAlarmConfigs)) {
                overlappingDays += 1
            }
        }
        date = date.plusDays(1)
    }

    return CalendarMonthAudit(
        emptyDayCount = emptyDays,
        multiWorkDayCount = multiWorkDays,
        overlappingWorkDayCount = overlappingDays
    )
}

private fun hasOverlappingAssignments(
    assignments: List<CalendarDayAssignment>,
    templateAlarmConfigs: Map<String, ShiftTemplateAlarmConfig>
): Boolean {
    val intervals = assignments.mapNotNull { assignment ->
        val config = templateAlarmConfigs[assignment.shiftCode] ?: return@mapNotNull null
        val start = config.startHour.coerceIn(0, 23) * 60 + config.startMinute.coerceIn(0, 59)
        val rawEnd = config.endHour.coerceIn(0, 23) * 60 + config.endMinute.coerceIn(0, 59)
        val end = if (rawEnd <= start) rawEnd + 24 * 60 else rawEnd
        start to end
    }

    for (leftIndex in intervals.indices) {
        for (rightIndex in (leftIndex + 1) until intervals.size) {
            val left = intervals[leftIndex]
            val right = intervals[rightIndex]
            if (left.first < right.second && right.first < left.second) {
                return true
            }
        }
    }
    return false
}
