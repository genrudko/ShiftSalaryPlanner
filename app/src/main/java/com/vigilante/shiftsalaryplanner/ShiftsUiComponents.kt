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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TemplateModeSwitcher(
    mode: TemplateMode,
    onModeChange: (TemplateMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(18.dp))
            .padding(4.dp)
    ) {
        TemplateModeButton(
            text = "Смены",
            selected = mode == TemplateMode.SHIFTS,
            onClick = { onModeChange(TemplateMode.SHIFTS) },
            modifier = Modifier.weight(1f)
        )

        TemplateModeButton(
            text = "Чередование",
            selected = mode == TemplateMode.CYCLES,
            onClick = { onModeChange(TemplateMode.CYCLES) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TemplateModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TemplateListItem(
    template: ShiftTemplateEntity,
    specialRule: ShiftSpecialRule?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftTemplateBadge(template = template)

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = shiftTemplateSubtitle(template),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Text(
                text = specialShiftRuleLabel(specialRule, template.isWeekendPaid),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PatternListItem(
    pattern: PatternTemplate,
    onEdit: () -> Unit,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pattern.name.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (pattern.previewText().isBlank()) {
                        "Пустой график"
                    } else {
                        pattern.previewText()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Text(
                    text = "Дней в цикле: ${pattern.usedLength()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onApply) {
                Text("Применить")
            }
            TextButton(onClick = onEdit) {
                Text("Изменить")
            }
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}
@Composable
fun CompactScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    AppScreenHeader(
        title = title,
        onBack = onBack
    )
}
fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}
