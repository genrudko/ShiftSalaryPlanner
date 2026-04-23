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

    private fun assertMoney(expected: Double, actual: Double, delta: Double = 0.01) {
        assertEquals(expected, actual, delta)
    }
}
