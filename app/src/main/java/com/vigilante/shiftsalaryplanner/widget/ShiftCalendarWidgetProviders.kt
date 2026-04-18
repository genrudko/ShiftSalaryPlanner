package com.vigilante.shiftsalaryplanner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.vigilante.shiftsalaryplanner.MainActivity
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val ACTION_CLASSIC_WIDGET_DATA_CHANGED =
    "com.vigilante.shiftsalaryplanner.widget.ACTION_CLASSIC_WIDGET_DATA_CHANGED"
private const val EXTRA_WIDGET_SIZE_MODE = "extra_widget_size_mode"

@Suppress("DEPRECATION")
abstract class BaseCalendarWidgetProvider : AppWidgetProvider() {

    protected abstract val providerClass: Class<out AppWidgetProvider>
    protected abstract val darkLayoutRes: Int
    protected abstract val lightLayoutRes: Int

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateCalendarWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateCalendarWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_CLASSIC_WIDGET_DATA_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                refreshAllWidgets(context)
            }
        }
    }

    private fun updateCalendarWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val widgetPrefs = context.profileSharedPreferences(PREFS_WIDGET_SETTINGS)
        val useLightTheme = shouldUseWidgetLightTheme(context, widgetPrefs)
        val amoled = effectiveWidgetThemeMode(context, widgetPrefs) == WidgetThemeMode.AMOLED
        val displaySettings = readWidgetDisplaySettings(widgetPrefs)
        val mode = resolveWidgetSizeMode(appWidgetManager.getAppWidgetOptions(appWidgetId))

        val views = RemoteViews(
            context.packageName,
            if (useLightTheme) lightLayoutRes else darkLayoutRes
        )

        val month = YearMonth.now()
        val locale = Locale("ru")
        val title = buildString {
            append(
                month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            )
            append(' ')
            append(month.year)
        }

        views.setTextViewText(R.id.widgetMonthTitle, title)
        views.setTextViewText(R.id.widgetMonthSubtitle, "Сегодня ${LocalDate.now().dayOfMonth}")
        views.setViewVisibility(
            R.id.widgetMonthSubtitle,
            if (mode != WidgetSizeMode.COMPACT && displaySettings.showMonthSubtitle) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widgetOpenAppButton,
            if (mode != WidgetSizeMode.COMPACT && displaySettings.showOpenAppButton) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widgetWeekdaysRow,
            if (mode != WidgetSizeMode.COMPACT && displaySettings.showWeekdayHeader) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widgetCalendarGrid,
            if (mode != WidgetSizeMode.COMPACT && displaySettings.showCalendarGrid) View.VISIBLE else View.GONE
        )
        views.setViewVisibility(
            R.id.widgetEmptyView,
            if (mode != WidgetSizeMode.COMPACT && displaySettings.showCalendarGrid) View.VISIBLE else View.GONE
        )

        if (amoled && !useLightTheme) {
            views.setInt(R.id.widgetRoot, "setBackgroundResource", R.drawable.widget_month_bg_amoled)
        }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId * 20 + providerClass.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widgetOpenAppButton, openAppPendingIntent)

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
        val componentName = ComponentName(context, providerClass)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        ids.forEach { appWidgetId ->
            updateCalendarWidget(context, appWidgetManager, appWidgetId)
        }
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
}

class ShiftMonthClassicWidgetProviderV2 : BaseCalendarWidgetProvider() {
    override val providerClass: Class<out AppWidgetProvider> = ShiftMonthClassicWidgetProviderV2::class.java
    override val darkLayoutRes: Int = R.layout.widget_shift_month_classic
    override val lightLayoutRes: Int = R.layout.widget_shift_month_classic_light
}

class ShiftMonthMiniWidgetProviderV2 : BaseCalendarWidgetProvider() {
    override val providerClass: Class<out AppWidgetProvider> = ShiftMonthMiniWidgetProviderV2::class.java
    override val darkLayoutRes: Int = R.layout.widget_shift_month_classic
    override val lightLayoutRes: Int = R.layout.widget_shift_month_classic_light
}
