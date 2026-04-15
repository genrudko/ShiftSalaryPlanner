package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment

@Composable
fun AdditionalPaymentsManagementScreen(
    payments: List<AdditionalPayment>,
    onBack: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (AdditionalPayment) -> Unit,
    onDeletePayment: (AdditionalPayment) -> Unit
) {
    val activeCount = payments.count { it.active }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FixedScreenHeader(
                title = "Доплаты, выплаты и премии",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(appScreenPadding())
            ) {
                CompactManageSummaryCard(
                    totalCount = payments.size,
                    activeCount = activeCount,
                    onAdd = onAddPayment
                )

                Spacer(modifier = Modifier.height(appBlockSpacing()))

                if (payments.isEmpty()) {
                    EmptyPaymentsCard()
                } else {
                    payments
                        .sortedWith(compareByDescending<AdditionalPayment> { it.active }.thenBy { it.name.lowercase() })
                        .forEachIndexed { index, payment ->
                            AdditionalPaymentManageCard(
                                payment = payment,
                                onEdit = { onEditPayment(payment) },
                                onDelete = { onDeletePayment(payment) }
                            )
                            if (index != payments.lastIndex) {
                                Spacer(modifier = Modifier.height(appBlockSpacing()))
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun CompactManageSummaryCard(
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
        PaymentInfoRow("Всего начислений", totalCount.toString())
        PaymentInfoRow("Активных", activeCount.toString())

        Spacer(modifier = Modifier.height(appBlockSpacing()))
        CompactActionPillButton(
            text = "Добавить начисление",
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyPaymentsCard() {
    AppEmptyCard(
        title = "Пока пусто",
        message = "Добавь доплату, выплату или премию для расчёта месяца."
    )
}

@Composable
private fun AdditionalPaymentManageCard(
    payment: AdditionalPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appPanelColor(), RoundedCornerShape(appCardRadius()))
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(appCardRadius()))
            .clickable(onClick = appHapticAction(onAction = onEdit))
            .padding(appCardPadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.name.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = additionalPaymentTypeLabel(payment.type),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = additionalPaymentDetailsLabel(payment),
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
                Text(
                    text = buildString {
                        append(paymentDistributionLabel(payment.distribution))
                        append(" • ")
                        append(if (payment.taxable) "с НДФЛ" else "без НДФЛ")
                        append(" • ")
                        append(if (payment.includeInShiftCost) "в цене смены" else "вне цены смены")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Switch(
                        checked = payment.active,
                        onCheckedChange = null,
                        modifier = Modifier.scale(0.58f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = appHapticAction(onAction = onDelete)) { Text("Удалить") }
            TextButton(onClick = appHapticAction(onAction = onEdit)) { Text("Изменить") }
        }
    }
}

@Composable
private fun CompactActionPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(appInputFieldHeight())
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
