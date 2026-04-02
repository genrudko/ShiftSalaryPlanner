@file:Suppress("unused")

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

enum class DeductionQueue(val sortOrder: Int) {
    FIRST(10),
    SECOND(20),
    THIRD(30),
    FOURTH(40),
    NONE(50)
}

enum class DeductionLegalKind {
    ALIMONY_MINOR_CHILDREN,
    HARM_TO_HEALTH,
    LOSS_OF_BREADWINNER,
    CRIME_DAMAGE,
    EXECUTION_GENERAL,
    EMPLOYER_OTHER
}

enum class DeductionBasisDocumentType {
    WRIT_OF_EXECUTION,
    COURT_ORDER,
    BAILIFF_COPY,
    NOTARIAL_AGREEMENT,
    EMPLOYEE_STATEMENT,
    EMPLOYER_ORDER,
    OTHER
}

enum class AlimonySharePreset(
    val fraction: Double,
    val label: String
) {
    ONE_CHILD(0.25, "1/4"),
    TWO_CHILDREN(1.0 / 3.0, "1/3"),
    THREE_PLUS(1.0 / 2.0, "1/2"),
    @Suppress("unused")
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

    // Новые юридические поля
    val legalKind: String = DeductionLegalKind.EMPLOYER_OTHER.name,
    val basisDocumentType: String = DeductionBasisDocumentType.OTHER.name,
    val recipientName: String = "",
    val caseNumber: String = "",
    val fixedAmountIndexed: Boolean = false,
    val preserveMinimumIncome: Boolean = false,

    val note: String = "",
    val shareLabel: String = "",

    // Legacy-совместимость со старым UI/калькулятором
    val priority: Int = 0,
    val maxPercentLimit: Double = 20.0
)

fun PayrollDeduction.resolvedType(): DeductionType =
    runCatching { DeductionType.valueOf(type) }.getOrElse { DeductionType.OTHER }

fun PayrollDeduction.resolvedMode(): DeductionMode =
    runCatching { DeductionMode.valueOf(mode) }.getOrElse { DeductionMode.FIXED }

fun PayrollDeduction.resolvedLegalKind(): DeductionLegalKind =
    runCatching { DeductionLegalKind.valueOf(legalKind) }
        .getOrElse { inferLegalKindFromType(resolvedType()) }

fun PayrollDeduction.resolvedBasisDocumentType(): DeductionBasisDocumentType =
    runCatching { DeductionBasisDocumentType.valueOf(basisDocumentType) }
        .getOrElse { DeductionBasisDocumentType.OTHER }

fun PayrollDeduction.effectiveFraction(): Double {
    return when (resolvedMode()) {
        DeductionMode.SHARE -> value.coerceIn(0.0, 1.0)
        DeductionMode.PERCENT -> (value / 100.0).coerceIn(0.0, 1.0)
        DeductionMode.FIXED -> 0.0
    }
}

fun DeductionLegalKind.defaultQueue(): DeductionQueue =
    when (this) {
        DeductionLegalKind.ALIMONY_MINOR_CHILDREN,
        DeductionLegalKind.HARM_TO_HEALTH,
        DeductionLegalKind.LOSS_OF_BREADWINNER,
        DeductionLegalKind.CRIME_DAMAGE -> DeductionQueue.FIRST

        DeductionLegalKind.EXECUTION_GENERAL -> DeductionQueue.FOURTH
        DeductionLegalKind.EMPLOYER_OTHER -> DeductionQueue.NONE
    }

fun DeductionLegalKind.defaultLimitPercent(): Double =
    when (this) {
        DeductionLegalKind.ALIMONY_MINOR_CHILDREN,
        DeductionLegalKind.HARM_TO_HEALTH,
        DeductionLegalKind.LOSS_OF_BREADWINNER,
        DeductionLegalKind.CRIME_DAMAGE -> 70.0

        DeductionLegalKind.EXECUTION_GENERAL -> 50.0
        DeductionLegalKind.EMPLOYER_OTHER -> 20.0
    }

fun DeductionLegalKind.defaultLegacyPriority(): Int = defaultQueue().sortOrder

fun DeductionLegalKind.displayName(): String =
    when (this) {
        DeductionLegalKind.ALIMONY_MINOR_CHILDREN -> "Алименты на несовершеннолетних"
        DeductionLegalKind.HARM_TO_HEALTH -> "Возмещение вреда здоровью"
        DeductionLegalKind.LOSS_OF_BREADWINNER -> "Возмещение вреда из-за смерти кормильца"
        DeductionLegalKind.CRIME_DAMAGE -> "Возмещение ущерба от преступления"
        DeductionLegalKind.EXECUTION_GENERAL -> "Иное взыскание по исполнительному документу"
        DeductionLegalKind.EMPLOYER_OTHER -> "Прочее удержание работодателя"
    }

fun DeductionQueue.displayName(): String =
    when (this) {
        DeductionQueue.FIRST -> "1 очередь"
        DeductionQueue.SECOND -> "2 очередь"
        DeductionQueue.THIRD -> "3 очередь"
        DeductionQueue.FOURTH -> "4 очередь"
        DeductionQueue.NONE -> "Вне очереди исполнительных документов"
    }

fun DeductionBasisDocumentType.displayName(): String =
    when (this) {
        DeductionBasisDocumentType.WRIT_OF_EXECUTION -> "Исполнительный лист"
        DeductionBasisDocumentType.COURT_ORDER -> "Судебный приказ"
        DeductionBasisDocumentType.BAILIFF_COPY -> "Постановление / копия от пристава"
        DeductionBasisDocumentType.NOTARIAL_AGREEMENT -> "Нотариальное соглашение"
        DeductionBasisDocumentType.EMPLOYEE_STATEMENT -> "Заявление работника"
        DeductionBasisDocumentType.EMPLOYER_ORDER -> "Приказ работодателя"
        DeductionBasisDocumentType.OTHER -> "Иное основание"
    }

fun PayrollDeduction.effectiveQueue(): DeductionQueue =
    resolvedLegalKind().defaultQueue()

fun PayrollDeduction.effectiveLimitPercent(): Double =
    resolvedLegalKind().defaultLimitPercent()

fun inferLegalKindFromType(type: DeductionType): DeductionLegalKind =
    when (type) {
        DeductionType.ALIMONY -> DeductionLegalKind.ALIMONY_MINOR_CHILDREN
        DeductionType.ENFORCEMENT -> DeductionLegalKind.EXECUTION_GENERAL
        DeductionType.OTHER -> DeductionLegalKind.EMPLOYER_OTHER
    }

fun legalKindOptions(type: DeductionType): List<DeductionLegalKind> =
    when (type) {
        DeductionType.ALIMONY -> listOf(
            DeductionLegalKind.ALIMONY_MINOR_CHILDREN
        )

        DeductionType.ENFORCEMENT -> listOf(
            DeductionLegalKind.HARM_TO_HEALTH,
            DeductionLegalKind.LOSS_OF_BREADWINNER,
            DeductionLegalKind.CRIME_DAMAGE,
            DeductionLegalKind.EXECUTION_GENERAL
        )

        DeductionType.OTHER -> listOf(
            DeductionLegalKind.EMPLOYER_OTHER
        )
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
    legalKind = DeductionLegalKind.ALIMONY_MINOR_CHILDREN.name,
    basisDocumentType = DeductionBasisDocumentType.OTHER.name,
    priority = DeductionLegalKind.ALIMONY_MINOR_CHILDREN.defaultLegacyPriority(),
    maxPercentLimit = DeductionLegalKind.ALIMONY_MINOR_CHILDREN.defaultLimitPercent()
)

fun defaultEnforcementDeduction(): PayrollDeduction = PayrollDeduction(
    title = "Исполнительное производство",
    type = DeductionType.ENFORCEMENT.name,
    mode = DeductionMode.FIXED.name,
    value = 0.0,
    active = true,
    applyToAdvance = false,
    applyToSalary = true,
    legalKind = DeductionLegalKind.EXECUTION_GENERAL.name,
    basisDocumentType = DeductionBasisDocumentType.OTHER.name,
    priority = DeductionLegalKind.EXECUTION_GENERAL.defaultLegacyPriority(),
    maxPercentLimit = DeductionLegalKind.EXECUTION_GENERAL.defaultLimitPercent()
)