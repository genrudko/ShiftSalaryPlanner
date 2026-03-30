package com.vigilante.shiftsalaryplanner.excel

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.time.LocalDate
import java.util.Locale
import kotlin.math.round

@Suppress("DEPRECATION")
class ExcelScheduleParser {


    fun parse(
        inputStream: InputStream,
        request: ExcelImportRequest,
        existingTemplates: List<ShiftTemplateEntity>
    ): ExcelImportParseResult {
        val workbook = WorkbookFactory.create(inputStream)
        workbook.use { wb ->
            val monthSheets = wb.sheetIterator().asSequence()
                .mapNotNull { sheet -> sheet.toMonthSheetSpecOrNull() }
                .toList()

            if (monthSheets.isEmpty()) {
                throw IllegalStateException("В файле не найдены месячные листы табеля")
            }

            val requestedMonths = resolveRequestedMonths(request)
            val filteredSheets = monthSheets.filter { it.month in requestedMonths }
            if (filteredSheets.isEmpty()) {
                throw IllegalStateException("В файле нет листов для выбранного периода")
            }

            val matchedNames = filteredSheets
                .flatMap { spec -> findCandidatesOnSheet(spec.sheet, request.surnameQuery) }
                .distinctBy { normalizeName(it) }
                .sortedBy { it }

            if (matchedNames.isEmpty()) {
                throw IllegalStateException("Сотрудник с фамилией '${request.surnameQuery}' не найден")
            }

            val resolvedFullName = when {
                request.selectedFullName.isNullOrBlank() && matchedNames.size > 1 -> {
                    return ExcelImportParseResult.CandidateSelectionRequired(
                        surnameQuery = request.surnameQuery,
                        candidates = matchedNames.map { ExcelPersonCandidate(it) }
                    )
                }

                request.selectedFullName.isNullOrBlank() -> matchedNames.first()
                else -> matchedNames.firstOrNull { normalizeName(it) == normalizeName(request.selectedFullName) }
                    ?: throw IllegalStateException("Выбранный сотрудник '${request.selectedFullName}' не найден в файле")
            }

            val parsedMonths = filteredSheets.map { spec ->
                parseMonthSheet(
                    sheet = spec.sheet,
                    month = spec.month,
                    year = request.year,
                    fullName = resolvedFullName,
                    emptyDayImportMode = request.emptyDayImportMode
                )
            }

            val importedDays = parsedMonths.flatMap { it.days }.sortedBy { it.date }
            if (importedDays.isEmpty()) {
                throw IllegalStateException("Для сотрудника '$resolvedFullName' не найдено ни одной смены для импорта")
            }

            val existingByCode = existingTemplates.associateBy { it.code }
            val templatesToCreate = importedDays
                .mapNotNull { day -> requiredTemplateFor(day.targetShiftCode, day.targetTemplateTitle, day.sourceCode, day.sourceHours) }
                .distinctBy { it.code }
                .filter { it.code !in existingByCode }
                .sortedBy { it.sortOrder }

            return ExcelImportParseResult.Preview(
                ExcelImportPreview(
                    fullName = resolvedFullName,
                    year = request.year,
                    selectedMonths = requestedMonths.sorted(),
                    parsedMonths = parsedMonths,
                    templatesToCreate = templatesToCreate,
                    importedDays = importedDays
                )
            )
        }
    }

    private fun parseMonthSheet(
        sheet: Sheet,
        month: Int,
        year: Int,
        fullName: String,
        emptyDayImportMode: EmptyDayImportMode
    ): ParsedExcelPersonMonth {
        val dayColumns = resolveDayColumns(sheet)
        val nameRowIndex = findNameRowIndex(sheet, fullName)
            ?: throw IllegalStateException("На листе '${sheet.sheetName}' сотрудник '$fullName' не найден")
        val rowPair = resolveEmployeeRowPair(sheet, nameRowIndex, dayColumns)

        val parsedDays = mutableListOf<ParsedExcelShiftDay>()

        for ((columnIndex, dayOfMonth) in dayColumns) {
            val rawCode = sheet.getRow(rowPair.codeRowIndex)?.getCell(columnIndex)?.readTrimmedString()
            val normalizedCode = normalizeCode(rawCode)
            val hours = sheet.getRow(rowPair.hourRowIndex)?.getCell(columnIndex)?.readNullableDouble()

            if (normalizedCode.isNullOrBlank()) {
                if (emptyDayImportMode == EmptyDayImportMode.FILL_AS_DAY_OFF) {
                    val date = LocalDate.of(year, month, dayOfMonth)
                    parsedDays += ParsedExcelShiftDay(
                        date = date,
                        sourceCode = "",
                        sourceHours = hours,
                        targetShiftCode = "ВЫХ",
                        targetTemplateTitle = "Выходной",
                        isAutoCreatedTemplate = false
                    )
                }
                continue
            }

            val mapping = mapSourceToTarget(normalizedCode, hours)
            val date = LocalDate.of(year, month, dayOfMonth)
            parsedDays += ParsedExcelShiftDay(
                date = date,
                sourceCode = normalizedCode,
                sourceHours = hours,
                targetShiftCode = mapping.first,
                targetTemplateTitle = mapping.second,
                isAutoCreatedTemplate = requiredTemplateFor(mapping.first, mapping.second, normalizedCode, hours) != null
            )
        }

        return ParsedExcelPersonMonth(
            month = month,
            fullName = fullName,
            days = parsedDays
        )
    }

    private fun findCandidatesOnSheet(sheet: Sheet, surnameQuery: String): List<String> {
        val normalizedSurname = normalizeSearchToken(surnameQuery)
        val candidates = mutableListOf<String>()
        val lastRow = sheet.lastRowNum
        for (rowIndex in 0..lastRow) {
            val fullName = sheet.getRow(rowIndex)?.getCell(1)?.readTrimmedString().orEmpty()
            if (fullName.isBlank()) continue
            val surname = fullName.substringBefore(' ').trim()
            if (normalizeSearchToken(surname).startsWith(normalizedSurname)) {
                candidates += fullName
            }
        }
        return candidates.distinct()
    }

    private fun findNameRowIndex(sheet: Sheet, fullName: String): Int? {
        val target = normalizeName(fullName)
        val lastRow = sheet.lastRowNum
        for (rowIndex in 0..lastRow) {
            val candidate = sheet.getRow(rowIndex)?.getCell(1)?.readTrimmedString().orEmpty()
            if (candidate.isNotBlank() && normalizeName(candidate) == target) {
                return rowIndex
            }
        }
        return null
    }

    private fun resolveEmployeeRowPair(
        sheet: Sheet,
        nameRowIndex: Int,
        dayColumns: List<Pair<Int, Int>>
    ): EmployeeRowPair {
        val candidateIndexes = ((nameRowIndex - 1)..(nameRowIndex + 2))
            .filter { it >= 0 && it <= sheet.lastRowNum }
            .distinct()

        val rowStats = candidateIndexes.map { rowIndex ->
            val row = sheet.getRow(rowIndex)
            RowStats(
                rowIndex = rowIndex,
                codeLikeCells = countCodeLikeCells(row, dayColumns),
                numericCells = countNumericCells(row, dayColumns)
            )
        }

        val codeRow = rowStats
            .maxWithOrNull(compareBy<RowStats> { it.codeLikeCells }.thenBy { it.numericCells })
            ?.takeIf { it.codeLikeCells > 0 }
            ?: throw IllegalStateException("На листе '${sheet.sheetName}' не удалось определить строку кодов")

        val hourRow = rowStats
            .filter { it.rowIndex != codeRow.rowIndex }
            .maxWithOrNull(compareBy<RowStats> { it.numericCells }.thenBy { it.codeLikeCells })
            ?.takeIf { it.numericCells > 0 }
            ?: throw IllegalStateException("На листе '${sheet.sheetName}' не удалось определить строку часов")

        return EmployeeRowPair(
            hourRowIndex = hourRow.rowIndex,
            codeRowIndex = codeRow.rowIndex
        )
    }

    private fun countCodeLikeCells(row: Row?, dayColumns: List<Pair<Int, Int>>): Int {
        if (row == null) return 0
        return dayColumns.count { (columnIndex, _) ->
            normalizeCode(row.getCell(columnIndex)?.readTrimmedString())?.let { it in KNOWN_CODES } == true
        }
    }

    private fun countNumericCells(row: Row?, dayColumns: List<Pair<Int, Int>>): Int {
        if (row == null) return 0
        return dayColumns.count { (columnIndex, _) ->
            row.getCell(columnIndex)?.readNullableDouble() != null
        }
    }

    private fun resolveDayColumns(sheet: Sheet): List<Pair<Int, Int>> {
        // В реальном табеле числа месяца обычно лежат в диапазоне E15:AL15.
        // Сначала пробуем этот точный диапазон, а уже потом ищем автоматически.
        val preferredRowIndex = 14 // Excel row 15
        val preferredColumns = 4..37 // E..AL
        val preferredDays = readDayColumns(sheet.getRow(preferredRowIndex), preferredColumns)
        if (preferredDays.size >= 28) {
            return preferredDays
        }

        val candidateRows = (0..minOf(sheet.lastRowNum, 25)).mapNotNull { rowIndex ->
            val row = sheet.getRow(rowIndex) ?: return@mapNotNull null
            val days = readDayColumns(row, 4..maxOf(37, row.lastCellNum.toInt()))
            if (days.size >= 28) rowIndex to days else null
        }

        return candidateRows.maxByOrNull { (_, days) -> days.size }?.second
            ?: throw IllegalStateException("На листе '${sheet.sheetName}' не найдена строка с числами месяца")
    }

    private fun readDayColumns(row: Row?, columnRange: IntRange): List<Pair<Int, Int>> {
        if (row == null) return emptyList()
        val days = mutableListOf<Pair<Int, Int>>()
        for (columnIndex in columnRange) {
            val cell = row.getCell(columnIndex) ?: continue
            val day = cell.readNullableInt() ?: continue
            if (day in 1..31) {
                days += columnIndex to day
            }
        }
        return days
    }

    private fun resolveRequestedMonths(request: ExcelImportRequest): Set<Int> = when (request.scopeType) {
        ExcelImportScopeType.SINGLE_MONTH -> setOf(requireNotNull(request.singleMonth))
        ExcelImportScopeType.MONTH_RANGE -> {
            val start = requireNotNull(request.rangeStartMonth)
            val end = requireNotNull(request.rangeEndMonth)
            if (start > end) throw IllegalStateException("Начальный месяц диапазона больше конечного")
            (start..end).toSet()
        }
        ExcelImportScopeType.SELECTED_MONTHS -> {
            if (request.selectedMonths.isEmpty()) throw IllegalStateException("Не выбраны месяцы для импорта")
            request.selectedMonths
        }
        ExcelImportScopeType.FULL_YEAR -> (1..12).toSet()
    }

    private fun mapSourceToTarget(sourceCode: String, hours: Double?): Pair<String, String> {
        val roundedHours = hours?.let { round(it * 100.0) / 100.0 }
        return when (sourceCode) {
            "Я" -> {
                if (roundedHours != null && roundedHours <= 8.01) {
                    "8Д" to "8 Д"
                } else {
                    "Д" to "Дневная"
                }
            }
            "Н" -> "Н" to "Ночная"
            "РВД" -> "РВД" to "Работа в выходной день"
            "РВН" -> "РВН" to "Работа в выходной день (ночь)"
            "СП" -> "8" to "8 СП"
            "ОТ" -> "ОТ" to "Отпуск"
            "Б" -> "Б" to "Больничный"
            else -> sourceCode to sourceCode
        }
    }

    private fun requiredTemplateFor(
        targetCode: String,
        targetTitle: String,
        sourceCode: String,
        hours: Double?
    ): ShiftTemplateEntity? {
        return when (targetCode) {
            "Д" -> ShiftTemplateEntity(
                code = "Д",
                title = "Дневная",
                iconKey = "SUN",
                totalHours = hours ?: 11.5,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#1E88E5",
                isWeekendPaid = false,
                active = true,
                sortOrder = 10
            )
            "Н" -> ShiftTemplateEntity(
                code = "Н",
                title = "Ночная",
                iconKey = "MOON",
                totalHours = hours ?: 11.5,
                breakHours = 0.0,
                nightHours = if ((hours ?: 11.5) >= 11.0) 8.0 else 0.0,
                colorHex = "#43A047",
                isWeekendPaid = false,
                active = true,
                sortOrder = 20
            )
            "8" -> ShiftTemplateEntity(
                code = "8",
                title = "8 СП",
                iconKey = "EIGHT",
                totalHours = hours ?: 8.0,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#EF5350",
                isWeekendPaid = false,
                active = true,
                sortOrder = 30
            )
            "8Д" -> ShiftTemplateEntity(
                code = "8Д",
                title = "8 Д",
                iconKey = "TEXT",
                totalHours = hours ?: 8.0,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#5C6BC0",
                isWeekendPaid = false,
                active = true,
                sortOrder = 35
            )
            "ОТ", "Б", "ВЫХ" -> null
            "РВД" -> ShiftTemplateEntity(
                code = "РВД",
                title = "Работа в выходной день",
                iconKey = "STAR",
                totalHours = hours ?: 11.5,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#66BB6A",
                isWeekendPaid = true,
                active = true,
                sortOrder = 70
            )
            "РВН" -> ShiftTemplateEntity(
                code = "РВН",
                title = "Работа в выходной день (ночь)",
                iconKey = "STAR",
                totalHours = hours ?: 11.5,
                breakHours = 0.0,
                nightHours = if ((hours ?: 11.5) >= 11.0) 8.0 else 0.0,
                colorHex = "#BDBDBD",
                isWeekendPaid = true,
                active = true,
                sortOrder = 80
            )
            else -> when (sourceCode) {
                "ОТ", "Б" -> null
                else -> ShiftTemplateEntity(
                    code = targetCode,
                    title = targetTitle,
                    iconKey = "TEXT",
                    totalHours = hours ?: 0.0,
                    breakHours = 0.0,
                    nightHours = 0.0,
                    colorHex = "#78909C",
                    isWeekendPaid = targetCode == "РВД" || targetCode == "РВН",
                    active = true,
                    sortOrder = 90
                )
            }
        }
    }

    private fun Sheet.toMonthSheetSpecOrNull(): MonthSheetSpec? {
        val normalized = sheetName.lowercase(Locale("ru", "RU")).replace(" ", "")
        val month = MONTH_NAME_TO_NUMBER.entries.firstOrNull { normalized.startsWith(it.key) }?.value
            ?: return null
        return MonthSheetSpec(month = month, sheet = this)
    }

    private fun normalizeSearchToken(value: String): String = value
        .trim()
        .lowercase(Locale("ru", "RU"))
        .replace('ё', 'е')

    private fun normalizeName(value: String): String = value
        .trim()
        .lowercase(Locale("ru", "RU"))
        .replace('ё', 'е')
        .replace(Regex("\\s+"), " ")

    private fun normalizeCode(value: String?): String? = value
        ?.trim()
        ?.uppercase(Locale("ru", "RU"))
        ?.replace(Regex("\\s+"), "")
        ?.takeIf { it.isNotBlank() }

    private fun org.apache.poi.ss.usermodel.Cell.readTrimmedString(): String? {
        val raw = when (cellType) {
            CellType.STRING -> stringCellValue
            CellType.NUMERIC -> {
                val numeric = numericCellValue
                val asLong = numeric.toLong()
                if (numeric == asLong.toDouble()) asLong.toString() else numeric.toString()
            }
            CellType.BOOLEAN -> booleanCellValue.toString()
            CellType.FORMULA -> when (cachedFormulaResultType) {
                CellType.STRING -> stringCellValue
                CellType.NUMERIC -> {
                    val numeric = numericCellValue
                    val asLong = numeric.toLong()
                    if (numeric == asLong.toDouble()) asLong.toString() else numeric.toString()
                }
                CellType.BOOLEAN -> booleanCellValue.toString()
                else -> null
            }
            else -> null
        }
        return raw?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun org.apache.poi.ss.usermodel.Cell.readNullableDouble(): Double? {
        return when (cellType) {
            CellType.NUMERIC -> numericCellValue
            CellType.STRING -> stringCellValue
                .replace(',', '.')
                .trim()
                .toDoubleOrNull()
            CellType.FORMULA -> when (cachedFormulaResultType) {
                CellType.NUMERIC -> numericCellValue
                CellType.STRING -> stringCellValue
                    .replace(',', '.')
                    .trim()
                    .toDoubleOrNull()
                else -> readTrimmedString()
                    ?.replace(',', '.')
                    ?.toDoubleOrNull()
            }
            else -> readTrimmedString()
                ?.replace(',', '.')
                ?.toDoubleOrNull()
        }
    }

    private fun org.apache.poi.ss.usermodel.Cell.readNullableInt(): Int? {
        return when (cellType) {
            CellType.NUMERIC -> numericCellValue.toInt()
            CellType.STRING -> stringCellValue.trim().toIntOrNull()
            CellType.FORMULA -> when (cachedFormulaResultType) {
                CellType.NUMERIC -> numericCellValue.toInt()
                CellType.STRING -> stringCellValue.trim().toIntOrNull()
                else -> readTrimmedString()?.toIntOrNull()
            }
            else -> readTrimmedString()?.toIntOrNull()
        }
    }

    private data class MonthSheetSpec(
        val month: Int,
        val sheet: Sheet
    )

    private data class EmployeeRowPair(
        val hourRowIndex: Int,
        val codeRowIndex: Int
    )

    private data class RowStats(
        val rowIndex: Int,
        val codeLikeCells: Int,
        val numericCells: Int
    )

    companion object {
        private val MONTH_NAME_TO_NUMBER = linkedMapOf(
            "январь" to 1,
            "февраль" to 2,
            "март" to 3,
            "апрель" to 4,
            "май" to 5,
            "июнь" to 6,
            "июль" to 7,
            "август" to 8,
            "сентябрь" to 9,
            "октябрь" to 10,
            "ноябрь" to 11,
            "декабрь" to 12
        )

        private val KNOWN_CODES = setOf(
            "Я", "Н", "РВД", "РВН", "СП", "ОТ", "Б", "В", "ВЫХ"
        )
    }
}
