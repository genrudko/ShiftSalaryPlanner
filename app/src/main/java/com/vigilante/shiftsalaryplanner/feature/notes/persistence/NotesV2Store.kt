package com.vigilante.shiftsalaryplanner.feature.notes.persistence

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.settings.AppNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface NotesV2StoreBackend {
    fun readRaw(): String?
    fun writeRaw(raw: String)
}

sealed interface NotesV2StoreState {
    data class Ready(val documents: List<NoteDocument>) : NotesV2StoreState
    data class PersistenceError(val documents: List<NoteDocument>) : NotesV2StoreState
    data object Corrupt : NotesV2StoreState
    data object UnsupportedSchema : NotesV2StoreState
}

class NotesV2Store(
    private val backend: NotesV2StoreBackend,
    private val legacyNotesProvider: () -> List<AppNote>,
) {
    private val _state = MutableStateFlow(initialize())

    val stateFlow: StateFlow<NotesV2StoreState> = _state.asStateFlow()
    val currentState: NotesV2StoreState get() = _state.value

    fun save(document: NoteDocument): Boolean {
        val ready = _state.value as? NotesV2StoreState.Ready ?: return false
        val existingIndex = ready.documents.indexOfFirst { it.id == document.id }
        val next = if (existingIndex >= 0) {
            ready.documents.toMutableList().apply { this[existingIndex] = document }
        } else {
            ready.documents + document
        }

        if (!persist(next)) return false
        _state.value = NotesV2StoreState.Ready(next)
        return true
    }

    private fun initialize(): NotesV2StoreState {
        return when (val result = NotesV2Bootstrap.resolve(backend.readRaw(), legacyNotesProvider())) {
            is NotesV2BootstrapResult.Ready -> {
                if (result.shouldPersist && !persist(result.documents)) {
                    NotesV2StoreState.PersistenceError(result.documents)
                } else {
                    NotesV2StoreState.Ready(result.documents)
                }
            }
            NotesV2BootstrapResult.Corrupt -> NotesV2StoreState.Corrupt
            NotesV2BootstrapResult.UnsupportedSchema -> NotesV2StoreState.UnsupportedSchema
        }
    }

    private fun persist(documents: List<NoteDocument>): Boolean = runCatching {
        backend.writeRaw(NoteDocumentJsonCodec.encodeList(documents).toString())
    }.isSuccess
}
