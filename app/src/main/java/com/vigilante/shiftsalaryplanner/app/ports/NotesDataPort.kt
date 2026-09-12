package com.vigilante.shiftsalaryplanner.app.ports

import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNotesStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface NotesDataPort {
    val notes: Flow<List<AppNote>>
    fun notesForDate(date: LocalDate): List<AppNote>
    fun save(note: AppNote)
    fun delete(id: String)
}

class DefaultNotesDataPort(
    private val store: AppNotesStore
) : NotesDataPort {
    override val notes: Flow<List<AppNote>> = store.notesFlow
    override fun notesForDate(date: LocalDate): List<AppNote> = store.notesForDate(date)
    override fun save(note: AppNote) = store.save(note)
    override fun delete(id: String) = store.delete(id)
}
