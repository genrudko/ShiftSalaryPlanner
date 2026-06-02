package com.vigilante.shiftsalaryplanner.wear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import com.vigilante.shiftsalaryplanner.wear.sync.WearAlarms
import com.vigilante.shiftsalaryplanner.wear.sync.WearAssignment
import com.vigilante.shiftsalaryplanner.wear.sync.WearCalendarDay
import com.vigilante.shiftsalaryplanner.wear.sync.WearNote
import com.vigilante.shiftsalaryplanner.wear.sync.WearPayroll
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshot
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshotUiState
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshotRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearPlannerRoot()
            }
        }
    }
}

private enum class WearTab(val label: String) {
    TODAY("Сегодня"),
    CALENDAR("Календарь"),
    PAYROLL("ЗП"),
    ALARMS("Будильники"),
    NOTES("Заметки"),
    ASSISTANT("AI"),
    SYNC("Sync")
}

private enum class CalendarViewMode(val label: String) {
    LIST("Список"),
    MONTH("Месяц")
}

private const val RotaryScrollMultiplier = 7.0f
private const val RotaryHapticIntervalMillis = 34L

private fun performRotaryHaptic(view: View, vibrator: Vibrator?) {
    val hapticType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_TICK
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }
    view.performHapticFeedback(hapticType, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    if (vibrator?.hasVibrator() == true) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }
}

@Suppress("DEPRECATION")
private fun Context.defaultVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

@Composable
private fun WearPlannerRoot() {
    val context = LocalContext.current
    val view = LocalView.current
    val vibrator = remember(context) { context.defaultVibrator() }
    val repository = remember { WearSnapshotRepository(context) }
    DisposableEffect(repository) {
        repository.start()
        onDispose { repository.close() }
    }
    val state by repository.state.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(WearTab.TODAY) }
    var tabDirection by rememberSaveable { mutableStateOf(1) }
    var calendarViewMode by rememberSaveable { mutableStateOf(CalendarViewMode.LIST) }
    var payrollPrivate by rememberSaveable { mutableStateOf(false) }
    var scrollIndicatorVisible by remember { mutableStateOf(false) }
    var rotaryHapticDistance by remember { mutableStateOf(0f) }
    var lastRotaryHapticAt by remember { mutableStateOf(0L) }
    val swipeThresholdPx = with(LocalDensity.current) { 44.dp.toPx() }
    val backGestureEdgePx = with(LocalDensity.current) { 28.dp.toPx() }
    val overscrollLimitPx = with(LocalDensity.current) { 38.dp.toPx() }
    val rotaryMinStepPx = with(LocalDensity.current) { 8.dp.toPx() }
    val rotaryHapticStepPx = with(LocalDensity.current) { 14.dp.toPx() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val overscrollOffset = remember { Animatable(0f) }
    val scrollIndicatorAlpha by animateFloatAsState(
        targetValue = if (scrollIndicatorVisible && scrollState.maxValue > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "scroll_indicator_alpha"
    )
    val overscrollConnection = remember(scrollState, overscrollLimitPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val pull = available.y
                val atTop = scrollState.value == 0 && pull > 0f
                val atBottom = scrollState.value == scrollState.maxValue && pull < 0f
                if (atTop || atBottom) {
                    coroutineScope.launch {
                        val next = (overscrollOffset.value + pull * 0.32f)
                            .coerceIn(-overscrollLimitPx, overscrollLimitPx)
                        overscrollOffset.snapTo(next)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                overscrollOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(selectedTab, calendarViewMode) {
        scrollState.animateScrollTo(0, tween(durationMillis = 140, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            overscrollOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    LaunchedEffect(scrollState.value, scrollState.maxValue, scrollState.isScrollInProgress) {
        if (scrollState.maxValue > 0) {
            scrollIndicatorVisible = true
            if (!scrollState.isScrollInProgress) {
                delay(900)
                scrollIndicatorVisible = false
            }
        } else {
            scrollIndicatorVisible = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .pointerInput(selectedTab, swipeThresholdPx, backGestureEdgePx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startedAtBackEdge = down.position.x <= backGestureEdgePx
                    var totalX = 0f
                    var totalY = 0f
                    var switched = false

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y

                        if (!switched && abs(totalX) > swipeThresholdPx && abs(totalX) > abs(totalY) * 1.3f) {
                            when {
                                totalX < -swipeThresholdPx -> {
                                    tabDirection = 1
                                    selectedTab = nextTab(selectedTab)
                                    switched = true
                                }

                                totalX > swipeThresholdPx && !startedAtBackEdge -> {
                                    tabDirection = -1
                                    selectedTab = previousTab(selectedTab)
                                    switched = true
                                }
                            }
                        }
                    } while (event.changes.any { it.id == down.id && it.pressed })
                }
            }
            .padding(top = 10.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TabSwitcher(
            selectedTab = selectedTab,
            onPrevious = {
                tabDirection = -1
                selectedTab = previousTab(selectedTab)
            },
            onNext = {
                tabDirection = 1
                selectedTab = nextTab(selectedTab)
            }
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp)
                    .nestedScroll(overscrollConnection)
                    .graphicsLayer {
                        translationY = overscrollOffset.value
                        scaleY = 1f + (abs(overscrollOffset.value) / overscrollLimitPx) * 0.06f
                        scaleX = 1f - (abs(overscrollOffset.value) / overscrollLimitPx) * 0.012f
                    }
                    .requestFocusOnHierarchyActive()
                    .onRotaryScrollEvent { event ->
                        val rawDelta = event.verticalScrollPixels
                        val scaledDelta = rawDelta * RotaryScrollMultiplier
                        val requested = when {
                            scaledDelta > 0f && scaledDelta < rotaryMinStepPx -> rotaryMinStepPx
                            scaledDelta < 0f && scaledDelta > -rotaryMinStepPx -> -rotaryMinStepPx
                            else -> scaledDelta
                        }
                        scrollIndicatorVisible = true
                        val consumed = scrollState.dispatchRawDelta(requested)
                        if (abs(consumed) > 0.5f) {
                            rotaryHapticDistance += abs(consumed)
                            val now = SystemClock.uptimeMillis()
                            if (
                                rotaryHapticDistance >= rotaryHapticStepPx &&
                                now - lastRotaryHapticAt >= RotaryHapticIntervalMillis
                            ) {
                                performRotaryHaptic(view, vibrator)
                                rotaryHapticDistance = 0f
                                lastRotaryHapticAt = now
                            }
                        }
                        val remaining = requested - consumed
                        if (abs(remaining) > 0.5f) {
                            coroutineScope.launch {
                                val next = (overscrollOffset.value - remaining * 0.24f)
                                    .coerceIn(-overscrollLimitPx, overscrollLimitPx)
                                overscrollOffset.snapTo(next)
                                overscrollOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                        true
                    }
                    .focusable()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        val direction = if (tabDirection >= 0) 1 else -1
                        val enter = slideInHorizontally(
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        ) { fullWidth -> direction * fullWidth / 2 } + fadeIn(
                            animationSpec = tween(120)
                        )
                        val exit = slideOutHorizontally(
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        ) { fullWidth -> -direction * fullWidth / 2 } + fadeOut(
                            animationSpec = tween(90)
                        )
                        enter togetherWith exit using SizeTransform(clip = false)
                    },
                    label = "tab_content"
                ) { tab ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (tab) {
                            WearTab.TODAY -> TodayScreen(
                                snapshot = state.snapshot,
                                payrollPrivate = payrollPrivate
                            )
                            WearTab.CALENDAR -> CalendarScreen(
                                month = state.snapshot.month,
                                days = state.snapshot.calendar,
                                viewMode = calendarViewMode,
                                onViewModeChange = { calendarViewMode = it }
                            )
                            WearTab.PAYROLL -> PayrollScreen(
                                payroll = state.snapshot.payroll,
                                payrollPrivate = payrollPrivate,
                                onTogglePrivacy = { payrollPrivate = !payrollPrivate }
                            )
                            WearTab.ALARMS -> AlarmsScreen(
                                alarms = state.snapshot.alarms,
                                onToggleAll = repository::setAllAlarmsEnabled,
                                onToggleTemplate = repository::setTemplateAlarmEnabled
                            )
                            WearTab.NOTES -> NotesScreen(
                                notes = state.snapshot.notes,
                                onAddNote = repository::addNote
                            )
                            WearTab.ASSISTANT -> AssistantScreen(
                                snapshot = state.snapshot,
                                onAsk = repository::askAssistant
                            )
                            WearTab.SYNC -> SyncScreen(
                                state = state,
                                privacyEnabled = payrollPrivate,
                                onRefresh = repository::requestSnapshot,
                                onTogglePrivacy = { payrollPrivate = !payrollPrivate }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            ScrollIndicator(
                state = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .graphicsLayer { alpha = scrollIndicatorAlpha }
            )
        }
    }
}

@Composable
private fun TabSwitcher(
    selectedTab: WearTab,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        val available = maxWidth - 52.dp
        val switcherWidth = when {
            available < 112.dp -> available
            available > 132.dp -> 132.dp
            else -> available
        }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .width(switcherWidth),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIconButton(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = "Предыдущий раздел",
                onClick = onPrevious,
                size = 22
            )
            AppText(
                text = selectedTab.label,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                size = 13,
                weight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            NavIconButton(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = "Следующий раздел",
                onClick = onNext,
                size = 22
            )
        }
    }
}

@Composable
private fun TodayScreen(
    snapshot: WearSnapshot,
    payrollPrivate: Boolean
) {
    val today = snapshot.calendar.firstOrNull { it.isToday }
    val nextAlarm = snapshot.alarms.upcoming.firstOrNull()
    val todayNotes = snapshot.notes.filter { it.date == snapshot.today }

    TodayHeroCard(
        dateLabel = todayDateLabel(snapshot.today),
        todaySummary = snapshot.todaySummary.ifBlank { "Сегодня смен нет" },
        assignments = today?.assignments.orEmpty(),
        nextAlarmLabel = nextAlarm?.let { "${formatDateTime(it.triggerAtMillis)} · ${it.title}" }
            ?: "Будильники не запланированы"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TodayMiniCard(
            title = "Завтра",
            value = snapshot.tomorrowSummary.ifBlank { "смен нет" },
            icon = Icons.Rounded.Upcoming,
            modifier = Modifier.weight(1f)
        )
        TodayMiniCard(
            title = "Будильник",
            value = nextAlarm?.title ?: "нет",
            icon = Icons.Rounded.Alarm,
            modifier = Modifier.weight(1f)
        )
    }

    TodayWideCard(
        title = "Ближайшие выплаты",
        primary = "К выплате: ${formatMoney(snapshot.payroll.netTotal, payrollPrivate)}",
        secondary = "Аванс ${formatIsoDate(snapshot.payroll.advanceDate)} · ЗП ${formatIsoDate(snapshot.payroll.salaryDate)}",
        icon = Icons.Rounded.Payments,
        primaryColor = AppColors.Accent
    )

    TodayWideCard(
        title = "Заметки",
        primary = todayNotes.firstOrNull()?.let { note ->
            note.title.ifBlank { note.body.lineSequence().firstOrNull().orEmpty() }
        }
            ?: "На сегодня заметок нет",
        secondary = if (todayNotes.size > 1) "Всего: ${todayNotes.size}" else "Быстрые записи синхронизируются с телефоном",
        icon = Icons.AutoMirrored.Rounded.StickyNote2,
        primaryColor = AppColors.Text
    )
}

@Composable
private fun TodayHeroCard(
    dateLabel: String,
    todaySummary: String,
    assignments: List<WearAssignment>,
    nextAlarmLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.AccentPanel)
            .border(BorderStroke(1.dp, AppColors.Accent.copy(alpha = 0.55f)), RoundedCornerShape(18.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText("Сегодня", size = 15, weight = FontWeight.Bold)
                AppText(dateLabel, color = AppColors.Muted, size = 10, maxLines = 1)
            }
            TodayIconBadge(icon = Icons.Rounded.Today)
        }

        AppText(
            text = if (assignments.isEmpty()) todaySummary else "Смен сегодня: ${assignments.size}",
            color = if (assignments.isEmpty()) AppColors.Text else AppColors.AccentSoft,
            size = if (assignments.isEmpty()) 12 else 10,
            weight = FontWeight.Bold,
            maxLines = 1
        )

        if (assignments.isNotEmpty()) {
            TodayPrimaryShift(assignment = assignments.first())
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                assignments.drop(1).take(2).forEach { assignment ->
                    TodayShiftLine(assignment)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Alarm,
                contentDescription = "Будильник",
                tint = AppColors.AccentSoft,
                modifier = Modifier.size(13.dp)
            )
            AppText(
                text = nextAlarmLabel,
                color = AppColors.Muted,
                size = 10,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TodayPrimaryShift(assignment: WearAssignment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(AppColors.Panel.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, AppColors.PanelBorder.copy(alpha = 0.68f)), RoundedCornerShape(15.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShiftVisualBadge(assignment, size = 34, corner = 12)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AppText("Смена", color = AppColors.AccentSoft, size = 8, weight = FontWeight.Bold, maxLines = 1)
            AppText(
                text = assignment.title,
                size = compactTextSize(assignment.title, base = 12, min = 9),
                weight = FontWeight.Bold,
                maxLines = 2
            )
            if (assignment.workplaceName.isNotBlank()) {
                AppText("Работа", color = AppColors.Muted, size = 8, weight = FontWeight.SemiBold, maxLines = 1)
                AppText(
                    text = assignment.workplaceName,
                    color = AppColors.Text.copy(alpha = 0.88f),
                    size = compactTextSize(assignment.workplaceName, base = 10, min = 8),
                    maxLines = 1
                )
            }
            AppText(shiftTimeLabel(assignment), color = AppColors.Muted, size = 9, maxLines = 1)
        }
    }
}

@Composable
private fun TodayShiftLine(assignment: WearAssignment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Panel.copy(alpha = 0.68f))
            .border(BorderStroke(1.dp, AppColors.PanelBorder.copy(alpha = 0.62f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        ShiftVisualBadge(assignment, size = 24, corner = 9)
        Column(modifier = Modifier.weight(1f)) {
            AppText(assignment.title, size = 11, weight = FontWeight.Bold, maxLines = 1)
            AppText(
                text = listOf(assignment.workplaceName, shiftTimeLabel(assignment))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                color = AppColors.Muted,
                size = 9,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TodayMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Panel)
            .border(BorderStroke(1.dp, AppColors.PanelBorder.copy(alpha = 0.65f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AppColors.Accent,
            modifier = Modifier.size(14.dp)
        )
        AppText(title, color = AppColors.Muted, size = 9, maxLines = 1)
        AppText(value, size = 10, weight = FontWeight.SemiBold, maxLines = 2)
    }
}

@Composable
private fun TodayWideCard(
    title: String,
    primary: String,
    secondary: String,
    icon: ImageVector,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Panel)
            .border(BorderStroke(1.dp, AppColors.PanelBorder.copy(alpha = 0.65f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TodayIconBadge(icon = icon, size = 24, iconSize = 14)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AppText(title, color = AppColors.Muted, size = 9, maxLines = 1)
            AppText(primary, color = primaryColor, size = 11, weight = FontWeight.Bold, maxLines = 1)
            AppText(secondary, color = AppColors.Muted, size = 9, maxLines = 1)
        }
    }
}

@Composable
private fun TodayIconBadge(
    icon: ImageVector,
    size: Int = 28,
    iconSize: Int = 16
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(AppColors.Accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
private fun CalendarScreen(
    month: String,
    days: List<WearCalendarDay>,
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit
) {
    CalendarHeader(
        title = monthTitle(month, days),
        viewMode = viewMode,
        onViewModeChange = onViewModeChange
    )

    when (viewMode) {
        CalendarViewMode.LIST -> CalendarList(days)
        CalendarViewMode.MONTH -> CalendarMonthGrid(days)
    }
}

@Composable
private fun CalendarHeader(
    title: String,
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Panel)
            .border(BorderStroke(1.dp, AppColors.PanelBorder.copy(alpha = 0.65f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppText(
            text = title,
            modifier = Modifier.weight(1f),
            size = 12,
            weight = FontWeight.Bold,
            maxLines = 1
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            CalendarViewMode.entries.forEach { mode ->
                CompactSegmentButton(
                    mode = mode,
                    selected = viewMode == mode,
                    onClick = { onViewModeChange(mode) }
                )
            }
        }
    }
}

@Composable
private fun CompactSegmentButton(
    mode: CalendarViewMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) AppColors.Accent else AppColors.PanelAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (mode) {
                CalendarViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
                CalendarViewMode.MONTH -> Icons.Rounded.CalendarMonth
            },
            contentDescription = mode.label,
            tint = if (selected) Color.Black else AppColors.Text,
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun CalendarList(days: List<WearCalendarDay>) {
    val visibleDays = remember(days) {
        days.filter { it.isToday || it.assignments.isNotEmpty() || it.notesCount > 0 }
            .anchoredAtToday()
    }
    if (visibleDays.isEmpty()) {
        EmptyPanel("В этом месяце смены пока не назначены.")
    } else {
        visibleDays.forEach { day ->
            CalendarDayListPanel(day)
        }
    }
}

@Composable
private fun CalendarDayListPanel(day: WearCalendarDay) {
    Panel(accent = if (day.isToday) AppColors.Accent else AppColors.PanelBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppText(day.day.toString(), size = 15, weight = FontWeight.Bold)
                AppText(
                    dayName(day.dayOfWeek),
                    color = if (isWeekend(day)) AppColors.Weekend else AppColors.Muted,
                    size = 8
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (day.assignments.isEmpty()) {
                    AppText("Без смен", color = AppColors.Muted, size = 10)
                } else {
                    day.assignments.forEach { ShiftCompactLine(it) }
                }
                if (day.notesCount > 0) {
                    AppText("Заметок: ${day.notesCount}", color = AppColors.AccentSoft, size = 9)
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(days: List<WearCalendarDay>) {
    if (days.isEmpty()) {
        EmptyPanel("Календарь пока не синхронизирован.")
        return
    }

    val weeks = remember(days) { calendarWeeks(days) }
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС").forEachIndexed { index, label ->
                AppText(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = if (index >= 5) AppColors.Weekend else AppColors.Muted,
                    size = 7,
                    weight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                        )
                    } else {
                        CalendarMonthCell(
                            day = day,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthCell(
    day: WearCalendarDay,
    modifier: Modifier = Modifier
) {
    val hasAssignments = day.assignments.isNotEmpty()
    val hasNotes = day.notesCount > 0
    val background = when {
        day.isToday -> AppColors.Accent.copy(alpha = 0.22f)
        hasAssignments -> AppColors.Accent.copy(alpha = 0.12f)
        hasNotes -> AppColors.PanelAlt
        else -> Color.Transparent
    }
    val borderColor = if (day.isToday) AppColors.Accent else Color.Transparent
    val dayColor = when {
        day.isToday -> AppColors.Text
        isWeekend(day) -> AppColors.Weekend
        else -> AppColors.Muted
    }

    Column(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(7.dp))
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        AppText(
            text = day.day.toString(),
            color = dayColor,
            size = 9,
            weight = if (day.isToday) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            day.assignments.take(2).forEach { assignment ->
                ColorDot(assignment.colorHex, size = 4)
            }
            if (hasNotes) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(AppColors.Warning)
                )
            }
        }
    }
}

@Composable
private fun PayrollScreen(
    payroll: WearPayroll,
    payrollPrivate: Boolean,
    onTogglePrivacy: () -> Unit
) {
    PayrollHero(
        payroll = payroll,
        payrollPrivate = payrollPrivate,
        onTogglePrivacy = onTogglePrivacy
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        PayrollPaymentTile(
            title = "Аванс",
            value = formatMoney(payroll.advance, payrollPrivate),
            subtitle = formatIsoDate(payroll.advanceDate),
            modifier = Modifier.weight(1f)
        )
        PayrollPaymentTile(
            title = "К зарплате",
            value = formatMoney(payroll.salary, payrollPrivate),
            subtitle = formatIsoDate(payroll.salaryDate),
            emphasize = true,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        PayrollStatTile(
            title = "Часы",
            value = formatHours(payroll.workedHours),
            subtitle = "оплач.",
            modifier = Modifier.weight(1f)
        )
        PayrollStatTile(
            title = "Ночные",
            value = formatHours(payroll.nightHours),
            subtitle = "часы",
            modifier = Modifier.weight(1f)
        )
    }

    Panel {
        SectionTitle("Расчёт")
        MetricLine("Начислено", formatMoney(payroll.grossTotal, payrollPrivate), AppColors.Text)
        MetricLine("НДФЛ", formatMoney(payroll.ndfl, payrollPrivate), AppColors.Muted)
        MetricLine("На руки", formatMoney(payroll.netTotal, payrollPrivate), AppColors.Accent)
    }
}

@Composable
private fun PayrollHero(
    payroll: WearPayroll,
    payrollPrivate: Boolean,
    onTogglePrivacy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.AccentPanel)
            .border(BorderStroke(1.dp, AppColors.Accent.copy(alpha = 0.55f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = payroll.periodLabel.ifBlank { "Текущий месяц" },
                color = AppColors.AccentSoft,
                size = 10,
                weight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Panel.copy(alpha = 0.62f))
                    .clickable(onClick = onTogglePrivacy)
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                AppText(
                    text = if (payrollPrivate) "Показать" else "Скрыть",
                    color = AppColors.Muted,
                    size = 8,
                    maxLines = 1
                )
            }
        }
        AppText(
            text = formatMoney(payroll.netTotal, payrollPrivate),
            color = AppColors.Accent,
            size = 23,
            weight = FontWeight.Bold,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PayrollInlineMetric(
                title = "Начислено",
                value = formatMoney(payroll.grossTotal, payrollPrivate),
                modifier = Modifier.weight(1f)
            )
            PayrollInlineMetric(
                title = "НДФЛ",
                value = formatMoney(payroll.ndfl, payrollPrivate),
                valueColor = AppColors.Muted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PayrollPaymentTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (emphasize) AppColors.Accent.copy(alpha = 0.16f) else AppColors.Panel)
            .border(
                BorderStroke(
                    1.dp,
                    if (emphasize) AppColors.Accent.copy(alpha = 0.55f) else AppColors.PanelBorder
                ),
                RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AppText(title, color = AppColors.Muted, size = 9, maxLines = 1)
        AppText(
            value,
            color = if (emphasize) AppColors.Accent else AppColors.Text,
            size = 13,
            weight = FontWeight.Bold,
            maxLines = 1
        )
        AppText(subtitle, color = AppColors.Muted, size = 9, maxLines = 1)
    }
}

@Composable
private fun PayrollStatTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(AppColors.PanelAlt.copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, AppColors.PanelBorder.copy(alpha = 0.75f)), RoundedCornerShape(13.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        AppText(title, color = AppColors.Muted, size = 9, maxLines = 1)
        AppText(value, color = AppColors.Text, size = 13, weight = FontWeight.Bold, maxLines = 1)
        AppText(subtitle, color = AppColors.Muted, size = 9, maxLines = 1)
    }
}

@Composable
private fun PayrollInlineMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppColors.Text
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.Panel.copy(alpha = 0.58f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        AppText(title, color = AppColors.Muted, size = 8, maxLines = 1)
        AppText(value, color = valueColor, size = 10, weight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun AlarmsScreen(
    alarms: WearAlarms,
    onToggleAll: (Boolean) -> Unit,
    onToggleTemplate: (String, Boolean) -> Unit
) {
    Panel {
        SectionTitle("Следующие")
        if (alarms.upcoming.isEmpty()) {
            AppText("Нет запланированных срабатываний", color = AppColors.Muted, size = 10)
        } else {
            alarms.upcoming.take(3).forEach { alarm ->
                AppText(formatDateTime(alarm.triggerAtMillis), color = AppColors.AccentSoft, size = 9)
                AppText(alarm.title, size = 11, maxLines = 2)
                Spacer(Modifier.height(2.dp))
            }
        }
    }

    AlarmAutoSetupStrip(
        alarms = alarms,
        onToggleAll = onToggleAll
    )

    SectionTitle("По сменам")
    alarms.templates.take(12).forEach { template ->
        Panel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorDot(template.colorHex)
                Column(modifier = Modifier.weight(1f)) {
                    AppText(template.title, size = 11, weight = FontWeight.SemiBold, maxLines = 1)
                    AppText(
                        text = listOf(template.displayCode, template.start, "${template.alarmsEnabledCount} шт.")
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        color = AppColors.Muted,
                        size = 9,
                        maxLines = 1
                    )
                }
                PillButton(
                    label = if (template.enabled) "Вкл" else "Выкл",
                    selected = template.enabled,
                    onClick = { onToggleTemplate(template.shiftCode, !template.enabled) }
                )
            }
        }
    }
}

@Composable
private fun AlarmAutoSetupStrip(
    alarms: WearAlarms,
    onToggleAll: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Panel)
            .border(BorderStroke(1.dp, AppColors.PanelBorder.copy(alpha = 0.65f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Alarm,
            contentDescription = null,
            tint = if (alarms.enabled) AppColors.Accent else AppColors.Muted,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AppText(
                text = if (alarms.enabled) "Автопостановка: вкл" else "Автопостановка: выкл",
                color = if (alarms.enabled) AppColors.Accent else AppColors.Muted,
                size = 10,
                weight = FontWeight.Bold,
                maxLines = 1
            )
            AppText(
                text = if (alarms.wearMirrorEnabled) {
                    "Wear: ${wearAlarmSoundLabel(alarms.wearSoundMode)}"
                } else {
                    "Wear-сигнал выключен"
                },
                color = AppColors.Muted,
                size = 8,
                maxLines = 1
            )
        }
        TinyActionChip(
            label = if (alarms.enabled) "Выкл" else "Вкл",
            selected = alarms.enabled,
            onClick = { onToggleAll(!alarms.enabled) }
        )
    }
}

@Composable
private fun NotesScreen(
    notes: List<WearNote>,
    onAddNote: (String) -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val launchVoiceNote = rememberSpeechInputLauncher("Новая заметка") { spoken ->
        draft = listOf(draft.trim(), spoken.trim())
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
    SectionTitle("Новая заметка")
    WatchInput(
        value = draft,
        placeholder = "Текст заметки",
        onValueChange = { draft = it }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PillButton(
            label = "Диктовать",
            selected = false,
            onClick = launchVoiceNote
        )
        PillButton(
            label = "Сохранить",
            selected = draft.isNotBlank(),
            onClick = {
                val body = draft.trim()
                if (body.isNotBlank()) {
                    onAddNote(body)
                    draft = ""
                }
            }
        )
    }

    SectionTitle("Последние")
    if (notes.isEmpty()) {
        EmptyPanel("Заметок пока нет.")
    } else {
        notes.take(10).forEach { note ->
            Panel {
                AppText(formatIsoDate(note.date), color = AppColors.AccentSoft, size = 10)
                AppText(note.title.ifBlank { note.body.lineSequence().firstOrNull().orEmpty() }, size = 12, weight = FontWeight.SemiBold)
                if (note.body.isNotBlank()) {
                    AppText(note.body, color = AppColors.Muted, size = 11, maxLines = 3)
                }
            }
        }
    }
}

@Composable
private fun AssistantScreen(
    snapshot: WearSnapshot,
    onAsk: (String) -> Unit
) {
    var prompt by rememberSaveable { mutableStateOf("") }
    val launchAssistantVoice = rememberSpeechInputLauncher("Спроси ассистента") { spoken ->
        val text = spoken.trim()
        if (text.isNotBlank() && snapshot.assistant.configured) {
            onAsk(text)
            prompt = ""
        } else {
            prompt = text
        }
    }

    GeminiHero(snapshot)

    GeminiInput(
        value = prompt,
        onValueChange = { prompt = it },
        enabled = snapshot.assistant.configured,
        onVoice = launchAssistantVoice,
        onSend = {
            val text = prompt.trim()
            if (text.isNotBlank()) {
                onAsk(text)
                prompt = ""
            }
        }
    )

    val reply = snapshot.assistant.lastReply
    val error = snapshot.assistant.lastError
    when {
        error.isNotBlank() -> GeminiAnswerBubble(
            title = "Не получилось",
            text = error,
            accent = AppColors.Warning,
            muted = false
        )
        reply.isNotBlank() -> GeminiAnswerBubble(
            title = "Ответ",
            text = reply,
            accent = AppColors.GeminiBlue
        )
        snapshot.assistant.configured -> GeminiAnswerBubble(
            title = "Готов",
            text = "Спроси про смены, выплаты, заметки или попроси добавить запись.",
            accent = AppColors.GeminiBlue,
            muted = true
        )
        else -> GeminiAnswerBubble(
            title = "Нужна настройка",
            text = "API-ключ и провайдер задаются на телефоне.",
            accent = AppColors.Warning,
            muted = true
        )
    }
}

@Composable
private fun GeminiHero(snapshot: WearSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.GeminiPanel)
            .border(BorderStroke(1.dp, AppColors.GeminiBlue.copy(alpha = 0.55f)), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AppColors.GeminiBlue.copy(alpha = 0.20f))
                    .border(BorderStroke(1.dp, AppColors.GeminiBlue.copy(alpha = 0.58f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = AppColors.GeminiBlue,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                AppText("Gemini", size = 17, weight = FontWeight.Bold, maxLines = 1)
                AppText(
                    text = if (snapshot.assistant.configured) {
                        snapshot.assistant.provider.ifBlank { "AI включён" }
                    } else {
                        "Настрой на телефоне"
                    },
                    color = if (snapshot.assistant.configured) AppColors.GeminiMint else AppColors.Muted,
                    size = 9,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardVoice,
                contentDescription = null,
                tint = AppColors.GeminiMint,
                modifier = Modifier.size(19.dp)
            )
        }
        AppText(
            text = "Спроси естественно",
            color = AppColors.Muted,
            size = 10,
            maxLines = 1
        )
    }
}

@Composable
private fun GeminiInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onVoice: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Panel)
            .border(BorderStroke(1.dp, AppColors.PanelBorder), RoundedCornerShape(20.dp))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(
                color = AppColors.Text,
                fontSize = 11.sp,
                lineHeight = 15.sp
            ),
            cursorBrush = SolidColor(AppColors.GeminiBlue),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 30.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        AppText(
                            text = if (enabled) "Сообщение" else "Настрой на телефоне",
                            color = AppColors.Muted,
                            size = 10,
                            maxLines = 1
                        )
                    }
                    inner()
                }
            }
        )
        Button(
            onClick = onVoice,
            modifier = Modifier.size(34.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = AppColors.PanelAlt,
                contentColor = AppColors.GeminiMint
            ),
            contentPadding = ButtonDefaults.CompactButtonContentPadding
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardVoice,
                contentDescription = "Голос",
                modifier = Modifier.size(16.dp)
            )
        }
        Button(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier.size(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.GeminiBlue,
                contentColor = Color.Black,
                disabledContainerColor = AppColors.PanelAlt,
                disabledContentColor = AppColors.Muted
            ),
            contentPadding = ButtonDefaults.CompactButtonContentPadding
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Отправить",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun GeminiAnswerBubble(
    title: String,
    text: String,
    accent: Color,
    muted: Boolean = false
) {
    Panel(accent = accent) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            SectionTitle(title)
        }
        AppText(
            text = text,
            color = if (muted) AppColors.Muted else AppColors.Text,
            size = 11,
            maxLines = 7
        )
    }
}

@Composable
private fun rememberSpeechInputLauncher(
    prompt: String,
    onResult: (String) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) onResult(spoken)
        }
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        runCatching { launcher.launch(intent) }
    }
}

@Composable
private fun SyncScreen(
    state: WearSnapshotUiState,
    privacyEnabled: Boolean,
    onRefresh: () -> Unit,
    onTogglePrivacy: () -> Unit
) {
    Panel(accent = if (state.connectedNodes > 0) AppColors.Accent else AppColors.Warning) {
        SectionTitle("Синхронизация")
        MetricLine(
            label = "Телефон",
            value = if (state.connectedNodes > 0) "рядом" else "нет связи",
            valueColor = if (state.connectedNodes > 0) AppColors.Accent else AppColors.Warning
        )
        MetricLine("Статус", state.commandStatus, AppColors.Text)
        MetricLine("Обновлено", formatDateTime(state.snapshot.generatedAt), AppColors.Muted)
        PillButton(
            label = "Обновить",
            selected = false,
            onClick = onRefresh
        )
    }

    Panel {
        SectionTitle("Приватность")
        AppText(
            text = if (privacyEnabled) "Суммы скрыты на экранах часов." else "Суммы видны на экранах часов.",
            color = if (privacyEnabled) AppColors.AccentSoft else AppColors.Muted,
            size = 11
        )
        PillButton(
            label = if (privacyEnabled) "Показать суммы" else "Скрыть суммы",
            selected = privacyEnabled,
            onClick = onTogglePrivacy
        )
    }

    Panel {
        SectionTitle("Wear-сервисы")
        AppText("Tile: Сегодня", size = 11)
        AppText("Complication: смена и зарплата", size = 11)
        AppText("Голос: заметки и AI", size = 11)
    }
}

@Composable
private fun ShiftRow(assignment: WearAssignment) {
    Panel(accent = parseColor(assignment.colorHex)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShiftVisualBadge(assignment, size = 24, corner = 9)
            Column(modifier = Modifier.weight(1f)) {
                AppText(assignment.title, size = 13, weight = FontWeight.Bold, maxLines = 1)
                AppText(
                    text = listOf(
                        assignment.workplaceName,
                        assignment.displayCode,
                        shiftTimeLabel(assignment)
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    color = AppColors.Muted,
                    size = 10,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ShiftCompactLine(assignment: WearAssignment) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ShiftVisualBadge(assignment, size = 20, corner = 8)
        Column(modifier = Modifier.weight(1f)) {
            AppText(assignment.title, size = 12, weight = FontWeight.SemiBold, maxLines = 1)
            AppText(shiftTimeLabel(assignment), color = AppColors.Muted, size = 10, maxLines = 1)
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AppText(label, color = AppColors.Muted, size = 11)
        AppText(value, color = valueColor, size = 12, weight = FontWeight.Bold)
    }
}

@Composable
private fun WatchInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = AppColors.Text,
            fontSize = 11.sp,
            lineHeight = 15.sp
        ),
        cursorBrush = SolidColor(AppColors.Accent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Panel)
            .border(BorderStroke(1.dp, AppColors.PanelBorder), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        decorationBox = { inner ->
            if (value.isBlank()) {
                AppText(placeholder, color = AppColors.Muted, size = 10)
            }
            inner()
        }
    )
}

@Composable
private fun Panel(
    accent: Color = AppColors.PanelBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Panel)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.65f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun EmptyPanel(text: String) {
    Panel {
        AppText(text, color = AppColors.Muted, size = 11)
    }
}

@Composable
private fun SectionTitle(text: String) {
    AppText(text, color = AppColors.AccentSoft, size = 9, weight = FontWeight.Bold, maxLines = 1)
}

@Composable
private fun PillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(28.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent,
                contentColor = Color.Black
            )
        } else {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = AppColors.PanelAlt,
                contentColor = AppColors.Text
            )
        },
        contentPadding = ButtonDefaults.CompactButtonContentPadding
    ) {
        AppText(
            text = label,
            color = if (selected) Color.Black else AppColors.Text,
            size = 10,
            weight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun TinyActionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppColors.Accent else AppColors.PanelAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = label,
            color = if (selected) Color.Black else AppColors.Text,
            size = 9,
            weight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NavIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 22
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppColors.Text,
            modifier = Modifier.size((size - 4).dp)
        )
    }
}

@Composable
private fun ColorDot(hex: String, size: Int = 9) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(parseColor(hex))
    )
}

@Composable
private fun ShiftVisualBadge(
    assignment: WearAssignment,
    size: Int = 24,
    corner: Int = 9
) {
    val background = parseColor(assignment.colorHex)
    val fallbackCode = assignment.displayCode.ifBlank { assignment.shiftCode }
    val glyph = wearShiftGlyph(assignment.iconKey, fallbackCode)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(background)
            .border(
                BorderStroke(1.dp, readableOn(background).copy(alpha = 0.18f)),
                RoundedCornerShape(corner.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = glyph,
            color = readableOn(background),
            size = wearShiftGlyphSize(glyph, size),
            weight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Text,
    size: Int = 12,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            lineHeight = (size + 4).sp,
            fontWeight = weight,
            textAlign = textAlign
        )
    )
}

private fun previousTab(current: WearTab): WearTab {
    val tabs = WearTab.entries
    val index = tabs.indexOf(current).takeIf { it >= 0 } ?: 0
    return tabs[(index - 1 + tabs.size) % tabs.size]
}

private fun nextTab(current: WearTab): WearTab {
    val tabs = WearTab.entries
    val index = tabs.indexOf(current).takeIf { it >= 0 } ?: 0
    return tabs[(index + 1) % tabs.size]
}

private fun shiftTimeLabel(assignment: WearAssignment): String {
    val time = when {
        assignment.start.isNotBlank() && assignment.end.isNotBlank() -> "${assignment.start}-${assignment.end}"
        assignment.start.isNotBlank() -> assignment.start
        else -> ""
    }
    val hours = assignment.paidHours.takeIf { it > 0.0 }?.let { formatHours(it) }.orEmpty()
    return listOf(time, hours).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun formatMoney(value: Double, privateMode: Boolean = false): String {
    if (privateMode) return "•••• ₽"
    return String.format(Locale.forLanguageTag("ru-RU"), "%,.0f ₽", value).replace(',', ' ')
}

private fun formatHours(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toInt()} ч"
    } else {
        "$rounded ч"
    }
}

private fun formatIsoDate(value: String): String {
    return runCatching {
        val date = LocalDate.parse(value)
        "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthValue.toString().padStart(2, '0')}"
    }.getOrDefault(value.ifBlank { "не задано" })
}

private fun formatDateTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""
    val dateTime = Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return "${dateTime.dayOfMonth.toString().padStart(2, '0')}.${dateTime.monthValue.toString().padStart(2, '0')} ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}

private fun wearAlarmSoundLabel(mode: String): String {
    return when (mode) {
        "RINGTONE" -> "рингтон"
        "NOTIFICATION" -> "уведомление"
        "SILENT" -> "без звука"
        else -> "будильник"
    }
}

private fun todayDateLabel(value: String): String {
    return runCatching {
        val date = LocalDate.parse(value)
        "${date.dayOfMonth} ${monthNameGenitive(date.monthValue)} · ${dayName(date.dayOfWeek.value)}"
    }.getOrDefault(value.ifBlank { "сегодня" })
}

private fun monthTitle(month: String, days: List<WearCalendarDay>): String {
    val source = month.ifBlank { days.firstOrNull()?.date?.take(7).orEmpty() }
    return runCatching {
        val yearMonth = YearMonth.parse(source)
        "${monthName(yearMonth.monthValue)} ${yearMonth.year}"
    }.getOrDefault(source.ifBlank { "Месяц" })
}

private fun monthName(value: Int): String {
    return when (value) {
        1 -> "Январь"
        2 -> "Февраль"
        3 -> "Март"
        4 -> "Апрель"
        5 -> "Май"
        6 -> "Июнь"
        7 -> "Июль"
        8 -> "Август"
        9 -> "Сентябрь"
        10 -> "Октябрь"
        11 -> "Ноябрь"
        12 -> "Декабрь"
        else -> "Месяц"
    }
}

private fun monthNameGenitive(value: Int): String {
    return when (value) {
        1 -> "января"
        2 -> "февраля"
        3 -> "марта"
        4 -> "апреля"
        5 -> "мая"
        6 -> "июня"
        7 -> "июля"
        8 -> "августа"
        9 -> "сентября"
        10 -> "октября"
        11 -> "ноября"
        12 -> "декабря"
        else -> "месяца"
    }
}

private fun calendarWeeks(days: List<WearCalendarDay>): List<List<WearCalendarDay?>> {
    if (days.isEmpty()) return emptyList()
    val sortedDays = days.sortedBy { it.day }
    val leadingBlanks = (sortedDays.first().dayOfWeek - 1).coerceIn(0, 6)
    val padded = buildList {
        repeat(leadingBlanks) { add(null) }
        addAll(sortedDays)
        while (size % 7 != 0) add(null)
    }
    return padded.chunked(7)
}

private fun List<WearCalendarDay>.anchoredAtToday(): List<WearCalendarDay> {
    val todayIndex = indexOfFirst { it.isToday }
    if (todayIndex <= 0) return this
    return drop(todayIndex) + take(todayIndex)
}

private fun isWeekend(day: WearCalendarDay): Boolean {
    return day.dayOfWeek >= 6
}

private fun dayName(value: Int): String {
    return when (value) {
        1 -> "пн"
        2 -> "вт"
        3 -> "ср"
        4 -> "чт"
        5 -> "пт"
        6 -> "сб"
        7 -> "вс"
        else -> ""
    }
}

private fun parseColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(AppColors.Accent)
}

private fun compactTextSize(text: String, base: Int, min: Int): Int {
    return when {
        text.length > 34 -> min
        text.length > 24 -> (base - 2).coerceAtLeast(min)
        text.length > 16 -> (base - 1).coerceAtLeast(min)
        else -> base
    }
}

private fun readableOn(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance > 0.58f) Color(0xFF07110A) else AppColors.Text
}

private fun wearShiftGlyph(iconKey: String, fallbackCode: String): String {
    val key = iconKey.trim()
    return when {
        key.startsWith("EMOJI:") -> key.removePrefix("EMOJI:").ifBlank { fallbackCode }
        key == "SUN" -> "☀"
        key == "MOON" -> "☾"
        key == "EIGHT" -> "8"
        key == "HOME" -> "⌂"
        key == "OT" -> "ОТ"
        key == "SICK" -> "✚"
        key == "STAR" -> "★"
        key == "CHECK" || key == "TASK" -> "✓"
        key == "TEXT" -> fallbackCode
        else -> fallbackCode.ifBlank { key }.take(3)
    }
}

private fun wearShiftGlyphSize(glyph: String, badgeSize: Int): Int {
    val maxByBadge = (badgeSize * 0.54f).roundToInt().coerceAtLeast(8)
    val byLength = when {
        glyph.length <= 1 -> 16
        glyph.length == 2 -> 12
        glyph.length == 3 -> 10
        else -> 8
    }
    return minOf(maxByBadge, byLength)
}

private object AppColors {
    val Background = Color(0xFF050708)
    val Panel = Color(0xFF11171A)
    val AccentPanel = Color(0xFF102018)
    val PanelAlt = Color(0xFF1B2328)
    val PanelBorder = Color(0xFF2B373D)
    val Text = Color(0xFFF2F7F4)
    val Muted = Color(0xFFA8B4AE)
    val Accent = Color(0xFF7AE582)
    val AccentSoft = Color(0xFFB7F7D0)
    val GeminiPanel = Color(0xFF10161D)
    val GeminiBlue = Color(0xFF8AB4F8)
    val GeminiMint = Color(0xFFB7F7D0)
    val Warning = Color(0xFFFFC857)
    val Weekend = Color(0xFFFF6B6B)
}
