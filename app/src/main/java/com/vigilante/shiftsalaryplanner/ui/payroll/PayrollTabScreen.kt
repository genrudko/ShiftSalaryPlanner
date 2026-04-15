package com.vigilante.shiftsalaryplanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize

@Composable
fun PayrollTab(
    state: PayrollTabState,
    actions: PayrollTabActions,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(PayrollTabUiState()) }
    val scrollState = rememberScrollState()
    val bottomSpacing = appScaledSpacing(92.dp)
    val screenPadding = appScreenPadding()
    val showStickyTotals by remember {
        derivedStateOf { scrollState.value > 42 }
    }
    val modeAnimationMillis = appAnimationDurationMillis(180)
    val visibility = state.reportVisibilitySettings

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(screenPadding)
        ) {
            PayrollTopHeader(
                currentMonth = state.currentMonth,
                onPrevMonth = actions.onPrevMonth,
                onNextMonth = actions.onNextMonth,
                onPickMonth = actions.onPickMonth,
                viewMode = uiState.viewMode,
                onModeChange = { next ->
                    uiState = reducePayrollTabUiState(
                        state = uiState,
                        action = PayrollTabUiAction.SetViewMode(next)
                    )
                }
            )

            Spacer(modifier = Modifier.height(appSectionSpacing()))

            if (visibility.showPayrollWorkedStatsRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
                ) {
                    PayrollStatTile(
                        title = "Часы",
                        value = formatDouble(state.summary.workedHours),
                        subtitle = "оплачиваемые",
                        modifier = Modifier.weight(1f)
                    )
                    PayrollStatTile(
                        title = "Смены",
                        value = state.detailedShiftStats.workedShiftCount.toString(),
                        subtitle = "рабочие",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (visibility.showPayrollWorkedStatsRow && visibility.showPayrollPaymentsStatsRow) {
                Spacer(modifier = Modifier.height(appBlockSpacing()))
            }

            if (visibility.showPayrollPaymentsStatsRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
                ) {
                    PayrollStatTile(
                        title = "Аванс",
                        value = formatMoney(state.payroll.advanceAmount),
                        subtitle = formatDate(state.paymentDates.advanceDate),
                        modifier = Modifier.weight(1f)
                    )
                    PayrollStatTile(
                        title = "К зарплате",
                        value = formatMoney(state.payroll.salaryPaymentAmount),
                        subtitle = formatDate(state.paymentDates.salaryDate),
                        modifier = Modifier.weight(1f),
                        emphasize = true
                    )
                }
            }

            if (visibility.showPayrollWorkedStatsRow || visibility.showPayrollPaymentsStatsRow) {
                Spacer(modifier = Modifier.height(appSectionSpacing()))
            }

            AnimatedContent(
                targetState = uiState.viewMode,
                transitionSpec = {
                    val fadeSpec = tween<Float>(durationMillis = modeAnimationMillis)
                    (fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec))
                        .using(
                            SizeTransform(clip = false) { _, _ ->
                                tween<IntSize>(durationMillis = modeAnimationMillis)
                            }
                        )
                },
                label = "payroll-view-mode"
            ) { mode ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (mode == PayrollViewMode.DETAILED && visibility.showPayrollSummaryCard) {
                        SummaryCard(
                            summary = state.summary,
                            payroll = state.payroll,
                            annualOvertime = state.annualOvertime,
                            paymentDates = state.paymentDates,
                            housingPaymentLabel = state.housingPaymentLabel,
                            detailedShiftStats = state.detailedShiftStats,
                            isExpanded = state.isSummaryExpanded,
                            onToggle = actions.onToggleSummary,
                            onOpenSettings = actions.onOpenSettings
                        )
                        Spacer(modifier = Modifier.height(appSectionSpacing()))
                    }

                    PayrollSheetCard(
                        currentMonth = state.currentMonth,
                        payrollDetailedResult = state.payrollDetailedResult,
                        onOpenSettings = actions.onOpenSettings,
                        onOpenVisibilitySettings = actions.onOpenVisibilitySettings,
                        onExportPdf = { actions.onExportSheetPdf(state.currentMonth, state.payrollDetailedResult) },
                        visibilitySettings = state.reportVisibilitySettings,
                        compactMode = mode == PayrollViewMode.COMPACT
                    )
                }
            }

            Spacer(modifier = Modifier.height(bottomSpacing))
        }

        AnimatedVisibility(
            visible = showStickyTotals && visibility.showPayrollStickyTotalsBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PayrollStickyTotalsBar(
                payrollGross = state.payroll.grossTotal,
                payrollNdfl = state.payroll.ndfl,
                payrollNet = state.payroll.netTotal,
                modifier = Modifier
                    .padding(horizontal = screenPadding, vertical = appScaledSpacing(12.dp))
            )
        }
    }
}

@Composable
private fun PayrollTopHeader(
    currentMonth: java.time.YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (java.time.YearMonth) -> Unit,
    viewMode: PayrollViewMode,
    onModeChange: (PayrollViewMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding())
        ) {
            MonthHeader(
                currentMonth = currentMonth,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onPickMonth = onPickMonth
            )

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            PayrollModeSwitcher(
                viewMode = viewMode,
                onModeChange = onModeChange
            )
        }
    }
}

@Composable
private fun PayrollModeSwitcher(
    viewMode: PayrollViewMode,
    onModeChange: (PayrollViewMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        PayrollModeChip(
            text = "Компактный",
            selected = viewMode == PayrollViewMode.COMPACT,
            onClick = { onModeChange(PayrollViewMode.COMPACT) },
            modifier = Modifier.weight(1f)
        )
        PayrollModeChip(
            text = "Детальный",
            selected = viewMode == PayrollViewMode.DETAILED,
            onClick = { onModeChange(PayrollViewMode.DETAILED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PayrollModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = containerColor,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = appHapticAction(onAction = onClick))
                .padding(vertical = appScaledSpacing(10.dp)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PayrollStickyTotalsBar(
    payrollGross: Double,
    payrollNdfl: Double,
    payrollNet: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        color = appPanelColor(),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appScaledSpacing(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            StickyValueCell(
                title = "Начислено",
                value = formatMoney(payrollGross),
                modifier = Modifier.weight(1f)
            )
            StickyValueCell(
                title = "НДФЛ",
                value = formatMoney(payrollNdfl),
                modifier = Modifier.weight(1f)
            )
            StickyValueCell(
                title = "На руки",
                value = formatMoney(payrollNet),
                emphasize = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StickyValueCell(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        color = if (emphasize) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(7.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
