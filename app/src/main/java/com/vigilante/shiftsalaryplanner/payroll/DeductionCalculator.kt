package com.vigilante.shiftsalaryplanner.payroll

import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

private fun deductionRoundMoney(value: Double): Double =
    round(value * 100.0) / 100.0

private fun toCents(value: Double): Int =
    (deductionRoundMoney(value) * 100.0).roundToInt()

private fun fromCents(value: Int): Double =
    value / 100.0

private data class DeductionRequestedParts(
    val advanceRequested: Double,
    val salaryRequested: Double
) {
    val totalRequested: Double get() = advanceRequested + salaryRequested
}

private data class RequestedDeduction(
    val deduction: PayrollDeduction,
    val advanceRequested: Double,
    val salaryRequested: Double
) {
    val totalRequested: Double get() = advanceRequested + salaryRequested
}

private data class GroupPaymentSplit(
    val advanceAmount: Double,
    val salaryAmount: Double
) {
    val totalAmount: Double get() = advanceAmount + salaryAmount
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
        val baseAdvance = deductionRoundMoney(netAdvanceBase.coerceAtLeast(0.0))
        val baseSalary = deductionRoundMoney(netSalaryBase.coerceAtLeast(0.0))
        val baseTotal = deductionRoundMoney(baseAdvance + baseSalary)

        if (baseTotal <= 0.0) {
            return emptyResult(
                netAdvanceAfterDeductions = baseAdvance,
                netSalaryAfterDeductions = baseSalary
            )
        }

        val requestedItems = deductions
            .filter { it.active }
            .mapNotNull { deduction ->
                val parts = deduction.requestedParts(baseAdvance, baseSalary)
                val advanceRequested = deductionRoundMoney(parts.advanceRequested.coerceAtLeast(0.0))
                val salaryRequested = deductionRoundMoney(parts.salaryRequested.coerceAtLeast(0.0))
                val totalRequested = deductionRoundMoney(advanceRequested + salaryRequested)

                if (totalRequested <= 0.0) {
                    null
                } else {
                    RequestedDeduction(
                        deduction = deduction,
                        advanceRequested = advanceRequested,
                        salaryRequested = salaryRequested
                    )
                }
            }

        if (requestedItems.isEmpty()) {
            return emptyResult(
                netAdvanceAfterDeductions = baseAdvance,
                netSalaryAfterDeductions = baseSalary
            )
        }

        var remainingAdvance = baseAdvance
        var remainingSalary = baseSalary
        var remainingGlobalCap = deductionRoundMoney(
            baseTotal * (resolveOverallCapPercent(requestedItems.map { it.deduction }) / 100.0)
        )

        val applied = mutableListOf<AppliedDeduction>()

        val queueOrder = requestedItems
            .map { it.deduction.effectiveQueue() }
            .distinct()
            .sortedBy { it.sortOrder }

        for (queue in queueOrder) {
            if (remainingGlobalCap <= 0.0) break

            val queueItems = requestedItems.filter { it.deduction.effectiveQueue() == queue }
            if (queueItems.isEmpty()) continue

            val queueRequestedAdvance = deductionRoundMoney(queueItems.sumOf { it.advanceRequested })
            val queueRequestedSalary = deductionRoundMoney(queueItems.sumOf { it.salaryRequested })
            val queueRequestedTotal = deductionRoundMoney(queueItems.sumOf { it.totalRequested })

            if (queueRequestedTotal <= 0.0) continue

            val queueTotalAvailable = deductionRoundMoney(
                minOf(
                    queueRequestedTotal,
                    remainingGlobalCap,
                    deductionRoundMoney(remainingAdvance + remainingSalary)
                )
            )
            if (queueTotalAvailable <= 0.0) continue

            val queueSplit = splitGroupAvailableBetweenPayments(
                totalAvailable = queueTotalAvailable,
                requestedAdvance = queueRequestedAdvance,
                requestedSalary = queueRequestedSalary,
                remainingAdvance = remainingAdvance,
                remainingSalary = remainingSalary
            )
            if (queueSplit.totalAmount <= 0.0) continue

            val advanceAllocations = allocateProportionally(
                total = queueSplit.advanceAmount,
                weights = queueItems.map { it.deduction.id to it.advanceRequested }
            )
            val salaryAllocations = allocateProportionally(
                total = queueSplit.salaryAmount,
                weights = queueItems.map { it.deduction.id to it.salaryRequested }
            )

            val queueAppliedItems = queueItems
                .sortedWith(
                    compareBy<RequestedDeduction> { it.deduction.title.lowercase() }
                        .thenBy { it.deduction.id }
                )
                .mapNotNull { item ->
                    val advanceAmount = deductionRoundMoney(advanceAllocations[item.deduction.id] ?: 0.0)
                    val salaryAmount = deductionRoundMoney(salaryAllocations[item.deduction.id] ?: 0.0)
                    val totalAmount = deductionRoundMoney(advanceAmount + salaryAmount)

                    if (totalAmount <= 0.0) {
                        null
                    } else {
                        AppliedDeduction(
                            deductionId = item.deduction.id,
                            title = item.deduction.title,
                            type = item.deduction.type,
                            advanceAmount = advanceAmount,
                            salaryAmount = salaryAmount,
                            totalAmount = totalAmount
                        )
                    }
                }

            val queueAppliedAdvance = deductionRoundMoney(queueAppliedItems.sumOf { it.advanceAmount })
            val queueAppliedSalary = deductionRoundMoney(queueAppliedItems.sumOf { it.salaryAmount })
            val queueAppliedTotal = deductionRoundMoney(queueAppliedItems.sumOf { it.totalAmount })

            if (queueAppliedTotal <= 0.0) continue

            remainingAdvance = deductionRoundMoney((remainingAdvance - queueAppliedAdvance).coerceAtLeast(0.0))
            remainingSalary = deductionRoundMoney((remainingSalary - queueAppliedSalary).coerceAtLeast(0.0))
            remainingGlobalCap = deductionRoundMoney((remainingGlobalCap - queueAppliedTotal).coerceAtLeast(0.0))

            applied += queueAppliedItems
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

    private fun emptyResult(
        netAdvanceAfterDeductions: Double,
        netSalaryAfterDeductions: Double
    ): DeductionCalculationResult {
        return DeductionCalculationResult(
            deductionsTotal = 0.0,
            deductionsAdvancePart = 0.0,
            deductionsSalaryPart = 0.0,
            alimonyAmount = 0.0,
            enforcementAmount = 0.0,
            otherDeductionsAmount = 0.0,
            netAdvanceAfterDeductions = deductionRoundMoney(netAdvanceAfterDeductions),
            netSalaryAfterDeductions = deductionRoundMoney(netSalaryAfterDeductions),
            netAfterDeductions = deductionRoundMoney(
                netAdvanceAfterDeductions + netSalaryAfterDeductions
            ),
            appliedDeductions = emptyList()
        )
    }

    private fun resolveOverallCapPercent(
        deductions: List<PayrollDeduction>
    ): Double {
        val legalKinds = deductions.map { it.resolvedLegalKind() }.toSet()

        return when {
            legalKinds.any {
                it == DeductionLegalKind.ALIMONY_MINOR_CHILDREN ||
                        it == DeductionLegalKind.HARM_TO_HEALTH ||
                        it == DeductionLegalKind.LOSS_OF_BREADWINNER ||
                        it == DeductionLegalKind.CRIME_DAMAGE
            } -> 70.0

            legalKinds.any { it == DeductionLegalKind.EXECUTION_GENERAL } -> 50.0
            deductions.isNotEmpty() -> 20.0
            else -> 0.0
        }
    }

    private fun splitGroupAvailableBetweenPayments(
        totalAvailable: Double,
        requestedAdvance: Double,
        requestedSalary: Double,
        remainingAdvance: Double,
        remainingSalary: Double
    ): GroupPaymentSplit {
        if (totalAvailable <= 0.0) {
            return GroupPaymentSplit(0.0, 0.0)
        }

        if (requestedAdvance <= 0.0) {
            return GroupPaymentSplit(
                advanceAmount = 0.0,
                salaryAmount = deductionRoundMoney(
                    minOf(totalAvailable, requestedSalary, remainingSalary)
                )
            )
        }

        if (requestedSalary <= 0.0) {
            return GroupPaymentSplit(
                advanceAmount = deductionRoundMoney(
                    minOf(totalAvailable, requestedAdvance, remainingAdvance)
                ),
                salaryAmount = 0.0
            )
        }

        val requestedTotal = requestedAdvance + requestedSalary
        var advanceAmount = deductionRoundMoney(totalAvailable * (requestedAdvance / requestedTotal))
        var salaryAmount = deductionRoundMoney(totalAvailable - advanceAmount)

        advanceAmount = deductionRoundMoney(minOf(advanceAmount, requestedAdvance, remainingAdvance))
        salaryAmount = deductionRoundMoney(minOf(salaryAmount, requestedSalary, remainingSalary))

        var remainder = deductionRoundMoney(totalAvailable - advanceAmount - salaryAmount)

        if (remainder > 0.0) {
            val extraAdvanceCapacity = deductionRoundMoney(
                minOf(
                    remainder,
                    (requestedAdvance - advanceAmount).coerceAtLeast(0.0),
                    (remainingAdvance - advanceAmount).coerceAtLeast(0.0)
                )
            )
            advanceAmount = deductionRoundMoney(advanceAmount + extraAdvanceCapacity)
            remainder = deductionRoundMoney(remainder - extraAdvanceCapacity)
        }

        if (remainder > 0.0) {
            val extraSalaryCapacity = deductionRoundMoney(
                minOf(
                    remainder,
                    (requestedSalary - salaryAmount).coerceAtLeast(0.0),
                    (remainingSalary - salaryAmount).coerceAtLeast(0.0)
                )
            )
            salaryAmount = deductionRoundMoney(salaryAmount + extraSalaryCapacity)
            remainder = deductionRoundMoney(remainder - extraSalaryCapacity)
        }

        return GroupPaymentSplit(
            advanceAmount = advanceAmount,
            salaryAmount = salaryAmount
        )
    }

    private fun allocateProportionally(
        total: Double,
        weights: List<Pair<String, Double>>
    ): Map<String, Double> {
        val positiveWeights = weights
            .map { it.first to it.second.coerceAtLeast(0.0) }
            .filter { it.second > 0.0 }

        val totalCents = toCents(total)
        if (positiveWeights.isEmpty() || totalCents <= 0) {
            return emptyMap()
        }

        val weightsSum = positiveWeights.sumOf { it.second }
        if (weightsSum <= 0.0) {
            return emptyMap()
        }

        data class FractionalPart(
            val id: String,
            val centsFloor: Int,
            val remainder: Double
        )

        val initial = positiveWeights.map { (id, weight) ->
            val exact = totalCents * (weight / weightsSum)
            val centsFloor = floor(exact).toInt()
            FractionalPart(
                id = id,
                centsFloor = centsFloor,
                remainder = exact - centsFloor
            )
        }

        val resultCents = initial
            .associate { it.id to it.centsFloor }
            .toMutableMap()

        var undistributed = totalCents - initial.sumOf { it.centsFloor }

        initial
            .sortedWith(
                compareByDescending<FractionalPart> { it.remainder }
                    .thenBy { it.id }
            )
            .forEach { part ->
                if (undistributed > 0) {
                    resultCents[part.id] = (resultCents[part.id] ?: 0) + 1
                    undistributed--
                }
            }

        return resultCents.mapValues { fromCents(it.value) }
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
