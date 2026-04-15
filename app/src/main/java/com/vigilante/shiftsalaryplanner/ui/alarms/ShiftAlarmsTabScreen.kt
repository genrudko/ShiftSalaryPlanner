package com.vigilante.shiftsalaryplanner

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShiftAlarmsTab(
    state: ShiftAlarmsTabState,
    actions: ShiftAlarmsTabActions,
    modifier: Modifier = Modifier
) {
    val settings = state.settings
    val shiftTemplates = state.shiftTemplates
    val lastRescheduleResult = state.lastRescheduleResult
    val canScheduleExactAlarms = state.canScheduleExactAlarms
    val notificationPermissionGranted = state.notificationPermissionGranted
    val onSave = actions.onSave
    val onRequestNotificationPermission = actions.onRequestNotificationPermission
    val onOpenExactAlarmSettings = actions.onOpenExactAlarmSettings
    val onOpenSystemClock = actions.onOpenSystemClock
    val onRescheduleNow = actions.onRescheduleNow

    var uiState by remember(settings, shiftTemplates) {
        mutableStateOf(ShiftAlarmsTabUiState.from(settings, shiftTemplates))
    }
    val expandedTemplates = remember { mutableStateMapOf<String, Boolean>() }
    val dispatch: (ShiftAlarmsTabUiAction) -> Unit = { action ->
        uiState = reduceShiftAlarmsTabUiState(uiState, action)
    }

    val editingTemplate = remember(uiState.editingTemplateCode, shiftTemplates) {
        shiftTemplates.firstOrNull { it.code == uiState.editingTemplateCode }
    }

    val enabledTemplateCount = remember(uiState.templateConfigs) { uiState.templateConfigs.count { it.enabled } }
    val enabledAlarmCount = remember(uiState.templateConfigs) { uiState.templateConfigs.sumOf { config -> config.alarms.count { it.enabled } } }

    val normalizedSettings = remember(uiState, settings.scheduleHorizonDays) {
        buildNormalizedShiftAlarmSettings(
            uiState = uiState,
            fallbackHorizonDays = settings.scheduleHorizonDays
        )
    }

    ShiftAlarmsAutoSaveEffect(
        settingsToSave = normalizedSettings,
        initialSettings = settings,
        onSave = onSave
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Будильники смен",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                AlarmHeaderSwitchRow(
                    title = "Буд.",
                    checked = uiState.enabled,
                    onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetEnabled(it)) }
                )
                AlarmHeaderSwitchRow(
                    title = "Авто",
                    checked = uiState.autoReschedule,
                    onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetAutoReschedule(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))

        AlarmCompactSection(
            title = "Общие настройки",
            subtitle = ""
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlarmQuickAction(
                    text = "Системные часы",
                    onClick = onOpenSystemClock,
                    modifier = Modifier.weight(1f)
                )
                AlarmQuickAction(
                    text = "Перепланировать",
                    onClick = onRescheduleNow,
                    hapticKind = AppHapticKind.SOFT,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            AlarmInfoPill(text = "Шаблонов: $enabledTemplateCount • будильников: $enabledAlarmCount")

            val needsPermissionActions =
                (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ||
                    (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

            if (needsPermissionActions) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Требуются разрешения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        AlarmQuickAction(
                            text = "Разрешить уведомления",
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!canScheduleExactAlarms) {
                        AlarmQuickAction(
                            text = "Разрешить точные будильники",
                            onClick = onOpenExactAlarmSettings,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Горизонт планирования",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlarmDaysMiniField(
                        value = uiState.scheduleHorizonDaysText,
                        onValueChange = { dispatch(ShiftAlarmsTabUiAction.SetScheduleHorizonDaysText(it)) },
                        width = 62.dp
                    )
                    Text(
                        text = "дн",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        SettingsSectionCard(
            title = "Шаблоны смен",
            subtitle = "По каждому шаблону можно включить свои будильники"
        ) {
            if (shiftTemplates.isEmpty()) {
                AppEmptyCard(
                    title = "Пока пусто",
                    message = "Пока нет рабочих смен для будильников. Добавь их в меню «Смены»."
                )
            } else {
                shiftTemplates.sortedBy { it.sortOrder }.forEachIndexed { index, template ->
                    val config = uiState.templateConfigs.firstOrNull { it.shiftCode == template.code }
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
                            dispatch(
                                ShiftAlarmsTabUiAction.SetTemplateConfigs(
                                    upsertShiftTemplateAlarmConfig(uiState.templateConfigs, updated)
                                )
                            )
                        },
                        onAddAlarm = {
                            val (defaultTriggerHour, defaultTriggerMinute) = resolveAlarmClockFromShiftStart(
                                startHour = config.startHour,
                                startMinute = config.startMinute,
                                minutesBefore = if (template.nightHours > 0.0) 90 else 60
                            )
                            val nextAlarm = ShiftAlarmConfig(
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
                            dispatch(
                                ShiftAlarmsTabUiAction.StartEditing(
                                    templateCode = template.code,
                                    alarm = nextAlarm
                                )
                            )
                            expandedTemplates[template.code] = true
                        },
                        onEditAlarm = { alarm ->
                            dispatch(
                                ShiftAlarmsTabUiAction.StartEditing(
                                    templateCode = template.code,
                                    alarm = alarm
                                )
                            )
                            expandedTemplates[template.code] = true
                        },
                        onDeleteAlarm = { alarm ->
                            val updated = config.copy(
                                alarms = config.alarms.filterNot { it.id == alarm.id }
                            )
                            dispatch(
                                ShiftAlarmsTabUiAction.SetTemplateConfigs(
                                    upsertShiftTemplateAlarmConfig(uiState.templateConfigs, updated)
                                )
                            )
                        }
                    )

                    if (index != shiftTemplates.lastIndex) {
                        Spacer(modifier = Modifier.height(appBlockSpacing()))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        AlarmCompactSection(
            title = "Системный статус",
            subtitle = "Служебная информация"
        ) {
            AlarmInfoPill(text = "Уведомления: ${if (notificationPermissionGranted) "ок" else "нет"}")
            Spacer(modifier = Modifier.height(6.dp))
            AlarmInfoPill(text = "Точные будильники: ${if (canScheduleExactAlarms) "ок" else "ограничены"}")
            if (!lastRescheduleResult?.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                AppFeedbackCard(
                    title = "Перепланировка",
                    message = lastRescheduleResult.message,
                    state = inferRescheduleFeedbackState(lastRescheduleResult)
                )
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(24.dp)))
    }

    if (uiState.showAlarmDialog && editingTemplate != null && uiState.editingAlarm != null) {
        ShiftTemplateAlarmEditDialog(
            template = editingTemplate,
            currentAlarm = uiState.editingAlarm,
            onDismiss = {
                dispatch(ShiftAlarmsTabUiAction.StopEditing)
            },
            onSave = { updatedAlarm ->
                val currentConfig = uiState.templateConfigs.firstOrNull { it.shiftCode == editingTemplate.code }
                    ?: defaultShiftTemplateAlarmConfig(editingTemplate)
                val updatedConfig = currentConfig.copy(
                    alarms = upsertShiftAlarmItem(currentConfig.alarms, updatedAlarm)
                )
                dispatch(
                    ShiftAlarmsTabUiAction.SetTemplateConfigs(
                        upsertShiftTemplateAlarmConfig(uiState.templateConfigs, updatedConfig)
                    )
                )
                dispatch(ShiftAlarmsTabUiAction.StopEditing)
            }
        )
    }
}

private fun inferRescheduleFeedbackState(result: ShiftAlarmRescheduleResult): AppFeedbackState {
    val normalizedMessage = result.message.lowercase()
    return when {
        normalizedMessage.contains("ошиб") || normalizedMessage.contains("не удалось") -> AppFeedbackState.ERROR
        result.scheduledCount > 0 || result.cancelledCount > 0 -> AppFeedbackState.SUCCESS
        result.skippedNoConfigCount > 0 || result.skippedNoTemplateCount > 0 -> AppFeedbackState.EMPTY
        else -> AppFeedbackState.INFO
    }
}

