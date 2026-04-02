package com.vigilante.shiftsalaryplanner

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import kotlinx.coroutines.delay

@Composable
fun ShiftAlarmsTab(
    settings: ShiftAlarmSettings,
    shiftTemplates: List<ShiftTemplateEntity>,
    lastRescheduleResult: ShiftAlarmRescheduleResult?,
    canScheduleExactAlarms: Boolean,
    notificationPermissionGranted: Boolean,
    canUseFullScreenIntent: Boolean,
    onSave: (ShiftAlarmSettings) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    @Suppress("unused") onRescheduleNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }
    var autoReschedule by remember(settings.autoReschedule) { mutableStateOf(settings.autoReschedule) }
    var scheduleHorizonDaysText by remember(settings.scheduleHorizonDays) {
        mutableStateOf(settings.scheduleHorizonDays.toString())
    }
    var templateConfigs by remember(settings, shiftTemplates) {
        mutableStateOf(mergeShiftAlarmConfigsWithTemplates(settings, shiftTemplates))
    }
    var editingTemplateCode by remember { mutableStateOf<String?>(null) }
    var editingAlarm by remember { mutableStateOf<ShiftAlarmConfig?>(null) }
    var showAlarmDialog by rememberSaveable { mutableStateOf(false) }
    val expandedTemplates = remember { mutableStateMapOf<String, Boolean>() }
    var lastAutoSavedSettings by remember(settings) { mutableStateOf(normalizeShiftAlarmSettings(settings)) }

    val editingTemplate = remember(editingTemplateCode, shiftTemplates) {
        shiftTemplates.firstOrNull { it.code == editingTemplateCode }
    }

    val enabledTemplateCount = remember(templateConfigs) { templateConfigs.count { it.enabled } }
    val enabledAlarmCount = remember(templateConfigs) { templateConfigs.sumOf { config -> config.alarms.count { it.enabled } } }

    val normalizedSettings = remember(enabled, autoReschedule, scheduleHorizonDaysText, templateConfigs, settings.scheduleHorizonDays) {
        normalizeShiftAlarmSettings(
            ShiftAlarmSettings(
                enabled = enabled,
                autoReschedule = autoReschedule,
                scheduleHorizonDays = parseInt(scheduleHorizonDaysText, settings.scheduleHorizonDays).coerceIn(7, 365),
                templateConfigs = templateConfigs
            )
        )
    }

    LaunchedEffect(normalizedSettings) {
        delay(800)
        if (normalizedSettings != lastAutoSavedSettings) {
            lastAutoSavedSettings = normalizedSettings
            onSave(normalizedSettings)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Будильники смен",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Изменения сохраняются автоматически",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionCard(
            title = "Общие настройки",
            subtitle = ""
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlarmToggleTileCompact(
                    title = "Будильники",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    modifier = Modifier.weight(1f)
                )
                AlarmToggleTileCompact(
                    title = "Автоперестройка",
                    checked = autoReschedule,
                    onCheckedChange = { autoReschedule = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            CompactIntField(
                label = "Планирование, дней",
                value = scheduleHorizonDaysText,
                onValueChange = { scheduleHorizonDaysText = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            AlarmInfoPill(
                text = "Шаблонов: $enabledTemplateCount • будильников: $enabledAlarmCount"
            )

            val needsPermissionActions =
                (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ||
                        (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ||
                        (!canUseFullScreenIntent && Build.VERSION.SDK_INT >= 34)

            if (needsPermissionActions) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Нужно выдать системные разрешения:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Разрешить уведомления")
                        }
                    }

                    if (!canScheduleExactAlarms) {
                        OutlinedButton(
                            onClick = onOpenExactAlarmSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Разрешить точные будильники")
                        }
                    }

                    if (!canUseFullScreenIntent && Build.VERSION.SDK_INT >= 34) {
                        OutlinedButton(
                            onClick = onOpenFullScreenIntentSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Разрешить полноэкранный режим")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionCard(
            title = "Шаблоны смен",
            subtitle = "По каждому шаблону можно включить свои будильники"
        ) {
            if (shiftTemplates.isEmpty()) {
                Text(
                    text = "Пока нет рабочих смен для будильников. Добавь их в меню «Смены».",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                shiftTemplates.sortedBy { it.sortOrder }.forEachIndexed { index, template ->
                    val config = templateConfigs.firstOrNull { it.shiftCode == template.code }
                        ?: defaultShiftTemplateAlarmConfig(template)
                    val expanded = expandedTemplates[template.code] ?: false

                    ShiftTemplateAlarmConfigCard(
                        template = template,
                        config = config,
                        expanded = expanded,
                        onToggleExpanded = {
                            expandedTemplates[template.code] = !expanded
                        },
                        onConfigChange = { updated ->
                            templateConfigs = upsertShiftTemplateAlarmConfig(templateConfigs, updated)
                        },
                        onAddAlarm = {
                            editingTemplateCode = template.code
                            val (defaultTriggerHour, defaultTriggerMinute) = resolveAlarmClockFromShiftStart(
                                startHour = config.startHour,
                                startMinute = config.startMinute,
                                minutesBefore = if (template.nightHours > 0.0) 90 else 60
                            )
                            editingAlarm = ShiftAlarmConfig(
                                title = defaultShiftAlarmTitle(
                                    shiftAlarmTemplateLabel(template),
                                    defaultTriggerHour,
                                    defaultTriggerMinute
                                ),
                                triggerHour = defaultTriggerHour,
                                triggerMinute = defaultTriggerMinute,
                                volumePercent = 100,
                                soundUri = null,
                                soundLabel = "",
                                enabled = true
                            )
                            showAlarmDialog = true
                            expandedTemplates[template.code] = true
                        },
                        onEditAlarm = { alarm ->
                            editingTemplateCode = template.code
                            editingAlarm = alarm
                            showAlarmDialog = true
                            expandedTemplates[template.code] = true
                        },
                        onDeleteAlarm = { alarm ->
                            val updated = config.copy(
                                alarms = config.alarms.filterNot { it.id == alarm.id }
                            )
                            templateConfigs = upsertShiftTemplateAlarmConfig(templateConfigs, updated)
                        }
                    )

                    if (index != shiftTemplates.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionCard(
            title = "Системный статус",
            subtitle = "Служебная информация"
        ) {
            AlarmInfoPill(text = "Уведомления: ${if (notificationPermissionGranted) "ок" else "нет"}")
            Spacer(modifier = Modifier.height(6.dp))
            AlarmInfoPill(text = "Точные будильники: ${if (canScheduleExactAlarms) "ок" else "ограничены"}")
            Spacer(modifier = Modifier.height(6.dp))
            AlarmInfoPill(text = "Полный экран: ${if (canUseFullScreenIntent) "ок" else "нет"}")
            if (!lastRescheduleResult?.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                AlarmInfoPill(text = lastRescheduleResult.message)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAlarmDialog && editingTemplate != null && editingAlarm != null) {
        ShiftTemplateAlarmEditDialog(
            template = editingTemplate,
            currentAlarm = editingAlarm,
            onDismiss = {
                editingTemplateCode = null
            },
            onSave = { updatedAlarm ->
                val currentConfig = templateConfigs.firstOrNull { it.shiftCode == editingTemplate.code }
                    ?: defaultShiftTemplateAlarmConfig(editingTemplate)
                val updatedConfig = currentConfig.copy(
                    alarms = upsertShiftAlarmItem(currentConfig.alarms, updatedAlarm)
                )
                templateConfigs = upsertShiftTemplateAlarmConfig(templateConfigs, updatedConfig)
                editingTemplateCode = null
            }
        )
    }
}

@Composable
private fun AlarmToggleTileCompact(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun AlarmInfoPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
