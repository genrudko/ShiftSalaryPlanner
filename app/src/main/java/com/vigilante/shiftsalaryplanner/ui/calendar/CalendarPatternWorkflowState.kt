package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class CalendarPatternWorkflowState(
    showPatternListDialog: Boolean = false,
    showPatternEditDialog: Boolean = false,
    editingPatternId: String? = null,
    showPatternApplyDialog: Boolean = false,
    applyingPatternId: String? = null,
    showPatternQuickPicker: Boolean = false,
    activePatternId: String? = null,
    patternRangeStartIso: String? = null,
    pendingPatternRangeStartIso: String? = null,
    pendingPatternRangeEndIso: String? = null,
    showPatternPreviewDialog: Boolean = false,
    clearRangeModeActive: Boolean = false,
    clearRangeStartIso: String? = null,
    pendingClearRangeStartIso: String? = null,
    pendingClearRangeEndIso: String? = null,
    showClearMonthConfirm: Boolean = false,
    showClearAllCalendarConfirm: Boolean = false
) {
    var showPatternListDialog by mutableStateOf(showPatternListDialog)
    var showPatternEditDialog by mutableStateOf(showPatternEditDialog)
    var editingPatternId by mutableStateOf(editingPatternId)
    var showPatternApplyDialog by mutableStateOf(showPatternApplyDialog)
    var applyingPatternId by mutableStateOf(applyingPatternId)
    var showPatternQuickPicker by mutableStateOf(showPatternQuickPicker)
    var activePatternId by mutableStateOf(activePatternId)
    var patternRangeStartIso by mutableStateOf(patternRangeStartIso)
    var pendingPatternRangeStartIso by mutableStateOf(pendingPatternRangeStartIso)
    var pendingPatternRangeEndIso by mutableStateOf(pendingPatternRangeEndIso)
    var showPatternPreviewDialog by mutableStateOf(showPatternPreviewDialog)
    var clearRangeModeActive by mutableStateOf(clearRangeModeActive)
    var clearRangeStartIso by mutableStateOf(clearRangeStartIso)
    var pendingClearRangeStartIso by mutableStateOf(pendingClearRangeStartIso)
    var pendingClearRangeEndIso by mutableStateOf(pendingClearRangeEndIso)
    var showClearMonthConfirm by mutableStateOf(showClearMonthConfirm)
    var showClearAllCalendarConfirm by mutableStateOf(showClearAllCalendarConfirm)

    fun beginCreatePattern() {
        editingPatternId = null
        showPatternEditDialog = true
    }

    fun beginEditPattern(patternId: String) {
        editingPatternId = patternId
        showPatternEditDialog = true
    }

    fun closePatternEditor() {
        showPatternEditDialog = false
        editingPatternId = null
    }

    fun beginApplyPattern(patternId: String) {
        applyingPatternId = patternId
        showPatternApplyDialog = true
    }

    fun closePatternApply() {
        showPatternApplyDialog = false
        applyingPatternId = null
    }

    fun selectQuickPattern(patternId: String) {
        activePatternId = patternId
        patternRangeStartIso = null
        showPatternQuickPicker = false
    }

    fun openPatternManagerFromQuickPicker() {
        showPatternQuickPicker = false
        showPatternListDialog = true
    }

    fun beginClearRangeMode() {
        activePatternId = null
        patternRangeStartIso = null
        pendingPatternRangeStartIso = null
        pendingPatternRangeEndIso = null
        clearRangeModeActive = true
        clearRangeStartIso = null
        pendingClearRangeStartIso = null
        pendingClearRangeEndIso = null
    }

    fun resetClearRangeMode() {
        clearRangeModeActive = false
        clearRangeStartIso = null
        pendingClearRangeStartIso = null
        pendingClearRangeEndIso = null
    }

    fun closePatternPreview() {
        showPatternPreviewDialog = false
        pendingPatternRangeStartIso = null
        pendingPatternRangeEndIso = null
    }
}

private fun encodeNullableString(value: String?): String =
    if (value == null) "0" else "1$value"

private fun decodeNullableString(value: String?): String? = when {
    value == null || value == "0" -> null
    value.startsWith("1") -> value.substring(1)
    else -> null
}

internal fun CalendarPatternWorkflowState.toSaveableStrings(): ArrayList<String> = arrayListOf(
    if (showPatternListDialog) "1" else "0",
    if (showPatternEditDialog) "1" else "0",
    encodeNullableString(editingPatternId),
    if (showPatternApplyDialog) "1" else "0",
    encodeNullableString(applyingPatternId),
    if (showPatternQuickPicker) "1" else "0",
    encodeNullableString(activePatternId),
    encodeNullableString(patternRangeStartIso),
    encodeNullableString(pendingPatternRangeStartIso),
    encodeNullableString(pendingPatternRangeEndIso),
    if (showPatternPreviewDialog) "1" else "0",
    if (clearRangeModeActive) "1" else "0",
    encodeNullableString(clearRangeStartIso),
    encodeNullableString(pendingClearRangeStartIso),
    encodeNullableString(pendingClearRangeEndIso),
    if (showClearMonthConfirm) "1" else "0",
    if (showClearAllCalendarConfirm) "1" else "0"
)

internal fun restoreCalendarPatternWorkflowState(saved: List<String>): CalendarPatternWorkflowState {
    fun flag(index: Int): Boolean = saved.getOrNull(index) == "1"
    return CalendarPatternWorkflowState(
        showPatternListDialog = flag(0),
        showPatternEditDialog = flag(1),
        editingPatternId = decodeNullableString(saved.getOrNull(2)),
        showPatternApplyDialog = flag(3),
        applyingPatternId = decodeNullableString(saved.getOrNull(4)),
        showPatternQuickPicker = flag(5),
        activePatternId = decodeNullableString(saved.getOrNull(6)),
        patternRangeStartIso = decodeNullableString(saved.getOrNull(7)),
        pendingPatternRangeStartIso = decodeNullableString(saved.getOrNull(8)),
        pendingPatternRangeEndIso = decodeNullableString(saved.getOrNull(9)),
        showPatternPreviewDialog = flag(10),
        clearRangeModeActive = flag(11),
        clearRangeStartIso = decodeNullableString(saved.getOrNull(12)),
        pendingClearRangeStartIso = decodeNullableString(saved.getOrNull(13)),
        pendingClearRangeEndIso = decodeNullableString(saved.getOrNull(14)),
        showClearMonthConfirm = flag(15),
        showClearAllCalendarConfirm = flag(16)
    )
}

val CalendarPatternWorkflowStateSaver: Saver<CalendarPatternWorkflowState, ArrayList<String>> = Saver(
    save = { state -> state.toSaveableStrings() },
    restore = { saved -> restoreCalendarPatternWorkflowState(saved) }
)

@Composable
fun rememberCalendarPatternWorkflowState(): CalendarPatternWorkflowState = rememberSaveable(
    saver = CalendarPatternWorkflowStateSaver
) {
    CalendarPatternWorkflowState()
}
