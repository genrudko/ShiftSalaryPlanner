package com.vigilante.shiftsalaryplanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode

enum class FinanceSubTab {
    SUMMARY,
    PAYROLL,
    PAYMENTS
}

data class FinanceSummaryState(
    val periodLabel: String,
    val workplaceLabel: String,
    val payroll: PayrollResult,
    val detailedShiftStats: DetailedShiftStats,
    val paymentDates: PaymentDates,
    val payMode: String,
    val paymentScheduleMode: String,
    val todaySummary: String = "",
    val tomorrowSummary: String = "",
    val nextAlarmSummary: String = "",
    val actualAdvanceNet: Double = 0.0,
    val actualSalaryNet: Double = 0.0,
    val paymentDifferenceToleranceRub: Double = 100.0,
    val onSaveActualPayments: (Double, Double) -> Unit = { _, _ -> }
)

@Composable
fun FinanceTab(
    selectedSubTab: FinanceSubTab,
    onSelectSubTab: (FinanceSubTab) -> Unit,
    summaryState: FinanceSummaryState,
    payrollContent: @Composable () -> Unit,
    paymentsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val enterAnimationMillis = appAnimationDurationMillis(160)
    val exitAnimationMillis = appAnimationDurationMillis(130)

    Column(modifier = modifier.fillMaxSize()) {
        FinanceSubTabSwitcher(
            selectedSubTab = selectedSubTab,
            onSelectSubTab = onSelectSubTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appScreenPadding(), vertical = appScaledSpacing(8.dp))
        )

        AnimatedContent(
            targetState = selectedSubTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(enterAnimationMillis)) togetherWith
                        fadeOut(animationSpec = tween(exitAnimationMillis)))
            },
            label = "finance-sub-tab",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { tab ->
            when (tab) {
                FinanceSubTab.SUMMARY -> FinanceSummaryTab(
                    state = summaryState,
                    onOpenPayroll = { onSelectSubTab(FinanceSubTab.PAYROLL) },
                    onOpenPayments = { onSelectSubTab(FinanceSubTab.PAYMENTS) },
                    modifier = Modifier.fillMaxSize()
                )
                FinanceSubTab.PAYROLL -> payrollContent()
                FinanceSubTab.PAYMENTS -> paymentsContent()
            }
        }
    }
}

@Composable
private fun FinanceSubTabSwitcher(
    selectedSubTab: FinanceSubTab,
    onSelectSubTab: (FinanceSubTab) -> Unit,
    modifier: Modifier = Modifier
) {
    AppExpressiveSurface(
        modifier = modifier,
        tone = AppExpressiveSurfaceTone.PANEL,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appScaledSpacing(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            FinanceSubTabButton(
                label = "Сводка",
                selected = selectedSubTab == FinanceSubTab.SUMMARY,
                onClick = { onSelectSubTab(FinanceSubTab.SUMMARY) },
                modifier = Modifier.weight(1f)
            )
            FinanceSubTabButton(
                label = "Расчёт",
                selected = selectedSubTab == FinanceSubTab.PAYROLL,
                onClick = { onSelectSubTab(FinanceSubTab.PAYROLL) },
                modifier = Modifier.weight(1f)
            )
            FinanceSubTabButton(
                label = "Выплаты",
                selected = selectedSubTab == FinanceSubTab.PAYMENTS,
                onClick = { onSelectSubTab(FinanceSubTab.PAYMENTS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FinanceSubTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                shape = RoundedCornerShape(appCornerRadius(12.dp))
            )
            .clickable(onClick = appHapticAction(onAction = onClick))
            .padding(vertical = appScaledSpacing(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FinanceSummaryTab(
    state: FinanceSummaryState,
    onOpenPayroll: () -> Unit,
    onOpenPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPerShiftPayment = remember(state.payMode, state.paymentScheduleMode) {
        runCatching { PayMode.valueOf(state.payMode) }.getOrElse { PayMode.HOURLY } == PayMode.PER_SHIFT ||
            runCatching { PaymentScheduleMode.valueOf(state.paymentScheduleMode) }
                .getOrElse { PaymentScheduleMode.TWICE_MONTHLY } == PaymentScheduleMode.PER_SHIFT
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp))) {
            Text(
                text = state.periodLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.workplaceLabel,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }

        FinancePayableHeroCard(
            value = formatFinanceMoney(state.payroll.netAfterDeductions),
            onOpenPayroll = onOpenPayroll
        )

        FinanceKeyMetrics(
            gross = state.payroll.grossTotal,
            tax = state.payroll.ndfl,
            deductions = state.payroll.deductionsTotal,
            shifts = state.detailedShiftStats.workedShiftCount,
            workedHours = state.payroll.workedHours,
            isPerShiftPayment = isPerShiftPayment
        )

        FinancePayoutPlanCard(
            state = state,
            isPerShiftPayment = isPerShiftPayment,
            onOpenPayments = onOpenPayments
        )

        ActualPaymentsComparisonCard(state = state, isPerShiftPayment = isPerShiftPayment)

        Spacer(modifier = Modifier.height(appScaledSpacing(20.dp)))
    }
}

@Composable
private fun FinancePayableHeroCard(
    value: String,
    onOpenPayroll: () -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.ACCENT,
        shape = RoundedCornerShape(appCornerRadius(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Text(
                text = "Ожидается к выплате",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "После НДФЛ и удержаний за выбранный период",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            FilledTonalButton(
                onClick = appHapticAction(onAction = onOpenPayroll),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Открыть расчёт")
            }
        }
    }
}

@Composable
private fun FinanceKeyMetrics(
    gross: Double,
    tax: Double,
    deductions: Double,
    shifts: Int,
    workedHours: Double,
    isPerShiftPayment: Boolean
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.SOFT,
        shape = RoundedCornerShape(appCardRadius())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                FinanceMetricItem("Начислено", formatFinanceMoney(gross), Modifier.weight(1f))
                FinanceMetricItem("НДФЛ", formatFinanceMoney(tax), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                FinanceMetricItem(
                    title = "Удержания",
                    value = formatFinanceMoney(deductions),
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricItem(
                    title = if (isPerShiftPayment) "Смены" else "Смены · часы",
                    value = if (isPerShiftPayment) {
                        shifts.toString()
                    } else {
                        "$shifts · ${formatDouble(workedHours)} ч"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FinanceMetricItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp))
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = appListSecondaryTextColor()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FinancePayoutPlanCard(
    state: FinanceSummaryState,
    isPerShiftPayment: Boolean,
    onOpenPayments: () -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.PANEL,
        shape = RoundedCornerShape(appCardRadius())
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
                Text(
                    text = "План выплат",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = appHapticAction(onAction = onOpenPayments)) {
                    Text("Все выплаты")
                }
            }
            if (isPerShiftPayment) {
                FinancePayoutRow(
                    title = "За смены",
                    date = "по датам смен",
                    amount = formatFinanceMoney(state.payroll.netAfterDeductions)
                )
            } else {
                FinancePayoutRow(
                    title = "Аванс",
                    date = formatDate(state.paymentDates.advanceDate),
                    amount = formatFinanceMoney(state.payroll.netAdvanceAfterDeductions)
                )
                FinancePayoutRow(
                    title = "Зарплата",
                    date = formatDate(state.paymentDates.salaryDate),
                    amount = formatFinanceMoney(state.payroll.netSalaryAfterDeductions)
                )
            }
        }
    }
}

@Composable
private fun FinancePayoutRow(
    title: String,
    date: String,
    amount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(date, style = MaterialTheme.typography.bodySmall, color = appListSecondaryTextColor())
        }
        Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActualPaymentsComparisonCard(state: FinanceSummaryState, isPerShiftPayment: Boolean) {
    var advanceText by rememberSaveable(state.actualAdvanceNet) {
        mutableStateOf(if (state.actualAdvanceNet > 0.0) formatDouble(state.actualAdvanceNet) else "")
    }
    var salaryText by rememberSaveable(state.actualSalaryNet) {
        mutableStateOf(if (state.actualSalaryNet > 0.0) formatDouble(state.actualSalaryNet) else "")
    }
    val actualAdvance = parseMoneyInput(advanceText)
    val actualSalary = parseMoneyInput(salaryText)
    val expectedTotal = if (isPerShiftPayment) {
        state.payroll.netAfterDeductions
    } else {
        state.payroll.netAdvanceAfterDeductions + state.payroll.netSalaryAfterDeductions
    }
    val actualTotal = if (isPerShiftPayment) actualAdvance else actualAdvance + actualSalary
    val hasActual = actualAdvance > 0.0 || actualSalary > 0.0
    val delta = actualTotal - expectedTotal
    val tolerance = state.paymentDifferenceToleranceRub.coerceAtLeast(0.0)
    val isWithinTolerance = kotlin.math.abs(delta) <= tolerance

    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = AppExpressiveSurfaceTone.PANEL,
        shape = RoundedCornerShape(appCardRadius()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            Text(
                text = "Ожидалось / пришло",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (isPerShiftPayment) {
                FinanceComparisonRow("За смены", state.payroll.netAfterDeductions, actualTotal)
            } else {
                FinanceComparisonRow("Аванс", state.payroll.netAdvanceAfterDeductions, actualAdvance)
                FinanceComparisonRow("Зарплата", state.payroll.netSalaryAfterDeductions, actualSalary)
            }
            if (hasActual) {
                FinanceComparisonRow("Итого", expectedTotal, actualTotal, emphasize = true)
                Text(
                    text = if (isWithinTolerance) {
                        "Разница в пределах допуска: ${formatFinanceMoney(delta)} из ${formatFinanceMoney(tolerance)}."
                    } else if (isPerShiftPayment) {
                        "Разница: ${formatFinanceMoney(delta)}. Допуск: ${formatFinanceMoney(tolerance)}. Проверь суммы “Оплата за смену” в шаблонах."
                    } else {
                        "Разница: ${formatFinanceMoney(delta)}. Допуск: ${formatFinanceMoney(tolerance)}. Проверь НДФЛ, удержания, доплаты и сокращённые дни."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWithinTolerance) appListSecondaryTextColor() else MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                CompactTextField(
                    label = if (isPerShiftPayment) "За смены пришло" else "Аванс пришёл",
                    value = advanceText,
                    onValueChange = { advanceText = it },
                    modifier = Modifier.weight(1f)
                )
                if (!isPerShiftPayment) {
                    CompactTextField(
                        label = "Зарплата пришла",
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            TextButton(
                onClick = appHapticAction {
                    state.onSaveActualPayments(actualAdvance, actualSalary)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Сохранить факт")
            }
        }
    }
}

@Composable
private fun FinanceComparisonRow(
    title: String,
    expected: Double,
    actual: Double,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${formatFinanceMoney(expected)} / ${if (actual > 0.0) formatFinanceMoney(actual) else "не указано"}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
        )
    }
}

internal fun formatFinanceMoney(value: Double): String {
    val roundedCents = java.lang.Math.round(value * 100.0)
    val negative = roundedCents < 0L
    val absoluteCents = kotlin.math.abs(roundedCents)
    val whole = absoluteCents / 100L
    val fraction = absoluteCents % 100L
    val groupedWhole = whole.toString()
        .reversed()
        .chunked(3)
        .joinToString("\u00A0")
        .reversed()
    val numeric = if (fraction == 0L) {
        groupedWhole
    } else {
        "$groupedWhole,${fraction.toString().padStart(2, '0')}"
    }
    val signed = if (negative) "-$numeric" else numeric
    return "$signed ${currentCurrencySymbol()}"
}

private fun parseMoneyInput(value: String): Double {
    return value
        .replace(" ", "")
        .replace(',', '.')
        .toDoubleOrNull()
        ?.coerceAtLeast(0.0)
        ?: 0.0
}

@Composable
private fun FinanceSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    emphasize: Boolean = false,
    modifier: Modifier = Modifier
) {
    AppExpressiveSurface(
        modifier = modifier,
        tone = if (emphasize) AppExpressiveSurfaceTone.ACCENT else AppExpressiveSurfaceTone.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
                color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = appListSecondaryTextColor()
            )
        }
    }
}
