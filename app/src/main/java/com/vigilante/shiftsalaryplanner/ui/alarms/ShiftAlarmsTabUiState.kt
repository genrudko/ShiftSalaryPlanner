package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

data class ShiftAlarmsTabUiState(
    val enabled: Boolean,
    val autoReschedule: Boolean,
    val scheduleHorizonDaysText: String,
    val templateConfigs: List<ShiftTemplateAlarmConfig>,
    val ringShowCurrentClock: Boolean,
    val ringShowDate: Boolean,
    val ringPulseAccent: Boolean,
    val ringAnimatedGradient: Boolean,
    val ringAnimationMode: ShiftAlarmRingAnimationMode,
    val ringAnimationStyle: ShiftAlarmRingAnimationStyle,
    val ringVisualStyle: ShiftAlarmRingVisualStyle,
    val ringActionStyle: ShiftAlarmRingActionStyle,
    val ringButtonsLayout: ShiftAlarmRingButtonsLayout,
    val ringClockAlignment: ShiftAlarmRingClockAlignment,
    val ringClockScale: Float,
    val ringTextScale: Float,
    val ringUseMonospaceClock: Boolean,
    val ringShowMetaInfo: Boolean,
    val ringShowSoundLabel: Boolean,
    val ringShowVolumeInfo: Boolean,
    val ringShowTimezoneInfo: Boolean,
    val editingTemplateCode: String? = null,
    val editingAlarm: ShiftAlarmConfig? = null,
    val showAlarmDialog: Boolean = false
) {
    companion object {
        fun from(
            settings: ShiftAlarmSettings,
            shiftTemplates: List<ShiftTemplateEntity>
        ): ShiftAlarmsTabUiState {
            return ShiftAlarmsTabUiState(
                enabled = settings.enabled,
                autoReschedule = settings.autoReschedule,
                scheduleHorizonDaysText = settings.scheduleHorizonDays.toString(),
                templateConfigs = mergeShiftAlarmConfigsWithTemplates(settings, shiftTemplates),
                ringShowCurrentClock = settings.ringUi.showCurrentClock,
                ringShowDate = settings.ringUi.showDate,
                ringPulseAccent = settings.ringUi.pulseAccent,
                ringAnimatedGradient = settings.ringUi.animatedGradient,
                ringAnimationMode = settings.ringUi.animationMode,
                ringAnimationStyle = settings.ringUi.animationStyle,
                ringVisualStyle = settings.ringUi.visualStyle,
                ringActionStyle = settings.ringUi.actionStyle,
                ringButtonsLayout = settings.ringUi.buttonsLayout,
                ringClockAlignment = settings.ringUi.clockAlignment,
                ringClockScale = settings.ringUi.clockScale,
                ringTextScale = settings.ringUi.textScale,
                ringUseMonospaceClock = settings.ringUi.useMonospaceClock,
                ringShowMetaInfo = settings.ringUi.showMetaInfo,
                ringShowSoundLabel = settings.ringUi.showSoundLabel,
                ringShowVolumeInfo = settings.ringUi.showVolumeInfo,
                ringShowTimezoneInfo = settings.ringUi.showTimezoneInfo
            )
        }
    }
}

sealed interface ShiftAlarmsTabUiAction {
    data class SetEnabled(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetAutoReschedule(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetScheduleHorizonDaysText(val value: String) : ShiftAlarmsTabUiAction
    data class SetTemplateConfigs(val value: List<ShiftTemplateAlarmConfig>) : ShiftAlarmsTabUiAction
    data class SetRingShowCurrentClock(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingShowDate(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingPulseAccent(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingAnimatedGradient(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingAnimationMode(val value: ShiftAlarmRingAnimationMode) : ShiftAlarmsTabUiAction
    data class SetRingAnimationStyle(val value: ShiftAlarmRingAnimationStyle) : ShiftAlarmsTabUiAction
    data class SetRingVisualStyle(val value: ShiftAlarmRingVisualStyle) : ShiftAlarmsTabUiAction
    data class SetRingActionStyle(val value: ShiftAlarmRingActionStyle) : ShiftAlarmsTabUiAction
    data class SetRingButtonsLayout(val value: ShiftAlarmRingButtonsLayout) : ShiftAlarmsTabUiAction
    data class SetRingClockAlignment(val value: ShiftAlarmRingClockAlignment) : ShiftAlarmsTabUiAction
    data class SetRingClockScale(val value: Float) : ShiftAlarmsTabUiAction
    data class SetRingTextScale(val value: Float) : ShiftAlarmsTabUiAction
    data class SetRingUseMonospaceClock(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingShowMetaInfo(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingShowSoundLabel(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingShowVolumeInfo(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetRingShowTimezoneInfo(val value: Boolean) : ShiftAlarmsTabUiAction
    data class StartEditing(
        val templateCode: String,
        val alarm: ShiftAlarmConfig
    ) : ShiftAlarmsTabUiAction

    data object StopEditing : ShiftAlarmsTabUiAction
}

fun reduceShiftAlarmsTabUiState(
    state: ShiftAlarmsTabUiState,
    action: ShiftAlarmsTabUiAction
): ShiftAlarmsTabUiState {
    return when (action) {
        is ShiftAlarmsTabUiAction.SetEnabled -> state.copy(enabled = action.value)
        is ShiftAlarmsTabUiAction.SetAutoReschedule -> state.copy(autoReschedule = action.value)
        is ShiftAlarmsTabUiAction.SetScheduleHorizonDaysText -> state.copy(scheduleHorizonDaysText = action.value)
        is ShiftAlarmsTabUiAction.SetTemplateConfigs -> state.copy(templateConfigs = action.value)
        is ShiftAlarmsTabUiAction.SetRingShowCurrentClock -> state.copy(ringShowCurrentClock = action.value)
        is ShiftAlarmsTabUiAction.SetRingShowDate -> state.copy(ringShowDate = action.value)
        is ShiftAlarmsTabUiAction.SetRingPulseAccent -> state.copy(ringPulseAccent = action.value)
        is ShiftAlarmsTabUiAction.SetRingAnimatedGradient -> state.copy(ringAnimatedGradient = action.value)
        is ShiftAlarmsTabUiAction.SetRingAnimationMode -> state.copy(ringAnimationMode = action.value)
        is ShiftAlarmsTabUiAction.SetRingAnimationStyle -> state.copy(ringAnimationStyle = action.value)
        is ShiftAlarmsTabUiAction.SetRingVisualStyle -> state.copy(ringVisualStyle = action.value)
        is ShiftAlarmsTabUiAction.SetRingActionStyle -> state.copy(ringActionStyle = action.value)
        is ShiftAlarmsTabUiAction.SetRingButtonsLayout -> state.copy(ringButtonsLayout = action.value)
        is ShiftAlarmsTabUiAction.SetRingClockAlignment -> state.copy(ringClockAlignment = action.value)
        is ShiftAlarmsTabUiAction.SetRingClockScale -> state.copy(ringClockScale = action.value.coerceIn(0.8f, 1.4f))
        is ShiftAlarmsTabUiAction.SetRingTextScale -> state.copy(ringTextScale = action.value.coerceIn(0.85f, 1.35f))
        is ShiftAlarmsTabUiAction.SetRingUseMonospaceClock -> state.copy(ringUseMonospaceClock = action.value)
        is ShiftAlarmsTabUiAction.SetRingShowMetaInfo -> state.copy(ringShowMetaInfo = action.value)
        is ShiftAlarmsTabUiAction.SetRingShowSoundLabel -> state.copy(ringShowSoundLabel = action.value)
        is ShiftAlarmsTabUiAction.SetRingShowVolumeInfo -> state.copy(ringShowVolumeInfo = action.value)
        is ShiftAlarmsTabUiAction.SetRingShowTimezoneInfo -> state.copy(ringShowTimezoneInfo = action.value)
        is ShiftAlarmsTabUiAction.StartEditing -> state.copy(
            editingTemplateCode = action.templateCode,
            editingAlarm = action.alarm,
            showAlarmDialog = true
        )

        ShiftAlarmsTabUiAction.StopEditing -> state.copy(
            editingTemplateCode = null,
            editingAlarm = null,
            showAlarmDialog = false
        )
    }
}
