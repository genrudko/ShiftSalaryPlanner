package com.vigilante.shiftsalaryplanner

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayCompensation
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ShiftTemplateEditorScreen(
    currentTemplate: ShiftTemplateEntity?,
    currentSpecialRule: ShiftSpecialRule? = null,
    currentAlarmTemplateConfig: ShiftTemplateAlarmConfig? = null,
    onBack: () -> Unit,
    onSave: (ShiftTemplateEntity, ShiftTemplateAlarmConfig) -> Unit,
    onSaveSpecialRule: (String, ShiftSpecialRule) -> Unit = { _, _ -> },
    onDelete: (ShiftTemplateEntity) -> Unit
) {
    val isEditing = currentTemplate != null
    val isProtectedTemplate = isProtectedSystemTemplate(currentTemplate)

    var titleText by rememberSaveable { mutableStateOf(currentTemplate?.title ?: "") }
    var codeText by rememberSaveable { mutableStateOf(currentTemplate?.code ?: "") }
    var iconKey by rememberSaveable {
        mutableStateOf(currentTemplate?.iconKey?.takeUnless { it.startsWith("EMOJI:") } ?: "TEXT")
    }
    var totalHoursText by rememberSaveable { mutableStateOf(currentTemplate?.totalHours?.toPlainString() ?: "0") }
    var breakHoursText by rememberSaveable { mutableStateOf(currentTemplate?.breakHours?.toPlainString() ?: "0") }
    var nightHoursText by rememberSaveable { mutableStateOf(currentTemplate?.nightHours?.toPlainString() ?: "0") }
    var colorHexText by rememberSaveable { mutableStateOf(currentTemplate?.colorHex ?: "#1E88E5") }
    var specialDayTypeName by rememberSaveable {
        mutableStateOf(
            currentSpecialRule?.specialDayTypeName
                ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false).specialDayTypeName
        )
    }
    var specialDayCompensationName by rememberSaveable {
        mutableStateOf(
            currentSpecialRule?.specialDayCompensationName
                ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false).specialDayCompensationName
        )
    }
    var active by rememberSaveable { mutableStateOf(currentTemplate?.active ?: true) }
    var shiftStartHourText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.startHour ?: 8).toString()) }
    var shiftStartMinuteText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.startMinute ?: 0).toString()) }
    var shiftEndHourText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.endHour ?: 20).toString()) }
    var shiftEndMinuteText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.endMinute ?: 0).toString()) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showUnsavedExitConfirm by rememberSaveable { mutableStateOf(false) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(false) }
    var showTotalHoursPicker by rememberSaveable { mutableStateOf(false) }
    var showBreakHoursPicker by rememberSaveable { mutableStateOf(false) }
    var showNightHoursPicker by rememberSaveable { mutableStateOf(false) }
    var emojiText by rememberSaveable {
        mutableStateOf(currentTemplate?.iconKey?.takeIf { it.startsWith("EMOJI:") }?.removePrefix("EMOJI:") ?: "")
    }
    var colorDraftHex by rememberSaveable { mutableStateOf(normalizeHexColor(colorHexText)) }

    val iconOptions = listOf("SUN", "MOON", "EIGHT", "HOME", "OT", "SICK", "STAR", "TEXT")
    val previewIconKey = if (emojiText.isNotBlank()) "EMOJI:${emojiText.trim()}" else iconKey
    val selectedSpecialDayType = runCatching { SpecialDayType.valueOf(specialDayTypeName) }
        .getOrElse { SpecialDayType.NONE }
    val selectedSpecialDayCompensation = runCatching { SpecialDayCompensation.valueOf(specialDayCompensationName) }
        .getOrElse { SpecialDayCompensation.NONE }
    val hasSpecialDayRule = selectedSpecialDayType != SpecialDayType.NONE

    val normalizedCurrentColor = normalizeHexColor(currentTemplate?.colorHex ?: "#1E88E5")
    val normalizedEditedColor = normalizeHexColor(colorHexText)
    val originalEmoji = currentTemplate?.iconKey?.takeIf { it.startsWith("EMOJI:") }?.removePrefix("EMOJI:") ?: ""
    val originalIconKey = currentTemplate?.iconKey?.takeUnless { it.startsWith("EMOJI:") } ?: "TEXT"
    val originalSpecialRule = currentSpecialRule
        ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false)
    val originalAlarmConfig = currentAlarmTemplateConfig ?: currentTemplate?.let { defaultShiftTemplateAlarmConfig(it) }

    val paidHoursPreview = remember(totalHoursText, breakHoursText) {
        max(0.0, parseDouble(totalHoursText, 0.0) - parseDouble(breakHoursText, 0.0))
    }

    val hasUnsavedChanges = remember(
        currentTemplate,
        currentSpecialRule,
        currentAlarmTemplateConfig,
        titleText,
        codeText,
        iconKey,
        emojiText,
        totalHoursText,
        breakHoursText,
        nightHoursText,
        colorHexText,
        specialDayTypeName,
        specialDayCompensationName,
        active,
        shiftStartHourText,
        shiftStartMinuteText,
        shiftEndHourText,
        shiftEndMinuteText
    ) {
        if (currentTemplate == null) {
            titleText.isNotBlank() ||
                    codeText.isNotBlank() ||
                    emojiText.isNotBlank() ||
                    iconKey != "TEXT" ||
                    normalizeHexColor(colorHexText) != "#1E88E5" ||
                    parseDouble(totalHoursText, 0.0) != 0.0 ||
                    parseDouble(breakHoursText, 0.0) != 0.0 ||
                    parseDouble(nightHoursText, 0.0) != 0.0 ||
                    parseInt(shiftStartHourText, 8) != 8 ||
                    parseInt(shiftStartMinuteText, 0) != 0 ||
                    parseInt(shiftEndHourText, 20) != 20 ||
                    parseInt(shiftEndMinuteText, 0) != 0
        } else {
            val codeChanged = codeText.trim().uppercase() != currentTemplate.code
            val titleChanged = titleText.trim() != currentTemplate.title
            val iconChanged = iconKey != originalIconKey
            val emojiChanged = emojiText.trim() != originalEmoji
            val hoursChanged = parseDouble(totalHoursText, currentTemplate.totalHours) != currentTemplate.totalHours
            val breakChanged = parseDouble(breakHoursText, currentTemplate.breakHours) != currentTemplate.breakHours
            val nightChanged = parseDouble(nightHoursText, currentTemplate.nightHours) != currentTemplate.nightHours
            val colorChanged = normalizedEditedColor != normalizedCurrentColor
            val activeChanged = active != currentTemplate.active
            val specialTypeChanged = specialDayTypeName != originalSpecialRule.specialDayTypeName
            val specialCompensationChanged = specialDayCompensationName != originalSpecialRule.specialDayCompensationName
            val alarmChanged = originalAlarmConfig != null && (
                    parseInt(shiftStartHourText, originalAlarmConfig.startHour) != originalAlarmConfig.startHour ||
                            parseInt(shiftStartMinuteText, originalAlarmConfig.startMinute) != originalAlarmConfig.startMinute ||
                            parseInt(shiftEndHourText, originalAlarmConfig.endHour) != originalAlarmConfig.endHour ||
                            parseInt(shiftEndMinuteText, originalAlarmConfig.endMinute) != originalAlarmConfig.endMinute
                    )

            if (isProtectedTemplate) {
                colorChanged || activeChanged || specialTypeChanged || specialCompensationChanged || alarmChanged
            } else {
                codeChanged || titleChanged || iconChanged || emojiChanged || hoursChanged ||
                        breakChanged || nightChanged || colorChanged || activeChanged ||
                        specialTypeChanged || specialCompensationChanged || alarmChanged
            }
        }
    }

    fun requestClose() {
        if (hasUnsavedChanges) showUnsavedExitConfirm = true else onBack()
    }

    fun performSave() {
        val finalCode = codeText.trim().uppercase()
        if (finalCode.isBlank()) return

        val finalIconKey = if (emojiText.isNotBlank()) "EMOJI:${emojiText.trim()}" else iconKey
        val effectiveSpecialDayType = runCatching { SpecialDayType.valueOf(specialDayTypeName) }
            .getOrElse { SpecialDayType.NONE }
        val effectiveSpecialDayCompensation = runCatching { SpecialDayCompensation.valueOf(specialDayCompensationName) }
            .getOrElse { SpecialDayCompensation.NONE }
        val legacyWeekendPaid = legacyWeekendPaidFlag(
            specialDayType = effectiveSpecialDayType,
            specialDayCompensation = effectiveSpecialDayCompensation
        )

        val savedTemplate = ShiftTemplateEntity(
            code = finalCode,
            title = if (isProtectedTemplate) currentTemplate?.title ?: finalCode else titleText.trim().ifBlank { finalCode },
            iconKey = finalIconKey,
            totalHours = if (isProtectedTemplate) currentTemplate?.totalHours ?: 0.0 else parseDouble(totalHoursText, currentTemplate?.totalHours ?: 0.0),
            breakHours = if (isProtectedTemplate) currentTemplate?.breakHours ?: 0.0 else parseDouble(breakHoursText, currentTemplate?.breakHours ?: 0.0),
            nightHours = if (isProtectedTemplate) currentTemplate?.nightHours ?: 0.0 else parseDouble(nightHoursText, currentTemplate?.nightHours ?: 0.0),
            colorHex = normalizeHexColor(colorHexText),
            isWeekendPaid = if (isProtectedTemplate) currentTemplate?.isWeekendPaid ?: false else legacyWeekendPaid,
            active = active,
            sortOrder = currentTemplate?.sortOrder ?: 100
        )

        val alarmTemplateConfig = (currentAlarmTemplateConfig ?: defaultShiftTemplateAlarmConfig(savedTemplate)).copy(
            shiftCode = finalCode,
            startHour = parseInt(shiftStartHourText, currentAlarmTemplateConfig?.startHour ?: 8).coerceIn(0, 23),
            startMinute = parseInt(shiftStartMinuteText, currentAlarmTemplateConfig?.startMinute ?: 0).coerceIn(0, 59),
            endHour = parseInt(shiftEndHourText, currentAlarmTemplateConfig?.endHour ?: 20).coerceIn(0, 23),
            endMinute = parseInt(shiftEndMinuteText, currentAlarmTemplateConfig?.endMinute ?: 0).coerceIn(0, 59)
        )

        onSave(savedTemplate, alarmTemplateConfig)
        onSaveSpecialRule(
            finalCode,
            ShiftSpecialRule(
                specialDayTypeName = if (isProtectedTemplate) {
                    currentSpecialRule?.specialDayTypeName ?: SpecialDayType.NONE.name
                } else {
                    effectiveSpecialDayType.name
                },
                specialDayCompensationName = if (isProtectedTemplate) {
                    currentSpecialRule?.specialDayCompensationName ?: SpecialDayCompensation.NONE.name
                } else {
                    when (effectiveSpecialDayType) {
                        SpecialDayType.RVD -> effectiveSpecialDayCompensation.name
                        SpecialDayType.WEEKEND_HOLIDAY -> SpecialDayCompensation.DOUBLE_PAY.name
                        SpecialDayType.NONE -> SpecialDayCompensation.NONE.name
                    }
                }
            )
        )
    }

    val imeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    val previewColor = Color(parseColorHex(colorHexText, 0xFFE0E0E0.toInt()))

    LaunchedEffect(showColorPickerDialog) {
        if (showColorPickerDialog) {
            colorDraftHex = normalizeHexColor(colorHexText)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeaderAction(
                title = if (isEditing) "Смена" else "Новая смена",
                onBack = { requestClose() },
                actionText = "💾",
                onAction = { performSave() },
                actionEnabled = codeText.trim().isNotBlank()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "Основное",
                    subtitle = "Название, код и внешний вид"
                ) {
                    if (!isProtectedTemplate) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactEditorTextField(
                                label = "Название",
                                value = titleText,
                                onValueChange = { titleText = it },
                                modifier = Modifier.weight(1.4f)
                            )
                            CompactEditorTextField(
                                label = "Код",
                                value = codeText,
                                onValueChange = { codeText = it.uppercase() },
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    } else {
                        PaymentInfoRow("Название", currentTemplate?.title ?: "")
                        PaymentInfoRow("Код", currentTemplate?.code ?: "")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        CompactColorPickerButton(
                            label = "Цвет",
                            subtitle = "Выбрать",
                            color = previewColor,
                            onClick = { showColorPickerDialog = true },
                            modifier = Modifier.weight(1f)
                        )

                        CompactEditorTextField(
                            label = "Эмодзи",
                            value = emojiText,
                            onValueChange = { emojiText = it.take(8) },
                            placeholder = "🚚",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Предпросмотр",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Текущий значок шаблона",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(previewColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = iconGlyph(previewIconKey, codeText.ifBlank { "?" }),
                                color = readableContentColor(previewColor),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactIconMenuField(
                        label = "Обычная иконка",
                        currentIconKey = iconKey,
                        iconOptions = iconOptions,
                        codeFallback = codeText.ifBlank { "?" },
                        enabled = emojiText.isBlank(),
                        onSelect = {
                            iconKey = it
                            emojiText = ""
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Если поле эмодзи заполнено, обычная иконка не используется.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!isProtectedTemplate) {
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSectionCard(
                        title = "Время и часы",
                        subtitle = "Параметры смены и интервалы"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DurationValueButton(
                                label = "Всего",
                                value = formatDurationLabel(totalHoursText, 0.0),
                                onClick = { showTotalHoursPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            DurationValueButton(
                                label = "Обед",
                                value = formatDurationLabel(breakHoursText, 0.0),
                                onClick = { showBreakHoursPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            DurationValueButton(
                                label = "Ночь",
                                value = formatDurationLabel(nightHoursText, 0.0),
                                onClick = { showNightHoursPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeValueButton(
                                label = "Начало",
                                value = formatTimeLabel(shiftStartHourText, shiftStartMinuteText, 8, 0),
                                onClick = { showStartTimePicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            TimeValueButton(
                                label = "Конец",
                                value = formatTimeLabel(shiftEndHourText, shiftEndMinuteText, 20, 0),
                                onClick = { showEndTimePicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactSwitchTile(
                                title = "Особый день",
                                checked = hasSpecialDayRule,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        specialDayTypeName = SpecialDayType.WEEKEND_HOLIDAY.name
                                        specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
                                    } else {
                                        specialDayTypeName = SpecialDayType.NONE.name
                                        specialDayCompensationName = SpecialDayCompensation.NONE.name
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CompactSwitchTile(
                                title = "Активна",
                                checked = active,
                                onCheckedChange = { active = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Оплачиваемые часы: ${formatDouble(paidHoursPreview)}",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (hasSpecialDayRule) {
                        Spacer(modifier = Modifier.height(12.dp))

                        SettingsSectionCard(
                            title = "Особый режим",
                            subtitle = "Компенсация выходных и РВД"
                        ) {
                            Text(
                                text = "Тип дня",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                PayModeChoiceCard(
                                    title = "Выходная / праздничная",
                                    subtitle = "Обычная работа в выходной или праздник с повышенной оплатой",
                                    selected = selectedSpecialDayType == SpecialDayType.WEEKEND_HOLIDAY,
                                    onClick = {
                                        specialDayTypeName = SpecialDayType.WEEKEND_HOLIDAY.name
                                        specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                PayModeChoiceCard(
                                    title = "РВД",
                                    subtitle = "Работа в выходной день по распоряжению работодателя",
                                    selected = selectedSpecialDayType == SpecialDayType.RVD,
                                    onClick = {
                                        specialDayTypeName = SpecialDayType.RVD.name
                                        if (selectedSpecialDayCompensation == SpecialDayCompensation.NONE) {
                                            specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
                                        }
                                    }
                                )
                            }

                            if (selectedSpecialDayType == SpecialDayType.RVD) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Компенсация РВД",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp)
                                ) {
                                    PayModeChoiceCard(
                                        title = "Двойная оплата",
                                        subtitle = "Часы не требуют отдельного отгула",
                                        selected = selectedSpecialDayCompensation == SpecialDayCompensation.DOUBLE_PAY,
                                        onClick = { specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    PayModeChoiceCard(
                                        title = "Одинарная оплата + отгул",
                                        subtitle = "Повышенная доплата не начисляется, сверхурочка настраивается отдельно",
                                        selected = selectedSpecialDayCompensation == SpecialDayCompensation.SINGLE_PAY_WITH_DAY_OFF,
                                        onClick = { specialDayCompensationName = SpecialDayCompensation.SINGLE_PAY_WITH_DAY_OFF.name }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            PaymentInfoRow(
                                label = "Режим дня",
                                value = specialShiftRuleLabel(
                                    ShiftSpecialRule(
                                        specialDayTypeName = specialDayTypeName,
                                        specialDayCompensationName = specialDayCompensationName
                                    ),
                                    fallbackWeekendPaid = false
                                )
                            )
                        }
                    }
                }

                if (isEditing && !isProtectedTemplate && !imeVisible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Удалить шаблон")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showTotalHoursPicker) {
        DurationPickerDialog(
            title = "Всего часов",
            initialValue = parseDouble(totalHoursText, currentTemplate?.totalHours ?: 0.0),
            onDismiss = { showTotalHoursPicker = false },
            onConfirm = {
                totalHoursText = compactHoursToString(it)
                showTotalHoursPicker = false
            }
        )
    }

    if (showBreakHoursPicker) {
        DurationPickerDialog(
            title = "Неоплачиваемый обед",
            initialValue = parseDouble(breakHoursText, currentTemplate?.breakHours ?: 0.0),
            onDismiss = { showBreakHoursPicker = false },
            onConfirm = {
                breakHoursText = compactHoursToString(it)
                showBreakHoursPicker = false
            }
        )
    }

    if (showNightHoursPicker) {
        DurationPickerDialog(
            title = "Ночные часы",
            initialValue = parseDouble(nightHoursText, currentTemplate?.nightHours ?: 0.0),
            onDismiss = { showNightHoursPicker = false },
            onConfirm = {
                nightHoursText = compactHoursToString(it)
                showNightHoursPicker = false
            }
        )
    }

    if (showStartTimePicker) {
        WheelTimePickerDialog(
            title = "Начало смены",
            initialHour = parseInt(shiftStartHourText, 8).coerceIn(0, 23),
            initialMinute = parseInt(shiftStartMinuteText, 0).coerceIn(0, 59),
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                shiftStartHourText = hour.toString()
                shiftStartMinuteText = minute.toString()
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        WheelTimePickerDialog(
            title = "Конец смены",
            initialHour = parseInt(shiftEndHourText, 20).coerceIn(0, 23),
            initialMinute = parseInt(shiftEndMinuteText, 0).coerceIn(0, 59),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                shiftEndHourText = hour.toString()
                shiftEndMinuteText = minute.toString()
                showEndTimePicker = false
            }
        )
    }

    if (showColorPickerDialog) {
        Dialog(
            onDismissRequest = { showColorPickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Цвет смены",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FullColorPicker(
                        selectedColorHex = colorDraftHex,
                        onColorSelected = { colorDraftHex = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showColorPickerDialog = false }) {
                            Text("Отмена")
                        }
                        TextButton(onClick = {
                            colorHexText = normalizeHexColor(colorDraftHex)
                            showColorPickerDialog = false
                        }) {
                            Text("Готово")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && currentTemplate != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить шаблон?") },
            text = { Text("Шаблон будет удалён. Дни в календаре с этим кодом тоже очистятся.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(currentTemplate)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }

    if (showUnsavedExitConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitConfirm = false },
            title = { Text("Сохранить изменения?") },
            text = { Text("В шаблоне есть несохранённые изменения.") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedExitConfirm = false
                    performSave()
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showUnsavedExitConfirm = false
                        onBack()
                    }) {
                        Text("Не сохранять")
                    }
                    TextButton(onClick = { showUnsavedExitConfirm = false }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }
}

@Composable
private fun CompactEditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = androidx.compose.foundation.BorderStroke(1.dp, appPanelBorderColor())
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                decorationBox = { innerTextField ->
                    if (value.isBlank() && placeholder.isNotBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun CompactColorPickerButton(
    label: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(color)
                    .border(1.dp, appPanelBorderColor(), RoundedCornerShape(17.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CompactIconMenuField(
    label: String,
    currentIconKey: String,
    iconOptions: List<String>,
    codeFallback: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled) { expanded = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iconGlyph(currentIconKey, codeFallback),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (enabled) currentIconKey else "Иконка скрыта эмодзи",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (enabled) "Нажми, чтобы выбрать" else "Очисти эмодзи, чтобы включить",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false }
            ) {
                iconOptions.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = iconGlyph(item, codeFallback))
                                Text(text = item)
                            }
                        },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSwitchTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun DurationValueButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimeValueButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DurationPickerDialog(
    title: String,
    initialValue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val initialHours = remember(initialValue) { decimalHoursToHourMinute(initialValue).first }
    val initialMinutes = remember(initialValue) { decimalHoursToHourMinute(initialValue).second }
    var selectedHour by remember(initialHours) { mutableIntStateOf(initialHours.coerceIn(0, 24)) }
    var selectedMinute by remember(initialMinutes) { mutableIntStateOf(initialMinutes.coerceIn(0, 59)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        value = selectedHour,
                        range = 0..24,
                        formatter = { "%02d".format(it) },
                        onValueChange = { selectedHour = it }
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    NumberWheel(
                        value = selectedMinute,
                        range = 0..59,
                        formatter = { "%02d".format(it) },
                        onValueChange = { selectedMinute = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    TextButton(
                        onClick = { onConfirm(hourMinuteToDecimalHours(selectedHour, selectedMinute)) }
                    ) {
                        Text("Готово")
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHour by remember(initialHour) { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember(initialMinute) { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        value = selectedHour,
                        range = 0..23,
                        formatter = { "%02d".format(it) },
                        onValueChange = { selectedHour = it }
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    NumberWheel(
                        value = selectedMinute,
                        range = 0..59,
                        formatter = { "%02d".format(it) },
                        onValueChange = { selectedMinute = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                        Text("Готово")
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberWheel(
    value: Int,
    range: IntRange,
    formatter: (Int) -> String,
    onValueChange: (Int) -> Unit
) {
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = true
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                setFormatter { picked -> formatter(picked) }
                this.value = value
                setOnValueChangedListener { _, _, newValue ->
                    onValueChange(newValue)
                }
            }
        },
        update = { picker ->
            picker.minValue = range.first
            picker.maxValue = range.last
            picker.setFormatter { picked -> formatter(picked) }
            if (picker.value != value) {
                picker.value = value
            }
        }
    )
}

private fun compactHoursToString(value: Double): String {
    val rounded = (value * 100).roundToInt() / 100.0
    val intPart = rounded.toInt().toDouble()
    return if (abs(rounded - intPart) < 0.001) {
        intPart.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun decimalHoursToHourMinute(value: Double): Pair<Int, Int> {
    val safe = value.coerceIn(0.0, 24.0)
    val totalMinutes = (safe * 60.0).roundToInt().coerceIn(0, 24 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return hours to minutes
}

private fun hourMinuteToDecimalHours(hour: Int, minute: Int): Double {
    return hour.coerceIn(0, 24) + minute.coerceIn(0, 59) / 60.0
}

private fun formatDurationLabel(valueText: String, fallback: Double): String {
    val (hour, minute) = decimalHoursToHourMinute(parseDouble(valueText, fallback))
    return "%02d:%02d".format(hour, minute)
}

private fun formatTimeLabel(
    hourText: String,
    minuteText: String,
    fallbackHour: Int,
    fallbackMinute: Int
): String {
    val hour = parseInt(hourText, fallbackHour).coerceIn(0, 23)
    val minute = parseInt(minuteText, fallbackMinute).coerceIn(0, 59)
    return "%02d:%02d".format(hour, minute)
}

fun readableContentColor(background: Color): Color {
    return if (background.luminance() > 0.55f) {
        Color.Black.copy(alpha = 0.87f)
    } else {
        Color.White
    }
}

@Composable
fun ShiftTemplateBadge(template: ShiftTemplateEntity) {
    val bgColor = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconGlyph(template.iconKey, template.code),
            color = readableContentColor(bgColor),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
