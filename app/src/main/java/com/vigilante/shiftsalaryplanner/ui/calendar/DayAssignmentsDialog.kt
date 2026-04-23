package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.Workplace
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
    onDismiss: () -> Unit
) {
    val workplaceNameById = workplaces.associate { it.id to it.name }
    val sortedAssignments = workplaces.mapNotNull { workplace ->
        assignments.firstOrNull { it.workplaceId == workplace.id }
    }
    val dateTitle = formatDateTitle(date)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Смены на $dateTitle") },
        text = {
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(
                                iconKey = template?.iconKey.orEmpty(),
                                fallbackCode = displayCode,
                                badgeColor = badgeColor,
                                size = 28.dp,
                                shape = RoundedCornerShape(10.dp),
                                unselectedBorderColor = appPanelBorderColor()
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
                                    text = "Часы: $hoursLabel · Время: $timeLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appListSecondaryTextColor()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        dismissButton = {}
    )
}

private fun formatDateTitle(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru-RU"))
    return date.format(formatter)
}
