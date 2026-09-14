package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.LegacyNoteMigrator
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextMark
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNoteAttachment
import com.vigilante.shiftsalaryplanner.settings.AppNoteChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2CoreTest {
    @Test
    fun `legacy note migrates to versioned document without losing context or appearance`() {
        val legacy = AppNote(
            id = "note-1",
            date = "2026-09-14",
            workplaceId = "wp-1",
            shiftCode = "N",
            title = "Смена",
            body = "Проверить наряд",
            colorHex = "#FFF59D",
            backgroundPattern = "GRID",
            textSizeMode = "LARGE",
            fontMode = "SERIF",
            textColorHex = "#112233",
            highlightColorHex = "#445566",
            textAlignMode = "CENTER",
            listMode = "BULLET",
            bulletListMode = "DASH",
            numberListMode = "ROMAN",
            bodyBold = true,
            bodyItalic = true,
            bodyHeading = true,
            bodyStrike = true,
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
        )

        val migrated = LegacyNoteMigrator.migrate(legacy)

        assertEquals(NoteDocument.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals("note-1", migrated.id)
        assertEquals("2026-09-14", migrated.context.dateIso)
        assertEquals("wp-1", migrated.context.workplaceId)
        assertEquals("N", migrated.context.shiftCode)
        assertEquals("#FFF59D", migrated.appearance.colorHex)
        assertEquals("GRID", migrated.appearance.backgroundPattern)
        assertEquals(100L, migrated.createdAtMillis)
        assertEquals(200L, migrated.updatedAtMillis)

        val block = migrated.blocks.single() as NoteBlock.Heading
        assertEquals("Проверить наряд", block.text.text)
        assertEquals(setOf(TextMark.BOLD, TextMark.ITALIC, TextMark.STRIKETHROUGH), block.text.spans.single().marks)
        assertEquals("LARGE", block.presentation.textSizeMode)
        assertEquals("SERIF", block.presentation.fontMode)
        assertEquals("#112233", block.presentation.textColorHex)
        assertEquals("#445566", block.presentation.highlightColorHex)
        assertEquals("CENTER", block.presentation.textAlignMode)
        assertEquals("BULLET", block.presentation.listMode)
        assertEquals("DASH", block.presentation.bulletListMode)
        assertEquals("ROMAN", block.presentation.numberListMode)
    }

    @Test
    fun `media markers become ordered typed blocks and attachment presentation survives`() {
        val legacy = AppNote(
            id = "note-2",
            date = "2026-09-14",
            body = "До\n[[media:image-1]]\nПосле\n[[media:video-1]]",
            attachments = listOf(
                AppNoteAttachment(
                    id = "image-1",
                    type = "IMAGE",
                    uri = "content://image/1",
                    label = "Фото",
                    displaySizeMode = "LARGE",
                    widthFraction = 0.8f,
                    aspectRatio = 1.5f,
                    createdAtMillis = 11L,
                ),
                AppNoteAttachment(
                    id = "video-1",
                    type = "VIDEO",
                    uri = "content://video/1",
                    label = "Видео",
                    displaySizeMode = "SMALL",
                    widthFraction = 0.4f,
                    aspectRatio = 1.777f,
                    createdAtMillis = 12L,
                ),
                AppNoteAttachment(
                    id = "audio-1",
                    type = "AUDIO",
                    uri = "file://audio/1",
                    label = "Голос",
                    createdAtMillis = 13L,
                ),
            ),
        )

        val migrated = LegacyNoteMigrator.migrate(legacy)

        assertEquals(5, migrated.blocks.size)
        assertEquals("До\n", (migrated.blocks[0] as NoteBlock.Paragraph).text.text)
        val image = migrated.blocks[1] as NoteBlock.Image
        assertEquals("content://image/1", image.uri)
        assertEquals("LARGE", image.presentation.displaySizeMode)
        assertEquals(0.8f, image.presentation.widthFraction)
        assertEquals(1.5f, image.presentation.aspectRatio)
        assertEquals("\nПосле\n", (migrated.blocks[2] as NoteBlock.Paragraph).text.text)
        val video = migrated.blocks[3] as NoteBlock.Video
        assertEquals("content://video/1", video.uri)
        val audio = migrated.blocks[4] as NoteBlock.Audio
        assertEquals("file://audio/1", audio.uri)
    }

    @Test
    fun `checklist survives as first class block and unknown marker remains text`() {
        val legacy = AppNote(
            id = "note-3",
            date = "2026-09-14",
            body = "До [[media:missing]] после",
            checklist = listOf(
                AppNoteChecklistItem(id = "c1", text = "Один", checked = false),
                AppNoteChecklistItem(id = "c2", text = "Два", checked = true),
            ),
        )

        val migrated = LegacyNoteMigrator.migrate(legacy)

        assertEquals(2, migrated.blocks.size)
        assertEquals("До [[media:missing]] после", (migrated.blocks[0] as NoteBlock.Paragraph).text.text)
        val checklist = migrated.blocks[1] as NoteBlock.Checklist
        assertEquals(listOf("Один", "Два"), checklist.items.map { it.text.text })
        assertFalse(checklist.items[0].checked)
        assertTrue(checklist.items[1].checked)
    }
}
