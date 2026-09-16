package com.vigilante.shiftsalaryplanner.feature.notes.compat

import android.content.Context
import com.vigilante.shiftsalaryplanner.feature.notes.media.AndroidNoteMediaImporter
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteMediaFileStore
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteRevisionStore
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NotesV2Store
import java.io.File
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.SharedPreferencesNotesV2Backend
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.SharedPreferencesNotesV2RevisionBackend
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
    val mediaStore = NoteMediaFileStore(File(appContext.filesDir, AndroidNoteMediaImporter.MEDIA_ROOT_NAME))
    return NotesV2Repository(
        store = store,
        legacyMirror = object : LegacyNotesMirror {
            override fun replaceAll(notes: List<AppNote>) = replaceLegacyProjection(notes)
        },
        permanentDeleteCleanup = { document -> mediaStore.deleteNoteMedia(document.id) },
        revisionStore = NoteRevisionStore(SharedPreferencesNotesV2RevisionBackend(prefs)),
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
