package com.vigilante.shiftsalaryplanner

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class ShiftAlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentNotificationId: Int = 77231

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> startAlarm(intent)
            ACTION_DISMISS_ALARM -> dismissAlarm()
            ACTION_SNOOZE_ALARM -> snoozeAlarm(intent)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun startAlarm(intent: Intent) {
        ensureRingingChannel(this)

        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY).orEmpty().ifBlank { "shift_alarm" }
        val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
        val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
        val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
        val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
        val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)

        currentNotificationId = (alarmKey.hashCode() and 0x7fffffff)

        val fullScreenPendingIntent = createFullScreenPendingIntent(
            alarmKey = alarmKey,
            title = title,
            text = text,
            volumePercent = volumePercent,
            soundUri = soundUri,
            soundLabel = soundLabel
        )

        startForeground(
            currentNotificationId,
            buildNotification(
                alarmKey = alarmKey,
                title = title,
                text = text,
                volumePercent = volumePercent,
                soundUri = soundUri,
                soundLabel = soundLabel,
                fullScreenIntent = fullScreenPendingIntent
            )
        )

        playSound(soundUri, volumePercent)
        startVibration()

        Handler(Looper.getMainLooper()).post {
            runCatching { fullScreenPendingIntent.send() }
        }
    }

    private fun createFullScreenPendingIntent(
        alarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String
    ): PendingIntent {
        return PendingIntent.getActivity(
            this,
            ("fullscreen|$alarmKey").hashCode() and 0x7fffffff,
            Intent(this, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
                putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
                putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
                putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
                if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
                if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dismissAlarm() {
        stopPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun snoozeAlarm(intent: Intent) {
        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY).orEmpty().ifBlank { "shift_alarm" }
        val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
        val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
        val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
        val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
        val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)

        ShiftAlarmScheduler.scheduleSnooze(
            context = this,
            baseAlarmKey = alarmKey,
            title = title,
            text = "$text • повтор через 10 мин",
            volumePercent = volumePercent,
            soundUri = soundUri,
            soundLabel = soundLabel,
            delayMinutes = 10
        )
        dismissAlarm()
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun buildNotification(
        alarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String,
        fullScreenIntent: PendingIntent
    ): Notification {
        val dismissIntent = PendingIntent.getService(
            this,
            ("dismiss|$alarmKey").hashCode() and 0x7fffffff,
            Intent(this, ShiftAlarmRingingService::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getService(
            this,
            ("snooze|$alarmKey").hashCode() and 0x7fffffff,
            Intent(this, ShiftAlarmRingingService::class.java).apply {
                action = ACTION_SNOOZE_ALARM
                putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
                putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
                putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
                putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
                if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
                if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, RINGING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .addAction(0, "Отложить 10 мин", snoozeIntent)
            .addAction(0, "Выключить", dismissIntent)
            .build()
    }

    private fun playSound(soundUri: String?, volumePercent: Int) {
        stopPlayback()
        val uri = runCatching {
            if (!soundUri.isNullOrBlank()) Uri.parse(soundUri)
            else android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        }.getOrNull() ?: return

        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        val alarmMax = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM)?.coerceAtLeast(1) ?: 1
        val alarmCurrent = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM)?.coerceIn(0, alarmMax) ?: alarmMax
        val systemFactor = alarmCurrent.toFloat() / alarmMax.toFloat()
        val appFactor = volumePercent.coerceIn(0, 100) / 100f
        val finalVolume = (systemFactor * appFactor).coerceIn(0f, 1f)

        mediaPlayer = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ShiftAlarmRingingService, uri)
                isLooping = true
                setVolume(finalVolume, finalVolume)
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun startVibration() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        val localVibrator = vibrator ?: return
        if (!localVibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            localVibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0L, 400L, 250L, 700L), 0)
            ) else {
            @Suppress("DEPRECATION")
            localVibrator.vibrate(longArrayOf(0L, 400L, 250L, 700L), 0)
        }
    }

    private fun stopPlayback() {
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        }
        mediaPlayer = null
    }

    companion object {
        const val ACTION_START_ALARM = "com.vigilante.shiftsalaryplanner.action.START_ALARM"
        const val ACTION_DISMISS_ALARM = "com.vigilante.shiftsalaryplanner.action.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.vigilante.shiftsalaryplanner.action.SNOOZE_ALARM"
        const val RINGING_CHANNEL_ID = "shift_alarm_ringing_fs2"
        private const val RINGING_CHANNEL_NAME = "Звонящие будильники смен"

        fun ensureRingingChannel(context: android.content.Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = notificationManager.getNotificationChannel(RINGING_CHANNEL_ID)
            if (existing != null) return
            val channel = NotificationChannel(
                RINGING_CHANNEL_ID,
                RINGING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Полноэкранные будильники смен"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
