package com.vigilante.shiftsalaryplanner

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.widget.WidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.WidgetThemeMode
import com.vigilante.shiftsalaryplanner.widget.readWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.readWidgetThemeMode
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

@Composable
fun WidgetSettingsScreen(
    prefs: SharedPreferences,
    refreshToken: Int,
    shiftTemplates: List<ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    onBack: () -> Unit,
    onSaveThemeMode: (WidgetThemeMode) -> Unit,
    onSaveShiftOverride: (String, WidgetShiftOverride) -> Unit,
    onResetShiftOverride: (String) -> Unit
) {
    var selectedThemeMode by remember(refreshToken) { mutableStateOf(readWidgetThemeMode(prefs)) }
    val draftOverrides = remember(refreshToken) { mutableStateMapOf<String, WidgetShiftOverride>() }
    val dirtyOverrides = remember(refreshToken) { mutableStateMapOf<String, Boolean>() }
    var showUnsavedExitConfirm by rememberSaveable { mutableStateOf(false) }

    fun requestClose() {
        if (dirtyOverrides.values.any { it }) {
            showUnsavedExitConfirm = true
        } else {
            onBack()
        }
    }

    fun saveAllDrafts() {
        dirtyOverrides.forEach { (shiftCode, dirty) ->
            if (dirty) {
                draftOverrides[shiftCode]?.let { draft ->
                    onSaveShiftOverride(shiftCode, draft)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppScreenHeader(
            title = "Настройки виджета",
            onBack = { requestClose() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSectionCard(
                title = "Тема виджета",
                subtitle = "Авто повторяет тему устройства"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetThemeMode.entries.forEach { mode ->
                        PayModeChoiceCard(
                            title = widgetThemeModeLabel(mode),
                            subtitle = when (mode) {
                                WidgetThemeMode.AUTO -> "Подстраивается под тему устройства"
                                WidgetThemeMode.DARK -> "Всегда использовать тёмный виджет"
                                WidgetThemeMode.LIGHT -> "Всегда использовать светлый виджет"
                            },
                            selected = selectedThemeMode == mode,
                            onClick = {
                                selectedThemeMode = mode
                                onSaveThemeMode(mode)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSectionCard(
                title = "Смены в виджете",
                subtitle = "Пустые поля используют стандартные подписи шаблона"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    shiftTemplates.forEach { template ->
                        WidgetShiftSettingsCard(
                            template = template,
                            calendarColorInt = shiftColors[template.code] ?: parseColorHex(template.colorHex, 0xFF4A67C9.toInt()),
                            initialSettings = readWidgetShiftOverride(prefs, template.code),
                            refreshToken = refreshToken,
                            onSave = { override -> onSaveShiftOverride(template.code, override) },
                            onReset = { onResetShiftOverride(template.code) },
                            onDraftChanged = { draft, dirty ->
                                draftOverrides[template.code] = draft
                                dirtyOverrides[template.code] = dirty
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showUnsavedExitConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitConfirm = false },
            title = { Text("Сохранить изменения?") },
            text = { Text("В настройках виджета есть несохранённые изменения.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveAllDrafts()
                        showUnsavedExitConfirm = false
                        onBack()
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showUnsavedExitConfirm = false }) {
                        Text("Отмена")
                    }
                    TextButton(
                        onClick = {
                            showUnsavedExitConfirm = false
                            onBack()
                        }
                    ) {
                        Text("Не сохранять")
                    }
                }
            }
        )
    }
}
private fun widgetThemeModeLabel(mode: WidgetThemeMode): String {
    return when (mode) {
        WidgetThemeMode.AUTO -> "Авто"
        WidgetThemeMode.DARK -> "Тёмный"
        WidgetThemeMode.LIGHT -> "Светлый"
    }
}
