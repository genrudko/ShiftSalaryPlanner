package com.vigilante.shiftsalaryplanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.excel.EmptyDayImportMode
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelImportRequest
import com.vigilante.shiftsalaryplanner.excel.ExcelImportScopeType
import com.vigilante.shiftsalaryplanner.excel.ExcelPersonCandidate
import java.time.LocalDate

@Composable
fun ExcelImportScreen(
    fileName: String?,
    preview: ExcelImportPreview?,
    candidates: List<ExcelPersonCandidate>,
    statusMessage: String?,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onAnalyze: (ExcelImportRequest, String?) -> Unit,
    onImport: (ExcelImportPreview) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var yearText by rememberSaveable { mutableStateOf(LocalDate.now().year.toString()) }
    var surnameText by rememberSaveable { mutableStateOf("") }
    var selectedFullName by rememberSaveable { mutableStateOf<String?>(null) }
    var scopeType by rememberSaveable { mutableStateOf(ExcelImportScopeType.FULL_YEAR.name) }
    var singleMonthText by rememberSaveable { mutableStateOf(LocalDate.now().monthValue.toString()) }
    var rangeStartText by rememberSaveable { mutableStateOf("1") }
    var rangeEndText by rememberSaveable { mutableStateOf("12") }
    var selectedMonthsText by rememberSaveable { mutableStateOf("") }
    var fillEmptyAsDayOff by rememberSaveable { mutableStateOf(false) }

    val resolvedScopeType = runCatching { ExcelImportScopeType.valueOf(scopeType) }.getOrElse { ExcelImportScopeType.FULL_YEAR }
    BackHandler(onBack = onBack)
    fun buildRequest(): ExcelImportRequest {
        val year = yearText.toIntOrNull() ?: throw IllegalStateException("Укажи корректный год")
        val surname = surnameText.trim()
        if (surname.isBlank()) throw IllegalStateException("Укажи фамилию")
        val selectedMonths = selectedMonthsText
            .split(',', ';', ' ')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..12 }
            .toSet()
        return ExcelImportRequest(
            year = year,
            surnameQuery = surname,
            selectedFullName = selectedFullName,
            scopeType = resolvedScopeType,
            singleMonth = singleMonthText.toIntOrNull(),
            rangeStartMonth = rangeStartText.toIntOrNull(),
            rangeEndMonth = rangeEndText.toIntOrNull(),
            selectedMonths = selectedMonths,
            emptyDayImportMode = if (fillEmptyAsDayOff) EmptyDayImportMode.FILL_AS_DAY_OFF else EmptyDayImportMode.SKIP_EMPTY
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(appScreenPadding())
        ) {
            FixedScreenHeader(
                title = "Импорт графика",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(appSectionSpacing()))

            Text(
                text = "Поиск по фамилии, выбор периода и полная перезапись выбранных месяцев",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(appScaledSpacing(16.dp)))

            SettingsNavigationCard(
                title = "Excel-файл",
                subtitle = fileName ?: "Файл пока не выбран",
                onClick = onPickFile
            )

            Spacer(modifier = Modifier.height(appSectionSpacing()))

            OutlinedTextField(
                value = yearText,
                onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                label = { Text("Год импорта") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(appSectionSpacing()))

            OutlinedTextField(
                value = surnameText,
                onValueChange = { surnameText = it },
                label = { Text("Фамилия") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(appSectionSpacing()))

            Text(
                text = "Период импорта",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(appBlockSpacing()))
            Column(verticalArrangement = Arrangement.spacedBy(appBlockSpacing())) {
                ScopeTypeOptionRow(
                    selected = resolvedScopeType == ExcelImportScopeType.FULL_YEAR,
                    title = "Весь год",
                    onClick = { scopeType = ExcelImportScopeType.FULL_YEAR.name }
                )
                ScopeTypeOptionRow(
                    selected = resolvedScopeType == ExcelImportScopeType.SINGLE_MONTH,
                    title = "Один месяц",
                    onClick = { scopeType = ExcelImportScopeType.SINGLE_MONTH.name }
                )
                if (resolvedScopeType == ExcelImportScopeType.SINGLE_MONTH) {
                    OutlinedTextField(
                        value = singleMonthText,
                        onValueChange = { singleMonthText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Месяц (1-12)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ScopeTypeOptionRow(
                    selected = resolvedScopeType == ExcelImportScopeType.MONTH_RANGE,
                    title = "Диапазон месяцев",
                    onClick = { scopeType = ExcelImportScopeType.MONTH_RANGE.name }
                )
                if (resolvedScopeType == ExcelImportScopeType.MONTH_RANGE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = rangeStartText,
                            onValueChange = { rangeStartText = it.filter(Char::isDigit).take(2) },
                            label = { Text("С") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rangeEndText,
                            onValueChange = { rangeEndText = it.filter(Char::isDigit).take(2) },
                            label = { Text("По") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                ScopeTypeOptionRow(
                    selected = resolvedScopeType == ExcelImportScopeType.SELECTED_MONTHS,
                    title = "Выбранные месяцы",
                    onClick = { scopeType = ExcelImportScopeType.SELECTED_MONTHS.name }
                )
                if (resolvedScopeType == ExcelImportScopeType.SELECTED_MONTHS) {
                    OutlinedTextField(
                        value = selectedMonthsText,
                        onValueChange = { selectedMonthsText = it },
                        label = { Text("Например: 1,3,5,12") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(appSectionSpacing()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Пустые дни заполнять как ВЫХ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Если выключено — пустые ячейки не импортируются",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = fillEmptyAsDayOff,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        fillEmptyAsDayOff = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(16.dp)))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    runCatching { buildRequest() }
                        .onSuccess { request -> onAnalyze(request, selectedFullName) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Проанализировать файл")
            }

            if (candidates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(appScaledSpacing(16.dp)))
                Text(
                    text = "Найдено несколько сотрудников",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(appBlockSpacing()))
                candidates.forEach { candidate ->
                    SettingsNavigationCard(
                        title = candidate.fullName,
                        subtitle = if (selectedFullName == candidate.fullName) "Выбрано" else "Нажми, чтобы выбрать и повторно проанализировать",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFullName = candidate.fullName
                            runCatching { buildRequest() }
                                .onSuccess { request -> onAnalyze(request, candidate.fullName) }
                        }
                    )
                    Spacer(modifier = Modifier.height(appBlockSpacing()))
                }
            }

            preview?.let { readyPreview ->
                Spacer(modifier = Modifier.height(appScaledSpacing(16.dp)))
                Text(
                    text = "Предпросмотр импорта",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(appBlockSpacing()))
                InfoLine(label = "Сотрудник", value = readyPreview.fullName)
                InfoLine(label = "Год", value = readyPreview.year.toString())
                InfoLine(label = "Месяцы", value = readyPreview.selectedMonths.joinToString())
                InfoLine(label = "Дней к импорту", value = readyPreview.importedDays.size.toString())
                InfoLine(label = "Новых шаблонов", value = readyPreview.templatesToCreate.size.toString())

                if (readyPreview.templatesToCreate.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(appBlockSpacing()))
                    Text(
                        text = "Будут созданы шаблоны: ${readyPreview.templatesToCreate.joinToString { it.code }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(appSectionSpacing()))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onImport(readyPreview)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Импортировать с очисткой и перезаписью")
                }
            }

            statusMessage?.let { status ->
                Spacer(modifier = Modifier.height(appScaledSpacing(16.dp)))
                AppFeedbackCard(
                    message = status,
                    state = inferAppFeedbackState(status),
                    title = "Статус импорта"
                )
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(20.dp)))
        }
    }
}
@Composable
private fun ScopeTypeOptionRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = appHapticAction(onAction = onClick))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
