package com.vigilante.shiftsalaryplanner

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProvider
import androidx.core.content.edit

fun saveManualHoliday(
    manualHolidayRecords: SnapshotStateList<ManualHolidayRecord>,
    manualHolidayPrefs: SharedPreferences,
    record: ManualHolidayRecord
) {
    val existingIndex = manualHolidayRecords.indexOfFirst { it.date == record.date }
    if (existingIndex >= 0) {
        manualHolidayRecords[existingIndex] = record
    } else {
        manualHolidayRecords.add(record)
    }

    val sorted = manualHolidayRecords.sortedBy { it.date }
    manualHolidayRecords.clear()
    manualHolidayRecords.addAll(sorted)

    writeManualHolidayRecords(manualHolidayPrefs, manualHolidayRecords.toList())
}

fun deleteManualHoliday(
    manualHolidayRecords: SnapshotStateList<ManualHolidayRecord>,
    manualHolidayPrefs: SharedPreferences,
    date: String
) {
    manualHolidayRecords.removeAll { it.date == date }
    writeManualHolidayRecords(manualHolidayPrefs, manualHolidayRecords.toList())
}

fun saveShiftColor(
    shiftColors: SnapshotStateMap<String, Int>,
    shiftColorsPrefs: SharedPreferences,
    context: Context,
    key: String,
    colorValue: Int
) {
    shiftColors[key] = colorValue
    shiftColorsPrefs.edit { putInt(key, colorValue) }
    ShiftMonthWidgetProvider.requestUpdate(context)
}

fun resetShiftColors(
    shiftColors: SnapshotStateMap<String, Int>,
    shiftColorsPrefs: SharedPreferences,
    context: Context
) {
    val defaults = defaultShiftColors()
    defaults.forEach { (key, value) ->
        saveShiftColor(
            shiftColors = shiftColors,
            shiftColorsPrefs = shiftColorsPrefs,
            context = context,
            key = key,
            colorValue = value
        )
    }
}

fun saveShiftSpecialRule(
    shiftSpecialRules: SnapshotStateMap<String, ShiftSpecialRule>,
    shiftSpecialPrefs: SharedPreferences,
    code: String,
    rule: ShiftSpecialRule
) {
    shiftSpecialRules[code] = rule
    writeShiftSpecialRule(shiftSpecialPrefs, code, rule)
}

fun removeShiftSpecialRule(
    shiftSpecialRules: SnapshotStateMap<String, ShiftSpecialRule>,
    shiftSpecialPrefs: SharedPreferences,
    code: String
) {
    shiftSpecialRules.remove(code)
    deleteShiftSpecialRule(shiftSpecialPrefs, code)
}