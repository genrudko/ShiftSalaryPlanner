package com.vigilante.shiftsalaryplanner.payroll

import org.junit.Assert.assertEquals
import org.junit.Test

class PayrollCharacterizationTest {
    @Test
    fun monthlySalary_proratesBaseAndFixedExtraByWorkedRatio() {
        val settings = PayrollSettings(
            baseSalary = 100_000.0,
            extraSalary = 20_000.0,
            extraSalaryMode = ExtraSalaryMode.FIXED_MONTHLY.name,
            monthlyNormHours = 160.0,
            payMode = PayMode.MONTHLY_SALARY.name,
            ndflEnabled = false,
            paymentScheduleMode = PaymentScheduleMode.ONCE_MONTHLY.name
        )
        val shifts = List(10) { WorkShiftItem(8.0, 0.0, false) }

        val result = PayrollCalculator.calculate(
            shifts = shifts,
            firstHalfShifts = shifts.take(5),
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(80.0, result.workedHours)
        assertMoney(60_000.0, result.basePay)
        assertMoney(60_000.0, result.grossTotal)
        assertMoney(60_000.0, result.netTotal)
    }

    @Test
    fun perShiftNetMode_usesShiftAmountsWithoutNightHolidayOrNdfl() {
        val settings = PayrollSettings(
            baseSalary = 999_999.0,
            monthlyNormHours = 1.0,
            payMode = PayMode.PER_SHIFT.name,
            perShiftPayTaxable = false,
            ndflEnabled = true,
            ndflPercent = 0.13,
            nightPercent = 0.4,
            holidayRateMultiplier = 2.0,
            paymentScheduleMode = PaymentScheduleMode.PER_SHIFT.name
        )
        val shifts = listOf(
            WorkShiftItem(
                paidHours = 12.0,
                nightHours = 8.0,
                isWeekendPaid = false,
                shiftPayAmount = 5_000.0
            ),
            WorkShiftItem(
                paidHours = 12.0,
                nightHours = 0.0,
                isWeekendPaid = true,
                shiftPayAmount = 7_000.0
            )
        )

        val result = PayrollCalculator.calculate(
            shifts = shifts,
            firstHalfShifts = shifts,
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(12_000.0, result.basePay)
        assertMoney(0.0, result.nightExtra)
        assertMoney(0.0, result.holidayExtra)
        assertMoney(0.0, result.taxableGrossTotal)
        assertMoney(12_000.0, result.nonTaxableTotal)
        assertMoney(0.0, result.ndfl)
        assertMoney(12_000.0, result.netTotal)
    }

    @Test
    fun vacationAndSickPay_coexistWithRegularWorkedPay() {
        val settings = PayrollSettings(
            baseSalary = 16_000.0,
            extraSalary = 0.0,
            monthlyNormHours = 160.0,
            payMode = PayMode.HOURLY.name,
            vacationAverageDaily = 3_000.0,
            sickAverageDaily = 2_500.0,
            sickPayPercent = 0.8,
            sickMaxDailyAmount = 5_000.0,
            ndflEnabled = false,
            paymentScheduleMode = PaymentScheduleMode.ONCE_MONTHLY.name
        )
        val regular = WorkShiftItem(8.0, 0.0, false)
        val vacation = WorkShiftItem(0.0, 0.0, false, isVacation = true)
        val sick = WorkShiftItem(0.0, 0.0, false, isSickLeave = true)

        val result = PayrollCalculator.calculate(
            shifts = listOf(regular, vacation, sick),
            firstHalfShifts = emptyList(),
            settings = settings,
            additionalPayments = emptyList()
        )

        assertMoney(800.0, result.basePay)
        assertEquals(1, result.vacationDays)
        assertMoney(3_000.0, result.vacationPay)
        assertEquals(1, result.sickDays)
        assertMoney(2_000.0, result.sickPay)
        assertMoney(5_800.0, result.grossTotal)
    }

    private fun assertMoney(expected: Double, actual: Double, delta: Double = 0.01) {
        assertEquals(expected, actual, delta)
    }
}
