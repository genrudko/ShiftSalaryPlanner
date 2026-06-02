package com.vigilante.shiftsalaryplanner.wear.tile

import android.content.ComponentName
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.vigilante.shiftsalaryplanner.wear.MainActivity
import com.vigilante.shiftsalaryplanner.wear.sync.WearAssignment
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshot
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshotCache
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService

class TodayTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val snapshot = WearSnapshotCache.load(this)
        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(5 * 60 * 1000L)
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(
                                    LayoutElementBuilders.Layout.Builder()
                                        .setRoot(tileRoot(snapshot))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    private fun tileRoot(snapshot: WearSnapshot): LayoutElementBuilders.LayoutElement {
        val todayAssignment = snapshot.calendar.firstOrNull { it.isToday }?.assignments?.firstOrNull()
        val shiftText = snapshot.todaySummary.ifBlank { "Сегодня без смен" }
        val payText = "К выплате ${formatMoney(snapshot.payroll.netTotal)}"
        val alarmText = snapshot.alarms.upcoming.firstOrNull()?.let { alarm ->
            "Будильник ${formatTileTime(alarm.triggerAtMillis)}"
        } ?: "Будильников нет"
        val shiftAccent = todayAssignment?.colorHex?.let(::parseTileColor)
            ?: if (snapshot.todaySummary.isBlank()) 0xFF8CF79B.toInt() else 0xFF8EC5FF.toInt()
        val shiftGlyph = todayAssignment?.let { assignment ->
            wearShiftGlyph(
                iconKey = assignment.iconKey,
                fallbackCode = assignment.displayCode.ifBlank { assignment.shiftCode }
            )
        } ?: "✓"
        val shiftInfo = shiftTileInfo(todayAssignment, shiftText)
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFF050708.toInt()))
                            .build()
                    )
                    .setClickable(openAppClickable())
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(tileSpacer(14f))
                    .addContent(todayPill())
                    .addContent(tileSpacer(7f))
                    .addContent(shiftCard(shiftInfo, shiftAccent, shiftGlyph))
                    .addContent(tileSpacer(6f))
                    .addContent(statusRow(alarmText, payText))
                    .build()
            )
            .build()
    }

    private fun todayPill(): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(104f))
            .setHeight(DimensionBuilders.dp(28f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(surface(0xFF101719.toInt(), 16f, 0xFF26363B.toInt()))
            .addContent(tileText("Сегодня", 13f, 0xFFF2F7F4.toInt(), true, 1))
            .build()
    }

    private fun shiftCard(
        shiftInfo: ShiftTileInfo,
        accentColor: Int,
        shiftGlyph: String
    ): LayoutElementBuilders.LayoutElement {
        val content = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.dp(150f))
            .setHeight(DimensionBuilders.dp(80f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
            .addContent(
                LayoutElementBuilders.Row.Builder()
                    .setWidth(DimensionBuilders.dp(150f))
                    .setHeight(DimensionBuilders.dp(39f))
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(shiftBadge(shiftGlyph, accentColor))
                    .addContent(tileHorizontalSpacer(8f))
                    .addContent(
                        LayoutElementBuilders.Column.Builder()
                            .setWidth(DimensionBuilders.dp(114f))
                            .setHeight(DimensionBuilders.dp(36f))
                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                            .addContent(
                                tileText(
                                    value = "Смена",
                                    sizeSp = 7.4f,
                                    color = accentColor,
                                    bold = true,
                                    maxLines = 1,
                                    alignment = LayoutElementBuilders.TEXT_ALIGN_START,
                                    limit = 12
                                )
                            )
                            .addContent(tileSpacer(1f))
                            .addContent(
                                tileText(
                                    value = shiftInfo.shiftTitle,
                                    sizeSp = compactTileTextSize(
                                        value = shiftInfo.shiftTitle,
                                        base = 10.8f,
                                        medium = 9.8f,
                                        min = 8.8f
                                    ),
                                    color = 0xFFF5F8F6.toInt(),
                                    bold = true,
                                    maxLines = 2,
                                    alignment = LayoutElementBuilders.TEXT_ALIGN_START,
                                    limit = 46
                                )
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(tileSpacer(4f))

        if (shiftInfo.workplace.isNotBlank()) {
            content
                .addContent(
                    tileText(
                        value = "Работа",
                        sizeSp = 7.2f,
                        color = 0xFF9FF7B0.toInt(),
                        bold = true,
                        maxLines = 1,
                        alignment = LayoutElementBuilders.TEXT_ALIGN_START,
                        limit = 12
                    )
                )
                .addContent(tileSpacer(1f))
                .addContent(
                    tileText(
                        value = shiftInfo.workplace,
                        sizeSp = compactTileTextSize(
                            value = shiftInfo.workplace,
                            base = 8.3f,
                            medium = 7.6f,
                            min = 7.0f
                        ),
                        color = 0xFFA8B4AE.toInt(),
                        bold = false,
                        maxLines = 2,
                        alignment = LayoutElementBuilders.TEXT_ALIGN_START,
                        limit = 54
                    )
                )
        }

        if (shiftInfo.timeLabel.isNotBlank()) {
            content
                .addContent(tileSpacer(2f))
                .addContent(
                    tileText(
                        value = shiftInfo.timeLabel,
                        sizeSp = 7.8f,
                        color = 0xFF9FC8FF.toInt(),
                        bold = true,
                        maxLines = 1,
                        alignment = LayoutElementBuilders.TEXT_ALIGN_START,
                        limit = 34
                    )
                )
        }

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(176f))
            .setHeight(DimensionBuilders.dp(96f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(surface(0xFF101719.toInt(), 24f, 0xFF2A3B40.toInt()))
            .addContent(content.build())
            .build()
    }

    private fun shiftBadge(glyph: String, color: Int): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(28f))
            .setHeight(DimensionBuilders.dp(28f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(surface(color, 12f, 0x66FFFFFF))
            .addContent(tileText(glyph, tileGlyphSize(glyph), readableTileText(color), true, 1))
            .build()
    }

    private fun statusRow(
        alarmText: String,
        payText: String
    ): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Row.Builder()
            .setWidth(DimensionBuilders.dp(176f))
            .setHeight(DimensionBuilders.dp(46f))
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(statusChip("⏰", alarmText, 0xFF192326.toInt(), 0xFFA8B4AE.toInt()))
            .addContent(tileHorizontalSpacer(6f))
            .addContent(statusChip("₽", payText, 0xFF173120.toInt(), 0xFF9FF7B0.toInt()))
            .build()
    }

    private fun statusChip(
        marker: String,
        value: String,
        background: Int,
        textColor: Int
    ): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(85f))
            .setHeight(DimensionBuilders.dp(46f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(surface(background, 18f, 0xFF28393D.toInt()))
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.dp(72f))
                    .setHeight(DimensionBuilders.dp(34f))
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(tileText(marker, 10f, textColor, true, 1))
                    .addContent(tileSpacer(1f))
                    .addContent(tileText(value, 9f, textColor, false, 2))
                    .build()
            )
            .build()
    }

    private fun tileText(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean,
        maxLines: Int = 2,
        alignment: Int = LayoutElementBuilders.TEXT_ALIGN_CENTER,
        limit: Int = 32
    ): LayoutElementBuilders.Text {
        return LayoutElementBuilders.Text.Builder()
            .setText(value.fitTileText(limit))
            .setMaxLines(maxLines)
            .setMultilineAlignment(alignment)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(DimensionBuilders.sp(sizeSp))
                    .setColor(ColorBuilders.argb(color))
                    .build()
            )
            .build()
    }

    private fun tileSpacer(heightDp: Float): LayoutElementBuilders.Spacer {
        return LayoutElementBuilders.Spacer.Builder()
            .setHeight(DimensionBuilders.dp(heightDp))
            .build()
    }

    private fun tileHorizontalSpacer(widthDp: Float): LayoutElementBuilders.Spacer {
        return LayoutElementBuilders.Spacer.Builder()
            .setWidth(DimensionBuilders.dp(widthDp))
            .build()
    }

    private fun surface(
        color: Int,
        radiusDp: Float,
        borderColor: Int?
    ): ModifiersBuilders.Modifiers {
        val builder = ModifiersBuilders.Modifiers.Builder()
            .setBackground(
                ModifiersBuilders.Background.Builder()
                    .setColor(ColorBuilders.argb(color))
                    .setCorner(
                        ModifiersBuilders.Corner.Builder()
                            .setRadius(DimensionBuilders.dp(radiusDp))
                            .build()
                    )
                    .build()
            )
        borderColor?.let {
            builder.setBorder(
                ModifiersBuilders.Border.Builder()
                    .setWidth(DimensionBuilders.dp(1f))
                    .setColor(ColorBuilders.argb(it))
                    .build()
            )
        }
        return builder.build()
    }

    private fun shiftTileInfo(
        assignment: WearAssignment?,
        fallbackSummary: String
    ): ShiftTileInfo {
        if (assignment == null) {
            return ShiftTileInfo(
                shiftTitle = fallbackSummary.ifBlank { "смен нет" },
                workplace = "",
                timeLabel = ""
            )
        }
        val title = assignment.title.ifBlank {
            assignment.displayCode.ifBlank { assignment.shiftCode }
        }
        return ShiftTileInfo(
            shiftTitle = title,
            workplace = assignment.workplaceName,
            timeLabel = shiftTimeLabel(assignment)
        )
    }

    private fun shiftTimeLabel(assignment: WearAssignment): String {
        val time = when {
            assignment.start.isNotBlank() && assignment.end.isNotBlank() -> "${assignment.start}-${assignment.end}"
            assignment.start.isNotBlank() -> assignment.start
            else -> ""
        }
        val hours = assignment.paidHours.takeIf { it > 0.0 }?.let(::formatHours).orEmpty()
        return listOf(time, hours).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun openAppClickable(): ModifiersBuilders.Clickable {
        val component = ComponentName(this, MainActivity::class.java)
        return ModifiersBuilders.Clickable.Builder()
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(component.packageName)
                            .setClassName(component.className)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun formatMoney(value: Double): String {
        return String.format(Locale.forLanguageTag("ru-RU"), "%,.0f ₽", value).replace(',', ' ')
    }

    private fun formatHours(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toInt()} ч"
        } else {
            "$rounded ч"
        }
    }

    private fun formatTileTime(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "--:--"
        val time = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }

    private fun parseTileColor(hex: String): Int {
        return runCatching { android.graphics.Color.parseColor(hex) }
            .getOrDefault(0xFF8CF79B.toInt())
    }

    private fun readableTileText(color: Int): Int {
        val red = android.graphics.Color.red(color) / 255f
        val green = android.graphics.Color.green(color) / 255f
        val blue = android.graphics.Color.blue(color) / 255f
        val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
        return if (luminance > 0.58f) 0xFF07110A.toInt() else 0xFFF2F7F4.toInt()
    }

    private fun wearShiftGlyph(iconKey: String, fallbackCode: String): String {
        val key = iconKey.trim()
        return when {
            key.startsWith("EMOJI:") -> key.removePrefix("EMOJI:").ifBlank { fallbackCode }
            key == "SUN" -> "☀"
            key == "MOON" -> "☾"
            key == "EIGHT" -> "8"
            key == "HOME" -> "⌂"
            key == "OT" -> "ОТ"
            key == "SICK" -> "✚"
            key == "STAR" -> "★"
            key == "CHECK" || key == "TASK" -> "✓"
            key == "TEXT" -> fallbackCode
            else -> fallbackCode.ifBlank { key }.take(3)
        }
    }

    private fun tileGlyphSize(glyph: String): Float {
        return when {
            glyph.length <= 1 -> 13f
            glyph.length == 2 -> 10.5f
            glyph.length == 3 -> 8.8f
            else -> 7.5f
        }
    }

    private fun compactTileTextSize(
        value: String,
        base: Float,
        medium: Float,
        min: Float
    ): Float {
        val length = value.trim().length
        return when {
            length >= 38 -> min
            length >= 24 -> medium
            length >= 18 -> base - 0.5f
            else -> base
        }
    }

    private fun String.fitTileText(limit: Int): String {
        val trimmed = trim()
        if (trimmed.length <= limit) return trimmed
        return trimmed.take((limit - 1).coerceAtLeast(0)).trimEnd() + "…"
    }

    private data class ShiftTileInfo(
        val shiftTitle: String,
        val workplace: String,
        val timeLabel: String
    )
}
