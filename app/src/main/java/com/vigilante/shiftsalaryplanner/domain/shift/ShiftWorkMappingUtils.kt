package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayCompensation
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayType
import com.vigilante.shiftsalaryplanner.payroll.WorkShiftItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

fun shiftTemplateSubtitle(template: ShiftTemplateEntity): String {
    fun fixed2(value: Double): String = String.format(Locale.US, "%.2f", value)
    return buildString {
        append("Оплач. ")
        append(fixed2(template.paidHours()))
        append(" ч")
        if (template.breakHours > 0.0) {
            append(" • Обед ")
            append(fixed2(template.breakHours))
            append(" ч")
        }
        if (template.nightHours > 0.0) {
            append(" • Ночь ")
            append(fixed2(template.nightHours))
            append(" ч")
        }
    }
}

data class MonthSummary(
    val workedDays: Int,
    val workedHours: Double,
    val nightHours: Double
)

fun calculateSummary(
    shiftCodesByDate: Map<LocalDate, String>,
    month: YearMonth,
    templateMap: Map<String, ShiftTemplateEntity>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    applyShortDayReduction: Boolean,
    shiftTimingsByCode: Map<String, ShiftTemplateAlarmConfig> = emptyMap()
): MonthSummary {
    val monthShiftItems = shiftCodesByDate
        .filterKeys { YearMonth.from(it) == month }
        .mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = holidayMap,
                applyShortDayReduction = applyShortDayReduction,
                shiftTiming = shiftTimingsByCode[code]
            )
        }

    return calculateSummaryForShifts(monthShiftItems)
}

fun calculateSummaryForShifts(shifts: List<WorkShiftItem>): MonthSummary {
    return MonthSummary(
        workedDays = shifts.count { it.paidHours > 0.0 },
        workedHours = shifts.sumOf { it.paidHours },
        nightHours = shifts.sumOf { it.nightHours }
    )
}

fun ShiftTemplateEntity.paidHours(): Double = max(0.0, totalHours - breakHours)

fun ShiftTemplateEntity.toWorkShiftItem(
    specialRule: ShiftSpecialRule? = null
): WorkShiftItem {
    val paid = paidHours()
    val normalizedCode = code.trim().uppercase()
    val normalizedTitle = title.trim().uppercase()
    val isVacation = iconKey == "OT" ||
        normalizedCode in setOf("ОТ", "ОТП", "ОТПУСК") ||
        "ОТПУ" in normalizedTitle
    val isSickLeave = iconKey == "SICK" ||
        normalizedCode in setOf("Б", "БЛ", "БОЛ", "БОЛЬН") ||
        "БОЛЬН" in normalizedTitle
    val resolvedSpecialDayType = resolveSpecialDayType(specialRule, isWeekendPaid)
    val resolvedSpecialDayCompensation = resolveSpecialDayCompensation(specialRule, isWeekendPaid)
    val legacyWeekendPaid = legacyWeekendPaidFlag(resolvedSpecialDayType, resolvedSpecialDayCompensation)

    return WorkShiftItem(
        paidHours = if (isVacation || isSickLeave) 0.0 else paid,
        nightHours = if (isVacation || isSickLeave) 0.0 else nightHours.coerceAtMost(paid),
        isWeekendPaid = if (isVacation || isSickLeave) false else legacyWeekendPaid,
        specialDayType = if (isVacation || isSickLeave) SpecialDayType.NONE.name else resolvedSpecialDayType.name,
        specialDayCompensation = if (isVacation || isSickLeave) SpecialDayCompensation.NONE.name else resolvedSpecialDayCompensation.name,
        isVacation = isVacation,
        isSickLeave = isSickLeave,
        shiftPayAmount = if (isVacation || isSickLeave) 0.0 else shiftPayAmount
    )
}

fun ShiftTemplateEntity.toWorkShiftItemForDate(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>,
    applyShortDayReduction: Boolean,
    specialRule: ShiftSpecialRule? = null,
    shiftTiming: ShiftTemplateAlarmConfig? = null,
    dayOverride: ShiftDayEntity? = null
): WorkShiftItem {
    val normalizedCode = code.trim().uppercase()
    val normalizedTitle = title.trim().uppercase()
    val isVacation = iconKey == "OT" ||
        normalizedCode in setOf("ОТ", "ОТП", "ОТПУСК") ||
        "ОТПУ" in normalizedTitle
    val isSickLeave = iconKey == "SICK" ||
        normalizedCode in setOf("Б", "БЛ", "БОЛ", "БОЛЬН") ||
        "БОЛЬН" in normalizedTitle
    val resolvedSpecialDayType = resolveSpecialDayType(specialRule, isWeekendPaid)
    val resolvedSpecialDayCompensation = resolveSpecialDayCompensation(specialRule, isWeekendPaid)
    val holiday = holidayMap[date]

    val overrideStartTime = parseTimeOrNull(dayOverride?.overrideStartTime)
    val overrideEndTime = parseTimeOrNull(dayOverride?.overrideEndTime)
    val timingStartTime = shiftTiming?.let {
        LocalTime.of(it.startHour.coerceIn(0, 23), it.startMinute.coerceIn(0, 59))
    }
    val estimatedStartTime = overrideStartTime ?: timingStartTime ?: if (nightHours > 0.0) {
        LocalTime.of(20, 0)
    } else {
        LocalTime.of(8, 0)
    }
    val overrideTotalHours = nonNegativeOrNull(dayOverride?.overrideTotalHours)
        ?: if (overrideStartTime != null && overrideEndTime != null) {
            durationHours(overrideStartTime, overrideEndTime)
        } else {
            null
        }
    val baseTotalHours = overrideTotalHours ?: totalHours
    val baseBreakHours = dayOverride?.overrideBreakHours?.coerceAtLeast(0.0) ?: breakHours
    val basePaid = nonNegativeOrNull(dayOverride?.overridePaidHours)
        ?: (baseTotalHours - baseBreakHours).coerceAtLeast(0.0)
    val baseNightHours = (dayOverride?.overrideNightHours?.coerceAtLeast(0.0) ?: nightHours)
        .coerceAtMost(basePaid)
    val baseShiftPayAmount = dayOverride?.overrideShiftPayAmount?.coerceAtLeast(0.0) ?: shiftPayAmount

    val baseHolidayPaidHours = calculateHolidayOverlapHours(
        shiftDate = date,
        shiftStartTime = estimatedStartTime,
        paidHours = basePaid,
        holidayMap = holidayMap
    )
    val hasHolidayOverlap = baseHolidayPaidHours > 0.0

    val effectiveSpecialDayType = when {
        resolvedSpecialDayType != SpecialDayType.NONE -> resolvedSpecialDayType
        hasHolidayOverlap -> SpecialDayType.WEEKEND_HOLIDAY
        else -> SpecialDayType.NONE
    }
    val effectiveSpecialDayCompensation = when {
        resolvedSpecialDayType != SpecialDayType.NONE -> resolvedSpecialDayCompensation
        hasHolidayOverlap -> SpecialDayCompensation.DOUBLE_PAY
        else -> SpecialDayCompensation.NONE
    }
    val legacyWeekendPaid = legacyWeekendPaidFlag(
        effectiveSpecialDayType,
        effectiveSpecialDayCompensation
    )

    if (isVacation || isSickLeave) {
        return WorkShiftItem(
            paidHours = 0.0,
            nightHours = 0.0,
            isWeekendPaid = false,
            date = date,
            specialDayType = SpecialDayType.NONE.name,
            specialDayCompensation = SpecialDayCompensation.NONE.name,
            isVacation = isVacation,
            isSickLeave = isSickLeave,
            shiftPayAmount = 0.0
        )
    }

    val isShortDay = holiday?.kind == HolidayKinds.SHORT_DAY
    val reductionHours = if (
        applyShortDayReduction &&
        isShortDay &&
        effectiveSpecialDayType == SpecialDayType.NONE &&
        basePaid > 0.0
    ) 1.0 else 0.0

    val adjustedPaidHours = (basePaid - reductionHours).coerceAtLeast(0.0)
    val adjustedNightHours = baseNightHours.coerceAtMost(adjustedPaidHours)
    val adjustedHolidayPaidHours = calculateHolidayOverlapHours(
        shiftDate = date,
        shiftStartTime = estimatedStartTime,
        paidHours = adjustedPaidHours,
        holidayMap = holidayMap
    ).coerceAtMost(adjustedPaidHours)

    return WorkShiftItem(
        paidHours = adjustedPaidHours,
        nightHours = adjustedNightHours,
        isWeekendPaid = legacyWeekendPaid,
        date = date,
        specialDayType = effectiveSpecialDayType.name,
        specialDayCompensation = effectiveSpecialDayCompensation.name,
        isVacation = false,
        isSickLeave = false,
        holidayPaidHours = adjustedHolidayPaidHours.takeIf { it > 0.0 },
        shiftPayAmount = baseShiftPayAmount
    )
}

private fun parseTimeOrNull(value: String?): LocalTime? {
    val text = value?.trim().orEmpty()
    if (text.isBlank()) return null
    return runCatching { LocalTime.parse(text) }.getOrNull()
}

private fun nonNegativeOrNull(value: Double?): Double? =
    value?.takeIf { it >= 0.0 }

private fun durationHours(start: LocalTime, end: LocalTime): Double {
    val startDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), start)
    val rawEndDateTime = LocalDateTime.of(LocalDate.of(2000, 1, 1), end)
    val endDateTime = if (rawEndDateTime.isAfter(startDateTime)) {
        rawEndDateTime
    } else {
        rawEndDateTime.plusDays(1)
    }
    return Duration.between(startDateTime, endDateTime).toMinutes() / 60.0
}

private fun calculateHolidayOverlapHours(
    shiftDate: LocalDate,
    shiftStartTime: LocalTime,
    paidHours: Double,
    holidayMap: Map<LocalDate, HolidayEntity>
): Double {
    val totalMinutes = (paidHours.coerceAtLeast(0.0) * 60.0).roundToInt().coerceAtLeast(0)
    if (totalMinutes <= 0) return 0.0

    var cursor = LocalDateTime.of(shiftDate, shiftStartTime)
    var remainingMinutes = totalMinutes
    var holidayMinutes = 0

    while (remainingMinutes > 0) {
        val dayEnd = cursor.toLocalDate().plusDays(1).atStartOfDay()
        val untilDayEnd = Duration.between(cursor, dayEnd).toMinutes().toInt().coerceAtLeast(1)
        val chunkMinutes = minOf(remainingMinutes, untilDayEnd)

        val day = cursor.toLocalDate()
        val holiday = holidayMap[day]
        val isPaidHoliday = holiday?.isNonWorking == true && holiday.kind == HolidayKinds.HOLIDAY
        if (isPaidHoliday) holidayMinutes += chunkMinutes

        cursor = cursor.plusMinutes(chunkMinutes.toLong())
        remainingMinutes -= chunkMinutes
    }

    return holidayMinutes / 60.0
}

