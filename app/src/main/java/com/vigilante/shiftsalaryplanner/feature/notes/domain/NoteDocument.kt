package com.vigilante.shiftsalaryplanner.feature.notes.domain

data class NoteDocument(
    val id: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val title: String,
    val blocks: List<NoteBlock> = emptyList(),
    val labels: Set<String> = emptySet(),
    val appearance: NoteAppearance = NoteAppearance(),
    val context: NoteContext = NoteContext(),
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val deletedAtMillis: Long? = null,
    val reminderAtMillis: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    companion object { const val CURRENT_SCHEMA_VERSION: Int = 2 }
}

data class NoteAppearance(val colorHex: String = "#FFF3BF", val backgroundPattern: String = "NONE")
data class NoteContext(val dateIso: String? = null, val workplaceId: String? = null, val shiftCode: String? = null)
data class StyledText(val text: String, val spans: List<TextSpan> = emptyList())
data class TextSpan(val start: Int, val endExclusive: Int, val marks: Set<TextMark>)
enum class TextMark { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, CODE }

data class NoteTextPresentation(
    val textSizeMode: String = "NORMAL",
    val fontMode: String = "SANS",
    val textColorHex: String = "",
    val highlightColorHex: String = "",
    val textAlignMode: String = "START",
    val listMode: String = "NONE",
    val bulletListMode: String = "DOT",
    val numberListMode: String = "DECIMAL",
)

data class NoteMediaPresentation(
    val displaySizeMode: String = "MEDIUM",
    val widthFraction: Float = 0f,
    val aspectRatio: Float = 0f,
    val createdAtMillis: Long = 0L,
)

sealed interface NoteBlock {
    val id: String
    data class Paragraph(override val id: String, val text: StyledText, val presentation: NoteTextPresentation = NoteTextPresentation()) : NoteBlock
    data class Heading(override val id: String, val level: Int = 1, val text: StyledText, val presentation: NoteTextPresentation = NoteTextPresentation()) : NoteBlock
    data class Checklist(override val id: String, val items: List<ChecklistItem>) : NoteBlock
    data class Image(override val id: String, val uri: String, val label: String = "", val presentation: NoteMediaPresentation = NoteMediaPresentation()) : NoteBlock
    data class Video(override val id: String, val uri: String, val label: String = "", val presentation: NoteMediaPresentation = NoteMediaPresentation()) : NoteBlock
    data class Audio(override val id: String, val uri: String, val label: String = "", val presentation: NoteMediaPresentation = NoteMediaPresentation(), val transcript: StyledText? = null) : NoteBlock
    data class FileAttachment(override val id: String, val uri: String, val label: String = "", val presentation: NoteMediaPresentation = NoteMediaPresentation()) : NoteBlock
    data class Divider(override val id: String) : NoteBlock
}

data class ChecklistItem(val id: String, val text: StyledText, val checked: Boolean, val indent: Int = 0)
