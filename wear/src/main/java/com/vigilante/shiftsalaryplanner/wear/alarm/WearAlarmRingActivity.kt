package com.vigilante.shiftsalaryplanner.wear.alarm

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.Wearable
import com.vigilante.shiftsalaryplanner.wear.sync.WearSyncContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WearAlarmRingActivity : ComponentActivity() {

    private var payload by mutableStateOf(WearAlarmPayload())
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val vibrationStopRunnable = Runnable { stopVibration() }
    private val autoStopRunnable = Runnable {
        stopLocalSignal()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWakeScreen()
        payload = payloadFromIntent(intent)

        lifecycleScope.launch {
            WearAlarmEvents.events.collectLatest { event ->
                if (event.alarmKey == payload.alarmKey) {
                    stopLocalSignal()
                    finish()
                }
            }
        }

        setContent {
            MaterialTheme {
                val currentPayload = payload
                LaunchedEffect(currentPayload.alarmKey, currentPayload.startedAt) {
                    startLocalSignal(currentPayload)
                }
                DisposableEffect(Unit) {
                    onDispose {
                        stopLocalSignal()
                        releaseWakeLock()
                    }
                }
                WearAlarmRingScreen(
                    payload = currentPayload,
                    onDismiss = {
                        sendCommandAndFinish(
                            path = WearSyncContract.PATH_ALARM_DISMISS_FROM_WEAR,
                            payload = currentPayload.toJson()
                        )
                    },
                    onSnooze = {
                        sendCommandAndFinish(
                            path = WearSyncContract.PATH_ALARM_SNOOZE_FROM_WEAR,
                            payload = currentPayload.toJson()
                        )
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        payload = payloadFromIntent(intent)
    }

    override fun onDestroy() {
        stopLocalSignal()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun configureWakeScreen() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        acquireWakeLock()
    }

    private fun startLocalSignal(payload: WearAlarmPayload) {
        stopLocalSignal()
        acquireWakeLock()
        startRingtone(payload)
        startVibration(payload)
        handler.postDelayed(
            autoStopRunnable,
            payload.ringDurationSeconds.coerceIn(10, 3_600) * 1_000L
        )
    }

    private fun stopLocalSignal() {
        handler.removeCallbacks(autoStopRunnable)
        handler.removeCallbacks(vibrationStopRunnable)
        runCatching { ringtone?.stop() }
        ringtone = null
        stopVibration()
    }

    private fun startRingtone(payload: WearAlarmPayload) {
        if (payload.wearSoundMode == "SILENT") return
        val ringtoneType = when (payload.wearSoundMode) {
            "RINGTONE" -> RingtoneManager.TYPE_RINGTONE
            "NOTIFICATION" -> RingtoneManager.TYPE_NOTIFICATION
            else -> RingtoneManager.TYPE_ALARM
        }
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, ringtoneType)
            ?: RingtoneManager.getDefaultUri(ringtoneType)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        ringtone = runCatching { RingtoneManager.getRingtone(this, uri) }.getOrNull()
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone?.isLooping = true
        ringtone?.play()
    }

    private fun startVibration(payload: WearAlarmPayload) {
        if (!payload.vibrationEnabled) return
        val target = getSystemService(Vibrator::class.java) ?: return
        val pattern = vibrationPattern(payload)
        if (pattern.isEmpty()) return
        vibrator = target
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            target.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(pattern, 0)
        }
        if (payload.vibrationDurationSeconds > 0) {
            handler.postDelayed(
                vibrationStopRunnable,
                payload.vibrationDurationSeconds.coerceIn(1, 300) * 1_000L
            )
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun vibrationPattern(payload: WearAlarmPayload): LongArray {
        return when (payload.vibrationType) {
            "SOFT" -> longArrayOf(0L, 220L, 320L)
            "STRONG" -> longArrayOf(0L, 760L, 220L)
            "HEARTBEAT" -> longArrayOf(0L, 160L, 90L, 160L, 460L)
            "CUSTOM" -> payload.customVibrationPattern
                .split(',', ';', ' ')
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { it in 30L..5_000L }
                .takeIf { it.size >= 2 }
                ?.toLongArray()
                ?: longArrayOf(0L, 520L, 360L)
            else -> longArrayOf(0L, 520L, 360L)
        }
    }

    private fun sendCommandAndFinish(path: String, payload: JSONObject) {
        stopLocalSignal()
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    WearAlarmCommandSender.send(this@WearAlarmRingActivity, path, payload)
                }
            }
            finish()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        releaseWakeLock()
        wakeLock = runCatching {
            @Suppress("DEPRECATION")
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "ShiftSalaryPlanner:WearAlarmWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12_000L)
            }
        }.getOrNull()
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    private fun payloadFromIntent(intent: Intent): WearAlarmPayload {
        val raw = intent.getStringExtra(EXTRA_ALARM_JSON).orEmpty()
        return runCatching { WearAlarmPayload.fromJson(JSONObject(raw)) }
            .getOrDefault(WearAlarmPayload())
    }

    companion object {
        const val EXTRA_ALARM_JSON = "extra_alarm_json"
    }
}

@Composable
private fun WearAlarmRingScreen(
    payload: WearAlarmPayload,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "wear_alarm")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .clip(CircleShape)
                    .background(AlarmColors.Accent.copy(alpha = 0.18f))
                    .border(BorderStroke(1.dp, AlarmColors.Accent.copy(alpha = 0.55f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = AlarmColors.Accent,
                    modifier = Modifier.size(27.dp)
                )
            }

            Text(
                text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                color = AlarmColors.Text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = payload.title,
                color = AlarmColors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = payload.text,
                color = AlarmColors.Muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmColors.Accent,
                    contentColor = Color.Black
                )
            ) {
                Text("Выключить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (payload.canSnooze) {
                Button(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AlarmColors.Panel,
                        contentColor = AlarmColors.Text
                    )
                ) {
                    Text("Отложить ${payload.snoozeIntervalMinutes} мин", fontSize = 11.sp)
                }
            }
        }
    }
}

private object AlarmColors {
    val Background = Color(0xFF050708)
    val Panel = Color(0xFF1B2328)
    val Text = Color(0xFFF2F7F4)
    val Muted = Color(0xFFA8B4AE)
    val Accent = Color(0xFF7AE582)
}
