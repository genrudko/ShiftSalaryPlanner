package com.vigilante.shiftsalaryplanner

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme

class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_ALARM
        applyWakeFlags()

        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY).orEmpty().ifBlank { "shift_alarm" }
        val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
        val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
        val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
        val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()
        val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)

        setContent {
            ShiftSalaryPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmRingingScreen(
                        title = title,
                        text = text,
                        soundInfo = when {
                            soundLabel.isNotBlank() -> "$soundLabel • громкость $volumePercent%"
                            !soundUri.isNullOrBlank() -> "Свой файл • громкость $volumePercent%"
                            else -> "Системная мелодия • громкость $volumePercent%"
                        },
                        onDismiss = {
                            startService(
                                Intent(this, ShiftAlarmRingingService::class.java).apply {
                                    action = ShiftAlarmRingingService.ACTION_DISMISS_ALARM
                                    putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
                                }
                            )
                            finish()
                        },
                        onSnooze = {
                            startService(
                                Intent(this, ShiftAlarmRingingService::class.java).apply {
                                    action = ShiftAlarmRingingService.ACTION_SNOOZE_ALARM
                                    putExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY, alarmKey)
                                    putExtra(ShiftAlarmScheduler.EXTRA_TITLE, title)
                                    putExtra(ShiftAlarmScheduler.EXTRA_TEXT, text)
                                    putExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, volumePercent)
                                    if (!soundUri.isNullOrBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI, soundUri)
                                    if (soundLabel.isNotBlank()) putExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL, soundLabel)
                                }
                            )
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyWakeFlags()
    }

    private fun applyWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
}

@Composable
private fun AlarmRingingScreen(
    title: String,
    text: String,
    soundInfo: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Будильник смены",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = soundInfo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Выключить")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
            Text("Отложить на 10 минут")
        }
    }
}
