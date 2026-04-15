package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsNavigationCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(appCornerRadius(18.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), shape)
            .clickable(onClick = appHapticAction(onAction = onClick))
            .padding(horizontal = appScreenPadding(), vertical = appScaledSpacing(18.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProductionCalendarSettingsCard(
    statusText: String?,
    isSyncing: Boolean,
    onSync: () -> Unit
) {
    val shape = RoundedCornerShape(appCornerRadius(18.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), shape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                text = "Производственный календарь",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Загрузка федеральных праздников и сокращённых дней из интернета",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(appCornerRadius(17.dp)))
                    .background(
                        if (isSyncing) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    )
                    .border(1.dp, appPanelBorderColor(), RoundedCornerShape(appCornerRadius(17.dp)))
                    .clickable(enabled = !isSyncing, onClick = appHapticAction(onAction = onSync)),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Синхронизировать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (!statusText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            AppFeedbackCard(
                message = statusText,
                state = inferAppFeedbackState(statusText)
            )
        }
    }
}
