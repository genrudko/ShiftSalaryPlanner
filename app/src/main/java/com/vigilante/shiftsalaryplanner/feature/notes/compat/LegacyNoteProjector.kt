package com.vigilante.shiftsalaryplanner.feature.notes.compat

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteTextPresentation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextMark
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNoteAttachment
import com.vigilante.shiftsalaryplanner.settings.AppNoteChecklistItem

/** Loss-aware projection for legacy readers. Notes v2 remains authoritative. */
object LegacyNoteProjector {
    fun project(document: NoteDocument): AppNote? {
        val date = document.context.dateIso?.takeIf { it.isNotBlank() } ?: return null
        if (document.archived || document.deletedAtMillis != null) return null

        val bodySegments = mutableListOf<String>()
        val checklist = mutableListOf<AppNoteChecklistItem>()
        val attachments = mutableListOf<AppNoteAttachment>()
        var firstText: Pair<StyledText, NoteTextPresentation>? = null
        var firstTextIsHeading = false

        document.blocks.forEach { block ->
            when (block) {
                is NoteBlock.Paragraph -> {
                    if (firstText == null) firstText = block.text to block.presentation
                    bodySegments += block.text.text
                }
                is NoteBlock.Heading -> {
                    if (firstText == null) {
                        firstText = block.text to block.presentation
                        firstTextIsHeading = true
                    }
                    bodySegments += block.text.text
                }
                is NoteBlock.Checklist -> block.items.forEach { item ->
                    checklist += AppNoteChecklistItem(id = item.id, text = item.text.text, checked = item.checked)
                }
                is NoteBlock.Image -> {
                    bodySegments += "[[media:${block.id}]]"
                    attachments += block.toLegacyAttachment("IMAGE")
                }
                is NoteBlock.Video -> {
                    bodySegments += "[[media:${block.id}]]"
                    attachments += block.toLegacyAttachment("VIDEO")
                }
                is NoteBlock.Audio -> attachments += block.toLegacyAttachment("AUDIO")
                is NoteBlock.FileAttachment -> attachments += block.toLegacyAttachment("FILE")
                is NoteBlock.Divider -> bodySegments += "---"
            }
        }

        val text = firstText?.first
        val presentation = firstText?.second ?: NoteTextPresentation()
        return AppNote(
            id = document.id,
            date = date,
            workplaceId = document.context.workplaceId,
            shiftCode = document.context.shiftCode,
            title = document.title,
            body = bodySegments.joinToString("\n").trimEnd(),
            colorHex = document.appearance.colorHex,
            backgroundPattern = document.appearance.backgroundPattern,
            textSizeMode = presentation.textSizeMode,
            fontMode = presentation.fontMode,
            textColorHex = presentation.textColorHex,
            highlightColorHex = presentation.highlightColorHex,
            textAlignMode = presentation.textAlignMode,
            listMode = presentation.listMode,
            bulletListMode = presentation.bulletListMode,
            numberListMode = presentation.numberListMode,
            bodyBold = text?.isFullyMarked(TextMark.BOLD) == true,
            bodyItalic = text?.isFullyMarked(TextMark.ITALIC) == true,
            bodyHeading = firstTextIsHeading,
            bodyStrike = text?.isFullyMarked(TextMark.STRIKETHROUGH) == true,
            checklist = checklist,
            attachments = attachments,
            createdAtMillis = document.createdAtMillis,
            updatedAtMillis = document.updatedAtMillis,
        )
    }

    fun projectAll(documents: List<NoteDocument>): List<AppNote> = documents.mapNotNull(::project)
        .sortedWith(compareByDescending<AppNote> { it.date }.thenByDescending { it.updatedAtMillis })

    private fun StyledText.isFullyMarked(mark: TextMark): Boolean {
        if (text.isEmpty()) return false
        val covered = BooleanArray(text.length)
        spans.filter { mark in it.marks }.forEach { span ->
            val from = span.start.coerceIn(0, text.length)
            val to = span.endExclusive.coerceIn(0, text.length)
            for (index in from until to) covered[index] = true
        }
        return covered.all { it }
    }

    private fun NoteBlock.Image.toLegacyAttachment(type: String) = AppNoteAttachment(
        id = id, type = type, uri = uri, label = label,
        displaySizeMode = presentation.displaySizeMode,
        widthFraction = presentation.widthFraction,
        aspectRatio = presentation.aspectRatio,
        createdAtMillis = presentation.createdAtMillis,
    )

    private fun NoteBlock.Video.toLegacyAttachment(type: String) = AppNoteAttachment(
        id = id, type = type, uri = uri, label = label,
        displaySizeMode = presentation.displaySizeMode,
        widthFraction = presentation.widthFraction,
        aspectRatio = presentation.aspectRatio,
        createdAtMillis = presentation.createdAtMillis,
    )

    private fun NoteBlock.Audio.toLegacyAttachment(type: String) = AppNoteAttachment(
        id = id, type = type, uri = uri, label = label,
        displaySizeMode = presentation.displaySizeMode,
        widthFraction = presentation.widthFraction,
        aspectRatio = presentation.aspectRatio,
        createdAtMillis = presentation.createdAtMillis,
    )

    private fun NoteBlock.FileAttachment.toLegacyAttachment(type: String) = AppNoteAttachment(
        id = id, type = type, uri = uri, label = label,
        displaySizeMode = presentation.displaySizeMode,
        widthFraction = presentation.widthFraction,
        aspectRatio = presentation.aspectRatio,
        createdAtMillis = presentation.createdAtMillis,
    )
}
