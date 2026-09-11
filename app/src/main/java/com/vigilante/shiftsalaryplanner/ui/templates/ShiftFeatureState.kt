package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class ShiftFeatureState(
    editingShiftTemplateCode: String? = null,
    creatingSystemStatus: Boolean = false,
    templateModeName: String = TemplateMode.SHIFTS.name
) {
    var editingShiftTemplateCode by mutableStateOf(editingShiftTemplateCode)
        private set
    var creatingSystemStatus by mutableStateOf(creatingSystemStatus)
        private set
    var templateModeName by mutableStateOf(templateModeName)
        private set

    fun setMode(mode: TemplateMode) {
        templateModeName = mode.name
    }

    fun openNewShift() {
        creatingSystemStatus = false
        editingShiftTemplateCode = null
    }

    fun openNewSystemStatus() {
        creatingSystemStatus = true
        editingShiftTemplateCode = null
    }

    fun openExistingShift(code: String, isSystemStatus: Boolean) {
        creatingSystemStatus = isSystemStatus
        editingShiftTemplateCode = code
    }

    fun clearEditingCode() {
        editingShiftTemplateCode = null
    }

    fun finishSpecialRuleSave() {
        creatingSystemStatus = false
    }

    fun clearEditor() {
        editingShiftTemplateCode = null
        creatingSystemStatus = false
    }
}

private fun encodeNullableShiftValue(value: String?): String =
    if (value == null) "0" else "1$value"

private fun decodeNullableShiftValue(value: String?): String? = when {
    value == null || value == "0" -> null
    value.startsWith("1") -> value.substring(1)
    else -> null
}

internal fun ShiftFeatureState.toSaveableStrings(): ArrayList<String> = arrayListOf(
    encodeNullableShiftValue(editingShiftTemplateCode),
    if (creatingSystemStatus) "1" else "0",
    templateModeName
)

internal fun restoreShiftFeatureState(saved: List<String>): ShiftFeatureState = ShiftFeatureState(
    editingShiftTemplateCode = decodeNullableShiftValue(saved.getOrNull(0)),
    creatingSystemStatus = saved.getOrNull(1) == "1",
    templateModeName = saved.getOrNull(2) ?: TemplateMode.SHIFTS.name
)

val ShiftFeatureStateSaver: Saver<ShiftFeatureState, ArrayList<String>> = Saver(
    save = { state -> state.toSaveableStrings() },
    restore = { saved -> restoreShiftFeatureState(saved) }
)

@Composable
fun rememberShiftFeatureState(): ShiftFeatureState = rememberSaveable(
    saver = ShiftFeatureStateSaver
) {
    ShiftFeatureState()
}
