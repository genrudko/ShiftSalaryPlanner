package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.app.ports.ScheduleDataPort
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsState
import com.vigilante.shiftsalaryplanner.settings.WorkplaceDateShiftAssignment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ScheduleDataPortTest {

    @Test
    fun featureCanUseFakeSchedulePortWithoutConcreteStorage() = runBlocking {
        val port = FakeScheduleDataPort()
        val date = LocalDate.of(2026, 9, 12)

        port.deleteShiftDay(date.toString())
        port.deleteShiftDays(date.toString(), date.plusDays(2).toString())
        port.setWorkplaceShift("work-2", date, "D")
        port.clearWorkplaceAssignments(date, date.plusDays(2))
        port.clearAllShiftDays()
        port.clearAllWorkplaceAssignments()
        val renamed = port.renameWorkplace("work-2", "Site B")
        port.replaceShiftCode("D", "DAY")
        val removed = port.removeShiftCode("DAY")
        port.restoreAssignments(emptyList())

        assertEquals(true, renamed)
        assertEquals(emptyList<WorkplaceDateShiftAssignment>(), removed)
        assertEquals(
            listOf(
                "deleteDay:2026-09-12",
                "deleteRange:2026-09-12:2026-09-14",
                "setWorkplace:work-2:2026-09-12:D",
                "clearWorkplace:2026-09-12:2026-09-14",
                "clearAllDays",
                "clearAllWorkplace",
                "renameWorkplace:work-2:Site B",
                "replaceShift:D:DAY",
                "removeShift:DAY",
                "restoreAssignments:0"
            ),
            port.calls
        )
    }

    private class FakeScheduleDataPort : ScheduleDataPort {
        override val shiftDays: Flow<List<ShiftDayEntity>> = MutableStateFlow(emptyList())
        override val shiftTemplates: Flow<List<ShiftTemplateEntity>> = MutableStateFlow(emptyList())
        override val holidays: Flow<List<HolidayEntity>> = MutableStateFlow(emptyList())
        override val workAssignments: Flow<WorkAssignmentsState> = MutableStateFlow(WorkAssignmentsState(workplaces = emptyList(), extraAssignmentsByDate = emptyMap()))

        val calls = mutableListOf<String>()

        override suspend fun upsertShiftDay(item: ShiftDayEntity) {
            calls += "upsertDay:${item.date}"
        }

        override suspend fun deleteShiftDay(date: String) {
            calls += "deleteDay:$date"
        }

        override suspend fun deleteShiftDays(startDate: String, endDate: String) {
            calls += "deleteRange:$startDate:$endDate"
        }

        override suspend fun clearAllShiftDays() {
            calls += "clearAllDays"
        }

        override suspend fun upsertShiftTemplate(item: ShiftTemplateEntity) {
            calls += "upsertTemplate:${item.code}"
        }

        override suspend fun upsertShiftTemplates(items: List<ShiftTemplateEntity>) {
            calls += "upsertTemplates:${items.size}"
        }

        override suspend fun deleteShiftTemplate(item: ShiftTemplateEntity) {
            calls += "deleteTemplate:${item.code}"
        }

        override suspend fun upsertHolidays(items: List<HolidayEntity>) {
            calls += "upsertHolidays:${items.size}"
        }

        override fun setWorkplaceShift(workplaceId: String, date: LocalDate, shiftCode: String?) {
            calls += "setWorkplace:$workplaceId:$date:$shiftCode"
        }

        override fun clearWorkplaceAssignments(startDate: LocalDate, endDate: LocalDate) {
            calls += "clearWorkplace:$startDate:$endDate"
        }

        override fun clearAllWorkplaceAssignments() {
            calls += "clearAllWorkplace"
        }

        override fun renameWorkplace(workplaceId: String, newName: String): Boolean {
            calls += "renameWorkplace:$workplaceId:$newName"
            return true
        }

        override fun replaceShiftCode(oldShiftCode: String, newShiftCode: String) {
            calls += "replaceShift:$oldShiftCode:$newShiftCode"
        }

        override fun removeShiftCode(shiftCode: String): List<WorkplaceDateShiftAssignment> {
            calls += "removeShift:$shiftCode"
            return emptyList()
        }

        override fun restoreAssignments(assignments: List<WorkplaceDateShiftAssignment>) {
            calls += "restoreAssignments:${assignments.size}"
        }
    }
}
