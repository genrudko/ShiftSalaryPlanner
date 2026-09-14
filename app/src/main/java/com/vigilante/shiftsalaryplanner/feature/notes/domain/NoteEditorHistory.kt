package com.vigilante.shiftsalaryplanner.feature.notes.domain

data class NoteEditorHistoryState(
    val current: NoteDocument,
    val undoStack: List<NoteDocument> = emptyList(),
    val redoStack: List<NoteDocument> = emptyList(),
    val maxUndoEntries: Int = 50,
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}

object NoteEditorHistory {
    fun start(document: NoteDocument, maxUndoEntries: Int = 50) = NoteEditorHistoryState(document, maxUndoEntries = maxUndoEntries.coerceAtLeast(1))

    fun commit(state: NoteEditorHistoryState, next: NoteDocument): NoteEditorHistoryState {
        if (next == state.current) return state
        return state.copy(
            current = next,
            undoStack = (state.undoStack + state.current).takeLast(state.maxUndoEntries),
            redoStack = emptyList(),
        )
    }

    fun undo(state: NoteEditorHistoryState): NoteEditorHistoryState {
        val previous = state.undoStack.lastOrNull() ?: return state
        return state.copy(
            current = previous,
            undoStack = state.undoStack.dropLast(1),
            redoStack = (state.redoStack + state.current).takeLast(state.maxUndoEntries),
        )
    }

    fun redo(state: NoteEditorHistoryState): NoteEditorHistoryState {
        val next = state.redoStack.lastOrNull() ?: return state
        return state.copy(
            current = next,
            undoStack = (state.undoStack + state.current).takeLast(state.maxUndoEntries),
            redoStack = state.redoStack.dropLast(1),
        )
    }
}
