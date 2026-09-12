package com.vigilante.shiftsalaryplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Semantic color roles for the Variant A / Evolution visual language. */
data class EvolutionColorRoles(
    val appBackground: Color,
    val surfacePrimary: Color,
    val surfaceSoft: Color,
    val surfaceAccent: Color,
    val surfaceFloating: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val financePositive: Color,
    val warningAccent: Color,
    val dangerAccent: Color,
    val contentPrimary: Color,
    val contentSecondary: Color
)

@Composable
fun evolutionColorRoles(): EvolutionColorRoles {
    val scheme = MaterialTheme.colorScheme
    val settings = LocalAppAppearanceSettings.current
    val isDark = scheme.background.luminance() < 0.5f
    val highContrast = settings.uiContrastMode == UiContrastMode.HIGH

    // Mint is the historical palette. In the new Expressive language its global
    // selection/navigation accent moves to indigo so finance green can stay semantic.
    val brandTarget = when (settings.colorSchemeMode) {
        AppColorSchemeMode.MINT -> if (isDark) Color(0xFFA9B8FF) else Color(0xFF5267E8)
        AppColorSchemeMode.OCEAN -> scheme.primary
        AppColorSchemeMode.SUNSET -> scheme.primary
        AppColorSchemeMode.GRAPHITE -> if (isDark) lerp(scheme.primary, Color(0xFFA9B8FF), 0.42f)
            else lerp(scheme.primary, Color(0xFF5267E8), 0.42f)
        AppColorSchemeMode.CUSTOM,
        AppColorSchemeMode.DYNAMIC -> scheme.primary
    }
    val secondaryTarget = when (settings.colorSchemeMode) {
        AppColorSchemeMode.MINT -> if (isDark) Color(0xFFC7B8FF) else Color(0xFF8267D8)
        else -> scheme.tertiary
    }

    val appBackground = if (isDark) {
        lerp(scheme.background, Color(0xFF101525), 0.34f)
    } else {
        lerp(scheme.background, Color(0xFFF5F7FF), 0.66f)
    }
    val surfacePrimary = if (isDark) {
        lerp(scheme.surface, Color.White, if (highContrast) 0.075f else 0.045f)
    } else {
        lerp(scheme.surface, Color.White, 0.82f)
    }
    val surfaceSoft = if (isDark) {
        lerp(surfacePrimary, brandTarget, 0.075f)
    } else {
        lerp(surfacePrimary, Color(0xFFEFF2FF), 0.62f)
    }
    val surfaceAccent = if (isDark) {
        lerp(surfacePrimary, brandTarget, 0.15f)
    } else {
        lerp(surfacePrimary, Color(0xFFE6EBFF), 0.74f)
    }
    val surfaceFloating = if (isDark) lerp(surfacePrimary, Color.White, 0.035f) else Color.White

    return EvolutionColorRoles(
        appBackground = appBackground,
        surfacePrimary = surfacePrimary,
        surfaceSoft = surfaceSoft,
        surfaceAccent = surfaceAccent,
        surfaceFloating = surfaceFloating,
        brandPrimary = brandTarget,
        brandSecondary = secondaryTarget,
        financePositive = if (isDark) Color(0xFF69D8A0) else Color(0xFF168C62),
        warningAccent = if (isDark) Color(0xFFFFC56E) else Color(0xFFE58B18),
        dangerAccent = scheme.error,
        contentPrimary = if (highContrast) scheme.onBackground else scheme.onSurface,
        contentSecondary = if (highContrast) lerp(scheme.onSurfaceVariant, scheme.onSurface, 0.28f)
            else scheme.onSurfaceVariant
    )
}
