package com.vigilante.shiftsalaryplanner

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate

@Composable
fun TemplatesScreen(
    state: TemplatesScreenState,
    actions: TemplatesScreenActions
) {
    TemplatesScreen(
        mode = state.mode,
        templates = state.templates,
        specialRules = state.specialRules,
        patterns = state.patterns,
        onModeChange = actions.onModeChange,
        onBack = actions.onBack,
        onAddShift = actions.onAddShift,
        onEditShift = actions.onEditShift,
        onAddPattern = actions.onAddPattern,
        onEditPattern = actions.onEditPattern,
        onApplyPattern = actions.onApplyPattern,
        onDeletePattern = actions.onDeletePattern
    )
}

@Composable
fun TemplatesScreen(
    mode: TemplateMode,
    templates: List<ShiftTemplateEntity>,
    specialRules: Map<String, ShiftSpecialRule>,
    patterns: List<PatternTemplate>,
    onModeChange: (TemplateMode) -> Unit,
    onBack: () -> Unit,
    onAddShift: () -> Unit,
    onEditShift: (ShiftTemplateEntity) -> Unit,
    onAddPattern: () -> Unit,
    onEditPattern: (PatternTemplate) -> Unit,
    onApplyPattern: (PatternTemplate) -> Unit,
    onDeletePattern: (PatternTemplate) -> Unit
) {
    var uiState by rememberSaveable { mutableStateOf(TemplatesScreenUiState()) }
    val dispatch: (TemplatesScreenUiAction) -> Unit = { action ->
        uiState = reduceTemplatesScreenUiState(uiState, action)
    }

    val pendingDeletePattern = remember(patterns, uiState.pendingDeletePatternId) {
        patterns.firstOrNull { it.id == uiState.pendingDeletePatternId }
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
                    title = "РЎРёСЃС‚РµРјРЅС‹Рµ СЃС‚Р°С‚СѓСЃС‹",
                    onBack = { dispatch(TemplatesScreenUiAction.SetShowSystemStatuses(false)) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        systemTemplates.forEachIndexed { index, template ->
                            TemplateListItem(
                                template = template,
                                specialRule = specialRules[template.code],
                                onClick = { onEditShift(template) }
                            )

                            if (index != systemTemplates.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            }
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
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackCircleButton(onClick = onBack)

                        Text(
                            text = "РЎРјРµРЅС‹",
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text("+")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TemplateStatPill(
                            label = "РЎРјРµРЅ",
                            value = regularTemplates.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        TemplateStatPill(
                            label = "Р¦РёРєР»РѕРІ",
                            value = patterns.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        TemplateStatPill(
                            label = "РЎРёСЃС‚РµРјРЅС‹С…",
                            value = systemTemplates.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TemplateModeSwitcher(
                        mode = mode,
                        onModeChange = onModeChange
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (mode) {
                        TemplateMode.SHIFTS -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (regularTemplates.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(20.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        regularTemplates.forEachIndexed { index, template ->
                                            TemplateListItem(
                                                template = template,
                                                specialRule = specialRules[template.code],
                                                onClick = { onEditShift(template) }
                                            )

                                            if (index != regularTemplates.lastIndex) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                            }
                                        }
                                    }
                                }

                                if (systemTemplates.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(18.dp))
                                            .clickable { dispatch(TemplatesScreenUiAction.SetShowSystemStatuses(true)) }
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "РЎРёСЃС‚РµРјРЅС‹Рµ СЃС‚Р°С‚СѓСЃС‹",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Р’С‹С…РѕРґРЅРѕР№, РћС‚РїСѓСЃРє, Р‘РѕР»СЊРЅРёС‡РЅС‹Р№",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        Text(
                                            text = "вЂє",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }
                        }

                        TemplateMode.CYCLES -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, appPanelBorderColor(), RoundedCornerShape(20.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (patterns.isEmpty()) {
                                    Text(
                                        text = "РџРѕРєР° РЅРµС‚ РЅРё РѕРґРЅРѕРіРѕ С‡РµСЂРµРґРѕРІР°РЅРёСЏ.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedButton(onClick = onAddPattern) {
                                        Text("РЎРѕР·РґР°С‚СЊ С‡РµСЂРµРґРѕРІР°РЅРёРµ")
                                    }
                                } else {
                                    patterns.forEachIndexed { index, pattern ->
                                        PatternListItem(
                                            pattern = pattern,
                                            onEdit = { onEditPattern(pattern) },
                                            onApply = { onApplyPattern(pattern) },
                                            onDelete = {
                                                dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(pattern.id))
                                            }
                                        )

                                        if (index != patterns.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
    if (pendingDeletePattern != null) {
        AlertDialog(
            onDismissRequest = { dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(null)) },
            title = { Text("РЈРґР°Р»РёС‚СЊ С‡РµСЂРµРґРѕРІР°РЅРёРµ?") },
            text = {
                Column {
                    Text(
                        text = pendingDeletePattern.name.ifBlank { "Р‘РµР· РЅР°Р·РІР°РЅРёСЏ" },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Р“СЂР°С„РёРє Р±СѓРґРµС‚ СѓРґР°Р»С‘РЅ Р±РµР· РІРѕР·РјРѕР¶РЅРѕСЃС‚Рё РІРѕСЃСЃС‚Р°РЅРѕРІР»РµРЅРёСЏ.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePattern(pendingDeletePattern)
                        dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(null))
                    }
                ) {
                    Text("РЈРґР°Р»РёС‚СЊ")
                }
            },
            dismissButton = {
                TextButton(onClick = { dispatch(TemplatesScreenUiAction.SetPendingDeletePatternId(null)) }) {
                    Text("РћС‚РјРµРЅР°")
                }
            }
        )
    }
}
