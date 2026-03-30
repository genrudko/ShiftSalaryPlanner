package com.vigilante.shiftsalaryplanner.payroll

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.round

enum class PayMode {
    HOURLY,
    MONTHLY_SALARY
}

enum class ExtraSalaryMode {
    INCLUDED_IN_RATE,
    FIXED_MONTHLY
}

enum class AdvanceMode {
    ACTUAL_EARNINGS,
    FIXED_PERCENT
}

enum class AnnualNormSourceMode {
    WORKDAY_HOURS,
    YEAR_TOTAL_HOURS
}

enum class OvertimePeriod {
    MONTH,
    QUARTER,
    HALF_YEAR,
    YEAR
}

enum class SpecialDayType {
    NONE,
    WEEKEND_HOLIDAY,
    RVD
}

enum class SpecialDayCompensation {
    NONE,
    DOUBLE_PAY,
    SINGLE_PAY_WITH_DAY_OFF
}

data class PayrollSettings(
    val baseSalary: Double = 102050.0,
    val extraSalary: Double = 49733.0,
    val housingPayment: Double = 0.0,
    val housingPaymentLabel: String = "Выплата на квартиру",
    val housingPaymentTaxable: Boolean = true,
    val housingPaymentWithAdvance: Boolean = false,
    val monthlyNormHours: Double = 165.0,
    val normMode: String = NormMode.MANUAL.name,
    val workdayHours: Double = 8.0,
    val annualNormSourceMode: String = AnnualNormSourceMode.WORKDAY_HOURS.name,
    val annualNormHours: Double = 1970.0,
    val payMode: String = PayMode.HOURLY.name,
    val extraSalaryMode: String = ExtraSalaryMode.INCLUDED_IN_RATE.name,
    val nightPercent: Double = 0.4,
    val holidayRateMultiplier: Double = 2.0,
    val ndflPercent: Double = 0.13,
    val vacationAverageDaily: Double = 0.0,
    val vacationAccruals12Months: Double = 0.0,
    val sickAverageDaily: Double = 0.0,
    val sickIncomeYear1: Double = 0.0,
    val sickIncomeYear2: Double = 0.0,
    val sickLimitYear1: Double = 0.0,
    val sickLimitYear2: Double = 0.0,
    val sickCalculationPeriodDays: Int = 730,
    val sickExcludedDays: Int = 0,
    val sickPayPercent: Double = 1.0,
    val sickMaxDailyAmount: Double = 6827.40,
    val progressiveNdflEnabled: Boolean = false,
    val taxableIncomeYtdBeforeCurrentMonth: Double = 0.0,
    val advanceMode: String = AdvanceMode.ACTUAL_EARNINGS.name,
    val advancePercent: Double = 50.0,
    val advanceDay: Int = 20,
    val salaryDay: Int = 5,
    val movePaymentsToPreviousWorkday: Boolean = true,
    val applyShortDayReduction: Boolean = false,
    val overtimeEnabled: Boolean = true,
    val overtimePeriod: String = OvertimePeriod.YEAR.name,
    val excludeWeekendHolidayFromOvertime: Boolean = true,
    val excludeRvdDoublePayFromOvertime: Boolean = true,
    val excludeRvdSingleWithDayOffFromOvertime: Boolean = false
)

data class WorkShiftItem(
    val paidHours: Double,
    val nightHours: Double,
    val isWeekendPaid: Boolean,
    val date: LocalDate? = null,
    val specialDayType: String = if (isWeekendPaid) SpecialDayType.WEEKEND_HOLIDAY.name else SpecialDayType.NONE.name,
    val specialDayCompensation: String = if (isWeekendPaid) SpecialDayCompensation.DOUBLE_PAY.name else SpecialDayCompensation.NONE.name,
    val isVacation: Boolean = false,
    val isSickLeave: Boolean = false
)

data class PayrollResult(
    val workedHours: Double,
    val nightHours: Double,
    val holidayHours: Double,
    val hourlyRate: Double,
    val basePay: Double,
    val nightExtra: Double,
    val holidayExtra: Double,
    val vacationDays: Int,
    val vacationPay: Double,
    val sickDays: Int,
    val sickPay: Double,
    val housingPayment: Double,
    val housingPaymentTaxable: Boolean,
    val housingAdvancePart: Double,
    val housingSalaryPart: Double,
    val additionalPaymentsTotal: Double,
    val additionalPaymentsAdvancePart: Double,
    val additionalPaymentsSalaryPart: Double,
    val additionalPaymentsTaxablePart: Double,
    val additionalPaymentsNonTaxablePart: Double,
    val taxableGrossTotal: Double,
    val nonTaxableTotal: Double,
    val grossTotal: Double,
    val ndfl: Double,
    val netTotal: Double,
    val advanceAmount: Double,
    val salaryPaymentAmount: Double,
    val shiftOnlyAdvanceNetAmount: Double,
    val shiftOnlySalaryNetAmount: Double,
    val deductionsTotal: Double,
    val deductionsAdvancePart: Double,
    val deductionsSalaryPart: Double,
    val alimonyAmount: Double,
    val enforcementAmount: Double,
    val otherDeductionsAmount: Double,
    val netAdvanceAfterDeductions: Double,
    val netSalaryAfterDeductions: Double,
    val netAfterDeductions: Double,
    val shiftOnlyAdvanceNetAfterDeductions: Double,
    val shiftOnlySalaryNetAfterDeductions: Double,
    val taxableIncomeYtdBeforeCurrentMonth: Double,
    val taxableIncomeYtdAfterCurrentMonth: Double
)


data class AnnualOvertimeResult(
    val enabled: Boolean,
    val periodLabel: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val year: Int,
    val annualNormHours: Double,
    val workedHours: Double,
    val holidayExcludedHours: Double,
    val rawOvertimeHours: Double,
    val payableOvertimeHours: Double,
    val firstTwoHours: Double,
    val remainingHours: Double,
    val hourlyRate: Double,
    val overtimePremiumAmount: Double
)

data class PaymentDates(
    val advanceDate: LocalDate,
    val salaryDate: LocalDate
)

private data class PayrollPart(
    val workedHours: Double,
    val nightHours: Double,
    val holidayHours: Double,
    val hourlyRate: Double,
    val basePay: Double,
    val nightExtra: Double,
    val holidayExtra: Double,
    val vacationDays: Int,
    val vacationPay: Double,
    val sickDays: Int,
    val sickPay: Double,
    val gross: Double
)

object PayrollCalculator {

    fun calculate(
        shifts: List<WorkShiftItem>,
        firstHalfShifts: List<WorkShiftItem>,
        settings: PayrollSettings,
        additionalPayments: List<AdditionalPayment>,
        deductions: List<PayrollDeduction> = emptyList()
    ): PayrollResult {
        val safeSettings = settings.sanitized()

        val totalPart = calculatePart(
            shifts = shifts,
            settings = safeSettings
        )

        val firstHalfPart = calculatePart(
            shifts = firstHalfShifts,
            settings = safeSettings
        )

        val activePayments = additionalPayments.filter { it.active }

        val additionalPaymentsTotal = roundMoney(activePayments.sumOf { it.amount })
        val additionalPaymentsAdvancePart = roundMoney(
            activePayments
                .filter { it.withAdvance }
                .sumOf { it.amount }
        )
        val additionalPaymentsSalaryPart = roundMoney(
            activePayments
                .filterNot { it.withAdvance }
                .sumOf { it.amount }
        )
        val additionalPaymentsTaxablePart = roundMoney(
            activePayments
                .filter { it.taxable }
                .sumOf { it.amount }
        )
        val additionalPaymentsNonTaxablePart = roundMoney(
            activePayments
                .filterNot { it.taxable }
                .sumOf { it.amount }
        )

        val housingAdvancePart = roundMoney(
            if (safeSettings.housingPaymentWithAdvance) safeSettings.housingPayment else 0.0
        )
        val housingSalaryPart = roundMoney(
            if (safeSettings.housingPaymentWithAdvance) 0.0 else safeSettings.housingPayment
        )

        val taxableHousing = roundMoney(
            if (safeSettings.housingPaymentTaxable) safeSettings.housingPayment else 0.0
        )
        val nonTaxableHousing = roundMoney(
            if (safeSettings.housingPaymentTaxable) 0.0 else safeSettings.housingPayment
        )

        val taxableGrossTotal = roundMoney(
            totalPart.gross + taxableHousing + additionalPaymentsTaxablePart
        )
        val nonTaxableTotal = roundMoney(
            nonTaxableHousing + additionalPaymentsNonTaxablePart
        )
        val grossTotal = roundMoney(taxableGrossTotal + nonTaxableTotal)

        val taxableIncomeYtdBeforeCurrentMonth = roundMoney(safeSettings.taxableIncomeYtdBeforeCurrentMonth)
        val taxableIncomeYtdAfterCurrentMonth = roundMoney(taxableIncomeYtdBeforeCurrentMonth + taxableGrossTotal)

        val ndfl = roundMoney(
            if (safeSettings.progressiveNdflEnabled) {
                calculateProgressiveNdfl(taxableIncomeYtdAfterCurrentMonth) -
                        calculateProgressiveNdfl(taxableIncomeYtdBeforeCurrentMonth)
            } else {
                taxableGrossTotal * safeSettings.ndflPercent
            }
        )
        val netTotal = roundMoney(grossTotal - ndfl)

        val advanceMode = runCatching { AdvanceMode.valueOf(safeSettings.advanceMode) }
            .getOrElse { AdvanceMode.ACTUAL_EARNINGS }

        val advanceBaseAmount = when (advanceMode) {
            AdvanceMode.ACTUAL_EARNINGS -> firstHalfPart.gross
            AdvanceMode.FIXED_PERCENT -> {
                totalPart.gross * (safeSettings.advancePercent / 100.0).coerceIn(0.0, 1.0)
            }
        }

        val advanceAmount = roundMoney(
            advanceBaseAmount + housingAdvancePart + additionalPaymentsAdvancePart
        )
        val salaryPaymentAmount = roundMoney(max(0.0, netTotal - advanceAmount))

        val totalShiftOnlyGross = roundMoney(totalPart.basePay + totalPart.nightExtra + totalPart.holidayExtra)
        val firstHalfShiftOnlyGross = roundMoney(firstHalfPart.basePay + firstHalfPart.nightExtra + firstHalfPart.holidayExtra)
        val shiftOnlyAdvanceNetAmount = roundMoney(
            calculateStandaloneNetAmount(
                taxableGross = firstHalfShiftOnlyGross,
                settings = safeSettings,
                taxableIncomeYtdBeforeCurrentMonth = safeSettings.taxableIncomeYtdBeforeCurrentMonth
            )
        )
        val shiftOnlySalaryNetAmount = roundMoney(
            max(
                0.0,
                calculateStandaloneNetAmount(
                    taxableGross = totalShiftOnlyGross,
                    settings = safeSettings,
                    taxableIncomeYtdBeforeCurrentMonth = safeSettings.taxableIncomeYtdBeforeCurrentMonth
                ) - shiftOnlyAdvanceNetAmount
            )
        )

        val deductionResult = DeductionCalculator.calculate(
            netAdvanceBase = advanceAmount,
            netSalaryBase = salaryPaymentAmount,
            deductions = deductions
        )
        val shiftOnlyDeductionResult = DeductionCalculator.calculate(
            netAdvanceBase = shiftOnlyAdvanceNetAmount,
            netSalaryBase = shiftOnlySalaryNetAmount,
            deductions = deductions
        )

        return PayrollResult(
            workedHours = totalPart.workedHours,
            nightHours = totalPart.nightHours,
            holidayHours = totalPart.holidayHours,
            hourlyRate = totalPart.hourlyRate,
            basePay = totalPart.basePay,
            nightExtra = totalPart.nightExtra,
            holidayExtra = totalPart.holidayExtra,
            vacationDays = totalPart.vacationDays,
            vacationPay = totalPart.vacationPay,
            sickDays = totalPart.sickDays,
            sickPay = totalPart.sickPay,
            housingPayment = roundMoney(safeSettings.housingPayment),
            housingPaymentTaxable = safeSettings.housingPaymentTaxable,
            housingAdvancePart = housingAdvancePart,
            housingSalaryPart = housingSalaryPart,
            additionalPaymentsTotal = additionalPaymentsTotal,
            additionalPaymentsAdvancePart = additionalPaymentsAdvancePart,
            additionalPaymentsSalaryPart = additionalPaymentsSalaryPart,
            additionalPaymentsTaxablePart = additionalPaymentsTaxablePart,
            additionalPaymentsNonTaxablePart = additionalPaymentsNonTaxablePart,
            taxableGrossTotal = taxableGrossTotal,
            nonTaxableTotal = nonTaxableTotal,
            grossTotal = grossTotal,
            ndfl = ndfl,
            netTotal = netTotal,
            advanceAmount = advanceAmount,
            salaryPaymentAmount = salaryPaymentAmount,
            shiftOnlyAdvanceNetAmount = shiftOnlyAdvanceNetAmount,
            shiftOnlySalaryNetAmount = shiftOnlySalaryNetAmount,
            deductionsTotal = deductionResult.deductionsTotal,
            deductionsAdvancePart = deductionResult.deductionsAdvancePart,
            deductionsSalaryPart = deductionResult.deductionsSalaryPart,
            alimonyAmount = deductionResult.alimonyAmount,
            enforcementAmount = deductionResult.enforcementAmount,
            otherDeductionsAmount = deductionResult.otherDeductionsAmount,
            netAdvanceAfterDeductions = deductionResult.netAdvanceAfterDeductions,
            netSalaryAfterDeductions = deductionResult.netSalaryAfterDeductions,
            netAfterDeductions = deductionResult.netAfterDeductions,
            shiftOnlyAdvanceNetAfterDeductions = shiftOnlyDeductionResult.netAdvanceAfterDeductions,
            shiftOnlySalaryNetAfterDeductions = shiftOnlyDeductionResult.netSalaryAfterDeductions,
            taxableIncomeYtdBeforeCurrentMonth = taxableIncomeYtdBeforeCurrentMonth,
            taxableIncomeYtdAfterCurrentMonth = taxableIncomeYtdAfterCurrentMonth
        )
    }



    fun calculatePeriodOvertime(
        shifts: List<WorkShiftItem>,
        settings: PayrollSettings,
        periodLabel: String,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        periodNormHours: Double
    ): AnnualOvertimeResult {
        val safeSettings = settings.sanitized()
        val safePeriodNormHours = periodNormHours.coerceAtLeast(0.0)

        val normalizedShifts = shifts.map { shift ->
            val safePaidHours = shift.paidHours.coerceAtLeast(0.0)
            val safeNightHours = shift.nightHours.coerceIn(0.0, safePaidHours)
            shift.copy(
                paidHours = safePaidHours,
                nightHours = safeNightHours
            )
        }

        val workedShifts = normalizedShifts.filterNot { it.isVacation || it.isSickLeave }
        val workedHours = workedShifts.sumOf { it.paidHours }
        val excludedHours = workedShifts
            .filter { shouldExcludeFromOvertime(it, safeSettings) }
            .sumOf { it.paidHours }

        val rawOvertimeHours = max(0.0, workedHours - safePeriodNormHours)
        val payableOvertimeHours = if (safeSettings.overtimeEnabled) {
            max(0.0, rawOvertimeHours - excludedHours)
        } else {
            0.0
        }
        val firstTwoHours = minOf(2.0, payableOvertimeHours)
        val remainingHours = max(0.0, payableOvertimeHours - firstTwoHours)
        val hourlyRate = calculateBaseHourlyRate(safeSettings)

        val payMode = runCatching { PayMode.valueOf(safeSettings.payMode) }
            .getOrElse { PayMode.HOURLY }

        val overtimePremiumAmount = if (!safeSettings.overtimeEnabled) {
            0.0
        } else {
            when (payMode) {
                PayMode.HOURLY -> {
                    firstTwoHours * hourlyRate * 0.5 + remainingHours * hourlyRate * 1.0
                }
                PayMode.MONTHLY_SALARY -> {
                    firstTwoHours * hourlyRate * 1.5 + remainingHours * hourlyRate * 2.0
                }
            }
        }

        return AnnualOvertimeResult(
            enabled = safeSettings.overtimeEnabled,
            periodLabel = periodLabel,
            periodStart = periodStart,
            periodEnd = periodEnd,
            year = periodEnd.year,
            annualNormHours = roundMoney(safePeriodNormHours),
            workedHours = roundMoney(workedHours),
            holidayExcludedHours = roundMoney(excludedHours),
            rawOvertimeHours = roundMoney(rawOvertimeHours),
            payableOvertimeHours = roundMoney(payableOvertimeHours),
            firstTwoHours = roundMoney(firstTwoHours),
            remainingHours = roundMoney(remainingHours),
            hourlyRate = roundMoney(hourlyRate),
            overtimePremiumAmount = roundMoney(overtimePremiumAmount)
        )
    }

    private fun calculateStandaloneNetAmount(
        taxableGross: Double,
        settings: PayrollSettings,
        taxableIncomeYtdBeforeCurrentMonth: Double
    ): Double {
        val safeTaxableGross = taxableGross.coerceAtLeast(0.0)
        val ndfl = roundMoney(
            if (settings.progressiveNdflEnabled) {
                calculateProgressiveNdfl(taxableIncomeYtdBeforeCurrentMonth + safeTaxableGross) -
                        calculateProgressiveNdfl(taxableIncomeYtdBeforeCurrentMonth)
            } else {
                safeTaxableGross * settings.ndflPercent
            }
        )
        return roundMoney((safeTaxableGross - ndfl).coerceAtLeast(0.0))
    }

    private fun calculatePart(
        shifts: List<WorkShiftItem>,
        settings: PayrollSettings
    ): PayrollPart {
        val normalizedShifts = shifts.map { shift ->
            val safePaidHours = shift.paidHours.coerceAtLeast(0.0)
            val safeNightHours = shift.nightHours.coerceIn(0.0, safePaidHours)

            shift.copy(
                paidHours = safePaidHours,
                nightHours = safeNightHours
            )
        }

        val workedShifts = normalizedShifts.filterNot { it.isVacation || it.isSickLeave }
        val workedHours = workedShifts.sumOf { it.paidHours }
        val nightHours = workedShifts.sumOf { it.nightHours }

        val holidayHours = workedShifts
            .filter { isPaidAtHolidayMultiplier(it) }
            .sumOf { it.paidHours }

        val vacationDays = normalizedShifts.count { it.isVacation }
        val vacationPay = roundMoney(vacationDays * settings.vacationAverageDaily.coerceAtLeast(0.0))

        val sickDays = normalizedShifts.count { it.isSickLeave }
        val sickDailyAmount = minOf(
            settings.sickAverageDaily.coerceAtLeast(0.0) * settings.sickPayPercent.coerceAtLeast(0.0),
            settings.sickMaxDailyAmount.coerceAtLeast(0.0)
        )
        val sickPay = roundMoney(sickDays * sickDailyAmount)

        val extraSalaryMode = runCatching { ExtraSalaryMode.valueOf(settings.extraSalaryMode) }
            .getOrElse { ExtraSalaryMode.INCLUDED_IN_RATE }

        val hourlyRate = calculateBaseHourlyRate(settings)

        val payMode = runCatching { PayMode.valueOf(settings.payMode) }
            .getOrElse { PayMode.HOURLY }

        val workRatio = when {
            settings.monthlyNormHours > 0.0 -> (workedHours / settings.monthlyNormHours).coerceIn(0.0, 1.0)
            workedHours > 0.0 -> 1.0
            else -> 0.0
        }

        val fixedExtraPay = when (extraSalaryMode) {
            ExtraSalaryMode.INCLUDED_IN_RATE -> 0.0
            ExtraSalaryMode.FIXED_MONTHLY -> settings.extraSalary.coerceAtLeast(0.0) * workRatio
        }

        val basePay = when (payMode) {
            PayMode.HOURLY -> {
                workedHours * hourlyRate + fixedExtraPay
            }

            PayMode.MONTHLY_SALARY -> {
                calculateMonthlySalaryBasePay(
                    monthlySalary = settings.baseSalary.coerceAtLeast(0.0),
                    normHours = settings.monthlyNormHours,
                    workedHours = workedHours
                ) + fixedExtraPay
            }
        }

        val nightExtra = nightHours * hourlyRate * settings.nightPercent
        val holidayExtra = holidayHours * hourlyRate * max(0.0, settings.holidayRateMultiplier - 1.0)
        val gross = basePay + nightExtra + holidayExtra + vacationPay + sickPay

        return PayrollPart(
            workedHours = workedHours,
            nightHours = nightHours,
            holidayHours = holidayHours,
            hourlyRate = roundMoney(hourlyRate),
            basePay = roundMoney(basePay),
            nightExtra = roundMoney(nightExtra),
            holidayExtra = roundMoney(holidayExtra),
            vacationDays = vacationDays,
            vacationPay = vacationPay,
            sickDays = sickDays,
            sickPay = sickPay,
            gross = roundMoney(gross)
        )
    }



    private fun calculateBaseHourlyRate(settings: PayrollSettings): Double {
        val extraSalaryMode = runCatching { ExtraSalaryMode.valueOf(settings.extraSalaryMode) }
            .getOrElse { ExtraSalaryMode.INCLUDED_IN_RATE }

        val baseSalaryForRate = settings.baseSalary + if (extraSalaryMode == ExtraSalaryMode.INCLUDED_IN_RATE) {
            settings.extraSalary
        } else {
            0.0
        }

        return if (settings.monthlyNormHours > 0.0) {
            baseSalaryForRate / settings.monthlyNormHours
        } else {
            0.0
        }
    }
    private fun calculateMonthlySalaryBasePay(
        monthlySalary: Double,
        normHours: Double,
        workedHours: Double
    ): Double {
        if (monthlySalary <= 0.0) return 0.0
        if (workedHours <= 0.0) return 0.0

        return when {
            normHours <= 0.0 -> monthlySalary
            else -> {
                val ratio = (workedHours / normHours).coerceIn(0.0, 1.0)
                monthlySalary * ratio
            }
        }
    }
}

fun calculatePaymentDates(
    month: YearMonth,
    settings: PayrollSettings,
    extraDayOffDates: Set<LocalDate> = emptySet()
): PaymentDates {
    val advanceBaseDate = month.atDay(settings.advanceDay.coerceIn(1, month.lengthOfMonth()))

    val nextMonth = month.plusMonths(1)
    val salaryBaseDate = nextMonth.atDay(settings.salaryDay.coerceIn(1, nextMonth.lengthOfMonth()))

    return PaymentDates(
        advanceDate = moveToPreviousWorkdayIfNeeded(
            advanceBaseDate,
            settings.movePaymentsToPreviousWorkday,
            extraDayOffDates
        ),
        salaryDate = moveToPreviousWorkdayIfNeeded(
            salaryBaseDate,
            settings.movePaymentsToPreviousWorkday,
            extraDayOffDates
        )
    )
}

private fun moveToPreviousWorkdayIfNeeded(
    date: LocalDate,
    enabled: Boolean,
    extraDayOffDates: Set<LocalDate> = emptySet()
): LocalDate {
    if (!enabled) return date

    var currentDate = date
    while (
        currentDate.dayOfWeek == DayOfWeek.SATURDAY ||
        currentDate.dayOfWeek == DayOfWeek.SUNDAY ||
        currentDate in extraDayOffDates
    ) {
        currentDate = currentDate.minusDays(1)
    }
    return currentDate
}

private fun PayrollSettings.sanitized(): PayrollSettings {
    return copy(
        baseSalary = baseSalary.coerceAtLeast(0.0),
        extraSalary = extraSalary.coerceAtLeast(0.0),
        housingPayment = housingPayment.coerceAtLeast(0.0),
        monthlyNormHours = monthlyNormHours.coerceAtLeast(0.0),
        workdayHours = workdayHours.coerceAtLeast(0.0),
        annualNormHours = annualNormHours.coerceAtLeast(0.0),
        nightPercent = nightPercent.coerceAtLeast(0.0),
        holidayRateMultiplier = holidayRateMultiplier.coerceAtLeast(1.0),
        ndflPercent = ndflPercent.coerceIn(0.0, 1.0),
        vacationAverageDaily = vacationAverageDaily.coerceAtLeast(0.0),
        vacationAccruals12Months = vacationAccruals12Months.coerceAtLeast(0.0),
        sickAverageDaily = sickAverageDaily.coerceAtLeast(0.0),
        sickIncomeYear1 = sickIncomeYear1.coerceAtLeast(0.0),
        sickIncomeYear2 = sickIncomeYear2.coerceAtLeast(0.0),
        sickLimitYear1 = sickLimitYear1.coerceAtLeast(0.0),
        sickLimitYear2 = sickLimitYear2.coerceAtLeast(0.0),
        sickCalculationPeriodDays = sickCalculationPeriodDays.coerceAtLeast(0),
        sickExcludedDays = sickExcludedDays.coerceAtLeast(0),
        sickPayPercent = sickPayPercent.coerceAtLeast(0.0),
        sickMaxDailyAmount = sickMaxDailyAmount.coerceAtLeast(0.0),
        taxableIncomeYtdBeforeCurrentMonth = taxableIncomeYtdBeforeCurrentMonth.coerceAtLeast(0.0),
        advancePercent = advancePercent.coerceIn(0.0, 100.0)
    )
}

private fun isPaidAtHolidayMultiplier(shift: WorkShiftItem): Boolean {
    return when (runCatching { SpecialDayType.valueOf(shift.specialDayType) }.getOrElse { if (shift.isWeekendPaid) SpecialDayType.WEEKEND_HOLIDAY else SpecialDayType.NONE }) {
        SpecialDayType.WEEKEND_HOLIDAY -> true
        SpecialDayType.RVD -> runCatching { SpecialDayCompensation.valueOf(shift.specialDayCompensation) }
            .getOrElse { if (shift.isWeekendPaid) SpecialDayCompensation.DOUBLE_PAY else SpecialDayCompensation.NONE } == SpecialDayCompensation.DOUBLE_PAY
        SpecialDayType.NONE -> false
    }
}

private fun shouldExcludeFromOvertime(
    shift: WorkShiftItem,
    settings: PayrollSettings
): Boolean {
    return when (runCatching { SpecialDayType.valueOf(shift.specialDayType) }.getOrElse { if (shift.isWeekendPaid) SpecialDayType.WEEKEND_HOLIDAY else SpecialDayType.NONE }) {
        SpecialDayType.WEEKEND_HOLIDAY -> settings.excludeWeekendHolidayFromOvertime
        SpecialDayType.RVD -> when (runCatching { SpecialDayCompensation.valueOf(shift.specialDayCompensation) }.getOrElse { if (shift.isWeekendPaid) SpecialDayCompensation.DOUBLE_PAY else SpecialDayCompensation.NONE }) {
            SpecialDayCompensation.DOUBLE_PAY -> settings.excludeRvdDoublePayFromOvertime
            SpecialDayCompensation.SINGLE_PAY_WITH_DAY_OFF -> settings.excludeRvdSingleWithDayOffFromOvertime
            SpecialDayCompensation.NONE -> false
        }
        SpecialDayType.NONE -> false
    }
}

private fun roundMoney(value: Double): Double {
    return round(value * 100.0) / 100.0
}


fun calculateVacationAverageDailyFromAccruals(accruals12Months: Double): Double {
    val safeAccruals = accruals12Months.coerceAtLeast(0.0)
    if (safeAccruals <= 0.0) return 0.0
    return roundMoney(safeAccruals / 12.0 / 29.3)
}

fun calculateDefaultSickCalculationPeriodDays(referenceYear: Int): Int {
    return Year.of(referenceYear - 2).length() + Year.of(referenceYear - 1).length()
}

fun calculateSickAverageDailyFromInputs(
    incomeYear1: Double,
    incomeYear2: Double,
    limitYear1: Double,
    limitYear2: Double,
    calculationPeriodDays: Int,
    excludedDays: Int
): Double {
    val safeIncomeYear1 = incomeYear1.coerceAtLeast(0.0)
    val safeIncomeYear2 = incomeYear2.coerceAtLeast(0.0)
    val safeLimitYear1 = limitYear1.coerceAtLeast(0.0)
    val safeLimitYear2 = limitYear2.coerceAtLeast(0.0)
    val safeCalculationDays = calculationPeriodDays.coerceAtLeast(1)
    val safeExcludedDays = excludedDays.coerceAtLeast(0).coerceAtMost(safeCalculationDays - 1)

    val countedIncomeYear1 = if (safeLimitYear1 > 0.0) minOf(safeIncomeYear1, safeLimitYear1) else safeIncomeYear1
    val countedIncomeYear2 = if (safeLimitYear2 > 0.0) minOf(safeIncomeYear2, safeLimitYear2) else safeIncomeYear2
    val effectiveDays = (safeCalculationDays - safeExcludedDays).coerceAtLeast(1)

    return roundMoney((countedIncomeYear1 + countedIncomeYear2) / effectiveDays.toDouble())
}
fun calculateNdflForTaxableSegment(
    taxableIncomeYtdBeforeSegment: Double,
    taxableSegmentAmount: Double,
    progressiveNdflEnabled: Boolean,
    flatRate: Double
): Double {
    val safeBefore = taxableIncomeYtdBeforeSegment.coerceAtLeast(0.0)
    val safeAmount = taxableSegmentAmount.coerceAtLeast(0.0)
    if (safeAmount <= 0.0) return 0.0

    return roundMoney(
        if (progressiveNdflEnabled) {
            calculateProgressiveNdfl(safeBefore + safeAmount) - calculateProgressiveNdfl(safeBefore)
        } else {
            safeAmount * flatRate.coerceIn(0.0, 1.0)
        }
    )
}
private fun calculateProgressiveNdfl(taxableIncome: Double): Double {
    if (taxableIncome <= 0.0) return 0.0

    var remaining = taxableIncome
    var tax = 0.0

    fun applyBracket(limit: Double, rate: Double) {
        if (remaining <= 0.0) return
        val amount = minOf(remaining, limit)
        tax += amount * rate
        remaining -= amount
    }

    applyBracket(2_400_000.0, 0.13)
    applyBracket(5_000_000.0 - 2_400_000.0, 0.15)
    applyBracket(20_000_000.0 - 5_000_000.0, 0.18)
    applyBracket(50_000_000.0 - 20_000_000.0, 0.20)
    if (remaining > 0.0) tax += remaining * 0.22

    return roundMoney(tax)
}
