package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.ui.theme.AnimationSpeedMode
import com.vigilante.shiftsalaryplanner.ui.theme.CornerStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.LocalAppAppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.UiContrastMode
import com.vigilante.shiftsalaryplanner.ui.theme.UiDensityMode
import com.vigilante.shiftsalaryplanner.ui.theme.sanitizeHexColor
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun appPanelColor(): Color {
    return MaterialTheme.colorScheme.surface
}

@Composable
fun appPanelBorderColor(): Color {
    return MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
}

@Composable
fun appListSecondaryTextColor(alpha: Float = 1f): Color {
    val scheme = MaterialTheme.colorScheme
    val highContrast = LocalAppAppearanceSettings.current.uiContrastMode == UiContrastMode.HIGH
    val base = if (scheme.background.luminance() < 0.5f) {
        lerp(
            scheme.onSurfaceVariant,
            scheme.onSurface,
            if (highContrast) 0.46f else 0.22f
        )
    } else {
        if (highContrast) lerp(scheme.onSurfaceVariant, scheme.onSurface, 0.20f) else scheme.onSurfaceVariant
    }
    return base.copy(alpha = alpha.coerceIn(0f, 1f))
}

@Composable
fun appInnerSurfaceColor(): Color {
    return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
}

@Composable
fun appBubbleBackgroundColor(defaultAlpha: Float = 0.24f): Color {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val baseAlpha = defaultAlpha
        .coerceIn(0f, 1f)
        .coerceAtLeast(if (isDark) 0.30f else 0.22f)
    var fallback = scheme.surfaceVariant.copy(alpha = baseAlpha)
    val luminanceDelta = abs(scheme.background.luminance() - fallback.luminance())
    if (luminanceDelta < 0.055f) {
        fallback = lerp(
            scheme.surfaceVariant,
            scheme.onSurface,
            if (isDark) 0.15f else 0.09f
        ).copy(alpha = (baseAlpha + 0.05f).coerceAtMost(0.44f))
    }
    val customHex = LocalAppAppearanceSettings.current.customBubbleHex.trim()
    if (customHex.isBlank()) return fallback

    val parsed = runCatching {
        Color(android.graphics.Color.parseColor(sanitizeHexColor(customHex, "#E3E8EF")))
    }.getOrNull() ?: return fallback

    val mix = if (scheme.background.luminance() < 0.5f) 0.34f else 0.28f
    return lerp(scheme.surface, parsed, mix)
}

@Composable
fun appIsCompactMode(): Boolean {
    return LocalAppAppearanceSettings.current.uiDensityMode == UiDensityMode.COMPACT
}

@Composable
fun appFontScale(): Float {
    return LocalDensity.current.fontScale
}

@Composable
fun appShouldHideBottomBarLabels(): Boolean {
    return appFontScale() > 1.15f
}

@Composable
fun appScaledSpacing(base: Dp): Dp {
    return if (appIsCompactMode()) {
        (base * 0.78f).coerceAtLeast(2.dp)
    } else {
        base
    }
}

@Composable
fun appScreenPadding(): Dp {
    return appScaledSpacing(16.dp)
}

@Composable
fun appCardPadding(): Dp {
    return appScaledSpacing(12.dp)
}

@Composable
fun appCardRadius(): Dp {
    return appCornerRadius(18.dp)
}

@Composable
fun appSectionSpacing(): Dp {
    return appScaledSpacing(12.dp)
}

@Composable
fun appBlockSpacing(): Dp {
    return appScaledSpacing(8.dp)
}

@Composable
fun appInputFieldHeight(base: Dp = 36.dp): Dp {
    return if (appIsCompactMode()) {
        (base * 0.74f).coerceAtLeast(28.dp)
    } else {
        base
    }
}

@Composable
fun appLargeButtonHeight(base: Dp = 48.dp): Dp {
    return if (appIsCompactMode()) {
        (base * 0.84f).coerceAtLeast(40.dp)
    } else {
        base
    }
}

@Composable
fun appFabButtonSize(base: Dp = 44.dp): Dp {
    return if (appIsCompactMode()) {
        (base * 0.9f).coerceAtLeast(40.dp)
    } else {
        base
    }
}

@Composable
fun Modifier.appLargeButtonSizing(base: Dp = 48.dp): Modifier {
    return this.heightIn(min = appLargeButtonHeight(base))
}

@Composable
fun appCornerRadius(base: Dp): Dp {
    val factor = when (LocalAppAppearanceSettings.current.cornerStyleMode) {
        CornerStyleMode.SOFT -> 1.22f
        CornerStyleMode.STANDARD -> 1f
        CornerStyleMode.SHARP -> 0.68f
    }
    return (base * factor).coerceAtLeast(6.dp)
}

@Composable
fun appAnimationDurationMillis(baseMs: Int): Int {
    return when (LocalAppAppearanceSettings.current.animationSpeedMode) {
        AnimationSpeedMode.NORMAL -> baseMs
        AnimationSpeedMode.SLOW -> (baseMs * 1.45f).roundToInt()
        AnimationSpeedMode.OFF -> 0
    }
}

enum class AppHapticKind {
    SOFT,
    CONFIRM
}

@Composable
fun appHapticAction(
    kind: AppHapticKind = AppHapticKind.SOFT,
    onAction: () -> Unit
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val feedbackType = when (kind) {
        AppHapticKind.SOFT -> HapticFeedbackType.TextHandleMove
        AppHapticKind.CONFIRM -> HapticFeedbackType.LongPress
    }
    return {
        haptic.performHapticFeedback(feedbackType)
        onAction()
    }
}

@Composable
fun BackCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner = appCornerRadius(14.dp)
    Box(
        modifier = modifier
            .size(appFabButtonSize(40.dp))
            .clip(RoundedCornerShape(corner))
            .background(appInnerSurfaceColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(corner))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Назад",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
