package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.Workplace

@Composable
fun WorkplacesRenameDialog(
    workplaces: List<Workplace>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val fields = remember(workplaces) {
        mutableStateMapOf<String, String>().apply {
            workplaces.forEach { workplace ->
                this[workplace.id] = workplace.name
            }
        }
    }
    val hasInvalidName = workplaces.any { workplace ->
        fields[workplace.id].isNullOrBlank()
    }
    var saveRequested by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Названия работ") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Измени названия, чтобы удобнее разделять смены и расчёт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )

                workplaces.forEach { workplace ->
                    OutlinedTextField(
                        value = fields[workplace.id].orEmpty(),
                        onValueChange = { value -> fields[workplace.id] = value },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(
                                text = "Работа ${workplaces.indexOf(workplace) + 1}",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingText = {
                            if (saveRequested && fields[workplace.id].isNullOrBlank()) {
                                Text("Название не может быть пустым")
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saveRequested = true
                    if (hasInvalidName) return@TextButton
                    onSave(
                        workplaces.associate { workplace ->
                            workplace.id to fields[workplace.id].orEmpty().trim()
                        }
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

