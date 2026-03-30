package com.vigilante.shiftsalaryplanner

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    onRescheduleNow: () -> Unit,
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
    val compactStatus = buildString {
        append(if (enabled) "включены" else "выключены")
        append(" • шаблонов: ")
        append(enabledTemplateCount)
        append(" • будильников: ")
        append(enabledAlarmCount)
    }

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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        SettingsSectionCard(
            title = "Общие настройки",
            subtitle = "Сохранение и перестройка выполняются автоматически"
        ) {
            CompactSwitchRow(
                title = "Включить будильники",
                checked = enabled,
                onCheckedChange = { enabled = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            CompactSwitchRow(
                title = "Автоперестройка",
                checked = autoReschedule,
                onCheckedChange = { autoReschedule = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            CompactIntField(
                label = "Горизонт планирования, дней",
                value = scheduleHorizonDaysText,
                onValueChange = { scheduleHorizonDaysText = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRescheduleNow,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Перестроить")
                }

                if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Уведомления")
                    }
                } else if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(
                        onClick = onOpenExactAlarmSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Точные")
                    }
                }
                else if (!canUseFullScreenIntent && Build.VERSION.SDK_INT >= 34) {
                    OutlinedButton(
                        onClick = onOpenFullScreenIntentSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Полный экран")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Статус: $compactStatus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Уведомления: ${if (notificationPermissionGranted) "ок" else "нет"} • точные: ${if (canScheduleExactAlarms) "ок" else "ограничены"} • полный экран: ${if (canUseFullScreenIntent) "ок" else "нет"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Изменения сохраняются автоматически",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Режим срабатывания: полноэкранный будильник со звуком",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionCard(
            title = "Шаблоны смен",
            subtitle = "Компактные карточки. Время смены редактируется в меню «Смены»."
        ) {
            if (shiftTemplates.isEmpty()) {
                Text("Пока нет рабочих смен для будильников. Добавь свои рабочие смены в меню «Смены», и они появятся здесь.")
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

        if (!lastRescheduleResult?.message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = lastRescheduleResult.message.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAlarmDialog && editingTemplate != null && editingAlarm != null) {
        ShiftTemplateAlarmEditDialog(
            template = editingTemplate,
            currentAlarm = editingAlarm,
            onDismiss = {
                showAlarmDialog = false
                editingTemplateCode = null
                editingAlarm = null
            },
            onSave = { updatedAlarm ->
                val template = editingTemplate
                val currentConfig = templateConfigs.firstOrNull { it.shiftCode == template.code }
                    ?: defaultShiftTemplateAlarmConfig(template)
                val updatedConfig = currentConfig.copy(
                    alarms = upsertShiftAlarmItem(currentConfig.alarms, updatedAlarm)
                )
                templateConfigs = upsertShiftTemplateAlarmConfig(templateConfigs, updatedConfig)
                showAlarmDialog = false
                editingTemplateCode = null
                editingAlarm = null
            }
        )
    }
}
