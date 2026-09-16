package com.vigilante.shiftsalaryplanner.feature.notes.domain

enum class NoteLibraryAction {
    ARCHIVE,
    UNARCHIVE,
    MOVE_TO_TRASH,
    RESTORE,
    DELETE_PERMANENTLY,
}

object NoteLibraryActions {
    fun availableFor(location: NoteLibraryLocation): List<NoteLibraryAction> = when (location) {
        NoteLibraryLocation.ACTIVE -> listOf(NoteLibraryAction.ARCHIVE, NoteLibraryAction.MOVE_TO_TRASH)
        NoteLibraryLocation.ARCHIVE -> listOf(NoteLibraryAction.UNARCHIVE, NoteLibraryAction.MOVE_TO_TRASH)
        NoteLibraryLocation.TRASH -> listOf(NoteLibraryAction.RESTORE, NoteLibraryAction.DELETE_PERMANENTLY)
    }
}
