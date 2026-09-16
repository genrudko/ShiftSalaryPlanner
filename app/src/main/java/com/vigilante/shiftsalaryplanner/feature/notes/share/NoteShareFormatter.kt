package com.vigilante.shiftsalaryplanner.feature.notes.share

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument

object NoteShareFormatter {
    fun toPlainText(document: NoteDocument): String = buildList {
        document.title.trim().takeIf { it.isNotBlank() }?.let(::add)
        document.blocks.forEach { block ->
            when (block) {
                is NoteBlock.Paragraph -> block.text.text.takeIf { it.isNotBlank() }?.let(::add)
                is NoteBlock.Heading -> block.text.text.takeIf { it.isNotBlank() }?.let(::add)
                is NoteBlock.Checklist -> block.items.forEach { item ->
                    val indent = "  ".repeat(item.indent.coerceAtLeast(0))
                    val marker = if (item.checked) "☑" else "☐"
                    add("$indent$marker ${item.text.text}")
                }
                is NoteBlock.Image -> add("Фото: ${block.label.ifBlank { "Изображение" }}")
                is NoteBlock.Video -> add("Видео: ${block.label.ifBlank { "Видео" }}")
                is NoteBlock.Audio -> add("Аудио: ${block.label.ifBlank { "Аудиозапись" }}")
                is NoteBlock.FileAttachment -> add("Файл: ${block.label.ifBlank { "Вложение" }}")
                is NoteBlock.Divider -> add("—")
            }
        }
        if (document.labels.isNotEmpty()) {
            add(document.labels.sorted().joinToString(" ") { "#$it" })
        }
    }.joinToString("\n").trim()
}
