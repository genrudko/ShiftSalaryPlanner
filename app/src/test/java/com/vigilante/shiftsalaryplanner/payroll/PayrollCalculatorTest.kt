package com.vigilante.shiftsalaryplanner.payroll

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

class PayrollCalculatorTest {

    @Test
    fun calculate_keepsFinancialTotalsConsistent() {
        val settings = PayrollSettings(
            baseSalary = 100_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 100.0,
            payMode = PayMode.HOURLY.name,
            ndflPercent = 0.13,
            housingPayment = 1_000.0,
            housingPaymentTaxable = false,
            housingPaymentWithAdvance = true,
            advanceMode = AdvanceMode.ACTUAL_EARNINGS.name
        )
        val shifts = listOf(
            WorkShiftItem(
                paidHours = 10.0,
                nightHours = 2.0,
                isWeekendPaid = false
            )
        )
        val additionalPayments = listOf(
            AdditionalPayment(
                name = "Премия",
                amount = 500.0,
                taxable = true,
                withAdvance = false,
                active = true
            )
        )

        val result = PayrollCalculator.calculate(
            shifts = shifts,
            firstHalfShifts = shifts,
            settings = settings,
            additionalPayments = additionalPayments
        )

        assertMoney(1000.0, result.hourlyRate)
        assertMoney(result.taxableGrossTotal + result.nonTaxableTotal, result.grossTotal)
        assertMoney(result.grossTotal - result.ndfl, result.netTotal)
        assertMoney(result.advanceGrossAmount + result.salaryGrossAmount, result.grossTotal)
        assertMoney(result.advanceNdflAmount + result.salaryNdflAmount, result.ndfl)
        assertMoney(result.advanceNetAmount + result.salaryNetAmount, result.netTotal)
        assertMoney(result.advanceAmount, result.advanceNetAmount)
        assertMoney(result.salaryPaymentAmount, result.salaryNetAmount)
        assertMoney(
            max(0.0, result.netTotal - result.advanceAmount),
            result.salaryPaymentAmount
        )
    }

    @Test
    fun calculate_actualEarningsAdvance_netMatchesKnown13PercentCase() {
        val settings = PayrollSettings(
            baseSalary = 100_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 100.0,
            payMode = PayMode.HOURLY.name,
            ndflPercent = 0.13,
            advanceMode = AdvanceMode.ACTUAL_EARNINGS.name
        )
        val shift = WorkShiftItem(
            paidHours = 10.0,
            nightHours = 0.0,
            isWeekendPaid = false
        )

        val result = PayrollCalculator.calculate(
            shifts = listOf(shift),
            firstHalfShifts = listOf(shift),
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(10_000.0, result.grossTotal)
        assertMoney(1_300.0, result.ndfl)
        assertMoney(8_700.0, result.netTotal)
        assertMoney(10_000.0, result.advanceGrossAmount)
        assertMoney(1_300.0, result.advanceNdflAmount)
        assertMoney(8_700.0, result.advanceAmount)
        assertMoney(0.0, result.salaryPaymentAmount)
    }

    @Test
    fun calculate_withDeductions_keepsPostDeductionTotalsConsistent() {
        val settings = PayrollSettings(
            baseSalary = 120_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 120.0,
            payMode = PayMode.HOURLY.name,
            ndflPercent = 0.13,
            advanceMode = AdvanceMode.ACTUAL_EARNINGS.name
        )
        val shifts = listOf(
            WorkShiftItem(paidHours = 10.0, nightHours = 0.0, isWeekendPaid = false),
            WorkShiftItem(paidHours = 10.0, nightHours = 0.0, isWeekendPaid = false)
        )
        val deductions = listOf(
            PayrollDeduction(
                id = "deduction_1",
                title = "Исполнительный лист",
                active = true,
                type = DeductionType.ENFORCEMENT.name,
                mode = DeductionMode.FIXED.name,
                value = 5_000.0,
                applyToAdvance = true,
                applyToSalary = true
            )
        )

        val result = PayrollCalculator.calculate(
            shifts = shifts,
            firstHalfShifts = listOf(shifts.first()),
            settings = settings,
            additionalPayments = emptyList(),
            deductions = deductions
        )

        assertMoney(
            result.netAdvanceAfterDeductions + result.netSalaryAfterDeductions,
            result.netAfterDeductions
        )
        assertMoney(
            result.netTotal - result.deductionsTotal,
            result.netAfterDeductions
        )
    }

    @Test
    fun progressiveNdfl_segmentAtBoundary_isCalculatedByBrackets() {
        val ndfl = calculateNdflForTaxableSegment(
            taxableIncomeYtdBeforeSegment = 2_300_000.0,
            taxableSegmentAmount = 200_000.0,
            progressiveNdflEnabled = true,
            flatRate = 0.13
        )

        assertMoney(28_000.0, ndfl)
    }

    @Test
    fun paymentDates_movesWeekendToPreviousWorkday() {
        val settings = PayrollSettings(
            advanceDay = 31,
            salaryDay = 5,
            movePaymentsToPreviousWorkday = true
        )

        val dates = calculatePaymentDates(
            month = YearMonth.of(2026, 5),
            settings = settings,
            extraDayOffDates = setOf(LocalDate.of(2026, 6, 12))
        )

        assertEquals(LocalDate.of(2026, 5, 29), dates.advanceDate)
        assertEquals(LocalDate.of(2026, 6, 5), dates.salaryDate)
    }

    @Test
    fun calculate_usesPartialHolidayHoursForNightShift() {
        val settings = PayrollSettings(
            baseSalary = 102_050.0,
            extraSalary = 0.0,
            monthlyNormHours = 165.0,
            payMode = PayMode.HOURLY.name,
            holidayRateMultiplier = 2.0
        )

        val shift = WorkShiftItem(
            paidHours = 11.5,
            nightHours = 8.0,
            isWeekendPaid = true,
            holidayPaidHours = 4.0
        )

        val result = PayrollCalculator.calculate(
            shifts = listOf(shift),
            firstHalfShifts = listOf(shift),
            settings = settings,
            additionalPayments = emptyList()
        )

        val expectedHourly = settings.baseSalary / settings.monthlyNormHours
        assertMoney(4.0, result.holidayHours)
        assertMoney(expectedHourly * 4.0, result.holidayExtra)
    }

    @Test
    fun calculate_separateSpecialDayPay_excludesHoursFromBaseAndPaysFullMultiplier() {
        val settings = PayrollSettings(
            baseSalary = 100_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 100.0,
            payMode = PayMode.HOURLY.name,
            holidayRateMultiplier = 2.0,
            specialDayPaymentMode = SpecialDayPaymentMode.SEPARATE_FULL_PAY.name
        )
        val regularShift = WorkShiftItem(
            paidHours = 8.0,
            nightHours = 0.0,
            isWeekendPaid = false
        )
        val holidayShift = WorkShiftItem(
            paidHours = 8.0,
            nightHours = 0.0,
            isWeekendPaid = true
        )

        val result = PayrollCalculator.calculate(
            shifts = listOf(regularShift, holidayShift),
            firstHalfShifts = emptyList(),
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(16.0, result.workedHours)
        assertMoney(8.0, result.baseWorkedHours)
        assertMoney(8_000.0, result.basePay)
        assertMoney(16_000.0, result.holidayExtra)
    }

    @Test
    fun calculate_oneCMixedSpecialDayPay_excludesRvdFromBaseAndKeepsHolidaysInBase() {
        val settings = PayrollSettings(
            baseSalary = 100_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 100.0,
            payMode = PayMode.HOURLY.name,
            holidayRateMultiplier = 2.0,
            specialDayPaymentMode = SpecialDayPaymentMode.HOLIDAYS_SEPARATE_RVD_EXTRA.name
        )
        val regularShift = WorkShiftItem(
            paidHours = 8.0,
            nightHours = 0.0,
            isWeekendPaid = false
        )
        val federalHolidayShift = WorkShiftItem(
            paidHours = 6.0,
            nightHours = 0.0,
            isWeekendPaid = true,
            specialDayType = SpecialDayType.WEEKEND_HOLIDAY.name,
            specialDayCompensation = SpecialDayCompensation.DOUBLE_PAY.name
        )
        val rvdShift = WorkShiftItem(
            paidHours = 4.0,
            nightHours = 0.0,
            isWeekendPaid = false,
            specialDayType = SpecialDayType.RVD.name,
            specialDayCompensation = SpecialDayCompensation.DOUBLE_PAY.name
        )

        val result = PayrollCalculator.calculate(
            shifts = listOf(regularShift, federalHolidayShift, rvdShift),
            firstHalfShifts = emptyList(),
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(18.0, result.workedHours)
        assertMoney(14.0, result.baseWorkedHours)
        assertMoney(14_000.0, result.basePay)
        assertMoney(14_000.0, result.holidayExtra)
    }

    @Test
    fun calculate_rvdUsesFullShiftHoursWhenHolidayOverlapExists() {
        val settings = PayrollSettings(
            baseSalary = 100_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 100.0,
            payMode = PayMode.HOURLY.name,
            holidayRateMultiplier = 2.0,
            specialDayPaymentMode = SpecialDayPaymentMode.SEPARATE_FULL_PAY.name
        )
        val rvdShift = WorkShiftItem(
            paidHours = 11.5,
            nightHours = 0.0,
            isWeekendPaid = false,
            specialDayType = SpecialDayType.RVD.name,
            specialDayCompensation = SpecialDayCompensation.DOUBLE_PAY.name,
            holidayPaidHours = 2.0
        )

        val result = PayrollCalculator.calculate(
            shifts = listOf(rvdShift),
            firstHalfShifts = emptyList(),
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(11.5, result.holidayHours)
        assertMoney(0.0, result.baseWorkedHours)
        assertMoney(23_000.0, result.holidayExtra)
    }

    @Test
    fun calculate_oneCLikeFirstHalfAdvance_doesNotIncludeTransferredDayPremium() {
        val settings = PayrollSettings(
            baseSalary = 102_050.0,
            extraSalary = 49_733.0,
            monthlyNormHours = 1_963.0 / 12.0,
            payMode = PayMode.HOURLY.name,
            extraSalaryMode = ExtraSalaryMode.INCLUDED_IN_RATE.name,
            ndflPercent = 0.13,
            nightPercent = 0.40,
            nightHoursBaseMode = NightHoursBaseMode.BASE_ONLY.name,
            holidayRateMultiplier = 2.0,
            specialDayPaymentMode = SpecialDayPaymentMode.HOLIDAYS_SEPARATE_RVD_EXTRA.name,
            advanceMode = AdvanceMode.ACTUAL_EARNINGS.name,
            paymentScheduleMode = PaymentScheduleMode.TWICE_MONTHLY.name
        )
        val normalNight = WorkShiftItem(
            paidHours = 11.5,
            nightHours = 7.25,
            isWeekendPaid = false
        )
        val holidayNight = WorkShiftItem(
            paidHours = 11.5,
            nightHours = 7.25,
            isWeekendPaid = true,
            specialDayType = SpecialDayType.WEEKEND_HOLIDAY.name,
            specialDayCompensation = SpecialDayCompensation.DOUBLE_PAY.name,
            holidayPaidHours = 4.0
        )
        val normalDay = WorkShiftItem(
            paidHours = 11.5,
            nightHours = 0.0,
            isWeekendPaid = false
        )
        val holidayDay = WorkShiftItem(
            paidHours = 11.5,
            nightHours = 0.0,
            isWeekendPaid = true,
            specialDayType = SpecialDayType.WEEKEND_HOLIDAY.name,
            specialDayCompensation = SpecialDayCompensation.DOUBLE_PAY.name,
            holidayPaidHours = 11.5
        )
        val rvdDay = WorkShiftItem(
            paidHours = 11.5,
            nightHours = 0.0,
            isWeekendPaid = false,
            specialDayType = SpecialDayType.RVD.name,
            specialDayCompensation = SpecialDayCompensation.DOUBLE_PAY.name
        )
        val firstHalfShifts = listOf(
            holidayNight,
            normalNight,
            normalDay,
            holidayDay,
            normalNight,
            normalNight,
            rvdDay
        )

        val result = PayrollCalculator.calculate(
            shifts = firstHalfShifts,
            firstHalfShifts = firstHalfShifts,
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(69.0, result.baseWorkedHours)
        assertMoney(27.0, result.holidayHours)
        assertEquals(93_075.93, result.advanceAmount, 2.0)
    }

    @Test
    fun calculatePeriodOvertime_percentModeUsesFixedPercentOfHourlyRate() {
        val settings = PayrollSettings(
            baseSalary = 1_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 10.0,
            payMode = PayMode.HOURLY.name,
            overtimeEnabled = true,
            overtimePaymentMode = OvertimePaymentMode.PERCENT_OF_HOURLY.name,
            overtimePercentOfHourly = 75.0
        )
        val result = PayrollCalculator.calculatePeriodOvertime(
            shifts = listOf(WorkShiftItem(paidHours = 12.0, nightHours = 0.0, isWeekendPaid = false)),
            settings = settings,
            periodLabel = "Тест",
            periodStart = LocalDate.of(2026, 6, 1),
            periodEnd = LocalDate.of(2026, 6, 30),
            periodNormHours = 10.0
        )

        assertMoney(2.0, result.payableOvertimeHours)
        assertMoney(100.0, result.hourlyRate)
        assertMoney(150.0, result.overtimePremiumAmount)
    }

    @Test
    fun calculatePeriodOvertime_customMultiplierUsesConfiguredSteps() {
        val settings = PayrollSettings(
            baseSalary = 1_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 10.0,
            payMode = PayMode.HOURLY.name,
            overtimeEnabled = true,
            overtimePaymentMode = OvertimePaymentMode.CUSTOM_MULTIPLIER.name,
            overtimeFirstStepHours = 1.0,
            overtimeFirstStepMultiplier = 1.25,
            overtimeNextStepMultiplier = 1.75
        )
        val result = PayrollCalculator.calculatePeriodOvertime(
            shifts = listOf(WorkShiftItem(paidHours = 12.0, nightHours = 0.0, isWeekendPaid = false)),
            settings = settings,
            periodLabel = "Тест",
            periodStart = LocalDate.of(2026, 6, 1),
            periodEnd = LocalDate.of(2026, 6, 30),
            periodNormHours = 10.0
        )

        assertMoney(2.0, result.payableOvertimeHours)
        assertMoney(100.0, result.hourlyRate)
        assertMoney(100.0, result.overtimePremiumAmount)
    }

    private fun assertMoney(expected: Double, actual: Double, delta: Double = 0.01) {
        assertEquals(expected, actual, delta)
    }
}
