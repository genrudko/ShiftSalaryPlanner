package com.vigilante.shiftsalaryplanner.payroll

import kotlin.math.round

private fun deductionRoundMoney(value: Double): Double =
    round(value * 100.0) / 100.0

private data class DeductionRequestedParts(
    val advanceRequested: Double,
    val salaryRequested: Double
) {
    val totalRequested: Double get() = advanceRequested + salaryRequested
}

data class AppliedDeduction(
    val deductionId: String,
    val title: String,
    val type: String,
    val advanceAmount: Double,
    val salaryAmount: Double,
    val totalAmount: Double
)

data class DeductionCalculationResult(
    val deductionsTotal: Double,
    val deductionsAdvancePart: Double,
    val deductionsSalaryPart: Double,
    val alimonyAmount: Double,
    val enforcementAmount: Double,
    val otherDeductionsAmount: Double,
    val netAdvanceAfterDeductions: Double,
    val netSalaryAfterDeductions: Double,
    val netAfterDeductions: Double,
    val appliedDeductions: List<AppliedDeduction>
)

object DeductionCalculator {

    fun calculate(
        netAdvanceBase: Double,
        netSalaryBase: Double,
        deductions: List<PayrollDeduction>
    ): DeductionCalculationResult {
        var remainingAdvance = deductionRoundMoney(netAdvanceBase.coerceAtLeast(0.0))
        var remainingSalary = deductionRoundMoney(netSalaryBase.coerceAtLeast(0.0))
        val baseAdvance = remainingAdvance
        val baseSalary = remainingSalary

        val applied = mutableListOf<AppliedDeduction>()

        deductions
            .filter { it.active }
            .sortedWith(compareBy<PayrollDeduction> { it.priority }.thenBy { it.title.lowercase() })
            .forEach { deduction ->
                val requested = deduction.requestedParts(baseAdvance, baseSalary)
                if (requested.totalRequested <= 0.0) return@forEach

                val maxAllowedTotal = deductionRoundMoney(
                    requested.totalRequested.coerceAtLeast(0.0)
                        .coerceAtMost(
                            (baseAdvance + baseSalary) *
                                    (deduction.maxPercentLimit.coerceIn(0.0, 100.0) / 100.0)
                        )
                )
                if (maxAllowedTotal <= 0.0) return@forEach

                var advanceAmount = 0.0
                var salaryAmount = 0.0

                if (requested.advanceRequested > 0.0 && requested.salaryRequested > 0.0) {
                    val totalRequested = requested.totalRequested
                    val advanceShare = requested.advanceRequested / totalRequested
                    advanceAmount = maxAllowedTotal * advanceShare
                    salaryAmount = maxAllowedTotal - advanceAmount
                } else if (requested.advanceRequested > 0.0) {
                    advanceAmount = maxAllowedTotal
                } else {
                    salaryAmount = maxAllowedTotal
                }

                advanceAmount = deductionRoundMoney(minOf(advanceAmount, remainingAdvance))
                salaryAmount = deductionRoundMoney(minOf(salaryAmount, remainingSalary))

                val actualTotal = deductionRoundMoney(advanceAmount + salaryAmount)
                if (actualTotal <= 0.0) return@forEach

                remainingAdvance = deductionRoundMoney((remainingAdvance - advanceAmount).coerceAtLeast(0.0))
                remainingSalary = deductionRoundMoney((remainingSalary - salaryAmount).coerceAtLeast(0.0))

                applied += AppliedDeduction(
                    deductionId = deduction.id,
                    title = deduction.title,
                    type = deduction.type,
                    advanceAmount = advanceAmount,
                    salaryAmount = salaryAmount,
                    totalAmount = actualTotal
                )
            }

        val deductionsTotal = deductionRoundMoney(applied.sumOf { it.totalAmount })
        val deductionsAdvancePart = deductionRoundMoney(applied.sumOf { it.advanceAmount })
        val deductionsSalaryPart = deductionRoundMoney(applied.sumOf { it.salaryAmount })
        val alimonyAmount = deductionRoundMoney(
            applied.filter { it.type == DeductionType.ALIMONY.name }.sumOf { it.totalAmount }
        )
        val enforcementAmount = deductionRoundMoney(
            applied.filter { it.type == DeductionType.ENFORCEMENT.name }.sumOf { it.totalAmount }
        )
        val otherDeductionsAmount = deductionRoundMoney(
            applied.filter { it.type == DeductionType.OTHER.name }.sumOf { it.totalAmount }
        )

        return DeductionCalculationResult(
            deductionsTotal = deductionsTotal,
            deductionsAdvancePart = deductionsAdvancePart,
            deductionsSalaryPart = deductionsSalaryPart,
            alimonyAmount = alimonyAmount,
            enforcementAmount = enforcementAmount,
            otherDeductionsAmount = otherDeductionsAmount,
            netAdvanceAfterDeductions = remainingAdvance,
            netSalaryAfterDeductions = remainingSalary,
            netAfterDeductions = deductionRoundMoney(remainingAdvance + remainingSalary),
            appliedDeductions = applied
        )
    }

    private fun PayrollDeduction.requestedParts(
        baseAdvance: Double,
        baseSalary: Double
    ): DeductionRequestedParts {
        val safeAdvanceBase = baseAdvance.coerceAtLeast(0.0)
        val safeSalaryBase = baseSalary.coerceAtLeast(0.0)
        val fraction = effectiveFraction()
        val fixedValue = value.coerceAtLeast(0.0)

        return when (resolvedMode()) {
            DeductionMode.SHARE,
            DeductionMode.PERCENT -> {
                DeductionRequestedParts(
                    advanceRequested = if (applyToAdvance) safeAdvanceBase * fraction else 0.0,
                    salaryRequested = if (applyToSalary) safeSalaryBase * fraction else 0.0
                )
            }

            DeductionMode.FIXED -> {
                when {
                    applyToAdvance && applyToSalary -> {
                        val totalBase = safeAdvanceBase + safeSalaryBase
                        if (totalBase <= 0.0) {
                            DeductionRequestedParts(0.0, 0.0)
                        } else {
                            val advanceShare = safeAdvanceBase / totalBase
                            val advanceRequested = fixedValue * advanceShare
                            DeductionRequestedParts(
                                advanceRequested = advanceRequested,
                                salaryRequested = fixedValue - advanceRequested
                            )
                        }
                    }

                    applyToAdvance -> DeductionRequestedParts(
                        advanceRequested = fixedValue,
                        salaryRequested = 0.0
                    )

                    applyToSalary -> DeductionRequestedParts(
                        advanceRequested = 0.0,
                        salaryRequested = fixedValue
                    )

                    else -> DeductionRequestedParts(0.0, 0.0)
                }
            }
        }
    }
}