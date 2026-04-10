package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineBreakdownItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollQuantityUnit
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetSection
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PayrollSheetCard(
    currentMonth: YearMonth,
    payrollDetailedResult: PayrollDetailedResult,
    onOpenSettings: () -> Unit
) {
    val items = payrollDetailedResult.lineItems
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Р Р°СЃС‡С‘С‚РЅС‹Р№ Р»РёСЃС‚", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Р—Р° ${formatPayrollSheetMonth(currentMonth)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenSettings) { Text("РќР°СЃС‚СЂРѕР№РєРё") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PayrollSheetSectionBlock("РћР±С‰Р°СЏ РёРЅС„РѕСЂРјР°С†РёСЏ", items.filter { it.section == PayrollSheetSection.HEADER }, false)
            PayrollSheetSectionBlock("РќР°С‡РёСЃР»РµРЅРѕ Р·Р° РјРµСЃСЏС†", items.filter { it.section == PayrollSheetSection.ACCRUAL }, false)
            PayrollSheetSectionBlock("РЈРґРµСЂР¶Р°РЅРѕ Р·Р° РјРµСЃСЏС†", items.filter { it.section == PayrollSheetSection.DEDUCTION }, true)
            PayrollSheetSectionBlock("РђРІР°РЅСЃ", items.filter { it.section == PayrollSheetSection.PRIOR_PAYMENT }, false)
            PayrollSheetSectionBlock("Рљ Р·Р°СЂРїР»Р°С‚Рµ", items.filter { it.section == PayrollSheetSection.PAYOUT }, false)
            PayrollSheetSectionBlock("РС‚РѕРіРё РјРµСЃСЏС†Р°", items.filter { it.section == PayrollSheetSection.REFERENCE }, false)
        }
    }
}

@Composable
private fun PayrollSheetSectionBlock(
    title: String,
    items: List<PayrollLineItem>,
    isDeductionSection: Boolean
) {
    if (items.isEmpty()) return
    Spacer(modifier = Modifier.height(6.dp))
    PayrollSummarySectionTitle(title)
    Spacer(modifier = Modifier.height(6.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "РџРѕР·РёС†РёСЏ",
                    modifier = Modifier.weight(1.35f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "РљРѕР»-РІРѕ",
                    modifier = Modifier.weight(0.65f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "РЎСѓРјРјР°",
                    modifier = Modifier.weight(0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(6.dp))

            items.forEachIndexed { index, item ->
                PayrollSheetRow(item = item, deductionStyle = isDeductionSection)
                if (index != items.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun PayrollSheetRow(
    item: PayrollLineItem,
    deductionStyle: Boolean
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }

    val isHeaderQuantity = item.section == PayrollSheetSection.HEADER && item.unit != PayrollQuantityUnit.NONE
    val normalizedQuantity = item.quantity ?: if (isHeaderQuantity) item.amount else null
    val quantityText = when (item.unit) {
        PayrollQuantityUnit.HOURS -> normalizedQuantity?.let { "${formatDouble(it)} С‡" }
        PayrollQuantityUnit.DAYS -> normalizedQuantity?.let { "${formatDouble(it)} РґРЅ" }
        PayrollQuantityUnit.MONTHS -> normalizedQuantity?.let { "${formatDouble(it)} РјРµСЃ" }
        PayrollQuantityUnit.TIMES -> normalizedQuantity?.let { "${formatDouble(it)} СЂР°Р·" }
        PayrollQuantityUnit.NONE -> null
    }

    val amountText = if (isHeaderQuantity) {
        "-"
    } else {
        val amountPrefix = if (deductionStyle) "- " else ""
        amountPrefix + formatMoney(item.amount)
    }

    val canExpand = item.expandableDetails || item.details.isNotEmpty() || item.ndflAmount != null || item.netAmount != null

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
                style = MaterialTheme.typography.bodyMedium,
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
                style = MaterialTheme.typography.bodyMedium,
                color = if (deductionStyle) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }

        val metaParts = buildList {
            if (!item.periodLabel.isNullOrBlank()) add("РџРµСЂРёРѕРґ: ${item.periodLabel}")
            if (!item.note.isNullOrBlank()) add(item.note)
        }

        if (metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = metaParts.joinToString(" вЂў "),
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
                item.ndflAmount?.let { PayrollBreakdownInfoRow(label = "РќР”Р¤Р›", value = formatMoney(it)) }
                item.netAmount?.let { PayrollBreakdownInfoRow(label = "РќР° СЂСѓРєРё", value = formatMoney(it), bold = true) }
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
        PayrollQuantityUnit.HOURS -> detail.quantity?.let { "${formatDouble(it)} С‡" }
        PayrollQuantityUnit.DAYS -> detail.quantity?.let { "${formatDouble(it)} РґРЅ" }
        PayrollQuantityUnit.MONTHS -> detail.quantity?.let { "${formatDouble(it)} РјРµСЃ" }
        PayrollQuantityUnit.TIMES -> detail.quantity?.let { "${formatDouble(it)} СЂР°Р·" }
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
            if (!quantityText.isNullOrBlank()) PayrollBreakdownInfoRow(label = "РљРѕР»РёС‡РµСЃС‚РІРѕ", value = quantityText)
            detail.ndflAmount?.let { PayrollBreakdownInfoRow(label = "РќР”Р¤Р›", value = formatMoney(it)) }
            detail.netAmount?.let { PayrollBreakdownInfoRow(label = "РќР° СЂСѓРєРё", value = formatMoney(it), bold = true) }
            if (!detail.note.isNullOrBlank()) PayrollBreakdownInfoRow(label = "РџСЂРёРјРµС‡Р°РЅРёРµ", value = detail.note)
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
