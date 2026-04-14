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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
            .padding(AppSpacing.lg)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = "Основные параметры, календарь, данные и служебные функции",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        SettingsSectionTitle("Основное")

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        CompactFeatureTile(
            title = "Расчёт зарплаты",
            subtitle = "Оклад, НДФЛ, даты выплат, норма часов и правила расчёта",
            meta = payModeLabel(payrollSettings.payMode),
            onClick = onOpenPayrollSettings,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

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
                title = "Внешний вид",
                subtitle = "Палитра смен и live-preview",
                onClick = onOpenColorSettings,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        CompactFeatureTile(
            title = "Виджет",
            subtitle = "Оформление, подписи и внешний вид",
            onClick = onOpenWidgetSettings,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        SettingsSectionTitle("Календарь")

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        CompactProductionCalendarTile(
            statusText = holidaySyncMessage,
            isSyncing = isHolidaySyncing,
            manualHolidayCount = manualHolidayCount,
            onSync = onSyncProductionCalendar,
            onOpenManualHolidays = onOpenManualHolidays
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        CompactFeatureTile(
            title = "Импорт",
            subtitle = "График смен из Excel",
            onClick = onOpenExcelImport,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        SettingsSectionTitle("Начисления и удержания")

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
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

        Spacer(modifier = Modifier.height(AppSpacing.md))

        SettingsSectionTitle("Данные")

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        CompactFeatureTile(
            title = "Резервная копия",
            subtitle = "Экспорт и импорт смен, шаблонов, настроек и будильников",
            onClick = onOpenBackupRestore,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppSpacing.xl))
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
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(AppRadius.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!meta.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Surface(
                    shape = RoundedCornerShape(AppRadius.pill),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = meta,
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
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
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
        ) {
            Text(
                text = "Производственный календарь",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "Федеральные и ручные праздники, переносы, обновление",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Surface(
                shape = RoundedCornerShape(AppRadius.pill),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = if (manualHolidayCount > 0) {
                        "Ручных праздников: $manualHolidayCount"
                    } else {
                        "Ручные праздники не добавлены"
                    },
                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                UiStateCard(
                    title = "Идёт проверка",
                    message = "Обновляем календарь и сверяем изменения",
                    kind = UiStateKind.LOADING
                )
            } else if (!statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                UiStateCard(
                    title = if (statusText.contains("ошиб", ignoreCase = true) || statusText.contains("не удалось", ignoreCase = true)) {
                        "Ошибка синхронизации"
                    } else {
                        "Состояние календаря"
                    },
                    message = statusText,
                    kind = if (statusText.contains("ошиб", ignoreCase = true) || statusText.contains("не удалось", ignoreCase = true)) {
                        UiStateKind.ERROR
                    } else {
                        UiStateKind.SUCCESS
                    }
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm + AppSpacing.xxs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenManualHolidays()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ручные праздники")
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSync()
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSyncing) "Проверка..." else "Обновить")
                }
            }
        }
    }
}
