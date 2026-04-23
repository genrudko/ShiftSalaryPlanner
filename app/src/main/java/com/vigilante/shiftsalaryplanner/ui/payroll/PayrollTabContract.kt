package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import java.time.LocalDate
import java.time.YearMonth

enum class PayrollPeriodMode {
    MONTH,
    RANGE,
    YEAR
}

data class PayrollWorkplaceOption(
    val id: String,
    val title: String
)

data class PayrollTabState(
    val currentMonth: YearMonth,
    val periodMode: PayrollPeriodMode,
    val selectedWorkplaceId: String,
    val workplaceOptions: List<PayrollWorkplaceOption>,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val periodLabel: String,
    val periodFileLabel: String,
    val summary: MonthSummary,
    val payroll: PayrollResult,
    val payrollDetailedResult: PayrollDetailedResult,
    val annualOvertime: AnnualOvertimeResult,
    val paymentDates: PaymentDates,
    val housingPaymentLabel: String,
    val detailedShiftStats: DetailedShiftStats,
    val isSummaryExpanded: Boolean,
    val reportVisibilitySettings: ReportVisibilitySettings
)

data class PayrollTabActions(
    val onChangePeriodMode: (PayrollPeriodMode) -> Unit,
    val onChangeWorkplace: (String) -> Unit,
    val onPrevMonth: () -> Unit,
    val onNextMonth: () -> Unit,
    val onPickMonth: (YearMonth) -> Unit,
    val onPrevYear: () -> Unit,
    val onNextYear: () -> Unit,
    val onPickYear: (Int) -> Unit,
    val onShiftRangeBackward: () -> Unit,
    val onShiftRangeForward: () -> Unit,
    val onPickRangeStart: (LocalDate) -> Unit,
    val onPickRangeEnd: (LocalDate) -> Unit,
    val onToggleSummary: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenDiagnostics: () -> Unit,
    val onOpenVisibilitySettings: () -> Unit,
    val onExportSheetPdf: (String, String, PayrollDetailedResult) -> Unit
)
