package com.vigilante.shiftsalaryplanner.feature.notes.domain

enum class NoteLibraryLocation { ACTIVE, ARCHIVE, TRASH }
enum class NoteContentType { TEXT, CHECKLIST, IMAGE, VIDEO, AUDIO, FILE }

data class NoteLibraryQuery(
    val text: String = "",
    val labels: Set<String> = emptySet(),
    val contentTypes: Set<NoteContentType> = emptySet(),
    val pinnedOnly: Boolean = false,
    val location: NoteLibraryLocation = NoteLibraryLocation.ACTIVE,
)

object NoteLibraryQueryEngine {
    fun query(notes: Iterable<NoteDocument>, query: NoteLibraryQuery): List<NoteDocument> = notes.asSequence()
        .filter { it.isIn(query.location) }
        .filter { !query.pinnedOnly || it.pinned }
        .filter { query.labels.isEmpty() || it.labels.containsAll(query.labels) }
        .filter { query.contentTypes.isEmpty() || it.contentTypes().any(query.contentTypes::contains) }
        .filter { query.text.isBlank() || it.searchableStrings().any { value -> value.contains(query.text.trim(), ignoreCase = true) } }
        .sortedWith(compareByDescending<NoteDocument> { it.pinned }.thenByDescending { it.updatedAtMillis })
        .toList()

    private fun NoteDocument.isIn(location: NoteLibraryLocation): Boolean = when (location) {
        NoteLibraryLocation.ACTIVE -> deletedAtMillis == null && !archived
        NoteLibraryLocation.ARCHIVE -> deletedAtMillis == null && archived
        NoteLibraryLocation.TRASH -> deletedAtMillis != null
    }

    private fun NoteDocument.contentTypes(): Set<NoteContentType> = buildSet {
        blocks.forEach { block -> when (block) {
            is NoteBlock.Paragraph, is NoteBlock.Heading -> add(NoteContentType.TEXT)
            is NoteBlock.Checklist -> add(NoteContentType.CHECKLIST)
            is NoteBlock.Image -> add(NoteContentType.IMAGE)
            is NoteBlock.Video -> add(NoteContentType.VIDEO)
            is NoteBlock.Audio -> add(NoteContentType.AUDIO)
            is NoteBlock.FileAttachment -> add(NoteContentType.FILE)
            is NoteBlock.Divider -> Unit
        } }
    }

    private fun NoteDocument.searchableStrings(): Sequence<String> = sequence {
        yield(title)
        labels.forEach { yield(it) }
        blocks.forEach { block -> when (block) {
            is NoteBlock.Paragraph -> yield(block.text.text)
            is NoteBlock.Heading -> yield(block.text.text)
            is NoteBlock.Checklist -> block.items.forEach { yield(it.text.text) }
            is NoteBlock.Image -> yield(block.label)
            is NoteBlock.Video -> yield(block.label)
            is NoteBlock.Audio -> { yield(block.label); block.transcript?.let { yield(it.text) } }
            is NoteBlock.FileAttachment -> yield(block.label)
            is NoteBlock.Divider -> Unit
        } }
    }
}
