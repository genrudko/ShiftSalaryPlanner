package com.vigilante.shiftsalaryplanner.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.wear.MainActivity
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshot
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshotCache
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ShiftComplicationService : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return buildComplication(WearSnapshotCache.load(this), request.complicationType)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return buildComplication(WearSnapshot(todaySummary = "День 08:00"), type)
    }

    private fun buildComplication(
        snapshot: WearSnapshot,
        type: ComplicationType
    ): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val nextShift = snapshot.todaySummary.ifBlank { "Без смен" }
        val daysToSalary = runCatching {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(snapshot.payroll.salaryDate))
        }.getOrNull()
        val title = daysToSalary
            ?.takeIf { it >= 0 }
            ?.let { "ЗП ${it}д" }
            ?: "Смены"
        val text = nextShift
            .replace("смен нет", "Нет смен")
            .take(18)
            .ifBlank { "Shift" }
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("$text, $title").build()
        )
            .setTitle(PlainComplicationText.Builder(title).build())
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.mipmap.ic_launcher_foreground)
                ).build()
            )
            .setTapAction(openAppPendingIntent())
            .build()
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
