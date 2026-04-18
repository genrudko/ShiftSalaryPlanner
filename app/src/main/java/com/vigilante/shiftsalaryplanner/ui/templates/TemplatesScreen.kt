package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesScreen(
    state: TemplatesScreenState,
    actions: TemplatesScreenActions
) {
    val mode = state.mode
    val templates = state.templates
    val specialRules = state.specialRules
    val patterns = state.patterns
    val onModeChange = actions.onModeChange
    val onBack = actions.onBack
    val onAddShift = actions.onAddShift
    val onEditShift = actions.onEditShift
    val onDuplicateShift = actions.onDuplicateShift
    val onDeleteShift = actions.onDeleteShift
    val onAddPattern = actions.onAddPattern
    val onEditPattern = actions.onEditPattern
    val onApplyPattern = actions.onApplyPattern
    val onDeletePattern = actions.onDeletePattern

    var uiState by remember { mutableStateOf(TemplatesScreenUiState()) }
    val dispatch: (TemplatesScreenUiAction) -> Unit = { action ->
        uiState = reduceTemplatesScreenUiState(uiState, action)
    }

    val pendingDeletePattern = remember(patterns, uiState.pendingDeletePatternId) {
        patterns.firstOrNull { it.id == uiState.pendingDeletePatternId }
    }
    val pendingDeleteShift = remember(templates, uiState.pendingDeleteShiftCode) {
        templates.firstOrNull { it.code == uiState.pendingDeleteShiftCode }
    }
    val systemTemplates = remember(templates) {
        templates.filter { isProtectedSystemTemplate(it) }.sortedBy { it.sortOrder }
    }
    val regularTemplates = remember(templates) {
        templates.filterNot { isProtectedSystemTemplate(it) }.sortedBy { it.sortOrder }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.showSystemStatuses) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                CompactScreenHeader(
                    title = "Системные статусы",
                    onBack = { dispatch(TemplatesScreenUiAction.SetShowSystemStatuses(false)) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(appCardPadding())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(appBubbleBackgroundColor(defaultAlpha = 0.42f))
                            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(20.dp))
                            .padding(appScaledSpacing(10.dp)),
                        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        systemTemplates.forEach { template ->
                            TemplateListItem(
                                template = template,
                                specialRule = specialRules[template.code],
                                onClick = { onEditShift(template) },
                                onDuplicate = { onDuplicateShift(template) },
                                onDelete = null
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = appScreenPadding()),
                        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
                    ) {
                        item("templates-topbar") {
                            Spacer(modifier = Modifier.height(appScreenPadding()))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BackCircleButton(onClick = onBack)

                                Text(
                                    text = "Смены",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                )

                                FloatingActionButton(
                                    onClick = {
                                        if (mode == TemplateMode.SHIFTS) onAddShift() else onAddPattern()
                                    },
                                    modifier = Modifier.size(appFabButtonSize())
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Добавить"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(appSectionSpacing()))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
                            ) {
                                TemplateStatPill(
                                    label = "Смен",
                                    value = regularTemplates.size.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                TemplateStatPill(
                                    label = "Циклов",
                                    value = patterns.size.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                TemplateStatPill(
                                    label = "Системных",
                                    value = systemTemplates.size.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(appSectionSpacing()))
                            TemplateModeSwitcher(
                                mode = mode,
                                onModeChange = onModeChange
                            )
                            Spacer(modifier = Modifier.height(appSectionSpacing()))
                        }

                        when (mode) {
                            TemplateMode.SHIFTS -> {
                                stickyHeader("shift-section-header") {
                                    TemplatesStickyHeader("Шаблоны смен")
                                }
                                item("shift-regular-list") {
                                    if (regularTemplates.isEmpty()) {
                                        AppEmptyCard(
                                            title = "Смен пока нет",
                                            message = "Добавь первую смену или продублируй существующую."
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(appBubbleBackgroundColor(defaultAlpha = 0.28f))
                                                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(20.dp))
                                                .padding(appScaledSpacing(10.dp)),
                                            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                                        ) {
                                            regularTemplates.forEach { template ->
                                                TemplateListItem(
                                                    template = template,
                                                    specialRule = specialRules[template.code],
                                                    onClick = { onEditShift(template) },
                                                    onDuplicate = { onDuplicateShift(template) },
                                                    onDelete = {
                                                        dispatch(TemplatesScreenUiAction.SetPendingDeleteShiftCode(template.code))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                item("shift-system-entry") {
                                    if (systemTemplates.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(appSectionSpacing()))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(appBubbleBackgroundColor(defaultAlpha = 0.28f))
                                                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(18.dp))
                                                .clickable { dispatch(TemplatesScreenUiAction.SetShowSystemStatuses(true)) }
                                                .padding(horizontal = 14.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Системные статусы",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "Выходной, Отпуск, Больничный",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = appListSecondaryTextColor()
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            TemplateMode.CYCLES -> {
                                stickyHeader("cycle-section-header") {
                                    TemplatesStickyHeader("Чередования")
                                }
                                item("cycle-list") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(appBubbleBackgroundColor(defaultAlpha = 0.28f))
                                            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(20.dp))
                                            .padding(appScaledSpacing(10.dp)),
                                        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                                    ) {
                                        if (patterns.isEmpty()) {
                                            AppEmptyCard(
                                                title = "Пока пусто",
                                                message = "Создай первое чередование, чтобы быстро применять графики."
                                            )
                                            Spacer(modifier = Modifier.height(appSectionSpacing()))
                                            OutlinedButton(
                                                onClick = onAddPattern,
                                                modifier = Modifier.appLargeButtonSizing()
                                            ) {
                                                Text("Создать чередование")
                                            }
                                        } else {
                                            patterns.forEach { pattern ->
                                                PatternListItem(
                                                    pattern = pattern,
                                                    onEdit = { onEditPattern(pattern) },
                                                    onApply = { onApplyPattern(pattern) },
                                                    onDelete = {
                                                        dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(pattern.id))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item("templates-bottom-space") {
                            Spacer(modifier = Modifier.height(appScaledSpacing(118.dp)))
                        }
                    }
                }
            }
        }
    }
    if (pendingDeleteShift != null) {
        AlertDialog(
            onDismissRequest = { dispatch(TemplatesScreenUiAction.SetPendingDeleteShiftCode(null)) },
            title = { Text("Удалить смену?") },
            text = {
                Column {
                    Text(
                        text = "${pendingDeleteShift.code} — ${pendingDeleteShift.title}",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Связанные отметки в календаре тоже будут очищены.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteShift(pendingDeleteShift)
                        dispatch(TemplatesScreenUiAction.SetPendingDeleteShiftCode(null))
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { dispatch(TemplatesScreenUiAction.SetPendingDeleteShiftCode(null)) }) {
                    Text("Отмена")
                }
            }
        )
    }
    if (pendingDeletePattern != null) {
        AlertDialog(
            onDismissRequest = { dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(null)) },
            title = { Text("Удалить чередование?") },
            text = {
                Column {
                    Text(
                        text = pendingDeletePattern.name.ifBlank { "Без названия" },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("График будет удалён без возможности восстановления.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePattern(pendingDeletePattern)
                        dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(null))
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(null)) }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TemplatesStickyHeader(
    title: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.985f)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = appListSecondaryTextColor(),
            modifier = Modifier.padding(top = appScaledSpacing(6.dp), bottom = appScaledSpacing(2.dp))
        )
    }
}

