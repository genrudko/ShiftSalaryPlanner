package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteDocumentJsonCodec
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Bootstrap
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2BootstrapResult
import com.vigilante.shiftsalaryplanner.settings.AppNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2BootstrapTest {
    @Test
    fun `missing v2 payload migrates every legacy note and requests first persist`() {
        val legacy = listOf(
            AppNote(id = "a", date = "2026-09-14", title = "A", body = "one", createdAtMillis = 1L, updatedAtMillis = 2L),
            AppNote(id = "b", date = "2026-09-15", title = "B", body = "two", createdAtMillis = 3L, updatedAtMillis = 4L),
        )

        val result = NotesV2Bootstrap.resolve(existingV2Raw = null, legacyNotes = legacy)

        val ready = result as NotesV2BootstrapResult.Ready
        assertEquals(listOf("a", "b"), ready.documents.map { it.id })
        assertTrue(ready.shouldPersist)
    }

    @Test
    fun `valid existing v2 payload wins over stale legacy and is not rewritten`() {
        val v2 = NoteDocument(id = "v2", title = "New", createdAtMillis = 10L, updatedAtMillis = 20L)
        val raw = NoteDocumentJsonCodec.encodeList(listOf(v2)).toString()
        val legacy = listOf(AppNote(id = "legacy", date = "2026-09-14", title = "Old"))

        val result = NotesV2Bootstrap.resolve(existingV2Raw = raw, legacyNotes = legacy)

        val ready = result as NotesV2BootstrapResult.Ready
        assertEquals(listOf("v2"), ready.documents.map { it.id })
        assertFalse(ready.shouldPersist)
    }

    @Test
    fun `valid empty v2 payload remains authoritative and does not resurrect legacy notes`() {
        val legacy = listOf(AppNote(id = "legacy", date = "2026-09-14", title = "Deleted in v2"))

        val result = NotesV2Bootstrap.resolve(existingV2Raw = "[]", legacyNotes = legacy)

        val ready = result as NotesV2BootstrapResult.Ready
        assertTrue(ready.documents.isEmpty())
        assertFalse(ready.shouldPersist)
    }

    @Test
    fun `malformed existing v2 payload fails closed instead of overwriting from legacy`() {
        val legacy = listOf(AppNote(id = "legacy", date = "2026-09-14", title = "Old"))

        val result = NotesV2Bootstrap.resolve(existingV2Raw = "not-json", legacyNotes = legacy)

        assertTrue(result is NotesV2BootstrapResult.Corrupt)
    }

    @Test
    fun `future schema payload fails closed instead of downgrading to legacy`() {
        val futureRaw = """[{"schemaVersion":999,"id":"future","title":"Future","blocks":[],"labels":[],"appearance":{},"context":{},"pinned":false,"archived":false,"deletedAtMillis":null,"reminderAtMillis":null,"createdAtMillis":1,"updatedAtMillis":2}]"""
        val legacy = listOf(AppNote(id = "legacy", date = "2026-09-14", title = "Old"))

        val result = NotesV2Bootstrap.resolve(existingV2Raw = futureRaw, legacyNotes = legacy)

        assertTrue(result is NotesV2BootstrapResult.UnsupportedSchema)
    }
}
