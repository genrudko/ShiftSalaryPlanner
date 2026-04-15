package com.vigilante.shiftsalaryplanner

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
                .height(140.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = true
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setFormatter { formatter(it) }
                    setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                }
            },
            update = { picker ->
                picker.minValue = range.first
                picker.maxValue = range.last
                picker.displayedValues = null
                picker.setFormatter { formatter(it) }
                if (picker.value != value.coerceIn(range.first, range.last)) {
                    picker.value = value.coerceIn(range.first, range.last)
                }
            }
        )
    }
}

fun normalizeShiftAlarmSettings(settings: ShiftAlarmSettings): ShiftAlarmSettings {
    return settings.copy(
        scheduleHorizonDays = settings.scheduleHorizonDays.coerceIn(7, 365),
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
                                triggerHour = alarm.triggerHour.coerceIn(0, 23),
                                triggerMinute = alarm.triggerMinute.coerceIn(0, 59),
                                volumePercent = alarm.volumePercent.coerceIn(0, 100),
                                soundUri = alarm.soundUri?.takeIf { it.isNotBlank() },
                                soundLabel = alarm.soundLabel.trim()
                            )
                        }
                        .sortedWith(compareBy<ShiftAlarmConfig> { it.triggerHour }.thenBy { it.triggerMinute })
                )
            }
            .sortedBy { it.shiftCode }
    )
}
