package com.vigilante.shiftsalaryplanner.payroll

import java.util.UUID

enum class DeductionType {
    ALIMONY,
    ENFORCEMENT,
    OTHER
}

enum class DeductionMode {
    SHARE,
    PERCENT,
    FIXED
}

enum class AlimonySharePreset(
    val fraction: Double,
    val label: String
) {
    ONE_CHILD(0.25, "1/4"),
    TWO_CHILDREN(1.0 / 3.0, "1/3"),
    THREE_PLUS(1.0 / 2.0, "1/2"),
    CUSTOM(0.0, "")
}

data class PayrollDeduction(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val type: String = DeductionType.OTHER.name,
    val mode: String = DeductionMode.FIXED.name,
    val value: Double = 0.0,
    val active: Boolean = true,
    val applyToAdvance: Boolean = false,
    val applyToSalary: Boolean = true,
    val priority: Int = 0,
    val note: String = "",
    val shareLabel: String = "",
    val preserveMinimumIncome: Boolean = false,
    val maxPercentLimit: Double = 50.0
)

fun PayrollDeduction.resolvedType(): DeductionType =
    runCatching { DeductionType.valueOf(type) }.getOrElse { DeductionType.OTHER }

fun PayrollDeduction.resolvedMode(): DeductionMode =
    runCatching { DeductionMode.valueOf(mode) }.getOrElse { DeductionMode.FIXED }

fun PayrollDeduction.effectiveFraction(): Double {
    return when (resolvedMode()) {
        DeductionMode.SHARE -> value.coerceIn(0.0, 1.0)
        DeductionMode.PERCENT -> (value / 100.0).coerceIn(0.0, 1.0)
        DeductionMode.FIXED -> 0.0
    }
}

fun defaultAlimonyDeduction(): PayrollDeduction = PayrollDeduction(
    title = "Алименты",
    type = DeductionType.ALIMONY.name,
    mode = DeductionMode.SHARE.name,
    value = AlimonySharePreset.ONE_CHILD.fraction,
    shareLabel = AlimonySharePreset.ONE_CHILD.label,
    active = true,
    applyToAdvance = true,
    applyToSalary = true,
    priority = 10,
    maxPercentLimit = 70.0
)

fun defaultEnforcementDeduction(): PayrollDeduction = PayrollDeduction(
    title = "Исполнительное производство",
    type = DeductionType.ENFORCEMENT.name,
    mode = DeductionMode.FIXED.name,
    value = 0.0,
    active = true,
    applyToAdvance = false,
    applyToSalary = true,
    priority = 20,
    maxPercentLimit = 50.0
)
