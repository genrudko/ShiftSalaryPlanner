package com.vigilante.shiftsalaryplanner.feature.notes.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument

interface NoteReminderSync {
    fun sync(document: NoteDocument)
    fun cancel(noteId: String)
    fun rescheduleAll(documents: List<NoteDocument>)
}

object NoteReminderPlanner {
    fun pending(documents: List<NoteDocument>, nowMillis: Long): List<NoteDocument> = documents
        .asSequence()
        .filter { !it.archived && it.deletedAtMillis == null }
        .filter { (it.reminderAtMillis ?: Long.MIN_VALUE) > nowMillis }
        .sortedBy { it.reminderAtMillis }
        .toList()
}

class AndroidNoteReminderScheduler(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : NoteReminderSync {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun sync(document: NoteDocument) {
        cancel(document.id)
        val trigger = document.reminderAtMillis ?: return
        if (document.archived || document.deletedAtMillis != null || trigger <= clock()) return
        ensureNotificationChannel(appContext)
        val pending = PendingIntent.getBroadcast(
            appContext,
            requestCode(document.id),
            Intent(appContext, NoteReminderReceiver::class.java).apply {
                action = ACTION_NOTE_REMINDER
                putExtra(EXTRA_NOTE_ID, document.id)
                putExtra(EXTRA_TITLE, document.title.ifBlank { "Напоминание" })
                putExtra(EXTRA_TEXT, reminderPreview(document))
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    override fun cancel(noteId: String) {
        val pending = PendingIntent.getBroadcast(
            appContext,
            requestCode(noteId),
            Intent(appContext, NoteReminderReceiver::class.java).apply { action = ACTION_NOTE_REMINDER },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    override fun rescheduleAll(documents: List<NoteDocument>) {
        NoteReminderPlanner.pending(documents, clock()).forEach(::sync)
    }

    private fun reminderPreview(document: NoteDocument): String {
        document.blocks.forEach { block ->
            val text = when (block) {
                is NoteBlock.Paragraph -> block.text.text
                is NoteBlock.Heading -> block.text.text
                is NoteBlock.Checklist -> block.items.firstOrNull()?.text?.text
                else -> null
            }?.trim().orEmpty()
            if (text.isNotBlank()) return text.take(160)
        }
        return "Откройте заметку"
    }

    companion object {
        const val CHANNEL_ID = "note_reminders_v2"
        const val ACTION_NOTE_REMINDER = "com.vigilante.shiftsalaryplanner.NOTES_V2_REMINDER"
        const val EXTRA_NOTE_ID = "notes_v2_reminder_note_id"
        const val EXTRA_TITLE = "notes_v2_reminder_title"
        const val EXTRA_TEXT = "notes_v2_reminder_text"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Напоминания о заметках", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Напоминания, заданные в заметках"
                }
            )
        }

        private fun requestCode(noteId: String): Int = noteId.hashCode() and 0x7fffffff
    }
}
