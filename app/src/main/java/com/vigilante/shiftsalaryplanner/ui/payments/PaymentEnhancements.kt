@file:Suppress("unused")

package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPaymentType
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PaymentDistribution
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.PremiumPeriod
import com.vigilante.shiftsalaryplanner.payroll.WorkShiftItem
import com.vigilante.shiftsalaryplanner.payroll.calculateNdflForTaxableSegment
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

private fun roundMoneyCompat(value: Double): Double = (value * 100.0).roundToInt() / 100.0

private fun isWorkedShift(shift: WorkShiftItem): Boolean =
    !shift.isVacation && !shift.isSickLeave && shift.paidHours > 0.0

private fun endMonthMatches(period: PremiumPeriod, monthValue: Int): Boolean {
    return when (period) {
        PremiumPeriod.MONTHLY -> true
        PremiumPeriod.QUARTERLY -> monthValue in setOf(3, 6, 9, 12)
        PremiumPeriod.HALF_YEARLY -> monthValue in setOf(6, 12)
        PremiumPeriod.YEARLY -> monthValue == 12
    }
}

fun additionalPaymentTypeLabel(typeName: String): String {
    return when (runCatching { AdditionalPaymentType.valueOf(typeName) }.getOrElse { AdditionalPaymentType.MONTHLY }) {
        AdditionalPaymentType.MONTHLY -> "Ежемесячная"
        AdditionalPaymentType.HOURLY -> "Почасовая"
        AdditionalPaymentType.ONE_TIME_MONTH -> "Разовая за месяц"
        AdditionalPaymentType.PREMIUM -> "Премия"
    }
}

fun premiumPeriodLabel(periodName: String): String {
    return when (runCatching { PremiumPeriod.valueOf(periodName) }.getOrElse { PremiumPeriod.MONTHLY }) {
        PremiumPeriod.MONTHLY -> "Месячная"
        PremiumPeriod.QUARTERLY -> "Квартальная"
        PremiumPeriod.HALF_YEARLY -> "Полугодовая"
        PremiumPeriod.YEARLY -> "Годовая"
    }
}

fun paymentDistributionLabel(distributionName: String): String {
    return when (runCatching { PaymentDistribution.valueOf(distributionName) }.getOrElse { PaymentDistribution.SALARY }) {
        PaymentDistribution.ADVANCE -> "В аванс"
        PaymentDistribution.SALARY -> "В зарплату"
        PaymentDistribution.SPLIT_BY_HALF_MONTH -> "Делить по половинам месяца"
    }
}

fun additionalPaymentDetailsLabel(payment: AdditionalPayment): String {
    val type = payment.resolvedType()
    return when (type) {
        AdditionalPaymentType.MONTHLY -> "Сумма: ${roundMoneyCompat(payment.amount)}"
        AdditionalPaymentType.HOURLY -> "Ставка: ${roundMoneyCompat(payment.amount)} в час"
        AdditionalPaymentType.ONE_TIME_MONTH -> "Месяц: ${payment.targetMonth.ifBlank { "не выбран" }}"
        AdditionalPaymentType.PREMIUM -> {
            val delayText = if (payment.delayMonths != 0) " • сдвиг ${payment.delayMonths} мес." else ""
            "${premiumPeriodLabel(payment.premiumPeriod)}${delayText}"
        }
    }
}

data class ResolvedAdditionalPayment(
    val sourceId: String,
    val displayName: String,
    val amount: Double,
    val taxable: Boolean,
    val withAdvance: Boolean,
    val includeInShiftCost: Boolean,
    val sourceTypeName: String
)

data class PaymentResolutionSummary(
    val lines: List<ResolvedAdditionalPayment>
) {
    @Suppress("unused")
    val total: Double get() = roundMoneyCompat(lines.sumOf { it.amount })
    val advanceTotal: Double get() = roundMoneyCompat(lines.filter { it.withAdvance }.sumOf { it.amount })
    val salaryTotal: Double get() = roundMoneyCompat(lines.filterNot { it.withAdvance }.sumOf { it.amount })
    val includedInShiftCostTotal: Double
        get() = roundMoneyCompat(lines.filter { it.includeInShiftCost }.sumOf { it.amount })

    fun asPayrollPayments(): List<AdditionalPayment> {
        return lines.mapIndexed { index, line ->
            AdditionalPayment(
                id = "${line.sourceId}_$index",
                name = line.displayName,
                amount = roundMoneyCompat(line.amount),
                taxable = line.taxable,
                withAdvance = line.withAdvance,
                active = true,
                type = line.sourceTypeName,
                includeInShiftCost = line.includeInShiftCost
            )
        }
    }
}

data class ResolvedAdditionalPaymentBreakdown(
    val payment: ResolvedAdditionalPayment,
    val grossAmount: Double,
    val ndflAmount: Double,
    val netAmount: Double
)

fun calculateResolvedAdditionalPaymentBreakdown(
    resolvedPayments: List<ResolvedAdditionalPayment>,
    payroll: PayrollResult,
    payrollSettings: PayrollSettings
): List<ResolvedAdditionalPaymentBreakdown> {
    var taxableIncomeCursor = roundMoneyCompat(
        (payroll.taxableIncomeYtdBeforeCurrentMonth +
                (payroll.taxableGrossTotal - payroll.additionalPaymentsTaxablePart).coerceAtLeast(0.0))
            .coerceAtLeast(0.0)
    )

    return resolvedPayments.map { payment ->
        val grossAmount = roundMoneyCompat(payment.amount)
        val ndflAmount = if (payment.taxable && grossAmount > 0.0) {
            calculateNdflForTaxableSegment(
                taxableIncomeYtdBeforeSegment = taxableIncomeCursor,
                taxableSegmentAmount = grossAmount,
                progressiveNdflEnabled = payrollSettings.progressiveNdflEnabled,
                flatRate = payrollSettings.ndflPercent
            )
        } else {
            0.0
        }
        if (payment.taxable && grossAmount > 0.0) {
            taxableIncomeCursor = roundMoneyCompat(taxableIncomeCursor + grossAmount)
        }
        val safeNdfl = roundMoneyCompat(ndflAmount.coerceIn(0.0, grossAmount))
        ResolvedAdditionalPaymentBreakdown(
            payment = payment,
            grossAmount = grossAmount,
            ndflAmount = safeNdfl,
            netAmount = roundMoneyCompat(grossAmount - safeNdfl)
        )
    }
}

private fun addResolvedFixedPayment(
    output: MutableList<ResolvedAdditionalPayment>,
    payment: AdditionalPayment,
    displayName: String,
    amount: Double
) {
    val safeAmount = roundMoneyCompat(amount)
    if (safeAmount == 0.0) return

    when (payment.resolvedDistribution()) {
        PaymentDistribution.ADVANCE -> {
            output += ResolvedAdditionalPayment(
                sourceId = payment.id,
                displayName = displayName,
                amount = safeAmount,
                taxable = payment.taxable,
                withAdvance = true,
                includeInShiftCost = payment.includeInShiftCost,
                sourceTypeName = payment.type
            )
        }

        PaymentDistribution.SALARY -> {
            output += ResolvedAdditionalPayment(
                sourceId = payment.id,
                displayName = displayName,
                amount = safeAmount,
                taxable = payment.taxable,
                withAdvance = false,
                includeInShiftCost = payment.includeInShiftCost,
                sourceTypeName = payment.type
            )
        }

        PaymentDistribution.SPLIT_BY_HALF_MONTH -> {
            val first = roundMoneyCompat(safeAmount / 2.0)
            val second = roundMoneyCompat(safeAmount - first)
            if (first != 0.0) {
                output += ResolvedAdditionalPayment(
                    sourceId = payment.id,
                    displayName = "$displayName (1-я половина)",
                    amount = first,
                    taxable = payment.taxable,
                    withAdvance = true,
                    includeInShiftCost = payment.includeInShiftCost,
                    sourceTypeName = payment.type
                )
            }
            if (second != 0.0) {
                output += ResolvedAdditionalPayment(
                    sourceId = payment.id,
                    displayName = "$displayName (2-я половина)",
                    amount = second,
                    taxable = payment.taxable,
                    withAdvance = false,
                    includeInShiftCost = payment.includeInShiftCost,
                    sourceTypeName = payment.type
                )
            }
        }
    }
}

private fun addResolvedHourlyPayment(
    output: MutableList<ResolvedAdditionalPayment>,
    payment: AdditionalPayment,
    totalHours: Double,
    firstHalfHours: Double
) {
    val secondHalfHours = (totalHours - firstHalfHours).coerceAtLeast(0.0)
    val rate = payment.amount
    val title = payment.name.ifBlank { "Почасовая доплата" }

    when (payment.resolvedDistribution()) {
        PaymentDistribution.ADVANCE -> {
            addResolvedFixedPayment(output, payment, title, rate * totalHours)
        }

        PaymentDistribution.SALARY -> {
            output += ResolvedAdditionalPayment(
                sourceId = payment.id,
                displayName = title,
                amount = roundMoneyCompat(rate * totalHours),
                taxable = payment.taxable,
                withAdvance = false,
                includeInShiftCost = payment.includeInShiftCost,
                sourceTypeName = payment.type
            )
        }

        PaymentDistribution.SPLIT_BY_HALF_MONTH -> {
            val firstAmount = roundMoneyCompat(rate * firstHalfHours)
            val secondAmount = roundMoneyCompat(rate * secondHalfHours)
            if (firstAmount != 0.0) {
                output += ResolvedAdditionalPayment(
                    sourceId = payment.id,
                    displayName = "$title (1-я половина)",
                    amount = firstAmount,
                    taxable = payment.taxable,
                    withAdvance = true,
                    includeInShiftCost = payment.includeInShiftCost,
                    sourceTypeName = payment.type
                )
            }
            if (secondAmount != 0.0) {
                output += ResolvedAdditionalPayment(
                    sourceId = payment.id,
                    displayName = "$title (2-я половина)",
                    amount = secondAmount,
                    taxable = payment.taxable,
                    withAdvance = false,
                    includeInShiftCost = payment.includeInShiftCost,
                    sourceTypeName = payment.type
                )
            }
        }
    }
}

private fun premiumPeriodTextForMonth(month: YearMonth, period: PremiumPeriod, delayMonths: Int): String {
    val baseMonth = month.minusMonths(delayMonths.toLong())
    return when (period) {
        PremiumPeriod.MONTHLY -> "за ${baseMonth.monthValue.toString().padStart(2, '0')}.${baseMonth.year}"
        PremiumPeriod.QUARTERLY -> {
            val quarter = ((baseMonth.monthValue - 1) / 3) + 1
            "за $quarter кв. ${baseMonth.year}"
        }
        PremiumPeriod.HALF_YEARLY -> {
            val half = if (baseMonth.monthValue <= 6) 1 else 2
            "за $half п/г ${baseMonth.year}"
        }
        PremiumPeriod.YEARLY -> "за ${baseMonth.year}"
    }
}

fun resolveAdditionalPaymentsForMonth(
    configuredPayments: List<AdditionalPayment>,
    month: YearMonth,
    shifts: List<WorkShiftItem>,
    firstHalfShifts: List<WorkShiftItem>
): PaymentResolutionSummary {
    val lines = mutableListOf<ResolvedAdditionalPayment>()
    val totalWorkedHours = shifts.filter(::isWorkedShift).sumOf { it.paidHours }
    val firstHalfWorkedHours = firstHalfShifts.filter(::isWorkedShift).sumOf { it.paidHours }

    configuredPayments.filter { it.active }.forEach { payment ->
        when (payment.resolvedType()) {
            AdditionalPaymentType.MONTHLY -> {
                addResolvedFixedPayment(
                    output = lines,
                    payment = payment,
                    displayName = payment.name.ifBlank { "Ежемесячная доплата" },
                    amount = payment.amount
                )
            }

            AdditionalPaymentType.HOURLY -> {
                addResolvedHourlyPayment(
                    output = lines,
                    payment = payment,
                    totalHours = totalWorkedHours,
                    firstHalfHours = firstHalfWorkedHours
                )
            }

            AdditionalPaymentType.ONE_TIME_MONTH -> {
                if (payment.targetMonth == month.toString()) {
                    addResolvedFixedPayment(
                        output = lines,
                        payment = payment,
                        displayName = payment.name.ifBlank { "Разовая выплата" },
                        amount = payment.amount
                    )
                }
            }

            AdditionalPaymentType.PREMIUM -> {
                val period = payment.resolvedPremiumPeriod()
                val baseMonth = month.minusMonths(payment.delayMonths.toLong())
                if (endMonthMatches(period, baseMonth.monthValue)) {
                    val premiumLabel = payment.name.ifBlank { "Премия" }
                    addResolvedFixedPayment(
                        output = lines,
                        payment = payment,
                        displayName = "$premiumLabel ${premiumPeriodTextForMonth(month, period, payment.delayMonths)}",
                        amount = payment.amount
                    )
                }
            }
        }
    }

    return PaymentResolutionSummary(lines = lines.filter { it.amount != 0.0 })
}

fun resolveAdditionalPaymentsForPeriod(
    configuredPayments: List<AdditionalPayment>,
    startDate: LocalDate,
    endDate: LocalDate,
    shifts: List<WorkShiftItem>
): PaymentResolutionSummary {
    if (endDate.isBefore(startDate)) {
        return PaymentResolutionSummary(lines = emptyList())
    }
    val normalizedShifts = shifts.filter { it.date != null }
    val shiftsByMonth = normalizedShifts.groupBy { YearMonth.from(it.date!!) }
    val lines = mutableListOf<ResolvedAdditionalPayment>()

    var month = YearMonth.from(startDate)
    val endMonth = YearMonth.from(endDate)
    while (!month.isAfter(endMonth)) {
        val monthShifts = shiftsByMonth[month].orEmpty()
        val monthFirstHalfShifts = monthShifts.filter { (it.date?.dayOfMonth ?: 0) <= 15 }
        lines += resolveAdditionalPaymentsForMonth(
            configuredPayments = configuredPayments,
            month = month,
            shifts = monthShifts,
            firstHalfShifts = monthFirstHalfShifts
        ).lines
        month = month.plusMonths(1)
    }

    return PaymentResolutionSummary(lines = lines.filter { it.amount != 0.0 })
}

data class DetailedShiftStats(
    val totalAssignedDays: Int,
    val workedShiftCount: Int,
    val firstHalfAssignedDays: Int,
    val secondHalfAssignedDays: Int,
    val firstHalfWorkedShifts: Int,
    val secondHalfWorkedShifts: Int,
    val firstHalfWorkedHours: Double,
    val secondHalfWorkedHours: Double,
    val firstHalfNightHours: Double,
    val secondHalfNightHours: Double,
    val dayShiftCount: Int,
    val nightShiftCount: Int,
    val weekendHolidayShiftCount: Int,
    val eightHourShiftCount: Int,
    val vacationShiftCount: Int,
    val sickShiftCount: Int,
    val shiftCostBaseTotal: Double,
    val shiftCostIncludedPayments: Double,
    val shiftCostAverageGross: Double,
    val shiftCostAverageNet: Double,
    val dayShiftCostAverageGross: Double,
    val dayShiftCostAverageNet: Double,
    val nightShiftCostAverageGross: Double,
    val nightShiftCostAverageNet: Double
) {
    val shiftCostAverage: Double get() = shiftCostAverageGross
    val dayShiftCostAverage: Double get() = dayShiftCostAverageGross
    val nightShiftCostAverage: Double get() = nightShiftCostAverageGross
}

fun calculateDetailedShiftStats(
    shifts: List<WorkShiftItem>,
    firstHalfShifts: List<WorkShiftItem>,
    paymentResolution: PaymentResolutionSummary,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult
): DetailedShiftStats {
    val workedShifts = shifts.filter(::isWorkedShift)
    val firstHalfWorked = firstHalfShifts.filter(::isWorkedShift)
    val secondHalfWorkedCount = (workedShifts.size - firstHalfWorked.size).coerceAtLeast(0)

    val totalWorkedHours = roundMoneyCompat(workedShifts.sumOf { it.paidHours })
    val firstHalfWorkedHours = roundMoneyCompat(firstHalfWorked.sumOf { it.paidHours })
    val secondHalfWorkedHours = roundMoneyCompat((totalWorkedHours - firstHalfWorkedHours).coerceAtLeast(0.0))

    val totalNightHours = roundMoneyCompat(workedShifts.sumOf { it.nightHours })
    val firstHalfNightHours = roundMoneyCompat(firstHalfWorked.sumOf { it.nightHours })
    val secondHalfNightHours = roundMoneyCompat((totalNightHours - firstHalfNightHours).coerceAtLeast(0.0))

    val includedInShiftCostTotal = roundMoneyCompat(paymentResolution.includedInShiftCostTotal)
    val includedTaxableInShiftCostTotal = roundMoneyCompat(
        paymentResolution.lines
            .filter { it.includeInShiftCost && it.taxable }
            .sumOf { it.amount }
    )
    val includedPerShift = if (workedShifts.isEmpty()) 0.0 else includedInShiftCostTotal / workedShifts.size
    val includedTaxablePerShift = if (workedShifts.isEmpty()) 0.0 else includedTaxableInShiftCostTotal / workedShifts.size
    val nightExtraPerHour = if (totalNightHours > 0.0) payroll.nightExtra / totalNightHours else 0.0
    val effectiveTaxRate = if (payroll.taxableGrossTotal > 0.0) {
        (payroll.ndfl / payroll.taxableGrossTotal).coerceIn(0.0, 1.0)
    } else {
        0.0
    }

    val shiftCostBaseTotal = roundMoneyCompat(
        payroll.basePay +
                payroll.nightExtra +
                payroll.holidayExtra +
                annualOvertime.overtimePremiumAmount +
                includedInShiftCostTotal
    )
    val shiftCostTaxableTotal = roundMoneyCompat(
        payroll.basePay +
                payroll.nightExtra +
                payroll.holidayExtra +
                annualOvertime.overtimePremiumAmount +
                includedTaxableInShiftCostTotal
    )
    val shiftCostNetTotal = roundMoneyCompat(shiftCostBaseTotal - (shiftCostTaxableTotal * effectiveTaxRate))

    val averageGross = if (workedShifts.isEmpty()) 0.0 else roundMoneyCompat(shiftCostBaseTotal / workedShifts.size)
    val averageNet = if (workedShifts.isEmpty()) 0.0 else roundMoneyCompat(shiftCostNetTotal / workedShifts.size)

    fun averageShiftCostFor(source: List<WorkShiftItem>, net: Boolean): Double {
        if (source.isEmpty()) return 0.0
        val total = source.sumOf { shift ->
            val base = payroll.hourlyRate * shift.paidHours
            val night = if (shift.nightHours > 0.0) nightExtraPerHour * shift.nightHours else 0.0
            val gross = base + night + includedPerShift
            if (!net) {
                gross
            } else {
                val taxablePart = base + night + includedTaxablePerShift
                gross - (taxablePart * effectiveTaxRate)
            }
        }
        return roundMoneyCompat(total / source.size)
    }

    val regularWorkedShifts = workedShifts.filter { it.specialDayType == "NONE" }
    val regularDayShifts = regularWorkedShifts.filter { it.nightHours <= 0.0 }
        .ifEmpty { workedShifts.filter { it.nightHours <= 0.0 } }
    val regularNightShifts = regularWorkedShifts.filter { it.nightHours > 0.0 }
        .ifEmpty { workedShifts.filter { it.nightHours > 0.0 } }

    return DetailedShiftStats(
        totalAssignedDays = shifts.size,
        workedShiftCount = workedShifts.size,
        firstHalfAssignedDays = firstHalfShifts.size,
        secondHalfAssignedDays = (shifts.size - firstHalfShifts.size).coerceAtLeast(0),
        firstHalfWorkedShifts = firstHalfWorked.size,
        secondHalfWorkedShifts = secondHalfWorkedCount,
        firstHalfWorkedHours = firstHalfWorkedHours,
        secondHalfWorkedHours = secondHalfWorkedHours,
        firstHalfNightHours = firstHalfNightHours,
        secondHalfNightHours = secondHalfNightHours,
        dayShiftCount = workedShifts.count { it.nightHours <= 0.0 },
        nightShiftCount = workedShifts.count { it.nightHours > 0.0 },
        weekendHolidayShiftCount = workedShifts.count { it.specialDayType != "NONE" },
        eightHourShiftCount = workedShifts.count { kotlin.math.abs(it.paidHours - 8.0) < 0.01 },
        vacationShiftCount = shifts.count { it.isVacation },
        sickShiftCount = shifts.count { it.isSickLeave },
        shiftCostBaseTotal = shiftCostBaseTotal,
        shiftCostIncludedPayments = includedInShiftCostTotal,
        shiftCostAverageGross = averageGross,
        shiftCostAverageNet = averageNet,
        dayShiftCostAverageGross = averageShiftCostFor(regularDayShifts, net = false),
        dayShiftCostAverageNet = averageShiftCostFor(regularDayShifts, net = true),
        nightShiftCostAverageGross = averageShiftCostFor(regularNightShifts, net = false),
        nightShiftCostAverageNet = averageShiftCostFor(regularNightShifts, net = true)
    )
}
