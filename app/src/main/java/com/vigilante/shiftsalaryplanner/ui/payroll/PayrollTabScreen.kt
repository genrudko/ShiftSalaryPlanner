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
fun PayrollTab(
    state: PayrollTabState,
    actions: PayrollTabActions,
    modifier: Modifier = Modifier
) {
    var uiState by rememberSaveable { mutableStateOf(PayrollTabUiState()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        MonthHeader(
            currentMonth = state.currentMonth,
            onPrevMonth = actions.onPrevMonth,
            onNextMonth = actions.onNextMonth,
            onPickMonth = actions.onPickMonth
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayrollStatTile(
                title = "Р В§Р В°РЎРѓРЎвЂ№",
                value = formatDouble(state.summary.workedHours),
                subtitle = "Р С•Р С—Р В»Р В°РЎвЂЎР С‘Р Р†Р В°Р ВµР СРЎвЂ№Р Вµ",
                modifier = Modifier.weight(1f)
            )
            PayrollStatTile(
                title = "Р РЋР СР ВµР Р…РЎвЂ№",
                value = state.detailedShiftStats.workedShiftCount.toString(),
                subtitle = "РЎР‚Р В°Р В±Р С•РЎвЂЎР С‘Р Вµ",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayrollStatTile(
                title = "Р С’Р Р†Р В°Р Р…РЎРѓ",
                value = formatMoney(state.payroll.advanceAmount),
                subtitle = formatDate(state.paymentDates.advanceDate),
                modifier = Modifier.weight(1f)
            )
            PayrollStatTile(
                title = "Р С™ Р В·Р В°РЎР‚Р С—Р В»Р В°РЎвЂљР Вµ",
                value = formatMoney(state.payroll.salaryPaymentAmount),
                subtitle = formatDate(state.paymentDates.salaryDate),
                modifier = Modifier.weight(1f),
                emphasize = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PayrollModeSwitcher(
            viewMode = uiState.viewMode,
            onModeChange = { next ->
                uiState = reducePayrollTabUiState(
                    state = uiState,
                    action = PayrollTabUiAction.SetViewMode(next)
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.viewMode == PayrollViewMode.SUMMARY) {
            SummaryCard(
                summary = state.summary,
                payroll = state.payroll,
                annualOvertime = state.annualOvertime,
                paymentDates = state.paymentDates,
                housingPaymentLabel = state.housingPaymentLabel,
                detailedShiftStats = state.detailedShiftStats,
                isExpanded = state.isSummaryExpanded,
                onToggle = actions.onToggleSummary,
                onOpenSettings = actions.onOpenSettings
            )
        } else {
            PayrollSheetCard(
                currentMonth = state.currentMonth,
                payrollDetailedResult = state.payrollDetailedResult,
                onOpenSettings = actions.onOpenSettings
            )
        }
    }
}

@Composable
private fun PayrollModeSwitcher(viewMode: PayrollViewMode, onModeChange: (PayrollViewMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PayrollModeChip(
            text = "Р РЋР Р†Р С•Р Т‘Р С”Р В°",
            selected = viewMode == PayrollViewMode.SUMMARY,
            onClick = { onModeChange(PayrollViewMode.SUMMARY) },
            modifier = Modifier.weight(1f)
        )
        PayrollModeChip(
            text = "Р вЂєР С‘РЎРѓРЎвЂљ",
            selected = viewMode == PayrollViewMode.SHEET,
            onClick = { onModeChange(PayrollViewMode.SHEET) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PayrollModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
