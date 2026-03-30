package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun appPanelColor(): Color {
    return if (isSystemInDarkTheme()) Color(0xFF181E2A) else MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun appPanelBorderColor(): Color {
    return if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
}

@Composable
fun appInnerSurfaceColor(): Color {
    return if (isSystemInDarkTheme()) Color(0xFF10151F) else MaterialTheme.colorScheme.surface
}

@Composable
fun BackCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(appInnerSurfaceColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "←",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}