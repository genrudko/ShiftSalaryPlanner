package com.vigilante.shiftsalaryplanner

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.Workplace
import java.time.LocalDate
import java.time.YearMonth

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
                periodMode = state.periodMode,
                selectedWorkplaceId = state.selectedWorkplaceId,
                workplaceOptions = state.workplaceOptions,
                currentMonth = state.currentMonth,
                periodStartDate = state.periodStartDate,
                periodEndDate = state.periodEndDate,
                onChangePeriodMode = actions.onChangePeriodMode,
                onChangeWorkplace = actions.onChangeWorkplace,
                onPrevMonth = actions.onPrevMonth,
                onNextMonth = actions.onNextMonth,
                onPickMonth = actions.onPickMonth,
                onPrevYear = actions.onPrevYear,
                onNextYear = actions.onNextYear,
                onPickYear = actions.onPickYear,
                onShiftRangeBackward = actions.onShiftRangeBackward,
                onShiftRangeForward = actions.onShiftRangeForward,
                onPickRangeStart = actions.onPickRangeStart,
                onPickRangeEnd = actions.onPickRangeEnd,
                viewMode = uiState.viewMode,
                amountViewMode = uiState.amountViewMode,
                onModeChange = { next ->
                    uiState = reducePayrollTabUiState(
                        state = uiState,
                        action = PayrollTabUiAction.SetViewMode(next)
                    )
                },
                onAmountViewModeChange = { next ->
                    uiState = reducePayrollTabUiState(
                        state = uiState,
                        action = PayrollTabUiAction.SetAmountViewMode(next)
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
                        value = formatHours(state.summary.workedHours),
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
                        value = formatMoney(
                            if (uiState.amountViewMode == PayrollAmountViewMode.GROSS) {
                                state.payroll.advanceGrossAmount
                            } else {
                                state.payroll.netAdvanceAfterDeductions
                            }
                        ),
                        subtitle = if (state.periodMode == PayrollPeriodMode.MONTH) {
                            "${formatDate(state.paymentDates.advanceDate)} • ${if (uiState.amountViewMode == PayrollAmountViewMode.GROSS) "до НДФЛ" else "на руки"}"
                        } else {
                            if (uiState.amountViewMode == PayrollAmountViewMode.GROSS) "за период • до НДФЛ" else "за период • на руки"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PayrollStatTile(
                        title = "К зарплате",
                        value = formatMoney(
                            if (uiState.amountViewMode == PayrollAmountViewMode.GROSS) {
                                state.payroll.salaryGrossAmount
                            } else {
                                state.payroll.netSalaryAfterDeductions
                            }
                        ),
                        subtitle = if (state.periodMode == PayrollPeriodMode.MONTH) {
                            "${formatDate(state.paymentDates.salaryDate)} • ${if (uiState.amountViewMode == PayrollAmountViewMode.GROSS) "до НДФЛ" else "на руки"}"
                        } else {
                            if (uiState.amountViewMode == PayrollAmountViewMode.GROSS) "за период • до НДФЛ" else "за период • на руки"
                        },
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
                            periodMode = state.periodMode,
                            periodLabel = state.periodLabel,
                            periodStartDate = state.periodStartDate,
                            periodEndDate = state.periodEndDate,
                            summary = state.summary,
                            payroll = state.payroll,
                            annualOvertime = state.annualOvertime,
                            paymentDates = state.paymentDates,
                            housingPaymentLabel = state.housingPaymentLabel,
                            detailedShiftStats = state.detailedShiftStats,
                            amountViewMode = uiState.amountViewMode,
                            isExpanded = state.isSummaryExpanded,
                            onToggle = actions.onToggleSummary,
                            onOpenSettings = actions.onOpenSettings
                        )
                        Spacer(modifier = Modifier.height(appSectionSpacing()))
                    }

                    PayrollSheetCard(
                        periodLabel = state.periodLabel,
                        payrollDetailedResult = state.payrollDetailedResult,
                        onOpenSettings = actions.onOpenSettings,
                        onOpenDiagnostics = actions.onOpenDiagnostics,
                        onOpenVisibilitySettings = actions.onOpenVisibilitySettings,
                        onExportPdf = {
                            actions.onExportSheetPdf(
                                state.periodLabel,
                                state.periodFileLabel,
                                state.payrollDetailedResult
                            )
                        },
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
    periodMode: PayrollPeriodMode,
    selectedWorkplaceId: String,
    workplaceOptions: List<PayrollWorkplaceOption>,
    currentMonth: YearMonth,
    periodStartDate: LocalDate,
    periodEndDate: LocalDate,
    onChangePeriodMode: (PayrollPeriodMode) -> Unit,
    onChangeWorkplace: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onPickYear: (Int) -> Unit,
    onShiftRangeBackward: () -> Unit,
    onShiftRangeForward: () -> Unit,
    onPickRangeStart: (LocalDate) -> Unit,
    onPickRangeEnd: (LocalDate) -> Unit,
    viewMode: PayrollViewMode,
    amountViewMode: PayrollAmountViewMode,
    onModeChange: (PayrollViewMode) -> Unit,
    onAmountViewModeChange: (PayrollAmountViewMode) -> Unit
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
            PeriodModeSwitcher(
                periodMode = periodMode,
                onChangePeriodMode = onChangePeriodMode
            )

            if (workplaceOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(appBlockSpacing()))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CalendarWorkplaceSwitcher(
                        workplaces = workplaceOptions.map { option ->
                            Workplace(
                                id = option.id,
                                name = option.title
                            )
                        },
                        activeWorkplaceId = selectedWorkplaceId,
                        onSwitchWorkplace = onChangeWorkplace
                    )
                }
            }

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            when (periodMode) {
                PayrollPeriodMode.MONTH -> {
                    MonthHeader(
                        currentMonth = currentMonth,
                        onPrevMonth = onPrevMonth,
                        onNextMonth = onNextMonth,
                        onPickMonth = onPickMonth
                    )
                }

                PayrollPeriodMode.YEAR -> {
                    YearHeader(
                        year = periodEndDate.year,
                        onPrevYear = onPrevYear,
                        onNextYear = onNextYear,
                        onPickYear = onPickYear
                    )
                }

                PayrollPeriodMode.RANGE -> {
                    DateRangeHeader(
                        periodStartDate = periodStartDate,
                        periodEndDate = periodEndDate,
                        onShiftRangeBackward = onShiftRangeBackward,
                        onShiftRangeForward = onShiftRangeForward,
                        onPickRangeStart = onPickRangeStart,
                        onPickRangeEnd = onPickRangeEnd
                    )
                }
            }

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            PayrollModeSwitcher(
                viewMode = viewMode,
                onModeChange = onModeChange
            )

            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))

            PayrollAmountModeSwitcher(
                amountViewMode = amountViewMode,
                onModeChange = onAmountViewModeChange
            )
        }
    }
}

@Composable
private fun PeriodModeSwitcher(
    periodMode: PayrollPeriodMode,
    onChangePeriodMode: (PayrollPeriodMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        PayrollModeChip(
            text = "Месяц",
            selected = periodMode == PayrollPeriodMode.MONTH,
            onClick = { onChangePeriodMode(PayrollPeriodMode.MONTH) },
            modifier = Modifier.weight(1f)
        )
        PayrollModeChip(
            text = "Диапазон",
            selected = periodMode == PayrollPeriodMode.RANGE,
            onClick = { onChangePeriodMode(PayrollPeriodMode.RANGE) },
            modifier = Modifier.weight(1f)
        )
        PayrollModeChip(
            text = "Год",
            selected = periodMode == PayrollPeriodMode.YEAR,
            onClick = { onChangePeriodMode(PayrollPeriodMode.YEAR) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun YearHeader(
    year: Int,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onPickYear: (Int) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeriodNavButton(
            icon = Icons.Rounded.ChevronLeft,
            contentDescription = "Предыдущий год",
            onClick = onPrevYear
        )

        Text(
            text = "$year год",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clickable(
                    onClick = appHapticAction(
                        onAction = {
                            DatePickerDialog(
                                context,
                                { _, pickedYear, _, _ -> onPickYear(pickedYear) },
                                year,
                                0,
                                1
                            ).show()
                        }
                    )
                )
        )

        PeriodNavButton(
            icon = Icons.Rounded.ChevronRight,
            contentDescription = "Следующий год",
            onClick = onNextYear
        )
    }
}

@Composable
private fun DateRangeHeader(
    periodStartDate: LocalDate,
    periodEndDate: LocalDate,
    onShiftRangeBackward: () -> Unit,
    onShiftRangeForward: () -> Unit,
    onPickRangeStart: (LocalDate) -> Unit,
    onPickRangeEnd: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val rangeTitle = if (periodStartDate == periodEndDate) {
        formatDate(periodStartDate)
    } else {
        "${formatDate(periodStartDate)} — ${formatDate(periodEndDate)}"
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeriodNavButton(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = "Предыдущий диапазон",
                onClick = onShiftRangeBackward
            )

            Text(
                text = rangeTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = appScaledSpacing(8.dp))
            )

            PeriodNavButton(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = "Следующий диапазон",
                onClick = onShiftRangeForward
            )
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(8.dp)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            DateRangeChip(
                text = "С ${formatDate(periodStartDate)}",
                modifier = Modifier.weight(1f),
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onPickRangeStart(LocalDate.of(year, month + 1, day))
                        },
                        periodStartDate.year,
                        periodStartDate.monthValue - 1,
                        periodStartDate.dayOfMonth
                    ).show()
                }
            )
            DateRangeChip(
                text = "По ${formatDate(periodEndDate)}",
                modifier = Modifier.weight(1f),
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onPickRangeEnd(LocalDate.of(year, month + 1, day))
                        },
                        periodEndDate.year,
                        periodEndDate.monthValue - 1,
                        periodEndDate.dayOfMonth
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun PayrollAmountModeSwitcher(
    amountViewMode: PayrollAmountViewMode,
    onModeChange: (PayrollAmountViewMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
    ) {
        PayrollAmountChip(
            text = "До НДФЛ",
            selected = amountViewMode == PayrollAmountViewMode.GROSS,
            onClick = { onModeChange(PayrollAmountViewMode.GROSS) },
            modifier = Modifier.weight(1f)
        )
        PayrollAmountChip(
            text = "На руки",
            selected = amountViewMode == PayrollAmountViewMode.NET,
            onClick = { onModeChange(PayrollAmountViewMode.NET) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PayrollAmountChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(10.dp)),
        color = containerColor,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.8f))
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = appHapticAction(onAction = onClick))
                .padding(vertical = appScaledSpacing(7.dp)),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DateRangeChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = appHapticAction(onAction = onClick))
                .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(9.dp)),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PeriodNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(10.dp))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
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
