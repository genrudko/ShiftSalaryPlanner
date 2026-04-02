package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings

@Suppress("unused")
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
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Основные параметры, календарь, данные и служебные функции",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionTitle("Основное")

        Spacer(modifier = Modifier.height(8.dp))

        CompactFeatureTile(
            title = "Расчёт зарплаты",
            subtitle = "Оклад, НДФЛ, даты выплат, норма часов и правила расчёта",
            meta = payModeLabel(payrollSettings.payMode),
            onClick = onOpenPayrollSettings,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactFeatureTile(
                title = "Параметры",
                subtitle = "Текущий режим, суммы и активные значения",
                meta = formatMoney(payrollSettings.baseSalary),
                onClick = onOpenCurrentParameters,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            CompactFeatureTile(
                title = "Виджет",
                subtitle = "Оформление, подписи и внешний вид",
                onClick = onOpenWidgetSettings,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionTitle("Календарь")

        Spacer(modifier = Modifier.height(8.dp))

        CompactProductionCalendarTile(
            statusText = holidaySyncMessage,
            isSyncing = isHolidaySyncing,
            manualHolidayCount = manualHolidayCount,
            onSync = onSyncProductionCalendar,
            onOpenManualHolidays = onOpenManualHolidays
        )

        Spacer(modifier = Modifier.height(8.dp))

        CompactFeatureTile(
            title = "Импорт",
            subtitle = "График смен из Excel",
            onClick = onOpenExcelImport,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionTitle("Начисления и удержания")

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactFeatureTile(
                title = "Допвыплаты",
                subtitle = "Надбавки и премии",
                meta = "$additionalPaymentsCount запис.",
                onClick = onOpenPayments,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            CompactFeatureTile(
                title = "Удержания",
                subtitle = "После НДФЛ",
                meta = "$deductionsCount запис.",
                onClick = onOpenDeductions,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionTitle("Данные")

        Spacer(modifier = Modifier.height(8.dp))

        CompactFeatureTile(
            title = "Резервная копия",
            subtitle = "Экспорт и импорт смен, шаблонов, настроек и будильников",
            onClick = onOpenBackupRestore,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CompactFeatureTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!meta.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = meta,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactProductionCalendarTile(
    statusText: String?,
    isSyncing: Boolean,
    manualHolidayCount: Int,
    onSync: () -> Unit,
    onOpenManualHolidays: () -> Unit
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
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Text(
                text = "Производственный календарь",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Федеральные и ручные праздники, переносы, обновление",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = if (manualHolidayCount > 0) {
                        "Ручных праздников: $manualHolidayCount"
                    } else {
                        "Ручные праздники не добавлены"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenManualHolidays,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ручные праздники")
                }

                OutlinedButton(
                    onClick = onSync,
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSyncing) "Проверка..." else "Обновить")
                }
            }
        }
    }
}
