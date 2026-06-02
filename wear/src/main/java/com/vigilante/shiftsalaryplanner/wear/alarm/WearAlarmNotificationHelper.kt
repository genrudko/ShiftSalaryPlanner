package com.vigilante.shiftsalaryplanner.wear.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.wear.sync.WearSyncContract

object WearAlarmNotificationHelper {
    private const val CHANNEL_ID = "wear_alarm_mirror"
    private const val NOTIFICATION_BASE_ID = 71_000

    fun show(context: Context, payload: WearAlarmPayload) {
        ensureChannel(context)
        val raw = payload.toJson().toString()
        val openIntent = Intent(context, WearAlarmRingActivity::class.java).apply {
            putExtra(WearAlarmRingActivity.EXTRA_ALARM_JSON, raw)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            requestCode("open|${payload.alarmKey}"),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPendingIntent = actionIntent(
            context = context,
            action = WearAlarmActionReceiver.ACTION_DISMISS,
            payload = raw,
            alarmKey = payload.alarmKey
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title)
            .setContentText(payload.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, true)
            .addAction(0, "Выключить", dismissPendingIntent)
        if (payload.canSnooze) {
            builder.addAction(
                0,
                "Отложить",
                actionIntent(
                    context = context,
                    action = WearAlarmActionReceiver.ACTION_SNOOZE,
                    payload = raw,
                    alarmKey = payload.alarmKey
                )
            )
        }
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId(payload.alarmKey), builder.build())
        }
    }

    fun cancel(context: Context, alarmKey: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(notificationId(alarmKey))
        }
    }

    private fun actionIntent(
        context: Context,
        action: String,
        payload: String,
        alarmKey: String
    ): PendingIntent {
        val intent = Intent(context, WearAlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra(WearAlarmActionReceiver.EXTRA_ALARM_JSON, payload)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("$action|$alarmKey"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Будильник на часах",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Дублирование будильника с телефона на Wear OS"
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationId(alarmKey: String): Int = NOTIFICATION_BASE_ID + requestCode(alarmKey) % 10_000

    private fun requestCode(value: String): Int = value.hashCode() and 0x7fffffff
}
