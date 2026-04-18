package com.vigilante.shiftsalaryplanner

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlin.math.roundToInt

class ShiftAlarmPlaybackService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmKey: String = ""
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var previousAlarmVolume: Int? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_SNOOZE -> {
                val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY).orEmpty()
                val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
                val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
                val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
                val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
                val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
                ShiftAlarmScheduler.scheduleSnooze(
                    context = this,
                    baseAlarmKey = alarmKey.ifBlank { "shift_alarm" },
                    title = title,
                    text = "$text • повтор через 10 мин",
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel,
                    delayMinutes = 10
                )
                stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {
                val alarmKey = intent?.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
                    .orEmpty()
                    .ifBlank { "shift_alarm" }
                val title = intent?.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
                val text = intent?.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
                val volumePercent = intent?.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100)
                    ?.coerceIn(0, 100) ?: 100
                val soundUri = intent?.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
                val soundLabel = intent?.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
                currentAlarmKey = alarmKey
                ShiftAlarmScheduler.ensureNotificationChannel(this)
                startForeground(
                    notificationIdForKey(alarmKey),
                    buildForegroundNotification(
                        alarmKey = alarmKey,
                        title = title,
                        text = text,
                        volumePercent = volumePercent,
                        soundUri = soundUri,
                        soundLabel = soundLabel
                    )
                )
                wakeAndOpenRingScreen(
                    alarmKey = alarmKey,
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel
                )
                startPlayback(soundUri, volumePercent)
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        stopPlayback()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun buildForegroundNotification(
        alarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String
    ): Notification {
        val fullScreenIntent = Intent(this, ShiftAlarmRingActivity::class.java).apply {
            putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
            putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
            putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
            putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
            if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
            if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            requestCodeFor("open|$alarmKey"),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, ShiftAlarmPlaybackService::class.java).apply {
            action = ACTION_STOP
            putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            requestCodeFor("dismiss|$alarmKey"),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, ShiftAlarmPlaybackService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
            putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
            putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
            putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
            if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
            if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            requestCodeFor("snooze|$alarmKey"),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ShiftAlarmScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Отложить 10 мин", snoozePendingIntent)
            .addAction(0, "Выключить", dismissPendingIntent)
            .build()
    }

    private fun startPlayback(soundUri: String?, volumePercent: Int) {
        stopPlayback()
        applyAlarmVolume(volumePercent)
        val resolvedUri = resolvePlaybackUri(soundUri)
        ringtone = runCatching {
            RingtoneManager.getRingtone(applicationContext, resolvedUri)
        }.getOrNull()
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone?.play()
        startVibration()
    }

    private fun stopPlayback() {
        runCatching { ringtone?.stop() }
        ringtone = null
        stopVibration()
        restoreAlarmVolume()
    }

    private fun startVibration() {
        vibrator = getSystemService(Vibrator::class.java)
        val target = vibrator ?: return
        val pattern = longArrayOf(0L, 600L, 400L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            target.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun wakeAndOpenRingScreen(
        alarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String
    ) {
        acquireWakeLock()
        val intent = Intent(this, ShiftAlarmRingActivity::class.java).apply {
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
            this,
            requestCodeFor("wake-open|$alarmKey"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val launched = runCatching { pending.send() }.isSuccess
        if (!launched) {
            runCatching { startActivity(intent) }
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        releaseWakeLock()
        wakeLock = runCatching {
            @Suppress("DEPRECATION")
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "ShiftSalaryPlanner:AlarmWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12_000L)
            }
        }.getOrNull()
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun applyAlarmVolume(volumePercent: Int) {
        val manager = audioManager ?: getSystemService(AudioManager::class.java).also { audioManager = it }
        if (manager == null) return
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val requested = volumePercent.coerceIn(0, 100)
        val target = if (requested <= 0) 0 else ((max * (requested / 100f)).roundToInt()).coerceIn(1, max)
        val current = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        if (previousAlarmVolume == null) {
            previousAlarmVolume = current
        }
        if (current != target) {
            runCatching {
                manager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            }
        }
    }

    private fun restoreAlarmVolume() {
        val manager = audioManager ?: return
        val previous = previousAlarmVolume ?: return
        runCatching {
            manager.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
        }
        previousAlarmVolume = null
    }

    private fun resolvePlaybackUri(soundUri: String?): android.net.Uri {
        if (!soundUri.isNullOrBlank()) {
            return soundUri.toUri()
        }
        return RingtoneManager.getActualDefaultRingtoneUri(
            applicationContext,
            RingtoneManager.TYPE_ALARM
        )
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(
                applicationContext,
                RingtoneManager.TYPE_RINGTONE
            )
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    companion object {
        private const val ACTION_START = "com.vigilante.shiftsalaryplanner.action.ALARM_START_PLAYBACK"
        private const val ACTION_STOP = "com.vigilante.shiftsalaryplanner.action.ALARM_STOP_PLAYBACK"
        private const val ACTION_SNOOZE = "com.vigilante.shiftsalaryplanner.action.ALARM_SNOOZE_PLAYBACK"
        const val EXTRA_SKIP_RING_UI_LAUNCH = "extra_skip_ring_ui_launch"

        fun startRinging(
            context: Context,
            alarmKey: String,
            title: String,
            text: String,
            volumePercent: Int,
            soundUri: String?,
            soundLabel: String,
            skipRingUiLaunch: Boolean = false
        ) {
            val intent = Intent(context, ShiftAlarmPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
                putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
                putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
                putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
                if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
                if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
                putExtra(EXTRA_SKIP_RING_UI_LAUNCH, skipRingUiLaunch)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context, alarmKey: String) {
            val intent = Intent(context, ShiftAlarmPlaybackService::class.java).apply {
                action = ACTION_STOP
                putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
            }
            context.startService(intent)
        }

        fun snooze(
            context: Context,
            alarmKey: String,
            title: String,
            text: String,
            volumePercent: Int,
            soundUri: String?,
            soundLabel: String
        ) {
            val intent = Intent(context, ShiftAlarmPlaybackService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
                putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
                putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
                putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
                if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
                if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
            }
            context.startService(intent)
        }

        private fun notificationIdForKey(key: String): Int = requestCodeFor("notification|$key")

        private fun requestCodeFor(key: String): Int = key.hashCode() and 0x7fffffff
    }
}
