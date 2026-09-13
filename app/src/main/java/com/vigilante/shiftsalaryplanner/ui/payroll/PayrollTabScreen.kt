package com.vigilante.shiftsalaryplanner

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode
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
    val payMode = runCatching { PayMode.valueOf(state.payMode) }.getOrElse { PayMode.HOURLY }
    val paymentScheduleMode = runCatching { PaymentScheduleMode.valueOf(state.paymentScheduleMode) }
        .getOrElse { PaymentScheduleMode.TWICE_MONTHLY }
    val isPerShiftPayment = payMode == PayMode.PER_SHIFT || paymentScheduleMode == PaymentScheduleMode.PER_SHIFT

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
                onPickRangeEnd = actions.onPickRangeEnd
            )

            Spacer(modifier = Modifier.height(appSectionSpacing()))

            PayrollCalculationOverviewCard(
                payroll = state.payroll,
                detailedShiftStats = state.detailedShiftStats,
                amountViewMode = uiState.amountViewMode,
                isPerShiftPayment = isPerShiftPayment
            )

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            PayrollDisplayOptionsBar(
                viewMode = uiState.viewMode,
                amountViewMode = uiState.amountViewMode,
                onViewModeChange = { next ->
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
                            isPerShiftPayment = isPerShiftPayment,
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
                        onOpenDeductions = actions.onOpenDeductions,
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
                payrollPayable = state.payroll.netAfterDeductions,
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
    onPickRangeEnd: (LocalDate) -> Unit
) {
    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
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
                        onSwitchWorkplace = onChangeWorkplace,
                        useEvolution = true
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
                        onPickMonth = onPickMonth,
                        useEvolution = true
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

        }
    }
}

@Composable
private fun PayrollCalculationOverviewCard(
    payroll: com.vigilante.shiftsalaryplanner.payroll.PayrollResult,
    detailedShiftStats: DetailedShiftStats,
    amountViewMode: PayrollAmountViewMode,
    isPerShiftPayment: Boolean
) {
    val isGross = amountViewMode == PayrollAmountViewMode.GROSS
    val heroTitle = if (isGross) "Начислено" else if (isPerShiftPayment) "К выплате за смены" else "К выплате"
    val heroValue = if (isGross) payroll.grossTotal else payroll.netAfterDeductions

    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
        EvolutionHeroCard(
            title = heroTitle,
            value = formatFinanceMoney(heroValue),
            icon = Icons.Rounded.Paid,
            iconDescription = heroTitle,
            semanticTone = EvolutionIconTone.FINANCE,
            supportingText = "${detailedShiftStats.workedShiftCount} смен • ${formatDouble(payroll.workedHours)} ч",
            modifier = Modifier.fillMaxWidth()
        )
        EvolutionSurface(
            modifier = Modifier.fillMaxWidth(),
            role = EvolutionSurfaceRole.SOFT,
            shape = RoundedCornerShape(appCornerRadius(18.dp)),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(appCardPadding()),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                CalculationOverviewMetric("Начислено", formatFinanceMoney(payroll.grossTotal), Modifier.weight(1f))
                CalculationOverviewMetric("НДФЛ", formatFinanceMoney(payroll.ndfl), Modifier.weight(1f))
                CalculationOverviewMetric("Удержания", formatFinanceMoney(payroll.deductionsTotal), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CalculationOverviewMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp))) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PayrollDisplayOptionsBar(
    viewMode: PayrollViewMode,
    amountViewMode: PayrollAmountViewMode,
    onViewModeChange: (PayrollViewMode) -> Unit,
    onAmountViewModeChange: (PayrollAmountViewMode) -> Unit
) {
    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(appScaledSpacing(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PayrollDisplayOption(
                title = "Вид",
                value = if (viewMode == PayrollViewMode.DETAILED) "Подробно" else "Компактно",
                onClick = {
                    onViewModeChange(if (viewMode == PayrollViewMode.DETAILED) PayrollViewMode.COMPACT else PayrollViewMode.DETAILED)
                },
                modifier = Modifier.weight(1f)
            )
            PayrollDisplayOption(
                title = "Суммы",
                value = if (amountViewMode == PayrollAmountViewMode.NET) "На руки" else "До НДФЛ",
                onClick = {
                    onAmountViewModeChange(if (amountViewMode == PayrollAmountViewMode.NET) PayrollAmountViewMode.GROSS else PayrollAmountViewMode.NET)
                },
                modifier = Modifier.weight(1f),
                emphasized = true
            )
        }
    }
}

@Composable
private fun PayrollDisplayOption(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    EvolutionSurface(
        modifier = modifier
            .clip(RoundedCornerShape(appCornerRadius(12.dp)))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        role = if (emphasized) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(6.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(1.dp))
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
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
    EvolutionSurface(
        modifier = modifier
            .clip(RoundedCornerShape(appCornerRadius(10.dp)))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        role = if (selected) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(10.dp)),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(vertical = appScaledSpacing(7.dp)),
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
    EvolutionSurface(
        modifier = modifier
            .clip(RoundedCornerShape(appCornerRadius(12.dp)))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(9.dp)),
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
    EvolutionSurface(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
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
    EvolutionSurface(
        modifier = modifier
            .clip(RoundedCornerShape(appCornerRadius(14.dp)))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        role = if (selected) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(vertical = appScaledSpacing(10.dp)),
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
    payrollPayable: Double,
    modifier: Modifier = Modifier
) {
    EvolutionSurface(
        modifier = modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.FLOATING,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = appCardPadding(), vertical = appScaledSpacing(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            StickyValueCell("Начислено", formatFinanceMoney(payrollGross), Modifier.weight(1f))
            StickyValueCell("НДФЛ", formatFinanceMoney(payrollNdfl), Modifier.weight(1f))
            StickyValueCell("На руки", formatFinanceMoney(payrollPayable), Modifier.weight(1f), emphasize = true)
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
    EvolutionSurface(
        modifier = modifier,
        role = if (emphasize) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(7.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
