package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info

enum class AppFeedbackState {
    LOADING,
    EMPTY,
    ERROR,
    SUCCESS,
    INFO
}

fun inferAppFeedbackState(message: String): AppFeedbackState {
    val normalized = message.lowercase()
    return when {
        normalized.contains("ошиб") || normalized.contains("не удалось") -> AppFeedbackState.ERROR
        normalized.contains("загруз") || normalized.contains("проверка") || normalized.contains("синхрон") -> AppFeedbackState.LOADING
        normalized.contains("готово") || normalized.contains("сохран") || normalized.contains("заверш") || normalized.contains("успеш") -> AppFeedbackState.SUCCESS
        normalized.contains("пуст") -> AppFeedbackState.EMPTY
        else -> AppFeedbackState.INFO
    }
}

@Composable
fun AppFeedbackCard(
    message: String,
    state: AppFeedbackState,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val (icon, color) = state.visuals()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(appCardRadius()),
        color = appPanelColor(),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appCardPadding()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AppEmptyCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(appCardRadius()),
        color = appPanelColor(),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding())
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppFeedbackState.visuals(): Pair<ImageVector, Color> {
    return when (this) {
        AppFeedbackState.LOADING -> Icons.Rounded.HourglassTop to MaterialTheme.colorScheme.primary
        AppFeedbackState.EMPTY -> Icons.Rounded.Inbox to MaterialTheme.colorScheme.onSurfaceVariant
        AppFeedbackState.ERROR -> Icons.Rounded.ErrorOutline to MaterialTheme.colorScheme.error
        AppFeedbackState.SUCCESS -> Icons.Rounded.CheckCircle to Color(0xFF2E7D32)
        AppFeedbackState.INFO -> Icons.Rounded.Info to MaterialTheme.colorScheme.secondary
    }
}

@Composable
fun AppCardSkeleton(
    modifier: Modifier = Modifier,
    lines: Int = 3
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCardRadius()),
        color = appPanelColor(),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            repeat(lines.coerceIn(1, 5)) { index ->
                AppSkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(if (index == lines - 1) 0.62f else 1f)
                        .height(if (index == 0) 16.dp else 12.dp)
                )
            }
        }
    }
}

@Composable
fun AppSkeletonBlock(
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "app-skeleton")
    val animatedAlpha = pulse.animateFloat(
        initialValue = 0.34f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = appAnimationDurationMillis(850).coerceAtLeast(1)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "app-skeleton-alpha"
    )
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = animatedAlpha.value),
                shape = RoundedCornerShape(appCornerRadius(8.dp))
            )
    )
}
