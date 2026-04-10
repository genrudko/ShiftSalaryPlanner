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
import androidx.compose.runtime.LaunchedEffect
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
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import kotlinx.coroutines.delay

@Composable
fun ShiftAlarmsTab(
    state: ShiftAlarmsTabState,
    actions: ShiftAlarmsTabActions,
    modifier: Modifier = Modifier
) {
    ShiftAlarmsTab(
        settings = state.settings,
        shiftTemplates = state.shiftTemplates,
        lastRescheduleResult = state.lastRescheduleResult,
        canScheduleExactAlarms = state.canScheduleExactAlarms,
        notificationPermissionGranted = state.notificationPermissionGranted,
        onSave = actions.onSave,
        onRequestNotificationPermission = actions.onRequestNotificationPermission,
        onOpenExactAlarmSettings = actions.onOpenExactAlarmSettings,
        onOpenSystemClock = actions.onOpenSystemClock,
        onRescheduleNow = actions.onRescheduleNow,
        modifier = modifier
    )
}

@Composable
fun ShiftAlarmsTab(
    settings: ShiftAlarmSettings,
    shiftTemplates: List<ShiftTemplateEntity>,
    lastRescheduleResult: ShiftAlarmRescheduleResult?,
    canScheduleExactAlarms: Boolean,
    notificationPermissionGranted: Boolean,
    onSave: (ShiftAlarmSettings) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenSystemClock: () -> Unit,
    onRescheduleNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember(settings, shiftTemplates) {
        mutableStateOf(ShiftAlarmsTabUiState.from(settings, shiftTemplates))
    }
    val expandedTemplates = remember { mutableStateMapOf<String, Boolean>() }
    var lastAutoSavedSettings by remember(settings) { mutableStateOf(normalizeShiftAlarmSettings(settings)) }
    val dispatch: (ShiftAlarmsTabUiAction) -> Unit = { action ->
        uiState = reduceShiftAlarmsTabUiState(uiState, action)
    }

    val editingTemplate = remember(uiState.editingTemplateCode, shiftTemplates) {
        shiftTemplates.firstOrNull { it.code == uiState.editingTemplateCode }
    }

    val enabledTemplateCount = remember(uiState.templateConfigs) { uiState.templateConfigs.count { it.enabled } }
    val enabledAlarmCount = remember(uiState.templateConfigs) { uiState.templateConfigs.sumOf { config -> config.alarms.count { it.enabled } } }

    val normalizedSettings = remember(uiState, settings.scheduleHorizonDays) {
        normalizeShiftAlarmSettings(
            ShiftAlarmSettings(
                enabled = uiState.enabled,
                autoReschedule = uiState.autoReschedule,
                scheduleHorizonDays = parseInt(uiState.scheduleHorizonDaysText, settings.scheduleHorizonDays).coerceIn(7, 365),
                templateConfigs = uiState.templateConfigs
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Р‘СѓРґРёР»СЊРЅРёРєРё СЃРјРµРЅ",
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
                    title = "Р‘СѓРґ.",
                    checked = uiState.enabled,
                    onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetEnabled(it)) }
                )
                AlarmHeaderSwitchRow(
                    title = "РђРІС‚Рѕ",
                    checked = uiState.autoReschedule,
                    onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetAutoReschedule(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AlarmCompactSection(
            title = "РћР±С‰РёРµ РЅР°СЃС‚СЂРѕР№РєРё",
            subtitle = ""
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlarmQuickAction(
                    text = "РЎРёСЃС‚РµРјРЅС‹Рµ С‡Р°СЃС‹",
                    onClick = onOpenSystemClock,
                    modifier = Modifier.weight(1f)
                )
                AlarmQuickAction(
                    text = "РџРµСЂРµРїР»Р°РЅРёСЂРѕРІР°С‚СЊ",
                    onClick = onRescheduleNow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            AlarmInfoPill(text = "РЁР°Р±Р»РѕРЅРѕРІ: $enabledTemplateCount вЂў Р±СѓРґРёР»СЊРЅРёРєРѕРІ: $enabledAlarmCount")

            val needsPermissionActions =
                (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ||
                    (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

            if (needsPermissionActions) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "РўСЂРµР±СѓСЋС‚СЃСЏ СЂР°Р·СЂРµС€РµРЅРёСЏ",
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
                            text = "Р Р°Р·СЂРµС€РёС‚СЊ СѓРІРµРґРѕРјР»РµРЅРёСЏ",
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!canScheduleExactAlarms) {
                        AlarmQuickAction(
                            text = "Р Р°Р·СЂРµС€РёС‚СЊ С‚РѕС‡РЅС‹Рµ Р±СѓРґРёР»СЊРЅРёРєРё",
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
                    text = "Р“РѕСЂРёР·РѕРЅС‚ РїР»Р°РЅРёСЂРѕРІР°РЅРёСЏ",
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
                        text = "РґРЅ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionCard(
            title = "РЁР°Р±Р»РѕРЅС‹ СЃРјРµРЅ",
            subtitle = "РџРѕ РєР°Р¶РґРѕРјСѓ С€Р°Р±Р»РѕРЅСѓ РјРѕР¶РЅРѕ РІРєР»СЋС‡РёС‚СЊ СЃРІРѕРё Р±СѓРґРёР»СЊРЅРёРєРё"
        ) {
            if (shiftTemplates.isEmpty()) {
                Text(
                    text = "РџРѕРєР° РЅРµС‚ СЂР°Р±РѕС‡РёС… СЃРјРµРЅ РґР»СЏ Р±СѓРґРёР»СЊРЅРёРєРѕРІ. Р”РѕР±Р°РІСЊ РёС… РІ РјРµРЅСЋ В«РЎРјРµРЅС‹В».",
                    style = MaterialTheme.typography.bodyMedium
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AlarmCompactSection(
            title = "РЎРёСЃС‚РµРјРЅС‹Р№ СЃС‚Р°С‚СѓСЃ",
            subtitle = "РЎР»СѓР¶РµР±РЅР°СЏ РёРЅС„РѕСЂРјР°С†РёСЏ"
        ) {
            AlarmInfoPill(text = "РЈРІРµРґРѕРјР»РµРЅРёСЏ: ${if (notificationPermissionGranted) "РѕРє" else "РЅРµС‚"}")
            Spacer(modifier = Modifier.height(6.dp))
            AlarmInfoPill(text = "РўРѕС‡РЅС‹Рµ Р±СѓРґРёР»СЊРЅРёРєРё: ${if (canScheduleExactAlarms) "РѕРє" else "РѕРіСЂР°РЅРёС‡РµРЅС‹"}")
            if (!lastRescheduleResult?.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                AlarmInfoPill(text = lastRescheduleResult.message)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
