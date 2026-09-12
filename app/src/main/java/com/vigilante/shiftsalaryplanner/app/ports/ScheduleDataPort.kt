package com.vigilante.shiftsalaryplanner.app.ports

import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsState
import com.vigilante.shiftsalaryplanner.settings.WorkplaceDateShiftAssignment
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ScheduleDataPort {
    val shiftDays: Flow<List<ShiftDayEntity>>
    val shiftTemplates: Flow<List<ShiftTemplateEntity>>
    val holidays: Flow<List<HolidayEntity>>
    val workAssignments: Flow<WorkAssignmentsState>
    suspend fun upsertShiftDay(item: ShiftDayEntity)
    suspend fun deleteShiftDay(date: String)
    suspend fun deleteShiftDays(startDate: String, endDate: String)
    suspend fun upsertShiftTemplate(item: ShiftTemplateEntity)
    suspend fun upsertShiftTemplates(items: List<ShiftTemplateEntity>)
    suspend fun deleteShiftTemplate(item: ShiftTemplateEntity)
    suspend fun upsertHolidays(items: List<HolidayEntity>)
    fun setWorkplaceShift(workplaceId: String, date: LocalDate, shiftCode: String?)
    fun clearWorkplaceAssignments(startDate: LocalDate, endDate: LocalDate)
    fun renameWorkplace(workplaceId: String, newName: String): Boolean
    fun replaceShiftCode(oldShiftCode: String, newShiftCode: String)
    fun removeShiftCode(shiftCode: String): List<WorkplaceDateShiftAssignment>
    fun restoreAssignments(assignments: List<WorkplaceDateShiftAssignment>)
}
