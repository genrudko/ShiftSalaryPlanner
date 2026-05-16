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
import androidx.compose.ui.unit.dp

enum class AppHealthSeverity {
    OK,
    INFO,
    WARNING,
    ERROR
}

data class AppHealthCheckItem(
    val title: String,
    val message: String,
    val severity: AppHealthSeverity = AppHealthSeverity.OK,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@Composable
fun AppHealthCheckScreen(
    items: List<AppHealthCheckItem>,
    onBack: () -> Unit,
    onRunMonthCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val problemCount = items.count { it.severity == AppHealthSeverity.WARNING || it.severity == AppHealthSeverity.ERROR }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding())
    ) {
        AppServiceScreenHeader(
            title = "Проверка",
            subtitle = if (problemCount == 0) "Всё выглядит нормально" else "Нужно внимание: $problemCount",
            onBack = onBack,
            trailing = {
                TextButton(onClick = appHapticAction(onAction = onRunMonthCheck)) {
                    Text("Проверить")
                }
            }
        )

        Spacer(modifier = Modifier.height(appSectionSpacing()))

        Column(verticalArrangement = Arrangement.spacedBy(appBlockSpacing())) {
            items.forEach { item ->
                HealthCheckRow(item = item)
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(96.dp)))
    }
}

@Composable
private fun HealthCheckRow(item: AppHealthCheckItem) {
    val accent = when (item.severity) {
        AppHealthSeverity.OK -> MaterialTheme.colorScheme.primary
        AppHealthSeverity.INFO -> MaterialTheme.colorScheme.tertiary
        AppHealthSeverity.WARNING -> MaterialTheme.colorScheme.error.copy(alpha = 0.82f)
        AppHealthSeverity.ERROR -> MaterialTheme.colorScheme.error
    }
    val label = when (item.severity) {
        AppHealthSeverity.OK -> "OK"
        AppHealthSeverity.INFO -> "Info"
        AppHealthSeverity.WARNING -> "Внимание"
        AppHealthSeverity.ERROR -> "Ошибка"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.24f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            val action = item.onAction
            if (!item.actionLabel.isNullOrBlank() && action != null) {
                TextButton(
                    onClick = appHapticAction(onAction = action),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(item.actionLabel)
                }
            }
        }
    }
}
