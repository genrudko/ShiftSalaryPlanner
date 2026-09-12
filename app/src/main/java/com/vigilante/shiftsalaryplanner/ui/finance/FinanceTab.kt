package com.vigilante.shiftsalaryplanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    EvolutionTextTabs(
        items = listOf(
            EvolutionTabItem(FinanceSubTab.SUMMARY, "Сводка"),
            EvolutionTabItem(FinanceSubTab.PAYROLL, "Расчёт"),
            EvolutionTabItem(FinanceSubTab.PAYMENTS, "Выплаты")
        ),
        selected = selectedSubTab,
        onSelected = onSelectSubTab,
        modifier = modifier
    )
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
            isPerShiftPayment = isPerShiftPayment
        )

        FinanceSummaryMetricRows(
            state = state,
            isPerShiftPayment = isPerShiftPayment
        )

        EvolutionActionRow(
            title = "Расчётный лист",
            subtitle = "Начисления, НДФЛ и удержания по строкам",
            trailingValue = "›",
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            tone = EvolutionIconTone.BRAND,
            onClick = onOpenPayroll
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
    isPerShiftPayment: Boolean
) {
    EvolutionHeroCard(
        title = if (isPerShiftPayment) "За смены к выплате" else "Ожидается на руки",
        value = value,
        icon = Icons.Rounded.Paid,
        iconDescription = "Сумма к выплате",
        semanticTone = EvolutionIconTone.FINANCE,
        supportingText = if (isPerShiftPayment) {
            "Итог за оплачиваемые смены после НДФЛ и удержаний"
        } else {
            "После НДФЛ и удержаний за выбранный период"
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FinanceSummaryMetricRows(
    state: FinanceSummaryState,
    isPerShiftPayment: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
        EvolutionActionRow(
            title = "Начислено",
            subtitle = "До НДФЛ и удержаний",
            trailingValue = formatFinanceMoney(state.payroll.grossTotal),
            icon = Icons.Rounded.Paid,
            tone = EvolutionIconTone.FINANCE
        )
        EvolutionActionRow(
            title = "НДФЛ",
            subtitle = "Удержано налога",
            trailingValue = formatFinanceMoney(state.payroll.ndfl),
            icon = Icons.Rounded.Calculate,
            tone = EvolutionIconTone.WARNING
        )
        if (isPerShiftPayment) {
            EvolutionActionRow(
                title = "Удержания",
                subtitle = "Кроме НДФЛ",
                trailingValue = formatFinanceMoney(state.payroll.deductionsTotal),
                icon = Icons.Rounded.Calculate,
                tone = EvolutionIconTone.WARNING
            )
            EvolutionActionRow(
                title = "Смены",
                subtitle = "${state.detailedShiftStats.workedShiftCount} смен",
                trailingValue = "${formatDouble(state.payroll.workedHours)} ч",
                icon = Icons.Rounded.WorkHistory,
                tone = EvolutionIconTone.BRAND
            )
            EvolutionActionRow(
                title = "За смены",
                subtitle = "После всех удержаний",
                trailingValue = formatFinanceMoney(state.payroll.netAfterDeductions),
                icon = Icons.Rounded.Payments,
                tone = EvolutionIconTone.FINANCE
            )
        } else {
            EvolutionActionRow(
                title = "Аванс",
                subtitle = formatDate(state.paymentDates.advanceDate),
                trailingValue = formatFinanceMoney(state.payroll.netAdvanceAfterDeductions),
                icon = Icons.Rounded.Payments,
                tone = EvolutionIconTone.FINANCE
            )
            EvolutionActionRow(
                title = "Остаток",
                subtitle = "${formatDate(state.paymentDates.salaryDate)} · удержания ${formatFinanceMoney(state.payroll.deductionsTotal)}",
                trailingValue = formatFinanceMoney(state.payroll.netSalaryAfterDeductions),
                icon = Icons.Rounded.Payments,
                tone = EvolutionIconTone.FINANCE
            )
        }
    }
}

@Composable
private fun FinancePayoutPlanCard(
    state: FinanceSummaryState,
    isPerShiftPayment: Boolean,
    onOpenPayments: () -> Unit
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

    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCardRadius()),
        shadowElevation = 0.dp
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
