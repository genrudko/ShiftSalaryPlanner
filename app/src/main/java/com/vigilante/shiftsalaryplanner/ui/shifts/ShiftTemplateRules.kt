package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.util.Locale

val SYSTEM_SHIFT_CODES = setOf("ВЫХ", "ОТ", "Б")
fun ShiftTemplateEntity.isSystemShiftTemplate(): Boolean = code in SYSTEM_SHIFT_CODES

fun ShiftTemplateEntity.isAlarmEligibleShiftTemplate(): Boolean = !isSystemShiftTemplate()

fun List<ShiftTemplateEntity>.alarmEligibleTemplates(): List<ShiftTemplateEntity> =
    filter { it.isAlarmEligibleShiftTemplate() }.sortedBy { it.sortOrder }
fun legacyBuiltInWorkingShiftTemplates(): List<ShiftTemplateEntity> = listOf(
    ShiftTemplateEntity(
        code = "Д",
        title = "Дневная",
        iconKey = "SUN",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 0.0,
        colorHex = "#1E88E5",
        isWeekendPaid = false,
        active = true,
        sortOrder = 10
    ),
    ShiftTemplateEntity(
        code = "Н",
        title = "Ночная",
        iconKey = "MOON",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 8.0,
        colorHex = "#43A047",
        isWeekendPaid = false,
        active = true,
        sortOrder = 20
    ),
    ShiftTemplateEntity(
        code = "8",
        title = "8 СП",
        iconKey = "EIGHT",
        totalHours = 8.0,
        breakHours = 0.0,
        nightHours = 0.0,
        colorHex = "#EF5350",
        isWeekendPaid = false,
        active = true,
        sortOrder = 30
    ),
    ShiftTemplateEntity(
        code = "РВД",
        title = "Работа в выходной день",
        iconKey = "STAR",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 0.0,
        colorHex = "#66BB6A",
        isWeekendPaid = true,
        active = true,
        sortOrder = 70
    ),
    ShiftTemplateEntity(
        code = "РВН",
        title = "Работа в выходной день (ночь)",
        iconKey = "STAR",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 8.0,
        colorHex = "#BDBDBD",
        isWeekendPaid = true,
        active = true,
        sortOrder = 80
    )
)

val LEGACY_BUILT_IN_WORKING_SHIFT_BY_CODE =
    legacyBuiltInWorkingShiftTemplates().associateBy { it.code }

fun ShiftTemplateEntity.matchesLegacyBuiltInWorkingTemplate(): Boolean {
    val legacy = LEGACY_BUILT_IN_WORKING_SHIFT_BY_CODE[code] ?: return false
    return title == legacy.title &&
            iconKey == legacy.iconKey &&
            totalHours.nearlyEquals(legacy.totalHours) &&
            breakHours.nearlyEquals(legacy.breakHours) &&
            nightHours.nearlyEquals(legacy.nightHours) &&
            colorHex.equals(legacy.colorHex, ignoreCase = true) &&
            isWeekendPaid == legacy.isWeekendPaid &&
            active == legacy.active &&
            sortOrder == legacy.sortOrder
}

fun ShiftTemplateAlarmConfig?.isMeaningfullyCustomizedFor(template: ShiftTemplateEntity): Boolean {
    if (this == null) return false
    return this != defaultShiftTemplateAlarmConfig(template)
}
fun isProtectedSystemTemplate(template: ShiftTemplateEntity?): Boolean {
    if (template == null) return false

    val code = template.code.trim().uppercase(Locale.ROOT)
    val title = template.title.trim().uppercase(Locale.ROOT)

    return code in setOf("ВЫХ", "ОТ", "Б") ||
            title in setOf("ВЫХОДНОЙ", "ОТПУСК", "БОЛЬНИЧНЫЙ")
}