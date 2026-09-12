package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.Workplace
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayAssignmentsDialog(
    date: LocalDate,
    assignments: List<CalendarDayAssignment>,
    workplaces: List<Workplace>,
    templateMap: Map<String, ShiftTemplateEntity>,
    templateAlarmConfigs: Map<String, ShiftTemplateAlarmConfig>,
    shiftColors: Map<String, Int>,
    shiftDayRecordsByCode: Map<String, ShiftDayEntity> = emptyMap(),
    notes: List<AppNote> = emptyList(),
    onAddNote: (LocalDate, CalendarDayAssignment?) -> Unit = { _, _ -> },
    onEditNote: (String) -> Unit = {},
    onSaveShiftDayOverride: (ShiftDayEntity) -> Unit = {},
    onDismiss: () -> Unit
) {
    val dateTitle = formatDateTitle(date)
    var editingAssignment by remember { mutableStateOf<CalendarDayAssignment?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(appCornerRadius(28.dp)),
        containerColor = evolutionColorRoles().surfaceFloating,
        tonalElevation = 0.dp,
        title = { Text("Смены на $dateTitle") },
        text = {
            DayAssignmentsContent(
                date = date,
                assignments = assignments,
                workplaces = workplaces,
                templateMap = templateMap,
                templateAlarmConfigs = templateAlarmConfigs,
                shiftColors = shiftColors,
                shiftDayRecordsByCode = shiftDayRecordsByCode,
                notes = notes,
                onAddNote = onAddNote,
                onEditNote = onEditNote,
                onEditAssignment = { assignment -> editingAssignment = assignment }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        dismissButton = {}
    )

    val assignmentToEdit = editingAssignment
    val shiftDayRecordToEdit = assignmentToEdit?.let { shiftDayRecordsByCode[it.shiftCode] }
    if (assignmentToEdit != null && shiftDayRecordToEdit != null) {
        ShiftDayOverrideDialog(
            date = date,
            assignment = assignmentToEdit,
            existing = shiftDayRecordToEdit,
            template = templateMap[assignmentToEdit.shiftCode],
            templateAlarmConfig = templateAlarmConfigs[assignmentToEdit.shiftCode],
            onDismiss = { editingAssignment = null },
            onSave = { updated ->
                onSaveShiftDayOverride(updated)
                editingAssignment = null
            }
        )
    }
}


@Composable
internal fun DayAssignmentsContent(
    date: LocalDate,
    assignments: List<CalendarDayAssignment>,
    workplaces: List<Workplace>,
    templateMap: Map<String, ShiftTemplateEntity>,
    templateAlarmConfigs: Map<String, ShiftTemplateAlarmConfig>,
    shiftColors: Map<String, Int>,
    shiftDayRecordsByCode: Map<String, ShiftDayEntity> = emptyMap(),
    notes: List<AppNote> = emptyList(),
    onAddNote: (LocalDate, CalendarDayAssignment?) -> Unit = { _, _ -> },
    onEditNote: (String) -> Unit = {},
    onEditAssignment: (CalendarDayAssignment) -> Unit = {}
) {
    val workplaceNameById = workplaces.associate { it.id to it.name }
    val sortedAssignments = workplaces.mapNotNull { workplace ->
        assignments.firstOrNull { it.workplaceId == workplace.id }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sortedAssignments.isEmpty()) {
            Text(
                text = "На этот день нет назначенных смен.",
                style = MaterialTheme.typography.bodyMedium,
                color = appListSecondaryTextColor()
            )
        } else {
            DayOverrideHintCard()
            sortedAssignments.forEach { assignment ->
                val template = templateMap[assignment.shiftCode]
                val templateAlarmConfig = templateAlarmConfigs[assignment.shiftCode]
                val displayCode = stripWorkplaceScopeFromShiftCode(assignment.shiftCode)
                val badgeColor = shiftCellColor(
                    assignment.shiftCode,
                    shiftColors,
                    templateMap
                )
                val workplaceName = workplaceNameById[assignment.workplaceId]
                    ?: assignment.workplaceId
                val hoursLabel = template?.let { "${formatHours(it.paidHours())} ч" } ?: "—"
                val timeLabel = templateAlarmConfig?.let { config ->
                    "${formatClockHm(config.startHour, config.startMinute)}-${formatClockHm(config.endHour, config.endMinute)}"
                } ?: "не задано"
                val shiftDayRecord = shiftDayRecordsByCode[assignment.shiftCode]
                val hasOverride = shiftDayRecord?.hasDayOverride() == true
                val actualHoursLabel = shiftDayRecord?.overridePaidHours
                    ?.let { "${formatHours(it)} ч" }
                    ?: hoursLabel

                EvolutionSurface(
                    modifier = Modifier.fillMaxWidth(),
                    role = if (hasOverride) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
                    shape = RoundedCornerShape(appCornerRadius(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(
                            iconKey = template?.iconKey.orEmpty(),
                            fallbackCode = displayCode,
                            badgeColor = badgeColor,
                            size = 30.dp,
                            shape = RoundedCornerShape(10.dp),
                            selected = hasOverride,
                            unselectedBorderColor = evolutionColorRoles().contentSecondary.copy(alpha = 0.35f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workplaceName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${template?.title ?: "Смена"} · $displayCode",
                                style = MaterialTheme.typography.bodySmall,
                                color = appListSecondaryTextColor()
                            )
                            Text(
                                text = "Часы: $actualHoursLabel · Время: ${shiftDayRecord.overrideTimeLabel(timeLabel)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = appListSecondaryTextColor()
                            )
                            if (hasOverride) {
                                OverridePill("Индивидуальная правка")
                            }
                        }
                        if (shiftDayRecord != null) {
                            TextButton(
                                onClick = appHapticAction { onEditAssignment(assignment) }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (hasOverride) "Изменить" else "Этот день")
                            }
                        }
                    }
                }
            }
        }

        AppServiceDivider()

        Text(
            text = "Заметки",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (notes.isEmpty()) {
            Text(
                text = "Пока нет заметок для этого дня.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        } else {
            notes.forEach { note ->
                NotePreviewCard(
                    note = note,
                    onClick = { onEditNote(note.id) }
                )
            }
        }
        TextButton(
            onClick = appHapticAction { onAddNote(date, null) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Заметка на день")
        }
        if (sortedAssignments.isNotEmpty()) {
            sortedAssignments.forEach { assignment ->
                val template = templateMap[assignment.shiftCode]
                TextButton(
                    onClick = appHapticAction { onAddNote(date, assignment) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Заметка: ${template?.title ?: stripWorkplaceScopeFromShiftCode(assignment.shiftCode)}")
                }
            }
        }
    }

}

@Composable
private fun ShiftDayOverrideDialog(
    date: LocalDate,
    assignment: CalendarDayAssignment,
    existing: ShiftDayEntity,
    template: ShiftTemplateEntity?,
    templateAlarmConfig: ShiftTemplateAlarmConfig?,
    onDismiss: () -> Unit,
    onSave: (ShiftDayEntity) -> Unit
) {
    val fallbackStart = templateAlarmConfig?.let { formatClockHm(it.startHour, it.startMinute) }.orEmpty()
    val fallbackEnd = templateAlarmConfig?.let { formatClockHm(it.endHour, it.endMinute) }.orEmpty()
    var startTime by remember(existing) { mutableStateOf(existing.overrideStartTime.orEmpty()) }
    var endTime by remember(existing) { mutableStateOf(existing.overrideEndTime.orEmpty()) }
    var paidHours by remember(existing) { mutableStateOf(existing.overridePaidHours?.let(::formatHours).orEmpty()) }
    var breakHours by remember(existing) { mutableStateOf(existing.overrideBreakHours?.let(::formatHours).orEmpty()) }
    var nightHours by remember(existing) { mutableStateOf(existing.overrideNightHours?.let(::formatHours).orEmpty()) }
    var shiftPay by remember(existing) { mutableStateOf(existing.overrideShiftPayAmount?.let(::formatHours).orEmpty()) }
    var note by remember(existing) { mutableStateOf(existing.overrideNote.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(appCornerRadius(28.dp)),
        containerColor = evolutionColorRoles().surfaceFloating,
        tonalElevation = 0.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Индивидуальная правка")
                Text(
                    text = "Только для ${formatDateTitle(date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appListSecondaryTextColor()
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EvolutionSurface(
                    modifier = Modifier.fillMaxWidth(),
                    role = EvolutionSurfaceRole.SOFT,
                    shape = RoundedCornerShape(appCornerRadius(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = template?.title ?: stripWorkplaceScopeFromShiftCode(assignment.shiftCode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Заполни только то, что отличается от шаблона.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appListSecondaryTextColor()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactOverrideField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = "Начало",
                                placeholder = fallbackStart,
                                modifier = Modifier.weight(1f)
                            )
                            CompactOverrideField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = "Конец",
                                placeholder = fallbackEnd,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactOverrideField(
                                value = paidHours,
                                onValueChange = { paidHours = it },
                                label = "Оплач.",
                                placeholder = template?.paidHours()?.let(::formatHours).orEmpty(),
                                modifier = Modifier.weight(1f)
                            )
                            CompactOverrideField(
                                value = breakHours,
                                onValueChange = { breakHours = it },
                                label = "Обед",
                                placeholder = template?.breakHours?.let(::formatHours).orEmpty(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactOverrideField(
                                value = nightHours,
                                onValueChange = { nightHours = it },
                                label = "Ночь",
                                placeholder = template?.nightHours?.let(::formatHours).orEmpty(),
                                modifier = Modifier.weight(1f)
                            )
                            CompactOverrideField(
                                value = shiftPay,
                                onValueChange = { shiftPay = it },
                                label = "За смену",
                                placeholder = template?.shiftPayAmount?.takeIf { it > 0.0 }?.let(::formatHours).orEmpty(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Text(
                    text = "Пустые поля берутся из шаблона. Правка влияет только на этот день.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = appHapticAction {
                    onSave(
                        existing.copy(
                            overrideStartTime = startTime.blankToNull(),
                            overrideEndTime = endTime.blankToNull(),
                            overridePaidHours = paidHours.toOverrideDoubleOrNull(),
                            overrideBreakHours = breakHours.toOverrideDoubleOrNull(),
                            overrideNightHours = nightHours.toOverrideDoubleOrNull(),
                            overrideShiftPayAmount = shiftPay.toOverrideDoubleOrNull(),
                            overrideNote = note.blankToNull()
                        )
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = appHapticAction {
                        onSave(
                            existing.copy(
                                overrideStartTime = null,
                                overrideEndTime = null,
                                overrideTotalHours = null,
                                overrideBreakHours = null,
                                overrideNightHours = null,
                                overridePaidHours = null,
                                overrideShiftPayAmount = null,
                                overrideNote = null
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сбросить")
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}

@Composable
private fun DayOverrideHintCard() {
    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Индивидуальная правка смены",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Кнопка «Этот день» меняет время и часы только выбранной даты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
        }
    }
}

@Composable
private fun OverridePill(text: String) {
    EvolutionSurface(
        role = EvolutionSurfaceRole.ACCENT,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CompactOverrideField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder) }
        } else {
            null
        },
        singleLine = true,
        modifier = modifier
    )
}

private fun ShiftDayEntity?.overrideTimeLabel(fallback: String): String {
    val start = this?.overrideStartTime
    val end = this?.overrideEndTime
    return if (!start.isNullOrBlank() || !end.isNullOrBlank()) {
        "${start.orEmpty().ifBlank { "?" }}-${end.orEmpty().ifBlank { "?" }}"
    } else {
        fallback
    }
}

private fun ShiftDayEntity.hasDayOverride(): Boolean =
    !overrideStartTime.isNullOrBlank() ||
        !overrideEndTime.isNullOrBlank() ||
        overrideTotalHours != null ||
        overrideBreakHours != null ||
        overrideNightHours != null ||
        overridePaidHours != null ||
        overrideShiftPayAmount != null ||
        !overrideNote.isNullOrBlank()

private fun String.blankToNull(): String? =
    trim().takeIf { it.isNotBlank() }

private fun String.toOverrideDoubleOrNull(): Double? =
    trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it >= 0.0 }

private fun formatDateTitle(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru-RU"))
    return date.format(formatter)
}
