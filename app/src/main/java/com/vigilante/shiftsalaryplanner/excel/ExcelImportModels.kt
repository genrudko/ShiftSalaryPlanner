package com.vigilante.shiftsalaryplanner.excel

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.time.LocalDate

enum class ExcelImportScopeType {
    SINGLE_MONTH,
    MONTH_RANGE,
    SELECTED_MONTHS,
    FULL_YEAR
}

enum class EmptyDayImportMode {
    SKIP_EMPTY,
    FILL_AS_DAY_OFF
}

data class ExcelPersonCandidate(
    val fullName: String
)

data class ExcelImportRequest(
    val year: Int,
    val surnameQuery: String,
    val selectedFullName: String? = null,
    val scopeType: ExcelImportScopeType,
    val singleMonth: Int? = null,
    val rangeStartMonth: Int? = null,
    val rangeEndMonth: Int? = null,
    val selectedMonths: Set<Int> = emptySet(),
    val emptyDayImportMode: EmptyDayImportMode = EmptyDayImportMode.SKIP_EMPTY
)

data class ParsedExcelShiftDay(
    val date: LocalDate,
    val sourceCode: String,
    val sourceHours: Double?,
    val targetShiftCode: String,
    val targetTemplateTitle: String,
    val isAutoCreatedTemplate: Boolean
)

data class ParsedExcelPersonMonth(
    val month: Int,
    val fullName: String,
    val days: List<ParsedExcelShiftDay>
)

data class ExcelImportPreview(
    val fullName: String,
    val year: Int,
    val selectedMonths: List<Int>,
    val parsedMonths: List<ParsedExcelPersonMonth>,
    val templatesToCreate: List<ShiftTemplateEntity>,
    val importedDays: List<ParsedExcelShiftDay>
)

sealed class ExcelImportParseResult {
    data class CandidateSelectionRequired(
        val surnameQuery: String,
        val candidates: List<ExcelPersonCandidate>
    ) : ExcelImportParseResult()

    data class Preview(
        val preview: ExcelImportPreview
    ) : ExcelImportParseResult()
}
