package com.vigilante.shiftsalaryplanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.settings.AppEventLogItem
import java.time.Instant
import java.time.ZoneId

@Composable
fun AppEventLogScreen(
    events: List<AppEventLogItem>,
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
            title = "Журнал событий",
            subtitle = "Последние действия приложения",
            onBack = onBack,
            trailing = {
                if (events.isNotEmpty()) {
                    TextButton(onClick = appHapticAction(onAction = onClear)) {
                        Text("Очистить")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        if (events.isEmpty()) {
            AppEmptyCard(
                title = "Пока пусто",
                message = "Здесь появятся сохранения, бэкапы, перепланировки, экспорты и другие важные действия."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(appBlockSpacing())) {
                events.forEach { event ->
                    EventLogRow(event = event)
                }
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

@Composable
private fun EventLogRow(event: AppEventLogItem) {
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
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = event.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatEventTime(event.timestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }
            if (event.message.isNotBlank()) {
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
        }
    }
}

@Composable
fun AppServiceScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val backButtonSize = if (appIsCompactMode()) 32.dp else 36.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
    ) {
        Surface(
            modifier = Modifier.size(backButtonSize),
            shape = RoundedCornerShape(appCornerRadius(12.dp)),
            color = appInnerSurfaceColor(),
            border = BorderStroke(1.dp, appPanelBorderColor())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = appHapticAction(onAction = onBack)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun AppServiceDivider() {
    HorizontalDivider(color = appPanelBorderColor().copy(alpha = 0.7f))
}

private fun formatEventTime(timestampMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return "${formatDate(dateTime.toLocalDate())} ${formatClockHm(dateTime.hour, dateTime.minute)}"
}
