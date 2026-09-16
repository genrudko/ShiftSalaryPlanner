package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.reminder.NoteReminderPlanner
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2ReminderTest {
    @Test
    fun `planner schedules only future active reminders`() {
        val now = 10_000L
        val docs = listOf(
            note("future", now + 5_000),
            note("past", now - 1),
            note("none", null),
            note("archived", now + 4_000, archived = true),
            note("trash", now + 3_000, deletedAt = now),
        )
        assertEquals(listOf("future"), NoteReminderPlanner.pending(docs, now).map { it.id })
    }

    @Test
    fun `android scheduler receiver and editor wire reminder runtime`() {
        val base = "src/main/java/com/vigilante/shiftsalaryplanner/"
        val scheduler = File(base + "feature/notes/reminder/AndroidNoteReminderScheduler.kt").readText()
        val receiver = File(base + "feature/notes/reminder/NoteReminderReceiver.kt").readText()
        val editor = File(base + "feature/notes/ui/NotesV2AndroidEditorScreen.kt").readText()
        val repository = File(base + "feature/notes/compat/NotesV2Repository.kt").readText()
        val systemReceiver = File(base + "alarms/ShiftAlarmSystemReceiver.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(scheduler.contains("setAndAllowWhileIdle"))
        assertTrue(receiver.contains("NotificationCompat.Builder"))
        assertTrue(manifest.contains("feature.notes.reminder.NoteReminderReceiver"))
        assertTrue(systemReceiver.contains("rescheduleAll"))
        assertTrue(editor.contains("DatePickerDialog"))
        assertTrue(editor.contains("TimePickerDialog"))
        assertTrue(editor.contains("setReminder"))
        assertTrue(repository.contains("reminderSync?.sync(document)"))
        assertTrue(repository.contains("reminderSync?.cancel(id)"))
    }

    private fun note(
        id: String,
        reminderAt: Long?,
        archived: Boolean = false,
        deletedAt: Long? = null,
    ) = NoteDocument(
        id = id,
        title = id,
        reminderAtMillis = reminderAt,
        archived = archived,
        deletedAtMillis = deletedAt,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
    )
}
