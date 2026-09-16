package com.vigilante.shiftsalaryplanner.feature.notes.persistence

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import org.json.JSONArray
import org.json.JSONObject

interface NotesV2RevisionBackend {
    fun readRaw(): String?
    fun writeRaw(raw: String)
}

data class NoteRevision(
    val capturedAtMillis: Long,
    val document: NoteDocument,
)

class NoteRevisionStore(
    private val backend: NotesV2RevisionBackend,
    private val maxRevisionsPerNote: Int = DEFAULT_MAX_REVISIONS_PER_NOTE,
) {
    private var entries: List<NoteRevision> = decode(backend.readRaw())

    init {
        require(maxRevisionsPerNote > 0) { "maxRevisionsPerNote must be positive" }
    }

    fun revisions(noteId: String): List<NoteRevision> = entries.filter { it.document.id == noteId }

    fun capture(document: NoteDocument, capturedAtMillis: Long): Boolean {
        val lastForNote = entries.lastOrNull { it.document.id == document.id }
        if (lastForNote?.document == document) return true

        val newRevision = NoteRevision(capturedAtMillis, document)
        val noteEntries = (entries.filter { it.document.id == document.id } + newRevision)
            .takeLast(maxRevisionsPerNote)
        val next = entries.filterNot { it.document.id == document.id } + noteEntries
        return persistAndReplace(next)
    }

    fun delete(noteId: String): Boolean {
        if (entries.none { it.document.id == noteId }) return true
        return persistAndReplace(entries.filterNot { it.document.id == noteId })
    }

    private fun persistAndReplace(next: List<NoteRevision>): Boolean {
        val raw = encode(next).toString()
        if (runCatching { backend.writeRaw(raw) }.isFailure) return false
        entries = next
        return true
    }

    private fun encode(revisions: List<NoteRevision>): JSONArray = JSONArray().apply {
        revisions.forEach { revision ->
            put(JSONObject().apply {
                put("capturedAtMillis", revision.capturedAtMillis)
                put("document", NoteDocumentJsonCodec.encodeList(listOf(revision.document)).getJSONObject(0))
            })
        }
    }

    private fun decode(raw: String?): List<NoteRevision> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val documentJson = item.optJSONObject("document") ?: continue
                    val document = NoteDocumentJsonCodec
                        .decodeList(JSONArray().put(documentJson).toString())
                        .singleOrNull() ?: continue
                    add(NoteRevision(item.getLong("capturedAtMillis"), document))
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val DEFAULT_MAX_REVISIONS_PER_NOTE: Int = 20
    }
}
