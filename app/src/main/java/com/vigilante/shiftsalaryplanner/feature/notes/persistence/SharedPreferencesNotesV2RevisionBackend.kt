package com.vigilante.shiftsalaryplanner.feature.notes.persistence

import android.content.SharedPreferences

class SharedPreferencesNotesV2RevisionBackend(
    private val prefs: SharedPreferences,
) : NotesV2RevisionBackend {
    override fun readRaw(): String? = prefs.getString(KEY_NOTES_V2_REVISIONS_JSON, null)

    override fun writeRaw(raw: String) {
        check(prefs.edit().putString(KEY_NOTES_V2_REVISIONS_JSON, raw).commit()) {
            "Unable to persist Notes v2 revision history"
        }
    }

    companion object {
        const val KEY_NOTES_V2_REVISIONS_JSON: String = "notes_v2_revisions_json"
    }
}
