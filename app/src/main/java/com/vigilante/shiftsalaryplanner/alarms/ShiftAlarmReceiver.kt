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
                val snoozeIntervalMinutes = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_INTERVAL_MINUTES, 10).coerceIn(1, 120)
                val snoozeCountLimit = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_COUNT_LIMIT, 3).coerceIn(0, 10)
                val snoozeCurrentCount = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_CURRENT_COUNT, 0).coerceAtLeast(0)
                val ringDurationSeconds = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_RING_DURATION_SECONDS, 180).coerceIn(10, 3_600)
                val rampUpDurationSeconds = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_RAMP_UP_DURATION_SECONDS, 0).coerceIn(0, 180)
                val vibrationEnabled = intent.getBooleanExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_ENABLED, true)
                val vibrationType = runCatching {
                    ShiftAlarmVibrationType.valueOf(
                        intent.getStringExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_TYPE)
                            ?: ShiftAlarmVibrationType.SYSTEM.name
                    )
                }.getOrElse { ShiftAlarmVibrationType.SYSTEM }
                val vibrationDurationSeconds = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_DURATION_SECONDS, 25).coerceIn(0, 300)
                val customVibrationPattern = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_CUSTOM_PATTERN).orEmpty()
                ShiftAlarmPlaybackService.snooze(
                    context = context,
                    alarmKey = alarmKey,
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel,
                    snoozeIntervalMinutes = snoozeIntervalMinutes,
                    snoozeCountLimit = snoozeCountLimit,
                    snoozeCurrentCount = snoozeCurrentCount,
                    ringDurationSeconds = ringDurationSeconds,
                    rampUpDurationSeconds = rampUpDurationSeconds,
                    vibrationEnabled = vibrationEnabled,
                    vibrationType = vibrationType,
                    vibrationDurationSeconds = vibrationDurationSeconds,
                    customVibrationPattern = customVibrationPattern
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
                val snoozeIntervalMinutes = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_INTERVAL_MINUTES, 10).coerceIn(1, 120)
                val snoozeCountLimit = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_COUNT_LIMIT, 3).coerceIn(0, 10)
                val snoozeCurrentCount = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_CURRENT_COUNT, 0).coerceAtLeast(0)
                val ringDurationSeconds = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_RING_DURATION_SECONDS, 180).coerceIn(10, 3_600)
                val rampUpDurationSeconds = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_RAMP_UP_DURATION_SECONDS, 0).coerceIn(0, 180)
                val vibrationEnabled = intent.getBooleanExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_ENABLED, true)
                val vibrationType = runCatching {
                    ShiftAlarmVibrationType.valueOf(
                        intent.getStringExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_TYPE)
                            ?: ShiftAlarmVibrationType.SYSTEM.name
                    )
                }.getOrElse { ShiftAlarmVibrationType.SYSTEM }
                val vibrationDurationSeconds = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_DURATION_SECONDS, 25).coerceIn(0, 300)
                val customVibrationPattern = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_CUSTOM_PATTERN).orEmpty()
                launchRingUi(
                    context = context,
                    alarmKey = alarmKey,
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel,
                    snoozeIntervalMinutes = snoozeIntervalMinutes,
                    snoozeCountLimit = snoozeCountLimit,
                    snoozeCurrentCount = snoozeCurrentCount,
                    ringDurationSeconds = ringDurationSeconds,
                    rampUpDurationSeconds = rampUpDurationSeconds,
                    vibrationEnabled = vibrationEnabled,
                    vibrationType = vibrationType,
                    vibrationDurationSeconds = vibrationDurationSeconds,
                    customVibrationPattern = customVibrationPattern
                )
                ShiftAlarmPlaybackService.startRinging(
                    context = context,
                    alarmKey = alarmKey,
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel,
                    snoozeIntervalMinutes = snoozeIntervalMinutes,
                    snoozeCountLimit = snoozeCountLimit,
                    snoozeCurrentCount = snoozeCurrentCount,
                    ringDurationSeconds = ringDurationSeconds,
                    rampUpDurationSeconds = rampUpDurationSeconds,
                    vibrationEnabled = vibrationEnabled,
                    vibrationType = vibrationType,
                    vibrationDurationSeconds = vibrationDurationSeconds,
                    customVibrationPattern = customVibrationPattern,
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
        soundLabel: String,
        snoozeIntervalMinutes: Int,
        snoozeCountLimit: Int,
        snoozeCurrentCount: Int,
        ringDurationSeconds: Int,
        rampUpDurationSeconds: Int,
        vibrationEnabled: Boolean,
        vibrationType: ShiftAlarmVibrationType,
        vibrationDurationSeconds: Int,
        customVibrationPattern: String
    ) {
        val ringIntent = Intent(context, ShiftAlarmRingActivity::class.java).apply {
            putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
            putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
            putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
            putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
            if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
            if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
            putExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_INTERVAL_MINUTES, snoozeIntervalMinutes.coerceIn(1, 120))
            putExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_COUNT_LIMIT, snoozeCountLimit.coerceIn(0, 10))
            putExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_CURRENT_COUNT, snoozeCurrentCount.coerceAtLeast(0))
            putExtra(ShiftAlarmScheduler.EXTRA_RING_DURATION_SECONDS, ringDurationSeconds.coerceIn(10, 3_600))
            putExtra(ShiftAlarmScheduler.EXTRA_RAMP_UP_DURATION_SECONDS, rampUpDurationSeconds.coerceIn(0, 180))
            putExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            putExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_TYPE, vibrationType.name)
            putExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_DURATION_SECONDS, vibrationDurationSeconds.coerceIn(0, 300))
            if (customVibrationPattern.isNotBlank()) {
                putExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_CUSTOM_PATTERN, customVibrationPattern.trim())
            }
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
