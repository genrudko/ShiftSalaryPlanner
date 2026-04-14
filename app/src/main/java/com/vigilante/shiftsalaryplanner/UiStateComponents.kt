package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class UiStateKind {
    LOADING,
    EMPTY,
    ERROR,
    SUCCESS
}

@Composable
fun UiStateCard(
    title: String,
    message: String,
    kind: UiStateKind,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val (tint, icon) = when (kind) {
        UiStateKind.LOADING -> MaterialTheme.colorScheme.tertiary to Icons.Outlined.HourglassTop
        UiStateKind.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Outlined.Inbox
        UiStateKind.ERROR -> MaterialTheme.colorScheme.error to Icons.Outlined.ErrorOutline
        UiStateKind.SUCCESS -> Color(0xFF2E7D32) to Icons.Outlined.CheckCircle
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(AppRadius.lg))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(AppRadius.lg))
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        StateIconChip(
            icon = icon,
            tint = tint
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StateIconChip(
    icon: ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier
            .background(tint.copy(alpha = 0.16f), RoundedCornerShape(AppRadius.pill))
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
    }
}
