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
            templateConfigs = uiState.templateConfigs,
            ringUi = ShiftAlarmRingUiSettings(
                showCurrentClock = uiState.ringShowCurrentClock,
                showDate = uiState.ringShowDate,
                pulseAccent = uiState.ringPulseAccent,
                animatedGradient = uiState.ringAnimatedGradient,
                animationMode = uiState.ringAnimationMode,
                animationStyle = uiState.ringAnimationStyle,
                visualStyle = uiState.ringVisualStyle,
                actionStyle = uiState.ringActionStyle,
                buttonsLayout = uiState.ringButtonsLayout,
                clockAlignment = uiState.ringClockAlignment,
                clockScale = uiState.ringClockScale,
                textScale = uiState.ringTextScale,
                useMonospaceClock = uiState.ringUseMonospaceClock,
                showMetaInfo = uiState.ringShowMetaInfo,
                showSoundLabel = uiState.ringShowSoundLabel,
                showVolumeInfo = uiState.ringShowVolumeInfo,
                showTimezoneInfo = uiState.ringShowTimezoneInfo
            ),
            behavior = ShiftAlarmBehaviorSettings(
                vibrationEnabled = uiState.behaviorVibrationEnabled,
                vibrationType = uiState.behaviorVibrationType,
                vibrationDurationSeconds = parseInt(uiState.behaviorVibrationDurationSecondsText, 25).coerceIn(0, 300),
                customVibrationPattern = uiState.behaviorCustomVibrationPattern.trim(),
                snoozeIntervalMinutes = parseInt(uiState.behaviorSnoozeIntervalMinutesText, 10).coerceIn(1, 120),
                snoozeCountLimit = parseInt(uiState.behaviorSnoozeCountLimitText, 3).coerceIn(0, 10),
                ringDurationSeconds = (parseInt(uiState.behaviorRingDurationMinutesText, 3).coerceIn(1, 60) * 60).coerceIn(10, 3_600),
                rampUpDurationSeconds = parseInt(uiState.behaviorRampUpDurationSecondsText, 0).coerceIn(0, 180),
                defaultSoundUri = uiState.behaviorDefaultSoundUri?.takeIf { it.isNotBlank() },
                defaultSoundLabel = uiState.behaviorDefaultSoundLabel.trim()
            )
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
