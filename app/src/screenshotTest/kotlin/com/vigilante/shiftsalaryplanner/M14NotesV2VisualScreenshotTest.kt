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
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryLocation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.ui.NotesV2LayoutMode
import com.vigilante.shiftsalaryplanner.feature.notes.ui.NotesV2LibraryScreen
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles

private val m14Notes = listOf(
    NoteDocument(
        id = "pinned",
        title = "На смену",
        blocks = listOf(
            NoteBlock.Checklist(
                "check",
                listOf(
                    ChecklistItem("c1", StyledText("Проверить наряд"), false),
                    ChecklistItem("c2", StyledText("Забрать документы"), true),
                    ChecklistItem("c3", StyledText("Позвонить диспетчеру"), false),
                )
            )
        ),
        labels = setOf("Работа"),
        appearance = NoteAppearance("#FFF3BF", "NONE"),
        context = NoteContext("2026-09-14", "main", "Д"),
        pinned = true,
        createdAtMillis = 1L,
        updatedAtMillis = 50L,
    ),
    NoteDocument(
        id = "mixed",
        title = "Идея для корпуса",
        blocks = listOf(
            NoteBlock.Paragraph("p", StyledText("Сделать защёлку тоньше и добавить упор под палец.")),
            NoteBlock.Image("img", "content://preview/photo", "Фото прототипа"),
        ),
        labels = setOf("Идеи", "3D"),
        appearance = NoteAppearance("#D8F3DC", "GRID"),
        createdAtMillis = 2L,
        updatedAtMillis = 40L,
    ),
    NoteDocument(
        id = "audio",
        title = "Голосовая заметка",
        blocks = listOf(
            NoteBlock.Audio("a", "content://preview/audio", "После ночной смены", transcript = StyledText("Проверить температуру подшипника на следующем обходе."))
        ),
        appearance = NoteAppearance("#BDE0FE", "WAVES"),
        createdAtMillis = 3L,
        updatedAtMillis = 30L,
    ),
    NoteDocument(
        id = "plain",
        title = "Покупки",
        blocks = listOf(NoteBlock.Paragraph("p2", StyledText("Филамент, сопло 0.6, изопропанол"))),
        labels = setOf("Дом"),
        appearance = NoteAppearance("#E7C6FF", "NONE"),
        createdAtMillis = 4L,
        updatedAtMillis = 20L,
    ),
)

@Composable
private fun M14NotesTheme(dark: Boolean, content: @Composable () -> Unit) {
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
private fun M14Library(
    dark: Boolean,
    notes: List<NoteDocument> = m14Notes,
    query: String = "",
    location: NoteLibraryLocation = NoteLibraryLocation.ACTIVE,
    layoutMode: NotesV2LayoutMode = NotesV2LayoutMode.GRID,
) = M14NotesTheme(dark) {
    NotesV2LibraryScreen(
        documents = notes,
        initialQueryText = query,
        initialLocation = location,
        initialLayoutMode = layoutMode,
        onCreateNote = {},
        onCreateChecklist = {},
        onCreateAudio = {},
        onAddImage = {},
        onOpenNote = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@PreviewTest
@Preview(name = "M14.1 Notes v2 library light", widthDp = 412, heightDp = 1100, showBackground = true)
@Composable
fun m14NotesV2LibraryLight() = M14Library(false)

@PreviewTest
@Preview(name = "M14.1 Notes v2 library dark", widthDp = 412, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m14NotesV2LibraryDark() = M14Library(true)

@PreviewTest
@Preview(name = "M14.1 Notes v2 library large font", widthDp = 412, heightDp = 1350, fontScale = 1.3f, showBackground = true)
@Composable
fun m14NotesV2LibraryLargeFont() = M14Library(false, layoutMode = NotesV2LayoutMode.LIST)

@PreviewTest
@Preview(name = "M14.1 Notes v2 search", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
fun m14NotesV2Search() = M14Library(false, query = "подшипника")

@PreviewTest
@Preview(name = "M14.1 Notes v2 empty", widthDp = 412, heightDp = 850, showBackground = true)
@Composable
fun m14NotesV2Empty() = M14Library(false, notes = emptyList())
