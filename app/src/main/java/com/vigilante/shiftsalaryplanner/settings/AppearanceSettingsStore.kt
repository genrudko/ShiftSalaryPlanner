package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.ui.theme.AnimationSpeedMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppColorSchemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppFontMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.CornerStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.CurrencySymbolMode
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.UiContrastMode
import com.vigilante.shiftsalaryplanner.ui.theme.UiDensityMode
import com.vigilante.shiftsalaryplanner.ui.theme.sanitizeHexColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppearanceSettingsStore(context: Context) {

    private val prefs = context.profileSharedPreferences(PREFS_NAME)

    private val _settingsFlow = MutableStateFlow(loadFromPrefs())
    val settingsFlow: Flow<AppearanceSettings> = _settingsFlow.asStateFlow()

    fun save(settings: AppearanceSettings) {
        prefs.edit {
            putString(KEY_THEME_MODE, settings.themeMode.name)
            putString(KEY_COLOR_SCHEME_MODE, settings.colorSchemeMode.name)
            putString(KEY_FONT_MODE, settings.fontMode.name)
            putString(KEY_CURRENCY_SYMBOL_MODE, settings.currencySymbolMode.name)
            putFloat(KEY_FONT_SCALE, settings.fontScale.coerceIn(0.85f, 1.3f))
            putString(KEY_UI_DENSITY_MODE, settings.uiDensityMode.name)
            putString(KEY_UI_CONTRAST_MODE, settings.uiContrastMode.name)
            putString(KEY_ANIMATION_SPEED_MODE, settings.animationSpeedMode.name)
            putString(KEY_CORNER_STYLE_MODE, settings.cornerStyleMode.name)
            putString(KEY_CUSTOM_PRIMARY_HEX, sanitizeHexColor(settings.customPrimaryHex, "#0D665A"))
            putString(KEY_CUSTOM_SECONDARY_HEX, sanitizeHexColor(settings.customSecondaryHex, "#3F6371"))
            putString(KEY_CUSTOM_TERTIARY_HEX, sanitizeHexColor(settings.customTertiaryHex, "#5A5C7E"))
            putString(KEY_CUSTOM_BACKGROUND_HEX, sanitizeHexColor(settings.customBackgroundHex, "#F4F8F7"))
            putString(
                KEY_CUSTOM_BUBBLE_HEX,
                settings.customBubbleHex.takeIf { it.isNotBlank() }?.let { sanitizeHexColor(it, "#E3E8EF") } ?: ""
            )
            putString(KEY_CUSTOM_FONT_URI, settings.customFontUri)
            putString(KEY_CUSTOM_FONT_DISPLAY_NAME, settings.customFontDisplayName)
            putInt(KEY_SCHEDULE_DARK_START_HOUR, settings.scheduledDarkStartHour.coerceIn(0, 23))
            putInt(KEY_SCHEDULE_DARK_START_MINUTE, settings.scheduledDarkStartMinute.coerceIn(0, 59))
            putInt(KEY_SCHEDULE_DARK_END_HOUR, settings.scheduledDarkEndHour.coerceIn(0, 23))
            putInt(KEY_SCHEDULE_DARK_END_MINUTE, settings.scheduledDarkEndMinute.coerceIn(0, 59))
        }
        _settingsFlow.value = loadFromPrefs()
    }

    private fun loadFromPrefs(): AppearanceSettings {
        val themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name)
        }.getOrElse { ThemeMode.AUTO }

        val colorSchemeMode = runCatching {
            AppColorSchemeMode.valueOf(
                prefs.getString(KEY_COLOR_SCHEME_MODE, AppColorSchemeMode.MINT.name)
                    ?: AppColorSchemeMode.MINT.name
            )
        }.getOrElse { AppColorSchemeMode.MINT }

        val fontMode = runCatching {
            AppFontMode.valueOf(prefs.getString(KEY_FONT_MODE, AppFontMode.SYSTEM.name) ?: AppFontMode.SYSTEM.name)
        }.getOrElse { AppFontMode.SYSTEM }

        val currencySymbolMode = runCatching {
            CurrencySymbolMode.valueOf(
                prefs.getString(KEY_CURRENCY_SYMBOL_MODE, CurrencySymbolMode.RUB.name)
                    ?: CurrencySymbolMode.RUB.name
            )
        }.getOrElse { CurrencySymbolMode.RUB }

        val uiDensityMode = runCatching {
            UiDensityMode.valueOf(
                prefs.getString(KEY_UI_DENSITY_MODE, UiDensityMode.COMFORTABLE.name)
                    ?: UiDensityMode.COMFORTABLE.name
            )
        }.getOrElse { UiDensityMode.COMFORTABLE }

        val uiContrastMode = runCatching {
            UiContrastMode.valueOf(
                prefs.getString(KEY_UI_CONTRAST_MODE, UiContrastMode.STANDARD.name)
                    ?: UiContrastMode.STANDARD.name
            )
        }.getOrElse { UiContrastMode.STANDARD }

        val animationSpeedMode = runCatching {
            AnimationSpeedMode.valueOf(
                prefs.getString(KEY_ANIMATION_SPEED_MODE, AnimationSpeedMode.NORMAL.name)
                    ?: AnimationSpeedMode.NORMAL.name
            )
        }.getOrElse { AnimationSpeedMode.NORMAL }

        val cornerStyleMode = runCatching {
            CornerStyleMode.valueOf(
                prefs.getString(KEY_CORNER_STYLE_MODE, CornerStyleMode.STANDARD.name)
                    ?: CornerStyleMode.STANDARD.name
            )
        }.getOrElse { CornerStyleMode.STANDARD }

        return AppearanceSettings(
            themeMode = themeMode,
            colorSchemeMode = colorSchemeMode,
            fontMode = fontMode,
            currencySymbolMode = currencySymbolMode,
            fontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.85f, 1.3f),
            uiDensityMode = uiDensityMode,
            uiContrastMode = uiContrastMode,
            animationSpeedMode = animationSpeedMode,
            cornerStyleMode = cornerStyleMode,
            customPrimaryHex = sanitizeHexColor(
                prefs.getString(KEY_CUSTOM_PRIMARY_HEX, "#0D665A") ?: "#0D665A",
                "#0D665A"
            ),
            customSecondaryHex = sanitizeHexColor(
                prefs.getString(KEY_CUSTOM_SECONDARY_HEX, "#3F6371") ?: "#3F6371",
                "#3F6371"
            ),
            customTertiaryHex = sanitizeHexColor(
                prefs.getString(KEY_CUSTOM_TERTIARY_HEX, "#5A5C7E") ?: "#5A5C7E",
                "#5A5C7E"
            ),
            customBackgroundHex = sanitizeHexColor(
                prefs.getString(KEY_CUSTOM_BACKGROUND_HEX, "#F4F8F7") ?: "#F4F8F7",
                "#F4F8F7"
            ),
            customBubbleHex = prefs.getString(KEY_CUSTOM_BUBBLE_HEX, "")?.trim().orEmpty().let { raw ->
                if (raw.isBlank()) "" else sanitizeHexColor(raw, "#E3E8EF")
            },
            customFontUri = prefs.getString(KEY_CUSTOM_FONT_URI, "") ?: "",
            customFontDisplayName = prefs.getString(KEY_CUSTOM_FONT_DISPLAY_NAME, "") ?: "",
            scheduledDarkStartHour = prefs.getInt(KEY_SCHEDULE_DARK_START_HOUR, 22).coerceIn(0, 23),
            scheduledDarkStartMinute = prefs.getInt(KEY_SCHEDULE_DARK_START_MINUTE, 0).coerceIn(0, 59),
            scheduledDarkEndHour = prefs.getInt(KEY_SCHEDULE_DARK_END_HOUR, 7).coerceIn(0, 23),
            scheduledDarkEndMinute = prefs.getInt(KEY_SCHEDULE_DARK_END_MINUTE, 0).coerceIn(0, 59)
        )
    }

    companion object {
        private const val PREFS_NAME = "appearance_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_COLOR_SCHEME_MODE = "color_scheme_mode"
        private const val KEY_FONT_MODE = "font_mode"
        private const val KEY_CURRENCY_SYMBOL_MODE = "currency_symbol_mode"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_UI_DENSITY_MODE = "ui_density_mode"
        private const val KEY_UI_CONTRAST_MODE = "ui_contrast_mode"
        private const val KEY_ANIMATION_SPEED_MODE = "animation_speed_mode"
        private const val KEY_CORNER_STYLE_MODE = "corner_style_mode"
        private const val KEY_CUSTOM_PRIMARY_HEX = "custom_primary_hex"
        private const val KEY_CUSTOM_SECONDARY_HEX = "custom_secondary_hex"
        private const val KEY_CUSTOM_TERTIARY_HEX = "custom_tertiary_hex"
        private const val KEY_CUSTOM_BACKGROUND_HEX = "custom_background_hex"
        private const val KEY_CUSTOM_BUBBLE_HEX = "custom_bubble_hex"
        private const val KEY_CUSTOM_FONT_URI = "custom_font_uri"
        private const val KEY_CUSTOM_FONT_DISPLAY_NAME = "custom_font_display_name"
        private const val KEY_SCHEDULE_DARK_START_HOUR = "schedule_dark_start_hour"
        private const val KEY_SCHEDULE_DARK_START_MINUTE = "schedule_dark_start_minute"
        private const val KEY_SCHEDULE_DARK_END_HOUR = "schedule_dark_end_hour"
        private const val KEY_SCHEDULE_DARK_END_MINUTE = "schedule_dark_end_minute"
    }
}
