package com.vigilante.shiftsalaryplanner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.net.toUri
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun rescheduleShiftAlarms(
    context: Context,
    settings: ShiftAlarmSettings,
    savedDays: List<ShiftDayEntity>,
    templateMap: Map<String, ShiftTemplateEntity>,
    mirrorToSystemClockApp: Boolean = false,
    allowSystemClockUiFallback: Boolean = true
): ShiftAlarmRescheduleResult = withContext(Dispatchers.IO) {
    ShiftAlarmScheduler.reschedule(
        context = context,
        settings = settings,
        savedDays = savedDays,
        templateMap = templateMap,
        mirrorToSystemClockApp = mirrorToSystemClockApp,
        allowSystemClockUiFallback = allowSystemClockUiFallback
    )
}

suspend fun saveAndRescheduleShiftAlarms(
    store: ShiftAlarmStore,
    context: Context,
    settings: ShiftAlarmSettings,
    savedDays: List<ShiftDayEntity>,
    templateMap: Map<String, ShiftTemplateEntity>,
    mirrorToSystemClockApp: Boolean = false,
    allowSystemClockUiFallback: Boolean = true
): ShiftAlarmRescheduleResult {
    store.save(settings)
    return rescheduleShiftAlarms(
        context = context,
        settings = settings,
        savedDays = savedDays,
        templateMap = templateMap,
        mirrorToSystemClockApp = mirrorToSystemClockApp,
        allowSystemClockUiFallback = allowSystemClockUiFallback
    )
}

@SuppressLint("InlinedApi")
fun openExactAlarmPermissionSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

fun openSystemClockOrDateSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_DATE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
