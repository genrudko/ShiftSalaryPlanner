package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import java.time.LocalDate
import java.time.YearMonth

private const val FINANCE_ALL_WORKPLACES_ID = "__all_workplaces__"

class FinanceFeatureState(
    initialMonth: YearMonth,
    showAdditionalPaymentDialog: Boolean = false,
    editingAdditionalPaymentId: String? = null,
    editingDeductionId: String? = null,
    isSummaryExpanded: Boolean = false,
    payrollPeriodModeName: String = PayrollPeriodMode.MONTH.name,
    payrollWorkplaceFilterId: String = FINANCE_ALL_WORKPLACES_ID,
    settingsWorkplaceId: String = WORKPLACE_MAIN_ID,
    payrollSelectedYear: Int = initialMonth.year,
    payrollRangeStartIso: String = initialMonth.atDay(1).toString(),
    payrollRangeEndIso: String = initialMonth.atEndOfMonth().toString()
) {
    var showAdditionalPaymentDialog by mutableStateOf(showAdditionalPaymentDialog)
    var editingAdditionalPaymentId by mutableStateOf(editingAdditionalPaymentId)
    var editingDeductionId by mutableStateOf(editingDeductionId)
    var isSummaryExpanded by mutableStateOf(isSummaryExpanded)
    var payrollPeriodModeName by mutableStateOf(payrollPeriodModeName)
    var payrollWorkplaceFilterId by mutableStateOf(payrollWorkplaceFilterId)
    var settingsWorkplaceId by mutableStateOf(settingsWorkplaceId)
    var payrollSelectedYear by mutableStateOf(payrollSelectedYear)
    var payrollRangeStartIso by mutableStateOf(payrollRangeStartIso)
    var payrollRangeEndIso by mutableStateOf(payrollRangeEndIso)

    var pendingReportCsvContent by mutableStateOf<String?>(null)
    var pendingReportCsvFileName by mutableStateOf("report.csv")
    var pendingReportPdfBytes by mutableStateOf<ByteArray?>(null)
    var pendingReportPdfFileName by mutableStateOf("report.pdf")

    fun changePeriodMode(mode: PayrollPeriodMode, currentStart: LocalDate, currentEnd: LocalDate) {
        payrollPeriodModeName = mode.name
        when (mode) {
            PayrollPeriodMode.MONTH -> Unit
            PayrollPeriodMode.YEAR -> payrollSelectedYear = currentEnd.year
            PayrollPeriodMode.RANGE -> {
                payrollRangeStartIso = currentStart.toString()
                payrollRangeEndIso = currentEnd.toString()
            }
        }
    }

    fun selectWorkplace(id: String) {
        payrollWorkplaceFilterId = id
    }

    fun openSettingsFor(workplaceId: String) {
        settingsWorkplaceId = workplaceId
    }

    fun previousYear() {
        payrollSelectedYear -= 1
    }

    fun nextYear() {
        payrollSelectedYear += 1
    }

    fun selectYear(year: Int) {
        payrollSelectedYear = year
    }

    fun shiftRange(start: LocalDate, end: LocalDate, days: Long) {
        payrollRangeStartIso = start.plusDays(days).toString()
        payrollRangeEndIso = end.plusDays(days).toString()
    }

    fun pickRangeStart(date: LocalDate, currentEnd: LocalDate) {
        if (date.isAfter(currentEnd)) {
            payrollRangeStartIso = currentEnd.toString()
            payrollRangeEndIso = date.toString()
        } else {
            payrollRangeStartIso = date.toString()
        }
    }

    fun pickRangeEnd(date: LocalDate, currentStart: LocalDate) {
        if (date.isBefore(currentStart)) {
            payrollRangeStartIso = date.toString()
            payrollRangeEndIso = currentStart.toString()
        } else {
            payrollRangeEndIso = date.toString()
        }
    }

    fun toggleSummary() {
        isSummaryExpanded = !isSummaryExpanded
    }

    fun openNewPayment(workplaceId: String) {
        settingsWorkplaceId = workplaceId
        editingAdditionalPaymentId = null
        showAdditionalPaymentDialog = true
    }

    fun openPayment(id: String, workplaceId: String) {
        settingsWorkplaceId = workplaceId
        editingAdditionalPaymentId = id
        showAdditionalPaymentDialog = true
    }

    fun closePaymentDialog() {
        showAdditionalPaymentDialog = false
        editingAdditionalPaymentId = null
    }

    fun startDeductionEdit(id: String?) {
        editingDeductionId = id
    }

    fun clearDeductionEdit() {
        editingDeductionId = null
    }

    fun stageCsv(content: String, fileName: String) {
        pendingReportCsvContent = content
        pendingReportCsvFileName = fileName
    }

    fun clearCsvPayload() {
        pendingReportCsvContent = null
    }

    fun stagePdf(bytes: ByteArray, fileName: String) {
        pendingReportPdfBytes = bytes
        pendingReportPdfFileName = fileName
    }

    fun clearPdfPayload() {
        pendingReportPdfBytes = null
    }
}

private fun encodeNullableFinanceValue(value: String?): String =
    if (value == null) "0" else "1$value"

private fun decodeNullableFinanceValue(value: String?): String? = when {
    value == null || value == "0" -> null
    value.startsWith("1") -> value.substring(1)
    else -> null
}

internal fun FinanceFeatureState.toSaveableStrings(): ArrayList<String> = arrayListOf(
    if (showAdditionalPaymentDialog) "1" else "0",
    encodeNullableFinanceValue(editingAdditionalPaymentId),
    encodeNullableFinanceValue(editingDeductionId),
    if (isSummaryExpanded) "1" else "0",
    payrollPeriodModeName,
    payrollWorkplaceFilterId,
    settingsWorkplaceId,
    payrollSelectedYear.toString(),
    payrollRangeStartIso,
    payrollRangeEndIso
)

internal fun restoreFinanceFeatureState(saved: List<String>, initialMonth: YearMonth): FinanceFeatureState =
    FinanceFeatureState(
        initialMonth = initialMonth,
        showAdditionalPaymentDialog = saved.getOrNull(0) == "1",
        editingAdditionalPaymentId = decodeNullableFinanceValue(saved.getOrNull(1)),
        editingDeductionId = decodeNullableFinanceValue(saved.getOrNull(2)),
        isSummaryExpanded = saved.getOrNull(3) == "1",
        payrollPeriodModeName = saved.getOrNull(4) ?: PayrollPeriodMode.MONTH.name,
        payrollWorkplaceFilterId = saved.getOrNull(5) ?: FINANCE_ALL_WORKPLACES_ID,
        settingsWorkplaceId = saved.getOrNull(6) ?: WORKPLACE_MAIN_ID,
        payrollSelectedYear = saved.getOrNull(7)?.toIntOrNull() ?: initialMonth.year,
        payrollRangeStartIso = saved.getOrNull(8) ?: initialMonth.atDay(1).toString(),
        payrollRangeEndIso = saved.getOrNull(9) ?: initialMonth.atEndOfMonth().toString()
    )

val FinanceFeatureStateSaver: Saver<FinanceFeatureState, ArrayList<String>> = Saver(
    save = { state -> state.toSaveableStrings() },
    restore = { saved -> restoreFinanceFeatureState(saved, YearMonth.of(1970, 1)) }
)

@Composable
fun rememberFinanceFeatureState(initialMonth: YearMonth): FinanceFeatureState = rememberSaveable(
    saver = FinanceFeatureStateSaver
) {
    FinanceFeatureState(initialMonth)
}
