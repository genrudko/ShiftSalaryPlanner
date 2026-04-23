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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
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
    onModeChange: (TemplateMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(appBubbleBackgroundColor(defaultAlpha = 0.30f))
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
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = appHapticAction(onAction = onClick))
            .padding(vertical = appScaledSpacing(8.dp)),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListItem(
    template: ShiftTemplateEntity,
    specialRule: ShiftSpecialRule?,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val canSwipeActions = !isProtectedSystemTemplate(template)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDuplicate()
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete?.invoke()
                    false
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.32f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = canSwipeActions,
        enableDismissFromEndToStart = canSwipeActions && onDelete != null,
        backgroundContent = {
            TemplateSwipeBackground(
                dismissValue = dismissState.targetValue,
                canDelete = canSwipeActions && onDelete != null
            )
        }
    ) {
        TemplateListItemContent(
            template = template,
            specialRule = specialRule,
            onClick = onClick
        )
    }
}

@Composable
private fun TemplateSwipeBackground(
    dismissValue: SwipeToDismissBoxValue,
    canDelete: Boolean
) {
    val shape = RoundedCornerShape(appCornerRadius(12.dp))
    val isDuplicate = dismissValue == SwipeToDismissBoxValue.StartToEnd
    val isDelete = dismissValue == SwipeToDismissBoxValue.EndToStart && canDelete
    val showActions = isDuplicate || isDelete
    val accentColor = when {
        isDuplicate -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        isDelete -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val borderColor = when {
        showActions -> appPanelBorderColor().copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accentColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = appScaledSpacing(12.dp), vertical = appScaledSpacing(10.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leftColor = when {
            !showActions -> Color.Transparent
            isDuplicate -> MaterialTheme.colorScheme.primary
            else -> appListSecondaryTextColor()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = leftColor
            )
            Text(
                text = "Дублировать",
                style = MaterialTheme.typography.labelMedium,
                color = leftColor
            )
        }
        if (canDelete) {
            val rightColor = when {
                !showActions -> Color.Transparent
                isDelete -> MaterialTheme.colorScheme.error
                else -> appListSecondaryTextColor()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
            ) {
                Text(
                    text = "Удалить",
                    style = MaterialTheme.typography.labelMedium,
                    color = rightColor
                )
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = rightColor
                )
            }
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
private fun TemplateListItemContent(
    template: ShiftTemplateEntity,
    specialRule: ShiftSpecialRule?,
    onClick: () -> Unit
) {
    val itemShape = RoundedCornerShape(appCornerRadius(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(appBubbleBackgroundColor(defaultAlpha = 0.24f))
            .border(1.dp, appPanelBorderColor().copy(alpha = 0.55f), itemShape)
            .clickable(onClick = appHapticAction(onAction = onClick))
            .padding(vertical = appScaledSpacing(6.dp), horizontal = appScaledSpacing(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftTemplateBadge(template = template)

        Spacer(modifier = Modifier.width(appScaledSpacing(8.dp)))

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
                color = appListSecondaryTextColor(),
                maxLines = 1
            )
            Text(
                text = specialShiftRuleLabel(specialRule, template.isWeekendPaid),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = appListSecondaryTextColor()
            )
        }

        Spacer(modifier = Modifier.width(appScaledSpacing(6.dp)))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(appBubbleBackgroundColor(defaultAlpha = 0.24f))
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(14.dp))
            .clickable(onClick = appHapticAction(onAction = onEdit))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    color = appListSecondaryTextColor(),
                    maxLines = 1
                )
                Text(
                    text = "Дней в цикле: ${pattern.usedLength()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = appHapticAction(onAction = onApply),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Применить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = appHapticAction(onAction = onEdit),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Изменить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = appHapticAction(onAction = onDelete),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
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
