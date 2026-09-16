package com.vigilante.shiftsalaryplanner.feature.notes.reminder

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vigilante.shiftsalaryplanner.MainActivity
import com.vigilante.shiftsalaryplanner.widget.EXTRA_OPEN_TAB

class NoteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        AndroidNoteReminderScheduler.ensureNotificationChannel(context)
        val noteId = intent.getStringExtra(AndroidNoteReminderScheduler.EXTRA_NOTE_ID).orEmpty()
        val title = intent.getStringExtra(AndroidNoteReminderScheduler.EXTRA_TITLE).orEmpty().ifBlank { "Напоминание" }
        val text = intent.getStringExtra(AndroidNoteReminderScheduler.EXTRA_TEXT).orEmpty().ifBlank { "Откройте заметку" }
        val contentIntent = PendingIntent.getActivity(
            context,
            noteId.hashCode() and 0x7fffffff,
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_TAB, "NOTES")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, AndroidNoteReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(noteId.hashCode() and 0x7fffffff, notification)
    }
}
