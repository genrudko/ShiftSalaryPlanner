package com.vigilante.shiftsalaryplanner.feature.notes.compat

import com.vigilante.shiftsalaryplanner.feature.notes.domain.LegacyNoteMigrator
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocumentMutations
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteRevisionStore
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Store
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreState
import com.vigilante.shiftsalaryplanner.feature.notes.reminder.NoteReminderSync
import com.vigilante.shiftsalaryplanner.settings.AppNote
import kotlinx.coroutines.flow.StateFlow

interface LegacyNotesMirror {
    fun replaceAll(notes: List<AppNote>)
}

class NotesV2Repository(
    private val store: NotesV2Store,
    private val legacyMirror: LegacyNotesMirror,
    private val revisionStore: NoteRevisionStore? = null,
    private val reminderSync: NoteReminderSync? = null,
    private val permanentDeleteCleanup: (NoteDocument) -> Unit = {},
) {
    val stateFlow: StateFlow<NotesV2StoreState> = store.stateFlow
    val currentState: NotesV2StoreState get() = store.currentState

    init {
        syncLegacyMirrorIfReady()
    }

    fun save(document: NoteDocument): Boolean {
        if (!store.save(document)) return false
        syncLegacyMirrorIfReady()
        runCatching { reminderSync?.sync(document) }
        return true
    }

    fun saveLegacy(note: AppNote): Boolean = save(LegacyNoteMigrator.migrate(note))

    fun saveFromEditor(
        document: NoteDocument,
        revisionBaseline: NoteDocument?,
        capturedAtMillis: Long,
    ): Boolean {
        if (revisionBaseline != null && revisionBaseline != document) {
            val revisions = revisionStore
            if (revisions != null && !revisions.capture(revisionBaseline, capturedAtMillis)) return false
        }
        return save(document)
    }

    fun moveToTrash(id: String, deletedAtMillis: Long): Boolean {
        val document = readyDocuments().firstOrNull { it.id == id } ?: return false
        return save(NoteDocumentMutations.moveToTrash(document, deletedAtMillis))
    }

    fun restoreFromTrash(id: String, updatedAtMillis: Long): Boolean {
        val document = readyDocuments().firstOrNull { it.id == id } ?: return false
        return save(NoteDocumentMutations.restoreFromTrash(document, updatedAtMillis))
    }

    fun setArchived(id: String, archived: Boolean, updatedAtMillis: Long): Boolean {
        val document = readyDocuments().firstOrNull { it.id == id } ?: return false
        return save(NoteDocumentMutations.setArchived(document, archived, updatedAtMillis))
    }

    fun deletePermanently(id: String): Boolean {
        val document = readyDocuments().firstOrNull { it.id == id } ?: return false
        if (revisionStore?.delete(id) == false) return false
        if (!store.delete(id)) return false
        syncLegacyMirrorIfReady()
        runCatching { reminderSync?.cancel(id) }
        runCatching { permanentDeleteCleanup(document) }
        return true
    }

    fun document(id: String): NoteDocument? = readyDocuments().firstOrNull { it.id == id }

    fun documents(): List<NoteDocument> = readyDocuments()

    private fun readyDocuments(): List<NoteDocument> = when (val state = store.currentState) {
        is NotesV2StoreState.Ready -> state.documents
        is NotesV2StoreState.PersistenceError -> state.documents
        NotesV2StoreState.Corrupt,
        NotesV2StoreState.UnsupportedSchema -> emptyList()
    }

    private fun syncLegacyMirrorIfReady() {
        val ready = store.currentState as? NotesV2StoreState.Ready ?: return
        legacyMirror.replaceAll(LegacyNoteProjector.projectAll(ready.documents))
    }
}
