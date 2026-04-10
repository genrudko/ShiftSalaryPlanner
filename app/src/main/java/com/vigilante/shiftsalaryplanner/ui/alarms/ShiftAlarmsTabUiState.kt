package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

data class ShiftAlarmsTabUiState(
    val enabled: Boolean,
    val autoReschedule: Boolean,
    val scheduleHorizonDaysText: String,
    val templateConfigs: List<ShiftTemplateAlarmConfig>,
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
                templateConfigs = mergeShiftAlarmConfigsWithTemplates(settings, shiftTemplates)
            )
        }
    }
}

sealed interface ShiftAlarmsTabUiAction {
    data class SetEnabled(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetAutoReschedule(val value: Boolean) : ShiftAlarmsTabUiAction
    data class SetScheduleHorizonDaysText(val value: String) : ShiftAlarmsTabUiAction
    data class SetTemplateConfigs(val value: List<ShiftTemplateAlarmConfig>) : ShiftAlarmsTabUiAction
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
