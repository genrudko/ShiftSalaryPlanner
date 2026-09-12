package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.LocalAppAppearanceSettings

enum class AppExpressiveSurfaceTone {
    PANEL,
    SOFT,
    ACCENT,
    FLOATING,
    GLASS
}

/**
 * Compatibility surface used across the pre-M9 UI.
 * Classic retains the old outlined panel behavior; Expressive modes delegate to
 * the Variant A surface system so existing screens can adopt the new grammar
 * without forcing a giant migration.
 */
@Composable
fun AppExpressiveSurface(
    modifier: Modifier = Modifier,
    tone: AppExpressiveSurfaceTone = AppExpressiveSurfaceTone.PANEL,
    shape: Shape = RoundedCornerShape(appCardRadius()),
    border: BorderStroke? = null,
    shadowElevation: Dp = expressiveSurfaceElevation(tone),
    content: @Composable BoxScope.() -> Unit
) {
    val mode = LocalAppAppearanceSettings.current.visualStyleMode
    if (mode == AppVisualStyleMode.CLASSIC) {
        val palette = classicExpressiveSurfacePalette(tone)
        Surface(
            modifier = modifier,
            shape = shape,
            color = palette.base,
            border = border ?: BorderStroke(1.dp, palette.border),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(content = content)
        }
        return
    }

    EvolutionSurface(
        modifier = modifier,
        role = tone.toEvolutionRole(),
        shape = shape,
        border = border,
        shadowElevation = shadowElevation,
        content = content
    )
}

private fun AppExpressiveSurfaceTone.toEvolutionRole(): EvolutionSurfaceRole = when (this) {
    AppExpressiveSurfaceTone.PANEL -> EvolutionSurfaceRole.PRIMARY
    AppExpressiveSurfaceTone.SOFT -> EvolutionSurfaceRole.SOFT
    AppExpressiveSurfaceTone.ACCENT -> EvolutionSurfaceRole.ACCENT
    AppExpressiveSurfaceTone.FLOATING -> EvolutionSurfaceRole.FLOATING
    AppExpressiveSurfaceTone.GLASS -> EvolutionSurfaceRole.SOFT
}

@Composable
private fun expressiveSurfaceElevation(tone: AppExpressiveSurfaceTone): Dp {
    return when (LocalAppAppearanceSettings.current.visualStyleMode) {
        AppVisualStyleMode.CLASSIC -> 0.dp
        AppVisualStyleMode.EXPRESSIVE -> when (tone) {
            AppExpressiveSurfaceTone.FLOATING -> 6.dp
            AppExpressiveSurfaceTone.ACCENT -> 2.dp
            else -> 1.dp
        }
        AppVisualStyleMode.EXPRESSIVE_GLASS -> when (tone) {
            AppExpressiveSurfaceTone.FLOATING,
            AppExpressiveSurfaceTone.GLASS -> 7.dp
            else -> 2.dp
        }
    }
}

private data class ClassicExpressiveSurfacePalette(
    val base: Color,
    val border: Color
)

@Composable
private fun classicExpressiveSurfacePalette(tone: AppExpressiveSurfaceTone): ClassicExpressiveSurfacePalette {
    val scheme = MaterialTheme.colorScheme
    val panel = appPanelColor()
    val bubble = appBubbleBackgroundColor(defaultAlpha = 0.22f)
    val base = when (tone) {
        AppExpressiveSurfaceTone.PANEL -> panel
        AppExpressiveSurfaceTone.SOFT -> bubble
        AppExpressiveSurfaceTone.ACCENT -> lerp(scheme.surface, scheme.primaryContainer, 0.28f)
        AppExpressiveSurfaceTone.FLOATING -> panel
        AppExpressiveSurfaceTone.GLASS -> scheme.surface
    }
    val border = when (tone) {
        AppExpressiveSurfaceTone.ACCENT -> scheme.primary.copy(alpha = 0.24f)
        AppExpressiveSurfaceTone.GLASS -> scheme.primary.copy(alpha = 0.20f)
        AppExpressiveSurfaceTone.FLOATING -> appPanelBorderColor().copy(alpha = 0.68f)
        AppExpressiveSurfaceTone.PANEL,
        AppExpressiveSurfaceTone.SOFT -> appPanelBorderColor()
    }
    return ClassicExpressiveSurfacePalette(base = base, border = border)
}
