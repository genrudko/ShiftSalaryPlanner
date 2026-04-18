package com.vigilante.shiftsalaryplanner.widget

import android.annotation.SuppressLint
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
import androidx.core.content.edit
import androidx.core.net.toUri
import com.vigilante.shiftsalaryplanner.BottomTab
import com.vigilante.shiftsalaryplanner.MainActivity
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.formatMoney
import com.vigilante.shiftsalaryplanner.paidHours
import com.vigilante.shiftsalaryplanner.payroll.PayrollCalculator
import com.vigilante.shiftsalaryplanner.settings.AdditionalPaymentsStore
import com.vigilante.shiftsalaryplanner.settings.AppearanceSettingsStore
import com.vigilante.shiftsalaryplanner.settings.DeductionsStore
import com.vigilante.shiftsalaryplanner.settings.PayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import com.vigilante.shiftsalaryplanner.setCurrencySymbol
import com.vigilante.shiftsalaryplanner.toWorkShiftItemForDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val ACTION_WIDGET_DATA_CHANGED = "com.vigilante.shiftsalaryplanner.widget.ACTION_WIDGET_DATA_CHANGED"
private const val ACTION_WIDGET_ADD_TODAY_SHIFT = "com.vigilante.shiftsalaryplanner.widget.ACTION_WIDGET_ADD_TODAY_SHIFT"
private const val ACTION_WIDGET_CLEAR_TODAY_SHIFT = "com.vigilante.shiftsalaryplanner.widget.ACTION_WIDGET_CLEAR_TODAY_SHIFT"
private const val EXTRA_WIDGET_SIZE_MODE = "extra_widget_size_mode"
private const val EXTRA_WIDGET_QUICK_SHIFT_CODE = "extra_widget_quick_shift_code"
const val EXTRA_OPEN_TAB = "extra_open_tab"

const val PREFS_WIDGET_SETTINGS = "widget_settings"
private const val KEY_WIDGET_THEME_MODE = "theme_mode"
private const val KEY_WIDGET_QUICK_TEMPLATE_INDEX = "widget_quick_template_index"
private const val KEY_FULL_LABEL_PREFIX = "full_label_"
private const val KEY_SHORT_LABEL_PREFIX = "short_label_"
private const val KEY_META_LABEL_PREFIX = "meta_label_"
private const val KEY_USE_CUSTOM_COLOR_PREFIX = "use_custom_color_"
private const val KEY_COLOR_HEX_PREFIX = "color_hex_"
private const val KEY_LINK_TEMPLATE_PREFIX = "link_template_"
private const val KEY_WIDGET_SHOW_MONTH_SUBTITLE = "show_month_subtitle"
private const val KEY_WIDGET_SHOW_OPEN_APP_BUTTON = "show_open_app_button"
private const val KEY_WIDGET_SHOW_TODAY_LINE = "show_today_line"
private const val KEY_WIDGET_SHOW_PAYROLL_LINE = "show_payroll_line"
private const val KEY_WIDGET_SHOW_CLOUD_LINE = "show_cloud_line"
private const val KEY_WIDGET_SHOW_DATA_LINE = "show_data_line"
private const val KEY_WIDGET_SHOW_WEEKDAY_HEADER = "show_weekday_header"
private const val KEY_WIDGET_SHOW_CALENDAR_GRID = "show_calendar_grid"
private const val KEY_WIDGET_SHOW_QUICK_ADD_ACTION = "show_quick_add_action"
private const val KEY_WIDGET_SHOW_RESET_ACTION = "show_reset_action"
private const val KEY_WIDGET_SHOW_PAYROLL_ACTION = "show_payroll_action"

private const val PREFS_GOOGLE_SYNC_META = "google_drive_sync_meta"
private const val KEY_META_ACCOUNT_EMAIL = "account_email"
private const val KEY_META_LAST_UPLOAD_AT = "last_upload_at"
private const val KEY_META_AUTO_UPLOAD_ENABLED = "auto_upload_enabled"
private const val KEY_META_AUTO_UPLOAD_INTERVAL_HOURS = "auto_upload_interval_hours"

private val widgetDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
private val widgetDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

enum class WidgetThemeMode {
    AUTO,
    DARK,
    LIGHT,
    AMOLED
}

data class WidgetShiftOverride(
    val fullLabel: String = "",
    val shortLabel: String = "",
    val metaLabel: String = "",
    val useCustomColor: Boolean = false,
    val colorHex: String = "",
    val linkWithTemplate: Boolean = true
)

data class WidgetDisplaySettings(
    val showMonthSubtitle: Boolean = true,
    val showOpenAppButton: Boolean = true,
    val showTodayLine: Boolean = true,
    val showPayrollLine: Boolean = true,
    val showCloudLine: Boolean = true,
    val showDataLine: Boolean = true,
    val showWeekdayHeader: Boolean = true,
    val showCalendarGrid: Boolean = true,
    val showQuickAddAction: Boolean = true,
    val showResetAction: Boolean = true,
    val showPayrollAction: Boolean = true
)

private data class WidgetHeaderSnapshot(
    val nextShiftLine: String,
    val todayLine: String,
    val payrollLine: String,
    val cloudLine: String,
    val dataLine: String,
    val quickAddShiftCode: String?,
    val quickAddButtonLabel: String
)

fun readWidgetThemeMode(prefs: SharedPreferences): WidgetThemeMode {
    val raw = prefs.getString(KEY_WIDGET_THEME_MODE, WidgetThemeMode.AUTO.name)
    return runCatching { WidgetThemeMode.valueOf(raw ?: WidgetThemeMode.AUTO.name) }
        .getOrElse { WidgetThemeMode.AUTO }
}

fun readWidgetDisplaySettings(prefs: SharedPreferences): WidgetDisplaySettings {
    return WidgetDisplaySettings(
        showMonthSubtitle = prefs.getBoolean(KEY_WIDGET_SHOW_MONTH_SUBTITLE, true),
        showOpenAppButton = prefs.getBoolean(KEY_WIDGET_SHOW_OPEN_APP_BUTTON, true),
        showTodayLine = prefs.getBoolean(KEY_WIDGET_SHOW_TODAY_LINE, true),
        showPayrollLine = prefs.getBoolean(KEY_WIDGET_SHOW_PAYROLL_LINE, true),
        showCloudLine = prefs.getBoolean(KEY_WIDGET_SHOW_CLOUD_LINE, true),
        showDataLine = prefs.getBoolean(KEY_WIDGET_SHOW_DATA_LINE, true),
        showWeekdayHeader = prefs.getBoolean(KEY_WIDGET_SHOW_WEEKDAY_HEADER, true),
        showCalendarGrid = prefs.getBoolean(KEY_WIDGET_SHOW_CALENDAR_GRID, true),
        showQuickAddAction = prefs.getBoolean(KEY_WIDGET_SHOW_QUICK_ADD_ACTION, true),
        showResetAction = prefs.getBoolean(KEY_WIDGET_SHOW_RESET_ACTION, true),
        showPayrollAction = prefs.getBoolean(KEY_WIDGET_SHOW_PAYROLL_ACTION, true)
    )
}

@SuppressLint("UseKtx")
fun writeWidgetThemeMode(prefs: SharedPreferences, mode: WidgetThemeMode) {
    prefs.edit { putString(KEY_WIDGET_THEME_MODE, mode.name) }
}

@SuppressLint("UseKtx")
fun writeWidgetDisplaySettings(prefs: SharedPreferences, settings: WidgetDisplaySettings) {
    prefs.edit {
        putBoolean(KEY_WIDGET_SHOW_MONTH_SUBTITLE, settings.showMonthSubtitle)
            .putBoolean(KEY_WIDGET_SHOW_OPEN_APP_BUTTON, settings.showOpenAppButton)
            .putBoolean(KEY_WIDGET_SHOW_TODAY_LINE, settings.showTodayLine)
            .putBoolean(KEY_WIDGET_SHOW_PAYROLL_LINE, settings.showPayrollLine)
            .putBoolean(KEY_WIDGET_SHOW_CLOUD_LINE, settings.showCloudLine)
            .putBoolean(KEY_WIDGET_SHOW_DATA_LINE, settings.showDataLine)
            .putBoolean(KEY_WIDGET_SHOW_WEEKDAY_HEADER, settings.showWeekdayHeader)
            .putBoolean(KEY_WIDGET_SHOW_CALENDAR_GRID, settings.showCalendarGrid)
            .putBoolean(KEY_WIDGET_SHOW_QUICK_ADD_ACTION, settings.showQuickAddAction)
            .putBoolean(KEY_WIDGET_SHOW_RESET_ACTION, settings.showResetAction)
            .putBoolean(KEY_WIDGET_SHOW_PAYROLL_ACTION, settings.showPayrollAction)
    }
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
    prefs.edit {
        putString(KEY_FULL_LABEL_PREFIX + shiftCode, override.fullLabel)
            .putString(KEY_SHORT_LABEL_PREFIX + shiftCode, override.shortLabel)
            .putString(KEY_META_LABEL_PREFIX + shiftCode, override.metaLabel)
            .putBoolean(KEY_USE_CUSTOM_COLOR_PREFIX + shiftCode, override.useCustomColor)
            .putString(KEY_COLOR_HEX_PREFIX + shiftCode, override.colorHex)
            .putBoolean(KEY_LINK_TEMPLATE_PREFIX + shiftCode, override.linkWithTemplate)
    }
}

fun clearWidgetShiftOverride(prefs: SharedPreferences, shiftCode: String) {
    prefs.edit {
        remove(KEY_FULL_LABEL_PREFIX + shiftCode)
            .remove(KEY_SHORT_LABEL_PREFIX + shiftCode)
            .remove(KEY_META_LABEL_PREFIX + shiftCode)
            .remove(KEY_USE_CUSTOM_COLOR_PREFIX + shiftCode)
            .remove(KEY_COLOR_HEX_PREFIX + shiftCode)
            .remove(KEY_LINK_TEMPLATE_PREFIX + shiftCode)
    }
}

fun effectiveWidgetThemeMode(context: Context, prefs: SharedPreferences): WidgetThemeMode {
    return when (readWidgetThemeMode(prefs)) {
        WidgetThemeMode.AUTO -> {
            val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (isNight) WidgetThemeMode.DARK else WidgetThemeMode.LIGHT
        }
        WidgetThemeMode.DARK -> WidgetThemeMode.DARK
        WidgetThemeMode.LIGHT -> WidgetThemeMode.LIGHT
        WidgetThemeMode.AMOLED -> WidgetThemeMode.AMOLED
    }
}

fun shouldUseWidgetLightTheme(context: Context, prefs: SharedPreferences): Boolean {
    return effectiveWidgetThemeMode(context, prefs) == WidgetThemeMode.LIGHT
}

private fun isWidgetAmoled(context: Context, prefs: SharedPreferences): Boolean {
    return effectiveWidgetThemeMode(context, prefs) == WidgetThemeMode.AMOLED
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

@Suppress("DEPRECATION")
class ShiftMonthWidgetProviderV2 : AppWidgetProvider() {

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
            ACTION_WIDGET_ADD_TODAY_SHIFT -> {
                handleQuickAddTodayShift(context, intent.getStringExtra(EXTRA_WIDGET_QUICK_SHIFT_CODE))
                refreshAllWidgets(context)
            }
            ACTION_WIDGET_CLEAR_TODAY_SHIFT -> {
                handleQuickClearTodayShift(context)
                refreshAllWidgets(context)
            }
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_WIDGET_DATA_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                refreshAllWidgets(context)
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val intent = Intent(context, ShiftMonthWidgetProviderV2::class.java).apply {
                action = ACTION_WIDGET_DATA_CHANGED
            }
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val widgetPrefs = context.profileSharedPreferences(PREFS_WIDGET_SETTINGS)
            val useLightTheme = shouldUseWidgetLightTheme(context, widgetPrefs)
            val amoled = isWidgetAmoled(context, widgetPrefs)
            val displaySettings = readWidgetDisplaySettings(widgetPrefs)
            val views = RemoteViews(
                context.packageName,
                if (useLightTheme) R.layout.widget_shift_month_light else R.layout.widget_shift_month
            )
            val month = YearMonth.now()
            val today = LocalDate.now()
            val locale = Locale("ru")
            val mode = resolveWidgetSizeMode(appWidgetManager.getAppWidgetOptions(appWidgetId))
            val snapshot = loadWidgetHeaderSnapshot(context, widgetPrefs)

            val title = buildString {
                append(
                    month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                )
                append(' ')
                append(month.year)
            }

            val preset = when (mode) {
                WidgetSizeMode.COMPACT -> "5x2"
                WidgetSizeMode.MEDIUM -> "5x3"
                WidgetSizeMode.LARGE -> "5x4"
            }

            val subtitle = when (mode) {
                WidgetSizeMode.COMPACT -> ""
                WidgetSizeMode.MEDIUM -> "Сегодня ${today.dayOfMonth} • $preset"
                WidgetSizeMode.LARGE -> "Текущий месяц • Сегодня ${today.dayOfMonth} • $preset"
            }

            views.setTextViewText(R.id.widgetMonthTitle, title)
            views.setTextViewText(R.id.widgetMonthSubtitle, subtitle)
            views.setTextViewText(R.id.widgetNextShiftLine, snapshot.nextShiftLine)
            views.setTextViewText(R.id.widgetTodayLine, snapshot.todayLine)
            views.setTextViewText(R.id.widgetPayrollLine, snapshot.payrollLine)
            views.setTextViewText(R.id.widgetCloudLine, snapshot.cloudLine)
            views.setTextViewText(R.id.widgetDataStateLine, snapshot.dataLine)

            views.setTextViewText(R.id.widgetActionAdd, snapshot.quickAddButtonLabel)
            views.setTextViewText(R.id.widgetActionReset, "Сброс")
            views.setTextViewText(R.id.widgetActionPayroll, "Расчёт")

            val showSubtitle = mode != WidgetSizeMode.COMPACT && displaySettings.showMonthSubtitle
            val showOpenAppButton = mode != WidgetSizeMode.COMPACT && displaySettings.showOpenAppButton
            val showTodayLine = mode != WidgetSizeMode.COMPACT && displaySettings.showTodayLine
            val showPayrollLine = mode != WidgetSizeMode.COMPACT && displaySettings.showPayrollLine
            val showCloudLine = mode == WidgetSizeMode.LARGE && displaySettings.showCloudLine
            val showDataStateLine = mode == WidgetSizeMode.LARGE && displaySettings.showDataLine
            val showWeekdayHeader = mode != WidgetSizeMode.COMPACT && displaySettings.showWeekdayHeader
            val showCalendarGrid = mode != WidgetSizeMode.COMPACT && displaySettings.showCalendarGrid
            val showQuickAddAction = displaySettings.showQuickAddAction
            val showResetAction = mode != WidgetSizeMode.COMPACT && displaySettings.showResetAction
            val showPayrollAction = displaySettings.showPayrollAction

            views.setViewVisibility(
                R.id.widgetMonthSubtitle,
                if (showSubtitle) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widgetOpenAppButton,
                if (showOpenAppButton) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(R.id.widgetTodayLine, if (showTodayLine) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetPayrollLine, if (showPayrollLine) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetCloudLine, if (showCloudLine) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetDataStateLine, if (showDataStateLine) View.VISIBLE else View.GONE)

            views.setViewVisibility(R.id.widgetWeekdaysRow, if (showWeekdayHeader) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetCalendarGrid, if (showCalendarGrid) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetEmptyView, if (showCalendarGrid) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetActionAdd, if (showQuickAddAction) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetActionReset, if (showResetAction) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetActionPayroll, if (showPayrollAction) View.VISIBLE else View.GONE)

            if (amoled && !useLightTheme) {
                views.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.widget_month_bg_amoled)
            }

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetOpenAppButton, openAppPendingIntent)

            val openPayrollIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_TAB, BottomTab.PAYROLL.name)
            }
            val openPayrollPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                openPayrollIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetActionPayroll, openPayrollPendingIntent)

            val addShiftIntent = Intent(context, ShiftMonthWidgetProviderV2::class.java).apply {
                action = ACTION_WIDGET_ADD_TODAY_SHIFT
                putExtra(EXTRA_WIDGET_QUICK_SHIFT_CODE, snapshot.quickAddShiftCode)
            }
            val addShiftPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 2,
                addShiftIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetActionAdd, addShiftPendingIntent)

            val clearTodayIntent = Intent(context, ShiftMonthWidgetProviderV2::class.java).apply {
                action = ACTION_WIDGET_CLEAR_TODAY_SHIFT
            }
            val clearTodayPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 3,
                clearTodayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetActionReset, clearTodayPendingIntent)

            val serviceIntent = Intent(context, ShiftMonthWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_WIDGET_SIZE_MODE, mode.name)
                data = toUri(Intent.URI_INTENT_SCHEME).toUri()
            }

            views.setRemoteAdapter(R.id.widgetCalendarGrid, serviceIntent)
            views.setEmptyView(R.id.widgetCalendarGrid, R.id.widgetEmptyView)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetCalendarGrid)
        }

        private fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ShiftMonthWidgetProviderV2::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            ids.forEach { updateAppWidget(context, appWidgetManager, it) }
        }

        private fun resolveWidgetSizeMode(options: Bundle): WidgetSizeMode {
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            return when {
                minHeightDp < 140 -> WidgetSizeMode.COMPACT
                minWidthDp < 240 -> WidgetSizeMode.MEDIUM
                else -> WidgetSizeMode.LARGE
            }
        }

        private fun loadWidgetHeaderSnapshot(
            context: Context,
            widgetPrefs: SharedPreferences
        ): WidgetHeaderSnapshot = runBlocking {
            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            val db = AppDatabase.getDatabase(context)

            val shiftDays = db.shiftDayDao().observeAll().first()
            val templates = db.shiftTemplateDao().observeAll().first().sortedBy { it.sortOrder }
            val templateMap = templates.associateBy { it.code }
            val activeTemplates = templates.filter { it.active }
            val quickTemplates = if (activeTemplates.isNotEmpty()) activeTemplates else templates
            val quickTemplate = resolveQuickTemplate(widgetPrefs, quickTemplates)
            val quickCode = quickTemplate?.code
            val quickLabel = quickTemplate?.let { "+ ${defaultWidgetShortLabel(it)}" } ?: "+ Сегодня"

            val holidayMap = db.holidayDao().observeByScope("RU-FED").first()
                .associateBy { LocalDate.parse(it.date) }
            val alarmConfigs = ShiftAlarmStore(context).settingsFlow.first().templateConfigs.associateBy { it.shiftCode }
            val payrollSettings = PayrollSettingsStore(context).settingsFlow.first()
            val additionalPayments = AdditionalPaymentsStore(context).paymentsFlow.first()
            val deductions = DeductionsStore(context).deductionsFlow.first()

            runCatching {
                val appearance = AppearanceSettingsStore(context).settingsFlow.first()
                setCurrencySymbol(appearance.currencySymbolMode.symbol)
            }

            val shiftByDate = shiftDays.mapNotNull {
                runCatching { LocalDate.parse(it.date) to it.shiftCode }.getOrNull()
            }.toMap()

            val nextShiftLine = buildNextShiftLine(
                now = now,
                shiftByDate = shiftByDate,
                templatesByCode = templateMap,
                alarmConfigs = alarmConfigs
            )

            val todayTemplate = shiftByDate[today]?.let { templateMap[it] }
            val todayLine = if (todayTemplate != null) {
                val item = todayTemplate.toWorkShiftItemForDate(
                    date = today,
                    holidayMap = holidayMap,
                    applyShortDayReduction = payrollSettings.applyShortDayReduction,
                    shiftTiming = alarmConfigs[todayTemplate.code]
                )
                val nightInfo = if (item.nightHours > 0.0) " • Ночь ${humanizeWidgetHours(item.nightHours)}" else ""
                "Сегодня: ${defaultWidgetLongLabel(todayTemplate)} • ${humanizeWidgetHours(item.paidHours)}$nightInfo"
            } else {
                "Сегодня: смена не выбрана"
            }

            val month = YearMonth.now()
            val monthShifts = shiftByDate
                .filterKeys { YearMonth.from(it) == month }
                .mapNotNull { (date, code) ->
                    templateMap[code]?.toWorkShiftItemForDate(
                        date = date,
                        holidayMap = holidayMap,
                        applyShortDayReduction = payrollSettings.applyShortDayReduction,
                        shiftTiming = alarmConfigs[code]
                    )
                }
            val firstHalfShifts = monthShifts.filter { (it.date?.dayOfMonth ?: 31) <= 15 }
            val payroll = PayrollCalculator.calculate(
                shifts = monthShifts,
                firstHalfShifts = firstHalfShifts,
                settings = payrollSettings,
                additionalPayments = additionalPayments,
                deductions = deductions
            )

            val payrollLine = "К выплате: ${formatMoney(payroll.netTotal)}"
            val cloudLine = buildCloudStatusLine(context, now)
            val dataLine = buildDataStateLine(shiftByDate)

            WidgetHeaderSnapshot(
                nextShiftLine = nextShiftLine,
                todayLine = todayLine,
                payrollLine = payrollLine,
                cloudLine = cloudLine,
                dataLine = dataLine,
                quickAddShiftCode = quickCode,
                quickAddButtonLabel = quickLabel
            )
        }

        private fun buildNextShiftLine(
            now: LocalDateTime,
            shiftByDate: Map<LocalDate, String>,
            templatesByCode: Map<String, ShiftTemplateEntity>,
            alarmConfigs: Map<String, com.vigilante.shiftsalaryplanner.ShiftTemplateAlarmConfig>
        ): String {
            val today = now.toLocalDate()
            for (offset in 0..90L) {
                val date = today.plusDays(offset)
                val code = shiftByDate[date] ?: continue
                val template = templatesByCode[code] ?: continue
                val start = LocalDateTime.of(date, resolveShiftStartTime(template, alarmConfigs[code]))
                val end = start.plusMinutes((template.paidHours() * 60.0).toLong().coerceAtLeast(0L))

                if (start.isBefore(now) && now.isBefore(end)) {
                    return "Сейчас: ${defaultWidgetLongLabel(template)} • до ${end.toLocalTime().toString().take(5)}"
                }
                if (!start.isAfter(now)) continue

                val eta = Duration.between(now, start)
                val etaLabel = formatDurationCompact(eta)
                return "Следующая: ${defaultWidgetLongLabel(template)} • через $etaLabel"
            }
            return "Следующая: не запланирована"
        }

        private fun buildCloudStatusLine(
            context: Context,
            now: LocalDateTime
        ): String {
            val prefs = context.profileSharedPreferences(PREFS_GOOGLE_SYNC_META)
            val account = prefs.getString(KEY_META_ACCOUNT_EMAIL, "").orEmpty()
            val autoEnabled = prefs.getBoolean(KEY_META_AUTO_UPLOAD_ENABLED, false)
            val intervalHours = prefs.getInt(KEY_META_AUTO_UPLOAD_INTERVAL_HOURS, 24).coerceAtLeast(1)
            val lastUploadAt = prefs.getLong(KEY_META_LAST_UPLOAD_AT, 0L)

            if (account.isBlank()) return "☁ Облако: не подключено"
            if (lastUploadAt <= 0L) return "☁ Облако: ожидает первую загрузку"

            val uploadedAt = java.time.Instant.ofEpochMilli(lastUploadAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val hoursSinceUpload = Duration.between(uploadedAt, now).toHours().coerceAtLeast(0L)
            return if (autoEnabled && hoursSinceUpload > (intervalHours * 2L)) {
                "⚠ Облако: синхронизация устарела"
            } else {
                "☁ Облако: ${uploadedAt.format(widgetDateTimeFormatter)}"
            }
        }

        private fun buildDataStateLine(shiftByDate: Map<LocalDate, String>): String {
            if (shiftByDate.isEmpty()) return "Локальные данные: нет смен"
            val latestDate = shiftByDate.keys.maxOrNull()
            return if (latestDate != null) {
                "Локальные данные: до ${latestDate.format(widgetDateFormatter)}"
            } else {
                "Локальные данные: доступны"
            }
        }

        private fun resolveQuickTemplate(
            widgetPrefs: SharedPreferences,
            quickTemplates: List<ShiftTemplateEntity>
        ): ShiftTemplateEntity? {
            if (quickTemplates.isEmpty()) return null
            val storedIndex = widgetPrefs.getInt(KEY_WIDGET_QUICK_TEMPLATE_INDEX, 0)
            val normalizedIndex = ((storedIndex % quickTemplates.size) + quickTemplates.size) % quickTemplates.size
            if (normalizedIndex != storedIndex) {
                widgetPrefs.edit { putInt(KEY_WIDGET_QUICK_TEMPLATE_INDEX, normalizedIndex) }
            }
            return quickTemplates.getOrNull(normalizedIndex)
        }

        private fun handleQuickAddTodayShift(
            context: Context,
            requestedShiftCode: String?
        ) {
            runBlocking {
                val db = AppDatabase.getDatabase(context)
                val templates = db.shiftTemplateDao().observeAll().first().sortedBy { it.sortOrder }
                val activeTemplates = templates.filter { it.active }
                val quickTemplates = if (activeTemplates.isNotEmpty()) activeTemplates else templates
                if (quickTemplates.isEmpty()) return@runBlocking

                val widgetPrefs = context.profileSharedPreferences(PREFS_WIDGET_SETTINGS)
                val currentQuick = resolveQuickTemplate(widgetPrefs, quickTemplates)
                val codeToApply = requestedShiftCode
                    ?.takeIf { code -> quickTemplates.any { it.code == code } }
                    ?: currentQuick?.code
                    ?: quickTemplates.first().code

                db.shiftDayDao().upsert(
                    ShiftDayEntity(
                        date = LocalDate.now().toString(),
                        shiftCode = codeToApply
                    )
                )

                val currentIndex = quickTemplates.indexOfFirst { it.code == codeToApply }.coerceAtLeast(0)
                val nextIndex = (currentIndex + 1) % quickTemplates.size
                widgetPrefs.edit { putInt(KEY_WIDGET_QUICK_TEMPLATE_INDEX, nextIndex) }
            }
        }

        private fun handleQuickClearTodayShift(context: Context) {
            runBlocking {
                val db = AppDatabase.getDatabase(context)
                db.shiftDayDao().deleteByDate(LocalDate.now().toString())
            }
        }

        private fun resolveShiftStartTime(
            template: ShiftTemplateEntity,
            alarmConfig: com.vigilante.shiftsalaryplanner.ShiftTemplateAlarmConfig?
        ): LocalTime {
            return if (alarmConfig != null) {
                LocalTime.of(
                    alarmConfig.startHour.coerceIn(0, 23),
                    alarmConfig.startMinute.coerceIn(0, 59)
                )
            } else if (template.nightHours > 0.0) {
                LocalTime.of(20, 0)
            } else {
                LocalTime.of(8, 0)
            }
        }

        private fun formatDurationCompact(duration: Duration): String {
            val totalMinutes = duration.toMinutes().coerceAtLeast(0L)
            val days = totalMinutes / (24 * 60)
            val hours = (totalMinutes % (24 * 60)) / 60
            val minutes = totalMinutes % 60
            return when {
                days > 0 -> "${days}д ${hours}ч"
                hours > 0 -> "${hours}ч ${minutes}м"
                else -> "${minutes}м"
            }
        }
    }
}
