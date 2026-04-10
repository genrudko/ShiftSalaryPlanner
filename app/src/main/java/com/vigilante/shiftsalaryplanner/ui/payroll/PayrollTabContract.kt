package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import java.time.YearMonth

data class PayrollTabState(
    val currentMonth: YearMonth,
    val summary: MonthSummary,
    val payroll: PayrollResult,
    val payrollDetailedResult: PayrollDetailedResult,
    val annualOvertime: AnnualOvertimeResult,
    val paymentDates: PaymentDates,
    val housingPaymentLabel: String,
    val detailedShiftStats: DetailedShiftStats,
    val isSummaryExpanded: Boolean
)

data class PayrollTabActions(
    val onPrevMonth: () -> Unit,
    val onNextMonth: () -> Unit,
    val onPickMonth: (YearMonth) -> Unit,
    val onToggleSummary: () -> Unit,
    val onOpenSettings: () -> Unit
)
