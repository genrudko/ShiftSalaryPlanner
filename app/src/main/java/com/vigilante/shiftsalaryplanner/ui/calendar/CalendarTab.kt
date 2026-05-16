package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppProfile
import com.vigilante.shiftsalaryplanner.settings.Workplace
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDayAssignment(
    val workplaceId: String,
    val shiftCode: String
)

const val CALENDAR_WORKPLACE_ALL_ID = "__all_workplaces__"

@Composable
fun CalendarTab(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit,
    profiles: List<AppProfile>,
    activeProfileId: String,
    onSwitchProfile: (String) -> Unit,
    onOpenProfiles: () -> Unit,
    workplaces: List<Workplace>,
    calendarWorkplaceFilterId: String,
    onSwitchCalendarWorkplaceFilter: (String) -> Unit,
    activeWorkplaceId: String,
    onOpenManageWorkplaces: () -> Unit,
    holidayMap: Map<LocalDate, HolidayEntity>,
    shiftCodesByDate: Map<LocalDate, String>,
    dayAssignmentsByDate: Map<LocalDate, List<CalendarDayAssignment>>,
    noteDates: Set<LocalDate>,
    todayNotes: List<AppNote>,
    onAddTodayNote: () -> Unit,
    onEditNote: (String) -> Unit,
    monthAudit: CalendarMonthAudit,
    monthHistoryItems: List<String>,
    onOpenMonthCheck: () -> Unit,
    templateMap: Map<String, ShiftTemplateEntity>,
    legendShiftTemplates: List<ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    quickShiftTemplates: List<ShiftTemplateEntity>,
    systemStatusCodes: Set<String>,
    quickPickerOpen: Boolean,
    activeBrushCode: String?,
    isLegendExpanded: Boolean,
    onToggleLegend: () -> Unit,
    onOpenColorSettings: () -> Unit,
    onToggleQuickPicker: () -> Unit,
    onCloseQuickPicker: () -> Unit,
    onSelectBrush: (String) -> Unit,
    onClearBrush: () -> Unit,
    onDisableBrush: () -> Unit,
    onAddNewShift: () -> Unit,
    pendingPatternRangeStartDate: LocalDate?,
    pendingPatternRangeEndDate: LocalDate?,
    onOpenPatternPreview: () -> Unit,
    activePattern: PatternTemplate?,
    patternRangeStartDate: LocalDate?,
    onCancelPatternMode: () -> Unit,
    clearRangeModeActive: Boolean,
    clearRangeStartDate: LocalDate?,
    pendingClearRangeStartDate: LocalDate?,
    pendingClearRangeEndDate: LocalDate?,
    onConfirmClearRange: () -> Unit,
    onCancelClearRangeMode: () -> Unit,
    onClearCurrentMonth: () -> Unit,
    onStartRangeClearMode: () -> Unit,
    onClearAllCalendar: () -> Unit,
    showQuickEraser: Boolean = true,
    showQuickNormal: Boolean = true,
    showQuickCycle: Boolean = true,
    showQuickNewTemplate: Boolean = true,
    showQuickClearMonth: Boolean = true,
    showQuickClearRange: Boolean = true,
    showQuickClearAll: Boolean = true,
    onOpenPatternEditor: () -> Unit,
    onEraseDate: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val swipeEnabled = activeBrushCode == null && activePattern == null && !clearRangeModeActive
    val previewRangeStart = pendingPatternRangeStartDate ?: pendingClearRangeStartDate
    val previewRangeEnd = pendingPatternRangeEndDate ?: pendingClearRangeEndDate
    val monthHolidayItems = remember(currentMonth, holidayMap) {
        holidayMap.entries
            .filter { YearMonth.from(it.key) == currentMonth }
            .sortedBy { it.key }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (isLandscape) 12.dp else 16.dp)
        ) {
            Box(
                modifier = Modifier.pointerInput(currentMonth, swipeEnabled) {
                    if (swipeEnabled) {
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                accumulated += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    accumulated > 80f -> onPrevMonth()
                                    accumulated < -80f -> onNextMonth()
                                }
                            }
                        )
                    }
                }
            ) {
                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        val initialValue = initialState.year * 12 + initialState.monthValue
                        val targetValue = targetState.year * 12 + targetState.monthValue
                        if (targetValue > initialValue) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(220)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { -it },
                                        animationSpec = tween(280)
                                    ) + fadeOut(animationSpec = tween(180))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(220)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { it },
                                        animationSpec = tween(280)
                                    ) + fadeOut(animationSpec = tween(180))
                        }
                    },
                    label = "calendar_month"
                ) { shownMonth ->
                    if (isLandscape) {
                        Column {
                            MonthHeader(
                                currentMonth = shownMonth,
                                onPrevMonth = onPrevMonth,
                                onNextMonth = onNextMonth,
                                onPickMonth = onPickMonth
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                CalendarWorkplaceSwitcher(
                                    workplaces = workplaces,
                                    activeWorkplaceId = calendarWorkplaceFilterId,
                                    onSwitchWorkplace = onSwitchCalendarWorkplaceFilter,
                                    onOpenManageWorkplaces = onOpenManageWorkplaces,
                                    showAllWorkplacesOption = true,
                                    allWorkplacesOptionId = CALENDAR_WORKPLACE_ALL_ID,
                                    allWorkplacesOptionLabel = "Все работы"
                                )
                                CalendarProfileSwitcher(
                                    profiles = profiles,
                                    activeProfileId = activeProfileId,
                                    onSwitchProfile = onSwitchProfile,
                                    onOpenProfiles = onOpenProfiles
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.width(124.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (activePattern != null) {
                                        PatternApplyModeCard(
                                            pattern = activePattern,
                                            rangeStartDate = patternRangeStartDate,
                                            previewRangeStartDate = pendingPatternRangeStartDate,
                                            previewRangeEndDate = pendingPatternRangeEndDate,
                                            onOpenPreview = onOpenPatternPreview,
                                            onCancel = onCancelPatternMode
                                        )
                                    } else if (clearRangeModeActive) {
                                        ClearRangeModeCard(
                                            rangeStartDate = clearRangeStartDate,
                                            previewRangeStartDate = pendingClearRangeStartDate,
                                            previewRangeEndDate = pendingClearRangeEndDate,
                                            onApply = onConfirmClearRange,
                                            onCancel = onCancelClearRangeMode
                                        )
                                    } else if (activeBrushCode != null) {
                                        ActiveBrushCard(
                                            activeBrushCode = activeBrushCode,
                                            templateMap = templateMap,
                                            onDisableBrush = onDisableBrush
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    CalendarGrid(
                                        currentMonth = shownMonth,
                                        shiftCodesByDate = shiftCodesByDate,
                                        dayAssignmentsByDate = dayAssignmentsByDate,
                                        noteDates = noteDates,
                                        holidayMap = holidayMap,
                                        templateMap = templateMap,
                                        shiftColors = shiftColors,
                                        activeBrushCode = activeBrushCode,
                                        previewRangeStartDate = previewRangeStart,
                                        previewRangeEndDate = previewRangeEnd,
                                        onEraseDate = onEraseDate,
                                        onDayClick = onDayClick,
                                        onDayLongPress = onDayLongPress,
                                        compactMode = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            CalendarTodayNotesCard(
                                notes = todayNotes,
                                onAddNote = onAddTodayNote,
                                onEditNote = onEditNote
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            MonthHolidayInfoCard(
                                holidayEntries = monthHolidayItems
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ShiftLegend(
                                shiftTemplates = legendShiftTemplates,
                                shiftColors = shiftColors,
                                isExpanded = isLegendExpanded,
                                onToggle = onToggleLegend,
                                onOpenSettings = onOpenColorSettings
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            MonthCheckInlineCard(
                                audit = monthAudit,
                                onClick = onOpenMonthCheck
                            )
                        }
                    } else {
                        Column {
                            MonthHeader(
                                currentMonth = shownMonth,
                                onPrevMonth = onPrevMonth,
                                onNextMonth = onNextMonth,
                                onPickMonth = onPickMonth
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                CalendarWorkplaceSwitcher(
                                    workplaces = workplaces,
                                    activeWorkplaceId = calendarWorkplaceFilterId,
                                    onSwitchWorkplace = onSwitchCalendarWorkplaceFilter,
                                    onOpenManageWorkplaces = onOpenManageWorkplaces,
                                    showAllWorkplacesOption = true,
                                    allWorkplacesOptionId = CALENDAR_WORKPLACE_ALL_ID,
                                    allWorkplacesOptionLabel = "Все работы"
                                )
                                CalendarProfileSwitcher(
                                    profiles = profiles,
                                    activeProfileId = activeProfileId,
                                    onSwitchProfile = onSwitchProfile,
                                    onOpenProfiles = onOpenProfiles
                                )
                            }

                            if (activePattern != null) {
                                PatternApplyModeCard(
                                    pattern = activePattern,
                                    rangeStartDate = patternRangeStartDate,
                                    previewRangeStartDate = pendingPatternRangeStartDate,
                                    previewRangeEndDate = pendingPatternRangeEndDate,
                                    onOpenPreview = onOpenPatternPreview,
                                    onCancel = onCancelPatternMode
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            } else if (clearRangeModeActive) {
                                ClearRangeModeCard(
                                    rangeStartDate = clearRangeStartDate,
                                    previewRangeStartDate = pendingClearRangeStartDate,
                                    previewRangeEndDate = pendingClearRangeEndDate,
                                    onApply = onConfirmClearRange,
                                    onCancel = onCancelClearRangeMode
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            } else if (activeBrushCode != null) {
                                ActiveBrushCard(
                                    activeBrushCode = activeBrushCode,
                                    templateMap = templateMap,
                                    onDisableBrush = onDisableBrush
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            CalendarGrid(
                                currentMonth = shownMonth,
                                shiftCodesByDate = shiftCodesByDate,
                                dayAssignmentsByDate = dayAssignmentsByDate,
                                noteDates = noteDates,
                                holidayMap = holidayMap,
                                templateMap = templateMap,
                                shiftColors = shiftColors,
                                activeBrushCode = activeBrushCode,
                                previewRangeStartDate = previewRangeStart,
                                previewRangeEndDate = previewRangeEnd,
                                onEraseDate = onEraseDate,
                                onDayClick = onDayClick,
                                onDayLongPress = onDayLongPress,
                                compactMode = false
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            CalendarTodayNotesCard(
                                notes = todayNotes,
                                onAddNote = onAddTodayNote,
                                onEditNote = onEditNote
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            MonthHolidayInfoCard(
                                holidayEntries = monthHolidayItems
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ShiftLegend(
                                shiftTemplates = legendShiftTemplates,
                                shiftColors = shiftColors,
                                isExpanded = isLegendExpanded,
                                onToggle = onToggleLegend,
                                onOpenSettings = onOpenColorSettings
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            MonthCheckInlineCard(
                                audit = monthAudit,
                                onClick = onOpenMonthCheck
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 92.dp))
        }

        FloatingActionButton(
            onClick = onToggleQuickPicker,
            modifier = Modifier
                .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (isLandscape) 8.dp else 16.dp)
        ) {
            Icon(
                imageVector = if (quickPickerOpen) Icons.Rounded.Close else Icons.Rounded.Edit,
                contentDescription = if (quickPickerOpen) "Закрыть быстрый выбор" else "Открыть быстрый выбор"
            )
        }

        if (quickPickerOpen) {
            QuickShiftBar(
                shiftTemplates = quickShiftTemplates,
                workplaces = workplaces,
                activeWorkplaceId = activeWorkplaceId,
                systemStatusCodes = systemStatusCodes,
                activeBrushCode = activeBrushCode,
                isRangeClearModeActive = clearRangeModeActive,
                onSelectBrush = onSelectBrush,
                onClearBrush = onClearBrush,
                onDisableBrush = onDisableBrush,
                onAddNewShift = onAddNewShift,
                onOpenPatternEditor = onOpenPatternEditor,
                onClearCurrentMonth = onClearCurrentMonth,
                onStartRangeClearMode = onStartRangeClearMode,
                onClearAllCalendar = onClearAllCalendar,
                showEraser = showQuickEraser,
                showNormal = showQuickNormal,
                showCycle = showQuickCycle,
                showNewTemplate = showQuickNewTemplate,
                showClearMonth = showQuickClearMonth,
                showClearRange = showQuickClearRange,
                showClearAll = showQuickClearAll,
                onClose = onCloseQuickPicker,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = if (isLandscape) 88.dp else 16.dp,
                        end = 16.dp,
                        bottom = if (isLandscape) 12.dp else 72.dp
                    )
            )
        }
    }
}

@Composable
fun ActiveBrushCard(
    activeBrushCode: String,
    templateMap: Map<String, ShiftTemplateEntity>,
    onDisableBrush: () -> Unit
) {
    val title = when (activeBrushCode) {
        BRUSH_CLEAR -> "Ластик"
        else -> {
            val template = templateMap[activeBrushCode]
            if (template != null) {
                "${stripWorkplaceScopeFromShiftCode(template.code)} · ${template.title}"
            } else {
                stripWorkplaceScopeFromShiftCode(activeBrushCode)
            }
        }
    }

    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.GLASS,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = if (activeBrushCode == BRUSH_CLEAR) Icons.AutoMirrored.Rounded.Backspace else Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Активный инструмент",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            TextButton(onClick = onDisableBrush) {
                Text("Сброс")
            }
        }
    }
}

@Composable
private fun MonthCheckInlineCard(
    audit: CalendarMonthAudit,
    onClick: () -> Unit
) {
    val problemCount = audit.emptyDayCount + audit.overlappingWorkDayCount
    val title = if (problemCount == 0) "Месяц без явных проблем" else "Проверить месяц"
    val subtitle = "Пустых: ${audit.emptyDayCount} · несколько работ: ${audit.multiWorkDayCount} · пересечений: ${audit.overlappingWorkDayCount}"
    val accent = if (audit.overlappingWorkDayCount > 0) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    AppExpressiveSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(appCornerRadius(14.dp)))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        tone = if (problemCount == 0) AppExpressiveSurfaceTone.SOFT else AppExpressiveSurfaceTone.ACCENT,
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(8.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }
            Text(
                text = if (problemCount == 0) "OK" else problemCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun CalendarTodayNotesCard(
    notes: List<AppNote>,
    onAddNote: () -> Unit,
    onEditNote: (String) -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.PANEL,
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Заметки на сегодня",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (notes.isEmpty()) "Быстрая запись для текущего дня" else "записей: ${notes.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = appListSecondaryTextColor()
                    )
                }
                TextButton(onClick = appHapticAction(onAction = onAddNote)) {
                    Text("Добавить")
                }
            }

            if (notes.isEmpty()) {
                Text(
                    text = "Сегодня заметок нет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            } else {
                notes.take(2).forEach { note ->
                    NotePreviewCard(
                        note = note,
                        onClick = { onEditNote(note.id) }
                    )
                }
                if (notes.size > 2) {
                    Text(
                        text = "Ещё ${notes.size - 2} заметок во вкладке “Заметки”.",
                        style = MaterialTheme.typography.labelSmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHistoryInlineCard(items: List<String>) {
    if (items.isEmpty()) return

    Spacer(modifier = Modifier.height(10.dp))
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.SOFT,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            Text(
                text = "История месяца",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            items.take(4).forEach { item ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
        }
    }
}

@Composable
fun PatternApplyModeCard(
    pattern: PatternTemplate,
    rangeStartDate: LocalDate?,
    previewRangeStartDate: LocalDate?,
    previewRangeEndDate: LocalDate?,
    onOpenPreview: () -> Unit,
    onCancel: () -> Unit
) {
    val subtitle = when {
        previewRangeStartDate != null && previewRangeEndDate != null -> {
            "Диапазон: ${formatDate(previewRangeStartDate)} — ${formatDate(previewRangeEndDate)}"
        }

        rangeStartDate != null -> {
            "Начало: ${formatDate(rangeStartDate)}"
        }

        else -> {
            "Выбери первый день диапазона"
        }
    }

    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.GLASS,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Режим чередования",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pattern.name.ifBlank { "Без названия" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = subtitle,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (previewRangeStartDate != null && previewRangeEndDate != null) {
                    TextButton(onClick = onOpenPreview) {
                        Text("Предпросмотр")
                    }
                }

                TextButton(onClick = onCancel) {
                    Text("Сбросить")
                }
            }
        }
    }
}

@Composable
private fun ClearRangeModeCard(
    rangeStartDate: LocalDate?,
    previewRangeStartDate: LocalDate?,
    previewRangeEndDate: LocalDate?,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    val subtitle = when {
        previewRangeStartDate != null && previewRangeEndDate != null -> {
            "Диапазон: ${formatDate(previewRangeStartDate)} — ${formatDate(previewRangeEndDate)}"
        }

        rangeStartDate != null -> {
            "Начало: ${formatDate(rangeStartDate)}"
        }

        else -> {
            "Выбери первый день диапазона"
        }
    }

    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.GLASS,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Text(
                text = "Режим очистки диапазона",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (previewRangeStartDate != null && previewRangeEndDate != null) {
                    TextButton(onClick = onApply) {
                        Text("Очистить")
                    }
                }

                TextButton(onClick = onCancel) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    shiftCodesByDate: Map<LocalDate, String>,
    dayAssignmentsByDate: Map<LocalDate, List<CalendarDayAssignment>>,
    noteDates: Set<LocalDate>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    templateMap: Map<String, ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    activeBrushCode: String?,
    previewRangeStartDate: LocalDate?,
    previewRangeEndDate: LocalDate?,
    onEraseDate: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    compactMode: Boolean = false
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val cellBounds = remember(currentMonth) { mutableStateMapOf<LocalDate, Rect>() }
    val gap = if (compactMode) 4.dp else 6.dp
    if (compactMode) 56.dp else 70.dp

    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.PANEL,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .padding(if (compactMode) 8.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                daysOfWeek.forEachIndexed { index, dayName ->
                    val isWeekendHeader = index >= 5

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compactMode) 22.dp else 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isWeekendHeader) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compactMode) 6.dp else 8.dp))

            val firstDay = currentMonth.atDay(1)
            val daysInMonth = currentMonth.lengthOfMonth()
            val leadingCells = firstDay.dayOfWeek.value - 1
            val firstVisibleDate = firstDay.minusDays(leadingCells.toLong())
            val totalCells = ((leadingCells + daysInMonth + 6) / 7) * 7
            val calendarCells = List(totalCells) { offset -> firstVisibleDate.plusDays(offset.toLong()) }
            val weeks = calendarCells.chunked(7)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(activeBrushCode, cellBounds.size) {
                        if (activeBrushCode != null) {
                            var lastHitDate: LocalDate? = null

                            fun applyAt(position: Offset) {
                                val hitDate = cellBounds.entries
                                    .firstOrNull { it.value.contains(position) }
                                    ?.key

                                if (hitDate != null && hitDate != lastHitDate) {
                                    lastHitDate = hitDate

                                    if (activeBrushCode == BRUSH_CLEAR) {
                                        onEraseDate(hitDate)
                                    } else {
                                        onDayClick(hitDate)
                                    }
                                }
                            }

                            detectDragGestures(
                                onDragStart = { offset ->
                                    applyAt(offset)
                                },
                                onDrag = { change, _ ->
                                    applyAt(change.position)
                                },
                                onDragEnd = {
                                    lastHitDate = null
                                },
                                onDragCancel = {
                                    lastHitDate = null
                                }
                            )
                        }
                    }
            ) {
                Column {
                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = gap),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            week.forEach { date ->
                                val isCurrentMonthCell = YearMonth.from(date) == currentMonth
                                val assignments = dayAssignmentsByDate[date].orEmpty()
                                val primaryCode = shiftCodesByDate[date] ?: assignments.firstOrNull()?.shiftCode
                                val template = primaryCode?.let { templateMap[it] }
                                val visualShiftCodes = if (assignments.isNotEmpty()) {
                                    assignments.map { assignment -> assignment.shiftCode }
                                } else {
                                    listOfNotNull(primaryCode)
                                }
                                val visualWorkplaceIds = if (assignments.isNotEmpty()) {
                                    assignments.map { assignment -> assignment.workplaceId }
                                } else {
                                    emptyList()
                                }
                                val visualIconKeys = visualShiftCodes.map { code ->
                                    templateMap[code]?.iconKey
                                }
                                val visualBackgroundColors = visualShiftCodes.map { code ->
                                    shiftCellColor(code, shiftColors, templateMap)
                                }
                                val isSpecialDay = isCalendarDayOff(date, holidayMap)
                                val isInPreviewRange = isDateInRange(
                                    date = date,
                                    start = previewRangeStartDate,
                                    end = previewRangeEndDate
                                )
                                val isPreviewEdge =
                                    date == previewRangeStartDate || date == previewRangeEndDate

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coordinates ->
                                            if (isCurrentMonthCell) {
                                                val pos = coordinates.positionInParent()
                                                cellBounds[date] = Rect(
                                                    left = pos.x,
                                                    top = pos.y,
                                                    right = pos.x + coordinates.size.width,
                                                    bottom = pos.y + coordinates.size.height
                                                )
                                            } else {
                                                cellBounds.remove(date)
                                            }
                                        }
                                ) {
                                    DayCell(
                                        date = date,
                                        shiftCode = primaryCode,
                                        template = template,
                                        assignmentWorkplaceIds = visualWorkplaceIds,
                                        assignmentShiftCodes = visualShiftCodes,
                                        assignmentIconKeys = visualIconKeys,
                                        assignmentBackgroundColors = visualBackgroundColors,
                                        backgroundColor = shiftCellColor(primaryCode, shiftColors, templateMap),
                                        isSpecialDay = isSpecialDay,
                                        isInPreviewRange = isInPreviewRange,
                                        isPreviewEdge = isPreviewEdge,
                                        isCurrentMonthCell = isCurrentMonthCell,
                                        hasNote = date in noteDates,
                                        compactMode = compactMode,
                                        onClick = { onDayClick(date) },
                                        onLongClick = { onDayLongPress(date) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
