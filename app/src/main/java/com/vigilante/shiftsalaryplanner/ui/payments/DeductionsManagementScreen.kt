package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.DeductionMode
import com.vigilante.shiftsalaryplanner.payroll.DeductionType
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import com.vigilante.shiftsalaryplanner.payroll.displayName
import com.vigilante.shiftsalaryplanner.payroll.effectiveLimitPercent
import com.vigilante.shiftsalaryplanner.payroll.effectiveQueue
import com.vigilante.shiftsalaryplanner.payroll.resolvedLegalKind
import com.vigilante.shiftsalaryplanner.settings.Workplace

@Composable
fun DeductionsManagementScreen(
    deductions: List<PayrollDeduction>,
    workplaces: List<Workplace>,
    selectedWorkplaceId: String,
    onSwitchWorkplace: (String) -> Unit,
    onBack: () -> Unit,
    onAddDeduction: () -> Unit,
    onEditDeduction: (PayrollDeduction) -> Unit,
    onDeleteDeduction: (PayrollDeduction) -> Unit,
    onToggleActive: (PayrollDeduction, Boolean) -> Unit
) {
    val activeCount = deductions.count { it.active }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FixedScreenHeader(
                title = "Удержания",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(appScreenPadding())
            ) {
                CalendarWorkplaceSwitcher(
                    workplaces = workplaces,
                    activeWorkplaceId = selectedWorkplaceId,
                    onSwitchWorkplace = onSwitchWorkplace
                )

                Spacer(modifier = Modifier.height(appBlockSpacing()))

                CompactDeductionsSummaryCard(
                    totalCount = deductions.size,
                    activeCount = activeCount,
                    onAdd = onAddDeduction
                )

                Spacer(modifier = Modifier.height(appBlockSpacing()))

                if (deductions.isEmpty()) {
                    CompactDeductionsEmptyCard()
                } else {
                    val sortedDeductions = deductions.sortedWith(
                        compareBy<PayrollDeduction> { it.effectiveQueue().sortOrder }
                            .thenBy { it.title.lowercase() }
                    )
                    sortedDeductions.forEachIndexed { index, deduction ->
                        DeductionItemCard(
                            deduction = deduction,
                            onEdit = { onEditDeduction(deduction) },
                            onDelete = { onDeleteDeduction(deduction) },
                            onToggleActive = { checked -> onToggleActive(deduction, checked) }
                        )
                        if (index != sortedDeductions.lastIndex) {
                            Spacer(modifier = Modifier.height(appBlockSpacing()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDeductionsSummaryCard(
    totalCount: Int,
    activeCount: Int,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appPanelColor(), RoundedCornerShape(appCardRadius()))
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(appCardRadius()))
            .padding(appCardPadding())
    ) {
        Text(
            text = "Сводка",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
        PaymentInfoRow("Всего удержаний", totalCount.toString())
        PaymentInfoRow("Активных", activeCount.toString())

        Spacer(modifier = Modifier.height(appBlockSpacing()))
        CompactDeductionsActionButton(
            text = "Добавить удержание",
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CompactDeductionsEmptyCard() {
    AppEmptyCard(
        title = "Пока пусто",
        message = "Добавь алименты, исполнительные удержания или другие удержания после НДФЛ."
    )
}

@Composable
private fun CompactDeductionsActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(appLargeButtonHeight())
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DeductionItemCard(
    deduction: PayrollDeduction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appPanelColor(), RoundedCornerShape(appCardRadius()))
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(appCardRadius()))
            .padding(appCardPadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deduction.title.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = deductionTypeLabel(deduction.type),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${deduction.resolvedLegalKind().displayName()} • ${deduction.effectiveQueue().displayName()} • до ${formatPercent(deduction.effectiveLimitPercent())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
                Text(
                    text = deductionValueLabel(deduction),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = buildString {
                        append(if (deduction.applyToAdvance) "Аванс" else "")
                        if (deduction.applyToAdvance && deduction.applyToSalary) append(" • ")
                        append(if (deduction.applyToSalary) "Зарплата" else "")
                        if (!deduction.applyToAdvance && !deduction.applyToSalary) append("Не применяется")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Switch(
                        checked = deduction.active,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleActive(checked)
                        },
                        modifier = Modifier.scale(0.58f)
                    )
                }
                Text(
                    text = if (deduction.active) "Вкл" else "Выкл",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (deduction.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = deduction.note,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = appHapticAction(onAction = onDelete)) {
                Text("Удалить")
            }
            TextButton(onClick = appHapticAction(onAction = onEdit)) {
                Text("Изменить")
            }
        }
    }
}

@Composable
private fun deductionTypeLabel(typeName: String): String {
    val type = runCatching { DeductionType.valueOf(typeName) }.getOrElse { DeductionType.OTHER }
    return when (type) {
        DeductionType.ALIMONY -> "Алименты"
        DeductionType.ENFORCEMENT -> "Исполнительное производство"
        DeductionType.OTHER -> "Прочее удержание"
    }
}

@Composable
private fun deductionValueLabel(deduction: PayrollDeduction): String {
    val mode = runCatching { DeductionMode.valueOf(deduction.mode) }.getOrElse { DeductionMode.FIXED }
    return when (mode) {
        DeductionMode.SHARE -> "Доля: ${deduction.shareLabel.ifBlank { formatPercent(deduction.value * 100.0) }}"
        DeductionMode.PERCENT -> "Процент: ${formatPercent(deduction.value)}"
        DeductionMode.FIXED -> "Сумма: ${formatMoney(deduction.value)}"
    }
}

private fun formatPercent(value: Double): String {
    val normalized = if (kotlin.math.abs(value - value.toInt()) < 0.0001) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
            .replace('.', ',')
            .trimEnd('0')
            .trimEnd(',')
    }
    return "$normalized%"
}
