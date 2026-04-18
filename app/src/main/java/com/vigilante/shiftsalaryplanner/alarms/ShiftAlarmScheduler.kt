package com.vigilante.shiftsalaryplanner

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar

object ShiftAlarmScheduler {

    private const val PREFS_SCHEDULER = "shift_alarm_scheduler"
    private const val KEY_SCHEDULED_KEYS = "scheduled_keys"
    private const val KEY_LAST_MIRRORED_SYSTEM_SIGNATURE = "last_mirrored_system_signature"
    private const val SYSTEM_CLOCK_LABEL_PREFIX = "SSP"

    const val CHANNEL_ID = "shift_schedule_alarms_v3"
    const val CHANNEL_NAME = "Будильники смен"
    const val ACTION_SHIFT_ALARM = "com.vigilante.shiftsalaryplanner.action.SHIFT_ALARM"
    const val EXTRA_ALARM_KEY = "alarm_key"
    const val EXTRA_TITLE = "alarm_title"
    const val EXTRA_TEXT = "alarm_text"
    const val EXTRA_VOLUME_PERCENT = "volume_percent"
    const val EXTRA_SOUND_URI = "sound_uri"
    const val EXTRA_SOUND_LABEL = "sound_label"
    const val EXTRA_TRIGGER_AT_MILLIS = "trigger_at_millis"

    fun reschedule(
        context: Context,
        settings: ShiftAlarmSettings,
        savedDays: List<ShiftDayEntity>,
        templateMap: Map<String, ShiftTemplateEntity>,
        mirrorToSystemClockApp: Boolean = false,
        allowSystemClockUiFallback: Boolean = true
    ): ShiftAlarmRescheduleResult {
        val prefs = context.profileSharedPreferences(PREFS_SCHEDULER)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val existingKeys = prefs.getStringSet(KEY_SCHEDULED_KEYS, emptySet()).orEmpty().toSet()

        var cancelledCount = 0
        existingKeys.forEach { key ->
            cancelAlarm(context, alarmManager, key)
            cancelledCount += 1
        }

        if (!settings.enabled || !settings.autoReschedule) {
            if (mirrorToSystemClockApp && allowSystemClockUiFallback) {
                clearLastMirroredSystemAlarm(context, allowSystemClockUiFallback)
            }
            prefs.edit { putStringSet(KEY_SCHEDULED_KEYS, emptySet()) }
            return ShiftAlarmRescheduleResult(
                scheduledCount = 0,
                cancelledCount = cancelledCount,
                message = if (!settings.enabled) "Будильники смен отключены" else "Автоперестройка выключена"
            )
        }

        ensureNotificationChannel(context)

        val now = Instant.now().atZone(ZoneId.systemDefault())
        val endDate = now.toLocalDate().plusDays(settings.scheduleHorizonDays.toLong())
        val configByCode = settings.templateConfigs.associateBy { it.shiftCode }
        val newKeys = linkedSetOf<String>()
        var scheduledCount = 0
        var skippedPastCount = 0
        var skippedNoTemplateCount = 0
        var skippedNoConfigCount = 0
        var usedInexactFallback = false
        var nearestTrigger: Long? = null
        var nearestTitle: String? = null

        savedDays
            .sortedBy { it.date }
            .forEach { shiftDay ->
                val date = runCatching { LocalDate.parse(shiftDay.date) }.getOrNull() ?: return@forEach
                if (date.isBefore(now.toLocalDate()) || date.isAfter(endDate)) return@forEach

                val template = templateMap[shiftDay.shiftCode]
                if (template == null) {
                    skippedNoTemplateCount += 1
                    return@forEach
                }

                val config = configByCode[shiftDay.shiftCode]
                val matchingAlarms = config?.alarms?.filter { it.enabled }.orEmpty()
                if (config == null || !config.enabled || matchingAlarms.isEmpty()) {
                    skippedNoConfigCount += 1
                    return@forEach
                }

                val startTime = LocalTime.of(config.startHour, config.startMinute)
                val templateLabel = shiftAlarmTemplateLabel(template)

                matchingAlarms.forEach { alarm ->
                    val triggerTime = LocalTime.of(alarm.triggerHour, alarm.triggerMinute)
                    val triggerDateTime = LocalDateTime.of(date, triggerTime)
                    val triggerInstant = triggerDateTime.atZone(now.zone).toInstant()
                    if (!triggerInstant.isAfter(now.toInstant())) {
                        skippedPastCount += 1
                        return@forEach
                    }

                    val title = alarm.title.ifBlank {
                        defaultShiftAlarmTitle(templateLabel, alarm.triggerHour, alarm.triggerMinute)
                    }
                    val text = buildString {
                        append(template.title.ifBlank { template.code })
                        append(" • ")
                        append(date)
                        append(" • начало ")
                        append(formatClockHm(startTime.hour, startTime.minute))
                    }
                    val key = "${shiftDay.date}|${shiftDay.shiftCode}|${alarm.id}"
                    val triggerAtMillis = triggerInstant.toEpochMilli()
                    if (nearestTrigger == null || triggerAtMillis < nearestTrigger!!) {
                        nearestTrigger = triggerAtMillis
                        nearestTitle = title
                    }
                    val scheduledExactly = scheduleDirectAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        key = key,
                        triggerAtMillis = triggerAtMillis,
                        title = title,
                        text = text,
                        volumePercent = alarm.volumePercent,
                        soundUri = alarm.soundUri,
                        soundLabel = alarm.soundLabel
                    )
                    if (!scheduledExactly) {
                        usedInexactFallback = true
                    }
                    newKeys += key
                    scheduledCount += 1
                }
            }

        prefs.edit { putStringSet(KEY_SCHEDULED_KEYS, newKeys) }
        val mirrorResult = when {
            !mirrorToSystemClockApp -> SystemClockMirrorResult.SKIPPED
            nearestTrigger != null && !nearestTitle.isNullOrBlank() -> {
                mirrorNearestAlarmToSystemClockApp(
                    context = context,
                    triggerAtMillis = nearestTrigger!!,
                    title = nearestTitle!!,
                    allowUiFallback = allowSystemClockUiFallback
                )
            }
            else -> {
                if (allowSystemClockUiFallback) {
                    clearLastMirroredSystemAlarm(context, allowSystemClockUiFallback)
                }
                SystemClockMirrorResult.SKIPPED
            }
        }

        return ShiftAlarmRescheduleResult(
            scheduledCount = scheduledCount,
            cancelledCount = cancelledCount,
            skippedPastCount = skippedPastCount,
            skippedNoTemplateCount = skippedNoTemplateCount,
            skippedNoConfigCount = skippedNoConfigCount,
            usedInexactFallback = usedInexactFallback,
            message = buildString {
                append("Запланировано: ")
                append(scheduledCount)
                append(" • отменено старых: ")
                append(cancelledCount)
                if (skippedPastCount > 0) {
                    append(" • пропущено прошедших: ")
                    append(skippedPastCount)
                }
                if (skippedNoConfigCount > 0) {
                    append(" • без конфигурации: ")
                    append(skippedNoConfigCount)
                }
                if (usedInexactFallback) {
                    append(" • часть поставлена без точного режима")
                }
                when (mirrorResult) {
                    SystemClockMirrorResult.CREATED -> append(" • ближайший системный будильник создан")
                    SystemClockMirrorResult.ALREADY_EXISTS -> append(" • ближайший системный уже был создан")
                    SystemClockMirrorResult.CREATED_WITH_CONFIRMATION -> append(" • ближайший системный создан (через подтверждение)")
                    SystemClockMirrorResult.DEFERRED_UNTIL_DAY_BEFORE -> append(" • системные часы поддерживают автопостановку только на ближайшую неделю, дальняя дата будет выставлена позже")
                    SystemClockMirrorResult.NOT_SUPPORTED -> append(" • системные часы не поддерживают автосоздание")
                    SystemClockMirrorResult.START_FAILED -> append(" • не удалось открыть системные часы")
                    SystemClockMirrorResult.SKIPPED -> Unit
                }
            }
        )
    }

    fun scheduleSnooze(
        context: Context,
        baseAlarmKey: String,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String,
        delayMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + delayMinutes.coerceAtLeast(1) * 60_000L
        val snoozeKey = "$baseAlarmKey|snooze|$triggerAtMillis"
        scheduleDirectAlarm(
            context = context,
            alarmManager = alarmManager,
            key = snoozeKey,
            triggerAtMillis = triggerAtMillis,
            title = title,
            text = text,
            volumePercent = volumePercent,
            soundUri = soundUri,
            soundLabel = soundLabel
        )
    }

    fun ensureNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Служебные события будильников смен"
            setShowBadge(true)
            enableVibration(true)
            setBypassDnd(true)
            // Звук воспроизводим вручную в сервисе, чтобы "Системная" всегда была актуальной.
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun canScheduleExactShiftAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasFullScreenIntentPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return true
        return runCatching { notificationManager.canUseFullScreenIntent() }.getOrDefault(true)
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleDirectAlarm(
        context: Context,
        alarmManager: AlarmManager,
        key: String,
        triggerAtMillis: Long,
        title: String,
        text: String,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String
    ): Boolean {
        val pendingIntent = buildPendingIntent(
            context = context,
            key = key,
            title = title,
            text = text,
            volumePercent = volumePercent,
            soundUri = soundUri,
            soundLabel = soundLabel,
            triggerAtMillis = triggerAtMillis
        )

        val showClockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fallbackIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val resolvedShowIntent = if (showClockIntent.resolveActivity(context.packageManager) != null) {
            showClockIntent
        } else {
            fallbackIntent
        }
        val showIntent = PendingIntent.getActivity(
            context,
            requestCodeForKey("show|$key"),
            resolvedShowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduledAsAlarmClock = runCatching {
            val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        }.isSuccess

        if (scheduledAsAlarmClock) return true

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        return false
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, key: String) {
        val pendingIntent = buildPendingIntent(
            context = context,
            key = key,
            title = null,
            text = null,
            volumePercent = 100,
            soundUri = null,
            soundLabel = "",
            triggerAtMillis = 0L
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        context: Context,
        key: String,
        title: String?,
        text: String?,
        volumePercent: Int,
        soundUri: String?,
        soundLabel: String,
        triggerAtMillis: Long
    ): PendingIntent {
        val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            action = ACTION_SHIFT_ALARM
            putExtra(EXTRA_ALARM_KEY, key)
            if (title != null) putExtra(EXTRA_TITLE, title)
            if (text != null) putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_VOLUME_PERCENT, volumePercent.coerceIn(0, 100))
            if (!soundUri.isNullOrBlank()) putExtra(EXTRA_SOUND_URI, soundUri)
            if (soundLabel.isNotBlank()) putExtra(EXTRA_SOUND_LABEL, soundLabel)
            putExtra(EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForKey(key),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeForKey(key: String): Int {
        return (key.hashCode() and 0x7fffffff)
    }

    private fun mirrorNearestAlarmToSystemClockApp(
        context: Context,
        triggerAtMillis: Long,
        title: String,
        allowUiFallback: Boolean
    ): SystemClockMirrorResult {
        val zone = ZoneId.systemDefault()
        val trigger = Instant.ofEpochMilli(triggerAtMillis).atZone(zone)
        val triggerDate = trigger.toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(LocalDate.now(zone), triggerDate)
        if (daysDiff > 7) {
            if (allowUiFallback) {
                clearLastMirroredSystemAlarm(context, allowUiFallback)
            }
            return SystemClockMirrorResult.DEFERRED_UNTIL_DAY_BEFORE
        }

        val signature = "${triggerDate}|${trigger.hour}:${trigger.minute}|$title"
        val prefs = context.profileSharedPreferences(PREFS_SCHEDULER)
        val lastSignature = prefs.getString(KEY_LAST_MIRRORED_SYSTEM_SIGNATURE, null)
        if (signature == lastSignature) return SystemClockMirrorResult.ALREADY_EXISTS
        if (!lastSignature.isNullOrBlank()) {
            dismissSystemClockAlarmBySignature(context, lastSignature, allowUiFallback)
        }
        val systemClockLabel = buildSystemClockLabel(
            title = title,
            triggerDate = triggerDate,
            hour = trigger.hour,
            minute = trigger.minute
        )

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, trigger.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, trigger.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, systemClockLabel)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (daysDiff in 1..7) {
                putIntegerArrayListExtra(
                    AlarmClock.EXTRA_DAYS,
                    arrayListOf(calendarDayOfWeek(trigger.dayOfWeek.value))
                )
            }
        }

        try {
            context.startActivity(intent)
            prefs.edit { putString(KEY_LAST_MIRRORED_SYSTEM_SIGNATURE, signature) }
            return SystemClockMirrorResult.CREATED
        } catch (notFound: ActivityNotFoundException) {
            return SystemClockMirrorResult.NOT_SUPPORTED
        } catch (_: Throwable) {
            if (!allowUiFallback) {
                return SystemClockMirrorResult.START_FAILED
            }
            val intentWithUi = Intent(intent).apply {
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            return try {
                context.startActivity(intentWithUi)
                prefs.edit { putString(KEY_LAST_MIRRORED_SYSTEM_SIGNATURE, signature) }
                SystemClockMirrorResult.CREATED_WITH_CONFIRMATION
            } catch (notFound: ActivityNotFoundException) {
                SystemClockMirrorResult.NOT_SUPPORTED
            } catch (_: Throwable) {
                SystemClockMirrorResult.START_FAILED
            }
        }
    }

    private fun clearLastMirroredSystemAlarm(
        context: Context,
        allowUiFallback: Boolean
    ) {
        val prefs = context.profileSharedPreferences(PREFS_SCHEDULER)
        val lastSignature = prefs.getString(KEY_LAST_MIRRORED_SYSTEM_SIGNATURE, null)
        if (!lastSignature.isNullOrBlank()) {
            dismissSystemClockAlarmBySignature(context, lastSignature, allowUiFallback)
        }
        prefs.edit { remove(KEY_LAST_MIRRORED_SYSTEM_SIGNATURE) }
    }

    private fun dismissSystemClockAlarmBySignature(
        context: Context,
        signature: String,
        allowUiFallback: Boolean
    ): Boolean {
        val parts = signature.split('|')
        if (parts.size < 3) return false
        val triggerDate = parts[0]
        val timeParts = parts[1].split(':')
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: return false
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: return false
        val title = parts.drop(2).joinToString("|")
        val token = buildSystemClockToken(
            triggerDate = triggerDate,
            hour = hour,
            minute = minute
        )

        val dismissByLabelIntent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
            putExtra(AlarmClock.EXTRA_MESSAGE, token)
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_IS_PM, hour >= 12)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (dismissByLabelIntent.resolveActivity(context.packageManager) == null) return false

        return try {
            context.startActivity(dismissByLabelIntent)
            true
        } catch (_: Throwable) {
            if (!allowUiFallback) {
                false
            } else {
                val dismissByTimeIntent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_TIME)
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_IS_PM, hour >= 12)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (dismissByTimeIntent.resolveActivity(context.packageManager) == null) return false
                runCatching { context.startActivity(dismissByTimeIntent) }.isSuccess
            }
        }
    }

    private fun calendarDayOfWeek(dayOfWeekIso: Int): Int {
        return when (dayOfWeekIso) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            else -> Calendar.SUNDAY
        }
    }

    private fun buildSystemClockLabel(
        title: String,
        triggerDate: String,
        hour: Int,
        minute: Int
    ): String {
        val token = buildSystemClockToken(
            triggerDate = triggerDate,
            hour = hour,
            minute = minute
        )
        return "$token $title"
    }

    private fun buildSystemClockLabel(
        title: String,
        triggerDate: LocalDate,
        hour: Int,
        minute: Int
    ): String {
        return buildSystemClockLabel(
            title = title,
            triggerDate = triggerDate.toString(),
            hour = hour,
            minute = minute
        )
    }

    private fun buildSystemClockToken(
        triggerDate: String,
        hour: Int,
        minute: Int
    ): String {
        val normalizedDate = triggerDate.replace("-", "")
        return "${SYSTEM_CLOCK_LABEL_PREFIX}_${normalizedDate}_${"%02d%02d".format(hour, minute)}"
    }

    private enum class SystemClockMirrorResult {
        CREATED,
        ALREADY_EXISTS,
        CREATED_WITH_CONFIRMATION,
        DEFERRED_UNTIL_DAY_BEFORE,
        NOT_SUPPORTED,
        START_FAILED,
        SKIPPED
    }
}
