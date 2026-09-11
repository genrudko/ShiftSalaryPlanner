package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPaymentType
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PaymentDistribution
import com.vigilante.shiftsalaryplanner.payroll.PayrollCalculator
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.WorkShiftItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PaymentEnhancementsTest {

    @Test
    fun resolvedHourlyPayment_isNotMultipliedAgainByPayrollCalculator() {
        val shifts = listOf(
            WorkShiftItem(
                paidHours = 8.0,
                nightHours = 0.0,
                isWeekendPaid = false,
                date = LocalDate.of(2026, 6, 7)
            )
        )
        val resolved = resolveAdditionalPaymentsForMonth(
            configuredPayments = listOf(
                AdditionalPayment(
                    id = "hourly_bonus",
                    name = "Доплата",
                    amount = 200.0,
                    active = true,
                    taxable = false,
                    type = AdditionalPaymentType.HOURLY.name,
                    distribution = PaymentDistribution.SALARY.name
                )
            ),
            month = YearMonth.of(2026, 6),
            shifts = shifts,
            firstHalfShifts = shifts,
            baseSalary = 0.0
        )

        val payroll = PayrollCalculator.calculate(
            shifts = shifts,
            firstHalfShifts = shifts,
            settings = PayrollSettings(
                baseSalary = 0.0,
                extraSalary = 0.0,
                monthlyNormHours = 160.0,
                payMode = PayMode.HOURLY.name,
                ndflEnabled = false
            ),
            additionalPayments = resolved.asPayrollPayments()
        )

        assertEquals(1_600.0, resolved.total, 0.001)
        assertEquals(1_600.0, payroll.additionalPaymentsTotal, 0.001)
    }

    @Test
    fun resolvedPerShiftPayment_isMultipliedByWorkedShiftsOnly() {
        val shifts = listOf(
            WorkShiftItem(
                paidHours = 8.0,
                nightHours = 0.0,
                isWeekendPaid = false,
                date = LocalDate.of(2026, 6, 7)
            ),
            WorkShiftItem(
                paidHours = 11.5,
                nightHours = 7.25,
                isWeekendPaid = false,
                date = LocalDate.of(2026, 6, 8)
            ),
            WorkShiftItem(
                paidHours = 0.0,
                nightHours = 0.0,
                isWeekendPaid = false,
                date = LocalDate.of(2026, 6, 9)
            )
        )

        val resolved = resolveAdditionalPaymentsForMonth(
            configuredPayments = listOf(
                AdditionalPayment(
                    id = "per_shift_bonus",
                    name = "Деньги за выход",
                    amount = 200.0,
                    active = true,
                    taxable = false,
                    type = AdditionalPaymentType.PER_SHIFT.name,
                    distribution = PaymentDistribution.SALARY.name
                )
            ),
            month = YearMonth.of(2026, 6),
            shifts = shifts,
            firstHalfShifts = shifts,
            baseSalary = 0.0
        )

        assertEquals(400.0, resolved.total, 0.001)
    }
}
