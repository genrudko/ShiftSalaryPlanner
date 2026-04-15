package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

@Composable
fun ShiftTemplatesDialog(
    templates: List<ShiftTemplateEntity>,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (ShiftTemplateEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактор смен") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onAddNew) {
                        Text("Добавить смену")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (templates.isEmpty()) {
                    Text("Шаблонов смен пока нет.")
                } else {
                    templates.forEach { template ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onEdit(template) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${template.code} — ${template.title}",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Всего часов: ${formatDouble(template.totalHours)}")
                            Text("Обед: ${formatDouble(template.breakHours)}")
                            Text("Оплачиваемые: ${formatDouble(template.paidHours())}")
                            Text("Ночные: ${formatDouble(template.nightHours)}")
                            Text(
                                buildString {
                                    append(if (template.active) "Активна" else "Неактивна")
                                    append(" • ")
                                    append(if (template.isWeekendPaid) "Выходная/праздничная" else "Обычная")
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onEdit(template) }) {
                                    Text("Изменить")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        dismissButton = {}
    )
}
