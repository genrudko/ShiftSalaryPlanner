package com.vigilante.shiftsalaryplanner.feature.notes.domain

object NoteDocumentMutations {
    const val MAX_CHECKLIST_INDENT: Int = 4

    fun moveBlock(document: NoteDocument, blockId: String, targetIndex: Int, updatedAtMillis: Long): NoteDocument {
        val from = document.blocks.indexOfFirst { it.id == blockId }
        if (from < 0 || document.blocks.size < 2) return document
        val to = targetIndex.coerceIn(0, document.blocks.lastIndex)
        if (from == to) return document
        val blocks = document.blocks.toMutableList()
        val block = blocks.removeAt(from)
        blocks.add(to, block)
        return document.copy(blocks = blocks, updatedAtMillis = updatedAtMillis)
    }

    fun removeBlock(document: NoteDocument, blockId: String, updatedAtMillis: Long): NoteDocument {
        if (document.blocks.none { it.id == blockId }) return document
        return document.copy(blocks = document.blocks.filterNot { it.id == blockId }, updatedAtMillis = updatedAtMillis)
    }

    fun toggleChecklistItem(document: NoteDocument, blockId: String, itemId: String, updatedAtMillis: Long): NoteDocument =
        updateChecklistItem(document, blockId, itemId, updatedAtMillis) { it.copy(checked = !it.checked) }

    fun setChecklistIndent(document: NoteDocument, blockId: String, itemId: String, indent: Int, updatedAtMillis: Long): NoteDocument =
        updateChecklistItem(document, blockId, itemId, updatedAtMillis) { it.copy(indent = indent.coerceIn(0, MAX_CHECKLIST_INDENT)) }

    fun setPinned(document: NoteDocument, pinned: Boolean, updatedAtMillis: Long): NoteDocument =
        if (document.pinned == pinned) document else document.copy(pinned = pinned, updatedAtMillis = updatedAtMillis)

    fun setArchived(document: NoteDocument, archived: Boolean, updatedAtMillis: Long): NoteDocument =
        if (document.archived == archived && document.deletedAtMillis == null) document
        else document.copy(archived = archived, deletedAtMillis = null, updatedAtMillis = updatedAtMillis)

    fun moveToTrash(document: NoteDocument, deletedAtMillis: Long): NoteDocument =
        document.copy(archived = false, deletedAtMillis = deletedAtMillis, updatedAtMillis = deletedAtMillis)

    fun restoreFromTrash(document: NoteDocument, updatedAtMillis: Long): NoteDocument =
        if (document.deletedAtMillis == null) document else document.copy(deletedAtMillis = null, updatedAtMillis = updatedAtMillis)

    fun setTitle(document: NoteDocument, title: String, updatedAtMillis: Long): NoteDocument =
        if (document.title == title) document else document.copy(title = title, updatedAtMillis = updatedAtMillis)

    fun setAppearance(document: NoteDocument, appearance: NoteAppearance, updatedAtMillis: Long): NoteDocument =
        if (document.appearance == appearance) document else document.copy(appearance = appearance, updatedAtMillis = updatedAtMillis)

    fun setLabels(document: NoteDocument, labels: Set<String>, updatedAtMillis: Long): NoteDocument =
        if (document.labels == labels) document else document.copy(labels = labels, updatedAtMillis = updatedAtMillis)

    fun setContext(document: NoteDocument, context: NoteContext, updatedAtMillis: Long): NoteDocument =
        if (document.context == context) document else document.copy(context = context, updatedAtMillis = updatedAtMillis)

    fun setReminder(document: NoteDocument, reminderAtMillis: Long?, updatedAtMillis: Long): NoteDocument =
        if (document.reminderAtMillis == reminderAtMillis) document else document.copy(reminderAtMillis = reminderAtMillis, updatedAtMillis = updatedAtMillis)

    fun insertBlock(document: NoteDocument, block: NoteBlock, afterBlockId: String?, updatedAtMillis: Long): NoteDocument {
        if (document.blocks.any { it.id == block.id }) return document
        val blocks = document.blocks.toMutableList()
        val index = if (afterBlockId == null) blocks.size else blocks.indexOfFirst { it.id == afterBlockId }.let { if (it < 0) return document else it + 1 }
        blocks.add(index, block)
        return document.copy(blocks = blocks, updatedAtMillis = updatedAtMillis)
    }

    fun replaceBlock(document: NoteDocument, block: NoteBlock, updatedAtMillis: Long): NoteDocument {
        val index = document.blocks.indexOfFirst { it.id == block.id }
        if (index < 0 || document.blocks[index] == block) return document
        val blocks = document.blocks.toMutableList()
        blocks[index] = block
        return document.copy(blocks = blocks, updatedAtMillis = updatedAtMillis)
    }

    fun replaceBlockText(document: NoteDocument, blockId: String, text: StyledText, updatedAtMillis: Long): NoteDocument {
        val index = document.blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return document
        val current = document.blocks[index]
        val replacement = when (current) {
            is NoteBlock.Paragraph -> current.copy(text = text)
            is NoteBlock.Heading -> current.copy(text = text)
            else -> return document
        }
        return replaceBlock(document, replacement, updatedAtMillis)
    }

    fun appendChecklistItem(document: NoteDocument, blockId: String, item: ChecklistItem, updatedAtMillis: Long): NoteDocument {
        val index = document.blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return document
        val checklist = document.blocks[index] as? NoteBlock.Checklist ?: return document
        if (checklist.items.any { it.id == item.id }) return document
        return replaceBlock(document, checklist.copy(items = checklist.items + item), updatedAtMillis)
    }

    fun updateChecklistItemText(document: NoteDocument, blockId: String, itemId: String, text: StyledText, updatedAtMillis: Long): NoteDocument =
        updateChecklistItem(document, blockId, itemId, updatedAtMillis) { if (it.text == text) it else it.copy(text = text) }

    fun removeChecklistItem(document: NoteDocument, blockId: String, itemId: String, updatedAtMillis: Long): NoteDocument {
        val index = document.blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return document
        val checklist = document.blocks[index] as? NoteBlock.Checklist ?: return document
        if (checklist.items.none { it.id == itemId }) return document
        return replaceBlock(document, checklist.copy(items = checklist.items.filterNot { it.id == itemId }), updatedAtMillis)
    }

    private fun updateChecklistItem(document: NoteDocument, blockId: String, itemId: String, updatedAtMillis: Long, transform: (ChecklistItem) -> ChecklistItem): NoteDocument {
        val blockIndex = document.blocks.indexOfFirst { it.id == blockId }
        if (blockIndex < 0) return document
        val checklist = document.blocks[blockIndex] as? NoteBlock.Checklist ?: return document
        val itemIndex = checklist.items.indexOfFirst { it.id == itemId }
        if (itemIndex < 0) return document
        val items = checklist.items.toMutableList()
        val replacement = transform(items[itemIndex])
        if (replacement == items[itemIndex]) return document
        items[itemIndex] = replacement
        val blocks = document.blocks.toMutableList()
        blocks[blockIndex] = checklist.copy(items = items)
        return document.copy(blocks = blocks, updatedAtMillis = updatedAtMillis)
    }
}
