package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LeaveBenefitsSettingsScreen(
    sickYear1: Int,
    sickYear2: Int,
    vacationAccruals12MonthsText: String,
    onVacationAccrualsChange: (String) -> Unit,
    computedVacationAverageDaily: Double,
    sickIncomeYear1Text: String,
    onSickIncomeYear1Change: (String) -> Unit,
    sickIncomeYear2Text: String,
    onSickIncomeYear2Change: (String) -> Unit,
    sickLimitYear1Text: String,
    onSickLimitYear1Change: (String) -> Unit,
    sickLimitYear2Text: String,
    onSickLimitYear2Change: (String) -> Unit,
    autoSickCalculationPeriodDays: Int,
    sickExcludedDaysText: String,
    onSickExcludedDaysChange: (String) -> Unit,
    effectiveSickCalculationDays: Int,
    sickPayPercentText: String,
    onSickPayPercentChange: (String) -> Unit,
    sickMaxDailyAmountText: String,
    onSickMaxDailyAmountChange: (String) -> Unit,
    computedSickAverageDaily: Double,
    isLoadingLimits: Boolean,
    limitsMessage: String?,
    onFetchLimits: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FixedScreenHeader(
                title = "Отпуск и больничный",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "Отпуск",
                    subtitle = "Упрощённый расчёт по сумме начислений за 12 месяцев"
                ) {
                    CompactDecimalField(
                        label = "Сумма начислений за последние 12 месяцев",
                        value = vacationAccruals12MonthsText,
                        onValueChange = onVacationAccrualsChange,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "Средний дневной заработок",
                        value = formatMoney(computedVacationAverageDaily)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onSave,
                            modifier = Modifier.heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("Сохранить", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Больничный",
                    subtitle = "Доход за два предыдущих года, лимиты ФНС и расчётный период"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onFetchLimits,
                            enabled = !isLoadingLimits,
                            modifier = Modifier.heightIn(min = 40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isLoadingLimits) "Загрузка..." else "Подтянуть лимиты", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (!limitsMessage.isNullOrBlank()) {
                        Text(
                            text = limitsMessage,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    CompactDecimalField(
                        label = "Доход за ${sickYear1} год",
                        value = sickIncomeYear1Text,
                        onValueChange = onSickIncomeYear1Change,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactDecimalField(
                        label = "Доход за ${sickYear2} год",
                        value = sickIncomeYear2Text,
                        onValueChange = onSickIncomeYear2Change,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "Лимит ${sickYear1}",
                            value = sickLimitYear1Text,
                            onValueChange = onSickLimitYear1Change,
                            modifier = Modifier.weight(1f)
                        )
                        CompactDecimalField(
                            label = "Лимит ${sickYear2}",
                            value = sickLimitYear2Text,
                            onValueChange = onSickLimitYear2Change,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "Дней в расчётном периоде",
                        value = autoSickCalculationPeriodDays.toString()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactIntField(
                        label = "Исключаемые дни",
                        value = sickExcludedDaysText,
                        onValueChange = onSickExcludedDaysChange,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentInfoRow(
                        label = "Дней к применению",
                        value = effectiveSickCalculationDays.toString()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "Коэф. больничного",
                            value = sickPayPercentText,
                            onValueChange = onSickPayPercentChange,
                            modifier = Modifier.weight(1f)
                        )
                        CompactDecimalField(
                            label = "Макс. в день",
                            value = sickMaxDailyAmountText,
                            onValueChange = onSickMaxDailyAmountChange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "Средний дневной заработок",
                        value = formatMoney(computedSickAverageDaily)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
