package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun ColorChoiceChip(
    colorValue: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(colorValue))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun ShiftColorPalette(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val colors = shiftEditorPalette()
    val selectedColorInt = parseColorHex(selectedColorHex, 0xFFE0E0E0.toInt())

    Column {
        Text(
            text = "Цвет смены",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        colors.chunked(5).forEach { rowColors ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowColors.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(colorInt))
                            .border(
                                width = if (colorInt == selectedColorInt) 3.dp else 1.dp,
                                color = if (colorInt == selectedColorInt) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onColorSelected(colorIntToHex(colorInt))
                            }
                    )
                }

                repeat(5 - rowColors.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.trackContinuousTouch(
    onTouch: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onTouch(down.position)
        down.consume()

        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: break
            onTouch(change.position)
            change.consume()
        } while (change.pressed)
    }
}

@Composable
fun FullColorPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val favoriteColors = listOf(
        "#1E88E5", "#1976D2", "#5C6BC0", "#7E57C2",
        "#43A047", "#26A69A", "#F9A825", "#FB8C00",
        "#EF5350", "#D81B60", "#8D6E63", "#78909C"
    )

    val normalizedSelectedHex = normalizeHexColor(selectedColorHex)
    val initialHsv = remember(normalizedSelectedHex) { hexToHsv(normalizedSelectedHex) }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var lastAppliedHex by remember { mutableStateOf(normalizedSelectedHex) }

    var colorAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var hueBarSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(normalizedSelectedHex) {
        if (normalizedSelectedHex != lastAppliedHex) {
            val externalHsv = hexToHsv(normalizedSelectedHex)
            hue = externalHsv[0]
            saturation = externalHsv[1]
            value = externalHsv[2]
            lastAppliedHex = normalizedSelectedHex
        }
    }

    val selectedColor = remember(hue, saturation, value) {
        Color.hsv(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
    }

    fun commitColor(
        newHue: Float = hue,
        newSaturation: Float = saturation,
        newValue: Float = value
    ) {
        val color = Color.hsv(
            newHue.coerceIn(0f, 360f),
            newSaturation.coerceIn(0f, 1f),
            newValue.coerceIn(0f, 1f)
        )
        val hex = normalizeHexColor(colorIntToHex(color.toArgb()))
        lastAppliedHex = hex
        onColorSelected(hex)
    }

    fun updateColorArea(offset: Offset) {
        if (colorAreaSize.width <= 0 || colorAreaSize.height <= 0) return

        val newSaturation = (offset.x / colorAreaSize.width.toFloat()).coerceIn(0f, 1f)
        val newValue = (1f - (offset.y / colorAreaSize.height.toFloat())).coerceIn(0f, 1f)

        saturation = newSaturation
        value = newValue
        commitColor(newSaturation = newSaturation, newValue = newValue)
    }

    fun updateHueBar(offset: Offset) {
        if (hueBarSize.width <= 0) return

        val newHue = ((offset.x / hueBarSize.width.toFloat()).coerceIn(0f, 1f)) * 360f
        hue = newHue
        commitColor(newHue = newHue)
    }

    val colorIndicatorX = saturation * colorAreaSize.width
    val colorIndicatorY = (1f - value) * colorAreaSize.height
    val hueIndicatorX = (hue / 360f) * hueBarSize.width

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Текущий цвет",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(selectedColor)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(21.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Избранные",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        favoriteColors.chunked(6).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { colorHex ->
                    FavoriteColorChip(
                        colorHex = colorHex,
                        selected = lastAppliedHex == normalizeHexColor(colorHex),
                        onClick = {
                            val normalized = normalizeHexColor(colorHex)
                            val hsv = hexToHsv(normalized)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                            lastAppliedHex = normalized
                            onColorSelected(normalized)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Точная настройка",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White,
                            Color.hsv(hue, 1f, 1f)
                        )
                    )
                )
                .onSizeChanged { colorAreaSize = it }
                .pointerInput(Unit) {
                    trackContinuousTouch { offset ->
                        updateColorArea(offset)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = colorIndicatorX.roundToInt() - 10,
                            y = colorIndicatorY.roundToInt() - 10
                        )
                    }
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Оттенок",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                )
                .onSizeChanged { hueBarSize = it }
                .pointerInput(Unit) {
                    trackContinuousTouch { offset ->
                        updateHueBar(offset)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = hueIndicatorX.roundToInt() - 9,
                            y = 3
                        )
                    }
                    .size(width = 18.dp, height = 20.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(9.dp)
                    )
            )
        }
    }
}

@Composable
fun UnifiedFullColorPickerDialog(
    title: String,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    showDismissButton: Boolean = true,
    dismissText: String = "Отмена",
    confirmText: String = "Готово"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(appCornerRadius(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(appScaledSpacing(16.dp))) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(appScaledSpacing(12.dp)))
                FullColorPicker(
                    selectedColorHex = selectedColorHex,
                    onColorSelected = onColorSelected
                )
                Spacer(modifier = Modifier.height(appScaledSpacing(12.dp)))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showDismissButton) {
                        TextButton(onClick = onDismiss) {
                            Text(dismissText)
                        }
                    }
                    TextButton(onClick = onConfirm) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteColorChip(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(parseColorHex(colorHex, 0xFFE0E0E0.toInt())))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
    )
}
@Composable
fun ColorChoiceButton(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(parseColorHex(colorHex, 0xFFE0E0E0.toInt())))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    )
}

