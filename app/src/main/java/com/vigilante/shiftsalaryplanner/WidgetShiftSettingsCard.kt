package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.widget.WidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.defaultWidgetLongLabel
import com.vigilante.shiftsalaryplanner.widget.defaultWidgetMetaLabel
import com.vigilante.shiftsalaryplanner.widget.defaultWidgetShortLabel

@Composable
fun WidgetShiftSettingsCard(
    template: ShiftTemplateEntity,
    calendarColorInt: Int,
    initialSettings: WidgetShiftOverride,
    refreshToken: Int,
    onSave: (WidgetShiftOverride) -> Unit,
    onReset: () -> Unit,
    onDraftChanged: (WidgetShiftOverride, Boolean) -> Unit
) {
    var linkWithTemplate by remember(refreshToken, template.code) { mutableStateOf(initialSettings.linkWithTemplate) }
    var fullLabel by remember(refreshToken, template.code) { mutableStateOf(initialSettings.fullLabel) }
    var shortLabel by remember(refreshToken, template.code) { mutableStateOf(initialSettings.shortLabel) }
    var metaLabel by remember(refreshToken, template.code) { mutableStateOf(initialSettings.metaLabel) }
    var useCustomColor by remember(refreshToken, template.code) { mutableStateOf(initialSettings.useCustomColor) }
    var colorHex by remember(refreshToken, template.code) {
        mutableStateOf(initialSettings.colorHex.ifBlank { colorIntToHex(calendarColorInt) })
    }
    var expanded by rememberSaveable(refreshToken, template.code) { mutableStateOf(false) }
    var showColorDialog by rememberSaveable(refreshToken, template.code) { mutableStateOf(false) }

    val defaultFull = defaultWidgetLongLabel(template)
    val defaultShort = defaultWidgetShortLabel(template)
    val defaultMeta = defaultWidgetMetaLabel(template)

    val draftOverride = remember(linkWithTemplate, fullLabel, shortLabel, metaLabel, useCustomColor, colorHex) {
        WidgetShiftOverride(
            fullLabel = if (linkWithTemplate) "" else fullLabel.trim(),
            shortLabel = if (linkWithTemplate) "" else shortLabel.trim(),
            metaLabel = if (linkWithTemplate) "" else metaLabel.trim(),
            useCustomColor = if (linkWithTemplate) false else useCustomColor,
            colorHex = if (!linkWithTemplate && useCustomColor) normalizeHexColor(colorHex) else "",
            linkWithTemplate = linkWithTemplate
        )
    }

    val normalizedInitial = remember(initialSettings, calendarColorInt) {
        WidgetShiftOverride(
            fullLabel = if (initialSettings.linkWithTemplate) "" else initialSettings.fullLabel.trim(),
            shortLabel = if (initialSettings.linkWithTemplate) "" else initialSettings.shortLabel.trim(),
            metaLabel = if (initialSettings.linkWithTemplate) "" else initialSettings.metaLabel.trim(),
            useCustomColor = if (initialSettings.linkWithTemplate) false else initialSettings.useCustomColor,
            colorHex = if (!initialSettings.linkWithTemplate && initialSettings.useCustomColor) {
                normalizeHexColor(initialSettings.colorHex.ifBlank { colorIntToHex(calendarColorInt) })
            } else {
                ""
            },
            linkWithTemplate = initialSettings.linkWithTemplate
        )
    }

    val hasChanges = remember(draftOverride, normalizedInitial) { draftOverride != normalizedInitial }

    LaunchedEffect(draftOverride, hasChanges) {
        onDraftChanged(draftOverride, hasChanges)
    }

    val previewColor = Color(
        parseColorHex(
            if (!linkWithTemplate && useCustomColor) colorHex else colorIntToHex(calendarColorInt),
            calendarColorInt
        )
    )
    val displayShort = if (linkWithTemplate) defaultShort else shortLabel.ifBlank { defaultShort }
    val displayMeta = if (linkWithTemplate) defaultMeta else metaLabel.ifBlank { defaultMeta }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(previewColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconGlyph(template.iconKey, template.code),
                        color = readableContentColor(previewColor),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title.ifBlank { template.code },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Код: ${template.code} • $displayShort • $displayMeta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (linkWithTemplate) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Text(
                            text = if (linkWithTemplate) "По шаблону" else "Свои",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (linkWithTemplate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Свернуть" else "Изменить",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetCompactToggleTile(
                        title = "По шаблону",
                        checked = linkWithTemplate,
                        onCheckedChange = { checked ->
                            linkWithTemplate = checked
                            if (checked) useCustomColor = false
                        },
                        modifier = Modifier.weight(1f)
                    )

                    WidgetCompactToggleTile(
                        title = "Свой цвет",
                        checked = !linkWithTemplate && useCustomColor,
                        onCheckedChange = { checked ->
                            if (!linkWithTemplate) useCustomColor = checked
                        },
                        enabled = !linkWithTemplate,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!linkWithTemplate) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WidgetCompactTextField(
                            label = "Полная",
                            value = fullLabel,
                            onValueChange = { fullLabel = it },
                            placeholder = defaultFull,
                            modifier = Modifier.weight(1f)
                        )
                        WidgetCompactTextField(
                            label = "Короткая",
                            value = shortLabel,
                            onValueChange = { shortLabel = it },
                            placeholder = defaultShort,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        WidgetCompactTextField(
                            label = "Нижняя подпись",
                            value = metaLabel,
                            onValueChange = { metaLabel = it },
                            placeholder = defaultMeta,
                            modifier = Modifier.weight(1.15f)
                        )

                        if (useCustomColor) {
                            WidgetColorPickerButton(
                                title = "Цвет",
                                color = previewColor,
                                onClick = { showColorDialog = true },
                                modifier = Modifier.weight(0.85f)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.weight(0.85f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Цвет шаблона",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasChanges) {
                        Text(
                            text = "Есть изменения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    TextButton(onClick = {
                        linkWithTemplate = true
                        fullLabel = ""
                        shortLabel = ""
                        metaLabel = ""
                        useCustomColor = false
                        colorHex = colorIntToHex(calendarColorInt)
                        onReset()
                    }) {
                        Text("Сбросить")
                    }

                    TextButton(
                        enabled = hasChanges,
                        onClick = { if (hasChanges) onSave(draftOverride) }
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }

    if (showColorDialog) {
        Dialog(
            onDismissRequest = { showColorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Цвет для ${template.title.ifBlank { template.code }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FullColorPicker(
                        selectedColorHex = colorHex,
                        onColorSelected = { colorHex = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showColorDialog = false }) {
                            Text("Готово")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCompactToggleTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                modifier = Modifier.scale(0.76f),
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun WidgetCompactTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun WidgetColorPickerButton(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
                    .border(1.dp, appPanelBorderColor(), RoundedCornerShape(10.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
