package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

@Composable
fun AppExpressiveSurface(
    modifier: Modifier = Modifier,
    tone: AppExpressiveSurfaceTone = AppExpressiveSurfaceTone.PANEL,
    shape: Shape = RoundedCornerShape(appCardRadius()),
    border: BorderStroke? = null,
    shadowElevation: Dp = expressiveSurfaceElevation(tone),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = expressiveSurfacePalette(tone)
    val mode = LocalAppAppearanceSettings.current.visualStyleMode
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = shape,
        color = palette.base,
        border = border ?: BorderStroke(1.dp, palette.border),
        shadowElevation = shadowElevation,
        tonalElevation = 0.dp
    ) {
        Box {
            if (mode != AppVisualStyleMode.CLASSIC) {
                LiquidGlassLayer(
                    tone = tone,
                    shape = shape,
                    primary = scheme.primary,
                    tertiary = scheme.tertiary,
                    onSurface = scheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun BoxScope.LiquidGlassLayer(
    tone: AppExpressiveSurfaceTone,
    shape: Shape,
    primary: Color,
    tertiary: Color,
    onSurface: Color
) {
    val shineAlpha = when (tone) {
        AppExpressiveSurfaceTone.ACCENT -> 0.30f
        AppExpressiveSurfaceTone.FLOATING -> 0.18f
        AppExpressiveSurfaceTone.GLASS -> 0.24f
        AppExpressiveSurfaceTone.PANEL -> 0.14f
        AppExpressiveSurfaceTone.SOFT -> 0.10f
    }
    val colorWashAlpha = when (tone) {
        AppExpressiveSurfaceTone.ACCENT -> 0.14f
        AppExpressiveSurfaceTone.FLOATING -> 0.08f
        AppExpressiveSurfaceTone.GLASS -> 0.10f
        AppExpressiveSurfaceTone.PANEL -> 0.06f
        AppExpressiveSurfaceTone.SOFT -> 0.05f
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = shineAlpha),
                        Color.Transparent,
                        primary.copy(alpha = colorWashAlpha),
                        tertiary.copy(alpha = colorWashAlpha * 0.72f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.62f),
                        primary.copy(alpha = 0.22f),
                        onSurface.copy(alpha = 0.12f)
                    )
                ),
                shape = shape
            )
    )
}

@Composable
private fun expressiveSurfaceElevation(tone: AppExpressiveSurfaceTone): Dp {
    val mode = LocalAppAppearanceSettings.current.visualStyleMode
    if (mode == AppVisualStyleMode.CLASSIC) return 0.dp
    return 0.dp
}

private data class ExpressiveSurfacePalette(
    val base: Color,
    val border: Color
)

@Composable
private fun expressiveSurfacePalette(tone: AppExpressiveSurfaceTone): ExpressiveSurfacePalette {
    val scheme = MaterialTheme.colorScheme
    val mode = LocalAppAppearanceSettings.current.visualStyleMode
    val panel = appPanelColor()
    val bubble = appBubbleBackgroundColor(defaultAlpha = 0.22f)
    val base = when (tone) {
        AppExpressiveSurfaceTone.PANEL -> panel
        AppExpressiveSurfaceTone.SOFT -> bubble
        AppExpressiveSurfaceTone.ACCENT -> if (mode == AppVisualStyleMode.CLASSIC) {
            lerp(scheme.surface, scheme.primaryContainer, 0.28f)
        } else {
            lerp(scheme.surface, scheme.primaryContainer, 0.48f)
        }
        AppExpressiveSurfaceTone.FLOATING -> panel
        AppExpressiveSurfaceTone.GLASS -> if (mode == AppVisualStyleMode.CLASSIC) {
            scheme.surface
        } else {
            lerp(scheme.surface, scheme.primaryContainer, 0.14f)
        }
    }
    val border = when (tone) {
        AppExpressiveSurfaceTone.ACCENT -> scheme.primary.copy(alpha = if (mode == AppVisualStyleMode.CLASSIC) 0.24f else 0.38f)
        AppExpressiveSurfaceTone.GLASS -> scheme.primary.copy(alpha = if (mode == AppVisualStyleMode.EXPRESSIVE_GLASS) 0.28f else 0.20f)
        AppExpressiveSurfaceTone.FLOATING -> appPanelBorderColor().copy(alpha = 0.68f)
        AppExpressiveSurfaceTone.PANEL,
        AppExpressiveSurfaceTone.SOFT -> appPanelBorderColor()
    }
    return ExpressiveSurfacePalette(
        base = base,
        border = border
    )
}
