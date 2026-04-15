package com.vigilante.shiftsalaryplanner.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import java.time.LocalTime

private val MintDarkColorScheme = darkColorScheme(
    primary = BrandDarkPrimary,
    onPrimary = BrandDarkOnPrimary,
    primaryContainer = BrandDarkPrimaryContainer,
    onPrimaryContainer = BrandDarkOnPrimaryContainer,
    secondary = BrandDarkSecondary,
    onSecondary = BrandDarkOnSecondary,
    secondaryContainer = BrandDarkSecondaryContainer,
    onSecondaryContainer = BrandDarkOnSecondaryContainer,
    tertiary = BrandDarkTertiary,
    onTertiary = BrandDarkOnTertiary,
    tertiaryContainer = BrandDarkTertiaryContainer,
    onTertiaryContainer = BrandDarkOnTertiaryContainer,
    background = BrandDarkBackground,
    onBackground = BrandDarkOnBackground,
    surface = BrandDarkSurface,
    onSurface = BrandDarkOnSurface,
    surfaceVariant = BrandDarkSurfaceVariant,
    onSurfaceVariant = BrandDarkOnSurfaceVariant,
    outline = BrandDarkOutline
)

private val MintLightColorScheme = lightColorScheme(
    primary = BrandLightPrimary,
    onPrimary = BrandLightOnPrimary,
    primaryContainer = BrandLightPrimaryContainer,
    onPrimaryContainer = BrandLightOnPrimaryContainer,
    secondary = BrandLightSecondary,
    onSecondary = BrandLightOnSecondary,
    secondaryContainer = BrandLightSecondaryContainer,
    onSecondaryContainer = BrandLightOnSecondaryContainer,
    tertiary = BrandLightTertiary,
    onTertiary = BrandLightOnTertiary,
    tertiaryContainer = BrandLightTertiaryContainer,
    onTertiaryContainer = BrandLightOnTertiaryContainer,
    background = BrandLightBackground,
    onBackground = BrandLightOnBackground,
    surface = BrandLightSurface,
    onSurface = BrandLightOnSurface,
    surfaceVariant = BrandLightSurfaceVariant,
    onSurfaceVariant = BrandLightOnSurfaceVariant,
    outline = BrandLightOutline
)

private val OceanLightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF005C9A),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD0E4FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001C34),
    secondary = androidx.compose.ui.graphics.Color(0xFF485E7C),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD1E4FF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF041C35),
    tertiary = androidx.compose.ui.graphics.Color(0xFF62597C),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE8DEFF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF1E1635),
    background = androidx.compose.ui.graphics.Color(0xFFF8F9FF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF181C22),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF181C22),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDFE3EB),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF43474E),
    outline = androidx.compose.ui.graphics.Color(0xFF73777F)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFA0CAFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003258),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF00497C),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD0E4FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFB1C8EA),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1B314C),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF314764),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD1E4FF),
    tertiary = androidx.compose.ui.graphics.Color(0xFFCDC2EA),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF332C4B),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF4A4263),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE8DEFF),
    background = androidx.compose.ui.graphics.Color(0xFF11141A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE2E2EA),
    surface = androidx.compose.ui.graphics.Color(0xFF11141A),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE2E2EA),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF43474E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC3C7D0),
    outline = androidx.compose.ui.graphics.Color(0xFF8D9199)
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8B4D00),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDDBA),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF2C1600),
    secondary = androidx.compose.ui.graphics.Color(0xFF75594B),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDBCA),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF2B170C),
    tertiary = androidx.compose.ui.graphics.Color(0xFF5E6237),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE3E8B0),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF1A1D00),
    background = androidx.compose.ui.graphics.Color(0xFFFFF8F5),
    onBackground = androidx.compose.ui.graphics.Color(0xFF211A16),
    surface = androidx.compose.ui.graphics.Color(0xFFFFF8F5),
    onSurface = androidx.compose.ui.graphics.Color(0xFF211A16),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF2DED3),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF51443D),
    outline = androidx.compose.ui.graphics.Color(0xFF84736A)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFFB86E),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF4B2800),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF6B3A00),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDDBA),
    secondary = androidx.compose.ui.graphics.Color(0xFFE6BEAA),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF432A1F),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF5B4034),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDBCA),
    tertiary = androidx.compose.ui.graphics.Color(0xFFC7CC97),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF30340D),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF464A22),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE3E8B0),
    background = androidx.compose.ui.graphics.Color(0xFF18120E),
    onBackground = androidx.compose.ui.graphics.Color(0xFFEDDFD9),
    surface = androidx.compose.ui.graphics.Color(0xFF18120E),
    onSurface = androidx.compose.ui.graphics.Color(0xFFEDDFD9),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF51443D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD5C3B9),
    outline = androidx.compose.ui.graphics.Color(0xFF9F8D84)
)

private val GraphiteLightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF425565),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFC7D6EA),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001E31),
    secondary = androidx.compose.ui.graphics.Color(0xFF4F5B66),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD2DCE9),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0B1D2A),
    tertiary = androidx.compose.ui.graphics.Color(0xFF655A70),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFEBDDFA),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF20182B),
    background = androidx.compose.ui.graphics.Color(0xFFF7F8FA),
    onBackground = androidx.compose.ui.graphics.Color(0xFF191C20),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF191C20),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE1E3E8),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF44474D),
    outline = androidx.compose.ui.graphics.Color(0xFF74777D)
)

private val GraphiteDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFA9BED4),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF102F46),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF2B455D),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFC7D6EA),
    secondary = androidx.compose.ui.graphics.Color(0xFFB7C8D8),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF21313F),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF384956),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD2DCE9),
    tertiary = androidx.compose.ui.graphics.Color(0xFFCFC0DC),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF362B41),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF4D4158),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFEBDDFA),
    background = androidx.compose.ui.graphics.Color(0xFF111417),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE1E2E5),
    surface = androidx.compose.ui.graphics.Color(0xFF111417),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE1E2E5),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF44474D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC4C7CD),
    outline = androidx.compose.ui.graphics.Color(0xFF8E9197)
)

val LocalAppAppearanceSettings = staticCompositionLocalOf { AppearanceSettings() }

private fun parseColorOrFallback(hex: String, fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(sanitizeHexColor(hex, "#000000")))
    }.getOrElse { fallback }
}

private fun readableOn(color: Color): Color {
    return if (ColorUtils.calculateLuminance(color.toArgb()) > 0.55) {
        Color(0xFF101417)
    } else {
        Color(0xFFFFFFFF)
    }
}

private fun lift(color: Color, amount: Float): Color {
    return lerp(color, Color.White, amount.coerceIn(0f, 1f))
}

private fun shade(color: Color, amount: Float): Color {
    return lerp(color, Color.Black, amount.coerceIn(0f, 1f))
}

private fun customLightColorScheme(settings: AppearanceSettings): ColorScheme {
    val primary = parseColorOrFallback(settings.customPrimaryHex, BrandLightPrimary)
    val secondary = parseColorOrFallback(settings.customSecondaryHex, BrandLightSecondary)
    val tertiary = parseColorOrFallback(settings.customTertiaryHex, BrandLightTertiary)
    val background = parseColorOrFallback(settings.customBackgroundHex, BrandLightBackground)
    val surface = lift(background, 0.08f)
    val surfaceVariant = shade(background, 0.11f)
    val onSurface = readableOn(surface)

    return lightColorScheme(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = lift(primary, 0.72f),
        onPrimaryContainer = readableOn(lift(primary, 0.72f)),
        secondary = secondary,
        onSecondary = readableOn(secondary),
        secondaryContainer = lift(secondary, 0.74f),
        onSecondaryContainer = readableOn(lift(secondary, 0.74f)),
        tertiary = tertiary,
        onTertiary = readableOn(tertiary),
        tertiaryContainer = lift(tertiary, 0.72f),
        onTertiaryContainer = readableOn(lift(tertiary, 0.72f)),
        background = background,
        onBackground = readableOn(background),
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = readableOn(surfaceVariant),
        outline = lerp(onSurface, surface, 0.58f)
    )
}

private fun customDarkColorScheme(settings: AppearanceSettings): ColorScheme {
    val primarySource = parseColorOrFallback(settings.customPrimaryHex, BrandDarkPrimary)
    val secondarySource = parseColorOrFallback(settings.customSecondaryHex, BrandDarkSecondary)
    val tertiarySource = parseColorOrFallback(settings.customTertiaryHex, BrandDarkTertiary)
    val backgroundSource = parseColorOrFallback(settings.customBackgroundHex, BrandDarkBackground)
    val background = shade(backgroundSource, 0.84f)
    val surface = shade(backgroundSource, 0.8f)
    val surfaceVariant = lift(surface, 0.2f)
    val primary = lift(primarySource, 0.3f)
    val secondary = lift(secondarySource, 0.28f)
    val tertiary = lift(tertiarySource, 0.3f)
    val onSurface = readableOn(surface)

    return darkColorScheme(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = shade(primarySource, 0.45f),
        onPrimaryContainer = readableOn(shade(primarySource, 0.45f)),
        secondary = secondary,
        onSecondary = readableOn(secondary),
        secondaryContainer = shade(secondarySource, 0.44f),
        onSecondaryContainer = readableOn(shade(secondarySource, 0.44f)),
        tertiary = tertiary,
        onTertiary = readableOn(tertiary),
        tertiaryContainer = shade(tertiarySource, 0.45f),
        onTertiaryContainer = readableOn(shade(tertiarySource, 0.45f)),
        background = background,
        onBackground = readableOn(background),
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = readableOn(surfaceVariant),
        outline = lerp(onSurface, surface, 0.52f)
    )
}

@Composable
fun ShiftSalaryPlannerTheme(
    appearanceSettings: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val baseDensity = LocalDensity.current
    val systemDarkTheme = isSystemInDarkTheme()
    val resolvedDarkTheme = when (appearanceSettings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> systemDarkTheme
        ThemeMode.SCHEDULE -> {
            isDarkTimeNow(
                now = LocalTime.now(),
                start = appearanceSettings.scheduledDarkStartTime(),
                end = appearanceSettings.scheduledDarkEndTime()
            )
        }
    }
    val customFontFamily = remember(appearanceSettings.customFontUri) {
        loadCustomFontFamily(
            context = context,
            uriString = appearanceSettings.customFontUri
        )
    }
    val appliedFontScale = appearanceSettings.fontScale.coerceIn(0.85f, 1.3f)
    val scopedDensity = remember(
        baseDensity.density,
        baseDensity.fontScale,
        appliedFontScale
    ) {
        Density(
            density = baseDensity.density,
            fontScale = (baseDensity.fontScale * appliedFontScale).coerceIn(0.7f, 2.2f)
        )
    }

    val colorScheme = when {
        appearanceSettings.colorSchemeMode == AppColorSchemeMode.DYNAMIC &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (resolvedDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        appearanceSettings.colorSchemeMode == AppColorSchemeMode.OCEAN -> {
            if (resolvedDarkTheme) OceanDarkColorScheme else OceanLightColorScheme
        }

        appearanceSettings.colorSchemeMode == AppColorSchemeMode.SUNSET -> {
            if (resolvedDarkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
        }

        appearanceSettings.colorSchemeMode == AppColorSchemeMode.GRAPHITE -> {
            if (resolvedDarkTheme) GraphiteDarkColorScheme else GraphiteLightColorScheme
        }

        appearanceSettings.colorSchemeMode == AppColorSchemeMode.CUSTOM -> {
            if (resolvedDarkTheme) customDarkColorScheme(appearanceSettings)
            else customLightColorScheme(appearanceSettings)
        }

        else -> {
            if (resolvedDarkTheme) MintDarkColorScheme else MintLightColorScheme
        }
    }

    val activity = context.findActivity()

    if (!view.isInEditMode) {
        SideEffect {
            activity?.window?.let { window ->
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT

                val controller = WindowCompat.getInsetsController(window, view)
                val useLightSystemIcons = colorScheme.background.luminance() < 0.55f
                controller.isAppearanceLightStatusBars = !useLightSystemIcons
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    controller.isAppearanceLightNavigationBars = !useLightSystemIcons
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppAppearanceSettings provides appearanceSettings,
        LocalDensity provides scopedDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(
                fontMode = appearanceSettings.fontMode,
                fontScale = 1f,
                customFontFamily = customFontFamily
            ),
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
