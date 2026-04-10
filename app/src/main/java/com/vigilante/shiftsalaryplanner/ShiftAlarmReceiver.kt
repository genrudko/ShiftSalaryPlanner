package com.vigilante.shiftsalaryplanner

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.provider.AlarmClock
import androidx.core.app.NotificationCompat
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShiftAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_DISMISS -> dismissAlarmNotification(context, intent)
                    ACTION_SNOOZE -> snoozeAlarm(context, intent)
                    else -> {
                        showAlarmNotification(context, intent)
                        refreshNearestSystemAlarm(context)
                    }
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private fun showAlarmNotification(context: Context, intent: Intent) {
        ShiftAlarmScheduler.ensureNotificationChannel(context)

        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
            .orEmpty()
            .ifBlank { "shift_alarm" }
        val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
        val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
        val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
        val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
        val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
        val notificationId = notificationIdForKey(alarmKey)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            notificationId,
            NotificationCompat.Builder(context, ShiftAlarmScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSound(
                    soundUri?.let { android.net.Uri.parse(it) }
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                )
                .setContentIntent(buildOpenClockPendingIntent(context, alarmKey))
                .addAction(0, "Отложить 10 мин", buildSnoozePendingIntent(context, notificationId, alarmKey, title, text, volumePercent, soundUri, soundLabel))
                .addAction(0, "Выключить", buildDismissPendingIntent(context, notificationId, alarmKey))
                .build()
        )
    }

    private fun snoozeAlarm(context: Context, intent: Intent) {
        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
            .orEmpty()
            .ifBlank { "shift_alarm" }
        val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
        val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
        val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
        val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
        val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()

        ShiftAlarmScheduler.scheduleSnooze(
            context = context,
            baseAlarmKey = alarmKey,
            title = title,
            text = "$text • повтор через 10 мин",
            volumePercent = volumePercent,
            soundUri = soundUri,
            soundLabel = soundLabel,
            delayMinutes = 10
        )

        dismissAlarmNotification(context, intent)
    }

    private fun dismissAlarmNotification(context: Context, intent: Intent) {
        val explicitNotificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY).orEmpty()
        val notificationId = if (explicitNotificationId >= 0) {
            explicitNotificationId
        } else {
            notificationIdForKey(alarmKey)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    private suspend fun refreshNearestSystemAlarm(context: Context) {
        runCatching {
            val db = AppDatabase.getDatabase(context)
            val settings = ShiftAlarmStore(context).settingsFlow.first()
            if (!settings.enabled || !settings.autoReschedule) return
            val savedDays = db.shiftDayDao().observeAll().first()
            val templates = db.shiftTemplateDao().observeAll().first()
            ShiftAlarmScheduler.reschedule(
                context = context,
                settings = settings,
                savedDays = savedDays,
                templateMap = templates.associateBy { it.code },
                mirrorToSystemClockApp = true,
                allowSystemClockUiFallback = false
            )
        }
    }

    private fun buildOpenClockPendingIntent(
        context: Context,
        alarmKey: String
    ): PendingIntent {
        val openClockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fallbackIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val resolvedIntent = if (openClockIntent.resolveActivity(context.packageManager) != null) {
            openClockIntent
        } else {
            fallbackIntent
        }

        return PendingIntent.getActivity(
            context,
            requestCode("open|$alarmKey"),
            resolvedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildDismissPendingIntent(
        context: Context,
        notificationId: Int,
        alarmKey: String
    ): PendingIntent {
        val dismissIntent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("dismiss|$alarmKey"),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSnoozePendingIntent(
        context: Context,
        notificationId: Int,
        alarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String
    ): PendingIntent {
        val snoozeIntent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
            putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
            putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
            putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
            if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
            if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode("snooze|$alarmKey"),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val ACTION_DISMISS = "com.vigilante.shiftsalaryplanner.action.ALARM_DISMISS"
        private const val ACTION_SNOOZE = "com.vigilante.shiftsalaryplanner.action.ALARM_SNOOZE"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"

        private fun notificationIdForKey(key: String): Int = requestCode("notification|$key")

        private fun requestCode(value: String): Int = value.hashCode() and 0x7fffffff
    }
}
