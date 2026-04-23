package com.vigilante.shiftsalaryplanner

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlin.math.roundToInt

class ShiftAlarmPlaybackService : Service() {

    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmKey: String = ""
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var previousAlarmVolume: Int? = null
    private var rampTargetVolumePercent: Int = 100
    private var rampDurationSeconds: Int = 0
    private var ringDurationSeconds: Int = 180
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ringtoneKeepAliveRunnable = object : Runnable {
        override fun run() {
            val activeRingtone = ringtone ?: return
            val isPlaying = runCatching { activeRingtone.isPlaying }.getOrDefault(false)
            if (!isPlaying) {
                runCatching { activeRingtone.play() }
            }
            mainHandler.postDelayed(this, 1_500L)
        }
    }
    private val autoStopRunnable = Runnable {
        stopPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    private val volumeRampRunnable = object : Runnable {
        private var elapsedSeconds = 0
        override fun run() {
            val target = rampTargetVolumePercent.coerceIn(0, 100)
            val duration = rampDurationSeconds.coerceIn(0, 180)
            if (duration <= 0 || elapsedSeconds >= duration) {
                applyAlarmVolume(target)
                return
            }
            elapsedSeconds += 1
            val progress = (elapsedSeconds.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            val interpolated = (target * progress).roundToInt().coerceIn(0, target)
            applyAlarmVolume(interpolated)
            if (progress < 1f) {
                mainHandler.postDelayed(this, 1_000L)
            }
        }

        fun reset() {
            elapsedSeconds = 0
        }
    }
    private val vibrationStopRunnable = Runnable { stopVibration() }

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
                val canSnooze = snoozeCountLimit > 0 && snoozeCurrentCount < snoozeCountLimit
                if (canSnooze) {
                ShiftAlarmScheduler.scheduleSnooze(
                    context = this,
                    baseAlarmKey = alarmKey.ifBlank { "shift_alarm" },
                    title = title,
                    text = "$text • повтор через $snoozeIntervalMinutes мин",
                    volumePercent = volumePercent,
                    soundUri = soundUri,
                    soundLabel = soundLabel,
                    delayMinutes = snoozeIntervalMinutes,
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
                val snoozeIntervalMinutes = intent?.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_INTERVAL_MINUTES, 10)
                    ?.coerceIn(1, 120) ?: 10
                val snoozeCountLimit = intent?.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_COUNT_LIMIT, 3)
                    ?.coerceIn(0, 10) ?: 3
                val snoozeCurrentCount = intent?.getIntExtra(ShiftAlarmScheduler.EXTRA_SNOOZE_CURRENT_COUNT, 0)
                    ?.coerceAtLeast(0) ?: 0
                val ringDurationSeconds = intent?.getIntExtra(ShiftAlarmScheduler.EXTRA_RING_DURATION_SECONDS, 180)
                    ?.coerceIn(10, 3_600) ?: 180
                val rampUpDurationSeconds = intent?.getIntExtra(ShiftAlarmScheduler.EXTRA_RAMP_UP_DURATION_SECONDS, 0)
                    ?.coerceIn(0, 180) ?: 0
                val vibrationEnabled = intent?.getBooleanExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_ENABLED, true) ?: true
                val vibrationType = runCatching {
                    ShiftAlarmVibrationType.valueOf(
                        intent?.getStringExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_TYPE)
                            ?: ShiftAlarmVibrationType.SYSTEM.name
                    )
                }.getOrElse { ShiftAlarmVibrationType.SYSTEM }
                val vibrationDurationSeconds = intent?.getIntExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_DURATION_SECONDS, 25)
                    ?.coerceIn(0, 300) ?: 25
                val customVibrationPattern = intent?.getStringExtra(ShiftAlarmScheduler.EXTRA_VIBRATION_CUSTOM_PATTERN).orEmpty()
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
                )
                wakeAndOpenRingScreen(
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
                startPlayback(
                    soundUri = soundUri,
                    volumePercent = volumePercent,
                    rampUpDurationSeconds = rampUpDurationSeconds,
                    ringDurationSeconds = ringDurationSeconds
                )
                startVibration(
                    enabled = vibrationEnabled,
                    type = vibrationType,
                    durationSeconds = vibrationDurationSeconds,
                    customPattern = customVibrationPattern
                )
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
    ): Notification {
        val fullScreenIntent = Intent(this, ShiftAlarmRingActivity::class.java).apply {
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
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            requestCodeFor("snooze|$alarmKey"),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canSnooze = snoozeCountLimit > 0 && snoozeCurrentCount < snoozeCountLimit
        val snoozeLabel = "Отложить ${snoozeIntervalMinutes.coerceIn(1, 120)} мин"

        val builder = NotificationCompat.Builder(this, ShiftAlarmScheduler.CHANNEL_ID)
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
            .addAction(0, "Выключить", dismissPendingIntent)
        if (canSnooze) {
            builder.addAction(0, snoozeLabel, snoozePendingIntent)
        }
        return builder.build()
    }

    private fun startPlayback(
        soundUri: String?,
        volumePercent: Int,
        rampUpDurationSeconds: Int,
        ringDurationSeconds: Int
    ) {
        stopPlayback()
        rampTargetVolumePercent = volumePercent.coerceIn(0, 100)
        this.rampDurationSeconds = rampUpDurationSeconds.coerceIn(0, 180)
        this.ringDurationSeconds = ringDurationSeconds.coerceIn(10, 3_600)
        scheduleAutoStop()
        if (this.rampDurationSeconds > 0 && rampTargetVolumePercent > 0) {
            applyAlarmVolume(0)
            scheduleVolumeRamp()
        } else {
            applyAlarmVolume(rampTargetVolumePercent)
        }

        val resolvedUri = resolvePlaybackUri(soundUri)

        val preparedPlayer = MediaPlayer()
        val mediaPlayerReady = runCatching {
            preparedPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            preparedPlayer.setDataSource(applicationContext, resolvedUri)
            preparedPlayer.isLooping = true
            preparedPlayer.prepare()
            preparedPlayer.start()
            true
        }.getOrElse {
            runCatching { preparedPlayer.reset() }
            runCatching { preparedPlayer.release() }
            false
        }
        mediaPlayer = if (mediaPlayerReady) preparedPlayer else null

        if (mediaPlayer != null) return

        ringtone = runCatching {
            RingtoneManager.getRingtone(applicationContext, resolvedUri)
        }.getOrNull()

        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        ringtone?.isLooping = true
        ringtone?.play()
        mainHandler.removeCallbacks(ringtoneKeepAliveRunnable)
        mainHandler.postDelayed(ringtoneKeepAliveRunnable, 1_500L)
    }

    private fun stopPlayback() {
        mainHandler.removeCallbacks(ringtoneKeepAliveRunnable)
        mainHandler.removeCallbacks(autoStopRunnable)
        mainHandler.removeCallbacks(volumeRampRunnable)
        mainHandler.removeCallbacks(vibrationStopRunnable)
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        stopVibration()
        restoreAlarmVolume()
    }

    private fun startVibration(
        enabled: Boolean,
        type: ShiftAlarmVibrationType,
        durationSeconds: Int,
        customPattern: String
    ) {
        if (!enabled) return
        vibrator = getSystemService(Vibrator::class.java)
        val target = vibrator ?: return
        val pattern = resolveVibrationPattern(type, customPattern)
        if (pattern.isEmpty()) return
        mainHandler.removeCallbacks(vibrationStopRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            target.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(pattern, 0)
        }
        if (durationSeconds > 0) {
            mainHandler.postDelayed(vibrationStopRunnable, durationSeconds.coerceIn(1, 300) * 1_000L)
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
        acquireWakeLock()
        val intent = Intent(this, ShiftAlarmRingActivity::class.java).apply {
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

    private fun scheduleAutoStop() {
        mainHandler.removeCallbacks(autoStopRunnable)
        mainHandler.postDelayed(autoStopRunnable, ringDurationSeconds.coerceIn(10, 3_600) * 1_000L)
    }

    private fun scheduleVolumeRamp() {
        mainHandler.removeCallbacks(volumeRampRunnable)
        volumeRampRunnable.reset()
        mainHandler.postDelayed(volumeRampRunnable, 1_000L)
    }

    private fun resolveVibrationPattern(
        type: ShiftAlarmVibrationType,
        customPattern: String
    ): LongArray {
        return when (type) {
            ShiftAlarmVibrationType.SYSTEM -> longArrayOf(0L, 600L, 400L)
            ShiftAlarmVibrationType.SOFT -> longArrayOf(0L, 260L, 340L)
            ShiftAlarmVibrationType.STRONG -> longArrayOf(0L, 900L, 260L)
            ShiftAlarmVibrationType.HEARTBEAT -> longArrayOf(0L, 180L, 100L, 180L, 520L)
            ShiftAlarmVibrationType.CUSTOM -> parseCustomVibrationPattern(customPattern)
        }
    }

    private fun parseCustomVibrationPattern(raw: String): LongArray {
        val parsed = raw
            .split(',', ';', ' ')
            .mapNotNull { chunk -> chunk.trim().toLongOrNull() }
            .filter { it in 30L..5_000L }
        return when {
            parsed.size >= 2 -> parsed.toLongArray()
            else -> longArrayOf(0L, 600L, 400L)
        }
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
            snoozeIntervalMinutes: Int,
            snoozeCountLimit: Int,
            snoozeCurrentCount: Int,
            ringDurationSeconds: Int,
            rampUpDurationSeconds: Int,
            vibrationEnabled: Boolean,
            vibrationType: ShiftAlarmVibrationType,
            vibrationDurationSeconds: Int,
            customVibrationPattern: String,
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
            val intent = Intent(context, ShiftAlarmPlaybackService::class.java).apply {
                action = ACTION_SNOOZE
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
            }
            context.startService(intent)
        }

        private fun notificationIdForKey(key: String): Int = requestCodeFor("notification|$key")

        private fun requestCodeFor(key: String): Int = key.hashCode() and 0x7fffffff
    }
}
