package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.forEach

@Composable
fun ShiftPickerDialog(
    date: LocalDate,
    currentShiftCode: String?,
    shiftTemplates: List<ShiftTemplateEntity>,
    templateMap: Map<String, ShiftTemplateEntity>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    onDismiss: () -> Unit,
    onSelectShiftCode: (String) -> Unit,
    onClearShift: () -> Unit
) {
    val currentTemplate = currentShiftCode?.let { templateMap[it] }
    val holiday = holidayMap[date]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выбор смены",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Дата: ${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                        fontWeight = FontWeight.Bold
                    )

                    if (holiday != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HolidayInfoCard(holiday = holiday)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentTemplate != null) {
                        Text(
                            text = "Текущая смена",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ShiftPickerOptionCard(
                            template = currentTemplate,
                            selected = true,
                            onClick = {}
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (currentShiftCode != null) {
                        Text(
                            text = "Текущая смена: $currentShiftCode",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "Выбери шаблон",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    shiftTemplates.forEach { shiftTemplate ->
                        ShiftPickerOptionCard(
                            template = shiftTemplate,
                            selected = currentShiftCode == shiftTemplate.code,
                            onClick = { onSelectShiftCode(shiftTemplate.code) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearShift,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Очистить")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Готово")
                    }
                }
            }
        }
    }
}
