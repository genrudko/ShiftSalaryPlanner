package com.vigilante.shiftsalaryplanner

import android.graphics.drawable.ColorDrawable
import android.widget.EditText
import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ShiftAlarmWheelTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftAlarmNumberWheel(
            label = "Часы",
            value = hour.coerceIn(0, 23),
            range = 0..23,
            formatter = { "%02d".format(it) },
            onValueChange = onHourChange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        ShiftAlarmNumberWheel(
            label = "Минуты",
            value = minute.coerceIn(0, 59),
            range = 0..59,
            formatter = { "%02d".format(it) },
            onValueChange = onMinuteChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ShiftAlarmNumberWheel(
    label: String,
    value: Int,
    range: IntRange,
    formatter: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val wheelTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val wheelBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f).toArgb()
    val wheelDividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f).toArgb()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = true
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setFormatter { formatter(it) }
                    setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                    applyAlarmWheelTheme(
                        textColor = wheelTextColor,
                        backgroundColor = wheelBackgroundColor,
                        dividerColor = wheelDividerColor
                    )
                }
            },
            update = { picker ->
                picker.minValue = range.first
                picker.maxValue = range.last
                picker.displayedValues = null
                picker.setFormatter { formatter(it) }
                picker.applyAlarmWheelTheme(
                    textColor = wheelTextColor,
                    backgroundColor = wheelBackgroundColor,
                    dividerColor = wheelDividerColor
                )
                if (picker.value != value.coerceIn(range.first, range.last)) {
                    picker.value = value.coerceIn(range.first, range.last)
                }
            }
        )
    }
}

private fun NumberPicker.applyAlarmWheelTheme(
    textColor: Int,
    backgroundColor: Int,
    dividerColor: Int
) {
    setBackgroundColor(backgroundColor)
    // Keep wheel text readable regardless of system/default NumberPicker theme.
    runCatching {
        val selectorWheelPaintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint").apply {
            isAccessible = true
        }
        (selectorWheelPaintField.get(this) as? android.graphics.Paint)?.color = textColor
    }
    runCatching {
        val setTextColorMethod = NumberPicker::class.java.getDeclaredMethod("setTextColor", Int::class.javaPrimitiveType).apply {
            isAccessible = true
        }
        setTextColorMethod.invoke(this, textColor)
    }
    runCatching {
        val inputTextField = NumberPicker::class.java.getDeclaredField("mInputText").apply {
            isAccessible = true
        }
        (inputTextField.get(this) as? EditText)?.apply {
            setTextColor(textColor)
            setHintTextColor(textColor)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }
    runCatching {
        val selectionDividerField = NumberPicker::class.java.getDeclaredField("mSelectionDivider").apply {
            isAccessible = true
        }
        selectionDividerField.set(this, ColorDrawable(dividerColor))
    }
    repeat(childCount) { index ->
        (getChildAt(index) as? EditText)?.apply {
            setTextColor(textColor)
            setHintTextColor(textColor)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }
    invalidate()
}

fun normalizeShiftAlarmSettings(settings: ShiftAlarmSettings): ShiftAlarmSettings {
    return settings.copy(
        scheduleHorizonDays = settings.scheduleHorizonDays.coerceIn(7, 365),
        behavior = settings.behavior.copy(
            vibrationDurationSeconds = settings.behavior.vibrationDurationSeconds.coerceIn(0, 300),
            customVibrationPattern = settings.behavior.customVibrationPattern.trim(),
            snoozeIntervalMinutes = settings.behavior.snoozeIntervalMinutes.coerceIn(1, 120),
            snoozeCountLimit = settings.behavior.snoozeCountLimit.coerceIn(0, 10),
            ringDurationSeconds = settings.behavior.ringDurationSeconds.coerceIn(10, 3_600),
            rampUpDurationSeconds = settings.behavior.rampUpDurationSeconds.coerceIn(0, 180)
        ),
        templateConfigs = settings.templateConfigs
            .map { config ->
                config.copy(
                    startHour = config.startHour.coerceIn(0, 23),
                    startMinute = config.startMinute.coerceIn(0, 59),
                    endHour = config.endHour.coerceIn(0, 23),
                    endMinute = config.endMinute.coerceIn(0, 59),
                    alarms = config.alarms
                        .map { alarm ->
                            alarm.copy(
                                title = alarm.title.trim(),
                                triggerHour = alarm.triggerHour.coerceIn(0, 23),
                                triggerMinute = alarm.triggerMinute.coerceIn(0, 59),
                                volumePercent = alarm.volumePercent.coerceIn(0, 100),
                                soundUri = alarm.soundUri?.takeIf { it.isNotBlank() },
                                soundLabel = alarm.soundLabel.trim(),
                                vibrationDurationSeconds = alarm.vibrationDurationSeconds.coerceIn(0, 300),
                                customVibrationPattern = alarm.customVibrationPattern.trim(),
                                snoozeIntervalMinutes = alarm.snoozeIntervalMinutes.coerceIn(1, 120),
                                snoozeCountLimit = alarm.snoozeCountLimit.coerceIn(0, 10),
                                ringDurationSeconds = alarm.ringDurationSeconds.coerceIn(10, 3_600),
                                rampUpDurationSeconds = alarm.rampUpDurationSeconds.coerceIn(0, 180)
                            )
                        }
                        .sortedWith(compareBy<ShiftAlarmConfig> { it.triggerHour }.thenBy { it.triggerMinute })
                )
            }
            .sortedBy { it.shiftCode }
    )
}
