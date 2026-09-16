package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.compat.LegacyNoteProjector
import com.vigilante.shiftsalaryplanner.feature.notes.compat.LegacyNotesMirror
import com.vigilante.shiftsalaryplanner.feature.notes.compat.NotesV2Repository
import com.vigilante.shiftsalaryplanner.feature.notes.domain.ChecklistItem
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteAppearance
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteContext
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteMediaPresentation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteTextPresentation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextMark
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextSpan
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteDocumentJsonCodec
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Store
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreBackend
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreState
import com.vigilante.shiftsalaryplanner.settings.AppNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2CompatibilityTest {
    private class FakeBackend(var raw: String? = null, var failWrites: Boolean = false) : NotesV2StoreBackend {
        override fun readRaw(): String? = raw
        override fun writeRaw(raw: String) {
            if (failWrites) error("disk full")
            this.raw = raw
        }
    }

    private class FakeMirror(initial: List<AppNote> = emptyList()) : LegacyNotesMirror {
        var notes: List<AppNote> = initial
        var replaceCount: Int = 0
        override fun replaceAll(notes: List<AppNote>) {
            replaceCount += 1
            this.notes = notes
        }
    }

    @Test
    fun `mixed active dated document projects to legacy without losing order context checklist or attachments`() {
        val body = StyledText("Проверить документы", listOf(TextSpan(0, 19, setOf(TextMark.BOLD))))
        val textPresentation = NoteTextPresentation(textSizeMode = "LARGE", fontMode = "SERIF")
        val mediaPresentation = NoteMediaPresentation(displaySizeMode = "LARGE", aspectRatio = 1.5f, createdAtMillis = 15L)
        val document = NoteDocument(
            id = "n1",
            title = "Смена",
            blocks = listOf(
                NoteBlock.Paragraph("p1", body, textPresentation),
                NoteBlock.Image("img-1", "file:///img.jpg", "Фото.jpg", mediaPresentation),
                NoteBlock.Paragraph("p2", StyledText("После фото")),
                NoteBlock.Checklist("c1", listOf(ChecklistItem("i1", StyledText("Позвонить"), checked = true))),
                NoteBlock.Audio("aud-1", "file:///voice.m4a", "Голос.m4a", mediaPresentation),
            ),
            appearance = NoteAppearance("#DDF6EE", "GRID"),
            context = NoteContext("2026-09-16", "main", "Д"),
            createdAtMillis = 10L,
            updatedAtMillis = 20L,
        )

        val legacy = LegacyNoteProjector.project(document)!!

        assertEquals("n1", legacy.id)
        assertEquals("2026-09-16", legacy.date)
        assertEquals("main", legacy.workplaceId)
        assertEquals("Д", legacy.shiftCode)
        assertEquals("Проверить документы\n[[media:img-1]]\nПосле фото", legacy.body)
        assertEquals(listOf("Позвонить"), legacy.checklist.map { it.text })
        assertTrue(legacy.checklist.single().checked)
        assertEquals(listOf("img-1", "aud-1"), legacy.attachments.map { it.id })
        assertEquals("#DDF6EE", legacy.colorHex)
        assertEquals("GRID", legacy.backgroundPattern)
        assertEquals("LARGE", legacy.textSizeMode)
        assertEquals("SERIF", legacy.fontMode)
        assertTrue(legacy.bodyBold)
        assertEquals(10L, legacy.createdAtMillis)
        assertEquals(20L, legacy.updatedAtMillis)
    }

    @Test
    fun `archive trash and undated documents are excluded from legacy projection`() {
        val base = NoteDocument(id = "n", title = "N", context = NoteContext(dateIso = "2026-09-16"), createdAtMillis = 1L, updatedAtMillis = 2L)
        assertNull(LegacyNoteProjector.project(base.copy(archived = true)))
        assertNull(LegacyNoteProjector.project(base.copy(deletedAtMillis = 3L)))
        assertNull(LegacyNoteProjector.project(base.copy(context = NoteContext())))
    }

    @Test
    fun `repository mirrors only after successful durable v2 write and trash removes legacy projection`() {
        val initial = NoteDocument(
            id = "n1", title = "Before", context = NoteContext(dateIso = "2026-09-16"), createdAtMillis = 1L, updatedAtMillis = 2L,
        )
        val backend = FakeBackend(NoteDocumentJsonCodec.encodeList(listOf(initial)).toString())
        val store = NotesV2Store(backend) { emptyList() }
        val mirror = FakeMirror()
        val repository = NotesV2Repository(store, mirror)

        assertEquals(listOf("n1"), mirror.notes.map { it.id })
        val changed = initial.copy(title = "After", updatedAtMillis = 3L)
        assertTrue(repository.save(changed))
        assertEquals("After", mirror.notes.single().title)

        backend.failWrites = true
        assertFalse(repository.save(changed.copy(title = "Must not mirror", updatedAtMillis = 4L)))
        assertEquals("After", mirror.notes.single().title)

        backend.failWrites = false
        assertTrue(repository.moveToTrash("n1", deletedAtMillis = 5L))
        assertTrue(mirror.notes.isEmpty())
        val v2 = (store.currentState as NotesV2StoreState.Ready).documents.single()
        assertEquals(5L, v2.deletedAtMillis)
    }

    @Test
    fun `permanent delete runs managed media cleanup only after durable v2 delete`() {
        val note = NoteDocument(id = "gc-note", title = "GC", createdAtMillis = 1L, updatedAtMillis = 2L)
        val backend = FakeBackend(NoteDocumentJsonCodec.encodeList(listOf(note)).toString())
        val store = NotesV2Store(backend) { emptyList() }
        val mirror = FakeMirror()
        val cleaned = mutableListOf<String>()
        val repository = NotesV2Repository(store, mirror) { document -> cleaned += document.id }

        backend.failWrites = true
        assertFalse(repository.deletePermanently("gc-note"))
        assertTrue(cleaned.isEmpty())

        backend.failWrites = false
        assertTrue(repository.deletePermanently("gc-note"))
        assertEquals(listOf("gc-note"), cleaned)
    }

    @Test
    fun `permanent delete removes v2 document and legacy mirror only after durable write`() {
        val first = NoteDocument(id = "n1", title = "One", context = NoteContext(dateIso = "2026-09-16"), createdAtMillis = 1L, updatedAtMillis = 2L)
        val second = NoteDocument(id = "n2", title = "Two", context = NoteContext(dateIso = "2026-09-17"), createdAtMillis = 1L, updatedAtMillis = 2L)
        val backend = FakeBackend(NoteDocumentJsonCodec.encodeList(listOf(first, second)).toString())
        val store = NotesV2Store(backend) { emptyList() }
        val mirror = FakeMirror()
        val repository = NotesV2Repository(store, mirror)

        assertTrue(repository.deletePermanently("n1"))
        assertEquals(listOf("n2"), (store.currentState as NotesV2StoreState.Ready).documents.map { it.id })
        assertEquals(listOf("n2"), mirror.notes.map { it.id })
    }
}
