package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.ChecklistItem
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.share.NoteShareFormatter
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2ShareExportTest {
    @Test
    fun `plain text share preserves title text checklist state and media labels`() {
        val note = NoteDocument(
            id = "share",
            title = "Схема",
            labels = setOf("работа"),
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
            blocks = listOf(
                NoteBlock.Paragraph("p", StyledText("Проверить шкаф")),
                NoteBlock.Checklist("c", listOf(
                    ChecklistItem("1", StyledText("Автомат"), checked = true),
                    ChecklistItem("2", StyledText("Кабель"), checked = false),
                )),
                NoteBlock.Image("i", "file:///tmp/photo.jpg", "Щит"),
            ),
        )
        val text = NoteShareFormatter.toPlainText(note)
        assertTrue(text.contains("Схема"))
        assertTrue(text.contains("Проверить шкаф"))
        assertTrue(text.contains("☑ Автомат"))
        assertTrue(text.contains("☐ Кабель"))
        assertTrue(text.contains("Щит"))
        assertTrue(text.contains("#работа"))
    }

    @Test
    fun `android editor shares managed attachments and exports full v2 json`() {
        val androidEditor = File("src/main/java/com/vigilante/shiftsalaryplanner/feature/notes/ui/NotesV2AndroidEditorScreen.kt").readText()
        val editor = File("src/main/java/com/vigilante/shiftsalaryplanner/feature/notes/ui/NotesV2EditorScreen.kt").readText()
        assertTrue(editor.contains("Поделиться"))
        assertTrue(editor.contains("Экспорт JSON"))
        assertTrue(androidEditor.contains("Intent.ACTION_SEND"))
        assertTrue(androidEditor.contains("Intent.ACTION_SEND_MULTIPLE"))
        assertTrue(androidEditor.contains("FileProvider.getUriForFile"))
        assertTrue(androidEditor.contains("ActivityResultContracts.CreateDocument(\"application/json\")"))
        assertTrue(androidEditor.contains("NoteDocumentJsonCodec.encodeList("))
    }
}
