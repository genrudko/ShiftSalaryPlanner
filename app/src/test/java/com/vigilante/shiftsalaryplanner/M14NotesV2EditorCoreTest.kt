package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.ChecklistItem
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteContentType
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocumentMutations
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteEditorHistory
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryLocation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryQuery
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryQueryEngine
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledTextMutations
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextMark
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2EditorCoreTest {
    private fun note(
        id: String,
        title: String = id,
        blocks: List<NoteBlock> = emptyList(),
        labels: Set<String> = emptySet(),
        pinned: Boolean = false,
        archived: Boolean = false,
        deletedAtMillis: Long? = null,
        updatedAtMillis: Long = 1L,
    ) = NoteDocument(
        id = id,
        title = title,
        blocks = blocks,
        labels = labels,
        pinned = pinned,
        archived = archived,
        deletedAtMillis = deletedAtMillis,
        createdAtMillis = 1L,
        updatedAtMillis = updatedAtMillis,
    )

    @Test
    fun `document mutations preserve block identity and checklist state`() {
        val source = note(
            id = "n1",
            blocks = listOf(
                NoteBlock.Paragraph("p1", StyledText("Первый")),
                NoteBlock.Checklist("check", listOf(
                    ChecklistItem("i1", StyledText("Один"), false),
                    ChecklistItem("i2", StyledText("Два"), false),
                )),
                NoteBlock.Paragraph("p2", StyledText("Последний")),
            ),
        )

        val toggled = NoteDocumentMutations.toggleChecklistItem(source, "check", "i2", 20L)
        val checklist = toggled.blocks[1] as NoteBlock.Checklist
        assertFalse(checklist.items[0].checked)
        assertTrue(checklist.items[1].checked)

        val indented = NoteDocumentMutations.setChecklistIndent(toggled, "check", "i2", 99, 21L)
        assertEquals(NoteDocumentMutations.MAX_CHECKLIST_INDENT, (indented.blocks[1] as NoteBlock.Checklist).items[1].indent)

        val moved = NoteDocumentMutations.moveBlock(indented, "p2", 0, 30L)
        assertEquals(listOf("p2", "p1", "check"), moved.blocks.map { it.id })
        assertEquals(30L, moved.updatedAtMillis)
    }

    @Test
    fun `rich text marks can overlap and toggle independently`() {
        val base = StyledText("abcdef")
        val bold = StyledTextMutations.toggleMark(base, 0, 4, TextMark.BOLD)
        val mixed = StyledTextMutations.toggleMark(bold, 2, 6, TextMark.ITALIC)
        assertEquals(listOf(
            TextSpan(0, 2, setOf(TextMark.BOLD)),
            TextSpan(2, 4, setOf(TextMark.BOLD, TextMark.ITALIC)),
            TextSpan(4, 6, setOf(TextMark.ITALIC)),
        ), mixed.spans)
        val italicRemoved = StyledTextMutations.toggleMark(mixed, 2, 6, TextMark.ITALIC)
        assertEquals(listOf(TextSpan(0, 4, setOf(TextMark.BOLD))), italicRemoved.spans)
    }

    @Test
    fun `library query searches content and separates active archive and trash`() {
        val notes = listOf(
            note("work", title = "Смена", labels = setOf("работа"), pinned = true, updatedAtMillis = 100L,
                blocks = listOf(NoteBlock.Paragraph("p", StyledText("Проверить Орион")))),
            note("home", title = "Покупки", labels = setOf("дом"), updatedAtMillis = 200L,
                blocks = listOf(
                    NoteBlock.Checklist("c", listOf(ChecklistItem("i", StyledText("Молоко"), false))),
                    NoteBlock.Image("img", "content://image/1"),
                )),
            note("archive", archived = true, updatedAtMillis = 300L,
                blocks = listOf(NoteBlock.Audio("a", "content://audio/1"))),
            note("trash", deletedAtMillis = 10L, updatedAtMillis = 400L),
        )

        assertEquals(listOf("work"), NoteLibraryQueryEngine.query(notes, NoteLibraryQuery(text = "орион")).map { it.id })
        assertEquals(listOf("home"), NoteLibraryQueryEngine.query(notes, NoteLibraryQuery(text = "молоко")).map { it.id })
        assertEquals(listOf("home"), NoteLibraryQueryEngine.query(notes, NoteLibraryQuery(contentTypes = setOf(NoteContentType.IMAGE))).map { it.id })
        assertEquals(listOf("archive"), NoteLibraryQueryEngine.query(notes, NoteLibraryQuery(location = NoteLibraryLocation.ARCHIVE)).map { it.id })
        assertEquals(listOf("trash"), NoteLibraryQueryEngine.query(notes, NoteLibraryQuery(location = NoteLibraryLocation.TRASH)).map { it.id })
        assertEquals(listOf("work", "home"), NoteLibraryQueryEngine.query(notes, NoteLibraryQuery()).map { it.id })
    }

    @Test
    fun `editor history supports bounded undo redo and divergent edits clear redo`() {
        val a = note("history", title = "A")
        val b = a.copy(title = "B", updatedAtMillis = 2L)
        val c = b.copy(title = "C", updatedAtMillis = 3L)
        var history = NoteEditorHistory.start(a, maxUndoEntries = 3)
        history = NoteEditorHistory.commit(history, b)
        history = NoteEditorHistory.commit(history, c)
        history = NoteEditorHistory.undo(history)
        assertEquals("B", history.current.title)
        assertTrue(history.canRedo)
        history = NoteEditorHistory.commit(history, history.current.copy(title = "D", updatedAtMillis = 4L))
        assertEquals("D", history.current.title)
        assertFalse(history.canRedo)
    }

    @Test
    fun `pin archive trash restore are explicit immutable transitions`() {
        val source = note("state")
        val pinned = NoteDocumentMutations.setPinned(source, true, 2L)
        assertTrue(pinned.pinned)
        val archived = NoteDocumentMutations.setArchived(pinned, true, 3L)
        assertTrue(archived.archived)
        val trashed = NoteDocumentMutations.moveToTrash(archived, 4L)
        assertEquals(4L, trashed.deletedAtMillis)
        assertFalse(trashed.archived)
        val restored = NoteDocumentMutations.restoreFromTrash(trashed, 5L)
        assertNull(restored.deletedAtMillis)
        assertEquals(5L, restored.updatedAtMillis)
    }
}
