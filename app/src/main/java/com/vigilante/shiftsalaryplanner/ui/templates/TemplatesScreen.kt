package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DragIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesScreen(
    state: TemplatesScreenState,
    actions: TemplatesScreenActions
) {
    val mode = state.mode
    val templates = state.templates
    val systemStatusCodes = state.systemStatusCodes
    val specialRules = state.specialRules
    val patterns = state.patterns
    val workplaces = state.workplaces
    val activeWorkplaceId = state.activeWorkplaceId
    val shiftSwipeDuplicateEnabled = state.shiftSwipeDuplicateEnabled
    val shiftSwipeDeleteEnabled = state.shiftSwipeDeleteEnabled
    val onModeChange = actions.onModeChange
    val onBack = actions.onBack
    val onSwitchWorkplace = actions.onSwitchWorkplace
    val onOpenManageWorkplaces = actions.onOpenManageWorkplaces
    val onAddShift = actions.onAddShift
    val onAddSystemStatus = actions.onAddSystemStatus
    val onEditShift = actions.onEditShift
    val onDuplicateShift = actions.onDuplicateShift
    val onDeleteShift = actions.onDeleteShift
    val onAddPattern = actions.onAddPattern
    val onEditPattern = actions.onEditPattern
    val onApplyPattern = actions.onApplyPattern
    val onDeletePattern = actions.onDeletePattern
    val onReorderShifts = actions.onReorderShifts

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
    val systemTemplates = remember(templates, systemStatusCodes) {
        templates
            .filter { template ->
                isProtectedSystemTemplate(template) || isSystemStatusCode(template.code, systemStatusCodes)
            }
            .groupBy { template -> stripWorkplaceScopeFromShiftCode(template.code) }
            .values
            .map { group ->
                group.firstOrNull { template -> !isWorkplaceScopedShiftCode(template.code) } ?: group.first()
            }
            .sortedBy { it.sortOrder }
    }
    val regularTemplates = remember(templates, systemStatusCodes) {
        templates.filterNot { template ->
            isProtectedSystemTemplate(template) || isSystemStatusCode(template.code, systemStatusCodes)
        }.sortedBy { it.sortOrder }
    }
    var reorderedTemplates by remember { mutableStateOf(regularTemplates) }
    var draggedTemplateCode by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(regularTemplates.map { it.code to it.sortOrder }) {
        if (draggedTemplateCode == null) {
            reorderedTemplates = regularTemplates
        }
    }
    fun moveTemplate(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in reorderedTemplates.indices || toIndex !in reorderedTemplates.indices) {
            return
        }
        reorderedTemplates = reorderedTemplates.toMutableList().also { list ->
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
        }
    }
    val patternsForActiveWorkplace = remember(patterns, activeWorkplaceId) {
        patterns.filter { pattern -> patternBelongsToWorkplace(pattern, activeWorkplaceId) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.showSystemStatuses) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = appScreenPadding()),
                    verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
                ) {
                    item("system-status-topbar") {
                        Spacer(modifier = Modifier.height(appScreenPadding()))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BackCircleButton(
                                onClick = { dispatch(TemplatesScreenUiAction.SetShowSystemStatuses(false)) }
                            )

                            Text(
                                text = "Системные статусы",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )

                            FloatingActionButton(
                                onClick = onAddSystemStatus,
                                modifier = Modifier.size(appFabButtonSize())
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Добавить системный статус"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(appSectionSpacing()))
                    }

                    item("system-status-list") {
                        AppExpressiveSurface(
                            modifier = Modifier.fillMaxWidth(),
                            tone = AppExpressiveSurfaceTone.PANEL,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(appScaledSpacing(10.dp)),
                                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                            ) {
                            systemTemplates.forEach { template ->
                                key(template.code) {
                                    TemplateListItem(
                                        template = template,
                                        specialRule = specialRules[template.code],
                                        onClick = { onEditShift(template) },
                                        onDuplicate = { onDuplicateShift(template) },
                                        onDelete = if (isProtectedSystemTemplate(template)) null else {
                                            { dispatch(TemplatesScreenUiAction.SetPendingDeleteShiftCode(template.code)) }
                                        },
                                        swipeDuplicateEnabled = shiftSwipeDuplicateEnabled,
                                        swipeDeleteEnabled = shiftSwipeDeleteEnabled
                                    )
                                }
                            }
                            }
                        }
                    }

                    item("system-status-bottom-space") {
                        Spacer(modifier = Modifier.height(appScaledSpacing(118.dp)))
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
                                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1.35f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    CalendarWorkplaceSwitcher(
                                        workplaces = workplaces,
                                        activeWorkplaceId = activeWorkplaceId,
                                        onSwitchWorkplace = onSwitchWorkplace,
                                        onOpenManageWorkplaces = onOpenManageWorkplaces
                                    )
                                }
                                TemplateStatPill(
                                    label = "Смен",
                                    value = regularTemplates.size.toString(),
                                    compact = true,
                                    modifier = Modifier.weight(0.52f)
                                )
                                TemplateStatPill(
                                    label = "Циклов",
                                    value = patternsForActiveWorkplace.size.toString(),
                                    compact = true,
                                    modifier = Modifier.weight(0.52f)
                                )
                                TemplateStatPill(
                                    label = "Системных",
                                    value = systemTemplates.size.toString(),
                                    compact = true,
                                    modifier = Modifier.weight(0.52f)
                                )
                            }
                            Spacer(modifier = Modifier.height(appSectionSpacing()))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TemplateModeSwitcher(
                                    mode = mode,
                                    onModeChange = onModeChange,
                                    modifier = Modifier.fillMaxWidth(0.78f)
                                )
                            }
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
                                        AppExpressiveSurface(
                                            modifier = Modifier.fillMaxWidth(),
                                            tone = AppExpressiveSurfaceTone.PANEL,
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(appScaledSpacing(10.dp)),
                                                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                                            ) {
                                            reorderedTemplates.forEachIndexed { index, template ->
                                                key(template.code) {
                                                    ReorderableTemplateListItem(
                                                        template = template,
                                                        specialRule = specialRules[template.code],
                                                        index = index,
                                                        totalCount = reorderedTemplates.size,
                                                        isDragging = draggedTemplateCode == template.code,
                                                        onDragStart = { draggedTemplateCode = template.code },
                                                        onMove = { from, to -> moveTemplate(from, to) },
                                                        onDragEnd = {
                                                            draggedTemplateCode = null
                                                            onReorderShifts(reorderedTemplates)
                                                        },
                                                        onClick = { onEditShift(template) },
                                                        onDuplicate = { onDuplicateShift(template) },
                                                        onDelete = {
                                                            dispatch(TemplatesScreenUiAction.SetPendingDeleteShiftCode(template.code))
                                                        },
                                                        swipeDuplicateEnabled = shiftSwipeDuplicateEnabled,
                                                        swipeDeleteEnabled = shiftSwipeDeleteEnabled
                                                    )
                                                }
                                            }
                                            }
                                        }
                                    }
                                }
                                item("shift-system-entry") {
                                    Spacer(modifier = Modifier.height(appSectionSpacing()))
                                    AppExpressiveSurface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { dispatch(TemplatesScreenUiAction.SetShowSystemStatuses(true)) },
                                        tone = AppExpressiveSurfaceTone.SOFT,
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
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
                                                    text = if (systemTemplates.isEmpty()) {
                                                        "Нет пользовательских статусов. Можно добавить."
                                                    } else {
                                                        "Встроенные и пользовательские статусы"
                                                    },
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
                                    AppExpressiveSurface(
                                        modifier = Modifier.fillMaxWidth(),
                                        tone = AppExpressiveSurfaceTone.PANEL,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(appScaledSpacing(10.dp)),
                                            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                                        ) {
                                        if (patternsForActiveWorkplace.isEmpty()) {
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
                                            patternsForActiveWorkplace.forEach { pattern ->
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
            shape = RoundedCornerShape(appCornerRadius(28.dp)),
            containerColor = appPanelColor(),
            tonalElevation = 0.dp,
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
            shape = RoundedCornerShape(appCornerRadius(28.dp)),
            containerColor = appPanelColor(),
            tonalElevation = 0.dp,
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
private fun ReorderableTemplateListItem(
    template: ShiftTemplateEntity,
    specialRule: ShiftSpecialRule?,
    index: Int,
    totalCount: Int,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    swipeDuplicateEnabled: Boolean,
    swipeDeleteEnabled: Boolean
) {
    var dragRemainder by remember(template.code) { mutableStateOf(0f) }
    val rowStepPx = with(androidx.compose.ui.platform.LocalDensity.current) { appScaledSpacing(76.dp).toPx() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (isDragging) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
            },
            border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "Перетащить",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(appScaledSpacing(34.dp))
                    .padding(appScaledSpacing(7.dp))
                    .pointerInput(template.code, index, totalCount) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragRemainder = 0f
                                onDragStart()
                            },
                            onDragEnd = {
                                dragRemainder = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                dragRemainder = 0f
                                onDragEnd()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragRemainder += dragAmount.y
                                val target = when {
                                    dragRemainder > rowStepPx * 0.55f -> index + 1
                                    dragRemainder < -rowStepPx * 0.55f -> index - 1
                                    else -> null
                                }?.coerceIn(0, totalCount - 1)
                                if (target != null && target != index) {
                                    onMove(index, target)
                                    dragRemainder = 0f
                                }
                            }
                        )
                    }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            TemplateListItem(
                template = template,
                specialRule = specialRule,
                onClick = onClick,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
                swipeDuplicateEnabled = swipeDuplicateEnabled,
                swipeDeleteEnabled = swipeDeleteEnabled
            )
        }
    }
}

private fun patternBelongsToWorkplace(
    pattern: PatternTemplate,
    workplaceId: String
): Boolean {
    val nonBlankSteps = pattern.normalizedSteps()
        .take(pattern.usedLength())
        .filter { it.isNotBlank() }
    if (nonBlankSteps.isEmpty()) {
        return workplaceId == WORKPLACE_MAIN_ID
    }
    val scopedWorkplaces = nonBlankSteps
        .filter { isWorkplaceScopedShiftCode(it) }
        .map(::workplaceIdFromShiftCode)
        .toSet()
    return if (scopedWorkplaces.isEmpty()) {
        workplaceId == WORKPLACE_MAIN_ID
    } else {
        workplaceId in scopedWorkplaces
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

