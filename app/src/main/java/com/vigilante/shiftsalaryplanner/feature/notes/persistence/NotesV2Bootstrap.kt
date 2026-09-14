package com.vigilante.shiftsalaryplanner.feature.notes.persistence

import com.vigilante.shiftsalaryplanner.feature.notes.domain.LegacyNoteMigrator
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.settings.AppNote
import org.json.JSONArray

sealed interface NotesV2BootstrapResult {
    data class Ready(
        val documents: List<NoteDocument>,
        val shouldPersist: Boolean,
    ) : NotesV2BootstrapResult

    data object Corrupt : NotesV2BootstrapResult
    data object UnsupportedSchema : NotesV2BootstrapResult
}

object NotesV2Bootstrap {
    fun resolve(
        existingV2Raw: String?,
        legacyNotes: List<AppNote>,
    ): NotesV2BootstrapResult {
        if (existingV2Raw == null) {
            return NotesV2BootstrapResult.Ready(
                documents = legacyNotes.map(LegacyNoteMigrator::migrate),
                shouldPersist = true,
            )
        }

        val array = runCatching { JSONArray(existingV2Raw) }.getOrElse {
            return NotesV2BootstrapResult.Corrupt
        }

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: return NotesV2BootstrapResult.Corrupt
            val schemaVersion = item.optInt("schemaVersion", Int.MIN_VALUE)
            if (schemaVersion != NoteDocument.CURRENT_SCHEMA_VERSION) {
                return NotesV2BootstrapResult.UnsupportedSchema
            }
        }

        val documents = NoteDocumentJsonCodec.decodeList(existingV2Raw)
        if (documents.size != array.length()) {
            return NotesV2BootstrapResult.Corrupt
        }

        return NotesV2BootstrapResult.Ready(
            documents = documents,
            shouldPersist = false,
        )
    }
}
