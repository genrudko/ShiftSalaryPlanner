package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.app.ports.AlarmDataPort
import com.vigilante.shiftsalaryplanner.app.ports.AlarmPlatformPort
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AlarmPortsTest {
    @Test
    fun featureCanUseFakeAlarmPortsWithoutConcreteStoreOrScheduler() {
        val data = FakeAlarmDataPort()
        val platform = FakeAlarmPlatformPort()
        val settings = ShiftAlarmSettings(enabled = true)

        data.save(settings)
        data.removeTemplateConfig("D")
        val result = platform.reschedule(settings, emptyList(), emptyMap(), false, false)
        val upcoming = platform.previewUpcomingAlarms(settings, emptyList(), emptyMap(), 10)
        val exact = platform.canScheduleExactAlarms()
        val notification = platform.hasNotificationPermission()
        val fullScreen = platform.hasFullScreenIntentPermission()
        val suppressed = platform.suppressScheduledAlarm("key")
        val cleared = platform.clearSuppressedAlarmsForDate(LocalDate.of(2026, 9, 12))

        assertEquals(listOf("save:true", "remove:D"), data.calls)
        assertEquals("fake", result.message)
        assertEquals(emptyList<ShiftAlarmUpcomingInfo>(), upcoming)
        assertTrue(exact)
        assertTrue(notification)
        assertFalse(fullScreen)
        assertTrue(suppressed)
        assertEquals(1, cleared)
    }

    private class FakeAlarmDataPort : AlarmDataPort {
        override val settings: Flow<ShiftAlarmSettings> = MutableStateFlow(ShiftAlarmSettings())
        val calls = mutableListOf<String>()
        override fun save(settings: ShiftAlarmSettings) { calls += "save:${settings.enabled}" }
        override fun synchronizeTemplates(templates: List<ShiftTemplateEntity>) { calls += "sync:${templates.size}" }
        override fun upsertTemplateConfig(config: ShiftTemplateAlarmConfig) { calls += "upsert:${config.shiftCode}" }
        override fun removeTemplateConfig(shiftCode: String) { calls += "remove:$shiftCode" }
    }

    private class FakeAlarmPlatformPort : AlarmPlatformPort {
        override fun reschedule(
            settings: ShiftAlarmSettings,
            savedDays: List<ShiftDayEntity>,
            templateMap: Map<String, ShiftTemplateEntity>,
            mirrorToSystemClockApp: Boolean,
            allowSystemClockUiFallback: Boolean
        ) = ShiftAlarmRescheduleResult(scheduledCount = 0, cancelledCount = 0, message = "fake")
        override fun previewUpcomingAlarms(
            settings: ShiftAlarmSettings,
            savedDays: List<ShiftDayEntity>,
            templateMap: Map<String, ShiftTemplateEntity>,
            limit: Int
        ): List<ShiftAlarmUpcomingInfo> = emptyList()
        override fun canScheduleExactAlarms() = true
        override fun hasNotificationPermission() = true
        override fun hasFullScreenIntentPermission() = false
        override fun suppressScheduledAlarm(alarmKey: String) = true
        override fun suppressScheduledAlarms(alarmKeys: List<String>) = alarmKeys.size
        override fun clearSuppressedAlarms() = 1
        override fun clearSuppressedAlarmsForDate(date: LocalDate) = 1
        override fun clearSuppressedAlarmsForRange(startDate: LocalDate, endDate: LocalDate) = 2
    }
}
