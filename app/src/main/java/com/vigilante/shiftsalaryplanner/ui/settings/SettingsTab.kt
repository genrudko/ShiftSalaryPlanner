package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings

@Suppress("unused")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsTab(
    payrollSettings: PayrollSettings,
    appearanceSummary: String,
    additionalPaymentsCount: Int,
    deductionsCount: Int,
    manualHolidayCount: Int,
    isHolidaySyncing: Boolean,
    holidaySyncMessage: String?,
    onOpenPayrollSettings: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onOpenReportVisibilitySettings: () -> Unit,
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        item("settings-header") {
            Spacer(modifier = Modifier.height(appScreenPadding()))
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
            Text(
                text = "Основные параметры, календарь, данные и служебные функции",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            Spacer(modifier = Modifier.height(appSectionSpacing()))
        }

        stickyHeader("settings-section-main") {
            SettingsStickyHeader("Основное")
        }
        item("settings-main-payroll") {
            CompactFeatureTile(
                title = "Расчёт зарплаты",
                subtitle = "Оклад, НДФЛ, даты выплат, норма часов и правила расчёта",
                meta = payModeLabel(payrollSettings.payMode),
                onClick = onOpenPayrollSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item("settings-main-appearance") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                CompactFeatureTile(
                    title = "Внешний вид",
                    subtitle = "Тема, цветовая схема и шрифт",
                    meta = appearanceSummary,
                    onClick = onOpenAppearanceSettings,
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
        }
        item("settings-main-current") {
            CompactFeatureTile(
                title = "Параметры",
                subtitle = "Текущий режим, суммы и активные значения",
                meta = formatMoney(payrollSettings.baseSalary),
                onClick = onOpenCurrentParameters,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item("settings-main-visibility") {
            CompactFeatureTile(
                title = "Видимость строк",
                subtitle = "Точная настройка, какие блоки показывать в «Расчёте» и «Выплатах»",
                meta = "Подменю и чекбоксы",
                onClick = onOpenReportVisibilitySettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item("settings-main-colors") {
            CompactFeatureTile(
                title = "Цвета смен",
                subtitle = "Цвета карточек и меток смен в календаре",
                onClick = onOpenColorSettings,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(appSectionSpacing()))
        }

        stickyHeader("settings-section-calendar") {
            SettingsStickyHeader("Календарь")
        }
        item("settings-calendar-main") {
            CompactProductionCalendarTile(
                statusText = holidaySyncMessage,
                isSyncing = isHolidaySyncing,
                manualHolidayCount = manualHolidayCount,
                onSync = onSyncProductionCalendar,
                onOpenManualHolidays = onOpenManualHolidays
            )
        }
        item("settings-calendar-import") {
            CompactFeatureTile(
                title = "Импорт",
                subtitle = "График смен из Excel",
                onClick = onOpenExcelImport,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(appSectionSpacing()))
        }

        stickyHeader("settings-section-payments") {
            SettingsStickyHeader("Начисления и удержания")
        }
        item("settings-payments-cards") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
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
            Spacer(modifier = Modifier.height(appSectionSpacing()))
        }

        stickyHeader("settings-section-data") {
            SettingsStickyHeader("Данные")
        }
        item("settings-data-backup") {
            CompactFeatureTile(
                title = "Резервная копия",
                subtitle = "Экспорт и импорт смен, шаблонов, настроек и будильников",
                onClick = onOpenBackupRestore,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(appScaledSpacing(112.dp)))
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = appListSecondaryTextColor()
    )
}

@Composable
private fun SettingsStickyHeader(
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = appScaledSpacing(6.dp), bottom = appScaledSpacing(2.dp))
        ) {
            SettingsSectionTitle(text)
        }
    }
}

@Composable
private fun CompactFeatureTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null
) {
    val shape = RoundedCornerShape(appCornerRadius(18.dp))
    Surface(
        modifier = modifier.clickable(onClick = appHapticAction(onAction = onClick)),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appScaledSpacing(11.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(appScaledSpacing(3.dp)))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            if (!meta.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(appBlockSpacing()))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = meta,
                        modifier = Modifier.padding(
                            horizontal = appScaledSpacing(8.dp),
                            vertical = appScaledSpacing(4.dp)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
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
    val shape = RoundedCornerShape(appCornerRadius(18.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding(), vertical = appScaledSpacing(11.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Производственный календарь",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                CompactCalendarIconAction(
                    icon = Icons.Rounded.EditCalendar,
                    contentDescription = "Ручные праздники",
                    onClick = onOpenManualHolidays
                )
                CompactCalendarIconAction(
                    icon = Icons.Rounded.Sync,
                    contentDescription = if (isSyncing) "Синхронизация выполняется" else "Синхронизация календаря",
                    enabled = !isSyncing,
                    onClick = onSync
                )
            }
            Spacer(modifier = Modifier.height(appScaledSpacing(3.dp)))
            Text(
                text = "Федеральные и ручные праздники, переносы, обновление",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            ) {
                Text(
                    text = if (manualHolidayCount > 0) {
                        "Ручных праздников: $manualHolidayCount"
                    } else {
                        "Ручные праздники не добавлены"
                    },
                    modifier = Modifier.padding(
                        horizontal = appScaledSpacing(8.dp),
                        vertical = appScaledSpacing(4.dp)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )
            }

            if (!statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(appBlockSpacing()))
                if (isSyncing) {
                    AppCardSkeleton(lines = 2)
                } else {
                    AppFeedbackCard(
                        message = statusText,
                        state = inferAppFeedbackState(statusText)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactCalendarIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(appCornerRadius(10.dp))
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        appListSecondaryTextColor(alpha = 0.74f)
    }

    Surface(
        modifier = Modifier
            .height(28.dp)
            .clickable(enabled = enabled, onClick = appHapticAction(onAction = onClick)),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor
            )
        }
    }
}
