package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryAction
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryActions
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class M14NotesV2LibraryActionsTest {
    @Test
    fun `active notes expose archive and trash actions`() {
        assertEquals(
            listOf(NoteLibraryAction.ARCHIVE, NoteLibraryAction.MOVE_TO_TRASH),
            NoteLibraryActions.availableFor(NoteLibraryLocation.ACTIVE),
        )
    }

    @Test
    fun `archive exposes unarchive and trash actions`() {
        assertEquals(
            listOf(NoteLibraryAction.UNARCHIVE, NoteLibraryAction.MOVE_TO_TRASH),
            NoteLibraryActions.availableFor(NoteLibraryLocation.ARCHIVE),
        )
    }

    @Test
    fun `trash exposes restore and permanent delete actions`() {
        assertEquals(
            listOf(NoteLibraryAction.RESTORE, NoteLibraryAction.DELETE_PERMANENTLY),
            NoteLibraryActions.availableFor(NoteLibraryLocation.TRASH),
        )
    }
}
