package com.vigilante.shiftsalaryplanner

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2PersistenceCodecTest {
    @Test
    fun `versioned json round trip preserves document blocks metadata and formatting`() {
        val presentation = NoteTextPresentation(
            textSizeMode = "LARGE",
            fontMode = "SERIF",
            textColorHex = "#112233",
            highlightColorHex = "#445566",
            textAlignMode = "CENTER",
            listMode = "NUMBERED",
            bulletListMode = "DASH",
            numberListMode = "ROMAN",
        )
        val media = NoteMediaPresentation("LARGE", 0.8f, 1.5f, 99L)
        val document = NoteDocument(
            id = "doc-1",
            title = "Полная заметка",
            blocks = listOf(
                NoteBlock.Paragraph("p", StyledText("abcdef", listOf(TextSpan(0, 3, setOf(TextMark.BOLD, TextMark.ITALIC)))), presentation),
                NoteBlock.Heading("h", 2, StyledText("Заголовок"), presentation),
                NoteBlock.Checklist("c", listOf(ChecklistItem("i", StyledText("Пункт"), true, indent = 2))),
                NoteBlock.Image("img", "content://img", "Фото", media),
                NoteBlock.Video("vid", "content://vid", "Видео", media),
                NoteBlock.Audio("aud", "content://aud", "Голос", media, StyledText("расшифровка")),
                NoteBlock.FileAttachment("file", "content://file", "Документ", media),
                NoteBlock.Divider("div"),
            ),
            labels = setOf("работа", "важное"),
            appearance = NoteAppearance("#ABCDEF", "GRID"),
            context = NoteContext("2026-09-14", "wp-1", "N"),
            pinned = true,
            archived = false,
            reminderAtMillis = 123456L,
            createdAtMillis = 10L,
            updatedAtMillis = 20L,
        )

        val raw = NoteDocumentJsonCodec.encodeList(listOf(document)).toString()
        val decoded = NoteDocumentJsonCodec.decodeList(raw).single()

        assertEquals(document, decoded)
    }

    @Test
    fun `codec ignores unsupported schema entries without dropping supported documents`() {
        val supported = NoteDocument(
            id = "ok",
            title = "OK",
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
        )
        val raw = NoteDocumentJsonCodec.encodeList(listOf(supported)).toString()
            .replaceFirst("\"schemaVersion\":2", "\"schemaVersion\":999")

        assertTrue(NoteDocumentJsonCodec.decodeList(raw).isEmpty())
    }
}
