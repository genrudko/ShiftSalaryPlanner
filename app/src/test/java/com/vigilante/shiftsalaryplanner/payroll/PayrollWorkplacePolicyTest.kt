package com.vigilante.shiftsalaryplanner.payroll

import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PayrollWorkplacePolicyTest {
    private val start = LocalDate.of(2026, 6, 1)
    private val end = LocalDate.of(2026, 6, 30)

    @Test
    fun explicitWorkplaceAlwaysWins() {
        val result = resolvePayrollSettingsWorkplaceId(
            selectedWorkplaceId = "work_second",
            allWorkplacesId = "__all_workplaces__",
            periodStart = start,
            periodEnd = end,
            savedDays = listOf(ShiftDayEntity("2026-06-03", "D")),
            extraAssignmentsByDate = emptyMap(),
            systemStatusCodes = emptySet()
        )
        assertEquals("work_second", result)
    }

    @Test
    fun allFilterWithOnlyOneEffectiveWorkplaceUsesItsSettings() {
        val result = resolvePayrollSettingsWorkplaceId(
            selectedWorkplaceId = "__all_workplaces__",
            allWorkplacesId = "__all_workplaces__",
            periodStart = start,
            periodEnd = end,
            savedDays = emptyList(),
            extraAssignmentsByDate = mapOf(
                LocalDate.of(2026, 6, 4) to mapOf("work_second" to "wp:work_second::D"),
                LocalDate.of(2026, 6, 7) to mapOf("work_second" to "wp:work_second::N")
            ),
            systemStatusCodes = emptySet()
        )
        assertEquals("work_second", result)
    }

    @Test
    fun allFilterWithMultipleWorkplacesFallsBackToAllSettings() {
        val result = resolvePayrollSettingsWorkplaceId(
            selectedWorkplaceId = "__all_workplaces__",
            allWorkplacesId = "__all_workplaces__",
            periodStart = start,
            periodEnd = end,
            savedDays = listOf(ShiftDayEntity("2026-06-03", "D")),
            extraAssignmentsByDate = mapOf(
                LocalDate.of(2026, 6, 4) to mapOf("work_second" to "wp:work_second::D")
            ),
            systemStatusCodes = emptySet()
        )
        assertEquals("__all_workplaces__", result)
    }

    @Test
    fun systemStatusesAndOutOfPeriodRowsDoNotSelectAWorkplace() {
        val result = resolvePayrollSettingsWorkplaceId(
            selectedWorkplaceId = "__all_workplaces__",
            allWorkplacesId = "__all_workplaces__",
            periodStart = start,
            periodEnd = end,
            savedDays = listOf(
                ShiftDayEntity("2026-06-03", "OT"),
                ShiftDayEntity("2026-07-03", "D")
            ),
            extraAssignmentsByDate = mapOf(
                LocalDate.of(2026, 6, 4) to mapOf("work_second" to "wp:work_second::OT")
            ),
            systemStatusCodes = setOf("OT")
        )
        assertEquals("__all_workplaces__", result)
    }
}
