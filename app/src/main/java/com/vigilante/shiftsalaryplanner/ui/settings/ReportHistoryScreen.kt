package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryItem
import java.time.Instant
import java.time.ZoneId

@Composable
fun ReportHistoryScreen(
    items: List<ReportHistoryItem>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "История отчётов",
            subtitle = "Снимки экспортированных расчётов",
            onBack = onBack,
            trailing = {
                if (items.isNotEmpty()) {
                    TextButton(onClick = appHapticAction(onAction = onClear)) {
                        Text("Очистить")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        if (items.isEmpty()) {
            AppEmptyCard(
                title = "История пока пустая",
                message = "Экспортируй PDF или месячный отчёт, и здесь появится снимок итогов."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(appBlockSpacing())) {
                items.forEach { item ->
                    ReportHistoryRow(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

@Composable
private fun ReportHistoryRow(item: ReportHistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.24f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(item.periodLabel, item.workplaceLabel)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = item.format.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AppServiceDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportMiniMetric("Начислено", formatMoney(item.gross))
                ReportMiniMetric("НДФЛ", formatMoney(item.ndfl))
                ReportMiniMetric("На руки", formatMoney(item.net))
            }
            Text(
                text = "${formatReportHistoryTime(item.timestampMillis)} • ${item.fileName}",
                style = MaterialTheme.typography.labelSmall,
                color = appListSecondaryTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReportMiniMetric(title: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = appListSecondaryTextColor()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatReportHistoryTime(timestampMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return "${formatDate(dateTime.toLocalDate())} ${formatClockHm(dateTime.hour, dateTime.minute)}"
}
