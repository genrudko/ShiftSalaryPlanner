package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettings
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryItem
import com.vigilante.shiftsalaryplanner.ui.today.todayBubblePadding

@Composable
fun TodayOverviewScreen(
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    paymentDates: PaymentDates,
    payroll: PayrollResult,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "Сегодня",
            subtitle = "Смена, будильник и ближайшие выплаты",
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(appSectionSpacing()))

        ServiceInfoCard("Смены") {
            ServiceMetricRow("Сегодня", todaySummary.ifBlank { "смен нет" })
            ServiceMetricRow("Завтра", tomorrowSummary.ifBlank { "смен нет" })
            ServiceMetricRow("Будильник", nextAlarmSummary.ifBlank { "нет будущих срабатываний" })
        }
        ServiceInfoCard("Финансы") {
            ServiceMetricRow("Аванс", "${formatMoney(payroll.netAdvanceAfterDeductions)} · ${formatDate(paymentDates.advanceDate)}")
            ServiceMetricRow("Зарплата", "${formatMoney(payroll.netSalaryAfterDeductions)} · ${formatDate(paymentDates.salaryDate)}")
            ServiceMetricRow("Итого на руки", formatMoney(payroll.netTotal), emphasize = true)
        }
        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

@Composable
fun QuickActionsSettingsScreen(
    settings: AppWorkflowSettings,
    onChange: (AppWorkflowSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "Быстрые действия",
            subtitle = "Какие кнопки показывать в быстром вводе",
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(appSectionSpacing()))

        ServiceInfoCard("Основной ряд") {
            WorkflowSwitchRow("Ластик", settings.showQuickEraser) { onChange(settings.copy(showQuickEraser = it)) }
            WorkflowSwitchRow("Обычный режим", settings.showQuickNormal) { onChange(settings.copy(showQuickNormal = it)) }
            WorkflowSwitchRow("Цикл", settings.showQuickCycle) { onChange(settings.copy(showQuickCycle = it)) }
            WorkflowSwitchRow("Новый шаблон", settings.showQuickNewTemplate) { onChange(settings.copy(showQuickNewTemplate = it)) }
        }
        ServiceInfoCard("Очистка") {
            WorkflowSwitchRow("Очистить месяц", settings.showQuickClearMonth) { onChange(settings.copy(showQuickClearMonth = it)) }
            WorkflowSwitchRow("Очистить диапазон", settings.showQuickClearRange) { onChange(settings.copy(showQuickClearRange = it)) }
            WorkflowSwitchRow("Очистить календарь", settings.showQuickClearAll) { onChange(settings.copy(showQuickClearAll = it)) }
        }
        ServiceInfoCard("Свайпы в списках") {
            Text(
                text = "Можно отключить опасные или лишние жесты отдельно для смен и будильников.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            WorkflowSwitchRow("Смены: дублировать вправо", settings.shiftSwipeDuplicateEnabled) {
                onChange(settings.copy(shiftSwipeDuplicateEnabled = it))
            }
            WorkflowSwitchRow("Смены: удалить влево", settings.shiftSwipeDeleteEnabled) {
                onChange(settings.copy(shiftSwipeDeleteEnabled = it))
            }
            WorkflowSwitchRow("Будильники: дублировать вправо", settings.alarmSwipeDuplicateEnabled) {
                onChange(settings.copy(alarmSwipeDuplicateEnabled = it))
            }
            WorkflowSwitchRow("Будильники: удалить влево", settings.alarmSwipeDeleteEnabled) {
                onChange(settings.copy(alarmSwipeDeleteEnabled = it))
            }
        }
        ServiceInfoCard("Факт выплат") {
            Text(
                text = "Если расчёт и фактическая выплата отличаются меньше этого значения, подсветка останется спокойной.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            CompactDecimalField(
                label = "Допуск, ₽",
                value = formatDouble(settings.paymentDifferenceToleranceRub),
                onValueChange = { value ->
                    onChange(
                        settings.copy(
                            paymentDifferenceToleranceRub = parseWorkflowMoney(value)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

private fun parseWorkflowMoney(value: String): Double {
    return value
        .replace(" ", "")
        .replace(',', '.')
        .toDoubleOrNull()
        ?.coerceAtLeast(0.0)
        ?: 0.0
}

@Composable
fun ReportCenterScreen(
    historyItems: List<ReportHistoryItem>,
    onBack: () -> Unit,
    onOpenMonthlyReport: () -> Unit,
    onOpenPayrollPdf: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "Центр отчётов",
            subtitle = "Расчётные листы, месячные отчёты и история",
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(appSectionSpacing()))

        ServiceActionCard(
            title = "Расчётный лист PDF",
            subtitle = "Подробный лист по текущему периоду и выбранной работе",
            action = "Открыть",
            onClick = onOpenPayrollPdf
        )
        ServiceActionCard(
            title = "Месячный отчёт",
            subtitle = "CSV/PDF по начислениям, выплатам, сменам и сверхурочке",
            action = "Открыть",
            onClick = onOpenMonthlyReport
        )
        ServiceActionCard(
            title = "История экспортов",
            subtitle = if (historyItems.isEmpty()) "Пока пусто" else "Сохранено снимков: ${historyItems.size}",
            action = "История",
            onClick = onOpenHistory
        )
        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

@Composable
internal fun ServiceInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = appBlockSpacing()),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(todayBubblePadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
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
private fun ServiceActionCard(
    title: String,
    subtitle: String,
    action: String,
    onClick: () -> Unit
) {
    ServiceInfoCard(title = title) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor()
        )
        TextButton(
            onClick = appHapticAction(onAction = onClick),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(action)
        }
    }
}

@Composable
internal fun ServiceMetricRow(
    title: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun WorkflowSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = { value ->
                onCheckedChange(value)
            }
        )
    }
}
