package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class WeatherLocationPickerTargetKind { WORKPLACE, SAVED_NEW, SAVED_EDIT }

data class WeatherLocationPickerTarget(
    val kind: WeatherLocationPickerTargetKind,
    val id: String? = null,
)

class SettingsFeatureState(
    isHolidaySyncing: Boolean = false,
    holidaySyncMessage: String? = null,
    showManualHolidayDialog: Boolean = false,
    editingManualHolidayDate: String? = null,
    showWorkplaceRenameDialog: Boolean = false,
    customFontStatusMessage: String? = null,
    weatherLocationPickerTarget: WeatherLocationPickerTarget? = null,
) {
    var isHolidaySyncing by mutableStateOf(isHolidaySyncing)
        private set
    var holidaySyncMessage by mutableStateOf(holidaySyncMessage)
        private set
    var showManualHolidayDialog by mutableStateOf(showManualHolidayDialog)
        private set
    var editingManualHolidayDate by mutableStateOf(editingManualHolidayDate)
        private set
    var showWorkplaceRenameDialog by mutableStateOf(showWorkplaceRenameDialog)
        private set
    var customFontStatusMessage by mutableStateOf(customFontStatusMessage)
        private set
    var weatherLocationPickerTarget by mutableStateOf(weatherLocationPickerTarget)
        private set

    fun startHolidaySync(message: String? = null) {
        isHolidaySyncing = true
        if (message != null) holidaySyncMessage = message
    }

    fun updateHolidaySyncMessage(message: String?) { holidaySyncMessage = message }

    fun finishHolidaySync(message: String? = holidaySyncMessage) {
        holidaySyncMessage = message
        isHolidaySyncing = false
    }

    fun openNewManualHoliday() {
        editingManualHolidayDate = null
        showManualHolidayDialog = true
    }

    fun openManualHoliday(date: String) {
        editingManualHolidayDate = date
        showManualHolidayDialog = true
    }

    fun closeManualHoliday() {
        showManualHolidayDialog = false
        editingManualHolidayDate = null
    }

    fun openWorkplaceRename() { showWorkplaceRenameDialog = true }
    fun closeWorkplaceRename() { showWorkplaceRenameDialog = false }
    fun setCustomFontStatus(message: String?) { customFontStatusMessage = message }

    fun openWorkplaceWeatherLocation(workplaceId: String) {
        weatherLocationPickerTarget = WeatherLocationPickerTarget(WeatherLocationPickerTargetKind.WORKPLACE, workplaceId)
    }
    fun openNewSavedWeatherLocation() {
        weatherLocationPickerTarget = WeatherLocationPickerTarget(WeatherLocationPickerTargetKind.SAVED_NEW)
    }
    fun openSavedWeatherLocation(id: String) {
        weatherLocationPickerTarget = WeatherLocationPickerTarget(WeatherLocationPickerTargetKind.SAVED_EDIT, id)
    }
    fun closeWeatherLocationPicker() { weatherLocationPickerTarget = null }
}

private fun encodeNullableSettingsValue(value: String?): String = if (value == null) "0" else "1$value"
private fun decodeNullableSettingsValue(value: String?): String? = when {
    value == null || value == "0" -> null
    value.startsWith("1") -> value.substring(1)
    else -> null
}

internal fun SettingsFeatureState.toSaveableStrings(): ArrayList<String> = arrayListOf(
    if (isHolidaySyncing) "1" else "0",
    encodeNullableSettingsValue(holidaySyncMessage),
    if (showManualHolidayDialog) "1" else "0",
    encodeNullableSettingsValue(editingManualHolidayDate),
    if (showWorkplaceRenameDialog) "1" else "0",
    encodeNullableSettingsValue(customFontStatusMessage),
    encodeNullableSettingsValue(weatherLocationPickerTarget?.kind?.name),
    encodeNullableSettingsValue(weatherLocationPickerTarget?.id),
)

internal fun restoreSettingsFeatureState(saved: List<String>): SettingsFeatureState = SettingsFeatureState(
    isHolidaySyncing = saved.getOrNull(0) == "1",
    holidaySyncMessage = decodeNullableSettingsValue(saved.getOrNull(1)),
    showManualHolidayDialog = saved.getOrNull(2) == "1",
    editingManualHolidayDate = decodeNullableSettingsValue(saved.getOrNull(3)),
    showWorkplaceRenameDialog = saved.getOrNull(4) == "1",
    customFontStatusMessage = decodeNullableSettingsValue(saved.getOrNull(5)),
    weatherLocationPickerTarget = decodeNullableSettingsValue(saved.getOrNull(6))
        ?.let { rawKind -> runCatching { WeatherLocationPickerTargetKind.valueOf(rawKind) }.getOrNull() }
        ?.let { kind ->
            WeatherLocationPickerTarget(
                kind = kind,
                id = decodeNullableSettingsValue(saved.getOrNull(7)),
            )
        },
)

val SettingsFeatureStateSaver: Saver<SettingsFeatureState, ArrayList<String>> = Saver(
    save = { it.toSaveableStrings() },
    restore = { restoreSettingsFeatureState(it) }
)

@Composable
fun rememberSettingsFeatureState(): SettingsFeatureState = rememberSaveable(
    saver = SettingsFeatureStateSaver
) { SettingsFeatureState() }
