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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun ShiftPickerDialog(
    date: LocalDate,
    currentShiftCode: String?,
    shiftTemplates: List<ShiftTemplateEntity>,
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
                        Text("✕")
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
                    shiftTemplates.chunked(columns).forEach { rowItems ->
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

@Composable
private fun CurrentSelectionBar(
    currentTemplate: ShiftTemplateEntity?,
    currentShiftCode: String?,
    onClearShift: () -> Unit
) {
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
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(chipColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconGlyph(currentTemplate.iconKey, currentTemplate.code),
                        color = readableContentColor(chipColor),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTemplate?.code ?: currentShiftCode.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentTemplate?.title ?: "Текущий код",
                    style = MaterialTheme.typography.bodySmall
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
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(chipColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconGlyph(template.iconKey, template.code),
                    color = readableContentColor(chipColor),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = template.code,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = compactTemplateTitle(template.title),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = formatHoursCompact(template.totalHours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(4.5.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.dp, borderColor, RoundedCornerShape(4.5.dp))
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
