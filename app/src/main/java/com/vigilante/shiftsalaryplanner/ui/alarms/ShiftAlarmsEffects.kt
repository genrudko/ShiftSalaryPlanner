package com.vigilante.shiftsalaryplanner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.net.toUri
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import kotlin.math.roundToInt
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

fun openFullScreenIntentPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    val fullScreenIntent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val appNotificationsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(fullScreenIntent) }
        .onFailure { runCatching { context.startActivity(appNotificationsIntent) } }
}

fun startInAppAlarmPreview(
    context: Context,
    behavior: ShiftAlarmBehaviorSettings
) {
    val previewVolumePercent = resolveCurrentAlarmVolumePercent(context)
    ShiftAlarmPlaybackService.startRinging(
        context = context,
        alarmKey = "preview_${System.currentTimeMillis()}",
        title = "Тестовый будильник",
        text = if (previewVolumePercent <= 0) {
            "Проверка встроенного будильника (тихий режим)"
        } else {
            "Проверка встроенного будильника приложения"
        },
        volumePercent = previewVolumePercent,
        soundUri = null,
        soundLabel = "",
        snoozeIntervalMinutes = behavior.snoozeIntervalMinutes.coerceIn(1, 120),
        snoozeCountLimit = behavior.snoozeCountLimit.coerceIn(0, 10),
        snoozeCurrentCount = 0,
        ringDurationSeconds = behavior.ringDurationSeconds.coerceIn(10, 3_600),
        rampUpDurationSeconds = behavior.rampUpDurationSeconds.coerceIn(0, 180),
        vibrationEnabled = behavior.vibrationEnabled,
        vibrationType = behavior.vibrationType,
        vibrationDurationSeconds = behavior.vibrationDurationSeconds.coerceIn(0, 300),
        customVibrationPattern = behavior.customVibrationPattern
    )
}

private fun resolveCurrentAlarmVolumePercent(context: Context): Int {
    val audioManager = context.getSystemService(AudioManager::class.java) ?: return 100
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
    val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM).coerceIn(0, max)
    return ((current * 100f) / max).roundToInt().coerceIn(0, 100)
}
