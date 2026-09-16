package com.vigilante.shiftsalaryplanner.feature.notes.compat

import com.vigilante.shiftsalaryplanner.feature.notes.domain.LegacyNoteMigrator
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocumentMutations
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Store
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2StoreState
import com.vigilante.shiftsalaryplanner.settings.AppNote
import kotlinx.coroutines.flow.StateFlow

interface LegacyNotesMirror {
    fun replaceAll(notes: List<AppNote>)
}

class NotesV2Repository(
    private val store: NotesV2Store,
    private val legacyMirror: LegacyNotesMirror,
) {
    val stateFlow: StateFlow<NotesV2StoreState> = store.stateFlow
    val currentState: NotesV2StoreState get() = store.currentState

    init {
        syncLegacyMirrorIfReady()
    }

    fun save(document: NoteDocument): Boolean {
        if (!store.save(document)) return false
        syncLegacyMirrorIfReady()
        return true
    }

    fun saveLegacy(note: AppNote): Boolean = save(LegacyNoteMigrator.migrate(note))

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
        if (!store.delete(id)) return false
        syncLegacyMirrorIfReady()
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
