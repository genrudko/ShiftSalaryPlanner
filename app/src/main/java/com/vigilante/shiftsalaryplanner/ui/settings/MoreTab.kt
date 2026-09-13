package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class MoreGroupKey {
    Work,
    Tools,
    App,
    Data,
    Advanced
}

private data class MoreDestination(
    val title: String,
    val subtitle: String,
    val glyph: String,
    val onClick: () -> Unit
)

private data class MoreGroup(
    val key: MoreGroupKey,
    val title: String,
    val destinations: List<MoreDestination>
)

@Composable
fun MoreTab(
    currentProfileLabel: String,
    appearanceSummary: String,
    onOpenWorkplaces: () -> Unit,
    onOpenShiftTemplates: () -> Unit,
    onOpenManualHolidays: () -> Unit,
    onSyncProductionCalendar: () -> Unit,
    isHolidaySyncing: Boolean,
    holidaySyncMessage: String?,
    onOpenAlarms: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenQuickActions: () -> Unit,
    onOpenWear: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenGoogleDrive: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenReportCenter: () -> Unit,
    onOpenHealthCheck: () -> Unit,
    onOpenEventLog: () -> Unit,
    onOpenCurrentParameters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val productionCalendarSubtitle = when {
        isHolidaySyncing -> "Обновление календаря…"
        !holidaySyncMessage.isNullOrBlank() -> holidaySyncMessage
        else -> "Федеральные праздники и сокращённые дни"
    }
    val groups = listOf(
        MoreGroup(
            key = MoreGroupKey.Work,
            title = "Работа",
            destinations = listOf(
                MoreDestination("Рабочие места", "Работы, контекст смен и расчёта", "Р", onOpenWorkplaces),
                MoreDestination("Шаблоны смен", "Смены, статусы и чередования", "С", onOpenShiftTemplates),
                MoreDestination("Производственный календарь", productionCalendarSubtitle, "К", onSyncProductionCalendar),
                MoreDestination("Ручные праздники", "Региональные праздники и особые дни", "П", onOpenManualHolidays)
            )
        ),
        MoreGroup(
            key = MoreGroupKey.Tools,
            title = "Инструменты",
            destinations = listOf(
                MoreDestination("Будильники", "Расписание и поведение будильников", "Б", onOpenAlarms),
                MoreDestination("Заметки", "Записи по дням и сменам", "З", onOpenNotes),
                MoreDestination("ИИ-ассистент", "Экспериментальные сценарии помощника", "ИИ", onOpenAssistant)
            )
        ),
        MoreGroup(
            key = MoreGroupKey.App,
            title = "Приложение",
            destinations = listOf(
                MoreDestination("Внешний вид", appearanceSummary, "Aa", onOpenAppearance),
                MoreDestination("Виджеты", "Настройки виджетов телефона", "W", onOpenWidgets),
                MoreDestination("Быстрые действия", "Кнопки быстрого ввода календаря", "↯", onOpenQuickActions),
                MoreDestination("Wear OS", "Зеркалирование и звук часов — в будильниках", "⌚", onOpenWear),
                MoreDestination("Профили", currentProfileLabel, "П", onOpenProfiles),
                MoreDestination("Быстрый старт", "Подсказки по основным возможностям", "?", onOpenQuickStart)
            )
        ),
        MoreGroup(
            key = MoreGroupKey.Data,
            title = "Данные",
            destinations = listOf(
                MoreDestination("Резервная копия", "Экспорт и восстановление данных", "↕", onOpenBackupRestore),
                MoreDestination("Google Drive", "Облачная копия находится в резервном копировании", "G", onOpenGoogleDrive),
                MoreDestination("Импорт / экспорт", "Импорт графика из Excel", "X", onOpenImport),
                MoreDestination("Центр отчётов", "PDF, CSV и история отчётов", "О", onOpenReportCenter)
            )
        ),
        MoreGroup(
            key = MoreGroupKey.Advanced,
            title = "Дополнительно",
            destinations = listOf(
                MoreDestination("Проверка приложения", "Разрешения и состояние сервисов", "✓", onOpenHealthCheck),
                MoreDestination("Журнал событий", "Последние действия приложения", "Ж", onOpenEventLog),
                MoreDestination("Текущие параметры", "Активные режимы и значения", "i", onOpenCurrentParameters)
            )
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        item("more-header") {
            Spacer(modifier = Modifier.height(appScreenPadding()))
            Text(
                text = "Ещё",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
            Text(
                text = "Работа, инструменты, приложение и данные — без перегруженной нижней панели",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(groups, key = { it.key.name }) { group ->
            MoreGroupCard(group)
        }

        item("more-bottom-space") {
            Spacer(modifier = Modifier.height(appScaledSpacing(24.dp)))
        }
    }
}

@Composable
private fun MoreGroupCard(group: MoreGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = appScaledSpacing(4.dp))
        )
        EvolutionSurface(
            modifier = Modifier.fillMaxWidth(),
            role = EvolutionSurfaceRole.SOFT,
            shape = RoundedCornerShape(appCornerRadius(22.dp)),
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                group.destinations.forEachIndexed { index, destination ->
                    MoreDestinationRow(destination)
                    if (index != group.destinations.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(start = appScaledSpacing(70.dp)),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreDestinationRow(destination: MoreDestination) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = appHapticAction(onAction = destination.onClick))
            .padding(horizontal = appScaledSpacing(14.dp), vertical = appScaledSpacing(11.dp)),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EvolutionIconTile(
            glyph = destination.glyph,
            contentDescription = null,
            tone = EvolutionIconTone.SECONDARY,
            size = 42.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = destination.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = destination.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
