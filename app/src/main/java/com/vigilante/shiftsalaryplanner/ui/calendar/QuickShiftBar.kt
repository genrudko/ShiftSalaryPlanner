package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.Workplace

@Composable
fun QuickShiftBar(
    shiftTemplates: List<ShiftTemplateEntity>,
    workplaces: List<Workplace>,
    activeWorkplaceId: String,
    systemStatusCodes: Set<String>,
    activeBrushCode: String?,
    isRangeClearModeActive: Boolean,
    onSelectBrush: (String) -> Unit,
    onClearBrush: () -> Unit,
    onDisableBrush: () -> Unit,
    onAddNewShift: () -> Unit,
    onOpenPatternEditor: () -> Unit,
    onClearCurrentMonth: () -> Unit,
    onStartRangeClearMode: () -> Unit,
    onClearAllCalendar: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMore by remember { mutableStateOf(false) }
    val panelContentColor = readableContentColor(appPanelColor())
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
            if (templates.isEmpty()) null else QuickShiftSection(workplaceId, title, templates)
        }
    }
    val sortedSystemTemplates = remember(systemTemplates) {
        systemTemplates.sortedBy { it.sortOrder }
    }
    val activeWorkplaceTemplates = groupedTemplates
        .firstOrNull { it.workplaceId == activeWorkplaceId }
        ?.templates
        .orEmpty()
    val compactTemplates = when {
        activeWorkplaceTemplates.isNotEmpty() -> activeWorkplaceTemplates
        groupedTemplates.isNotEmpty() -> groupedTemplates.first().templates
        sortedSystemTemplates.isNotEmpty() -> sortedSystemTemplates
        else -> shiftTemplates
    }
    val mainItems = compactTemplates.take(4)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = appPanelColor()
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = panelContentColor
                    )

                    val statusText = when (activeBrushCode) {
                        null -> if (isRangeClearModeActive) "Режим очистки диапазона" else "Обычный режим"
                        BRUSH_CLEAR -> "Активен ластик"
                        else -> "Кисть: ${stripWorkplaceScopeFromShiftCode(activeBrushCode)}"
                    }

                    val effectiveStatusText = if (showMore && !isRangeClearModeActive) {
                        "$statusText · смен: ${regularTemplates.size} · статусы: ${systemTemplates.size}"
                    } else {
                        statusText
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = effectiveStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }

                if (showMore) {
                    TextButton(onClick = appHapticAction(onAction = { showMore = false })) {
                        Text("Назад", color = panelContentColor)
                    }
                }

                TextButton(onClick = appHapticAction(onAction = onClose)) {
                    Text("Закрыть", color = panelContentColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!showMore) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mainItems.forEach { template ->
                        val displayCode = stripWorkplaceScopeFromShiftCode(template.code)
                        CompactQuickShiftButton(
                            iconKey = template.iconKey,
                            codeFallback = displayCode,
                            title = displayCode,
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
                        icon = Icons.AutoMirrored.Rounded.Backspace,
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
                        isSelected = activeBrushCode == null && !isRangeClearModeActive,
                        onClick = onDisableBrush,
                        modifier = Modifier.weight(1f)
                    )

                    CompactQuickShiftButton(
                        icon = Icons.Rounded.MoreHoriz,
                        title = "Ещё",
                        color = Color(0xFF81C784),
                        isSelected = false,
                        onClick = { showMore = true },
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedTemplates.forEach { section ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                        ) {
                            Text(
                                text = "${section.title} · ${section.templates.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = appListSecondaryTextColor(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        section.templates.chunked(4).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { template ->
                                    val displayCode = stripWorkplaceScopeFromShiftCode(template.code)
                                    CompactQuickShiftButton(
                                        iconKey = template.iconKey,
                                        codeFallback = displayCode,
                                        title = displayCode,
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

                    if (sortedSystemTemplates.isNotEmpty()) {
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

                        sortedSystemTemplates.chunked(4).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { template ->
                                    val displayCode = stripWorkplaceScopeFromShiftCode(template.code)
                                    CompactQuickShiftButton(
                                        iconKey = template.iconKey,
                                        codeFallback = displayCode,
                                        title = displayCode,
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
                        icon = Icons.AutoMirrored.Rounded.Backspace,
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
                        isSelected = activeBrushCode == null && !isRangeClearModeActive,
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

                Spacer(modifier = Modifier.height(6.dp))

                QuickEraseActionsRow(
                    isRangeClearModeActive = isRangeClearModeActive,
                    onClearCurrentMonth = onClearCurrentMonth,
                    onStartRangeClearMode = onStartRangeClearMode,
                    onClearAllCalendar = onClearAllCalendar
                )
            }
        }
    }
}

private data class QuickShiftSection(
    val workplaceId: String,
    val title: String,
    val templates: List<ShiftTemplateEntity>
)

@Composable
private fun QuickEraseActionsRow(
    isRangeClearModeActive: Boolean,
    onClearCurrentMonth: () -> Unit,
    onStartRangeClearMode: () -> Unit,
    onClearAllCalendar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompactQuickShiftButton(
            icon = Icons.Rounded.CalendarMonth,
            title = "Очистить месяц",
            color = Color(0xFFFFCC80),
            isSelected = false,
            onClick = onClearCurrentMonth,
            modifier = Modifier.weight(1f),
            labelMaxLines = 2
        )
        CompactQuickShiftButton(
            icon = Icons.AutoMirrored.Rounded.EventNote,
            title = "Очистить диапазон",
            color = Color(0xFFFFAB91),
            isSelected = isRangeClearModeActive,
            onClick = onStartRangeClearMode,
            modifier = Modifier.weight(1f),
            labelMaxLines = 2
        )
        CompactQuickShiftButton(
            icon = Icons.Rounded.DeleteOutline,
            title = "Очистить календарь",
            color = Color(0xFFEF9A9A),
            isSelected = false,
            onClick = onClearAllCalendar,
            modifier = Modifier.weight(1f),
            labelMaxLines = 2
        )
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
    useColorAsBackground: Boolean = false,
    labelMaxLines: Int = 1
) {
    val panelColor = appPanelColor()
    val backgroundColor = when {
        useColorAsBackground && isSelected -> color.copy(alpha = 0.42f)
        useColorAsBackground -> color.copy(alpha = 0.22f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        useColorAsBackground && isSelected -> color
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    val effectiveBackgroundColor = if (backgroundColor.alpha < 1f) {
        backgroundColor.compositeOver(panelColor)
    } else {
        backgroundColor
    }
    val contentColor = readableContentColor(effectiveBackgroundColor)

    Column(
        modifier = modifier
            .height(if (labelMaxLines > 1) 56.dp else 50.dp)
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
            maxLines = labelMaxLines,
            lineHeight = if (labelMaxLines > 1) 10.sp else 9.sp,
            textAlign = TextAlign.Center
        )
    }
}
