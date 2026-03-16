package com.vigilante.shiftsalaryplanner

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ShiftAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ShiftAlarmScheduler.ensureNotificationChannel(context)

        val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE)
            ?: "Скоро смена"
        val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT)
            ?: "Проверь календарь смен"
        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
            ?: "shift_alarm"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            1001001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, ShiftAlarmScheduler.CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_ALARM)
            .setDefaults(Notification.DEFAULT_ALL)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_HIGH)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmKey.hashCode() and 0x7fffffff, builder.build())
    }
}
