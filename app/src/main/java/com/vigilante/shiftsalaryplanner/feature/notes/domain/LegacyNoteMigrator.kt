package com.vigilante.shiftsalaryplanner.feature.notes.domain

import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNoteAttachment

object LegacyNoteMigrator {
    private val mediaMarkerRegex = Regex("""\[\[media:([A-Za-z0-9-]+)]]""")

    fun migrate(note: AppNote): NoteDocument {
        val attachmentsById = note.attachments.associateBy { it.id }
        val referenced = linkedSetOf<String>()
        val blocks = buildList {
            addBodyBlocks(note, attachmentsById, referenced)
            if (note.checklist.isNotEmpty()) {
                add(NoteBlock.Checklist(
                    id = "legacy-checklist-${note.id}",
                    items = note.checklist.map { ChecklistItem(it.id, StyledText(it.text), it.checked) },
                ))
            }
            note.attachments.filterNot { it.id in referenced }.forEach { add(it.toBlock()) }
        }
        return NoteDocument(
            id = note.id,
            title = note.title,
            blocks = blocks,
            appearance = NoteAppearance(note.colorHex, note.backgroundPattern),
            context = NoteContext(note.date, note.workplaceId, note.shiftCode),
            createdAtMillis = note.createdAtMillis,
            updatedAtMillis = note.updatedAtMillis,
        )
    }

    private fun MutableList<NoteBlock>.addBodyBlocks(
        note: AppNote,
        attachmentsById: Map<String, AppNoteAttachment>,
        referenced: MutableSet<String>,
    ) {
        if (note.body.isEmpty()) return
        var cursor = 0
        var textIndex = 0
        mediaMarkerRegex.findAll(note.body).forEach { match ->
            val attachment = attachmentsById[match.groupValues[1]] ?: return@forEach
            if (match.range.first > cursor) {
                add(note.createTextBlock(textIndex++, note.body.substring(cursor, match.range.first)))
            }
            add(attachment.toBlock())
            referenced += attachment.id
            cursor = match.range.last + 1
        }
        if (cursor < note.body.length) add(note.createTextBlock(textIndex, note.body.substring(cursor)))
    }

    private fun AppNote.createTextBlock(index: Int, value: String): NoteBlock {
        val marks = buildSet {
            if (bodyBold) add(TextMark.BOLD)
            if (bodyItalic) add(TextMark.ITALIC)
            if (bodyStrike) add(TextMark.STRIKETHROUGH)
        }
        val styled = StyledText(value, if (value.isNotEmpty() && marks.isNotEmpty()) listOf(TextSpan(0, value.length, marks)) else emptyList())
        val presentation = NoteTextPresentation(
            textSizeMode, fontMode, textColorHex, highlightColorHex, textAlignMode,
            listMode, bulletListMode, numberListMode,
        )
        val blockId = "legacy-text-$id-$index"
        return if (bodyHeading) NoteBlock.Heading(blockId, 1, styled, presentation)
        else NoteBlock.Paragraph(blockId, styled, presentation)
    }

    private fun AppNoteAttachment.toBlock(): NoteBlock {
        val presentation = NoteMediaPresentation(displaySizeMode, widthFraction, aspectRatio, createdAtMillis)
        return when (type.uppercase()) {
            "IMAGE" -> NoteBlock.Image(id, uri, label, presentation)
            "VIDEO" -> NoteBlock.Video(id, uri, label, presentation)
            "AUDIO" -> NoteBlock.Audio(id, uri, label, presentation)
            else -> NoteBlock.FileAttachment(id, uri, label, presentation)
        }
    }
}
