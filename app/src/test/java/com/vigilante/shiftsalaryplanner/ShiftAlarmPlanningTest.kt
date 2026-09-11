package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ShiftAlarmPlanningTest {
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 9, 11, 8, 0, 0, 0, zone)

    @Test
    fun planner_disabledSchedulerOrAutoReschedule_returnsNoPlans() {
        val day = day("2026-09-12", "D")
        val templates = mapOf("D" to template("D"))
        val config = config("D", alarm("a1", 7, 0))

        assertTrue(
            planUpcomingShiftAlarms(
                settings = ShiftAlarmSettings(enabled = false, templateConfigs = listOf(config)),
                savedDays = listOf(day),
                templateMap = templates,
                suppressedKeys = emptySet(),
                limit = 3,
                now = now
            ).isEmpty()
        )
        assertTrue(
            planUpcomingShiftAlarms(
                settings = ShiftAlarmSettings(enabled = true, autoReschedule = false, templateConfigs = listOf(config)),
                savedDays = listOf(day),
                templateMap = templates,
                suppressedKeys = emptySet(),
                limit = 3,
                now = now
            ).isEmpty()
        )
    }

    @Test
    fun planner_excludesPastAndOutsideHorizon() {
        val templates = mapOf("D" to template("D"))
        val config = config("D", alarm("a1", 7, 0))
        val plans = planUpcomingShiftAlarms(
            settings = ShiftAlarmSettings(
                enabled = true,
                autoReschedule = true,
                scheduleHorizonDays = 2,
                templateConfigs = listOf(config)
            ),
            savedDays = listOf(
                day("2026-09-11", "D"),
                day("2026-09-12", "D"),
                day("2026-09-14", "D")
            ),
            templateMap = templates,
            suppressedKeys = emptySet(),
            limit = 10,
            now = now
        )

        assertEquals(listOf("2026-09-12|D|a1"), plans.map { it.alarmKey })
    }

    @Test
    fun planner_excludesMissingTemplateDisabledConfigAndDisabledAlarm() {
        val plans = planUpcomingShiftAlarms(
            settings = ShiftAlarmSettings(
                enabled = true,
                scheduleHorizonDays = 3,
                templateConfigs = listOf(
                    config("DISABLED_CONFIG", alarm("a1", 9, 0), enabled = false),
                    config("DISABLED_ALARM", alarm("a2", 9, 0, enabled = false))
                )
            ),
            savedDays = listOf(
                day("2026-09-12", "MISSING_TEMPLATE"),
                day("2026-09-12", "DISABLED_CONFIG"),
                day("2026-09-12", "DISABLED_ALARM")
            ),
            templateMap = mapOf(
                "DISABLED_CONFIG" to template("DISABLED_CONFIG"),
                "DISABLED_ALARM" to template("DISABLED_ALARM")
            ),
            suppressedKeys = emptySet(),
            limit = 10,
            now = now
        )

        assertTrue(plans.isEmpty())
    }

    @Test
    fun planner_suppressionExcludesOnlyMatchingAlarm() {
        val plans = planUpcomingShiftAlarms(
            settings = ShiftAlarmSettings(
                enabled = true,
                scheduleHorizonDays = 3,
                templateConfigs = listOf(
                    config("D", alarm("early", 7, 0), alarm("late", 8, 0))
                )
            ),
            savedDays = listOf(day("2026-09-12", "D")),
            templateMap = mapOf("D" to template("D")),
            suppressedKeys = setOf("2026-09-12|D|early"),
            limit = 10,
            now = now
        )

        assertEquals(listOf("2026-09-12|D|late"), plans.map { it.alarmKey })
    }

    @Test
    fun planner_ordersChronologicallyHonorsLimitAndKeepsStableKey() {
        val plans = planUpcomingShiftAlarms(
            settings = ShiftAlarmSettings(
                enabled = true,
                scheduleHorizonDays = 3,
                templateConfigs = listOf(
                    config("D", alarm("late", 9, 30), alarm("early", 6, 15))
                )
            ),
            savedDays = listOf(
                day("2026-09-13", "D"),
                day("2026-09-12", "D")
            ),
            templateMap = mapOf("D" to template("D")),
            suppressedKeys = emptySet(),
            limit = 3,
            now = now
        )

        assertEquals(
            listOf(
                "2026-09-12|D|early",
                "2026-09-12|D|late",
                "2026-09-13|D|early"
            ),
            plans.map { it.alarmKey }
        )
        assertTrue(plans.zipWithNext().all { (a, b) -> a.triggerAtMillis <= b.triggerAtMillis })
    }

    private fun day(date: String, code: String) = ShiftDayEntity(date = date, shiftCode = code)

    private fun template(code: String) = ShiftTemplateEntity(
        code = code,
        title = "Shift $code",
        iconKey = "TEXT",
        totalHours = 12.0,
        breakHours = 0.5,
        nightHours = 0.0,
        colorHex = "#123456",
        isWeekendPaid = false,
        active = true,
        sortOrder = 0
    )

    private fun alarm(id: String, hour: Int, minute: Int, enabled: Boolean = true) = ShiftAlarmConfig(
        id = id,
        triggerHour = hour,
        triggerMinute = minute,
        enabled = enabled
    )

    private fun config(
        code: String,
        vararg alarms: ShiftAlarmConfig,
        enabled: Boolean = true
    ) = ShiftTemplateAlarmConfig(
        shiftCode = code,
        enabled = enabled,
        startHour = 8,
        startMinute = 0,
        endHour = 20,
        endMinute = 0,
        alarms = alarms.toList()
    )
}
