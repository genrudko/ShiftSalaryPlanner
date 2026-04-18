package com.vigilante.shiftsalaryplanner

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ShiftAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISMISS -> {
                val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
                    .orEmpty()
                    .ifBlank { "shift_alarm" }
                ShiftAlarmPlaybackService.stop(context, alarmKey)
            }

            ACTION_SNOOZE -> {
                val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
                    .orEmpty()
                    .ifBlank { "shift_alarm" }
                val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
                val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
                val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
                val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
                val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
                ShiftAlarmPlaybackService.snooze(
                    context = context,
                    alarmKey = alarmKey,
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel
                )
            }

            else -> {
                val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
                    .orEmpty()
                    .ifBlank { "shift_alarm" }
                val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
                val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
                val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
                val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
                val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
                launchRingUi(
                    context = context,
                    alarmKey = alarmKey,
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel
                )
                ShiftAlarmPlaybackService.startRinging(
                    context = context,
                    alarmKey = alarmKey,
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel,
                    skipRingUiLaunch = true
                )
            }
        }
    }

    private fun launchRingUi(
        context: Context,
        alarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String
    ) {
        val ringIntent = Intent(context, ShiftAlarmRingActivity::class.java).apply {
            putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
            putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
            putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
            putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
            if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
            if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            requestCodeFor("ring|$alarmKey"),
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val launched = runCatching {
            pending.send()
        }.isSuccess
        if (!launched) {
            runCatching { context.startActivity(ringIntent) }
        }
    }

    private fun requestCodeFor(key: String): Int = key.hashCode() and 0x7fffffff

    companion object {
        private const val ACTION_DISMISS = "com.vigilante.shiftsalaryplanner.action.ALARM_DISMISS"
        private const val ACTION_SNOOZE = "com.vigilante.shiftsalaryplanner.action.ALARM_SNOOZE"
    }
}
