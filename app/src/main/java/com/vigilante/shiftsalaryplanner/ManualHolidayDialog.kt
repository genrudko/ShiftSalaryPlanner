package com.vigilante.shiftsalaryplanner

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import java.time.LocalDate

@Composable
fun ManualHolidayDialog(
    currentRecord: ManualHolidayRecord?,
    onDismiss: () -> Unit,
    onSave: (ManualHolidayRecord) -> Unit
) {
    val context = LocalContext.current

    var selectedDate by rememberSaveable { mutableStateOf(currentRecord?.date ?: LocalDate.now().toString()) }
    var titleText by rememberSaveable { mutableStateOf(currentRecord?.title ?: "") }
    var kindText by rememberSaveable { mutableStateOf(currentRecord?.kind ?: HolidayKinds.HOLIDAY) }

    val selectedDateValue = remember(selectedDate) { LocalDate.parse(selectedDate) }
    val isShortDay = kindText == HolidayKinds.SHORT_DAY

    fun openDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth).toString()
            },
            selectedDateValue.year,
            selectedDateValue.monthValue - 1,
            selectedDateValue.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentRecord == null) "Новый праздник" else "Редактирование дня") },
        text = {
            Column {
                OutlinedButton(
                    onClick = { openDatePicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Дата: ${formatDate(selectedDateValue)}")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Тип дня",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val holidaySelected = !isShortDay

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (holidaySelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (holidaySelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Праздник",
                            color = if (holidaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isShortDay) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isShortDay) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Сокр. день",
                            color = if (isShortDay) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isShortDay) {
                        "Сокращённый рабочий день будет учитываться в производственном календаре."
                    } else {
                        "Нерабочий праздничный день будет учитываться как выходной и праздник."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ManualHolidayRecord(
                            date = selectedDate,
                            title = titleText.trim().ifBlank {
                                if (isShortDay) "Сокращённый день" else "Ручной праздник"
                            },
                            kind = kindText,
                            isNonWorking = !isShortDay
                        )
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
