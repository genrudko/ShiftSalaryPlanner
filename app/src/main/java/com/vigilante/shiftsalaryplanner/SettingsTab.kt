package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings

@Composable
fun SettingsTab(
    payrollSettings: PayrollSettings,
    additionalPaymentsCount: Int,
    deductionsCount: Int,
    manualHolidayCount: Int,
    isHolidaySyncing: Boolean,
    holidaySyncMessage: String?,
    onOpenPayrollSettings: () -> Unit,
    onOpenColorSettings: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenDeductions: () -> Unit,
    onOpenCurrentParameters: () -> Unit,
    onOpenManualHolidays: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenExcelImport: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
    onSyncProductionCalendar: () -> Unit,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsNavigationCard(
            title = "Расчёт зарплаты",
            subtitle = "Оклад, надбавка, НДФЛ, даты выплат, норма часов",
            onClick = onOpenPayrollSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsNavigationCard(
            title = "Цвета календаря",
            subtitle = "Цвета смен и пустых дней",
            onClick = onOpenColorSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProductionCalendarSettingsCard(
            statusText = holidaySyncMessage,
            isSyncing = isHolidaySyncing,
            onSync = onSyncProductionCalendar
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsNavigationCard(
            title = "Ручные праздники",
            subtitle = if (manualHolidayCount > 0) {
                "Добавлены вручную: $manualHolidayCount"
            } else {
                "Региональные праздники и свои особые дни"
            },
            onClick = onOpenManualHolidays
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsNavigationCard(
            title = "Допвыплаты и надбавки",
            subtitle = "Открывается отдельным вложенным экраном • записей: $additionalPaymentsCount",
            onClick = onOpenPayments
        )

        Spacer(modifier = Modifier.height(12.dp))
        SettingsNavigationCard(
            title = "Удержания после НДФЛ",
            subtitle = "Алименты, исполнительные и прочие • записей: $deductionsCount",
            onClick = onOpenDeductions
        )

        Spacer(modifier = Modifier.height(12.dp))
        SettingsNavigationCard(
            title = "Резервная копия",
            subtitle = "Экспорт и импорт смен, шаблонов, зарплатных настроек и будильников",
            onClick = onOpenBackupRestore
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsNavigationCard(
            title = "Импорт графика",
            subtitle = "Импорт смен из Excel",
            onClick = onOpenExcelImport
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsNavigationCard(
            title = "Виджет",
            subtitle = "Тема виджета, подписи смен и отдельные цвета карточек",
            onClick = onOpenWidgetSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsNavigationCard(
            title = "Текущие параметры",
            subtitle = buildString {
                append(payModeLabel(payrollSettings.payMode))
                append(" • Оклад ")
                append(formatMoney(payrollSettings.baseSalary))
            },
            onClick = onOpenCurrentParameters
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}