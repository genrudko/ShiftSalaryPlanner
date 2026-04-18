package com.vigilante.shiftsalaryplanner

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.util.UUID

@Composable
fun ShiftTemplateAlarmConfigCard(
    template: ShiftTemplateEntity,
    config: ShiftTemplateAlarmConfig,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onConfigChange: (ShiftTemplateAlarmConfig) -> Unit,
    onAddAlarm: () -> Unit,
    onEditAlarm: (ShiftAlarmConfig) -> Unit,
    onDeleteAlarm: (ShiftAlarmConfig) -> Unit
) {
    val activeAlarmCount = config.alarms.count { it.enabled }
    val chipColor = Color(parseColorHex(template.colorHex, 0xFF42A5F5.toInt()))
    val triggerHaptic = appHapticAction(onAction = {})

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = appBubbleBackgroundColor(defaultAlpha = 0.24f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = appHapticAction(onAction = onToggleExpanded))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    iconKey = template.iconKey,
                    fallbackCode = template.code,
                    badgeColor = chipColor,
                    size = 26.dp,
                    shape = RoundedCornerShape(9.dp),
                    selected = expanded,
                    unselectedBorderColor = appPanelBorderColor()
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shiftAlarmTemplateLabel(template),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${if (config.enabled) "Вкл" else "Выкл"} • Всего ${config.alarms.size} • Активных $activeAlarmCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = appListSecondaryTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        modifier = Modifier.scale(0.66f),
                        checked = config.enabled,
                        onCheckedChange = { checked ->
                            triggerHaptic()
                            val updated = if (checked && config.alarms.isEmpty()) {
                                defaultShiftTemplateAlarmConfig(template).copy(
                                    shiftCode = config.shiftCode,
                                    enabled = true,
                                    startHour = config.startHour,
                                    startMinute = config.startMinute,
                                    endHour = config.endHour,
                                    endMinute = config.endMinute
                                )
                            } else {
                                config.copy(enabled = checked)
                            }
                            onConfigChange(updated)
                        }
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = appBubbleBackgroundColor(defaultAlpha = 0.42f)
                    ) {
                        Text(
                            text = if (config.alarms.isEmpty()) {
                                "Будильников пока нет"
                            } else {
                                "Будильников: ${config.alarms.size}"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = appHapticAction(onAction = onAddAlarm),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Будильник")
                    }
                }

                if (config.alarms.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Добавь один или несколько будильников для этой смены.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    config.alarms
                        .sortedWith(compareBy<ShiftAlarmConfig> { it.triggerHour }.thenBy { it.triggerMinute })
                        .forEach { alarm ->
                            Spacer(modifier = Modifier.height(8.dp))
                            ShiftTemplateAlarmItemCard(
                                alarm = alarm,
                                onToggleEnabled = { checked ->
                                    onConfigChange(
                                        config.copy(
                                            alarms = config.alarms.map {
                                                if (it.id == alarm.id) it.copy(enabled = checked) else it
                                            }
                                        )
                                    )
                                },
                                onEdit = { onEditAlarm(alarm) },
                                onDelete = { onDeleteAlarm(alarm) }
                            )
                        }
                }
            }
        }
    }
}


@Composable
fun ShiftTemplateAlarmItemCard(
    alarm: ShiftAlarmConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val triggerHaptic = appHapticAction(onAction = {})
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = appBubbleBackgroundColor(defaultAlpha = 0.20f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlarmTimeBadge(
                text = formatClockHm(alarm.triggerHour, alarm.triggerMinute)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.title.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlarmMetaChip(text = "${alarm.volumePercent.coerceIn(0, 100)}%")
                    AlarmMetaChip(text = shiftAlarmSoundSummary(alarm))
                }
            }

            Switch(
                modifier = Modifier.scale(0.68f),
                checked = alarm.enabled,
                onCheckedChange = { checked ->
                    triggerHaptic()
                    onToggleEnabled(checked)
                }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = appHapticAction(onAction = onEdit),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Изменить будильник",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = appHapticAction(onAction = onDelete),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Удалить будильник",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmTimeBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AlarmMetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = appBubbleBackgroundColor(defaultAlpha = 0.52f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ShiftTemplateAlarmEditDialog(
    template: ShiftTemplateEntity,
    currentAlarm: ShiftAlarmConfig?,
    onDismiss: () -> Unit,
    onSave: (ShiftAlarmConfig) -> Unit
) {
    val context = LocalContext.current
    val defaultAlarmClock = remember(currentAlarm?.id, template.code) {
        if (currentAlarm != null) {
            currentAlarm.triggerHour to currentAlarm.triggerMinute
        } else {
            resolveAlarmClockFromShiftStart(
                startHour = 8,
                startMinute = 0,
                minutesBefore = if (template.nightHours > 0.0) 90 else 60
            )
        }
    }
    var titleText by remember(currentAlarm?.id) {
        mutableStateOf(
            currentAlarm?.title ?: defaultShiftAlarmTitle(
                shiftAlarmTemplateLabel(template),
                defaultAlarmClock.first,
                defaultAlarmClock.second
            )
        )
    }
    var triggerHour by remember(currentAlarm?.id) {
        mutableIntStateOf((currentAlarm?.triggerHour ?: defaultAlarmClock.first).coerceIn(0, 23))
    }
    var triggerMinute by remember(currentAlarm?.id) {
        mutableIntStateOf((currentAlarm?.triggerMinute ?: defaultAlarmClock.second).coerceIn(0, 59))
    }
    var volumePercent by remember(currentAlarm?.id) {
        mutableIntStateOf((currentAlarm?.volumePercent ?: 100).coerceIn(0, 100))
    }
    var soundUriText by remember(currentAlarm?.id) {
        mutableStateOf(currentAlarm?.soundUri ?: "")
    }
    var soundLabelText by remember(currentAlarm?.id) {
        mutableStateOf(currentAlarm?.soundLabel ?: "")
    }
    var enabled by remember(currentAlarm?.id) {
        mutableStateOf(currentAlarm?.enabled ?: true)
    }
    var showAdvancedSoundSettings by remember(currentAlarm?.id) {
        mutableStateOf(false)
    }
    var showAlarmTonePickerDialog by remember(currentAlarm?.id) {
        mutableStateOf(false)
    }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            soundUriText = uri.toString()
            soundLabelText = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringAfterLast(':')
                ?.ifBlank { "Свой файл" }
                ?: "Свой файл"
        }
    }

    val alarmToneOptions = remember(context) { loadAlarmToneOptions(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (currentAlarm == null) "Новый будильник" else "Редактировать будильник"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = shiftAlarmTemplateLabel(template),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Время срабатывания",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShiftAlarmWheelTimePicker(
                    hour = triggerHour,
                    minute = triggerMinute,
                    onHourChange = { triggerHour = it },
                    onMinuteChange = { triggerMinute = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выбрано: ${formatClockHm(triggerHour, triggerMinute)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                CompactSwitchRow(
                    title = "Активен",
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = appBubbleBackgroundColor(defaultAlpha = 0.28f),
                    border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Звук: ${if (soundUriText.isBlank()) "Системный" else soundLabelText.ifBlank { "Свой файл" }} • ${volumePercent.coerceIn(0, 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = appHapticAction { showAdvancedSoundSettings = !showAdvancedSoundSettings }
                        ) {
                            Text(if (showAdvancedSoundSettings) "Скрыть" else "Звук")
                        }
                    }
                }

                if (showAdvancedSoundSettings) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Громкость: ${volumePercent.coerceIn(0, 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = volumePercent.toFloat(),
                        onValueChange = { volumePercent = it.toInt().coerceIn(0, 100) },
                        valueRange = 0f..100f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = appHapticAction {
                                soundUriText = ""
                                soundLabelText = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Системная")
                        }
                        OutlinedButton(
                            onClick = appHapticAction {
                                showAlarmTonePickerDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Будильники")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = appHapticAction {
                                soundPickerLauncher.launch(arrayOf("audio/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Выбрать файл")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = appHapticAction(kind = AppHapticKind.SOFT) {
                    onSave(
                        ShiftAlarmConfig(
                            id = currentAlarm?.id ?: UUID.randomUUID().toString(),
                            title = titleText.trim(),
                            triggerHour = triggerHour.coerceIn(0, 23),
                            triggerMinute = triggerMinute.coerceIn(0, 59),
                            volumePercent = volumePercent.coerceIn(0, 100),
                            soundUri = soundUriText.ifBlank { null },
                            soundLabel = soundLabelText.trim(),
                            enabled = enabled
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = appHapticAction(onAction = onDismiss)) {
                Text("Отмена")
            }
        }
    )

    if (showAlarmTonePickerDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmTonePickerDialog = false },
            title = { Text("Мелодии будильника") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AlarmToneOptionRow(
                        label = "Системная по умолчанию",
                        selected = soundUriText.isBlank(),
                        onClick = appHapticAction {
                            soundUriText = ""
                            soundLabelText = ""
                            showAlarmTonePickerDialog = false
                        }
                    )

                    alarmToneOptions.forEach { option ->
                        AlarmToneOptionRow(
                            label = option.title,
                            selected = soundUriText == option.uri.toString(),
                            onClick = appHapticAction {
                                soundUriText = option.uri.toString()
                                soundLabelText = option.title
                                showAlarmTonePickerDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = appHapticAction { showAlarmTonePickerDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun AlarmToneOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private data class AlarmToneOption(
    val uri: Uri,
    val title: String
)

private fun loadAlarmToneOptions(context: android.content.Context): List<AlarmToneOption> {
    val manager = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_ALARM)
    }
    val cursor = runCatching { manager.cursor }.getOrNull() ?: return emptyList()
    val seen = LinkedHashSet<String>()
    return buildList {
        cursor.use { data ->
            while (data.moveToNext()) {
                val uri = runCatching { manager.getRingtoneUri(data.position) }.getOrNull() ?: continue
                val uriText = uri.toString()
                if (!seen.add(uriText)) continue
                val title = data.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    ?.trim()
                    ?.ifBlank { "Будильник" }
                    ?: "Будильник"
                add(AlarmToneOption(uri = uri, title = title))
            }
        }
    }
}
