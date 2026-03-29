package com.vigilante.shiftsalaryplanner.widget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.ColorUtils
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val PREFS_SHIFT_COLORS = "shift_colors"
private const val EXTRA_WIDGET_SIZE_MODE = "extra_widget_size_mode"

data class WidgetDayCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val shiftTitle: String,
    val shiftSubtitle: String,
    val titleChipColor: Int,
    val subtitleChipColor: Int,
    val textColor: Int,
    val showCard: Boolean
)

class ShiftMonthWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ShiftMonthRemoteViewsFactory(applicationContext, intent)
    }
}

class ShiftMonthRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetSizeMode: WidgetSizeMode = runCatching {
        WidgetSizeMode.valueOf(intent.getStringExtra(EXTRA_WIDGET_SIZE_MODE) ?: WidgetSizeMode.LARGE.name)
    }.getOrDefault(WidgetSizeMode.LARGE)

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val colorPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_SHIFT_COLORS, Context.MODE_PRIVATE)
    }

    private var cells: List<WidgetDayCell> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        cells = loadMonthCells()
    }

    override fun onDestroy() {
        cells = emptyList()
    }

    override fun getCount(): Int = cells.size

    override fun getViewAt(position: Int): RemoteViews {
        val cell = cells.getOrNull(position) ?: return RemoteViews(context.packageName, layoutForMode())
        val views = RemoteViews(context.packageName, layoutForMode())

        views.setTextViewText(R.id.widgetDayNumber, cell.date.dayOfMonth.toString())
        views.setTextColor(R.id.widgetDayNumber, resolveDayNumberColor(cell))
        views.setViewVisibility(R.id.widgetTodayDot, if (cell.isToday) View.VISIBLE else View.GONE)
        views.setInt(
            R.id.widgetCellRoot,
            "setBackgroundResource",
            if (cell.isToday) R.drawable.widget_cell_today_bg else R.drawable.widget_cell_bg
        )
        views.setFloat(R.id.widgetCellRoot, "setAlpha", if (cell.inCurrentMonth) 1f else 0.45f)

        if (!cell.showCard) {
            setCardVisibility(views, false)
            return views
        }

        setCardVisibility(views, true)

        when (widgetSizeMode) {
            WidgetSizeMode.COMPACT -> {
                views.setTextViewText(R.id.widgetShiftCard, cell.shiftTitle)
                views.setTextColor(R.id.widgetShiftCard, cell.textColor)
                views.setInt(R.id.widgetShiftChipBg, "setColorFilter", cell.titleChipColor)
            }
            WidgetSizeMode.MEDIUM,
            WidgetSizeMode.LARGE -> {
                views.setTextViewText(R.id.widgetShiftTitle, cell.shiftTitle)
                views.setTextViewText(R.id.widgetShiftSubtitle, cell.shiftSubtitle)
                views.setTextColor(R.id.widgetShiftTitle, cell.textColor)
                views.setTextColor(R.id.widgetShiftSubtitle, cell.textColor)
                views.setInt(R.id.widgetShiftTitleBg, "setColorFilter", cell.titleChipColor)
                views.setInt(R.id.widgetShiftSubtitleBg, "setColorFilter", cell.subtitleChipColor)
            }
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = cells.getOrNull(position)?.date?.toEpochDay() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadMonthCells(): List<WidgetDayCell> = runBlocking {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val shiftDays = db.shiftDayDao().observeAll().first()
            val templates = db.shiftTemplateDao().observeAll().first()
            val templateMap = templates.associateBy { it.code }
            val shiftMap = shiftDays.associateBy { LocalDate.parse(it.date, dateFormatter) }

            val month = YearMonth.now()
            val start = month.atDay(1).let { first ->
                var cursor = first
                while (cursor.dayOfWeek != DayOfWeek.MONDAY) {
                    cursor = cursor.minusDays(1)
                }
                cursor
            }
            val endExclusive = start.plusDays(42)

            generateSequence(start) { current ->
                val next = current.plusDays(1)
                if (next.isBefore(endExclusive)) next else null
            }
                .take(42)
                .map { date ->
                    toCell(
                        date = date,
                        month = month,
                        shift = shiftMap[date],
                        templateMap = templateMap
                    )
                }
                .toList()
        }
    }

    private fun toCell(
        date: LocalDate,
        month: YearMonth,
        shift: ShiftDayEntity?,
        templateMap: Map<String, ShiftTemplateEntity>
    ): WidgetDayCell {
        val template = shift?.shiftCode?.let { templateMap[it] }
        val baseColor = resolveCardColor(template)
        val titleChipColor = baseColor
        val subtitleChipColor = darken(baseColor, 0.22f)
        val textColor = resolveTextColor(baseColor)

        val shiftTitle = when {
            template == null -> ""
            widgetSizeMode == WidgetSizeMode.COMPACT -> compactLabel(template)
            else -> longLabel(template)
        }
        val shiftSubtitle = when {
            template == null -> ""
            widgetSizeMode == WidgetSizeMode.COMPACT -> ""
            else -> compactMeta(template)
        }

        return WidgetDayCell(
            date = date,
            inCurrentMonth = YearMonth.from(date) == month,
            isToday = date == LocalDate.now(),
            shiftTitle = shiftTitle,
            shiftSubtitle = shiftSubtitle,
            titleChipColor = titleChipColor,
            subtitleChipColor = subtitleChipColor,
            textColor = textColor,
            showCard = template != null
        )
    }

    private fun compactLabel(template: ShiftTemplateEntity): String {
        return when (template.code.uppercase()) {
            "ВЫХ" -> "Вых"
            "ОТ" -> "Отп"
            "Б" -> "Бол"
            else -> template.code
        }
    }

    private fun longLabel(template: ShiftTemplateEntity): String {
        return when (template.code.uppercase()) {
            "ВЫХ" -> "Выходной"
            "ОТ" -> "Отпуск"
            "Б" -> "Больничный"
            else -> template.title.ifBlank { template.code }
        }
    }

    private fun compactMeta(template: ShiftTemplateEntity): String {
        return when (template.code.uppercase()) {
            "ВЫХ" -> "ВД"
            "ОТ" -> "Отдых"
            "Б" -> "Больничн."
            else -> formatDuration(template.totalHours - template.breakHours, template.nightHours)
        }
    }

    private fun resolveCardColor(template: ShiftTemplateEntity?): Int {
        if (template == null) return Color.TRANSPARENT
        return colorPrefs.getInt(template.code, parseColor(template.colorHex, Color.parseColor("#4A67C9")))
    }

    private fun resolveTextColor(background: Int): Int {
        val luminance = ColorUtils.calculateLuminance(background)
        return if (luminance < 0.52) Color.WHITE else Color.parseColor("#142547")
    }

    private fun resolveDayNumberColor(cell: WidgetDayCell): Int {
        return when {
            cell.isToday -> Color.WHITE
            cell.date.dayOfWeek == DayOfWeek.SATURDAY || cell.date.dayOfWeek == DayOfWeek.SUNDAY -> Color.parseColor("#FF7E86")
            cell.inCurrentMonth -> Color.parseColor("#F1F5FF")
            else -> Color.parseColor("#A1AACC")
        }
    }

    private fun parseColor(value: String?, fallback: Int): Int {
        return try {
            if (value.isNullOrBlank()) fallback else Color.parseColor(value)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun darken(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - amount)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun formatDuration(paidHours: Double, nightHours: Double): String {
        return when {
            paidHours > 0.0 -> humanizeHours(paidHours)
            nightHours > 0.0 -> humanizeHours(nightHours)
            else -> ""
        }
    }

    private fun humanizeHours(value: Double): String {
        val minutes = (value * 60.0).toLong().coerceAtLeast(0L)
        val hoursPart = Duration.ofMinutes(minutes).toHours()
        val minutesPart = (minutes % 60L)
        return if (minutesPart == 0L) {
            "${hoursPart}ч"
        } else {
            "${hoursPart}ч ${minutesPart}м"
        }
    }

    private fun setCardVisibility(views: RemoteViews, visible: Boolean) {
        val value = if (visible) View.VISIBLE else View.GONE
        when (widgetSizeMode) {
            WidgetSizeMode.COMPACT -> {
                views.setViewVisibility(R.id.widgetShiftChipWrap, value)
            }
            WidgetSizeMode.MEDIUM,
            WidgetSizeMode.LARGE -> {
                views.setViewVisibility(R.id.widgetShiftTitleWrap, value)
                views.setViewVisibility(R.id.widgetShiftSubtitleWrap, value)
            }
        }
    }

    private fun layoutForMode(): Int = when (widgetSizeMode) {
        WidgetSizeMode.COMPACT -> R.layout.widget_shift_month_day_compact
        WidgetSizeMode.MEDIUM -> R.layout.widget_shift_month_day_medium
        WidgetSizeMode.LARGE -> R.layout.widget_shift_month_day_large
    }
}
