package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
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

    return MonthSummary(
        workedDays = monthShiftItems.count { it.paidHours > 0.0 },
        workedHours = monthShiftItems.sumOf { it.paidHours },
        nightHours = monthShiftItems.sumOf { it.nightHours }
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
        isSickLeave = isSickLeave
    )
}

fun ShiftTemplateEntity.toWorkShiftItemForDate(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>,
    applyShortDayReduction: Boolean,
    specialRule: ShiftSpecialRule? = null,
    shiftTiming: ShiftTemplateAlarmConfig? = null
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

    val estimatedStartTime = shiftTiming?.let {
        LocalTime.of(it.startHour.coerceIn(0, 23), it.startMinute.coerceIn(0, 59))
    } ?: if (nightHours > 0.0) {
        LocalTime.of(20, 0)
    } else {
        LocalTime.of(8, 0)
    }

    val baseHolidayPaidHours = calculateHolidayOverlapHours(
        shiftDate = date,
        shiftStartTime = estimatedStartTime,
        paidHours = paidHours(),
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
            isSickLeave = isSickLeave
        )
    }

    val basePaid = paidHours()
    val isShortDay = holiday?.kind == HolidayKinds.SHORT_DAY
    val reductionHours = if (
        applyShortDayReduction &&
        isShortDay &&
        effectiveSpecialDayType == SpecialDayType.NONE &&
        basePaid > 0.0
    ) 1.0 else 0.0

    val adjustedPaidHours = (basePaid - reductionHours).coerceAtLeast(0.0)
    val adjustedNightHours = nightHours.coerceAtMost(adjustedPaidHours)
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
        holidayPaidHours = adjustedHolidayPaidHours.takeIf { it > 0.0 }
    )
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
        val isNonWorkingHoliday = holiday?.isNonWorking == true && holiday.kind != HolidayKinds.SHORT_DAY
        if (isNonWorkingHoliday) holidayMinutes += chunkMinutes

        cursor = cursor.plusMinutes(chunkMinutes.toLong())
        remainingMinutes -= chunkMinutes
    }

    return holidayMinutes / 60.0
}

