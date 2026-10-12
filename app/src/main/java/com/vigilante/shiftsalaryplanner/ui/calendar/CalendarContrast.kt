package com.vigilante.shiftsalaryplanner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG 2.x relative-luminance contrast for opaque text and surface colors. */
internal fun calendarTextContrast(foreground: Color, background: Color): Float {
    val light = max(foreground.luminance(), background.luminance())
    val dark = min(foreground.luminance(), background.luminance())
    return (light + 0.05f) / (dark + 0.05f)
}

/** Keep the preferred accent if readable; otherwise choose a guaranteed AA alternative. */
internal fun calendarAccessibleBadgeText(preferred: Color, background: Color): Color =
    if (calendarTextContrast(preferred, background) >= 4.5f) preferred
    else if (background.luminance() > 0.179f) Color.Black else Color.White
