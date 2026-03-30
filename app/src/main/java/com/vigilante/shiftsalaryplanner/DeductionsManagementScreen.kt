package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.DeductionMode
import com.vigilante.shiftsalaryplanner.payroll.DeductionType
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import com.vigilante.shiftsalaryplanner.payroll.displayName
import com.vigilante.shiftsalaryplanner.payroll.effectiveLimitPercent
import com.vigilante.shiftsalaryplanner.payroll.effectiveQueue
import com.vigilante.shiftsalaryplanner.payroll.resolvedLegalKind

@Composable
fun DeductionsManagementScreen(
    deductions: List<PayrollDeduction>,
    onBack: () -> Unit,
    onAddDeduction: () -> Unit,
    onEditDeduction: (PayrollDeduction) -> Unit,
    onDeleteDeduction: (PayrollDeduction) -> Unit,
    onToggleActive: (PayrollDeduction, Boolean) -> Unit
) {
    val activeCount = deductions.count { it.active }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appInnerSurfaceColor())
    ) {
        FixedScreenHeader(
            title = "Удержания",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            InfoCard(title = "Сводка") {
                PaymentInfoRow("Всего удержаний", deductions.size.toString())
                PaymentInfoRow("Активных", activeCount.toString())
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAddDeduction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить удержание")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (deductions.isEmpty()) {
                InfoCard(title = "Пока пусто") {
                    Text(
                        text = "Добавь алименты, исполнительные удержания или другие удержания после НДФЛ.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                deductions
                    .sortedWith(
                        compareBy<PayrollDeduction> { it.effectiveQueue().sortOrder }
                            .thenBy { it.title.lowercase() }
                    )
                    .forEach { deduction ->
                        DeductionItemCard(
                            deduction = deduction,
                            onEdit = { onEditDeduction(deduction) },
                            onDelete = { onDeleteDeduction(deduction) },
                            onToggleActive = { checked -> onToggleActive(deduction, checked) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appPanelColor(), RoundedCornerShape(20.dp))
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deduction.title.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deductionTypeLabel(deduction.type),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${deduction.resolvedLegalKind().displayName()} • ${deduction.effectiveQueue().displayName()} • до ${formatPercent(deduction.effectiveLimitPercent())}",
                    style = MaterialTheme.typography.bodySmall
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
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = deduction.active,
                    onCheckedChange = onToggleActive
                )
                Text(
                    text = if (deduction.active) "Активно" else "Отключено",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (deduction.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = deduction.note,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
            TextButton(onClick = onEdit) {
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