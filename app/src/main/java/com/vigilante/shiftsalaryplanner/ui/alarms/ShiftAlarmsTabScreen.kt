package com.vigilante.shiftsalaryplanner

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
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
    val fullScreenIntentPermissionGranted = state.fullScreenIntentPermissionGranted
    val onSave = actions.onSave
    val onRequestNotificationPermission = actions.onRequestNotificationPermission
    val onOpenExactAlarmSettings = actions.onOpenExactAlarmSettings
    val onOpenFullScreenIntentSettings = actions.onOpenFullScreenIntentSettings
    val onOpenSystemClock = actions.onOpenSystemClock
    val onRescheduleNow = actions.onRescheduleNow

    var uiState by remember(settings, shiftTemplates) {
        mutableStateOf(ShiftAlarmsTabUiState.from(settings, shiftTemplates))
    }
    var showRingAppearanceDialog by remember { mutableStateOf(false) }
    var showAlarmBehaviorDialog by remember { mutableStateOf(false) }
    val expandedTemplates = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current
    val dispatch: (ShiftAlarmsTabUiAction) -> Unit = { action ->
        uiState = reduceShiftAlarmsTabUiState(uiState, action)
    }
    val behaviorSoundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val label = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringAfterLast(':')
                ?.ifBlank { "Свой файл" }
                ?: "Свой файл"
            dispatch(
                ShiftAlarmsTabUiAction.SetBehaviorDefaultSound(
                    uri = uri.toString(),
                    label = label
                )
            )
        }
    }
    val behaviorSystemRingtonePickerLauncher = rememberLauncherForActivityResult(
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
            dispatch(
                ShiftAlarmsTabUiAction.SetBehaviorDefaultSound(
                    uri = pickedUri.toString(),
                    label = title.ifBlank { "Системная мелодия" }
                )
            )
        }
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        item("alarms-header") {
            Spacer(modifier = Modifier.height(appScreenPadding()))
            Text(
                text = "Будильники смен",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
            Text(
                text = "Гибкая настройка будильников по шаблонам смен",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            Spacer(modifier = Modifier.height(appSectionSpacing()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                AlarmModeToggleCard(
                    title = "Будильники",
                    checked = uiState.enabled,
                    onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetEnabled(it)) },
                    modifier = Modifier.weight(1f)
                )
                AlarmModeToggleCard(
                    title = "Автоперепланировка",
                    checked = uiState.autoReschedule,
                    onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetAutoReschedule(it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(appSectionSpacing()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                TemplateStatPill(
                    label = "Шаблонов",
                    value = shiftTemplates.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                TemplateStatPill(
                    label = "Активно",
                    value = enabledTemplateCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                TemplateStatPill(
                    label = "Будильников",
                    value = enabledAlarmCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(appSectionSpacing()))
        }

        stickyHeader("alarms-section-general") {
            AlarmsStickyHeader("Общие настройки")
        }
        item("alarms-general-card") {
            AlarmCompactSection(
                title = "Быстрые действия",
                subtitle = "Тест звонка, перепланировка и параметры"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlarmQuickAction(
                        text = "Тест звонка",
                        icon = Icons.Rounded.Alarm,
                        compact = true,
                        onClick = onOpenSystemClock,
                        modifier = Modifier.weight(1f)
                    )
                    AlarmQuickAction(
                        text = "Перепланировать",
                        icon = Icons.Rounded.Autorenew,
                        emphasized = true,
                        compact = true,
                        onClick = onRescheduleNow,
                        hapticKind = AppHapticKind.SOFT,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                AlarmInfoPill(text = "Шаблонов: $enabledTemplateCount • будильников: $enabledAlarmCount")

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

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                AlarmQuickAction(
                    text = "Экран звонка",
                    subtitle = buildString {
                        append(
                            when (uiState.ringVisualStyle) {
                                ShiftAlarmRingVisualStyle.MODERN -> "Современный"
                                ShiftAlarmRingVisualStyle.MINIMAL -> "Минимал"
                            }
                        )
                        append(" • ")
                        append(
                            when (uiState.ringAnimationMode) {
                                ShiftAlarmRingAnimationMode.OFF -> "анимация выкл"
                                ShiftAlarmRingAnimationMode.SOFT -> "анимация мягко"
                                ShiftAlarmRingAnimationMode.VIVID -> "анимация ярко"
                            }
                        )
                        append(" • ")
                        append(
                            when (uiState.ringActionStyle) {
                                ShiftAlarmRingActionStyle.BUTTONS -> "кнопки"
                                ShiftAlarmRingActionStyle.SLIDER -> "слайдер"
                                ShiftAlarmRingActionStyle.CHIPS -> "чипы"
                            }
                        )
                    },
                    icon = Icons.Rounded.Tune,
                    compact = true,
                    onClick = { showRingAppearanceDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                AlarmQuickAction(
                    text = "Поведение звонка",
                    subtitle = shiftAlarmBehaviorSummary(
                        ShiftAlarmBehaviorSettings(
                            vibrationEnabled = uiState.behaviorVibrationEnabled,
                            vibrationType = uiState.behaviorVibrationType,
                            vibrationDurationSeconds = parseInt(uiState.behaviorVibrationDurationSecondsText, 25).coerceIn(0, 300),
                            customVibrationPattern = uiState.behaviorCustomVibrationPattern,
                            snoozeIntervalMinutes = parseInt(uiState.behaviorSnoozeIntervalMinutesText, 10).coerceIn(1, 120),
                            snoozeCountLimit = parseInt(uiState.behaviorSnoozeCountLimitText, 3).coerceIn(0, 10),
                            ringDurationSeconds = (
                                parseInt(uiState.behaviorRingDurationMinutesText, 3).coerceIn(1, 60) * 60
                                ).coerceIn(10, 3_600),
                            rampUpDurationSeconds = parseInt(uiState.behaviorRampUpDurationSecondsText, 0).coerceIn(0, 180),
                            defaultSoundUri = uiState.behaviorDefaultSoundUri,
                            defaultSoundLabel = uiState.behaviorDefaultSoundLabel
                        )
                    ),
                    icon = Icons.Rounded.Tune,
                    compact = true,
                    onClick = { showAlarmBehaviorDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        stickyHeader("alarms-section-templates") {
            AlarmsStickyHeader("Шаблоны смен")
        }
        item("alarms-templates-card") {
            if (shiftTemplates.isEmpty()) {
                AppEmptyCard(
                    title = "Пока пусто",
                    message = "Пока нет рабочих смен для будильников. Добавь их в меню «Смены»."
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(appCornerRadius(20.dp)),
                    color = appBubbleBackgroundColor(defaultAlpha = 0.28f),
                    border = BorderStroke(1.dp, appPanelBorderColor())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(appScaledSpacing(10.dp))
                    ) {
                        Text(
                            text = "По каждому шаблону можно включить отдельные будильники",
                            style = MaterialTheme.typography.bodySmall,
                            color = appListSecondaryTextColor()
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = appScaledSpacing(2.dp)),
                        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
                    ) {
                        shiftTemplates.sortedBy { it.sortOrder }.forEach { template ->
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
                                        title = "",
                                        manualTitle = false,
                                        triggerHour = defaultTriggerHour,
                                        triggerMinute = defaultTriggerMinute,
                                        volumePercent = 100,
                                        soundUri = uiState.behaviorDefaultSoundUri,
                                        soundLabel = uiState.behaviorDefaultSoundLabel,
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
                                onDuplicateAlarm = { alarm ->
                                    val duplicated = alarm.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        enabled = true
                                    )
                                    val updated = config.copy(
                                        alarms = upsertShiftAlarmItem(config.alarms, duplicated)
                                    )
                                    dispatch(
                                        ShiftAlarmsTabUiAction.SetTemplateConfigs(
                                            upsertShiftTemplateAlarmConfig(uiState.templateConfigs, updated)
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
                        }
                    }
                }
            }
        }

        stickyHeader("alarms-section-system") {
            AlarmsStickyHeader("Системный статус")
        }
        item("alarms-system-card") {
            AlarmCompactSection(
                title = "Служебная информация",
                subtitle = "Права доступа и результаты последней перепланировки"
            ) {
                val needsPermissionActions =
                    (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ||
                        (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ||
                        (!fullScreenIntentPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

                AlarmInfoPill(text = "Уведомления: ${if (notificationPermissionGranted) "ок" else "нет"}")
                Spacer(modifier = Modifier.height(6.dp))
                AlarmInfoPill(text = "Точные будильники: ${if (canScheduleExactAlarms) "ок" else "ограничены"}")
                Spacer(modifier = Modifier.height(6.dp))
                AlarmInfoPill(
                    text = "Полноэкранный режим: ${
                        if (fullScreenIntentPermissionGranted) "ок" else "нет"
                    }"
                )

                if (needsPermissionActions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Требуются разрешения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            AlarmQuickAction(
                                text = "Разрешить уведомления",
                                onClick = onRequestNotificationPermission,
                                compact = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (!canScheduleExactAlarms) {
                            AlarmQuickAction(
                                text = "Разрешить точные будильники",
                                onClick = onOpenExactAlarmSettings,
                                compact = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (!fullScreenIntentPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            AlarmQuickAction(
                                text = "Разрешить полноэкранный режим",
                                onClick = onOpenFullScreenIntentSettings,
                                compact = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (!lastRescheduleResult?.message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AppFeedbackCard(
                        title = "Перепланировка",
                        message = lastRescheduleResult.message,
                        state = inferRescheduleFeedbackState(lastRescheduleResult)
                    )
                }
            }
        }

        item("alarms-bottom-space") {
            Spacer(modifier = Modifier.height(appScaledSpacing(112.dp)))
        }
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

    if (showRingAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showRingAppearanceDialog = false },
            title = { Text("Экран звонка") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Вид и анимации",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Показывать текущее время",
                        checked = uiState.ringShowCurrentClock,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingShowCurrentClock(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Показывать дату",
                        checked = uiState.ringShowDate,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingShowDate(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Пульсация акцента",
                        checked = uiState.ringPulseAccent,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingPulseAccent(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Анимированный фон",
                        checked = uiState.ringAnimatedGradient,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingAnimatedGradient(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AlarmChoiceRow(
                        title = "Анимация фона",
                        options = listOf(
                            "Aurora" to ShiftAlarmRingAnimationStyle.AURORA,
                            "Orbit" to ShiftAlarmRingAnimationStyle.ORBIT,
                            "Wave" to ShiftAlarmRingAnimationStyle.WAVE
                        ),
                        selected = uiState.ringAnimationStyle,
                        onSelect = { dispatch(ShiftAlarmsTabUiAction.SetRingAnimationStyle(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AlarmChoiceRow(
                        title = "Стиль",
                        options = listOf(
                            "Современный" to ShiftAlarmRingVisualStyle.MODERN,
                            "Минимал" to ShiftAlarmRingVisualStyle.MINIMAL
                        ),
                        selected = uiState.ringVisualStyle,
                        onSelect = { dispatch(ShiftAlarmsTabUiAction.SetRingVisualStyle(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AlarmChoiceRow(
                        title = "Анимация",
                        options = listOf(
                            "Выкл" to ShiftAlarmRingAnimationMode.OFF,
                            "Мягко" to ShiftAlarmRingAnimationMode.SOFT,
                            "Ярко" to ShiftAlarmRingAnimationMode.VIVID
                        ),
                        selected = uiState.ringAnimationMode,
                        onSelect = { dispatch(ShiftAlarmsTabUiAction.SetRingAnimationMode(it)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Кнопки и расположение",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmChoiceRow(
                        title = "Вид действий",
                        options = listOf(
                            "Кнопки" to ShiftAlarmRingActionStyle.BUTTONS,
                            "Слайдер" to ShiftAlarmRingActionStyle.SLIDER,
                            "Чипы" to ShiftAlarmRingActionStyle.CHIPS
                        ),
                        selected = uiState.ringActionStyle,
                        onSelect = { dispatch(ShiftAlarmsTabUiAction.SetRingActionStyle(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AlarmChoiceRow(
                        title = "Расположение кнопок",
                        options = listOf(
                            "Горизонтально" to ShiftAlarmRingButtonsLayout.HORIZONTAL,
                            "Вертикально" to ShiftAlarmRingButtonsLayout.VERTICAL
                        ),
                        selected = uiState.ringButtonsLayout,
                        onSelect = { dispatch(ShiftAlarmsTabUiAction.SetRingButtonsLayout(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AlarmChoiceRow(
                        title = "Позиция часов",
                        options = listOf(
                            "Сверху" to ShiftAlarmRingClockAlignment.TOP,
                            "По центру" to ShiftAlarmRingClockAlignment.CENTER
                        ),
                        selected = uiState.ringClockAlignment,
                        onSelect = { dispatch(ShiftAlarmsTabUiAction.SetRingClockAlignment(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AlarmFloatSliderRow(
                        title = "Размер часов",
                        value = uiState.ringClockScale,
                        valueLabel = "${(uiState.ringClockScale * 100f).toInt()}%",
                        valueRange = 0.8f..1.4f,
                        onValueChange = { dispatch(ShiftAlarmsTabUiAction.SetRingClockScale(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmFloatSliderRow(
                        title = "Размер текста",
                        value = uiState.ringTextScale,
                        valueLabel = "${(uiState.ringTextScale * 100f).toInt()}%",
                        valueRange = 0.85f..1.35f,
                        onValueChange = { dispatch(ShiftAlarmsTabUiAction.SetRingTextScale(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Моноширинные цифры часов",
                        checked = uiState.ringUseMonospaceClock,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingUseMonospaceClock(it)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Доп. информация",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Показывать инфо-блок",
                        checked = uiState.ringShowMetaInfo,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingShowMetaInfo(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Показывать название мелодии",
                        checked = uiState.ringShowSoundLabel,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingShowSoundLabel(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Показывать громкость",
                        checked = uiState.ringShowVolumeInfo,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingShowVolumeInfo(it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmSettingToggleItem(
                        title = "Показывать часовой пояс",
                        checked = uiState.ringShowTimezoneInfo,
                        onCheckedChange = { dispatch(ShiftAlarmsTabUiAction.SetRingShowTimezoneInfo(it)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRingAppearanceDialog = false }) {
                    Text("Готово")
                }
            }
        )
    }

    if (showAlarmBehaviorDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmBehaviorDialog = false },
            title = { Text("Поведение звонка") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    CompactSwitchRow(
                        title = "Вибрация",
                        checked = uiState.behaviorVibrationEnabled,
                        onCheckedChange = {
                            dispatch(ShiftAlarmsTabUiAction.SetBehaviorVibrationEnabled(it))
                        }
                    )
                    if (uiState.behaviorVibrationEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        AlarmChoiceGrid(
                            title = "Тип вибрации",
                            options = listOf(
                                "Система" to ShiftAlarmVibrationType.SYSTEM,
                                "Мягкая" to ShiftAlarmVibrationType.SOFT,
                                "Сильная" to ShiftAlarmVibrationType.STRONG,
                                "Ритм" to ShiftAlarmVibrationType.HEARTBEAT,
                                "Своя" to ShiftAlarmVibrationType.CUSTOM
                            ),
                            selected = uiState.behaviorVibrationType,
                            onSelect = { dispatch(ShiftAlarmsTabUiAction.SetBehaviorVibrationType(it)) },
                            columns = 3
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CompactIntField(
                                label = "Вибро (сек, 0=∞)",
                                value = uiState.behaviorVibrationDurationSecondsText,
                                onValueChange = {
                                    dispatch(ShiftAlarmsTabUiAction.SetBehaviorVibrationDurationSecondsText(it))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CompactIntField(
                                label = "Рост (сек)",
                                value = uiState.behaviorRampUpDurationSecondsText,
                                onValueChange = {
                                    dispatch(ShiftAlarmsTabUiAction.SetBehaviorRampUpDurationSecondsText(it))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (uiState.behaviorVibrationType == ShiftAlarmVibrationType.CUSTOM) {
                            Spacer(modifier = Modifier.height(6.dp))
                            CompactTextField(
                                label = "Паттерн (мс: 200,120,400,120)",
                                value = uiState.behaviorCustomVibrationPattern,
                                onValueChange = {
                                    dispatch(ShiftAlarmsTabUiAction.SetBehaviorCustomVibrationPattern(it))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        CompactIntField(
                            label = "Рост (сек)",
                            value = uiState.behaviorRampUpDurationSecondsText,
                            onValueChange = {
                                dispatch(ShiftAlarmsTabUiAction.SetBehaviorRampUpDurationSecondsText(it))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactIntField(
                            label = "Snooze (мин)",
                            value = uiState.behaviorSnoozeIntervalMinutesText,
                            onValueChange = {
                                dispatch(ShiftAlarmsTabUiAction.SetBehaviorSnoozeIntervalMinutesText(it))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactIntField(
                            label = "Повторы",
                            value = uiState.behaviorSnoozeCountLimitText,
                            onValueChange = {
                                dispatch(ShiftAlarmsTabUiAction.SetBehaviorSnoozeCountLimitText(it))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    CompactIntField(
                        label = "Звонок (мин)",
                        value = uiState.behaviorRingDurationMinutesText,
                        onValueChange = {
                            dispatch(ShiftAlarmsTabUiAction.SetBehaviorRingDurationMinutesText(it))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val defaultSoundText = when {
                        uiState.behaviorDefaultSoundLabel.isNotBlank() -> uiState.behaviorDefaultSoundLabel
                        !uiState.behaviorDefaultSoundUri.isNullOrBlank() -> "Свой файл"
                        else -> "Системная мелодия"
                    }
                    Text(
                        text = "Мелодия по умолчанию",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmInfoPill(text = defaultSoundText)
                    Spacer(modifier = Modifier.height(6.dp))
                    AlarmActionGrid(
                        items = listOf(
                            "Система" to {
                                dispatch(ShiftAlarmsTabUiAction.ClearBehaviorDefaultSound)
                            },
                            "Будильники" to {
                                val existingUri = uiState.behaviorDefaultSoundUri
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                                    ?: resolveSystemAlarmRingtoneUriForBehavior(context)
                                val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Выбор мелодии будильника")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, resolveSystemAlarmRingtoneUriForBehavior(context))
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                }
                                behaviorSystemRingtonePickerLauncher.launch(pickerIntent)
                            },
                            "Выбрать файл" to {
                                behaviorSoundPickerLauncher.launch(arrayOf("audio/*"))
                            }
                        ),
                        columns = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlarmBehaviorDialog = false }) {
                    Text("Готово")
                }
            }
        )
    }
}

@Composable
private fun AlarmModeToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val triggerHaptic = appHapticAction(onAction = {})
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.26f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    triggerHaptic()
                    onCheckedChange(!checked)
                })
                .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(8.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = { next ->
                    triggerHaptic()
                    onCheckedChange(next)
                },
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Composable
private fun AlarmsStickyHeader(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = appScaledSpacing(6.dp), bottom = appScaledSpacing(2.dp))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = appListSecondaryTextColor()
            )
        }
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

private fun resolveSystemAlarmRingtoneUriForBehavior(context: android.content.Context): Uri {
    return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
}

@Composable
private fun <T> AlarmChoiceGrid(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    columns: Int = 2
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    val isSelected = value == selected
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = appHapticAction(onAction = { onSelect(value) })),
                        shape = RoundedCornerShape(appCornerRadius(10.dp)),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        } else {
                            appBubbleBackgroundColor(defaultAlpha = 0.30f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.56f)
                            else appPanelBorderColor().copy(alpha = 0.85f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = appScaledSpacing(6.dp), vertical = appScaledSpacing(8.dp)),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AlarmActionGrid(
    items: List<Pair<String, () -> Unit>>,
    columns: Int = 2
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { (label, onClick) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = appHapticAction(onAction = onClick)),
                        shape = RoundedCornerShape(appCornerRadius(10.dp)),
                        color = appBubbleBackgroundColor(defaultAlpha = 0.30f),
                        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = appScaledSpacing(6.dp), vertical = appScaledSpacing(8.dp)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AlarmSettingToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val triggerHaptic = appHapticAction(onAction = {})
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(10.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.30f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    triggerHaptic()
                    onCheckedChange(!checked)
                })
                .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = {
                    triggerHaptic()
                    onCheckedChange(it)
                },
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Composable
private fun <T> AlarmChoiceRow(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (label, value) ->
                val isSelected = value == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = appHapticAction(onAction = { onSelect(value) })),
                    shape = RoundedCornerShape(appCornerRadius(10.dp)),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    } else {
                        appBubbleBackgroundColor(defaultAlpha = 0.30f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.56f)
                        else appPanelBorderColor().copy(alpha = 0.85f)
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = appScaledSpacing(6.dp), vertical = appScaledSpacing(8.dp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmFloatSliderRow(
    title: String,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

