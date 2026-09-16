package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.feature.notes.domain.ChecklistItem
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteAppearance
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteContext
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteMediaPresentation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextMark
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextSpan
import com.vigilante.shiftsalaryplanner.feature.notes.ui.NotesV2EditorScreen
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles

private val m14EditorDocument = NoteDocument(
    id = "editor-mixed",
    title = "Подготовка к смене",
    blocks = listOf(
        NoteBlock.Heading("h1", level = 2, text = StyledText("Перед выездом")),
        NoteBlock.Paragraph(
            "p1",
            StyledText(
                text = "Проверить документы и зарядить рацию до начала смены.",
                spans = listOf(
                    TextSpan(0, 20, setOf(TextMark.BOLD)),
                    TextSpan(23, 37, setOf(TextMark.ITALIC)),
                ),
            ),
        ),
        NoteBlock.Checklist(
            "check",
            listOf(
                ChecklistItem("i1", StyledText("Наряд и допуск"), checked = true),
                ChecklistItem("i2", StyledText("Рация и фонарь"), checked = false),
                ChecklistItem("i3", StyledText("Позвонить диспетчеру"), checked = false, indent = 1),
            ),
        ),
        NoteBlock.Image(
            "img",
            "content://preview/photo",
            label = "Фото схемы",
            presentation = NoteMediaPresentation(displaySizeMode = "LARGE", aspectRatio = 1.6f),
        ),
        NoteBlock.Audio("audio", "content://preview/audio", label = "Голосовая заметка 00:42"),
        NoteBlock.Video("video", "content://preview/video", label = "Видео обхода"),
        NoteBlock.FileAttachment("file", "content://preview/file", label = "Инструкция.pdf"),
    ),
    labels = setOf("Работа", "Важно"),
    appearance = NoteAppearance("#FFF3BF", "GRID"),
    context = NoteContext(dateIso = "2026-09-16", workplaceId = "main", shiftCode = "Д"),
    pinned = true,
    createdAtMillis = 1L,
    updatedAtMillis = 2L,
)

private val m14EmptyEditorDocument = NoteDocument(
    id = "new",
    title = "",
    blocks = emptyList(),
    appearance = NoteAppearance("#FFF3BF", "NONE"),
    createdAtMillis = 1L,
    updatedAtMillis = 1L,
)

@Composable
private fun M14EditorTheme(dark: Boolean, content: @Composable () -> Unit) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE,
        )
    ) {
        val roles = evolutionColorRoles()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = roles.appBackground,
            contentColor = roles.contentPrimary,
        ) { content() }
    }
}

@Composable
private fun M14Editor(dark: Boolean, document: NoteDocument = m14EditorDocument) = M14EditorTheme(dark) {
    NotesV2EditorScreen(
        initialDocument = document,
        onBack = {},
        onSave = {},
        onDelete = {},
        onRequestImage = {},
        onRequestVideo = {},
        onRequestAudio = {},
        onRequestFile = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@PreviewTest
@Preview(name = "M14.1 Notes v2 editor light", widthDp = 412, heightDp = 1500, showBackground = true)
@Composable
fun m14NotesV2EditorLight() = M14Editor(false)

@PreviewTest
@Preview(name = "M14.1 Notes v2 editor dark", widthDp = 412, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m14NotesV2EditorDark() = M14Editor(true)

@PreviewTest
@Preview(name = "M14.1 Notes v2 editor large font", widthDp = 412, heightDp = 1900, fontScale = 1.3f, showBackground = true)
@Composable
fun m14NotesV2EditorLargeFont() = M14Editor(false)

@PreviewTest
@Preview(name = "M14.1 Notes v2 editor empty", widthDp = 412, heightDp = 1000, showBackground = true)
@Composable
fun m14NotesV2EditorEmpty() = M14Editor(false, m14EmptyEditorDocument)
