package com.vigilante.shiftsalaryplanner

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
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
                .padding(14.dp)
        ) {
            WidgetSectionTitle("Тема")

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Тема виджета",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Режим применяется сразу",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WidgetThemeMode.entries.forEach { mode ->
                            WidgetThemeModeChip(
                                mode = mode,
                                selected = selectedThemeMode == mode,
                                onClick = {
                                    selectedThemeMode = mode
                                    onSaveThemeMode(mode)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            WidgetSectionTitle("Смены")

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Смены в виджете",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Подписи и цвет можно настроить отдельно для каждой смены",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    WidgetInfoPill(
                        text = if (dirtyOverrides.values.any { it }) {
                            "Есть несохранённые изменения"
                        } else {
                            "Карточки можно раскрывать по одной"
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shiftTemplates.forEach { template ->
                            WidgetShiftSettingsCard(
                                template = template,
                                calendarColorInt = shiftColors[template.code]
                                    ?: parseColorHex(template.colorHex, 0xFF4A67C9.toInt()),
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
            }

            Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun WidgetSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WidgetThemeModeChip(
    mode: WidgetThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        appPanelBorderColor()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp)
        ) {
            Text(
                text = widgetThemeModeLabel(mode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (mode) {
                    WidgetThemeMode.AUTO -> "Авто"
                    WidgetThemeMode.DARK -> "Тёмный"
                    WidgetThemeMode.LIGHT -> "Светлый"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WidgetInfoPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
