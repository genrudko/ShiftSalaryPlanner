package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import java.time.LocalDate
import kotlin.math.abs

data class PayrollDiagnosticsState(
    val periodLabel: String,
    val workplaceLabel: String,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val payrollSettings: PayrollSettings,
    val summary: MonthSummary,
    val payroll: PayrollResult,
    val resolvedAdditionalPayments: List<ResolvedAdditionalPaymentBreakdown>,
    val deductions: List<PayrollDeduction>,
    val rawWorkedHoursBeforeShortReduction: Double,
    val rawFirstHalfHoursBeforeShortReduction: Double
) {
    val shortDayReductionHours: Double
        get() = (rawWorkedHoursBeforeShortReduction - payroll.workedHours).coerceAtLeast(0.0)

    val shortDayReductionHoursInFirstHalf: Double
        get() = (rawFirstHalfHoursBeforeShortReduction - rawFirstHalfHours()).coerceAtLeast(0.0)

    private fun rawFirstHalfHours(): Double = (payroll.advanceGrossAmount / payroll.hourlyRate)
        .takeIf { payroll.hourlyRate > 0.0 } ?: 0.0
}

@Composable
fun PayrollDiagnosticsScreen(
    state: PayrollDiagnosticsState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(appPanelColor(), RoundedCornerShape(appCornerRadius(12.dp)))
                    .clickable(onClick = appHapticAction(onAction = onBack)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Назад"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Диагностика расчёта",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.periodLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
        }

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        DiagnosticsSection(title = "Контекст") {
            DiagnosticsRow("Работа", state.workplaceLabel)
            DiagnosticsRow(
                "Период",
                if (state.periodStartDate == state.periodEndDate) {
                    formatDate(state.periodStartDate)
                } else {
                    "${formatDate(state.periodStartDate)} — ${formatDate(state.periodEndDate)}"
                }
            )
            DiagnosticsRow("Режим оплаты", payModeLabel(state.payrollSettings.payMode))
            DiagnosticsRow("Режим аванса", advanceModeLabel(state.payrollSettings.advanceMode))
            DiagnosticsRow("Сокращённый предпраздничный", if (state.payrollSettings.applyShortDayReduction) "Вкл" else "Выкл")
        }

        DiagnosticsSection(title = "Часы") {
            DiagnosticsRow("По шаблонам (до сокращения)", formatHours(state.rawWorkedHoursBeforeShortReduction))
            DiagnosticsRow("Уменьшено на предпраздничных", formatHours(state.shortDayReductionHours))
            DiagnosticsRow("Оплачиваемые часы (итог)", formatHours(state.payroll.workedHours), emphasize = true)
            DiagnosticsRow("Ночные часы", formatHours(state.payroll.nightHours))
            DiagnosticsRow("Праздничные/выходные часы", formatHours(state.payroll.holidayHours))
            DiagnosticsRow("Рабочих смен", state.summary.workedDays.toString())
        }

        DiagnosticsSection(title = "Начисления") {
            DiagnosticsRow("Часовая ставка", formatMoney(state.payroll.hourlyRate))
            DiagnosticsRow("База", formatMoney(state.payroll.basePay))
            DiagnosticsRow("Ночные", formatMoney(state.payroll.nightExtra))
            DiagnosticsRow("Праздничные/выходные", formatMoney(state.payroll.holidayExtra))
            DiagnosticsRow("Отпуск", formatMoney(state.payroll.vacationPay))
            DiagnosticsRow("Больничный", formatMoney(state.payroll.sickPay))
            DiagnosticsRow("Допвыплаты (облагаемые)", formatMoney(state.payroll.additionalPaymentsTaxablePart))
            DiagnosticsRow("Допвыплаты (необлагаемые)", formatMoney(state.payroll.additionalPaymentsNonTaxablePart))
            DiagnosticsRow("Облагаемая база", formatMoney(state.payroll.taxableGrossTotal))
            DiagnosticsRow("Необлагаемые", formatMoney(state.payroll.nonTaxableTotal))
            DiagnosticsRow("Начислено всего", formatMoney(state.payroll.grossTotal), emphasize = true)
        }

        DiagnosticsSection(title = "Налоги и выплаты") {
            DiagnosticsRow("НДФЛ", formatMoney(state.payroll.ndfl))
            DiagnosticsRow("На руки за период", formatMoney(state.payroll.netTotal), emphasize = true)
            HorizontalDivider(modifier = Modifier.padding(vertical = appScaledSpacing(6.dp)))
            DiagnosticsRow("Аванс (до НДФЛ)", formatMoney(state.payroll.advanceGrossAmount))
            DiagnosticsRow("Аванс НДФЛ", formatMoney(state.payroll.advanceNdflAmount))
            DiagnosticsRow("Аванс на руки", formatMoney(state.payroll.netAdvanceAfterDeductions), emphasize = true)
            HorizontalDivider(modifier = Modifier.padding(vertical = appScaledSpacing(6.dp)))
            DiagnosticsRow("К зарплате (до НДФЛ)", formatMoney(state.payroll.salaryGrossAmount))
            DiagnosticsRow("К зарплате НДФЛ", formatMoney(state.payroll.salaryNdflAmount))
            DiagnosticsRow("К зарплате на руки", formatMoney(state.payroll.netSalaryAfterDeductions), emphasize = true)
        }

        DiagnosticsSection(title = "Проверка формул") {
            DiagnosticsCheckRow(
                title = "Начислено = Облагаемая + Необлагаемая",
                left = state.payroll.grossTotal,
                right = state.payroll.taxableGrossTotal + state.payroll.nonTaxableTotal
            )
            DiagnosticsCheckRow(
                title = "На руки = Начислено - НДФЛ",
                left = state.payroll.netTotal,
                right = state.payroll.grossTotal - state.payroll.ndfl
            )
            DiagnosticsCheckRow(
                title = "Выплаты gross: аванс + зарплата = начислено",
                left = state.payroll.advanceGrossAmount + state.payroll.salaryGrossAmount,
                right = state.payroll.grossTotal
            )
            DiagnosticsCheckRow(
                title = "Выплаты net: аванс + зарплата = на руки",
                left = state.payroll.advanceNetAmount + state.payroll.salaryNetAmount,
                right = state.payroll.netTotal
            )
            DiagnosticsCheckRow(
                title = "НДФЛ: аванс + зарплата = общий НДФЛ",
                left = state.payroll.advanceNdflAmount + state.payroll.salaryNdflAmount,
                right = state.payroll.ndfl
            )
            DiagnosticsCheckRow(
                title = "После удержаний: аванс + зарплата = общий итог",
                left = state.payroll.netAdvanceAfterDeductions + state.payroll.netSalaryAfterDeductions,
                right = state.payroll.netAfterDeductions
            )
        }

        DiagnosticsSection(title = "Служебно") {
            DiagnosticsRow("Активных допвыплат", state.resolvedAdditionalPayments.size.toString())
            DiagnosticsRow("Активных удержаний", state.deductions.count { it.active }.toString())
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(92.dp)))
    }
}

@Composable
private fun DiagnosticsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = appBlockSpacing()),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun DiagnosticsRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DiagnosticsCheckRow(
    title: String,
    left: Double,
    right: Double
) {
    val diff = abs(left - right)
    val ok = diff <= 0.02
    val resultColor = if (ok) {
        Color(0xFF2E7D32)
    } else {
        MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp))
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${formatMoney(left)}  vs  ${formatMoney(right)}",
            style = MaterialTheme.typography.labelMedium,
            color = appListSecondaryTextColor()
        )
        Text(
            text = if (ok) "OK (Δ ${formatMoney(diff)})" else "Есть расхождение (Δ ${formatMoney(diff)})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = resultColor
        )
    }
}
