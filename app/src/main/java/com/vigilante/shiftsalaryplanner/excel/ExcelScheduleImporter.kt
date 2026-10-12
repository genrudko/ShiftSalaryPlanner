package com.vigilante.shiftsalaryplanner.excel

import androidx.room.withTransaction
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import java.time.LocalDate

class ExcelScheduleImporter(
    private val database: AppDatabase,
) {
    private val shiftTemplateDao = database.shiftTemplateDao()
    private val shiftDayDao = database.shiftDayDao()

    suspend fun import(preview: ExcelImportPreview) {
        validateExcelImportPreview(preview)
        // Removing the old period and inserting the imported days must be atomic.
        database.withTransaction {
            preview.selectedMonths.sorted().forEach { month ->
                val from = LocalDate.of(preview.year, month, 1)
                val to = from.withDayOfMonth(from.lengthOfMonth())
                shiftDayDao.deleteByDateRange(from.toString(), to.toString())
            }
            preview.templatesToCreate.forEach { shiftTemplateDao.upsert(it) }
            preview.importedDays.sortedBy { it.date }.forEach { day ->
                shiftDayDao.upsert(ShiftDayEntity(date = day.date.toString(), shiftCode = day.targetShiftCode))
            }
        }
    }
}

internal fun validateExcelImportPreview(preview: ExcelImportPreview) {
    require(preview.year in 1900..2100) { "Неверный год табеля" }
    require(preview.selectedMonths.isNotEmpty() &&
        preview.selectedMonths.distinct().size == preview.selectedMonths.size &&
        preview.selectedMonths.all { it in 1..12 }) { "Некорректный список месяцев" }
    require(preview.importedDays.isNotEmpty()) { "Табель не содержит смен для импорта" }
    require(preview.importedDays.all {
        it.date.year == preview.year &&
            it.date.monthValue in preview.selectedMonths &&
            it.targetShiftCode.isNotBlank()
    }) { "Дни табеля не соответствуют выбранному периоду" }
    require(preview.importedDays.map { it.date }.distinct().size == preview.importedDays.size) {
        "Табель содержит повторяющиеся даты"
    }
    require(preview.templatesToCreate.all { it.code.isNotBlank() } &&
        preview.templatesToCreate.map { it.code }.distinct().size == preview.templatesToCreate.size) {
        "Некорректные шаблоны смен из табеля"
    }
}
