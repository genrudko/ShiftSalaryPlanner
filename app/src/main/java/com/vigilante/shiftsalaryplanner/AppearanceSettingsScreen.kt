package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

@Composable
fun AppearanceSettingsScreen(
    shiftTemplates: List<ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    onBack: () -> Unit,
    onColorSelected: (String, Int) -> Unit,
    onResetDefaults: () -> Unit
) {
    val colorOptions = listOf(
        0xFFBBDEFB.toInt(),
        0xFF90CAF9.toInt(),
        0xFFD1C4E9.toInt(),
        0xFFB39DDB.toInt(),
        0xFFFFE0B2.toInt(),
        0xFFFFCC80.toInt(),
        0xFFFFCDD2.toInt(),
        0xFFEF9A9A.toInt(),
        0xFFC8E6C9.toInt(),
        0xFFA5D6A7.toInt(),
        0xFFFFF9C4.toInt(),
        0xFFFFF59D.toInt(),
        0xFFF8BBD0.toInt(),
        0xFFF48FB1.toInt(),
        0xFFE0E0E0.toInt(),
        0xFFBDBDBD.toInt()
    )

    val shiftItems = shiftTemplates.map { it.code to it.title } + listOf(KEY_EMPTY_DAY to "Пустой день")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeaderAction(
                title = "Внешний вид",
                onBack = onBack,
                actionText = "Сбросить",
                onAction = onResetDefaults
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppSpacing.lg)
            ) {
                AppearanceLivePreview(
                    shiftTemplates = shiftTemplates,
                    shiftColors = shiftColors
                )

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                Text(
                    text = "Цвета смен",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "Изменения применяются сразу и отражаются в превью сверху",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                shiftItems.forEach { (key, label) ->
                    val fallback = if (key == KEY_EMPTY_DAY) {
                        0xFFF5F5F5.toInt()
                    } else {
                        val templateColorHex = shiftTemplates.firstOrNull { it.code == key }?.colorHex
                        parseColorHex(templateColorHex ?: "#E0E0E0", 0xFFE0E0E0.toInt())
                    }
                    val selectedColor = shiftColors[key] ?: defaultShiftColors()[key] ?: fallback

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.xl),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, appPanelBorderColor())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))

                            colorOptions.chunked(4).forEach { rowColors ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = AppSpacing.xs + AppSpacing.xxs),
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                                ) {
                                    rowColors.forEach { colorValue ->
                                        ColorChoiceChip(
                                            colorValue = colorValue,
                                            isSelected = colorValue == selectedColor,
                                            onClick = { onColorSelected(key, colorValue) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                }

                Spacer(modifier = Modifier.height(AppSpacing.xxl))
            }
        }
    }
}

@Composable
private fun AppearanceLivePreview(
    shiftTemplates: List<ShiftTemplateEntity>,
    shiftColors: Map<String, Int>
) {
    val previewTemplates = shiftTemplates
        .filter { it.active }
        .sortedBy { it.sortOrder }
        .take(3)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md)
        ) {
            Text(
                text = "Live-preview",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "Мини-карточки и таббар",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                previewTemplates.forEach { template ->
                    val colorValue = shiftColors[template.code]
                        ?: defaultShiftColors()[template.code]
                        ?: parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppRadius.lg),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, appPanelBorderColor())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.sm)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                            ) {
                                Spacer(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(colorValue), RoundedCornerShape(AppRadius.pill))
                                )
                                Text(
                                    text = template.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Text(
                                text = template.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.lg),
                color = appPanelColor(),
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs + AppSpacing.xxs),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BottomTab.entries.take(4).forEach { tab ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
