package com.vigilante.shiftsalaryplanner.feature.notes.persistence

import android.content.Context
import android.content.SharedPreferences
import com.vigilante.shiftsalaryplanner.settings.AppNotesStore
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences

class SharedPreferencesNotesV2Backend(
    private val prefs: SharedPreferences,
) : NotesV2StoreBackend {
    override fun readRaw(): String? = prefs.getString(KEY_NOTES_V2_JSON, null)

    override fun writeRaw(raw: String) {
        check(prefs.edit().putString(KEY_NOTES_V2_JSON, raw).commit()) {
            "Failed to persist Notes v2 payload"
        }
    }

    companion object {
        const val KEY_NOTES_V2_JSON: String = "notes_v2_json"
    }
}

fun createNotesV2Store(context: Context): NotesV2Store {
    val appContext = context.applicationContext
    val prefs = appContext.profileSharedPreferences(AppNotesStore.PREFS_NAME)
    val legacyStore = AppNotesStore(appContext)
    return NotesV2Store(SharedPreferencesNotesV2Backend(prefs)) {
        legacyStore.allNotesSnapshot()
    }
}
