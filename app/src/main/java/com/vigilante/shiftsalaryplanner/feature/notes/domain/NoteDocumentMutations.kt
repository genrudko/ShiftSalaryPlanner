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
