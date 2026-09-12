package com.vigilante.shiftsalaryplanner.app.ports

import android.content.Context
import com.vigilante.shiftsalaryplanner.ShiftAlarmRescheduleResult
import com.vigilante.shiftsalaryplanner.ShiftAlarmSettings
import com.vigilante.shiftsalaryplanner.ShiftAlarmUpcomingInfo
import com.vigilante.shiftsalaryplanner.ShiftAlarmScheduler
import com.vigilante.shiftsalaryplanner.ShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AlarmDataPort {
    val settings: Flow<ShiftAlarmSettings>
    fun save(settings: ShiftAlarmSettings)
    fun synchronizeTemplates(templates: List<ShiftTemplateEntity>)
    fun upsertTemplateConfig(config: ShiftTemplateAlarmConfig)
    fun removeTemplateConfig(shiftCode: String)
}

class DefaultAlarmDataPort(
    private val store: ShiftAlarmStore
) : AlarmDataPort {
    override val settings: Flow<ShiftAlarmSettings> = store.settingsFlow
    override fun save(settings: ShiftAlarmSettings) = store.save(settings)
    override fun synchronizeTemplates(templates: List<ShiftTemplateEntity>) = store.synchronizeTemplates(templates)
    override fun upsertTemplateConfig(config: ShiftTemplateAlarmConfig) = store.upsertTemplateConfig(config)
    override fun removeTemplateConfig(shiftCode: String) = store.removeTemplateConfig(shiftCode)
}

interface AlarmPlatformPort {
    fun reschedule(
        settings: ShiftAlarmSettings,
        savedDays: List<ShiftDayEntity>,
        templateMap: Map<String, ShiftTemplateEntity>,
        mirrorToSystemClockApp: Boolean = false,
        allowSystemClockUiFallback: Boolean = true
    ): ShiftAlarmRescheduleResult

    fun previewUpcomingAlarms(
        settings: ShiftAlarmSettings,
        savedDays: List<ShiftDayEntity>,
        templateMap: Map<String, ShiftTemplateEntity>,
        limit: Int = 3
    ): List<ShiftAlarmUpcomingInfo>

    fun canScheduleExactAlarms(): Boolean
    fun hasNotificationPermission(): Boolean
    fun hasFullScreenIntentPermission(): Boolean
    fun suppressScheduledAlarm(alarmKey: String): Boolean
    fun suppressScheduledAlarms(alarmKeys: List<String>): Int
    fun clearSuppressedAlarms(): Int
    fun clearSuppressedAlarmsForDate(date: LocalDate): Int
    fun clearSuppressedAlarmsForRange(startDate: LocalDate, endDate: LocalDate): Int
}

class DefaultAlarmPlatformPort(
    private val context: Context
) : AlarmPlatformPort {
    override fun reschedule(
        settings: ShiftAlarmSettings,
        savedDays: List<ShiftDayEntity>,
        templateMap: Map<String, ShiftTemplateEntity>,
        mirrorToSystemClockApp: Boolean,
        allowSystemClockUiFallback: Boolean
    ): ShiftAlarmRescheduleResult = ShiftAlarmScheduler.reschedule(
        context = context,
        settings = settings,
        savedDays = savedDays,
        templateMap = templateMap,
        mirrorToSystemClockApp = mirrorToSystemClockApp,
        allowSystemClockUiFallback = allowSystemClockUiFallback
    )

    override fun previewUpcomingAlarms(
        settings: ShiftAlarmSettings,
        savedDays: List<ShiftDayEntity>,
        templateMap: Map<String, ShiftTemplateEntity>,
        limit: Int
    ): List<ShiftAlarmUpcomingInfo> = ShiftAlarmScheduler.previewUpcomingAlarms(
        context = context,
        settings = settings,
        savedDays = savedDays,
        templateMap = templateMap,
        limit = limit
    )

    override fun canScheduleExactAlarms() = ShiftAlarmScheduler.canScheduleExactShiftAlarms(context)
    override fun hasNotificationPermission() = ShiftAlarmScheduler.hasNotificationPermission(context)
    override fun hasFullScreenIntentPermission() = ShiftAlarmScheduler.hasFullScreenIntentPermission(context)
    override fun suppressScheduledAlarm(alarmKey: String) = ShiftAlarmScheduler.suppressScheduledAlarm(context, alarmKey)
    override fun suppressScheduledAlarms(alarmKeys: List<String>) = ShiftAlarmScheduler.suppressScheduledAlarms(context, alarmKeys)
    override fun clearSuppressedAlarms() = ShiftAlarmScheduler.clearSuppressedAlarms(context)
    override fun clearSuppressedAlarmsForDate(date: LocalDate) = ShiftAlarmScheduler.clearSuppressedAlarmsForDate(context, date)
    override fun clearSuppressedAlarmsForRange(startDate: LocalDate, endDate: LocalDate) =
        ShiftAlarmScheduler.clearSuppressedAlarmsForRange(context, startDate, endDate)
}
