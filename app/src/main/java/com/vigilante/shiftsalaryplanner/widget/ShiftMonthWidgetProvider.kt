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
import com.vigilante.shiftsalaryplanner.MainActivity
import com.vigilante.shiftsalaryplanner.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private const val ACTION_WIDGET_DATA_CHANGED = "com.vigilante.shiftsalaryplanner.widget.ACTION_WIDGET_DATA_CHANGED"
private const val EXTRA_WIDGET_SIZE_MODE = "extra_widget_size_mode"

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
            val views = RemoteViews(context.packageName, R.layout.widget_shift_month)
            val month = YearMonth.now()
            val today = LocalDate.now()
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
            views.setTextViewText(R.id.widgetMonthSubtitle, "Сегодня: ${today.dayOfMonth}")

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetOpenAppButton, openAppPendingIntent)

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val mode = resolveWidgetSizeMode(options)

            val serviceIntent = Intent(context, ShiftMonthWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_WIDGET_SIZE_MODE, mode.name)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            views.setRemoteAdapter(R.id.widgetCalendarGrid, serviceIntent)
            views.setEmptyView(R.id.widgetCalendarGrid, R.id.widgetEmptyView)
            views.setViewVisibility(
                R.id.widgetMonthSubtitle,
                if (mode == WidgetSizeMode.COMPACT) View.GONE else View.VISIBLE
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetCalendarGrid)
        }

        private fun resolveWidgetSizeMode(options: Bundle): WidgetSizeMode {
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val minSide = max(1, minOf(minWidthDp, minHeightDp))

            return when {
                minSide < 180 -> WidgetSizeMode.COMPACT
                minSide < 260 -> WidgetSizeMode.MEDIUM
                else -> WidgetSizeMode.LARGE
            }
        }
    }
}
