package com.vigilante.shiftsalaryplanner.ui.theme

import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import android.net.Uri
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.vigilante.shiftsalaryplanner.R
import java.io.File

private val BaseTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)

val Typography = BaseTypography

private val ManropeExternalFamily = FontFamily(
    Font(R.font.manrope_variable, weight = FontWeight.Normal),
    Font(R.font.manrope_variable, weight = FontWeight.Medium),
    Font(R.font.manrope_variable, weight = FontWeight.SemiBold),
    Font(R.font.manrope_variable, weight = FontWeight.Bold)
)

fun appTypography(
    fontMode: AppFontMode,
    fontScale: Float,
    customFontFamily: FontFamily? = null
): Typography {
    val family = when (fontMode) {
        AppFontMode.SYSTEM -> FontFamily.Default
        AppFontMode.SANS -> FontFamily.SansSerif
        AppFontMode.SERIF -> FontFamily.Serif
        AppFontMode.MONO -> FontFamily.Monospace
        AppFontMode.EXTERNAL_MANROPE -> ManropeExternalFamily
        AppFontMode.EXTERNAL_CUSTOM -> customFontFamily ?: FontFamily.Default
    }
    val scale = fontScale.coerceIn(0.85f, 1.3f)

    fun TextStyle.adjust(): TextStyle {
        fun scaleUnit(unit: TextUnit): TextUnit {
            return if (unit == TextUnit.Unspecified) unit else unit * scale
        }

        return copy(
            fontFamily = family,
            fontSize = scaleUnit(fontSize),
            lineHeight = scaleUnit(lineHeight)
        )
    }

    return BaseTypography.copy(
        displayLarge = BaseTypography.displayLarge.adjust(),
        displayMedium = BaseTypography.displayMedium.adjust(),
        displaySmall = BaseTypography.displaySmall.adjust(),
        headlineLarge = BaseTypography.headlineLarge.adjust(),
        headlineMedium = BaseTypography.headlineMedium.adjust(),
        headlineSmall = BaseTypography.headlineSmall.adjust(),
        titleLarge = BaseTypography.titleLarge.adjust(),
        titleMedium = BaseTypography.titleMedium.adjust(),
        titleSmall = BaseTypography.titleSmall.adjust(),
        bodyLarge = BaseTypography.bodyLarge.adjust(),
        bodyMedium = BaseTypography.bodyMedium.adjust(),
        bodySmall = BaseTypography.bodySmall.adjust(),
        labelLarge = BaseTypography.labelLarge.adjust(),
        labelMedium = BaseTypography.labelMedium.adjust(),
        labelSmall = BaseTypography.labelSmall.adjust()
    )
}

fun loadCustomFontFamily(
    context: Context,
    uriString: String
): FontFamily? {
    if (uriString.isBlank()) return null

    return runCatching {
        val uri = Uri.parse(uriString)
        val cachedFile = File(
            context.cacheDir,
            "appearance_custom_font_${uriString.hashCode()}.ttf"
        )
        context.contentResolver.openInputStream(uri)?.use { input ->
            cachedFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        FontFamily(AndroidTypeface.createFromFile(cachedFile))
    }.getOrNull()
}
