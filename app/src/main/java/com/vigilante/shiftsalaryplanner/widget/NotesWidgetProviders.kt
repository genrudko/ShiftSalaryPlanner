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
import com.vigilante.shiftsalaryplanner.BottomTab
import com.vigilante.shiftsalaryplanner.MainActivity
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.parseColorHex
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNotesStore
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class NotesWidgetMode {
    RECENT,
    TODAY
}

private data class NotesWidgetTheme(
    val layoutRes: Int,
    val titleColor: Int,
    val subtitleColor: Int,
    val cardTitleColor: Int,
    val cardBodyColor: Int,
    val emptyColor: Int,
    val actionColor: Int
)

abstract class BaseNotesWidgetProvider : AppWidgetProvider() {

    protected abstract val providerClass: Class<out AppWidgetProvider>
    protected abstract val mode: NotesWidgetMode

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            safeUpdateNotesWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        safeUpdateNotesWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AppNotesStore.ACTION_APP_NOTES_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> refreshAllWidgets(context)
        }
    }

    private fun safeUpdateNotesWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        runCatching {
            updateNotesWidget(context, appWidgetManager, appWidgetId)
        }.onFailure {
            showFallbackWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateNotesWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val widgetPrefs = context.profileSharedPreferences(PREFS_WIDGET_SETTINGS)
        val useLightTheme = shouldUseWidgetLightTheme(context, widgetPrefs)
        val amoled = effectiveWidgetThemeMode(context, widgetPrefs) == WidgetThemeMode.AMOLED
        val theme = notesWidgetTheme(useLightTheme)
        val notes = resolveNotes(context)
        val views = RemoteViews(context.packageName, theme.layoutRes)

        if (amoled && !useLightTheme) {
            views.setInt(R.id.widgetNotesRoot, "setBackgroundResource", R.drawable.widget_month_bg_amoled)
        }

        views.setTextViewText(R.id.widgetNotesTitle, if (mode == NotesWidgetMode.TODAY) "Заметки сегодня" else "Заметки")
        views.setTextViewText(R.id.widgetNotesSubtitle, notesSubtitle(notes))
        views.setTextColor(R.id.widgetNotesTitle, theme.titleColor)
        views.setTextColor(R.id.widgetNotesSubtitle, theme.subtitleColor)
        views.setTextColor(R.id.widgetNotesOpenApp, theme.actionColor)
        views.setTextColor(R.id.widgetNotesEmpty, theme.emptyColor)

        val openNotesPendingIntent = openNotesPendingIntent(context, appWidgetId)
        views.setOnClickPendingIntent(R.id.widgetNotesRoot, openNotesPendingIntent)
        views.setOnClickPendingIntent(R.id.widgetNotesOpenApp, openNotesPendingIntent)

        bindNoteRow(views, notes.getOrNull(0), 1, theme)
        bindNoteRow(views, notes.getOrNull(1), 2, theme)
        bindNoteRow(views, notes.getOrNull(2), 3, theme)

        views.setViewVisibility(R.id.widgetNotesEmpty, if (notes.isEmpty()) View.VISIBLE else View.GONE)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, providerClass)
        appWidgetManager.getAppWidgetIds(componentName).forEach { appWidgetId ->
            safeUpdateNotesWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun showFallbackWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_notes)
        val openNotesPendingIntent = openNotesPendingIntent(context, appWidgetId)
        views.setTextViewText(R.id.widgetNotesTitle, "Заметки")
        views.setTextViewText(R.id.widgetNotesSubtitle, "Нажмите, чтобы открыть приложение")
        views.setTextViewText(R.id.widgetNotesEmpty, "Не удалось обновить виджет")
        views.setViewVisibility(R.id.widgetNoteRow1, View.GONE)
        views.setViewVisibility(R.id.widgetNoteRow2, View.GONE)
        views.setViewVisibility(R.id.widgetNoteRow3, View.GONE)
        views.setViewVisibility(R.id.widgetNotesEmpty, View.VISIBLE)
        views.setOnClickPendingIntent(R.id.widgetNotesRoot, openNotesPendingIntent)
        views.setOnClickPendingIntent(R.id.widgetNotesOpenApp, openNotesPendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun resolveNotes(context: Context): List<AppNote> {
        val allNotes = AppNotesStore(context).allNotesSnapshot()
        val todayIso = LocalDate.now().toString()
        return when (mode) {
            NotesWidgetMode.TODAY -> allNotes
                .filter { it.date == todayIso }
                .sortedByDescending { it.updatedAtMillis }
                .take(3)
            NotesWidgetMode.RECENT -> allNotes
                .sortedByDescending { it.updatedAtMillis }
                .take(3)
        }
    }

    private fun notesSubtitle(notes: List<AppNote>): String {
        return when {
            notes.isEmpty() && mode == NotesWidgetMode.TODAY -> "На сегодня заметок нет"
            notes.isEmpty() -> "Пока пусто"
            mode == NotesWidgetMode.TODAY -> "Сегодня: ${notes.size}"
            else -> "Последние: ${notes.size}"
        }
    }

    private fun openNotesPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_TAB, BottomTab.NOTES.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            providerClass.hashCode() + appWidgetId * 37,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun bindNoteRow(
        views: RemoteViews,
        note: AppNote?,
        index: Int,
        theme: NotesWidgetTheme
    ) {
        val rowId = rowId(index)
        val stripeId = stripeId(index)
        val titleId = titleId(index)
        val bodyId = bodyId(index)
        val metaId = metaId(index)

        if (note == null) {
            views.setViewVisibility(rowId, View.GONE)
            return
        }

        views.setViewVisibility(rowId, View.VISIBLE)
        views.setInt(stripeId, "setBackgroundColor", parseColorHex(note.colorHex, theme.actionColor))
        views.setTextViewText(titleId, note.title.ifBlank { "Без заголовка" })
        views.setTextViewText(bodyId, noteBodyPreview(note))
        views.setTextViewText(metaId, noteMeta(note))
        views.setTextColor(titleId, theme.cardTitleColor)
        views.setTextColor(bodyId, theme.cardBodyColor)
        views.setTextColor(metaId, theme.subtitleColor)
    }

    private fun noteBodyPreview(note: AppNote): String {
        val bodyLine = note.body
            .replace(Regex("""\[\[media:([A-Za-z0-9-]+)]]"""), "")
            .lines()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
        if (!bodyLine.isNullOrBlank()) return bodyLine.take(96)

        val checklistLine = note.checklist.firstOrNull()?.text?.trim()
        if (!checklistLine.isNullOrBlank()) return "☑ $checklistLine"

        return if (note.attachments.isNotEmpty()) {
            "Вложения: ${note.attachments.size}"
        } else {
            "Пустая заметка"
        }
    }

    private fun noteMeta(note: AppNote): String {
        val date = runCatching {
            LocalDate.parse(note.date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }.getOrElse { note.date }
        val done = note.checklist.count { it.checked }
        val checklist = if (note.checklist.isNotEmpty()) " · чек $done/${note.checklist.size}" else ""
        val attachments = if (note.attachments.isNotEmpty()) " · влож. ${note.attachments.size}" else ""
        return "$date$checklist$attachments"
    }

    private fun notesWidgetTheme(light: Boolean): NotesWidgetTheme {
        return if (light) {
            NotesWidgetTheme(
                layoutRes = R.layout.widget_notes_light,
                titleColor = 0xFF17211D.toInt(),
                subtitleColor = 0xFF5E6B66.toInt(),
                cardTitleColor = 0xFF18211E.toInt(),
                cardBodyColor = 0xFF53605B.toInt(),
                emptyColor = 0xFF6B756F.toInt(),
                actionColor = 0xFF0D665A.toInt()
            )
        } else {
            NotesWidgetTheme(
                layoutRes = R.layout.widget_notes,
                titleColor = 0xFFF7F9FF.toInt(),
                subtitleColor = 0xFFAEB8D8.toInt(),
                cardTitleColor = 0xFFF7F9FF.toInt(),
                cardBodyColor = 0xFFC8D0EA.toInt(),
                emptyColor = 0xFFD7DDF1.toInt(),
                actionColor = 0xFF8FEAD7.toInt()
            )
        }
    }

    private fun rowId(index: Int): Int = when (index) {
        1 -> R.id.widgetNoteRow1
        2 -> R.id.widgetNoteRow2
        else -> R.id.widgetNoteRow3
    }

    private fun stripeId(index: Int): Int = when (index) {
        1 -> R.id.widgetNoteStripe1
        2 -> R.id.widgetNoteStripe2
        else -> R.id.widgetNoteStripe3
    }

    private fun titleId(index: Int): Int = when (index) {
        1 -> R.id.widgetNoteTitle1
        2 -> R.id.widgetNoteTitle2
        else -> R.id.widgetNoteTitle3
    }

    private fun bodyId(index: Int): Int = when (index) {
        1 -> R.id.widgetNoteBody1
        2 -> R.id.widgetNoteBody2
        else -> R.id.widgetNoteBody3
    }

    private fun metaId(index: Int): Int = when (index) {
        1 -> R.id.widgetNoteMeta1
        2 -> R.id.widgetNoteMeta2
        else -> R.id.widgetNoteMeta3
    }
}

class NotesListWidgetProvider : BaseNotesWidgetProvider() {
    override val providerClass: Class<out AppWidgetProvider> = NotesListWidgetProvider::class.java
    override val mode: NotesWidgetMode = NotesWidgetMode.RECENT
}

class TodayNotesWidgetProvider : BaseNotesWidgetProvider() {
    override val providerClass: Class<out AppWidgetProvider> = TodayNotesWidgetProvider::class.java
    override val mode: NotesWidgetMode = NotesWidgetMode.TODAY
}
