package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

@Composable
fun ColorSettingsDialog(
    shiftTemplates: List<ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    onDismiss: () -> Unit,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Настройка цветов")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Пока это выбор из готовых цветов.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                shiftItems.forEach { item ->
                    val key = item.first
                    val label = item.second
                    val fallback = if (key == KEY_EMPTY_DAY) {
                        0xFFF5F5F5.toInt()
                    } else {
                        val templateColorHex = shiftTemplates.firstOrNull { it.code == key }?.colorHex
                        parseColorHex(templateColorHex ?: "#E0E0E0", 0xFFE0E0E0.toInt())
                    }

                    val selectedColor = shiftColors[key] ?: defaultShiftColors()[key] ?: fallback

                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    colorOptions.chunked(4).forEach { rowColors ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { colorValue ->
                                ColorChoiceChip(
                                    colorValue = colorValue,
                                    isSelected = colorValue == selectedColor,
                                    onClick = {
                                        onColorSelected(key, colorValue)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onResetDefaults) {
                Text("Сбросить")
            }
        }
    )
}
