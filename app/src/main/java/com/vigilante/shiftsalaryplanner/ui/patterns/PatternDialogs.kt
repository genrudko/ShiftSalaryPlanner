package com.vigilante.shiftsalaryplanner

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

@Composable
fun PatternListDialog(
    patterns: List<PatternTemplate>,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (PatternTemplate) -> Unit,
    onApply: (PatternTemplate) -> Unit,
    onDelete: (PatternTemplate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Чередования") },
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
                        Text("Новый график")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (patterns.isEmpty()) {
                    Text("Пока нет ни одного чередования.")
                } else {
                    patterns.forEach { pattern ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onEdit(pattern) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = pattern.name.ifBlank { "Без названия" },
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Дней в цикле: ${pattern.usedLength()}")
                            Text(
                                text = pattern.previewText().ifBlank { "Пустой график" },
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onApply(pattern) }) {
                                    Text("Применить")
                                }
                                TextButton(onClick = { onEdit(pattern) }) {
                                    Text("Изменить")
                                }
                                TextButton(onClick = { onDelete(pattern) }) {
                                    Text("Удалить")
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

@Composable
fun PatternEditDialog(
    currentPattern: PatternTemplate?,
    shiftTemplates: List<ShiftTemplateEntity>,
    onDismiss: () -> Unit,
    onSave: (PatternTemplate) -> Unit
) {
    var nameText by rememberSaveable {
        mutableStateOf(currentPattern?.name ?: "")
    }

    var selectedBrushCode by rememberSaveable {
        mutableStateOf(shiftTemplates.firstOrNull()?.code ?: BRUSH_CLEAR)
    }

    val initialSteps = remember(currentPattern?.id) {
        currentPattern?.normalizedSteps() ?: List(35) { "" }
    }

    val steps = remember(currentPattern?.id) {
        mutableStateListOf<String>().apply {
            addAll(initialSteps)
        }
    }

    val usedLength = steps.indexOfLast { it.isNotBlank() }
        .let { if (it == -1) 0 else it + 1 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
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
                        text = if (currentPattern == null) "Новое чередование" else "Редактировать чередование",
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
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Название") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )

                    Text(
                        text = "Дней в цикле: $usedLength",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PatternQuickActionsRow(
                        onClearAll = {
                            for (i in steps.indices) {
                                steps[i] = ""
                            }
                        },
                        onTrimTail = {
                            val trimmed = trimTrailingBlankSteps(steps.toList(), minSize = 35)
                            for (i in steps.indices) {
                                steps[i] = trimmed.getOrElse(i) { "" }
                            }
                        },
                        onShiftLeft = {
                            val shifted = shiftStepsLeft(steps.toList())
                            for (i in steps.indices) {
                                steps[i] = shifted[i]
                            }
                        },
                        onShiftRight = {
                            val shifted = shiftStepsRight(steps.toList())
                            for (i in steps.indices) {
                                steps[i] = shifted[i]
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Сетка цикла",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Нажимай по ячейкам, чтобы расставлять выбранную смену или очищать дни.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PatternGrid(
                        steps = steps,
                        selectedBrushCode = selectedBrushCode,
                        shiftTemplates = shiftTemplates,
                        onSetStep = { index, value ->
                            steps[index] = value
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Выбор смены",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PatternBrushGrid(
                        selectedBrushCode = selectedBrushCode,
                        shiftTemplates = shiftTemplates,
                        onSelect = { selectedBrushCode = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            val finalName = nameText.trim().ifBlank {
                                "График ${if (usedLength > 0) usedLength else 1}"
                            }

                            onSave(
                                PatternTemplate(
                                    id = currentPattern?.id ?: UUID.randomUUID().toString(),
                                    name = finalName,
                                    steps = steps.toList()
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}


@Composable
fun PatternApplyDialog(
    currentPattern: PatternTemplate,
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onApply: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    var cycleStartDate by rememberSaveable {
        mutableStateOf(currentMonth.atDay(1).toString())
    }

    val cycleStartLocalDate = remember(cycleStartDate) {
        LocalDate.parse(cycleStartDate)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.62f),
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
                        text = "Применить чередование",
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
                        text = currentPattern.name.ifBlank { "Без названия" },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Дней в цикле: ${currentPattern.usedLength()}")
                    Text(
                        text = currentPattern.previewText().ifBlank {
                            "Пустой график"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Открытый месяц",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentMonth.atDay(1)
                            .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru-RU")))
                            .replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("ru-RU")) else it.toString()
                            }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Дата 1-го дня цикла",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    cycleStartDate = LocalDate.of(year, month + 1, day).toString()
                                },
                                cycleStartLocalDate.year,
                                cycleStartLocalDate.monthValue - 1,
                                cycleStartLocalDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(formatDate(cycleStartLocalDate))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Будут заполнены все дни открытого месяца. Пустые шаги цикла очистят день.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = { onApply(cycleStartLocalDate) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Применить")
                    }
                }
            }
        }
    }
}
@Composable
fun PatternQuickPickerDialog(
    patterns: List<PatternTemplate>,
    onDismiss: () -> Unit,
    onSelect: (PatternTemplate) -> Unit,
    onOpenManager: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор чередования") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (patterns.isEmpty()) {
                    Text("Нет сохранённых чередований.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onOpenManager) {
                        Text("Открыть редактор")
                    }
                } else {
                    patterns.forEach { pattern ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onSelect(pattern) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pattern.name.ifBlank { "Без названия" },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pattern.previewText().ifBlank {
                                        "Пустой график"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            TextButton(onClick = { onSelect(pattern) }) {
                                Text("Выбрать")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onOpenManager) {
                            Text("Редактор")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun PatternApplyPreviewDialog(
    currentPattern: PatternTemplate,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    var phaseOffsetText by rememberSaveable(
        currentPattern.id,
        rangeStart.toString(),
        rangeEnd.toString()
    ) {
        mutableStateOf("0")
    }

    val phaseOffset = phaseOffsetText.toIntOrNull() ?: 0

    val previewRows = remember(currentPattern, rangeStart, rangeEnd, phaseOffset) {
        buildPatternPreviewRows(
            pattern = currentPattern,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            phaseOffset = phaseOffset,
            maxItems = 12
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f),
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
                        text = "Предпросмотр чередования",
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
                        text = currentPattern.name.ifBlank { "Без названия" },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Диапазон: ${formatDate(rangeStart)} — ${formatDate(rangeEnd)}")
                    Text("Дней: ${ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1}")

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phaseOffsetText,
                        onValueChange = { newValue ->
                            phaseOffsetText = newValue.filterIndexed { index, ch ->
                                ch.isDigit() || (index == 0 && ch == '-')
                            }
                        },
                        label = { Text("Смещение фазы") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "0 = с первого шага, 1 = со второго, -1 = шаг назад",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Предпросмотр",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    previewRows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(formatDate(row.first))

                            Text(
                                text = row.second.ifBlank { "Очистить" },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1 > previewRows.size) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = { onApply(phaseOffset) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Text("Применить")
                    }
                }
            }
        }
    }
}
