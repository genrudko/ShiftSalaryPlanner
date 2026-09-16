package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.compat.LegacyNotesMirror
import com.vigilante.shiftsalaryplanner.feature.notes.compat.NotesV2Repository
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteContext
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteRevisionStore
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2RevisionBackend
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteDocumentJsonCodec
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Store
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreBackend
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreState
import com.vigilante.shiftsalaryplanner.settings.AppNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2RevisionStoreTest {
    private class FakeRevisionBackend(
        var raw: String? = null,
        var failWrites: Boolean = false,
    ) : NotesV2RevisionBackend {
        var writeCount = 0
        override fun readRaw(): String? = raw
        override fun writeRaw(raw: String) {
            if (failWrites) error("revision disk full")
            writeCount += 1
            this.raw = raw
        }
    }

    private class FakeNotesBackend(var raw: String? = null) : NotesV2StoreBackend {
        override fun readRaw(): String? = raw
        override fun writeRaw(raw: String) { this.raw = raw }
    }

    private class FakeMirror : LegacyNotesMirror {
        var notes: List<AppNote> = emptyList()
        override fun replaceAll(notes: List<AppNote>) { this.notes = notes }
    }

    private fun document(title: String, updatedAt: Long = 1L) = NoteDocument(
        id = "n1",
        title = title,
        context = NoteContext(dateIso = "2026-09-16"),
        createdAtMillis = 1L,
        updatedAtMillis = updatedAt,
    )

    @Test
    fun `revision history is durable deduplicated and bounded per note`() {
        val backend = FakeRevisionBackend()
        val store = NoteRevisionStore(backend, maxRevisionsPerNote = 2)
        val v1 = document("v1", 1L)
        val v2 = document("v2", 2L)
        val v3 = document("v3", 3L)

        assertTrue(store.capture(v1, capturedAtMillis = 10L))
        assertTrue(store.capture(v1, capturedAtMillis = 11L))
        assertEquals(1, backend.writeCount)
        assertTrue(store.capture(v2, capturedAtMillis = 20L))
        assertTrue(store.capture(v3, capturedAtMillis = 30L))
        assertEquals(listOf("v2", "v3"), store.revisions("n1").map { it.document.title })

        val restored = NoteRevisionStore(backend, maxRevisionsPerNote = 2)
        assertEquals(listOf("v2", "v3"), restored.revisions("n1").map { it.document.title })
        assertEquals(listOf(20L, 30L), restored.revisions("n1").map { it.capturedAtMillis })
    }

    @Test
    fun `editor save captures session baseline once before overwriting current document`() {
        val baseline = document("before", 2L)
        val notesBackend = FakeNotesBackend(NoteDocumentJsonCodec.encodeList(listOf(baseline)).toString())
        val store = NotesV2Store(notesBackend) { emptyList() }
        val revisionBackend = FakeRevisionBackend()
        val revisions = NoteRevisionStore(revisionBackend)
        val repository = NotesV2Repository(store, FakeMirror(), revisionStore = revisions)

        val firstAutosave = baseline.copy(title = "after 1", updatedAtMillis = 3L)
        assertTrue(repository.saveFromEditor(firstAutosave, revisionBaseline = baseline, capturedAtMillis = 100L))
        assertEquals(listOf("before"), revisions.revisions("n1").map { it.document.title })

        val secondAutosave = firstAutosave.copy(title = "after 2", updatedAtMillis = 4L)
        assertTrue(repository.saveFromEditor(secondAutosave, revisionBaseline = baseline, capturedAtMillis = 110L))
        assertEquals(listOf("before"), revisions.revisions("n1").map { it.document.title })
        assertEquals("after 2", (store.currentState as NotesV2StoreState.Ready).documents.single().title)
    }

    @Test
    fun `failed revision capture prevents editor overwrite so baseline is not lost`() {
        val baseline = document("before", 2L)
        val notesBackend = FakeNotesBackend(NoteDocumentJsonCodec.encodeList(listOf(baseline)).toString())
        val store = NotesV2Store(notesBackend) { emptyList() }
        val revisionBackend = FakeRevisionBackend(failWrites = true)
        val repository = NotesV2Repository(store, FakeMirror(), revisionStore = NoteRevisionStore(revisionBackend))

        assertFalse(repository.saveFromEditor(
            baseline.copy(title = "must not overwrite", updatedAtMillis = 3L),
            revisionBaseline = baseline,
            capturedAtMillis = 100L,
        ))
        assertEquals("before", (store.currentState as NotesV2StoreState.Ready).documents.single().title)
    }

    @Test
    fun `permanent delete clears revision history`() {
        val baseline = document("before", 2L)
        val notesBackend = FakeNotesBackend(NoteDocumentJsonCodec.encodeList(listOf(baseline)).toString())
        val store = NotesV2Store(notesBackend) { emptyList() }
        val revisionBackend = FakeRevisionBackend()
        val revisions = NoteRevisionStore(revisionBackend)
        assertTrue(revisions.capture(baseline, 10L))
        val repository = NotesV2Repository(store, FakeMirror(), revisionStore = revisions)

        assertTrue(repository.deletePermanently("n1"))
        assertTrue(revisions.revisions("n1").isEmpty())
    }
}
