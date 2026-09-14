package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteDocumentJsonCodec
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Store
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreBackend
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreState
import com.vigilante.shiftsalaryplanner.settings.AppNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2StoreTest {
    private class FakeBackend(
        var raw: String? = null,
        var failWrites: Boolean = false,
    ) : NotesV2StoreBackend {
        var writeCount: Int = 0
        override fun readRaw(): String? = raw
        override fun writeRaw(raw: String) {
            writeCount += 1
            if (failWrites) error("disk full")
            this.raw = raw
        }
    }

    @Test
    fun `first bootstrap persists migrated legacy once without modifying legacy source`() {
        val legacy = mutableListOf(
            AppNote(id = "legacy", date = "2026-09-14", title = "Old", body = "Body", createdAtMillis = 1L, updatedAtMillis = 2L)
        )
        val backend = FakeBackend()

        val store = NotesV2Store(backend) { legacy.toList() }

        val ready = store.currentState as NotesV2StoreState.Ready
        assertEquals(listOf("legacy"), ready.documents.map { it.id })
        assertEquals(1, backend.writeCount)
        assertEquals(listOf("legacy"), NoteDocumentJsonCodec.decodeList(backend.raw!!).map { it.id })
        assertEquals(1, legacy.size)
        assertEquals("Old", legacy.single().title)
    }

    @Test
    fun `existing v2 payload is authoritative and does not rewrite storage`() {
        val existing = NoteDocument(id = "v2", title = "New", createdAtMillis = 1L, updatedAtMillis = 2L)
        val backend = FakeBackend(NoteDocumentJsonCodec.encodeList(listOf(existing)).toString())

        val store = NotesV2Store(backend) { listOf(AppNote(id = "legacy", date = "2026-09-14", title = "Old")) }

        assertEquals(listOf("v2"), (store.currentState as NotesV2StoreState.Ready).documents.map { it.id })
        assertEquals(0, backend.writeCount)
    }

    @Test
    fun `corrupt and future payloads fail closed and refuse save`() {
        val corruptBackend = FakeBackend("not-json")
        val corrupt = NotesV2Store(corruptBackend) { listOf(AppNote(id = "legacy", date = "2026-09-14")) }
        assertTrue(corrupt.currentState is NotesV2StoreState.Corrupt)
        assertFalse(corrupt.save(NoteDocument(id = "new", title = "New", createdAtMillis = 1L, updatedAtMillis = 2L)))
        assertEquals(0, corruptBackend.writeCount)

        val futureBackend = FakeBackend("""[{"schemaVersion":999,"id":"future"}]""")
        val future = NotesV2Store(futureBackend) { emptyList() }
        assertTrue(future.currentState is NotesV2StoreState.UnsupportedSchema)
        assertFalse(future.save(NoteDocument(id = "new", title = "New", createdAtMillis = 1L, updatedAtMillis = 2L)))
        assertEquals(0, futureBackend.writeCount)
    }

    @Test
    fun `save replaces by id only after durable write succeeds`() {
        val first = NoteDocument(id = "n1", title = "Before", createdAtMillis = 1L, updatedAtMillis = 2L)
        val backend = FakeBackend(NoteDocumentJsonCodec.encodeList(listOf(first)).toString())
        val store = NotesV2Store(backend) { emptyList() }
        val replacement = first.copy(title = "After", updatedAtMillis = 3L)

        assertTrue(store.save(replacement))
        assertEquals("After", (store.currentState as NotesV2StoreState.Ready).documents.single().title)
        assertEquals("After", NoteDocumentJsonCodec.decodeList(backend.raw!!).single().title)

        backend.failWrites = true
        val failed = replacement.copy(title = "Must not appear", updatedAtMillis = 4L)
        assertFalse(store.save(failed))
        assertEquals("After", (store.currentState as NotesV2StoreState.Ready).documents.single().title)
    }

    @Test
    fun `failed first migration persist exposes persistence error instead of false ready`() {
        val backend = FakeBackend(failWrites = true)

        val store = NotesV2Store(backend) {
            listOf(AppNote(id = "legacy", date = "2026-09-14", title = "Old"))
        }

        assertTrue(store.currentState is NotesV2StoreState.PersistenceError)
        assertEquals(1, backend.writeCount)
    }
}
