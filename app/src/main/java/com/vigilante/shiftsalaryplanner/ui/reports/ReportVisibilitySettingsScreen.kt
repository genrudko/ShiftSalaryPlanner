package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings

@Composable
fun ReportVisibilitySettingsScreen(
    settings: ReportVisibilitySettings,
    onBack: () -> Unit,
    onChange: (ReportVisibilitySettings) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(
                title = "Видимость строк",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(appScreenPadding()),
                verticalArrangement = Arrangement.spacedBy(appSectionSpacing())
            ) {
                VisibilityGroupCard(title = "Расчёт") {
                    VisibilitySubmenuCard(title = "Верхняя часть экрана") {
                        VisibilityCheckboxRow(
                            title = "Плитки: часы/смены",
                            checked = settings.showPayrollWorkedStatsRow,
                            onCheckedChange = { onChange(settings.copy(showPayrollWorkedStatsRow = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Плитки: аванс/к зарплате",
                            checked = settings.showPayrollPaymentsStatsRow,
                            onCheckedChange = { onChange(settings.copy(showPayrollPaymentsStatsRow = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Сводка расчёта»",
                            checked = settings.showPayrollSummaryCard,
                            onCheckedChange = { onChange(settings.copy(showPayrollSummaryCard = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Липкий итог внизу",
                            checked = settings.showPayrollStickyTotalsBar,
                            onCheckedChange = { onChange(settings.copy(showPayrollStickyTotalsBar = it)) }
                        )
                    }

                    VisibilitySubmenuCard(title = "Расчётный лист: разделы") {
                        VisibilityCheckboxRow(
                            title = "Общая информация",
                            checked = settings.showPayrollHeaderSection,
                            onCheckedChange = { onChange(settings.copy(showPayrollHeaderSection = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Начислено за месяц",
                            checked = settings.showPayrollAccrualSection,
                            onCheckedChange = { onChange(settings.copy(showPayrollAccrualSection = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Удержано за месяц",
                            checked = settings.showPayrollDeductionSection,
                            onCheckedChange = { onChange(settings.copy(showPayrollDeductionSection = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Аванс",
                            checked = settings.showPayrollPriorPaymentSection,
                            onCheckedChange = { onChange(settings.copy(showPayrollPriorPaymentSection = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "К зарплате",
                            checked = settings.showPayrollPayoutSection,
                            onCheckedChange = { onChange(settings.copy(showPayrollPayoutSection = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Итоги месяца",
                            checked = settings.showPayrollReferenceSection,
                            onCheckedChange = { onChange(settings.copy(showPayrollReferenceSection = it)) }
                        )
                    }
                }

                VisibilityGroupCard(title = "Выплаты") {
                    VisibilitySubmenuCard(title = "Верхняя панель") {
                        VisibilityCheckboxRow(
                            title = "Плитки действий (отчёт/видимость)",
                            checked = settings.showPaymentsActionTiles,
                            onCheckedChange = { onChange(settings.copy(showPaymentsActionTiles = it)) }
                        )
                    }

                    VisibilitySubmenuCard(title = "Главное за месяц") {
                        VisibilityCheckboxRow(
                            title = "Показывать раздел",
                            checked = settings.showPaymentsMainSummary,
                            onCheckedChange = { onChange(settings.copy(showPaymentsMainSummary = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Строка: аванс/к зарплате",
                            checked = settings.showPaymentsMainSummaryTopRow,
                            onCheckedChange = { onChange(settings.copy(showPaymentsMainSummaryTopRow = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Строка: на руки/смены",
                            checked = settings.showPaymentsMainSummaryBottomRow,
                            onCheckedChange = { onChange(settings.copy(showPaymentsMainSummaryBottomRow = it)) }
                        )
                    }

                    VisibilitySubmenuCard(title = "Выплаты и итог") {
                        VisibilityCheckboxRow(
                            title = "Показывать раздел",
                            checked = settings.showPaymentsPayoutAndTotals,
                            onCheckedChange = { onChange(settings.copy(showPaymentsPayoutAndTotals = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Выплаты»",
                            checked = settings.showPaymentsPayoutCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsPayoutCard = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Итоги начисления»",
                            checked = settings.showPaymentsTotalsCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsTotalsCard = it)) }
                        )
                    }

                    VisibilitySubmenuCard(title = "Смены и стоимость") {
                        VisibilityCheckboxRow(
                            title = "Показывать раздел",
                            checked = settings.showPaymentsShiftCosts,
                            onCheckedChange = { onChange(settings.copy(showPaymentsShiftCosts = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Статистика смен»",
                            checked = settings.showPaymentsShiftStatsCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsShiftStatsCard = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Стоимость смены»",
                            checked = settings.showPaymentsShiftCostCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsShiftCostCard = it)) }
                        )
                    }

                    VisibilitySubmenuCard(title = "Доплаты и премии") {
                        VisibilityCheckboxRow(
                            title = "Показывать раздел",
                            checked = settings.showPaymentsAdditionalPayments,
                            onCheckedChange = { onChange(settings.copy(showPaymentsAdditionalPayments = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Основные доплаты»",
                            checked = settings.showPaymentsBaseAllowanceCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsBaseAllowanceCard = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Доплаты месяца»",
                            checked = settings.showPaymentsMonthAdditionalCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsMonthAdditionalCard = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Настроенные начисления»",
                            checked = settings.showPaymentsConfiguredAdditionalCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsConfiguredAdditionalCard = it)) }
                        )
                    }

                    VisibilitySubmenuCard(title = "Отсутствия и переработка") {
                        VisibilityCheckboxRow(
                            title = "Показывать раздел",
                            checked = settings.showPaymentsAbsenceAndOvertime,
                            onCheckedChange = { onChange(settings.copy(showPaymentsAbsenceAndOvertime = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Отпуск и больничный»",
                            checked = settings.showPaymentsAbsenceCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsAbsenceCard = it)) }
                        )
                        VisibilityCheckboxRow(
                            title = "Карточка «Сверхурочка»",
                            checked = settings.showPaymentsOvertimeCard,
                            onCheckedChange = { onChange(settings.copy(showPaymentsOvertimeCard = it)) }
                        )
                    }
                }

                Text(
                    text = "Изменения сохраняются сразу. Подменю можно сворачивать.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )

                Spacer(modifier = Modifier.height(appScaledSpacing(36.dp)))
            }
        }
    }
}

@Composable
private fun VisibilityGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCardRadius()),
        color = appPanelColor(),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun VisibilitySubmenuCard(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    val shape = RoundedCornerShape(appCornerRadius(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f), shape)
            .padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(6.dp)),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = appHapticAction(onAction = { expanded = !expanded }))
                .padding(horizontal = appScaledSpacing(4.dp), vertical = appScaledSpacing(2.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun VisibilityCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val rowShape = RoundedCornerShape(appCornerRadius(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f), rowShape)
            .clickable(onClick = appHapticAction(onAction = { onCheckedChange(!checked) }))
            .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(4.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) }
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
