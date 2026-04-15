package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import java.time.LocalDate

@Composable
fun ManualHolidaysScreen(
    records: List<ManualHolidayRecord>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ManualHolidayRecord) -> Unit,
    onDelete: (ManualHolidayRecord) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeaderAction(
                title = "Ручные праздники",
                onBack = onBack,
                actionText = "Добавить",
                onAction = onAdd
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "Локальные дни",
                    subtitle = "Можно добавить региональные праздники и особые дни поверх загруженного календаря"
                ) {
                    if (records.isEmpty()) {
                        Text(
                            text = "Пока нет ручных праздничных дней.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        records.forEachIndexed { index, record ->
                            ManualHolidayRow(
                                record = record,
                                onEdit = { onEdit(record) },
                                onDelete = { onDelete(record) }
                            )

                            if (index != records.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ManualHolidayRow(
    record: ManualHolidayRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeLabel = when {
        record.kind == HolidayKinds.SHORT_DAY -> "Сокращённый день"
        record.isNonWorking -> "Нерабочий праздник"
        else -> "Особый день"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDate(LocalDate.parse(record.date)),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = record.title.ifBlank { typeLabel },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row {
            TextButton(onClick = onEdit) {
                Text("Изм.")
            }
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}
