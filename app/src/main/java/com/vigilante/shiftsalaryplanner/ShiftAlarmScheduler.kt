package com.vigilante.shiftsalaryplanner

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ShiftAlarmScheduler {

    private const val PREFS_SCHEDULER = "shift_alarm_scheduler"
    private const val KEY_SCHEDULED_KEYS = "scheduled_keys"

    const val CHANNEL_ID = "shift_schedule_alarms"
    const val CHANNEL_NAME = "Будильники смен"
    const val ACTION_SHIFT_ALARM = "com.vigilante.shiftsalaryplanner.action.SHIFT_ALARM"
    const val EXTRA_ALARM_KEY = "alarm_key"
    const val EXTRA_TITLE = "alarm_title"
    const val EXTRA_TEXT = "alarm_text"

    fun reschedule(
        context: Context,
        settings: ShiftAlarmSettings,
        savedDays: List<ShiftDayEntity>,
        templateMap: Map<String, ShiftTemplateEntity>
    ): ShiftAlarmRescheduleResult {
        val prefs = context.getSharedPreferences(PREFS_SCHEDULER, Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val existingKeys = prefs.getStringSet(KEY_SCHEDULED_KEYS, emptySet()).orEmpty().toSet()

        var cancelledCount = 0
        existingKeys.forEach { key ->
            cancelAlarm(context, alarmManager, key)
            cancelledCount += 1
        }

        if (!settings.enabled || !settings.autoReschedule) {
            prefs.edit().putStringSet(KEY_SCHEDULED_KEYS, emptySet()).apply()
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
                val shiftStart = LocalDateTime.of(date, startTime)
                val templateLabel = shiftAlarmTemplateLabel(template)

                matchingAlarms.forEach { alarm ->
                    val triggerDateTime = shiftStart.minusMinutes(alarm.triggerMinutesBefore.toLong())
                    val triggerInstant = triggerDateTime.atZone(now.zone).toInstant()
                    if (!triggerInstant.isAfter(now.toInstant())) {
                        skippedPastCount += 1
                        return@forEach
                    }

                    val title = alarm.title.ifBlank {
                        defaultShiftAlarmTitle(templateLabel, alarm.triggerMinutesBefore)
                    }
                    val text = buildString {
                        append(template.title.ifBlank { template.code })
                        append(" • ")
                        append(date)
                        append(" • начало ")
                        append(formatClockHm(startTime.hour, startTime.minute))
                    }
                    val key = "${shiftDay.date}|${shiftDay.shiftCode}|${alarm.id}"
                    val pendingIntent = buildPendingIntent(context, key, title, text)
                    val scheduledExactly = scheduleAlarm(
                        alarmManager = alarmManager,
                        triggerAtMillis = triggerInstant.toEpochMilli(),
                        pendingIntent = pendingIntent,
                        context = context
                    )
                    if (!scheduledExactly) {
                        usedInexactFallback = true
                    }
                    newKeys += key
                    scheduledCount += 1
                }
            }

        prefs.edit().putStringSet(KEY_SCHEDULED_KEYS, newKeys).apply()

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
            }
        )
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Напоминания о сменах"
            setShowBadge(true)
            enableVibration(true)
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

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        context: Context
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactShiftAlarms(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            true
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, key: String) {
        val pendingIntent = buildPendingIntent(context, key, title = null, text = null)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        context: Context,
        key: String,
        title: String?,
        text: String?
    ): PendingIntent {
        val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            action = ACTION_SHIFT_ALARM
            putExtra(EXTRA_ALARM_KEY, key)
            if (title != null) putExtra(EXTRA_TITLE, title)
            if (text != null) putExtra(EXTRA_TEXT, text)
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
}
