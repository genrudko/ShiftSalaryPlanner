package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.Workplace
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun ShiftPickerDialog(
    date: LocalDate,
    currentShiftCode: String?,
    shiftTemplates: List<ShiftTemplateEntity>,
    workplaces: List<Workplace>,
    systemStatusCodes: Set<String>,
    templateMap: Map<String, ShiftTemplateEntity>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    onDismiss: () -> Unit,
    onSelectShiftCode: (String) -> Unit,
    onClearShift: () -> Unit
) {
    val currentTemplate = currentShiftCode?.let { templateMap[it] }
    val holiday = holidayMap[date]
    val dateLabel = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 4
    val workplaceNameById = remember(workplaces) { workplaces.associate { it.id to it.name } }
    val regularTemplates = remember(shiftTemplates, systemStatusCodes) {
        shiftTemplates.filterNot { template -> isSystemStatusCode(template.code, systemStatusCodes) }
    }
    val systemTemplates = remember(shiftTemplates, systemStatusCodes) {
        shiftTemplates
            .filter { template -> isSystemStatusCode(template.code, systemStatusCodes) }
            .groupBy { template -> stripWorkplaceScopeFromShiftCode(template.code) }
            .values
            .map { group ->
                group.firstOrNull { template -> !isWorkplaceScopedShiftCode(template.code) } ?: group.first()
            }
    }
    val groupedTemplates = remember(regularTemplates, workplaces, workplaceNameById) {
        val templatesByWorkplaceId = regularTemplates.groupBy { workplaceIdFromShiftCode(it.code) }
        val orderedIds = workplaces.map { it.id }
        val orderedSections = orderedIds.mapNotNull { workplaceId ->
            val templates = templatesByWorkplaceId[workplaceId].orEmpty()
            if (templates.isEmpty()) {
                null
            } else {
                workplaceId to (workplaceNameById[workplaceId] ?: "Работа")
            }
        }
        val extraSections = templatesByWorkplaceId
            .keys
            .filterNot { it in orderedIds }
            .sorted()
            .map { workplaceId -> workplaceId to "Работа" }

        (orderedSections + extraSections).mapNotNull { (workplaceId, title) ->
            val templates = templatesByWorkplaceId[workplaceId].orEmpty()
            if (templates.isEmpty()) null else ShiftPickerSection(workplaceId, title, templates)
        }
    }
    val groupedSystemTemplates = remember(systemTemplates, workplaces, workplaceNameById) {
        val templatesByWorkplaceId = systemTemplates.groupBy { workplaceIdFromShiftCode(it.code) }
        val orderedIds = workplaces.map { it.id }
        val orderedSections = orderedIds.mapNotNull { workplaceId ->
            val templates = templatesByWorkplaceId[workplaceId].orEmpty()
            if (templates.isEmpty()) {
                null
            } else {
                workplaceId to (workplaceNameById[workplaceId] ?: "Работа")
            }
        }
        val extraSections = templatesByWorkplaceId
            .keys
            .filterNot { it in orderedIds }
            .sorted()
            .map { workplaceId -> workplaceId to "Работа" }

        (orderedSections + extraSections).mapNotNull { (workplaceId, title) ->
            val templates = templatesByWorkplaceId[workplaceId].orEmpty()
            if (templates.isEmpty()) null else ShiftPickerSection(workplaceId, title, templates)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.72f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Смена",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (holiday != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            CompactHolidayPill(holiday = holiday)
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (currentTemplate != null || currentShiftCode != null) {
                    CurrentSelectionBar(
                        currentTemplate = currentTemplate,
                        currentShiftCode = currentShiftCode,
                        onClearShift = onClearShift
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedTemplates.forEach { section ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                        ) {
                            Text(
                                text = "${section.title} · ${section.templates.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = appListSecondaryTextColor(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        section.templates.chunked(columns).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { template ->
                                    MiniShiftGridItem(
                                        template = template,
                                        selected = currentShiftCode == template.code,
                                        onClick = { onSelectShiftCode(template.code) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (groupedSystemTemplates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Системные статусы",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        groupedSystemTemplates.forEach { section ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                            ) {
                                Text(
                                    text = "${section.title} · ${section.templates.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appListSecondaryTextColor(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            section.templates.chunked(columns).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { template ->
                                        MiniShiftGridItem(
                                            template = template,
                                            selected = currentShiftCode == template.code,
                                            onClick = { onSelectShiftCode(template.code) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentSelectionBar(
    currentTemplate: ShiftTemplateEntity?,
    currentShiftCode: String?,
    onClearShift: () -> Unit
) {
    val currentDisplayCode = currentTemplate?.code
        ?.let(::stripWorkplaceScopeFromShiftCode)
        ?: currentShiftCode.orEmpty().let(::stripWorkplaceScopeFromShiftCode)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentTemplate != null) {
                val chipColor = Color(parseColorHex(currentTemplate.colorHex, 0xFFE0E0E0.toInt()))
                val displayCode = stripWorkplaceScopeFromShiftCode(currentTemplate.code)
                IconBadge(
                    iconKey = currentTemplate.iconKey,
                    fallbackCode = displayCode,
                    badgeColor = chipColor,
                    size = 28.dp,
                    shape = RoundedCornerShape(9.dp),
                    selected = true,
                    unselectedBorderColor = appPanelBorderColor()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentDisplayCode,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentTemplate?.title ?: "Текущий код",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }

            TextButton(onClick = onClearShift) {
                Text("Сброс")
            }
        }
    }
}

@Composable
private fun MiniShiftGridItem(
    template: ShiftTemplateEntity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayCode = stripWorkplaceScopeFromShiftCode(template.code)
    val chipColor = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        appPanelBorderColor()
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(
                iconKey = template.iconKey,
                fallbackCode = displayCode,
                badgeColor = chipColor,
                size = 28.dp,
                shape = RoundedCornerShape(9.dp),
                selected = selected,
                unselectedBorderColor = borderColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = displayCode,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = compactTemplateTitle(template.title),
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor(),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = formatHoursCompact(template.totalHours),
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }
    }
}

@Composable
private fun CompactHolidayPill(holiday: HolidayEntity) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = if (holiday.isNonWorking) holiday.title else "Особый день",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun compactTemplateTitle(title: String): String {
    return when {
        title.length <= 10 -> title
        else -> title.take(8).trimEnd() + "…"
    }
}

private fun formatHoursCompact(value: Double): String {
    val rounded = ((value * 100).toInt()) / 100.0
    val intValue = rounded.toInt().toDouble()
    val text = if (abs(rounded - intValue) < 0.001) {
        intValue.toInt().toString()
    } else {
        rounded.toString()
    }
    return "${text}ч"
}

private data class ShiftPickerSection(
    val workplaceId: String,
    val title: String,
    val templates: List<ShiftTemplateEntity>
)
