package com.vigilante.shiftsalaryplanner

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
    onDuplicateAlarm: (ShiftAlarmConfig) -> Unit,
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
                    val templateLabel = shiftAlarmTemplateLabel(template)
                    config.alarms
                        .sortedWith(compareBy<ShiftAlarmConfig> { it.triggerHour }.thenBy { it.triggerMinute })
                        .forEach { alarm ->
                            Spacer(modifier = Modifier.height(8.dp))
                            ShiftTemplateAlarmItemCard(
                                alarm = alarm,
                                templateLabel = templateLabel,
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
                                onDuplicate = { onDuplicateAlarm(alarm) },
                                onDelete = { onDeleteAlarm(alarm) }
                            )
                        }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftTemplateAlarmItemCard(
    alarm: ShiftAlarmConfig,
    templateLabel: String,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val triggerHaptic = appHapticAction(onAction = {})
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDuplicate()
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.32f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            AlarmSwipeBackground(dismissValue = dismissState.targetValue)
        }
    ) {
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
                        text = resolveShiftAlarmTitle(alarm, templateLabel),
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
}

@Composable
private fun AlarmSwipeBackground(
    dismissValue: SwipeToDismissBoxValue
) {
    val shape = RoundedCornerShape(12.dp)
    val isDuplicate = dismissValue == SwipeToDismissBoxValue.StartToEnd
    val isDelete = dismissValue == SwipeToDismissBoxValue.EndToStart
    val accentColor = when {
        isDuplicate -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        isDelete -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isDuplicate -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
        isDelete -> MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accentColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = appScaledSpacing(12.dp), vertical = appScaledSpacing(10.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = if (isDuplicate) MaterialTheme.colorScheme.primary else appListSecondaryTextColor()
            )
            Text(
                text = "Дублировать",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDuplicate) MaterialTheme.colorScheme.primary else appListSecondaryTextColor()
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Удалить",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDelete) MaterialTheme.colorScheme.error else appListSecondaryTextColor()
            )
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = if (isDelete) MaterialTheme.colorScheme.error else appListSecondaryTextColor()
            )
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
    val templateLabel = remember(template.code, template.title) { shiftAlarmTemplateLabel(template) }
    var titleText by remember(currentAlarm?.id) { mutableStateOf(currentAlarm?.title.orEmpty()) }
    var manualTitle by remember(currentAlarm?.id) { mutableStateOf(currentAlarm?.manualTitle ?: false) }
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
    val autoTitlePreview = remember(templateLabel, triggerHour, triggerMinute) {
        defaultShiftAlarmTitle(templateLabel, triggerHour, triggerMinute)
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

    val systemRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pickedUri = result.data?.let { data ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
        }
        if (pickedUri != null) {
            val title = runCatching {
                RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
            }.getOrNull().orEmpty()
            soundUriText = pickedUri.toString()
            soundLabelText = title.ifBlank { "Системная мелодия" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentAlarm == null) "Новый будильник" else "Редактировать будильник",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = templateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                CompactSwitchRow(
                    title = "Свое название",
                    checked = manualTitle,
                    onCheckedChange = { checked ->
                        manualTitle = checked
                        if (checked) {
                            if (titleText.isBlank()) {
                                titleText = autoTitlePreview
                            }
                        } else {
                            titleText = ""
                        }
                    }
                )
                if (manualTitle) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CompactTextField(
                        label = "Название",
                        value = titleText,
                        onValueChange = { titleText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmMetaChip(text = "Авто: $autoTitlePreview")
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                val existingUri = soundUriText
                                    .takeIf { it.isNotBlank() }
                                    ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                                    ?: resolveSystemAlarmRingtoneUri(context)
                                val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Выбор мелодии будильника")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, resolveSystemAlarmRingtoneUri(context))
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                }
                                systemRingtonePickerLauncher.launch(pickerIntent)
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
                            manualTitle = manualTitle,
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

}

private fun resolveSystemAlarmRingtoneUri(context: android.content.Context): Uri {
    return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
}
