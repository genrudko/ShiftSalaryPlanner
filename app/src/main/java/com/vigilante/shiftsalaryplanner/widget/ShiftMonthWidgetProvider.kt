package com.vigilante.shiftsalaryplanner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.vigilante.shiftsalaryplanner.MainActivity
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private const val ACTION_WIDGET_DATA_CHANGED = "com.vigilante.shiftsalaryplanner.widget.ACTION_WIDGET_DATA_CHANGED"
private const val EXTRA_WIDGET_SIZE_MODE = "extra_widget_size_mode"
const val PREFS_WIDGET_SETTINGS = "widget_settings"
private const val KEY_WIDGET_THEME_MODE = "theme_mode"
private const val KEY_FULL_LABEL_PREFIX = "full_label_"
private const val KEY_SHORT_LABEL_PREFIX = "short_label_"
private const val KEY_META_LABEL_PREFIX = "meta_label_"
private const val KEY_USE_CUSTOM_COLOR_PREFIX = "use_custom_color_"
private const val KEY_COLOR_HEX_PREFIX = "color_hex_"
private const val KEY_LINK_TEMPLATE_PREFIX = "link_template_"

enum class WidgetThemeMode {
    AUTO,
    DARK,
    LIGHT
}

data class WidgetShiftOverride(
    val fullLabel: String = "",
    val shortLabel: String = "",
    val metaLabel: String = "",
    val useCustomColor: Boolean = false,
    val colorHex: String = "",
    val linkWithTemplate: Boolean = true
)

fun readWidgetThemeMode(prefs: SharedPreferences): WidgetThemeMode {
    val raw = prefs.getString(KEY_WIDGET_THEME_MODE, WidgetThemeMode.AUTO.name)
    return runCatching { WidgetThemeMode.valueOf(raw ?: WidgetThemeMode.AUTO.name) }
        .getOrElse { WidgetThemeMode.AUTO }
}

fun writeWidgetThemeMode(prefs: SharedPreferences, mode: WidgetThemeMode) {
    prefs.edit().putString(KEY_WIDGET_THEME_MODE, mode.name).apply()
}

fun readWidgetShiftOverride(prefs: SharedPreferences, shiftCode: String): WidgetShiftOverride {
    val fullLabel = prefs.getString(KEY_FULL_LABEL_PREFIX + shiftCode, "") ?: ""
    val shortLabel = prefs.getString(KEY_SHORT_LABEL_PREFIX + shiftCode, "") ?: ""
    val metaLabel = prefs.getString(KEY_META_LABEL_PREFIX + shiftCode, "") ?: ""
    val useCustomColor = prefs.getBoolean(KEY_USE_CUSTOM_COLOR_PREFIX + shiftCode, false)
    val colorHex = prefs.getString(KEY_COLOR_HEX_PREFIX + shiftCode, "") ?: ""
    val hasStoredLinkFlag = prefs.contains(KEY_LINK_TEMPLATE_PREFIX + shiftCode)
    val hasLegacyCustomizations = fullLabel.isNotBlank() || shortLabel.isNotBlank() || metaLabel.isNotBlank() || useCustomColor || colorHex.isNotBlank()
    val linkWithTemplate = if (hasStoredLinkFlag) {
        prefs.getBoolean(KEY_LINK_TEMPLATE_PREFIX + shiftCode, true)
    } else {
        !hasLegacyCustomizations
    }

    return WidgetShiftOverride(
        fullLabel = fullLabel,
        shortLabel = shortLabel,
        metaLabel = metaLabel,
        useCustomColor = useCustomColor,
        colorHex = colorHex,
        linkWithTemplate = linkWithTemplate
    )
}

fun writeWidgetShiftOverride(prefs: SharedPreferences, shiftCode: String, override: WidgetShiftOverride) {
    prefs.edit()
        .putString(KEY_FULL_LABEL_PREFIX + shiftCode, override.fullLabel)
        .putString(KEY_SHORT_LABEL_PREFIX + shiftCode, override.shortLabel)
        .putString(KEY_META_LABEL_PREFIX + shiftCode, override.metaLabel)
        .putBoolean(KEY_USE_CUSTOM_COLOR_PREFIX + shiftCode, override.useCustomColor)
        .putString(KEY_COLOR_HEX_PREFIX + shiftCode, override.colorHex)
        .putBoolean(KEY_LINK_TEMPLATE_PREFIX + shiftCode, override.linkWithTemplate)
        .apply()
}

fun clearWidgetShiftOverride(prefs: SharedPreferences, shiftCode: String) {
    prefs.edit()
        .remove(KEY_FULL_LABEL_PREFIX + shiftCode)
        .remove(KEY_SHORT_LABEL_PREFIX + shiftCode)
        .remove(KEY_META_LABEL_PREFIX + shiftCode)
        .remove(KEY_USE_CUSTOM_COLOR_PREFIX + shiftCode)
        .remove(KEY_COLOR_HEX_PREFIX + shiftCode)
        .remove(KEY_LINK_TEMPLATE_PREFIX + shiftCode)
        .apply()
}

fun effectiveWidgetThemeMode(context: Context, prefs: SharedPreferences): WidgetThemeMode {
    return when (readWidgetThemeMode(prefs)) {
        WidgetThemeMode.AUTO -> {
            val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (isNight) WidgetThemeMode.DARK else WidgetThemeMode.LIGHT
        }
        WidgetThemeMode.DARK -> WidgetThemeMode.DARK
        WidgetThemeMode.LIGHT -> WidgetThemeMode.LIGHT
    }
}

fun shouldUseWidgetLightTheme(context: Context, prefs: SharedPreferences): Boolean {
    return effectiveWidgetThemeMode(context, prefs) == WidgetThemeMode.LIGHT
}

private fun widgetIconGlyph(iconKey: String, fallbackCode: String): String {
    return when {
        iconKey.startsWith("EMOJI:") -> iconKey.removePrefix("EMOJI:").ifBlank { fallbackCode }
        iconKey == "SUN" -> "☀"
        iconKey == "MOON" -> "☾"
        iconKey == "EIGHT" -> "8"
        iconKey == "HOME" -> "⌂"
        iconKey == "OT" -> "ОТ"
        iconKey == "SICK" -> "✚"
        iconKey == "STAR" -> "★"
        iconKey == "TEXT" -> fallbackCode
        else -> fallbackCode
    }
}

fun defaultWidgetShortLabel(template: ShiftTemplateEntity): String {
    return widgetIconGlyph(template.iconKey, template.code.ifBlank { template.title.take(3).uppercase() })
}

fun defaultWidgetLongLabel(template: ShiftTemplateEntity): String {
    val title = when (template.code.uppercase()) {
        "ВЫХ" -> "Выходной"
        "ОТ" -> "Отпуск"
        "Б" -> "Больничный"
        else -> template.title.ifBlank { template.code }
    }
    val glyph = widgetIconGlyph(template.iconKey, template.code.ifBlank { title })
    return when {
        template.iconKey.startsWith("EMOJI:") -> glyph
        glyph.isBlank() || glyph == title -> title
        else -> "$glyph $title"
    }
}

fun defaultWidgetMetaLabel(template: ShiftTemplateEntity): String {
    return when (template.code.uppercase()) {
        "ВЫХ" -> "ВД"
        "ОТ" -> "Отдых"
        "Б" -> "Больничн."
        else -> {
            val paidHours = (template.totalHours - template.breakHours).coerceAtLeast(0.0)
            if (paidHours > 0.0) humanizeWidgetHours(paidHours) else ""
        }
    }
}

private fun humanizeWidgetHours(value: Double): String {
    val minutes = (value * 60.0).toLong().coerceAtLeast(0L)
    val hoursPart = Duration.ofMinutes(minutes).toHours()
    val minutesPart = minutes % 60L
    return "$hoursPart:${minutesPart.toString().padStart(2, '0')}"
}

enum class WidgetSizeMode {
    COMPACT,
    MEDIUM,
    LARGE
}

class ShiftMonthWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_WIDGET_DATA_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, ShiftMonthWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                ids.forEach { updateAppWidget(context, appWidgetManager, it) }
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val intent = Intent(context, ShiftMonthWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_DATA_CHANGED
            }
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val widgetPrefs = context.getSharedPreferences(PREFS_WIDGET_SETTINGS, Context.MODE_PRIVATE)
            val useLightTheme = shouldUseWidgetLightTheme(context, widgetPrefs)
            val views = RemoteViews(context.packageName, if (useLightTheme) R.layout.widget_shift_month_light else R.layout.widget_shift_month)
            val month = YearMonth.now()
            val today = LocalDate.now()
            val locale = Locale("ru")
            val mode = resolveWidgetSizeMode(appWidgetManager.getAppWidgetOptions(appWidgetId))

            val title = buildString {
                append(
                    month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                )
                append(' ')
                append(month.year)
            }

            val subtitle = when (mode) {
                WidgetSizeMode.COMPACT -> ""
                WidgetSizeMode.MEDIUM -> "Сегодня ${today.dayOfMonth}"
                WidgetSizeMode.LARGE -> "Текущий месяц • Сегодня ${today.dayOfMonth}"
            }

            views.setTextViewText(R.id.widgetMonthTitle, title)
            views.setTextViewText(R.id.widgetMonthSubtitle, subtitle)
            views.setViewVisibility(
                R.id.widgetMonthSubtitle,
                if (mode == WidgetSizeMode.COMPACT) View.GONE else View.VISIBLE
            )
            views.setViewVisibility(
                R.id.widgetOpenAppButton,
                if (mode == WidgetSizeMode.COMPACT) View.GONE else View.VISIBLE
            )

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetOpenAppButton, openAppPendingIntent)

            val serviceIntent = Intent(context, ShiftMonthWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_WIDGET_SIZE_MODE, mode.name)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            views.setRemoteAdapter(R.id.widgetCalendarGrid, serviceIntent)
            views.setEmptyView(R.id.widgetCalendarGrid, R.id.widgetEmptyView)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetCalendarGrid)
        }

        private fun resolveWidgetSizeMode(options: Bundle): WidgetSizeMode {
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val minSide = max(1, minOf(minWidthDp, minHeightDp))

            return when {
                minSide < 170 -> WidgetSizeMode.COMPACT
                minSide < 250 -> WidgetSizeMode.MEDIUM
                else -> WidgetSizeMode.LARGE
            }
        }
    }
}
