package com.vigilante.shiftsalaryplanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.Composable
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
    val paymentDates: PaymentDates
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.26f),
        border = BorderStroke(1.dp, appPanelBorderColor())
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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(appCardRadius()),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
            border = BorderStroke(1.dp, appPanelBorderColor())
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

        Spacer(modifier = Modifier.height(appScaledSpacing(28.dp)))
    }
}

@Composable
private fun FinanceSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    emphasize: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
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
