package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

@Composable
fun QuickShiftBar(
    shiftTemplates: List<ShiftTemplateEntity>,
    activeBrushCode: String?,
    onSelectBrush: (String) -> Unit,
    onClearBrush: () -> Unit,
    onDisableBrush: () -> Unit,
    onAddNewShift: () -> Unit,
    onOpenPatternEditor: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainItems = shiftTemplates.take(4)
    val extraItems = shiftTemplates.drop(4)
    var showMore by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isSystemInDarkTheme()) Color(0xFF1E2433) else Color(0xFFEEF2FB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showMore) "Все шаблоны" else "Быстрый ввод",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val statusText = when (activeBrushCode) {
                        null -> "Обычный режим"
                        BRUSH_CLEAR -> "Активен ластик"
                        else -> "Кисть: $activeBrushCode"
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }

                if (showMore) {
                    TextButton(onClick = appHapticAction(onAction = { showMore = false })) {
                        Text("Назад")
                    }
                }

                TextButton(onClick = appHapticAction(onAction = onClose)) {
                    Text("Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!showMore) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mainItems.forEach { template ->
                        CompactQuickShiftButton(
                            iconKey = template.iconKey,
                            codeFallback = template.code,
                            title = template.code,
                            color = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())),
                            isSelected = activeBrushCode == template.code,
                            onClick = { onSelectBrush(template.code) },
                            modifier = Modifier.weight(1f),
                            useColorAsBackground = true
                        )
                    }

                    repeat(4 - mainItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactQuickShiftButton(
                        icon = Icons.Rounded.Backspace,
                        title = "Ластик",
                        color = Color(0xFFEF9A9A),
                        isSelected = activeBrushCode == BRUSH_CLEAR,
                        onClick = onClearBrush,
                        modifier = Modifier.weight(1f)
                    )

                    CompactQuickShiftButton(
                        icon = Icons.Rounded.RadioButtonUnchecked,
                        title = "Обычный",
                        color = Color(0xFFBDBDBD),
                        isSelected = activeBrushCode == null,
                        onClick = onDisableBrush,
                        modifier = Modifier.weight(1f)
                    )

                    if (extraItems.isNotEmpty()) {
                        CompactQuickShiftButton(
                            icon = Icons.Rounded.MoreHoriz,
                            title = "Ещё",
                            color = Color(0xFF81C784),
                            isSelected = false,
                            onClick = { showMore = true },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        CompactQuickShiftButton(
                            icon = Icons.Rounded.Add,
                            title = "Новая",
                            color = Color(0xFF64B5F6),
                            isSelected = false,
                            onClick = onAddNewShift,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CompactQuickShiftButton(
                        icon = Icons.Rounded.Autorenew,
                        title = "Цикл",
                        color = Color(0xFFFFB74D),
                        isSelected = false,
                        onClick = onOpenPatternEditor,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shiftTemplates.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { template ->
                                CompactQuickShiftButton(
                                    iconKey = template.iconKey,
                                    codeFallback = template.code,
                                    title = template.code,
                                    color = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())),
                                    isSelected = activeBrushCode == template.code,
                                    onClick = { onSelectBrush(template.code) },
                                    modifier = Modifier.weight(1f),
                                    useColorAsBackground = true
                                )
                            }

                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactQuickShiftButton(
                        icon = Icons.Rounded.Add,
                        title = "Новая",
                        color = Color(0xFF64B5F6),
                        isSelected = false,
                        onClick = onAddNewShift,
                        modifier = Modifier.weight(1f)
                    )

                    CompactQuickShiftButton(
                        icon = Icons.Rounded.Backspace,
                        title = "Ластик",
                        color = Color(0xFFEF9A9A),
                        isSelected = activeBrushCode == BRUSH_CLEAR,
                        onClick = onClearBrush,
                        modifier = Modifier.weight(1f)
                    )

                    CompactQuickShiftButton(
                        icon = Icons.Rounded.RadioButtonUnchecked,
                        title = "Обычный",
                        color = Color(0xFFBDBDBD),
                        isSelected = activeBrushCode == null,
                        onClick = onDisableBrush,
                        modifier = Modifier.weight(1f)
                    )

                    CompactQuickShiftButton(
                        icon = Icons.Rounded.Autorenew,
                        title = "Цикл",
                        color = Color(0xFFFFB74D),
                        isSelected = false,
                        onClick = onOpenPatternEditor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactQuickShiftButton(
    iconKey: String? = null,
    codeFallback: String = "",
    icon: ImageVector? = null,
    title: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useColorAsBackground: Boolean = false
) {
    val backgroundColor = when {
        useColorAsBackground && isSelected -> color.copy(alpha = 0.42f)
        useColorAsBackground -> color.copy(alpha = 0.22f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.White
    }

    val borderColor = when {
        useColorAsBackground && isSelected -> color
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    val contentColor = when {
        useColorAsBackground && !isSelected -> Color(0xFF1A1A1A)
        useColorAsBackground && isSelected -> readableContentColor(color)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = appHapticAction(onAction = onClick))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconKey != null) {
            IconBadge(
                iconKey = iconKey,
                fallbackCode = codeFallback,
                badgeColor = if (useColorAsBackground) {
                    color
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                },
                size = 18.dp,
                shape = RoundedCornerShape(6.dp),
                selected = isSelected,
                unselectedBorderColor = if (useColorAsBackground) {
                    color.copy(alpha = 0.58f)
                } else {
                    appPanelBorderColor()
                },
                selectedGlowColor = if (useColorAsBackground) color else null
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        } else {
            val glyph = iconGlyph(iconKey ?: "TEXT", codeFallback)
            Text(
                text = glyph,
                color = contentColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = (shiftGlyphFontSize(glyph) - 1).coerceAtLeast(12).sp,
                maxLines = 1,
                lineHeight = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = title,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1,
            lineHeight = 9.sp
        )
    }
}
