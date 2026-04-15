package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

@Composable
fun PatternGrid(
    steps: List<String>,
    selectedBrushCode: String,
    shiftTemplates: List<ShiftTemplateEntity>,
    onSetStep: (Int, String) -> Unit
) {
    val templateMap = remember(shiftTemplates) {
        shiftTemplates.associateBy { it.code }
    }

    Column {
        steps.chunked(7).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEachIndexed { columnIndex, stepCode ->
                    val absoluteIndex = rowIndex * 7 + columnIndex
                    val template = templateMap[stepCode]

                    PatternStepCell(
                        index = absoluteIndex + 1,
                        code = stepCode,
                        color = if (template != null) {
                            Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))
                        } else {
                            Color(0xFFF2F2F2)
                        },
                        onClick = {
                            if (selectedBrushCode == BRUSH_CLEAR) {
                                onSetStep(absoluteIndex, "")
                            } else {
                                onSetStep(absoluteIndex, selectedBrushCode)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PatternStepCell(
    index: Int,
    code: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (code.isBlank()) Color(0xFFF7F7F7) else color.copy(alpha = 0.28f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = code,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun PatternBrushChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun PatternBrushGrid(
    selectedBrushCode: String,
    shiftTemplates: List<ShiftTemplateEntity>,
    onSelect: (String) -> Unit
) {
    val items = listOf(BRUSH_CLEAR) + shiftTemplates.map { it.code }

    items.chunked(4).forEach { rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rowItems.forEach { code ->
                val template = shiftTemplates.firstOrNull { it.code == code }

                PatternBrushChip(
                    label = if (code == BRUSH_CLEAR) "Очистить" else code,
                    selected = selectedBrushCode == code,
                    color = if (code == BRUSH_CLEAR) {
                        Color(0xFFEF9A9A)
                    } else {
                        Color(parseColorHex(template?.colorHex ?: "#E0E0E0", 0xFFE0E0E0.toInt()))
                    },
                    onClick = { onSelect(code) },
                    modifier = Modifier.weight(1f)
                )
            }

            repeat(4 - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActivePatternBrushCard(
    selectedBrushCode: String,
    shiftTemplates: List<ShiftTemplateEntity>
) {
    val currentTemplate = shiftTemplates.firstOrNull { it.code == selectedBrushCode }

    val title = when {
        selectedBrushCode == BRUSH_CLEAR -> "Очистить"
        currentTemplate != null -> "${currentTemplate.code} — ${currentTemplate.title}"
        else -> selectedBrushCode
    }

    val dotColor = when {
        selectedBrushCode == BRUSH_CLEAR -> Color(0xFFEF9A9A)
        currentTemplate != null -> Color(parseColorHex(currentTemplate.colorHex, 0xFFE0E0E0.toInt()))
        else -> Color(0xFFBDBDBD)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Активный инструмент",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PatternQuickActionsRow(
    onClearAll: () -> Unit,
    onTrimTail: () -> Unit,
    onShiftLeft: () -> Unit,
    onShiftRight: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            onClick = onClearAll,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Text("Сброс", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = onTrimTail,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Text("Хвост", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = onShiftLeft,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowLeft,
                contentDescription = "Сдвиг влево"
            )
        }

        OutlinedButton(
            onClick = onShiftRight,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = "Сдвиг вправо"
            )
        }
    }
}
