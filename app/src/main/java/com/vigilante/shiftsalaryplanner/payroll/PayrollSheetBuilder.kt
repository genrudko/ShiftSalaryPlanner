package com.vigilante.shiftsalaryplanner.payroll

import java.time.YearMonth
import java.util.UUID

object PayrollSheetBuilder {

    fun build(
        month: YearMonth,
        summary: PayrollResult,
        accrualItems: List<PayrollLineItem> = emptyList(),
        deductionItems: List<PayrollLineItem> = emptyList(),
        priorPaymentItems: List<PayrollLineItem> = emptyList(),
        payoutItems: List<PayrollLineItem> = emptyList(),
        referenceItems: List<PayrollLineItem> = emptyList(),
        headerItems: List<PayrollLineItem> = emptyList()
    ): PayrollDetailedResult {
        val allItems = buildList {
            addAll(headerItems.sortedBy { it.sortOrder })
            addAll(accrualItems.sortedBy { it.sortOrder })
            addAll(deductionItems.sortedBy { it.sortOrder })
            addAll(priorPaymentItems.sortedBy { it.sortOrder })
            addAll(payoutItems.sortedBy { it.sortOrder })
            addAll(referenceItems.sortedBy { it.sortOrder })
        }

        return PayrollDetailedResult(
            summary = summary,
            lineItems = allItems
        )
    }

    fun headerLine(
        title: String,
        amount: Double,
        sortOrder: Int,
        note: String? = null
    ): PayrollLineItem {
        return PayrollLineItem(
            id = UUID.randomUUID().toString(),
            section = PayrollSheetSection.HEADER,
            kind = PayrollLineKind.REFERENCE_VALUE,
            title = title,
            amount = amount,
            sortOrder = sortOrder,
            note = note
        )
    }

    fun accrualLine(
        kind: PayrollLineKind,
        title: String,
        amount: Double,
        sortOrder: Int,
        code: String? = null,
        periodLabel: String? = null,
        quantity: Double? = null,
        unit: PayrollQuantityUnit = PayrollQuantityUnit.NONE,
        note: String? = null
    ): PayrollLineItem {
        return PayrollLineItem(
            id = UUID.randomUUID().toString(),
            section = PayrollSheetSection.ACCRUAL,
            kind = kind,
            code = code,
            title = title,
            periodLabel = periodLabel,
            quantity = quantity,
            unit = unit,
            amount = amount,
            sortOrder = sortOrder,
            note = note
        )
    }

    fun deductionLine(
        kind: PayrollLineKind,
        title: String,
        amount: Double,
        sortOrder: Int,
        code: String? = null,
        periodLabel: String? = null,
        quantity: Double? = null,
        unit: PayrollQuantityUnit = PayrollQuantityUnit.NONE,
        note: String? = null
    ): PayrollLineItem {
        return PayrollLineItem(
            id = UUID.randomUUID().toString(),
            section = PayrollSheetSection.DEDUCTION,
            kind = kind,
            code = code,
            title = title,
            periodLabel = periodLabel,
            quantity = quantity,
            unit = unit,
            amount = amount,
            sortOrder = sortOrder,
            note = note
        )
    }

    fun priorPaymentLine(
        kind: PayrollLineKind,
        title: String,
        amount: Double,
        sortOrder: Int,
        code: String? = null,
        periodLabel: String? = null,
        note: String? = null
    ): PayrollLineItem {
        return PayrollLineItem(
            id = UUID.randomUUID().toString(),
            section = PayrollSheetSection.PRIOR_PAYMENT,
            kind = kind,
            code = code,
            title = title,
            periodLabel = periodLabel,
            amount = amount,
            sortOrder = sortOrder,
            note = note
        )
    }

    fun payoutLine(
        title: String,
        amount: Double,
        sortOrder: Int,
        note: String? = null
    ): PayrollLineItem {
        return PayrollLineItem(
            id = UUID.randomUUID().toString(),
            section = PayrollSheetSection.PAYOUT,
            kind = PayrollLineKind.FINAL_PAYOUT,
            title = title,
            amount = amount,
            sortOrder = sortOrder,
            note = note
        )
    }

    fun referenceLine(
        title: String,
        amount: Double,
        sortOrder: Int,
        code: String? = null,
        periodLabel: String? = null,
        quantity: Double? = null,
        unit: PayrollQuantityUnit = PayrollQuantityUnit.NONE,
        note: String? = null
    ): PayrollLineItem {
        return PayrollLineItem(
            id = UUID.randomUUID().toString(),
            section = PayrollSheetSection.REFERENCE,
            kind = PayrollLineKind.REFERENCE_VALUE,
            code = code,
            title = title,
            periodLabel = periodLabel,
            quantity = quantity,
            unit = unit,
            amount = amount,
            sortOrder = sortOrder,
            note = note
        )
    }
}