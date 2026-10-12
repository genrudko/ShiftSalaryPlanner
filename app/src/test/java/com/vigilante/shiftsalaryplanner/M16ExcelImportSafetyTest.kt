package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ParsedExcelShiftDay
import com.vigilante.shiftsalaryplanner.excel.validateExcelImportPreview
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class M16ExcelImportSafetyTest {
    private fun preview(days: List<ParsedExcelShiftDay>, months: List<Int> = listOf(10)): ExcelImportPreview =
        ExcelImportPreview(
            fullName = "Test", year = 2026, selectedMonths = months,
            parsedMonths = emptyList(), templatesToCreate = emptyList(), importedDays = days,
        )

    private fun day(date: String, code: String = "Д"): ParsedExcelShiftDay =
        ParsedExcelShiftDay(LocalDate.parse(date), "Д", 12.0, code, "Day", false)

    @Test fun rejectsEmptyPreviewBeforePeriodDeletion() {
        assertTrue(runCatching { validateExcelImportPreview(preview(emptyList())) }.isFailure)
    }

    @Test fun rejectsDatesOutsideSelectedMonthsBeforePeriodDeletion() {
        assertTrue(runCatching { validateExcelImportPreview(preview(listOf(day("2026-11-01")))) }.isFailure)
    }

    @Test fun rejectsDuplicateDayAndBlankShiftCode() {
        assertTrue(runCatching { validateExcelImportPreview(preview(listOf(day("2026-10-01"), day("2026-10-01")))) }.isFailure)
        assertTrue(runCatching { validateExcelImportPreview(preview(listOf(day("2026-10-01", "")))) }.isFailure)
    }

    @Test fun acceptsValidMultiMonthPreview() {
        validateExcelImportPreview(preview(listOf(day("2026-10-01"), day("2026-11-01")), listOf(10, 11)))
    }
}
