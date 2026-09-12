package com.vigilante.shiftsalaryplanner.app.ports

import com.vigilante.shiftsalaryplanner.data.HolidayDao
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftDayDao
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateDao
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsState
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsStore
import com.vigilante.shiftsalaryplanner.settings.WorkplaceDateShiftAssignment
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DefaultScheduleDataPort(
    private val shiftDayDao: ShiftDayDao,
    private val shiftTemplateDao: ShiftTemplateDao,
    private val holidayDao: HolidayDao,
    private val workAssignmentsStore: WorkAssignmentsStore
) : ScheduleDataPort {
    override val shiftDays: Flow<List<ShiftDayEntity>> = shiftDayDao.observeAll()
    override val shiftTemplates: Flow<List<ShiftTemplateEntity>> = shiftTemplateDao.observeAll()
    override val holidays: Flow<List<HolidayEntity>> = holidayDao.observeByScope("RU-FED")
    override val workAssignments: Flow<WorkAssignmentsState> = workAssignmentsStore.stateFlow
    override suspend fun upsertShiftDay(item: ShiftDayEntity) = shiftDayDao.upsert(item)
    override suspend fun deleteShiftDay(date: String) = shiftDayDao.deleteByDate(date)
    override suspend fun deleteShiftDays(startDate: String, endDate: String) = shiftDayDao.deleteByDateRange(startDate, endDate)
    override suspend fun upsertShiftTemplate(item: ShiftTemplateEntity) = shiftTemplateDao.upsert(item)
    override suspend fun upsertShiftTemplates(items: List<ShiftTemplateEntity>) = shiftTemplateDao.upsertAll(items)
    override suspend fun deleteShiftTemplate(item: ShiftTemplateEntity) = shiftTemplateDao.delete(item)
    override suspend fun upsertHolidays(items: List<HolidayEntity>) = holidayDao.upsertAll(items)
    override fun setWorkplaceShift(workplaceId: String, date: LocalDate, shiftCode: String?) = workAssignmentsStore.setShiftForDate(workplaceId, date, shiftCode)
    override fun clearWorkplaceAssignments(startDate: LocalDate, endDate: LocalDate) = workAssignmentsStore.clearDateRange(startDate, endDate)
    override fun renameWorkplace(workplaceId: String, newName: String): Boolean = workAssignmentsStore.renameWorkplace(workplaceId, newName)
    override fun replaceShiftCode(oldShiftCode: String, newShiftCode: String) = workAssignmentsStore.replaceShiftCode(oldShiftCode, newShiftCode)
    override fun removeShiftCode(shiftCode: String): List<WorkplaceDateShiftAssignment> = workAssignmentsStore.removeShiftCode(shiftCode)
    override fun restoreAssignments(assignments: List<WorkplaceDateShiftAssignment>) = workAssignmentsStore.restoreAssignments(assignments)
}
