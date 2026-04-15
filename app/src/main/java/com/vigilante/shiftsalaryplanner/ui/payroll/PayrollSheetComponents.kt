package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineBreakdownItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollQuantityUnit
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetSection
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PayrollSheetCard(
    currentMonth: YearMonth,
    payrollDetailedResult: PayrollDetailedResult,
    onOpenSettings: () -> Unit,
    onOpenVisibilitySettings: () -> Unit,
    onExportPdf: () -> Unit,
    visibilitySettings: ReportVisibilitySettings,
    compactMode: Boolean = false
) {
    val items = payrollDetailedResult.lineItems
    val visibleSections = listOf(
        Triple(PayrollSheetSection.HEADER, "Общая информация", false),
        Triple(PayrollSheetSection.ACCRUAL, "Начислено за месяц", false),
        Triple(PayrollSheetSection.DEDUCTION, "Удержано за месяц", true),
        Triple(PayrollSheetSection.PRIOR_PAYMENT, "Аванс", false),
        Triple(PayrollSheetSection.PAYOUT, "К зарплате", false),
        Triple(PayrollSheetSection.REFERENCE, "Итоги месяца", false)
    ).filter { (section, _, _) -> visibilitySettings.isPayrollSectionVisible(section) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(appCardPadding())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Расчётный лист", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "За ${formatPayrollSheetMonth(currentMonth)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (compactMode) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Компактный режим",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = appHapticAction(onAction = onOpenVisibilitySettings)) { Text("Строки") }
                    TextButton(onClick = appHapticAction(onAction = onExportPdf)) { Text("PDF") }
                    TextButton(onClick = appHapticAction(onAction = onOpenSettings)) { Text("Настройки") }
                }
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))

            if (items.isEmpty()) {
                AppEmptyCard(
                    title = "Пока нет строк расчёта",
                    message = "Открой «Настройки», проверь оклад и параметры начислений."
                )
            } else {
                if (visibleSections.isEmpty()) {
                    AppEmptyCard(
                        title = "Все строки скрыты",
                        message = "Нажми «Строки», чтобы включить нужные блоки расчётного листа."
                    )
                } else {
                    visibleSections.forEach { (section, title, isDeductionSection) ->
                        PayrollSheetSectionBlock(
                            title = title,
                            items = items.filter { it.section == section },
                            isDeductionSection = isDeductionSection,
                            compactMode = compactMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayrollSheetSectionBlock(
    title: String,
    items: List<PayrollLineItem>,
    isDeductionSection: Boolean,
    compactMode: Boolean
) {
    if (items.isEmpty()) return
    Spacer(modifier = Modifier.height(if (compactMode) 2.dp else 6.dp))
    if (!compactMode) {
        PayrollSummarySectionTitle(title)
        Spacer(modifier = Modifier.height(6.dp))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compactMode) appScaledSpacing(8.dp) else appScaledSpacing(10.dp),
                    vertical = if (compactMode) appScaledSpacing(4.dp) else appScaledSpacing(8.dp)
                )
        ) {
            if (!compactMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Позиция",
                        modifier = Modifier.weight(1.35f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Кол-во",
                        modifier = Modifier.weight(0.65f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Сумма",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
            }

            items.forEachIndexed { index, item ->
                PayrollSheetRow(
                    item = item,
                    deductionStyle = isDeductionSection,
                    compactMode = compactMode
                )
                if (index != items.lastIndex) {
                    Spacer(modifier = Modifier.height(if (compactMode) 3.dp else 6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(if (compactMode) 3.dp else 6.dp))
                }
            }
        }
    }
}

@Composable
private fun PayrollSheetRow(
    item: PayrollLineItem,
    deductionStyle: Boolean,
    compactMode: Boolean
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }

    val isHeaderQuantity = item.section == PayrollSheetSection.HEADER && item.unit != PayrollQuantityUnit.NONE
    val normalizedQuantity = item.quantity ?: if (isHeaderQuantity) item.amount else null
    val quantityText = when (item.unit) {
        PayrollQuantityUnit.HOURS -> normalizedQuantity?.let { "${formatDouble(it)} ч" }
        PayrollQuantityUnit.DAYS -> normalizedQuantity?.let { "${formatDouble(it)} дн" }
        PayrollQuantityUnit.MONTHS -> normalizedQuantity?.let { "${formatDouble(it)} мес" }
        PayrollQuantityUnit.TIMES -> normalizedQuantity?.let { "${formatDouble(it)} раз" }
        PayrollQuantityUnit.NONE -> null
    }

    val amountText = if (isHeaderQuantity) {
        "-"
    } else {
        val amountPrefix = if (deductionStyle) "- " else ""
        amountPrefix + formatMoney(item.amount)
    }

    val canExpand = !compactMode &&
        (item.expandableDetails || item.details.isNotEmpty() || item.ndflAmount != null || item.netAmount != null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canExpand) { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = item.title,
                modifier = Modifier.weight(1.35f),
                style = if (compactMode) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = quantityText ?: "-",
                modifier = Modifier.weight(0.65f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountText,
                modifier = Modifier.weight(0.8f),
                style = if (compactMode) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = if (deductionStyle) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }

        val metaParts = buildList {
            if (!item.periodLabel.isNullOrBlank()) add("Период: ${item.periodLabel}")
            if (!item.note.isNullOrBlank()) add(item.note)
        }

        if (!compactMode && metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = metaParts.joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded && canExpand) {
            Spacer(modifier = Modifier.height(6.dp))
            if (item.details.isNotEmpty()) {
                item.details.forEachIndexed { index, detail ->
                    PayrollBreakdownDetailRow(detail = detail, depth = 0)
                    if (index != item.details.lastIndex) Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                item.ndflAmount?.let { PayrollBreakdownInfoRow(label = "НДФЛ", value = formatMoney(it)) }
                item.netAmount?.let { PayrollBreakdownInfoRow(label = "На руки", value = formatMoney(it), bold = true) }
            }
        }
    }
}

private fun formatPayrollSheetMonth(month: YearMonth): String {
    val ruLocale = Locale.forLanguageTag("ru")
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale)
    val raw = month.atDay(1).format(formatter)
    return raw.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(ruLocale) else char.toString()
    }
}

@Composable
private fun PayrollBreakdownDetailRow(detail: PayrollLineBreakdownItem) {
    PayrollBreakdownDetailRow(detail = detail, depth = 0)
}

@Composable
private fun PayrollBreakdownDetailRow(
    detail: PayrollLineBreakdownItem,
    depth: Int
) {
    var expanded by rememberSaveable(detail.title, detail.amount, depth) { mutableStateOf(false) }
    val hasNestedDetails = detail.details.isNotEmpty()
    val canExpand = hasNestedDetails
    val quantityText = when (detail.unit) {
        PayrollQuantityUnit.HOURS -> detail.quantity?.let { "${formatDouble(it)} ч" }
        PayrollQuantityUnit.DAYS -> detail.quantity?.let { "${formatDouble(it)} дн" }
        PayrollQuantityUnit.MONTHS -> detail.quantity?.let { "${formatDouble(it)} мес" }
        PayrollQuantityUnit.TIMES -> detail.quantity?.let { "${formatDouble(it)} раз" }
        PayrollQuantityUnit.NONE -> null
    }
    val leftPad = (depth * 10).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = leftPad)
            .clickable(enabled = canExpand) { expanded = !expanded }
    ) {
        PayrollBreakdownInfoRow(label = detail.title, value = formatMoney(detail.amount), bold = true)
        if (!canExpand || expanded) {
            if (!quantityText.isNullOrBlank()) PayrollBreakdownInfoRow(label = "Количество", value = quantityText)
            detail.ndflAmount?.let { PayrollBreakdownInfoRow(label = "НДФЛ", value = formatMoney(it)) }
            detail.netAmount?.let { PayrollBreakdownInfoRow(label = "На руки", value = formatMoney(it), bold = true) }
            if (!detail.note.isNullOrBlank()) PayrollBreakdownInfoRow(label = "Примечание", value = detail.note)
            if (hasNestedDetails) {
                Spacer(modifier = Modifier.height(2.dp))
                detail.details.forEachIndexed { index, nestedDetail ->
                    PayrollBreakdownDetailRow(detail = nestedDetail, depth = depth + 1)
                    if (index != detail.details.lastIndex) Spacer(modifier = Modifier.height(3.dp))
                }
            }
        }
    }
}

@Composable
private fun PayrollBreakdownInfoRow(
    label: String,
    value: String,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

