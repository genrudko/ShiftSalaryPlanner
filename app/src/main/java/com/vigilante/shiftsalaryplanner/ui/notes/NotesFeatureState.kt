package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.LocalDate

class NotesFeatureState(
    editingNoteId: String? = null,
    noteDraftDateIso: String = LocalDate.now().toString(),
    noteDraftWorkplaceId: String? = null,
    noteDraftShiftCode: String? = null
) {
    var editingNoteId by mutableStateOf(editingNoteId)
        private set
    var noteDraftDateIso by mutableStateOf(noteDraftDateIso)
        private set
    var noteDraftWorkplaceId by mutableStateOf(noteDraftWorkplaceId)
        private set
    var noteDraftShiftCode by mutableStateOf(noteDraftShiftCode)
        private set

    fun openEditor(
        noteId: String?,
        dateIso: String,
        workplaceId: String?,
        shiftCode: String?
    ) {
        editingNoteId = noteId
        noteDraftDateIso = dateIso
        noteDraftWorkplaceId = workplaceId
        noteDraftShiftCode = shiftCode
    }

    fun clearEditor() {
        editingNoteId = null
    }
}

private fun encodeNullableNoteValue(value: String?): String =
    if (value == null) "0" else "1$value"

private fun decodeNullableNoteValue(value: String?): String? = when {
    value == null || value == "0" -> null
    value.startsWith("1") -> value.substring(1)
    else -> null
}

internal fun NotesFeatureState.toSaveableStrings(): ArrayList<String> = arrayListOf(
    encodeNullableNoteValue(editingNoteId),
    noteDraftDateIso,
    encodeNullableNoteValue(noteDraftWorkplaceId),
    encodeNullableNoteValue(noteDraftShiftCode)
)

internal fun restoreNotesFeatureState(saved: List<String>): NotesFeatureState = NotesFeatureState(
    editingNoteId = decodeNullableNoteValue(saved.getOrNull(0)),
    noteDraftDateIso = saved.getOrNull(1) ?: LocalDate.now().toString(),
    noteDraftWorkplaceId = decodeNullableNoteValue(saved.getOrNull(2)),
    noteDraftShiftCode = decodeNullableNoteValue(saved.getOrNull(3))
)

val NotesFeatureStateSaver: Saver<NotesFeatureState, ArrayList<String>> = Saver(
    save = { state -> state.toSaveableStrings() },
    restore = { saved -> restoreNotesFeatureState(saved) }
)

@Composable
fun rememberNotesFeatureState(): NotesFeatureState = rememberSaveable(
    saver = NotesFeatureStateSaver
) {
    NotesFeatureState()
}
