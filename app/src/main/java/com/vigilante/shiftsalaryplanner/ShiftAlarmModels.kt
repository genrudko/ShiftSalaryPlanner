package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.util.UUID

data class ShiftAlarmConfig(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val triggerHour: Int = 7,
    val triggerMinute: Int = 0,
    val volumePercent: Int = 100,
    val soundUri: String? = null,
    val soundLabel: String = "",
    val enabled: Boolean = true
)

data class ShiftTemplateAlarmConfig(
    val shiftCode: String,
    val enabled: Boolean = false,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 20,
    val endMinute: Int = 0,
    val alarms: List<ShiftAlarmConfig> = emptyList()
)

data class ShiftAlarmSettings(
    val enabled: Boolean = false,
    val autoReschedule: Boolean = true,
    val scheduleHorizonDays: Int = 90,
    val templateConfigs: List<ShiftTemplateAlarmConfig> = emptyList()
)

data class ShiftAlarmRescheduleResult(
    val scheduledCount: Int = 0,
    val cancelledCount: Int = 0,
    val skippedPastCount: Int = 0,
    val skippedNoTemplateCount: Int = 0,
    val skippedNoConfigCount: Int = 0,
    val usedInexactFallback: Boolean = false,
    val message: String = ""
)

fun formatClockHm(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

fun shiftAlarmTemplateLabel(template: ShiftTemplateEntity): String {
    return buildString {
        append(template.code)
        if (template.title.isNotBlank() && template.title != template.code) {
            append(" — ")
            append(template.title)
        }
    }
}

fun shiftAlarmSoundSummary(alarm: ShiftAlarmConfig): String {
    return when {
        alarm.soundLabel.isNotBlank() -> alarm.soundLabel
        !alarm.soundUri.isNullOrBlank() -> "Свой файл"
        else -> "Системная мелодия"
    }
}

fun defaultShiftAlarmTitle(templateLabel: String, triggerHour: Int, triggerMinute: Int): String {
    return "$templateLabel • ${formatClockHm(triggerHour, triggerMinute)}"
}

fun resolveAlarmClockFromShiftStart(
    startHour: Int,
    startMinute: Int,
    minutesBefore: Int
): Pair<Int, Int> {
    val startTotalMinutes = startHour.coerceIn(0, 23) * 60 + startMinute.coerceIn(0, 59)
    val triggerTotalMinutes = ((startTotalMinutes - minutesBefore) % (24 * 60) + (24 * 60)) % (24 * 60)
    return triggerTotalMinutes / 60 to triggerTotalMinutes % 60
}

fun defaultShiftTemplateAlarmConfig(template: ShiftTemplateEntity): ShiftTemplateAlarmConfig {
    val isNight = template.nightHours > 0.0
    val defaultMinutes = if (isNight) 90 else 60
    val startHour = if (isNight) 20 else 8
    val startMinute = 0
    val (triggerHour, triggerMinute) = resolveAlarmClockFromShiftStart(
        startHour = startHour,
        startMinute = startMinute,
        minutesBefore = defaultMinutes
    )
    val shiftMinutes = (((template.totalHours.takeIf { it > 0.0 } ?: 12.0) * 60.0).toInt()).coerceAtLeast(0)
    val endTotalMinutes = startHour * 60 + startMinute + shiftMinutes
    val endHour = ((endTotalMinutes / 60) % 24 + 24) % 24
    val endMinute = ((endTotalMinutes % 60) + 60) % 60
    return ShiftTemplateAlarmConfig(
        shiftCode = template.code,
        enabled = false,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        alarms = listOf(
            ShiftAlarmConfig(
                title = defaultShiftAlarmTitle(shiftAlarmTemplateLabel(template), triggerHour, triggerMinute),
                triggerHour = triggerHour,
                triggerMinute = triggerMinute,
                volumePercent = 100,
                soundUri = null,
                soundLabel = "",
                enabled = true
            )
        )
    )
}
