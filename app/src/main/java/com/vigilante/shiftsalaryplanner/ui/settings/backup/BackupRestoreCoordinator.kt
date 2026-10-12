package com.vigilante.shiftsalaryplanner

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.room.withTransaction
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProviderV2
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
    val mediaRoot = java.io.File(
        context.filesDir,
        com.vigilante.shiftsalaryplanner.feature.notes.media.AndroidNoteMediaImporter.MEDIA_ROOT_NAME,
    )
    val prepared = context.contentResolver.openInputStream(uri)?.use { input ->
        com.vigilante.shiftsalaryplanner.feature.notes.backup.NotesV2BackupPackageCodec.prepareJsonAndMedia(
            input = input,
            mediaRoot = mediaRoot,
            validateJson = { parseAppBackupJson(it) },
        )
    } ?: throw IllegalStateException("Не удалось прочитать файл")

    prepared.use { staged ->
        restoreBackupFromRawJson(
            context = context,
            rawJson = staged.backupJson,
            existingShiftTemplates = existingShiftTemplates,
            existingSavedDays = existingSavedDays,
            manualHolidayPrefs = manualHolidayPrefs,
            shiftColorsPrefs = shiftColorsPrefs,
            manualHolidayRecords = manualHolidayRecords,
            shiftColors = shiftColors,
            upsertShiftTemplate = upsertShiftTemplate,
            deleteShiftTemplate = deleteShiftTemplate,
            upsertShiftDay = upsertShiftDay,
            deleteShiftDayByDate = deleteShiftDayByDate,
            onStatus = {}, // Completion is not announced until Notes media is installed.
            onAfterImport = {},
        )
        staged.install()
        onStatus("Резервная копия восстановлена. Экран будет обновлён.")
        onAfterImport()
    }
}

suspend fun restoreBackupFromRawJson(
    context: Context,
    rawJson: String,
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
    val backupData = parseAppBackupJson(rawJson)

    // A failed DAO call must not leave the calendar half-deleted. Snapshot the
    // preferences so they can be reverted on an ordinary (non-process-death) failure.
    val previousPreferences = backupData.sharedPrefs.keys.associateWith { name ->
        sharedPreferencesToJson(context.profileSharedPreferences(name))
    }
    try {
        AppDatabase.getDatabase(context).withTransaction {
            backupData.sharedPrefs.forEach { (name, snapshot) ->
                applySharedPreferencesSnapshot(context.profileSharedPreferences(name), snapshot)
            }

            val importedShiftCodes = backupData.shiftTemplates.map { it.code }.toSet()
            backupData.shiftTemplates.forEach { template -> upsertShiftTemplate(template) }
            existingShiftTemplates
                .filter { it.code !in importedShiftCodes }
                .forEach { template -> deleteShiftTemplate(template) }

            val importedDates = backupData.shiftDays.map { it.date }.toSet()
            backupData.shiftDays.forEach { day -> upsertShiftDay(day) }
            existingSavedDays
                .filter { it.date !in importedDates }
                .forEach { day -> deleteShiftDayByDate(day.date) }
        }
    } catch (failure: Throwable) {
        previousPreferences.forEach { (name, snapshot) ->
            runCatching {
                applySharedPreferencesSnapshot(context.profileSharedPreferences(name), snapshot)
            }.onFailure { failure.addSuppressed(it) }
        }
        throw failure
    }

    manualHolidayRecords.clear()
    manualHolidayRecords.addAll(readManualHolidayRecords(manualHolidayPrefs))

    shiftColors.clear()
    val defaults = defaultShiftColors()
    defaults.forEach { (key, value) ->
        shiftColors[key] = shiftColorsPrefs.getInt(key, value)
    }

    ShiftMonthWidgetProviderV2.requestUpdate(context)

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
