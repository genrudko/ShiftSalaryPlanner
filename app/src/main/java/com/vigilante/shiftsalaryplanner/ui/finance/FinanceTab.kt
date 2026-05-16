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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult

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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        Text(
            text = state.periodLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
        Text(
            text = state.workplaceLabel,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor()
        )

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            FinanceSummaryCard(
                title = "Начислено",
                value = formatMoney(state.payroll.grossTotal),
                subtitle = "до НДФЛ",
                modifier = Modifier.weight(1f)
            )
            FinanceSummaryCard(
                title = "На руки",
                value = formatMoney(state.payroll.netTotal),
                subtitle = "итог периода",
                emphasize = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(appBlockSpacing()))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            FinanceSummaryCard(
                title = "НДФЛ",
                value = formatMoney(state.payroll.ndfl),
                subtitle = "удержано",
                modifier = Modifier.weight(1f)
            )
            FinanceSummaryCard(
                title = "Смен",
                value = state.detailedShiftStats.workedShiftCount.toString(),
                subtitle = "рабочих",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(appBlockSpacing()))

        AppExpressiveSurface(
            modifier = Modifier.fillMaxWidth(),
            tone = AppExpressiveSurfaceTone.SOFT,
            shape = RoundedCornerShape(appCardRadius())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(appCardPadding()),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
            ) {
                Text(
                    text = "Ближайшие выплаты",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Аванс: ${formatDate(state.paymentDates.advanceDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Зарплата: ${formatDate(state.paymentDates.salaryDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (state.todaySummary.isNotBlank() || state.tomorrowSummary.isNotBlank() || state.nextAlarmSummary.isNotBlank()) {
            Spacer(modifier = Modifier.height(appBlockSpacing()))
            AppExpressiveSurface(
                modifier = Modifier.fillMaxWidth(),
                tone = AppExpressiveSurfaceTone.PANEL,
                shape = RoundedCornerShape(appCardRadius())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(appCardPadding()),
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                ) {
                    Text(
                        text = "Сегодня и завтра",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.todaySummary.isNotBlank()) {
                        Text(text = "Сегодня: ${state.todaySummary}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.tomorrowSummary.isNotBlank()) {
                        Text(text = "Завтра: ${state.tomorrowSummary}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.nextAlarmSummary.isNotBlank()) {
                        Text(
                            text = "Следующий будильник: ${state.nextAlarmSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = appListSecondaryTextColor()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(appBlockSpacing()))
        ActualPaymentsComparisonCard(state = state)

        Spacer(modifier = Modifier.height(appScaledSpacing(28.dp)))
    }
}

@Composable
private fun ActualPaymentsComparisonCard(state: FinanceSummaryState) {
    var advanceText by rememberSaveable(state.actualAdvanceNet) {
        mutableStateOf(if (state.actualAdvanceNet > 0.0) formatDouble(state.actualAdvanceNet) else "")
    }
    var salaryText by rememberSaveable(state.actualSalaryNet) {
        mutableStateOf(if (state.actualSalaryNet > 0.0) formatDouble(state.actualSalaryNet) else "")
    }
    val actualAdvance = parseMoneyInput(advanceText)
    val actualSalary = parseMoneyInput(salaryText)
    val expectedTotal = state.payroll.netAdvanceAfterDeductions + state.payroll.netSalaryAfterDeductions
    val actualTotal = actualAdvance + actualSalary
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
            FinanceComparisonRow("Аванс", state.payroll.netAdvanceAfterDeductions, actualAdvance)
            FinanceComparisonRow("Зарплата", state.payroll.netSalaryAfterDeductions, actualSalary)
            if (hasActual) {
                FinanceComparisonRow("Итого", expectedTotal, actualTotal, emphasize = true)
                Text(
                    text = if (isWithinTolerance) {
                        "Разница в пределах допуска: ${formatMoney(delta)} из ${formatMoney(tolerance)}."
                    } else {
                        "Разница: ${formatMoney(delta)}. Допуск: ${formatMoney(tolerance)}. Проверь НДФЛ, удержания, доплаты и сокращённые дни."
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
                    label = "Аванс пришёл",
                    value = advanceText,
                    onValueChange = { advanceText = it },
                    modifier = Modifier.weight(1f)
                )
                CompactTextField(
                    label = "Зарплата пришла",
                    value = salaryText,
                    onValueChange = { salaryText = it },
                    modifier = Modifier.weight(1f)
                )
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
            text = "${formatMoney(expected)} / ${if (actual > 0.0) formatMoney(actual) else "не указано"}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
        )
    }
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
