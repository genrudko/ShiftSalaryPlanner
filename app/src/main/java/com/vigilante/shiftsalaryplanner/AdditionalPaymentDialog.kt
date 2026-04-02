package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPaymentType
import com.vigilante.shiftsalaryplanner.payroll.PaymentDistribution
import com.vigilante.shiftsalaryplanner.payroll.PremiumPeriod
import java.time.YearMonth
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun AdditionalPaymentDialog(
    currentPayment: AdditionalPayment?,
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onSave: (AdditionalPayment) -> Unit
) {
    var nameText by rememberSaveable { mutableStateOf(currentPayment?.name ?: "") }
    var amountText by rememberSaveable {
        mutableStateOf(currentPayment?.amount?.toPlainString() ?: "")
    }
    var taxable by rememberSaveable { mutableStateOf(currentPayment?.taxable ?: true) }
    var active by rememberSaveable { mutableStateOf(currentPayment?.active ?: true) }
    var includeInShiftCost by rememberSaveable { mutableStateOf(currentPayment?.includeInShiftCost ?: true) }
    var typeName by rememberSaveable { mutableStateOf(currentPayment?.type ?: AdditionalPaymentType.MONTHLY.name) }
    var premiumPeriodName by rememberSaveable { mutableStateOf(currentPayment?.premiumPeriod ?: PremiumPeriod.MONTHLY.name) }
    var targetMonthText by rememberSaveable { mutableStateOf(currentPayment?.targetMonth?.ifBlank { currentMonth.toString() } ?: currentMonth.toString()) }
    var delayMonthsText by rememberSaveable { mutableStateOf((currentPayment?.delayMonths ?: 0).toString()) }
    var distributionName by rememberSaveable {
        mutableStateOf(
            currentPayment?.distribution ?: if (currentPayment?.withAdvance == true) {
                PaymentDistribution.ADVANCE.name
            } else {
                PaymentDistribution.SALARY.name
            }
        )
    }

    val selectedType = runCatching { AdditionalPaymentType.valueOf(typeName) }.getOrElse { AdditionalPaymentType.MONTHLY }
    val selectedPremiumPeriod = runCatching { PremiumPeriod.valueOf(premiumPeriodName) }.getOrElse { PremiumPeriod.MONTHLY }
    val selectedDistribution = runCatching { PaymentDistribution.valueOf(distributionName) }.getOrElse { PaymentDistribution.SALARY }

    val amountLabel = when (selectedType) {
        AdditionalPaymentType.HOURLY -> "Ставка в час"
        AdditionalPaymentType.PREMIUM -> "Сумма премии"
        else -> "Сумма"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (currentPayment == null) "Новое начисление" else "Редактировать начисление")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Название") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    singleLine = true
                )

                Text("Тип начисления", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    PayModeChoiceCard(
                        title = "Ежемесячная",
                        subtitle = "Фиксированная сумма каждый месяц",
                        selected = selectedType == AdditionalPaymentType.MONTHLY,
                        onClick = { AdditionalPaymentType.MONTHLY.name }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PayModeChoiceCard(
                        title = "Почасовая",
                        subtitle = "Ставка умножается на оплаченные часы за месяц",
                        selected = selectedType == AdditionalPaymentType.HOURLY,
                        onClick = { AdditionalPaymentType.HOURLY.name }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PayModeChoiceCard(
                        title = "Разовая за месяц",
                        subtitle = "Сработает только в выбранном месяце",
                        selected = selectedType == AdditionalPaymentType.ONE_TIME_MONTH,
                        onClick = { AdditionalPaymentType.ONE_TIME_MONTH.name }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PayModeChoiceCard(
                        title = "Премия",
                        subtitle = "Месячная, квартальная, полугодовая или годовая",
                        selected = selectedType == AdditionalPaymentType.PREMIUM,
                        onClick = { AdditionalPaymentType.PREMIUM.name }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                PayrollNumberField(
                    label = amountLabel,
                    value = amountText,
                    onValueChange = { amountText = it }
                )

                if (selectedType == AdditionalPaymentType.ONE_TIME_MONTH) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetMonthText,
                        onValueChange = { newValue ->
                            targetMonthText = newValue.filter { it.isDigit() || it == '-' }.take(7)
                        },
                        label = { Text("Месяц выплаты (ГГГГ-ММ)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { targetMonthText = currentMonth.toString() }) {
                            Text("Текущий месяц")
                        }
                    }
                }

                if (selectedType == AdditionalPaymentType.PREMIUM) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Периодичность премии", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        PremiumPeriod.entries.forEachIndexed { index, period ->
                            PayModeChoiceCard(
                                title = premiumPeriodLabel(period.name),
                                subtitle = when (period) {
                                    PremiumPeriod.MONTHLY -> "Каждый месяц"
                                    PremiumPeriod.QUARTERLY -> "В конце квартала"
                                    PremiumPeriod.HALF_YEARLY -> "В конце полугодия"
                                    PremiumPeriod.YEARLY -> "В конце года"
                                },
                                selected = selectedPremiumPeriod == period,
                                onClick = { period.name }
                            )
                            if (index != PremiumPeriod.entries.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    PayrollNumberField(
                        label = "Сдвиг периода, мес.",
                        value = delayMonthsText,
                        onValueChange = { delayMonthsText = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Куда начислять", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    PayModeChoiceCard(
                        title = "В аванс",
                        subtitle = "Целиком в первую выплату месяца",
                        selected = selectedDistribution == PaymentDistribution.ADVANCE,
                        onClick = { PaymentDistribution.ADVANCE.name }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PayModeChoiceCard(
                        title = "В зарплату",
                        subtitle = "Целиком во вторую выплату месяца",
                        selected = selectedDistribution == PaymentDistribution.SALARY,
                        onClick = { PaymentDistribution.SALARY.name }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PayModeChoiceCard(
                        title = "Делить по половинам месяца",
                        subtitle = "Подходит прежде всего для почасовых начислений",
                        selected = selectedDistribution == PaymentDistribution.SPLIT_BY_HALF_MONTH,
                        onClick = { PaymentDistribution.SPLIT_BY_HALF_MONTH.name }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                CompactSwitchRow(title = "Активна", checked = active, onCheckedChange = { active = it })
                Spacer(modifier = Modifier.height(8.dp))
                CompactSwitchRow(title = "Облагается НДФЛ", checked = taxable, onCheckedChange = { taxable = it })
                Spacer(modifier = Modifier.height(8.dp))
                CompactSwitchRow(
                    title = "Включать в стоимость смены",
                    checked = includeInShiftCost,
                    onCheckedChange = { includeInShiftCost = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        AdditionalPayment(
                            id = currentPayment?.id ?: UUID.randomUUID().toString(),
                            name = nameText.trim(),
                            amount = parseDouble(amountText, currentPayment?.amount ?: 0.0),
                            taxable = taxable,
                            withAdvance = selectedDistribution == PaymentDistribution.ADVANCE,
                            active = active,
                            type = typeName,
                            premiumPeriod = premiumPeriodName,
                            targetMonth = targetMonthText.trim(),
                            delayMonths = parseDouble(delayMonthsText, currentPayment?.delayMonths?.toDouble() ?: 0.0).roundToInt(),
                            includeInShiftCost = includeInShiftCost,
                            distribution = distributionName
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
