package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayCompensation
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayType
import kotlin.math.max

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
        mutableStateOf(currentSpecialRule?.specialDayTypeName ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false).specialDayTypeName)
    }
    var specialDayCompensationName by rememberSaveable {
        mutableStateOf(currentSpecialRule?.specialDayCompensationName ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false).specialDayCompensationName)
    }
    var active by rememberSaveable { mutableStateOf(currentTemplate?.active ?: true) }
    var sortOrderText by rememberSaveable { mutableStateOf((currentTemplate?.sortOrder ?: 100).toString()) }
    var shiftStartHourText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.startHour ?: 8).toString()) }
    var shiftStartMinuteText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.startMinute ?: 0).toString()) }
    var shiftEndHourText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.endHour ?: 20).toString()) }
    var shiftEndMinuteText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.endMinute ?: 0).toString()) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showUnsavedExitConfirm by rememberSaveable { mutableStateOf(false) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var emojiText by rememberSaveable {
        mutableStateOf(currentTemplate?.iconKey?.takeIf { it.startsWith("EMOJI:") }?.removePrefix("EMOJI:") ?: "")
    }

    val iconOptions = listOf("SUN", "MOON", "EIGHT", "HOME", "OT", "SICK", "STAR", "TEXT")
    val previewIconKey = if (emojiText.isNotBlank()) "EMOJI:${emojiText.trim()}" else iconKey
    val selectedSpecialDayType = runCatching { SpecialDayType.valueOf(specialDayTypeName) }.getOrElse { SpecialDayType.NONE }
    val selectedSpecialDayCompensation = runCatching { SpecialDayCompensation.valueOf(specialDayCompensationName) }.getOrElse { SpecialDayCompensation.NONE }
    val hasSpecialDayRule = selectedSpecialDayType != SpecialDayType.NONE

    val normalizedCurrentColor = normalizeHexColor(currentTemplate?.colorHex ?: "#1E88E5")
    val normalizedEditedColor = normalizeHexColor(colorHexText)
    val originalEmoji = currentTemplate?.iconKey?.takeIf { it.startsWith("EMOJI:") }?.removePrefix("EMOJI:") ?: ""
    val originalIconKey = currentTemplate?.iconKey?.takeUnless { it.startsWith("EMOJI:") } ?: "TEXT"
    val originalSpecialRule = currentSpecialRule ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false)
    val originalAlarmConfig = currentAlarmTemplateConfig ?: currentTemplate?.let { defaultShiftTemplateAlarmConfig(it) }

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
        sortOrderText,
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
                    parseInt(sortOrderText, 100) != 100
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
            val sortChanged = parseInt(sortOrderText, currentTemplate.sortOrder) != currentTemplate.sortOrder
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
                codeChanged || titleChanged || iconChanged || emojiChanged || hoursChanged || breakChanged ||
                        nightChanged || colorChanged || activeChanged || sortChanged ||
                        specialTypeChanged || specialCompensationChanged || alarmChanged
            }
        }
    }

    fun requestClose() {
        if (hasUnsavedChanges) {
            showUnsavedExitConfirm = true
        } else {
            onBack()
        }
    }

    fun performSave() {
        val finalCode = codeText.trim().uppercase()
        if (finalCode.isBlank()) return

        val finalIconKey = if (emojiText.isNotBlank()) "EMOJI:${emojiText.trim()}" else iconKey
        val effectiveSpecialDayType = runCatching { SpecialDayType.valueOf(specialDayTypeName) }.getOrElse { SpecialDayType.NONE }
        val effectiveSpecialDayCompensation = runCatching { SpecialDayCompensation.valueOf(specialDayCompensationName) }.getOrElse { SpecialDayCompensation.NONE }
        val legacyWeekendPaid = legacyWeekendPaidFlag(
            specialDayType = effectiveSpecialDayType,
            specialDayCompensation = effectiveSpecialDayCompensation
        )

        val savedTemplate = ShiftTemplateEntity(
            code = finalCode,
            title = if (isProtectedTemplate) (currentTemplate?.title ?: finalCode) else titleText.trim().ifBlank { finalCode },
            iconKey = finalIconKey,
            totalHours = if (isProtectedTemplate) (currentTemplate?.totalHours ?: 0.0) else parseDouble(totalHoursText, currentTemplate?.totalHours ?: 0.0),
            breakHours = if (isProtectedTemplate) (currentTemplate?.breakHours ?: 0.0) else parseDouble(breakHoursText, currentTemplate?.breakHours ?: 0.0),
            nightHours = if (isProtectedTemplate) (currentTemplate?.nightHours ?: 0.0) else parseDouble(nightHoursText, currentTemplate?.nightHours ?: 0.0),
            colorHex = normalizeHexColor(colorHexText),
            isWeekendPaid = if (isProtectedTemplate) (currentTemplate?.isWeekendPaid ?: false) else legacyWeekendPaid,
            active = active,
            sortOrder = if (isProtectedTemplate) (currentTemplate?.sortOrder ?: 100) else parseInt(sortOrderText, currentTemplate?.sortOrder ?: 100)
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
                specialDayTypeName = if (isProtectedTemplate) (currentSpecialRule?.specialDayTypeName ?: SpecialDayType.NONE.name) else effectiveSpecialDayType.name,
                specialDayCompensationName = if (isProtectedTemplate) (currentSpecialRule?.specialDayCompensationName ?: SpecialDayCompensation.NONE.name) else when (effectiveSpecialDayType) {
                    SpecialDayType.RVD -> effectiveSpecialDayCompensation.name
                    SpecialDayType.WEEKEND_HOLIDAY -> SpecialDayCompensation.DOUBLE_PAY.name
                    SpecialDayType.NONE -> SpecialDayCompensation.NONE.name
                }
            )
        )
    }

    val imeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

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
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            label = { Text("Название") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = codeText,
                            onValueChange = { codeText = it.uppercase() },
                            label = { Text("Код") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            singleLine = true
                        )
                    } else {
                        PaymentInfoRow("Название", currentTemplate?.title ?: "")
                        PaymentInfoRow("Код", currentTemplate?.code ?: "")
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(text = "Иконка", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Текущий значок", style = MaterialTheme.typography.bodyMedium)

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(parseColorHex(colorHexText, 0xFFE0E0E0.toInt()))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = iconGlyph(previewIconKey, codeText.ifBlank { "?" }),
                                color = readableContentColor(Color(parseColorHex(colorHexText, 0xFFE0E0E0.toInt()))),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = emojiText,
                        onValueChange = { emojiText = it.take(8) },
                        label = { Text("Эмодзи-значок") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        placeholder = { Text("Например 🚚 или 🛠️") }
                    )

                    Text(
                        text = "Если поле заполнено, будет использоваться эмодзи. Чтобы вернуться к обычной иконке, очисти поле.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    iconOptions.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                IconChoiceButton(
                                    iconKey = item,
                                    codeFallback = codeText.ifBlank { "?" },
                                    selected = emojiText.isBlank() && iconKey == item,
                                    onClick = {
                                        iconKey = item
                                        emojiText = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Цвет", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(parseColorHex(colorHexText, 0xFFE0E0E0.toInt())))
                                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(18.dp))
                        )

                        OutlinedTextField(
                            value = colorHexText,
                            onValueChange = { colorHexText = normalizeHexColor(it) },
                            label = { Text("HEX") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showColorPickerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Открыть палитру")
                    }
                }

                if (!isProtectedTemplate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionCard(
                        title = "Расчёт",
                        subtitle = "Часы, обед, ночные"
                    ) {
                        PayrollNumberField(label = "Всего часов", value = totalHoursText, onValueChange = { totalHoursText = it })
                        PayrollNumberField(label = "Неоплачиваемый обед, часов", value = breakHoursText, onValueChange = { breakHoursText = it })
                        PayrollNumberField(label = "Ночные часы", value = nightHoursText, onValueChange = { nightHoursText = it })
                        PayrollIntField(label = "Порядок сортировки", value = sortOrderText, onValueChange = { sortOrderText = it })

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Время смены", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactIntField(label = "Начало, ч", value = shiftStartHourText, onValueChange = { shiftStartHourText = it }, modifier = Modifier.weight(1f))
                            CompactIntField(label = "Начало, мин", value = shiftStartMinuteText, onValueChange = { shiftStartMinuteText = it }, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactIntField(label = "Конец, ч", value = shiftEndHourText, onValueChange = { shiftEndHourText = it }, modifier = Modifier.weight(1f))
                            CompactIntField(label = "Конец, мин", value = shiftEndMinuteText, onValueChange = { shiftEndMinuteText = it }, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        CompactSwitchRow(
                            title = "Работа в выходной / праздник",
                            checked = hasSpecialDayRule,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    specialDayTypeName = SpecialDayType.WEEKEND_HOLIDAY.name
                                    specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
                                } else {
                                    specialDayTypeName = SpecialDayType.NONE.name
                                    specialDayCompensationName = SpecialDayCompensation.NONE.name
                                }
                            }
                        )

                        if (hasSpecialDayRule) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Тип дня", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
                                Text(text = "Компенсация РВД", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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

                        Spacer(modifier = Modifier.height(8.dp))
                        CompactSwitchRow(title = "Активна", checked = active, onCheckedChange = { active = it })
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Оплачиваемые часы: ${formatDouble(max(0.0, parseDouble(totalHoursText, 0.0) - parseDouble(breakHoursText, 0.0)))}",
                            fontWeight = FontWeight.Bold
                        )
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
                        selectedColorHex = colorHexText,
                        onColorSelected = { colorHexText = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showColorPickerDialog = false }) {
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
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
