package com.vigilante.shiftsalaryplanner.feature.notes.compat

import android.content.Context
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Store
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.SharedPreferencesNotesV2Backend
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNotesStore
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences

fun createNotesV2Repository(
    context: Context,
    legacyNotesProvider: () -> List<AppNote>,
    replaceLegacyProjection: (List<AppNote>) -> Unit,
): NotesV2Repository {
    val appContext = context.applicationContext
    val prefs = appContext.profileSharedPreferences(AppNotesStore.PREFS_NAME)
    val store = NotesV2Store(
        backend = SharedPreferencesNotesV2Backend(prefs),
        legacyNotesProvider = legacyNotesProvider,
    )
    return NotesV2Repository(
        store = store,
        legacyMirror = object : LegacyNotesMirror {
            override fun replaceAll(notes: List<AppNote>) = replaceLegacyProjection(notes)
        },
    )
}

fun createNotesV2Repository(context: Context): NotesV2Repository {
    val legacyStore = AppNotesStore(context.applicationContext)
    return createNotesV2Repository(
        context = context,
        legacyNotesProvider = legacyStore::allNotesSnapshot,
        replaceLegacyProjection = legacyStore::replaceAllProjected,
    )
}
