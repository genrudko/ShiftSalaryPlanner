package com.vigilante.shiftsalaryplanner.ui.theme

import java.time.LocalTime

enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO,
    SCHEDULE
}

enum class AppColorSchemeMode {
    MINT,
    OCEAN,
    SUNSET,
    GRAPHITE,
    CUSTOM,
    DYNAMIC
}

enum class AppFontMode {
    SYSTEM,
    SANS,
    SERIF,
    MONO,
    EXTERNAL_MANROPE,
    EXTERNAL_CUSTOM
}

enum class UiDensityMode {
    COMFORTABLE,
    COMPACT
}

enum class UiContrastMode {
    STANDARD,
    HIGH
}

enum class AnimationSpeedMode {
    NORMAL,
    SLOW,
    OFF
}

enum class CornerStyleMode {
    SOFT,
    STANDARD,
    SHARP
}

enum class CurrencySymbolMode(val symbol: String) {
    RUB("₽"),
    USD("$"),
    EUR("€"),
    KZT("₸"),
    BYN("Br")
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val colorSchemeMode: AppColorSchemeMode = AppColorSchemeMode.MINT,
    val fontMode: AppFontMode = AppFontMode.SYSTEM,
    val currencySymbolMode: CurrencySymbolMode = CurrencySymbolMode.RUB,
    val fontScale: Float = 1.0f,
    val uiDensityMode: UiDensityMode = UiDensityMode.COMFORTABLE,
    val uiContrastMode: UiContrastMode = UiContrastMode.STANDARD,
    val animationSpeedMode: AnimationSpeedMode = AnimationSpeedMode.NORMAL,
    val cornerStyleMode: CornerStyleMode = CornerStyleMode.STANDARD,
    val customPrimaryHex: String = "#0D665A",
    val customSecondaryHex: String = "#3F6371",
    val customTertiaryHex: String = "#5A5C7E",
    val customBackgroundHex: String = "#F4F8F7",
    val customBubbleHex: String = "",
    val customFontUri: String = "",
    val customFontDisplayName: String = "",
    val scheduledDarkStartHour: Int = 22,
    val scheduledDarkStartMinute: Int = 0,
    val scheduledDarkEndHour: Int = 7,
    val scheduledDarkEndMinute: Int = 0
)

fun AppearanceSettings.scheduledDarkStartTime(): LocalTime {
    return LocalTime.of(
        scheduledDarkStartHour.coerceIn(0, 23),
        scheduledDarkStartMinute.coerceIn(0, 59)
    )
}

fun AppearanceSettings.scheduledDarkEndTime(): LocalTime {
    return LocalTime.of(
        scheduledDarkEndHour.coerceIn(0, 23),
        scheduledDarkEndMinute.coerceIn(0, 59)
    )
}

fun isDarkTimeNow(
    now: LocalTime,
    start: LocalTime,
    end: LocalTime
): Boolean {
    if (start == end) return false

    return if (start < end) {
        now >= start && now < end
    } else {
        now >= start || now < end
    }
}

fun sanitizeHexColor(input: String, fallback: String): String {
    val clean = input.trim().removePrefix("#").uppercase()
    val normalized = when (clean.length) {
        3 -> clean.map { "$it$it" }.joinToString(separator = "")
        6 -> clean
        8 -> clean.substring(2)
        else -> ""
    }

    if (normalized.length != 6 || normalized.any { it !in "0123456789ABCDEF" }) {
        return fallback
    }
    return "#$normalized"
}
