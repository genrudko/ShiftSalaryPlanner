package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettings
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryItem
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleBackground
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleId
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleSettings
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleSize
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleWidth
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import java.time.LocalDate

data class TodayShiftPreview(
    val title: String,
    val workplace: String,
    val code: String,
    val iconKey: String,
    val badgeColor: Int,
    val timeLabel: String,
    val hoursLabel: String
)

data class UpcomingPaymentItem(
    val title: String,
    val periodLabel: String,
    val date: LocalDate,
    val amount: Double? = null
)

@Composable
fun TodayOverviewScreen(
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    paymentDates: PaymentDates,
    payroll: PayrollResult,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "Сегодня",
            subtitle = "Смена, будильник и ближайшие выплаты",
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(appSectionSpacing()))

        ServiceInfoCard("Смены") {
            ServiceMetricRow("Сегодня", todaySummary.ifBlank { "смен нет" })
            ServiceMetricRow("Завтра", tomorrowSummary.ifBlank { "смен нет" })
            ServiceMetricRow("Будильник", nextAlarmSummary.ifBlank { "нет будущих срабатываний" })
        }
        ServiceInfoCard("Финансы") {
            ServiceMetricRow("Аванс", "${formatMoney(payroll.netAdvanceAfterDeductions)} · ${formatDate(paymentDates.advanceDate)}")
            ServiceMetricRow("Зарплата", "${formatMoney(payroll.netSalaryAfterDeductions)} · ${formatDate(paymentDates.salaryDate)}")
            ServiceMetricRow("Итого на руки", formatMoney(payroll.netTotal), emphasize = true)
        }
        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

@Composable
fun TodayTabScreen(
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    paymentDates: PaymentDates,
    payroll: PayrollResult,
    todayShifts: List<TodayShiftPreview>,
    upcomingPayments: List<UpcomingPaymentItem>,
    notes: List<AppNote>,
    onAddNote: () -> Unit,
    onEditNote: (String) -> Unit,
    monthAudit: CalendarMonthAudit,
    onOpenCalendar: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMonthCheck: () -> Unit,
    todayLayoutSettings: TodayLayoutSettings,
    onChangeTodayLayoutSettings: (TodayLayoutSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    var isEditingLayout by remember { mutableStateOf(false) }
    var draggedBubbleId by remember { mutableStateOf<TodayBubbleId?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val normalizedLayout = todayLayoutSettings.normalized()
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState, enabled = !isEditingLayout)
                .padding(appScreenPadding()),
            verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сегодня",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = appHapticAction(onAction = { isEditingLayout = !isEditingLayout })) {
                    Icon(
                        imageVector = if (isEditingLayout) Icons.Rounded.Done else Icons.Rounded.Settings,
                        contentDescription = if (isEditingLayout) "Готово" else "Настроить экран Сегодня"
                    )
                }
            }
            if (isEditingLayout) {
                TodayEditHint()
            }

            TodayBubbleTileRows(
                bubbles = normalizedLayout.bubbles.filter { it.visible },
                isEditingLayout = isEditingLayout,
                draggedBubbleId = draggedBubbleId,
                dragOffsetY = dragOffsetY,
                onEnterEditMode = { isEditingLayout = true },
                onDragStart = { bubble ->
                    isEditingLayout = true
                    draggedBubbleId = bubble.id
                    dragOffsetY = 0f
                },
                onDrag = { dragOffsetY += it },
                onDragEnd = { bubble, visibleIndex ->
                    val shouldMoveUp = dragOffsetY < -96f && visibleIndex > 0
                    val shouldMoveDown = dragOffsetY > 96f &&
                        visibleIndex < normalizedLayout.bubbles.count { it.visible } - 1
                    if (shouldMoveUp || shouldMoveDown) {
                        val fromIndex = normalizedLayout.bubbles.indexOfFirst { it.id == bubble.id }
                        val direction = if (shouldMoveUp) -1 else 1
                        val toIndex = normalizedLayout.bubbles.nextVisibleIndex(fromIndex, direction)
                        if (toIndex != fromIndex) {
                            onChangeTodayLayoutSettings(
                                normalizedLayout.copy(
                                    bubbles = normalizedLayout.bubbles.moveBubbleToIndex(fromIndex, toIndex)
                                )
                            )
                        }
                    }
                    draggedBubbleId = null
                    dragOffsetY = 0f
                },
                onChangeBubble = { updated ->
                    onChangeTodayLayoutSettings(normalizedLayout.copy(bubbles = normalizedLayout.bubbles.replaceBubble(updated)))
                }
            ) { bubble ->
                TodayBubbleVisualScope(bubble) {
                    TodayBubbleContent(
                        bubbleId = bubble.id,
                        today = today,
                        todaySummary = todaySummary,
                        tomorrowSummary = tomorrowSummary,
                        nextAlarmSummary = nextAlarmSummary,
                        paymentDates = paymentDates,
                        payroll = payroll,
                        todayShifts = todayShifts,
                        upcomingPayments = upcomingPayments,
                        notes = notes,
                        onAddNote = onAddNote,
                        onEditNote = onEditNote,
                        monthAudit = monthAudit,
                        onOpenCalendar = onOpenCalendar,
                        onOpenAlarms = onOpenAlarms,
                        onOpenFinance = onOpenFinance,
                        onOpenMonthCheck = onOpenMonthCheck,
                        actionsEnabled = !isEditingLayout
                    )
                }
            }

            if (isEditingLayout) {
                TodayAddHiddenBubblesPanel(
                    hiddenBubbles = normalizedLayout.bubbles.filterNot { it.visible },
                    onAdd = { bubble ->
                        onChangeTodayLayoutSettings(
                            normalizedLayout.copy(
                                bubbles = normalizedLayout.bubbles.replaceBubble(bubble.copy(visible = true))
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(if (isEditingLayout) 146.dp else 92.dp)))
        }
        if (isEditingLayout) {
            TodayEditFloatingBar(
                visibleCount = normalizedLayout.bubbles.count { it.visible },
                onDone = {
                    draggedBubbleId = null
                    dragOffsetY = 0f
                    isEditingLayout = false
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = appScreenPadding(), vertical = appScaledSpacing(92.dp))
            )
        }
    }
}

private data class TodayBubbleVisuals(
    val size: TodayBubbleSize = TodayBubbleSize.NORMAL,
    val background: TodayBubbleBackground = TodayBubbleBackground.DEFAULT
)

private val LocalTodayBubbleVisuals = staticCompositionLocalOf { TodayBubbleVisuals() }

@Composable
private fun TodayBubbleVisualScope(
    bubble: TodayBubbleSettings,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTodayBubbleVisuals provides TodayBubbleVisuals(
            size = bubble.size,
            background = bubble.background
        ),
        content = content
    )
}

@Composable
private fun todayBubblePadding(): Dp {
    return when (LocalTodayBubbleVisuals.current.size) {
        TodayBubbleSize.COMPACT -> appScaledSpacing(8.dp)
        TodayBubbleSize.NORMAL -> appCardPadding()
        TodayBubbleSize.LARGE -> appScaledSpacing(30.dp)
    }
}

@Composable
private fun todayBubbleMinHeight(size: TodayBubbleSize): Dp {
    return when (size) {
        TodayBubbleSize.COMPACT -> appScaledSpacing(92.dp)
        TodayBubbleSize.NORMAL -> appScaledSpacing(150.dp)
        TodayBubbleSize.LARGE -> appScaledSpacing(230.dp)
    }
}

@Composable
private fun todayBubbleColor(defaultAlpha: Float): Color {
    return when (LocalTodayBubbleVisuals.current.background) {
        TodayBubbleBackground.DEFAULT -> appBubbleBackgroundColor(defaultAlpha = defaultAlpha)
        TodayBubbleBackground.SOFT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = defaultAlpha)
        TodayBubbleBackground.ACCENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = defaultAlpha + 0.02f)
        TodayBubbleBackground.GLASS -> MaterialTheme.colorScheme.surface.copy(alpha = defaultAlpha + 0.06f)
        TodayBubbleBackground.MINT -> Color(0xFF58D6B2).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.OCEAN -> Color(0xFF4EA7D8).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.SKY -> Color(0xFF9CC7FF).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.LAVENDER -> Color(0xFFB7A2F4).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.ROSE -> Color(0xFFFF9AB3).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.PEACH -> Color(0xFFFFB17A).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.AMBER -> Color(0xFFFFD166).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.FOREST -> Color(0xFF6FC17B).copy(alpha = defaultAlpha + 0.04f)
        TodayBubbleBackground.GRAPHITE -> Color(0xFF707684).copy(alpha = defaultAlpha + 0.08f)
    }
}

@Composable
private fun TodayBubbleTileRows(
    bubbles: List<TodayBubbleSettings>,
    isEditingLayout: Boolean,
    draggedBubbleId: TodayBubbleId?,
    dragOffsetY: Float,
    onEnterEditMode: () -> Unit,
    onDragStart: (TodayBubbleSettings) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (TodayBubbleSettings, Int) -> Unit,
    onChangeBubble: (TodayBubbleSettings) -> Unit,
    content: @Composable (TodayBubbleSettings) -> Unit
) {
    var index = 0
    while (index < bubbles.size) {
        val first = bubbles[index]
        val second = bubbles.getOrNull(index + 1)
            ?.takeIf { first.width == TodayBubbleWidth.HALF && it.width == TodayBubbleWidth.HALF }

        if (second != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                TodayBubbleTile(
                    bubble = first,
                    visibleIndex = index,
                    isEditingLayout = isEditingLayout,
                    draggedBubbleId = draggedBubbleId,
                    dragOffsetY = dragOffsetY,
                    onEnterEditMode = onEnterEditMode,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onChangeBubble = onChangeBubble,
                    modifier = Modifier.weight(1f),
                    content = content
                )
                TodayBubbleTile(
                    bubble = second,
                    visibleIndex = index + 1,
                    isEditingLayout = isEditingLayout,
                    draggedBubbleId = draggedBubbleId,
                    dragOffsetY = dragOffsetY,
                    onEnterEditMode = onEnterEditMode,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onChangeBubble = onChangeBubble,
                    modifier = Modifier.weight(1f),
                    content = content
                )
            }
            index += 2
        } else if (first.width == TodayBubbleWidth.HALF) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                TodayBubbleTile(
                    bubble = first,
                    visibleIndex = index,
                    isEditingLayout = isEditingLayout,
                    draggedBubbleId = draggedBubbleId,
                    dragOffsetY = dragOffsetY,
                    onEnterEditMode = onEnterEditMode,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onChangeBubble = onChangeBubble,
                    modifier = Modifier.weight(1f),
                    content = content
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            index += 1
        } else {
            TodayBubbleTile(
                bubble = first,
                visibleIndex = index,
                isEditingLayout = isEditingLayout,
                draggedBubbleId = draggedBubbleId,
                dragOffsetY = dragOffsetY,
                onEnterEditMode = onEnterEditMode,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onChangeBubble = onChangeBubble,
                content = content
            )
            index += 1
        }
    }
}

@Composable
private fun TodayBubbleTile(
    bubble: TodayBubbleSettings,
    visibleIndex: Int,
    isEditingLayout: Boolean,
    draggedBubbleId: TodayBubbleId?,
    dragOffsetY: Float,
    onEnterEditMode: () -> Unit,
    onDragStart: (TodayBubbleSettings) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (TodayBubbleSettings, Int) -> Unit,
    onChangeBubble: (TodayBubbleSettings) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (TodayBubbleSettings) -> Unit
) {
    EditableTodayBubble(
        bubble = bubble,
        isEditing = isEditingLayout,
        isDragging = draggedBubbleId == bubble.id,
        dragOffsetY = if (draggedBubbleId == bubble.id) dragOffsetY else 0f,
        onEnterEditMode = onEnterEditMode,
        onDragStart = { onDragStart(bubble) },
        onDrag = onDrag,
        onDragEnd = { onDragEnd(bubble, visibleIndex) },
        onChange = onChangeBubble,
        modifier = modifier
    ) {
        content(bubble)
    }
}

@Composable
private fun TodayEditHint() {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        tone = AppExpressiveSurfaceTone.ACCENT,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Text(
            text = "Редактирование: тяни карточку за удержание, углы меняют размер. Завершить можно кнопкой «Готово» внизу.",
            modifier = Modifier.padding(todayBubblePadding()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TodayEditFloatingBar(
    visibleCount: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppExpressiveSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(24.dp)),
        tone = AppExpressiveSurfaceTone.FLOATING,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = appScaledSpacing(14.dp), vertical = appScaledSpacing(10.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(appScaledSpacing(8.dp))
                        .size(appScaledSpacing(18.dp)),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Редактирование экрана",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$visibleCount карточек на экране",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
            TextButton(onClick = appHapticAction(onAction = onDone)) {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = null,
                    modifier = Modifier.size(appScaledSpacing(18.dp))
                )
                Spacer(modifier = Modifier.size(appScaledSpacing(6.dp)))
                Text("Готово", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EditableTodayBubble(
    bubble: TodayBubbleSettings,
    isEditing: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onEnterEditMode: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onChange: (TodayBubbleSettings) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val editShape = RoundedCornerShape(appCornerRadius(22.dp))
    var showBackgroundPicker by remember(bubble.id) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = todayBubbleMinHeight(bubble.size))
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                scaleX = if (isDragging) 1.015f else 1f
                scaleY = if (isDragging) 1.015f else 1f
            }
            .pointerInput(bubble.id, isEditing) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onEnterEditMode()
                        onDragStart()
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { _, dragAmount -> onDrag(dragAmount.y) }
                )
            }
    ) {
        content()
        if (isEditing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                        shape = editShape
                    )
            )
            TodayResizeHandle(
                alignment = Alignment.TopStart,
                onResize = { axis, grow -> onChange(bubble.resizeBy(axis, grow)) }
            )
            TodayResizeHandle(
                alignment = Alignment.TopEnd,
                onResize = { axis, grow -> onChange(bubble.resizeBy(axis, grow)) }
            )
            TodayResizeHandle(
                alignment = Alignment.BottomStart,
                onResize = { axis, grow -> onChange(bubble.resizeBy(axis, grow)) }
            )
            TodayResizeHandle(
                alignment = Alignment.BottomEnd,
                onResize = { axis, grow -> onChange(bubble.resizeBy(axis, grow)) }
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = appScaledSpacing(6.dp)),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TodayEditIconButton(
                    icon = Icons.Rounded.DragIndicator,
                    label = "Перетащить",
                    onClick = onEnterEditMode
                )
                TodayEditIconButton(
                    icon = Icons.Rounded.Palette,
                    label = "Фон",
                    onClick = { showBackgroundPicker = !showBackgroundPicker }
                )
                TodayEditIconButton(
                    icon = Icons.Rounded.Close,
                    label = "Убрать",
                    onClick = { onChange(bubble.copy(visible = false)) }
                )
            }
            if (showBackgroundPicker) {
                TodayBackgroundPicker(
                    selected = bubble.background,
                    onSelect = { background ->
                        showBackgroundPicker = false
                        onChange(bubble.copy(background = background))
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = appScaledSpacing(46.dp))
                )
            }
        }
    }
}

@Composable
private fun TodayEditIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(appScaledSpacing(34.dp)),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = appHapticAction(onAction = onClick)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(appScaledSpacing(18.dp)),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TodayBackgroundPicker(
    selected: TodayBubbleBackground,
    onSelect: (TodayBubbleBackground) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            TodayBubbleBackground.values().toList().chunked(7).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { background ->
                        val swatchColor = todayBubbleBackgroundPreviewColor(background)
                        Box(
                            modifier = Modifier
                                .size(appScaledSpacing(23.dp))
                                .border(
                                    width = if (background == selected) 2.dp else 1.dp,
                                    color = if (background == selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        appPanelBorderColor()
                                    },
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .background(swatchColor, RoundedCornerShape(999.dp))
                                .clickable(onClick = appHapticAction(onAction = { onSelect(background) }))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun todayBubbleBackgroundPreviewColor(background: TodayBubbleBackground): Color {
    return when (background) {
        TodayBubbleBackground.DEFAULT -> appBubbleBackgroundColor(defaultAlpha = 0.32f)
        TodayBubbleBackground.SOFT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        TodayBubbleBackground.ACCENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        TodayBubbleBackground.GLASS -> MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        TodayBubbleBackground.MINT -> Color(0xFF58D6B2)
        TodayBubbleBackground.OCEAN -> Color(0xFF4EA7D8)
        TodayBubbleBackground.SKY -> Color(0xFF9CC7FF)
        TodayBubbleBackground.LAVENDER -> Color(0xFFB7A2F4)
        TodayBubbleBackground.ROSE -> Color(0xFFFF9AB3)
        TodayBubbleBackground.PEACH -> Color(0xFFFFB17A)
        TodayBubbleBackground.AMBER -> Color(0xFFFFD166)
        TodayBubbleBackground.FOREST -> Color(0xFF6FC17B)
        TodayBubbleBackground.GRAPHITE -> Color(0xFF707684)
    }
}

@Composable
private fun BoxScope.TodayResizeHandle(
    alignment: Alignment,
    onResize: (TodayResizeAxis, Boolean) -> Unit
) {
    var horizontalDragSum by remember { mutableStateOf(0f) }
    var verticalDragSum by remember { mutableStateOf(0f) }
    Surface(
        modifier = Modifier
            .align(alignment)
            .size(appScaledSpacing(22.dp))
            .pointerInput(alignment) {
                detectDragGestures(
                    onDragEnd = {
                        horizontalDragSum = 0f
                        verticalDragSum = 0f
                    },
                    onDragCancel = {
                        horizontalDragSum = 0f
                        verticalDragSum = 0f
                    },
                    onDrag = { _, dragAmount ->
                        val horizontalSign = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart) {
                            -1f
                        } else {
                            1f
                        }
                        val verticalSign = if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd) {
                            -1f
                        } else {
                            1f
                        }
                        horizontalDragSum += dragAmount.x * horizontalSign
                        verticalDragSum += dragAmount.y * verticalSign
                        if (horizontalDragSum > 42f) {
                            onResize(TodayResizeAxis.HORIZONTAL, true)
                            horizontalDragSum = 0f
                        } else if (horizontalDragSum < -42f) {
                            onResize(TodayResizeAxis.HORIZONTAL, false)
                            horizontalDragSum = 0f
                        }
                        if (verticalDragSum > 42f) {
                            onResize(TodayResizeAxis.VERTICAL, true)
                            verticalDragSum = 0f
                        } else if (verticalDragSum < -42f) {
                            onResize(TodayResizeAxis.VERTICAL, false)
                            verticalDragSum = 0f
                        }
                    }
                )
            },
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.58f))
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun TodayAddHiddenBubblesPanel(
    hiddenBubbles: List<TodayBubbleSettings>,
    onAdd: (TodayBubbleSettings) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.18f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(todayBubblePadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            Text(
                text = "Добавить",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (hiddenBubbles.isEmpty()) {
                Text(
                    text = "Все карточки уже на экране.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            } else {
                hiddenBubbles.forEach { bubble ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = appHapticAction(onAction = { onAdd(bubble) })),
                        shape = RoundedCornerShape(appCornerRadius(14.dp)),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                        border = BorderStroke(1.dp, appPanelBorderColor())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = todayBubbleTitle(bubble.id),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayBubbleContent(
    bubbleId: TodayBubbleId,
    today: LocalDate,
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    paymentDates: PaymentDates,
    payroll: PayrollResult,
    todayShifts: List<TodayShiftPreview>,
    upcomingPayments: List<UpcomingPaymentItem>,
    notes: List<AppNote>,
    onAddNote: () -> Unit,
    onEditNote: (String) -> Unit,
    monthAudit: CalendarMonthAudit,
    onOpenCalendar: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMonthCheck: () -> Unit,
    actionsEnabled: Boolean
) {
    val openCalendar = if (actionsEnabled) onOpenCalendar else ({})
    val openAlarms = if (actionsEnabled) onOpenAlarms else ({})
    val openFinance = if (actionsEnabled) onOpenFinance else ({})
    val openMonthCheck = if (actionsEnabled) onOpenMonthCheck else ({})
    val addNote = if (actionsEnabled) onAddNote else ({})
    val editNote: (String) -> Unit = if (actionsEnabled) onEditNote else ({ _ -> })

    when (bubbleId) {
        TodayBubbleId.HERO -> TodayHeroCard(
            dateLabel = formatDate(today),
            todaySummary = todaySummary.ifBlank { "Сегодня смен нет" },
            nextAlarmSummary = nextAlarmSummary.ifBlank { "Будильники не запланированы" },
            todayShifts = todayShifts,
            onOpenCalendar = openCalendar
        )

        TodayBubbleId.PULSE -> TodayPulseCard(
            todaySummary = todaySummary,
            nextAlarmSummary = nextAlarmSummary,
            monthAudit = monthAudit,
            onOpenMonthCheck = openMonthCheck
        )

        TodayBubbleId.NEXT_STEP -> TodayNextStepCard(
            todaySummary = todaySummary,
            nextAlarmSummary = nextAlarmSummary,
            paymentDates = paymentDates,
            onOpenCalendar = openCalendar,
            onOpenAlarms = openAlarms,
            onOpenFinance = openFinance
        )

        TodayBubbleId.QUICK_ACTIONS -> {
            TodayMiniActionCard(
                title = "Завтра",
                value = tomorrowSummary.ifBlank { "смен нет" },
                icon = Icons.Rounded.Upcoming,
                onClick = openCalendar,
                modifier = Modifier.fillMaxWidth()
            )
        }

        TodayBubbleId.TOMORROW -> TodayMiniActionCard(
            title = "Завтра",
            value = tomorrowSummary.ifBlank { "смен нет" },
            icon = Icons.Rounded.Upcoming,
            onClick = openCalendar,
            modifier = Modifier.fillMaxWidth()
        )

        TodayBubbleId.NEXT_ALARM -> TodayMiniActionCard(
            title = "Будильник",
            value = nextAlarmSummary.ifBlank { "нет" },
            icon = Icons.Rounded.Alarm,
            onClick = openAlarms,
            modifier = Modifier.fillMaxWidth()
        )

        TodayBubbleId.MONTH_STATUS -> TodayMonthStatusCard(
            monthAudit = monthAudit,
            onOpenMonthCheck = openMonthCheck
        )

        TodayBubbleId.PAYMENTS -> TodayPaymentsCard(
            upcomingPayments = upcomingPayments,
            paymentDates = paymentDates,
            payroll = payroll,
            onOpenFinance = openFinance
        )

        TodayBubbleId.NOTES -> TodayNotesCard(
            notes = notes,
            onAddNote = addNote,
            onEditNote = editNote
        )
    }
}

@Composable
private fun TodayOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else appPanelBorderColor()
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun todayBubbleTitle(id: TodayBubbleId): String = when (id) {
    TodayBubbleId.HERO -> "Главная карточка"
    TodayBubbleId.PULSE -> "Пульс дня"
    TodayBubbleId.NEXT_STEP -> "Что дальше"
    TodayBubbleId.QUICK_ACTIONS -> "Быстрые карточки"
    TodayBubbleId.TOMORROW -> "Завтра"
    TodayBubbleId.NEXT_ALARM -> "Будильник"
    TodayBubbleId.MONTH_STATUS -> "Проверка месяца"
    TodayBubbleId.PAYMENTS -> "Ближайшие выплаты"
    TodayBubbleId.NOTES -> "Заметки"
}

private fun List<TodayBubbleSettings>.replaceBubble(updated: TodayBubbleSettings): List<TodayBubbleSettings> {
    return map { bubble -> if (bubble.id == updated.id) updated else bubble }
}

private fun List<TodayBubbleSettings>.nextVisibleIndex(index: Int, direction: Int): Int {
    var next = index + direction
    while (next in indices) {
        if (this[next].visible) return next
        next += direction
    }
    return index
}

private fun List<TodayBubbleSettings>.moveBubbleToIndex(index: Int, targetIndex: Int): List<TodayBubbleSettings> {
    if (targetIndex == index || index !in indices || targetIndex !in indices) return this
    return toMutableList().also { items ->
        val bubble = items.removeAt(index)
        items.add(targetIndex, bubble)
    }
}

private enum class TodayResizeAxis {
    HORIZONTAL,
    VERTICAL
}

private fun TodayBubbleSettings.resizeBy(axis: TodayResizeAxis, grow: Boolean): TodayBubbleSettings {
    return when (axis) {
        TodayResizeAxis.HORIZONTAL -> copy(
            width = when {
                grow -> TodayBubbleWidth.FULL
                !grow -> TodayBubbleWidth.HALF
                else -> width
            }
        )

        TodayResizeAxis.VERTICAL -> copy(size = size.resizeBy(grow))
    }
}

private fun TodayBubbleSize.resizeBy(grow: Boolean): TodayBubbleSize = when {
    grow && this == TodayBubbleSize.COMPACT -> TodayBubbleSize.NORMAL
    grow && this == TodayBubbleSize.NORMAL -> TodayBubbleSize.LARGE
    !grow && this == TodayBubbleSize.LARGE -> TodayBubbleSize.NORMAL
    !grow && this == TodayBubbleSize.NORMAL -> TodayBubbleSize.COMPACT
    else -> this
}

@Composable
private fun TodayPulseCard(
    todaySummary: String,
    nextAlarmSummary: String,
    monthAudit: CalendarMonthAudit,
    onOpenMonthCheck: () -> Unit
) {
    val hasShift = hasUsefulDaySummary(todaySummary)
    val hasAlarm = nextAlarmSummary.isNotBlank()
    val hasMonthWarnings = monthAudit.overlappingWorkDayCount > 0
    val readyCount = listOf(hasShift, hasAlarm, !hasMonthWarnings).count { it }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(20.dp)),
        color = todayBubbleColor(defaultAlpha = 0.24f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(todayBubblePadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Пульс дня",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$readyCount из 3 пунктов в порядке",
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$readyCount/3",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TodayPulseRow(
                title = "Смена на сегодня",
                ok = hasShift,
                message = if (hasShift) todaySummary else "Не назначена"
            )
            TodayPulseRow(
                title = "Будильник",
                ok = hasAlarm,
                message = if (hasAlarm) nextAlarmSummary else "Нет ближайшего срабатывания"
            )
            TodayPulseRow(
                title = "Месяц",
                ok = !hasMonthWarnings,
                message = if (hasMonthWarnings) {
                    "Есть пересечения по времени"
                } else {
                    "Критичных пересечений нет"
                },
                onClick = onOpenMonthCheck
            )
        }
    }
}

@Composable
private fun TodayNextStepCard(
    todaySummary: String,
    nextAlarmSummary: String,
    paymentDates: PaymentDates,
    onOpenCalendar: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenFinance: () -> Unit
) {
    val today = LocalDate.now()
    val paymentHints = listOf(
        "Аванс" to java.time.temporal.ChronoUnit.DAYS.between(today, paymentDates.advanceDate),
        "Зарплата" to java.time.temporal.ChronoUnit.DAYS.between(today, paymentDates.salaryDate)
    )
    val closestPayment = paymentHints
        .filter { (_, days) -> days in 0..2 }
        .minByOrNull { (_, days) -> days }

    val noShiftToday = !hasUsefulDaySummary(todaySummary)
    val noUpcomingAlarm = nextAlarmSummary.isBlank()
    val title: String
    val subtitle: String
    val icon: ImageVector
    val action: String
    val onClick: () -> Unit

    when {
        closestPayment != null -> {
            val (label, days) = closestPayment
            title = if (days == 0L) "$label сегодня" else "$label скоро"
            subtitle = if (days == 0L) {
                "Проверь сумму и отметь фактическую выплату."
            } else {
                "До выплаты ${daysUntilLabel(if (label == "Аванс") paymentDates.advanceDate else paymentDates.salaryDate)}."
            }
            icon = Icons.Rounded.Payments
            action = "Финансы"
            onClick = onOpenFinance
        }
        noShiftToday -> {
            title = "День ещё пустой"
            subtitle = "Можно быстро назначить смену или статус на сегодня."
            icon = Icons.Rounded.Today
            action = "Календарь"
            onClick = onOpenCalendar
        }
        noUpcomingAlarm -> {
            title = "Будильник не запланирован"
            subtitle = "Если завтра ранняя смена, лучше проверить расписание звонка."
            icon = Icons.Rounded.Alarm
            action = "Будильники"
            onClick = onOpenAlarms
        }
        else -> {
            title = "Всё на сегодня собрано"
            subtitle = "Смена и ближайший будильник уже под рукой."
            icon = Icons.Rounded.CheckCircle
            action = "Календарь"
            onClick = onOpenCalendar
        }
    }

    TodayWideActionCard(
        title = title,
        subtitle = subtitle,
        icon = icon,
        action = action,
        onClick = onClick
    )
}

@Composable
private fun TodayPulseRow(
    title: String,
    ok: Boolean,
    message: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = appHapticAction(onAction = onClick)) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = appListSecondaryTextColor()
            )
        }
    }
}

@Composable
private fun TodayMonthStatusCard(
    monthAudit: CalendarMonthAudit,
    onOpenMonthCheck: () -> Unit
) {
    val problemCount = monthAudit.emptyDayCount + monthAudit.overlappingWorkDayCount
    TodayWideActionCard(
        title = if (problemCount == 0) "Месяц выглядит ровно" else "Месяц требует внимания",
        subtitle = "Пустых: ${monthAudit.emptyDayCount} · несколько работ: ${monthAudit.multiWorkDayCount} · пересечений: ${monthAudit.overlappingWorkDayCount}",
        icon = Icons.Rounded.EventAvailable,
        action = "Проверить",
        onClick = onOpenMonthCheck
    )
}

@Composable
private fun TodayWideActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    action: String,
    onClick: () -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(todayBubblePadding()),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TodayHeroCard(
    dateLabel: String,
    todaySummary: String,
    nextAlarmSummary: String,
    todayShifts: List<TodayShiftPreview>,
    onOpenCalendar: () -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(26.dp)),
        tone = AppExpressiveSurfaceTone.ACCENT,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(todayBubblePadding())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сегодня",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = appListSecondaryTextColor()
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(appScaledSpacing(10.dp))
                        )
                    }
                }
                Text(
                    text = todaySummary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (todayShifts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                        todayShifts.take(3).forEach { shift ->
                            TodayShiftLine(shift = shift)
                        }
                    }
                }
                Text(
                    text = "Следующий будильник: $nextAlarmSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
                TextButton(onClick = appHapticAction(onAction = onOpenCalendar)) {
                    Text("Открыть календарь")
                }
            }
        }
    }
}

@Composable
private fun TodayShiftLine(shift: TodayShiftPreview) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        tone = AppExpressiveSurfaceTone.GLASS,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appScaledSpacing(9.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(9.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                iconKey = shift.iconKey,
                fallbackCode = shift.code,
                badgeColor = androidx.compose.ui.graphics.Color(shift.badgeColor),
                size = 30.dp,
                shape = RoundedCornerShape(appCornerRadius(10.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shift.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${shift.timeLabel} · ${shift.hoursLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }
            if (shift.workplace.isNotBlank()) {
                Text(
                    text = shift.workplace,
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }
        }
    }
}

@Composable
private fun TodayMiniActionCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppExpressiveSurface(
        modifier = modifier.clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(todayBubblePadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TodayPaymentsCard(
    upcomingPayments: List<UpcomingPaymentItem>,
    paymentDates: PaymentDates,
    payroll: PayrollResult,
    onOpenFinance: () -> Unit
) {
    ServiceInfoCard("Ближайшие выплаты") {
        val visiblePayments = upcomingPayments.take(2)
        if (visiblePayments.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                visiblePayments.forEach { item ->
                    TodayMiniMetric(
                        title = item.title,
                        value = daysUntilLabel(item.date),
                        icon = Icons.Rounded.Payments,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (visiblePayments.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            visiblePayments.forEach { item ->
                val amountLabel = item.amount?.takeIf { it > 0.0 }?.let { " · ${formatMoney(it)}" }.orEmpty()
                ServiceMetricRow(
                    item.title,
                    "${formatDate(item.date)} · ${item.periodLabel}$amountLabel"
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                TodayMiniMetric(
                    title = "Аванс",
                    value = daysUntilLabel(paymentDates.advanceDate),
                    icon = Icons.Rounded.Payments,
                    modifier = Modifier.weight(1f)
                )
                TodayMiniMetric(
                    title = "Зарплата",
                    value = daysUntilLabel(paymentDates.salaryDate),
                    icon = Icons.Rounded.Payments,
                    modifier = Modifier.weight(1f)
                )
            }
            ServiceMetricRow("Аванс", "${formatMoney(payroll.netAdvanceAfterDeductions)} · ${formatDate(paymentDates.advanceDate)}")
            ServiceMetricRow("Зарплата", "${formatMoney(payroll.netSalaryAfterDeductions)} · ${formatDate(paymentDates.salaryDate)}")
        }
        ServiceMetricRow("Всего на руки", formatMoney(payroll.netTotal), emphasize = true)
        TextButton(
            onClick = appHapticAction(onAction = onOpenFinance),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Открыть финансы")
        }
    }
}

@Composable
private fun TodayNotesCard(
    notes: List<AppNote>,
    onAddNote: () -> Unit,
    onEditNote: (String) -> Unit
) {
    ServiceInfoCard("Заметки") {
        if (notes.isEmpty()) {
            Text(
                text = "На сегодня заметок нет. Можно оставить задачу, напоминание или короткую запись по смене.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        } else {
            notes.take(3).forEach { note ->
                NotePreviewCard(
                    note = note,
                    onClick = { onEditNote(note.id) }
                )
            }
        }
        TextButton(
            onClick = appHapticAction(onAction = onAddNote),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Добавить заметку")
        }
    }
}

@Composable
private fun TodayMiniMetric(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AppExpressiveSurface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        tone = AppExpressiveSurfaceTone.GLASS,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(appScaledSpacing(10.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = appListSecondaryTextColor()
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun daysUntilLabel(date: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days < 0 -> "прошло"
        days == 0L -> "сегодня"
        days == 1L -> "завтра"
        else -> "через $days дн."
    }
}

private fun hasUsefulDaySummary(value: String): Boolean {
    val normalized = value.trim().lowercase()
    return normalized.isNotBlank() && normalized != "смен нет"
}

@Composable
fun QuickActionsSettingsScreen(
    settings: AppWorkflowSettings,
    onChange: (AppWorkflowSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "Быстрые действия",
            subtitle = "Какие кнопки показывать в быстром вводе",
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(appSectionSpacing()))

        ServiceInfoCard("Основной ряд") {
            WorkflowSwitchRow("Ластик", settings.showQuickEraser) { onChange(settings.copy(showQuickEraser = it)) }
            WorkflowSwitchRow("Обычный режим", settings.showQuickNormal) { onChange(settings.copy(showQuickNormal = it)) }
            WorkflowSwitchRow("Цикл", settings.showQuickCycle) { onChange(settings.copy(showQuickCycle = it)) }
            WorkflowSwitchRow("Новый шаблон", settings.showQuickNewTemplate) { onChange(settings.copy(showQuickNewTemplate = it)) }
        }
        ServiceInfoCard("Очистка") {
            WorkflowSwitchRow("Очистить месяц", settings.showQuickClearMonth) { onChange(settings.copy(showQuickClearMonth = it)) }
            WorkflowSwitchRow("Очистить диапазон", settings.showQuickClearRange) { onChange(settings.copy(showQuickClearRange = it)) }
            WorkflowSwitchRow("Очистить календарь", settings.showQuickClearAll) { onChange(settings.copy(showQuickClearAll = it)) }
        }
        ServiceInfoCard("Свайпы в списках") {
            Text(
                text = "Можно отключить опасные или лишние жесты отдельно для смен и будильников.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            WorkflowSwitchRow("Смены: дублировать вправо", settings.shiftSwipeDuplicateEnabled) {
                onChange(settings.copy(shiftSwipeDuplicateEnabled = it))
            }
            WorkflowSwitchRow("Смены: удалить влево", settings.shiftSwipeDeleteEnabled) {
                onChange(settings.copy(shiftSwipeDeleteEnabled = it))
            }
            WorkflowSwitchRow("Будильники: дублировать вправо", settings.alarmSwipeDuplicateEnabled) {
                onChange(settings.copy(alarmSwipeDuplicateEnabled = it))
            }
            WorkflowSwitchRow("Будильники: удалить влево", settings.alarmSwipeDeleteEnabled) {
                onChange(settings.copy(alarmSwipeDeleteEnabled = it))
            }
        }
        ServiceInfoCard("Факт выплат") {
            Text(
                text = "Если расчёт и фактическая выплата отличаются меньше этого значения, подсветка останется спокойной.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            CompactDecimalField(
                label = "Допуск, ₽",
                value = formatDouble(settings.paymentDifferenceToleranceRub),
                onValueChange = { value ->
                    onChange(
                        settings.copy(
                            paymentDifferenceToleranceRub = parseWorkflowMoney(value)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

private fun parseWorkflowMoney(value: String): Double {
    return value
        .replace(" ", "")
        .replace(',', '.')
        .toDoubleOrNull()
        ?.coerceAtLeast(0.0)
        ?: 0.0
}

@Composable
fun ReportCenterScreen(
    historyItems: List<ReportHistoryItem>,
    onBack: () -> Unit,
    onOpenMonthlyReport: () -> Unit,
    onOpenPayrollPdf: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "Центр отчётов",
            subtitle = "Расчётные листы, месячные отчёты и история",
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(appSectionSpacing()))

        ServiceActionCard(
            title = "Расчётный лист PDF",
            subtitle = "Подробный лист по текущему периоду и выбранной работе",
            action = "Открыть",
            onClick = onOpenPayrollPdf
        )
        ServiceActionCard(
            title = "Месячный отчёт",
            subtitle = "CSV/PDF по начислениям, выплатам, сменам и сверхурочке",
            action = "Открыть",
            onClick = onOpenMonthlyReport
        )
        ServiceActionCard(
            title = "История экспортов",
            subtitle = if (historyItems.isEmpty()) "Пока пусто" else "Сохранено снимков: ${historyItems.size}",
            action = "История",
            onClick = onOpenHistory
        )
        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

@Composable
private fun ServiceInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = appBlockSpacing()),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(todayBubblePadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun ServiceActionCard(
    title: String,
    subtitle: String,
    action: String,
    onClick: () -> Unit
) {
    ServiceInfoCard(title = title) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor()
        )
        TextButton(
            onClick = appHapticAction(onAction = onClick),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(action)
        }
    }
}

@Composable
private fun ServiceMetricRow(
    title: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun WorkflowSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = { value ->
                onCheckedChange(value)
            }
        )
    }
}
