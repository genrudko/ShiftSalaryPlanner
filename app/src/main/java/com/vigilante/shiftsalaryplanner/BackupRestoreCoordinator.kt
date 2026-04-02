package com.vigilante.shiftsalaryplanner

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProvider
import kotlinx.coroutines.delay

fun buildBackupJsonForExport(
    prefSnapshots: List<Pair<String, SharedPreferences>>,
    shiftDays: List<ShiftDayEntity>,
    shiftTemplates: List<ShiftTemplateEntity>
): String {
    return exportAppBackupJson(
        prefSnapshots = prefSnapshots,
        shiftDays = shiftDays,
        shiftTemplates = shiftTemplates
    )
}

suspend fun restoreBackupFromUri(
    context: Context,
    uri: Uri,
    existingShiftTemplates: List<ShiftTemplateEntity>,
    existingSavedDays: List<ShiftDayEntity>,
    manualHolidayPrefs: SharedPreferences,
    shiftColorsPrefs: SharedPreferences,
    manualHolidayRecords: SnapshotStateList<ManualHolidayRecord>,
    shiftColors: SnapshotStateMap<String, Int>,
    upsertShiftTemplate: suspend (ShiftTemplateEntity) -> Unit,
    deleteShiftTemplate: suspend (ShiftTemplateEntity) -> Unit,
    upsertShiftDay: suspend (ShiftDayEntity) -> Unit,
    deleteShiftDayByDate: suspend (String) -> Unit,
    onStatus: (String) -> Unit,
    onAfterImport: () -> Unit
) {
    val raw = context.contentResolver.openInputStream(uri)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
        ?: throw IllegalStateException("Не удалось прочитать файл")

    val backupData = parseAppBackupJson(raw)

    backupData.sharedPrefs.forEach { (name, snapshot) ->
        val targetPrefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        applySharedPreferencesSnapshot(targetPrefs, snapshot)
    }

    val importedShiftCodes = backupData.shiftTemplates.map { it.code }.toSet()
    backupData.shiftTemplates.forEach { template ->
        upsertShiftTemplate(template)
    }
    existingShiftTemplates
        .filter { it.code !in importedShiftCodes }
        .forEach { template ->
            deleteShiftTemplate(template)
        }

    val importedDates = backupData.shiftDays.map { it.date }.toSet()
    backupData.shiftDays.forEach { day ->
        upsertShiftDay(day)
    }
    existingSavedDays
        .filter { it.date !in importedDates }
        .forEach { day ->
            deleteShiftDayByDate(day.date)
        }

    manualHolidayRecords.clear()
    manualHolidayRecords.addAll(readManualHolidayRecords(manualHolidayPrefs))

    shiftColors.clear()
    val defaults = defaultShiftColors()
    defaults.forEach { (key, value) ->
        shiftColors[key] = shiftColorsPrefs.getInt(key, value)
    }

    ShiftMonthWidgetProvider.requestUpdate(context)

    onStatus(
        buildString {
            append("Восстановление завершено. Смен: ")
            append(backupData.shiftDays.size)
            append(" • шаблонов: ")
            append(backupData.shiftTemplates.size)
            append(". Экран будет обновлён.")
        }
    )

    delay(200)
    onAfterImport()
}