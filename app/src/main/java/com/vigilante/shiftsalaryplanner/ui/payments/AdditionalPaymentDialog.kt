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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    var amountText by rememberSaveable { mutableStateOf(currentPayment?.amount?.toPlainString() ?: "") }
    var taxable by rememberSaveable { mutableStateOf(currentPayment?.taxable ?: true) }
    var active by rememberSaveable { mutableStateOf(currentPayment?.active ?: true) }
    var includeInShiftCost by rememberSaveable { mutableStateOf(currentPayment?.includeInShiftCost ?: true) }
    var typeName by rememberSaveable { mutableStateOf(currentPayment?.type ?: AdditionalPaymentType.MONTHLY.name) }
    var premiumPeriodName by rememberSaveable { mutableStateOf(currentPayment?.premiumPeriod ?: PremiumPeriod.MONTHLY.name) }
    var targetMonthText by rememberSaveable {
        mutableStateOf(currentPayment?.targetMonth?.ifBlank { currentMonth.toString() } ?: currentMonth.toString())
    }
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
        AdditionalPaymentType.SALARY_PERCENT -> "Процент от оклада, %"
        AdditionalPaymentType.HOURLY -> "Ставка в час"
        AdditionalPaymentType.PREMIUM -> "Сумма премии"
        else -> "Сумма"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentPayment == null) "Новое начисление" else "Редактировать начисление") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CompactTextField(
                    label = "Название",
                    value = nameText,
                    onValueChange = { nameText = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("Тип начисления", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "Ежемесячная",
                            subtitle = "Фиксированная сумма каждый месяц",
                            selected = selectedType == AdditionalPaymentType.MONTHLY,
                            onClick = { typeName = AdditionalPaymentType.MONTHLY.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                        PayModeChoiceCard(
                            title = "Почасовая",
                            subtitle = "Ставка умножается на часы",
                            selected = selectedType == AdditionalPaymentType.HOURLY,
                            onClick = { typeName = AdditionalPaymentType.HOURLY.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "От оклада, %",
                            subtitle = "Процент от базового оклада",
                            selected = selectedType == AdditionalPaymentType.SALARY_PERCENT,
                            onClick = { typeName = AdditionalPaymentType.SALARY_PERCENT.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "Разовая",
                            subtitle = "Только выбранный месяц",
                            selected = selectedType == AdditionalPaymentType.ONE_TIME_MONTH,
                            onClick = { typeName = AdditionalPaymentType.ONE_TIME_MONTH.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                        PayModeChoiceCard(
                            title = "Премия",
                            subtitle = "Периодическая выплата",
                            selected = selectedType == AdditionalPaymentType.PREMIUM,
                            onClick = { typeName = AdditionalPaymentType.PREMIUM.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (selectedType) {
                        AdditionalPaymentType.MONTHLY -> "Фиксированная сумма каждый месяц"
                        AdditionalPaymentType.SALARY_PERCENT -> "Процент от базового оклада в настройках расчёта"
                        AdditionalPaymentType.HOURLY -> "Ставка умножается на оплаченные часы за месяц"
                        AdditionalPaymentType.ONE_TIME_MONTH -> "Начисление только в выбранном месяце"
                        AdditionalPaymentType.PREMIUM -> "Премия по выбранной периодичности"
                    },
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.height(6.dp))
                CompactDecimalField(
                    label = amountLabel,
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == AdditionalPaymentType.ONE_TIME_MONTH) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CompactTextField(
                        label = "Месяц выплаты (ГГГГ-ММ)",
                        value = targetMonthText,
                        onValueChange = { newValue ->
                            targetMonthText = newValue.filter { it.isDigit() || it == '-' }.take(7)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Периодичность премии", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PayModeChoiceCard(
                                title = premiumPeriodLabel(PremiumPeriod.MONTHLY.name),
                                subtitle = "Каждый месяц",
                                selected = selectedPremiumPeriod == PremiumPeriod.MONTHLY,
                                onClick = { premiumPeriodName = PremiumPeriod.MONTHLY.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            PayModeChoiceCard(
                                title = premiumPeriodLabel(PremiumPeriod.QUARTERLY.name),
                                subtitle = "В конце квартала",
                                selected = selectedPremiumPeriod == PremiumPeriod.QUARTERLY,
                                onClick = { premiumPeriodName = PremiumPeriod.QUARTERLY.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PayModeChoiceCard(
                                title = premiumPeriodLabel(PremiumPeriod.HALF_YEARLY.name),
                                subtitle = "В конце полугодия",
                                selected = selectedPremiumPeriod == PremiumPeriod.HALF_YEARLY,
                                onClick = { premiumPeriodName = PremiumPeriod.HALF_YEARLY.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            PayModeChoiceCard(
                                title = premiumPeriodLabel(PremiumPeriod.YEARLY.name),
                                subtitle = "В конце года",
                                selected = selectedPremiumPeriod == PremiumPeriod.YEARLY,
                                onClick = { premiumPeriodName = PremiumPeriod.YEARLY.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    CompactIntField(
                        label = "Сдвиг периода, мес.",
                        value = delayMonthsText,
                        onValueChange = { delayMonthsText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Куда начислять", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "В аванс",
                            subtitle = "Целиком в первую выплату месяца",
                            selected = selectedDistribution == PaymentDistribution.ADVANCE,
                            onClick = { distributionName = PaymentDistribution.ADVANCE.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                        PayModeChoiceCard(
                            title = "В зарплату",
                            subtitle = "Целиком во вторую выплату месяца",
                            selected = selectedDistribution == PaymentDistribution.SALARY,
                            onClick = { distributionName = PaymentDistribution.SALARY.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }
                    PayModeChoiceCard(
                        title = "Делить 50/50",
                        subtitle = "Подходит прежде всего для почасовых начислений",
                        selected = selectedDistribution == PaymentDistribution.SPLIT_BY_HALF_MONTH,
                        onClick = { distributionName = PaymentDistribution.SPLIT_BY_HALF_MONTH.name },
                        showSubtitle = false
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniToggleCard(
                        title = "Активна",
                        checked = active,
                        onCheckedChange = { active = it },
                        modifier = Modifier.weight(1f)
                    )
                    MiniToggleCard(
                        title = "НДФЛ",
                        checked = taxable,
                        onCheckedChange = { taxable = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                MiniToggleCard(
                    title = "В цене смены",
                    checked = includeInShiftCost,
                    onCheckedChange = { includeInShiftCost = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        AdditionalPayment(
                            id = currentPayment?.id ?: UUID.randomUUID().toString(),
                            workplaceId = currentPayment?.workplaceId ?: "work_main",
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

@Composable
private fun MiniToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.56f)
                )
            }
        }
    }
}
