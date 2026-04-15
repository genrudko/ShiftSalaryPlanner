package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

fun mergeShiftAlarmConfigsWithTemplates(
    settings: ShiftAlarmSettings,
    templates: List<ShiftTemplateEntity>
): List<ShiftTemplateAlarmConfig> {
    val existingByCode = settings.templateConfigs.associateBy { it.shiftCode }
    return templates
        .sortedBy { it.sortOrder }
        .map { template ->
            existingByCode[template.code] ?: defaultShiftTemplateAlarmConfig(template)
        }
}

fun upsertShiftTemplateAlarmConfig(
    items: List<ShiftTemplateAlarmConfig>,
    updated: ShiftTemplateAlarmConfig
): List<ShiftTemplateAlarmConfig> {
    val mutable = items.toMutableList()
    val index = mutable.indexOfFirst { it.shiftCode == updated.shiftCode }
    if (index >= 0) {
        mutable[index] = updated
    } else {
        mutable.add(updated)
    }
    return mutable
}

fun upsertShiftAlarmItem(
    items: List<ShiftAlarmConfig>,
    updated: ShiftAlarmConfig
): List<ShiftAlarmConfig> {
    val mutable = items.toMutableList()
    val index = mutable.indexOfFirst { it.id == updated.id }
    if (index >= 0) {
        mutable[index] = updated
    } else {
        mutable.add(updated)
    }
    return mutable
}
