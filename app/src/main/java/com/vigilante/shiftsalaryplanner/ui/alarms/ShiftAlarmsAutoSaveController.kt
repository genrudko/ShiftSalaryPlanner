package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

fun buildNormalizedShiftAlarmSettings(
    uiState: ShiftAlarmsTabUiState,
    fallbackHorizonDays: Int
): ShiftAlarmSettings {
    return normalizeShiftAlarmSettings(
        ShiftAlarmSettings(
            enabled = uiState.enabled,
            autoReschedule = uiState.autoReschedule,
            scheduleHorizonDays = parseInt(uiState.scheduleHorizonDaysText, fallbackHorizonDays).coerceIn(7, 365),
            templateConfigs = uiState.templateConfigs
        )
    )
}

@Composable
fun ShiftAlarmsAutoSaveEffect(
    settingsToSave: ShiftAlarmSettings,
    initialSettings: ShiftAlarmSettings,
    onSave: (ShiftAlarmSettings) -> Unit,
    debounceMs: Long = 800
) {
    var lastAutoSavedSettings by remember(initialSettings) {
        mutableStateOf(normalizeShiftAlarmSettings(initialSettings))
    }

    LaunchedEffect(settingsToSave) {
        delay(debounceMs)
        if (settingsToSave != lastAutoSavedSettings) {
            lastAutoSavedSettings = settingsToSave
            onSave(settingsToSave)
        }
    }
}
