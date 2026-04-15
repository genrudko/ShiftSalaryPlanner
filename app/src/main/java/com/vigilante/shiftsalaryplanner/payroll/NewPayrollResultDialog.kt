package com.vigilante.shiftsalaryplanner.payroll

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.formatMoney
import com.vigilante.shiftsalaryplanner.payroll.calculators.PayrollEngine

@Composable
fun NewPayrollResultDialog(
    result: PayrollEngine.MonthlyPayrollResult?,
    error: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (error != null) "❌ Ошибка расчёта" else "📊 Расчёт зарплаты",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (result != null) {
                    // Аванс
                    AccrualSection(title = "💰 Аванс (1-15)") {
                        AccrualRow("Начислено", result.advance.gross)
                        AccrualRow("НДФЛ", result.advance.ndfl, isDeduction = true)
                        AccrualRow("К выплате", result.advance.net, isTotal = true)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Основная часть
                    AccrualSection(title = "💵 Основная зарплата (16-31)") {
                        AccrualRow("Начислено", result.mainSalary.gross)
                        AccrualRow("НДФЛ", result.mainSalary.ndfl, isDeduction = true)
                        AccrualRow("К выплате", result.mainSalary.net, isTotal = true)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Итоги
                    AccrualSection(title = "📈 Итого за месяц") {
                        AccrualRow("Всего начислено", result.totalGross, isBold = true)
                        AccrualRow("НДФЛ всего", result.totalNdfl, isDeduction = true)
                        AccrualRow("На руки всего", result.totalNet, isTotal = true, isBold = true)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Справочно
                    Text(
                        text = "Смен: ${result.shiftsTotal}, Часов: ${result.hoursTotal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (result.vacationDays > 0) {
                        Text(
                            text = "Отпуск: ${result.vacationDays} дн.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (result.sickDays > 0) {
                        Text(
                            text = "Больничный: ${result.sickDays} дн.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun AccrualSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun AccrualRow(
    label: String,
    amount: Double,
    isDeduction: Boolean = false,
    isTotal: Boolean = false,
    isBold: Boolean = false
) {
    val color = when {
        isDeduction -> MaterialTheme.colorScheme.error
        isTotal -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isTotal || isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatMoney(amount),
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isTotal || isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}
