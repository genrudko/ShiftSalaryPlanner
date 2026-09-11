package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotesFeatureStateTest {
    @Test
    fun `open editor replaces complete note draft atomically`() {
        val state = NotesFeatureState(
            noteDraftDateIso = "2026-09-01"
        )

        state.openEditor(
            noteId = "n1",
            dateIso = "2026-09-11",
            workplaceId = "work-2",
            shiftCode = "N"
        )

        assertEquals("n1", state.editingNoteId)
        assertEquals("2026-09-11", state.noteDraftDateIso)
        assertEquals("work-2", state.noteDraftWorkplaceId)
        assertEquals("N", state.noteDraftShiftCode)
    }

    @Test
    fun `clear editor clears identity but keeps draft context until next open`() {
        val state = NotesFeatureState(noteDraftDateIso = "2026-09-01")
        state.openEditor("n1", "2026-09-11", "work-2", "N")

        state.clearEditor()

        assertNull(state.editingNoteId)
        assertEquals("2026-09-11", state.noteDraftDateIso)
        assertEquals("work-2", state.noteDraftWorkplaceId)
        assertEquals("N", state.noteDraftShiftCode)
    }

    @Test
    fun `saveable representation restores all four note fields`() {
        val original = NotesFeatureState(noteDraftDateIso = "2026-09-01")
        original.openEditor("note,7", "2026-09-12", "work-3", "D")

        val restored = restoreNotesFeatureState(original.toSaveableStrings())

        assertEquals(original.toSaveableStrings(), restored.toSaveableStrings())
    }
}
