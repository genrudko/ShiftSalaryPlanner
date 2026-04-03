package com.vigilante.shiftsalaryplanner.payroll

import java.time.LocalDate

enum class PayrollSheetSection {
    HEADER,
    ACCRUAL,
    DEDUCTION,
    PRIOR_PAYMENT,
    PAYOUT,
    REFERENCE
}

enum class PayrollQuantityUnit {
    HOURS,
    DAYS,
    MONTHS,
    TIMES,
    NONE
}

enum class PayrollLineKind {
    BASE_SALARY,
    NIGHT_EXTRA,
    INTERSTIM_ALLOWANCE,
    INDEXATION_PAYMENT,
    HOUSING_COMPENSATION,
    OPERATION_PREMIUM,
    SPECIAL_DAY_PAY,
    SPECIAL_DAY_EXTRA,
    IGRV_EXTRA,
    VACATION_PAY,
    SICK_PAY_EMPLOYER,
    SICK_PAY_EXTERNAL,
    NDFL,
    ADVANCE_PAID,
    INTERPAYMENT_PAID,
    EXECUTIVE_DEDUCTION,
    FINAL_PAYOUT,
    REFERENCE_VALUE,
    OTHER
}

data class PayrollLineItem(
    val id: String,
    val section: PayrollSheetSection,
    val kind: PayrollLineKind,
    val code: String? = null,
    val title: String,
    val periodLabel: String? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val quantity: Double? = null,
    val unit: PayrollQuantityUnit = PayrollQuantityUnit.NONE,
    val amount: Double,
    val sortOrder: Int = 0,
    val note: String? = null
)

data class PayrollDetailedResult(
    val summary: PayrollResult,
    val lineItems: List<PayrollLineItem>
)