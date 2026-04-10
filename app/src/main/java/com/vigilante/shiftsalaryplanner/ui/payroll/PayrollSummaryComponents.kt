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
fun SummaryCard(
    summary: MonthSummary,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    detailedShiftStats: DetailedShiftStats,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onToggle)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "РЎРІРѕРґРєР° Р·Р° РјРµСЃСЏС†",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isExpanded) "Р Р°Р·РІРµСЂРЅСѓС‚Р°СЏ РґРµС‚Р°Р»РёР·Р°С†РёСЏ" else "РљСЂР°С‚РєРёР№ РёС‚РѕРі",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onOpenSettings) {
                    Text("РќР°СЃС‚СЂРѕР№РєРё")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded) {
                PayrollSummarySectionTitle("РЎРјРµРЅС‹ Рё С‡Р°СЃС‹")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "Р Р°Р±РѕС‡РёС… РґРЅРµР№: ${summary.workedDays}")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "РћРїР»Р°С‡РёРІР°РµРјС‹Рµ С‡Р°СЃС‹: ${formatDouble(summary.workedHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "РќРѕС‡РЅС‹Рµ С‡Р°СЃС‹: ${formatDouble(summary.nightHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "РџСЂР°Р·РґРЅРёС‡РЅС‹Рµ/РІС‹С…РѕРґРЅС‹Рµ: ${formatDouble(payroll.holidayHours)} С‡")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "РћС‚РїСѓСЃРє: ${payroll.vacationDays} РґРЅ. вЂў Р‘РѕР»СЊРЅРёС‡РЅС‹Р№: ${payroll.sickDays} РґРЅ.")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "РЎРІРµСЂС…СѓСЂРѕС‡РєР° (${annualOvertime.periodLabel}): ${formatDouble(annualOvertime.payableOvertimeHours)} С‡")
                Spacer(modifier = Modifier.height(6.dp))
                PayrollInfoPill(text = "РЎРјРµРЅС‹: Р” ${detailedShiftStats.dayShiftCount} вЂў Рќ ${detailedShiftStats.nightShiftCount} вЂў Р’/Рџ ${detailedShiftStats.weekendHolidayShiftCount}")

                if (detailedShiftStats.workedShiftCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SummaryPanelCard(title = "РЎС‚РѕРёРјРѕСЃС‚СЊ СЃРјРµРЅС‹") {
                        PaymentInfoRow("РЎСЂРµРґРЅСЏСЏ (РґРѕ РќР”Р¤Р›)", formatMoney(detailedShiftStats.shiftCostAverageGross), bold = detailedShiftStats.shiftCostAverageGross > 0.0)
                        PaymentInfoRow("РЎСЂРµРґРЅСЏСЏ (РЅР° СЂСѓРєРё)", formatMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
                        PaymentInfoRow("Р”РЅРµРІРЅР°СЏ", "${formatMoney(detailedShiftStats.dayShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.dayShiftCostAverageNet)}")
                        PaymentInfoRow("РќРѕС‡РЅР°СЏ", "${formatMoney(detailedShiftStats.nightShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.nightShiftCostAverageNet)}")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                PayrollSummarySectionTitle("РќР°С‡РёСЃР»РµРЅРёСЏ")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryPanelCard(title = "РћСЃРЅРѕРІРЅС‹Рµ СЃСѓРјРјС‹") {
                    PaymentInfoRow("Р§Р°СЃРѕРІР°СЏ СЃС‚Р°РІРєР°", formatMoney(payroll.hourlyRate))
                    PaymentInfoRow("Р‘Р°Р·Р°", formatMoney(payroll.basePay))
                    PaymentInfoRow("РќРѕС‡РЅС‹Рµ", formatMoney(payroll.nightExtra))
                    PaymentInfoRow("РџСЂР°Р·РґРЅРёС‡РЅС‹Рµ/РІС‹С…РѕРґРЅС‹Рµ", formatMoney(payroll.holidayExtra))
                    PaymentInfoRow("РћС‚РїСѓСЃРєРЅС‹Рµ", formatMoney(payroll.vacationPay))
                    PaymentInfoRow("Р‘РѕР»СЊРЅРёС‡РЅС‹Р№", formatMoney(payroll.sickPay))
                    CompactSummaryDivider()
                    PaymentInfoRow("Р”РѕРїРІС‹РїР»Р°С‚С‹ РІСЃРµРіРѕ", formatMoney(payroll.additionalPaymentsTotal))
                    PaymentInfoRow("Р’ Р°РІР°РЅСЃ", formatMoney(payroll.additionalPaymentsAdvancePart))
                    PaymentInfoRow("Р’ Р·Р°СЂРїР»Р°С‚Сѓ", formatMoney(payroll.additionalPaymentsSalaryPart))
                    CompactSummaryDivider()
                    PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
                    PaymentInfoRow("РР· РЅРµС‘ РІ Р°РІР°РЅСЃ", formatMoney(payroll.housingAdvancePart))
                    PaymentInfoRow("РР· РЅРµС‘ РІ Р·Р°СЂРїР»Р°С‚Сѓ", formatMoney(payroll.housingSalaryPart))
                }

                Spacer(modifier = Modifier.height(10.dp))
                SummaryPanelCard(title = "РС‚РѕРі СЂР°СЃС‡С‘С‚Р°") {
                    PaymentInfoRow("РћР±Р»Р°РіР°РµРјР°СЏ Р±Р°Р·Р°", formatMoney(payroll.taxableGrossTotal))
                    PaymentInfoRow("РќРµРѕР±Р»Р°РіР°РµРјС‹Рµ РІС‹РїР»Р°С‚С‹", formatMoney(payroll.nonTaxableTotal))
                    PaymentInfoRow("Р’СЃРµРіРѕ РЅР°С‡РёСЃР»РµРЅРѕ", formatMoney(payroll.grossTotal))
                    PaymentInfoRow("РќР”Р¤Р›", formatMoney(payroll.ndfl))
                    PaymentInfoRow("Р”РѕРїР»Р°С‚Р° Р·Р° РїРµСЂРµСЂР°Р±РѕС‚РєСѓ", formatMoney(annualOvertime.overtimePremiumAmount))
                    if (payroll.taxableIncomeYtdAfterCurrentMonth > 0.0) {
                        PaymentInfoRow("Р‘Р°Р·Р° СЃ РЅР°С‡Р°Р»Р° РіРѕРґР° РґРѕ РјРµСЃСЏС†Р°", formatMoney(payroll.taxableIncomeYtdBeforeCurrentMonth))
                        PaymentInfoRow("Р‘Р°Р·Р° СЃ РЅР°С‡Р°Р»Р° РіРѕРґР° РїРѕСЃР»Рµ РјРµСЃСЏС†Р°", formatMoney(payroll.taxableIncomeYtdAfterCurrentMonth))
                    }
                    PaymentInfoRow("РќР° СЂСѓРєРё Р·Р° РјРµСЃСЏС†", formatMoney(payroll.netTotal), bold = true)
                }

                Spacer(modifier = Modifier.height(10.dp))
                PayrollSummarySectionTitle("Р’С‹РїР»Р°С‚С‹")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryPanelCard(title = "РџРѕ РґР°С‚Р°Рј") {
                    PaymentInfoRow("РђРІР°РЅСЃ", formatMoney(payroll.advanceAmount))
                    PaymentInfoRow("РђРІР°РЅСЃ С‚РѕР»СЊРєРѕ РїРѕ СЃРјРµРЅР°Рј", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
                    PaymentInfoRow("Р”Р°С‚Р° Р°РІР°РЅСЃР°", formatDate(paymentDates.advanceDate))
                    CompactSummaryDivider()
                    PaymentInfoRow("Рљ Р·Р°СЂРїР»Р°С‚Рµ", formatMoney(payroll.salaryPaymentAmount), bold = true)
                    PaymentInfoRow("Р—Р°СЂРїР»Р°С‚Р° С‚РѕР»СЊРєРѕ РїРѕ СЃРјРµРЅР°Рј", formatMoney(payroll.shiftOnlySalaryNetAmount))
                    PaymentInfoRow("Р”Р°С‚Р° Р·Р°СЂРїР»Р°С‚С‹", formatDate(paymentDates.salaryDate))
                }
            } else {
                SummaryCollapsedPill(text = "Р§Р°СЃС‹: ${formatDouble(summary.workedHours)}")
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "РЎРјРµРЅС‹: ${detailedShiftStats.workedShiftCount} вЂў Р” ${detailedShiftStats.dayShiftCount} вЂў Рќ ${detailedShiftStats.nightShiftCount}")
                if (detailedShiftStats.workedShiftCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "РЎСЂРµРґРЅСЏСЏ СЃРјРµРЅР°: ${formatMoney(detailedShiftStats.shiftCostAverageGross)} / ${formatMoney(detailedShiftStats.shiftCostAverageNet)}")
                }
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "РђРІР°РЅСЃ: ${formatMoney(payroll.advanceAmount)}")
                if (payroll.vacationPay > 0.0 || payroll.sickPay > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "РћС‚РїСѓСЃРє/Р±РѕР»СЊРЅРёС‡РЅС‹Р№: ${formatMoney(payroll.vacationPay + payroll.sickPay)}")
                }
                if (annualOvertime.payableOvertimeHours > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryCollapsedPill(text = "РЎРІРµСЂС…СѓСЂРѕС‡РєР°: ${formatDouble(annualOvertime.payableOvertimeHours)} С‡")
                }
                Spacer(modifier = Modifier.height(6.dp))
                SummaryCollapsedPill(text = "Рљ Р·Р°СЂРїР»Р°С‚Рµ: ${formatMoney(payroll.salaryPaymentAmount)}", emphasize = true)
            }
        }
    }
}

@Composable
fun PayrollStatTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    val containerColor = if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PayrollSummarySectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SummaryPanelCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SummaryCollapsedPill(text: String, emphasize: Boolean = false) {
    val containerColor = if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    Surface(shape = RoundedCornerShape(999.dp), color = containerColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PayrollInfoPill(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactSummaryDivider() {
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(6.dp))
}
