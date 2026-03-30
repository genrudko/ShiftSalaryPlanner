package com.vigilante.shiftsalaryplanner.excel

import com.vigilante.shiftsalaryplanner.data.ShiftDayDao
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateDao
import java.time.LocalDate

class ExcelScheduleImporter(
    private val shiftTemplateDao: ShiftTemplateDao,
    private val shiftDayDao: ShiftDayDao
) {

    suspend fun import(preview: ExcelImportPreview) {
        preview.templatesToCreate.forEach { template ->
            shiftTemplateDao.upsert(template)
        }

        preview.importedDays
            .map { it.date }
            .distinct()
            .forEach { date ->
                shiftDayDao.deleteByDate(date.toString())
            }

        preview.importedDays
            .sortedBy { it.date }
            .forEach { day ->
                shiftDayDao.upsert(
                    ShiftDayEntity(
                        date = day.date.toString(),
                        shiftCode = day.targetShiftCode
                    )
                )
            }
    }

    suspend fun clearPeriod(startDate: LocalDate, endDate: LocalDate) {
        var current = startDate
        while (!current.isAfter(endDate)) {
            shiftDayDao.deleteByDate(current.toString())
            current = current.plusDays(1)
        }
    }
}
