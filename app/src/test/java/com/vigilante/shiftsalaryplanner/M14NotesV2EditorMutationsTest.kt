package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.ChecklistItem
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteAppearance
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteContext
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocumentMutations
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class M14NotesV2EditorMutationsTest {
    private fun note(blocks: List<NoteBlock> = emptyList()) = NoteDocument(
        id = "note",
        title = "Черновик",
        blocks = blocks,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
    )

    @Test
    fun `title appearance labels context and reminder are first class document edits`() {
        val source = note()
        val titled = NoteDocumentMutations.setTitle(source, "Новая", 2L)
        val styled = NoteDocumentMutations.setAppearance(titled, NoteAppearance("#BDE0FE", "DOTS"), 3L)
        val labeled = NoteDocumentMutations.setLabels(styled, setOf("работа", "важное"), 4L)
        val contextual = NoteDocumentMutations.setContext(
            labeled,
            NoteContext(dateIso = "2026-09-14", workplaceId = "main", shiftCode = "Д"),
            5L,
        )
        val reminded = NoteDocumentMutations.setReminder(contextual, 123_456L, 6L)

        assertEquals("Новая", reminded.title)
        assertEquals("#BDE0FE", reminded.appearance.colorHex)
        assertEquals("DOTS", reminded.appearance.backgroundPattern)
        assertEquals(setOf("работа", "важное"), reminded.labels)
        assertEquals("main", reminded.context.workplaceId)
        assertEquals("Д", reminded.context.shiftCode)
        assertEquals(123_456L, reminded.reminderAtMillis)
        assertEquals(6L, reminded.updatedAtMillis)
    }

    @Test
    fun `blocks can be inserted replaced and edited without losing stable ids`() {
        val paragraph = NoteBlock.Paragraph("p1", StyledText("Первый"))
        val checklist = NoteBlock.Checklist("c1", emptyList())
        var document = note(listOf(paragraph))

        document = NoteDocumentMutations.insertBlock(document, checklist, afterBlockId = "p1", updatedAtMillis = 2L)
        assertEquals(listOf("p1", "c1"), document.blocks.map { it.id })

        document = NoteDocumentMutations.replaceBlockText(document, "p1", StyledText("Изменённый"), 3L)
        assertEquals("Изменённый", (document.blocks[0] as NoteBlock.Paragraph).text.text)
        assertEquals("p1", document.blocks[0].id)

        val heading = NoteBlock.Heading("p1", level = 2, text = StyledText("Заголовок"))
        document = NoteDocumentMutations.replaceBlock(document, heading, 4L)
        assertEquals(heading, document.blocks[0])
    }

    @Test
    fun `checklist items support append text edit and remove`() {
        var document = note(listOf(NoteBlock.Checklist("c1", emptyList())))
        val item = ChecklistItem("i1", StyledText("Позвонить"), checked = false)

        document = NoteDocumentMutations.appendChecklistItem(document, "c1", item, 2L)
        document = NoteDocumentMutations.updateChecklistItemText(document, "c1", "i1", StyledText("Позвонить диспетчеру"), 3L)
        val edited = (document.blocks.single() as NoteBlock.Checklist).items.single()
        assertEquals("i1", edited.id)
        assertEquals("Позвонить диспетчеру", edited.text.text)

        document = NoteDocumentMutations.removeChecklistItem(document, "c1", "i1", 4L)
        assertEquals(emptyList<ChecklistItem>(), (document.blocks.single() as NoteBlock.Checklist).items)
    }

    @Test
    fun `unknown ids and identical metadata edits remain true no ops`() {
        val source = note(listOf(NoteBlock.Paragraph("p1", StyledText("Текст"))))
        assertSame(source, NoteDocumentMutations.replaceBlockText(source, "missing", StyledText("x"), 2L))
        assertSame(source, NoteDocumentMutations.removeChecklistItem(source, "missing", "i", 2L))
        assertSame(source, NoteDocumentMutations.setTitle(source, source.title, 2L))
        assertSame(source, NoteDocumentMutations.setLabels(source, source.labels, 2L))
        assertSame(source, NoteDocumentMutations.setAppearance(source, source.appearance, 2L))
        assertSame(source, NoteDocumentMutations.setContext(source, source.context, 2L))
        assertSame(source, NoteDocumentMutations.setReminder(source, source.reminderAtMillis, 2L))
    }
}
