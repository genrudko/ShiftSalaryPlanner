package com.vigilante.shiftsalaryplanner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
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

private val LightColorScheme = lightColorScheme(
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

@Composable
fun ShiftSalaryPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
