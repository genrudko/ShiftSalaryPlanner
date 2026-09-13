package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.YearMonth

private fun reviewFinancePayroll() = PayrollResult(
    workedHours = 168.0,
    baseWorkedHours = 152.0,
    nightHours = 48.0,
    holidayHours = 12.0,
    hourlyRate = 1000.0,
    basePay = 152000.0,
    nightExtra = 14400.0,
    holidayExtra = 12000.0,
    vacationDays = 0,
    vacationPay = 0.0,
    sickDays = 0,
    sickPay = 0.0,
    housingPayment = 10000.0,
    housingPaymentTaxable = true,
    housingAdvancePart = 0.0,
    housingSalaryPart = 10000.0,
    additionalPaymentsTotal = 33682.0,
    additionalPaymentsAdvancePart = 0.0,
    additionalPaymentsSalaryPart = 33682.0,
    additionalPaymentsTaxablePart = 33682.0,
    additionalPaymentsNonTaxablePart = 0.0,
    taxableGrossTotal = 222082.0,
    nonTaxableTotal = 0.0,
    grossTotal = 222082.0,
    ndfl = 27571.0,
    netTotal = 194511.0,
    advanceGrossAmount = 80000.0,
    salaryGrossAmount = 142082.0,
    advanceNdflAmount = 10000.0,
    salaryNdflAmount = 17571.0,
    advanceNetAmount = 70000.0,
    salaryNetAmount = 124511.0,
    advanceAmount = 70000.0,
    salaryPaymentAmount = 124511.0,
    shiftOnlyAdvanceNetAmount = 70000.0,
    shiftOnlySalaryNetAmount = 124511.0,
    deductionsTotal = 10000.0,
    deductionsAdvancePart = 0.0,
    deductionsSalaryPart = 10000.0,
    alimonyAmount = 0.0,
    enforcementAmount = 0.0,
    otherDeductionsAmount = 10000.0,
    netAdvanceAfterDeductions = 70000.0,
    netSalaryAfterDeductions = 114511.0,
    netAfterDeductions = 184511.0,
    shiftOnlyAdvanceNetAfterDeductions = 70000.0,
    shiftOnlySalaryNetAfterDeductions = 114511.0,
    taxableIncomeYtdBeforeCurrentMonth = 1240000.0,
    taxableIncomeYtdAfterCurrentMonth = 1462082.0
)

private fun reviewFinanceShiftStats() = DetailedShiftStats(
    totalAssignedDays = 18,
    workedShiftCount = 14,
    firstHalfAssignedDays = 9,
    secondHalfAssignedDays = 9,
    firstHalfWorkedShifts = 7,
    secondHalfWorkedShifts = 7,
    firstHalfWorkedHours = 84.0,
    secondHalfWorkedHours = 84.0,
    firstHalfNightHours = 24.0,
    secondHalfNightHours = 24.0,
    dayShiftCount = 8,
    nightShiftCount = 6,
    weekendHolidayShiftCount = 2,
    eightHourShiftCount = 2,
    vacationShiftCount = 0,
    sickShiftCount = 0,
    shiftCostBaseTotal = 222082.0,
    shiftCostIncludedPayments = 33682.0,
    shiftCostAverageGross = 15863.0,
    shiftCostAverageNet = 13179.0,
    dayShiftCostAverageGross = 14900.0,
    dayShiftCostAverageNet = 12350.0,
    nightShiftCostAverageGross = 17140.0,
    nightShiftCostAverageNet = 14280.0
)

private fun reviewFinanceState(perShift: Boolean = false) = FinanceSummaryState(
    periodLabel = "Сентябрь 2026",
    workplaceLabel = "Работа: Северный парк",
    payroll = reviewFinancePayroll(),
    detailedShiftStats = reviewFinanceShiftStats(),
    paymentDates = PaymentDates(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)),
    payMode = if (perShift) PayMode.PER_SHIFT.name else PayMode.HOURLY.name,
    paymentScheduleMode = if (perShift) PaymentScheduleMode.PER_SHIFT.name else PaymentScheduleMode.TWICE_MONTHLY.name,
    todaySummary = "Ночная смена 20:00–08:00",
    tomorrowSummary = "Выходной",
    nextAlarmSummary = "сегодня в 18:20",
    actualAdvanceNet = if (perShift) 183900.0 else 70000.0,
    actualSalaryNet = if (perShift) 0.0 else 113900.0,
    paymentDifferenceToleranceRub = 100.0
)

@Composable
private fun M8FinanceReviewSurface(dark: Boolean, perShift: Boolean = false) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            FinanceTab(
                selectedSubTab = FinanceSubTab.SUMMARY,
                onSelectSubTab = {},
                summaryState = reviewFinanceState(perShift),
                payrollContent = { Text("Расчёт") },
                paymentsContent = { Text("Выплаты") },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@PreviewTest
@Preview(name = "Finance summary light", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
fun m8FinanceSummaryLight() = M8FinanceReviewSurface(false)

@PreviewTest
@Preview(name = "Finance summary dark", widthDp = 412, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m8FinanceSummaryDark() = M8FinanceReviewSurface(true)

@PreviewTest
@Preview(name = "Finance summary large font", widthDp = 412, heightDp = 980, fontScale = 1.3f, showBackground = true)
@Composable
fun m8FinanceSummaryLargeFont() = M8FinanceReviewSurface(false)

@PreviewTest
@Preview(name = "Finance summary per shift", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
fun m8FinanceSummaryPerShift() = M8FinanceReviewSurface(false, perShift = true)

private fun reviewFinanceOvertime() = AnnualOvertimeResult(
    enabled = true,
    periodLabel = "2026",
    periodStart = LocalDate.of(2026, 1, 1),
    periodEnd = LocalDate.of(2026, 12, 31),
    year = 2026,
    annualNormHours = 1972.0,
    workedHours = 168.0,
    holidayExcludedHours = 12.0,
    rawOvertimeHours = 4.0,
    payableOvertimeHours = 4.0,
    firstTwoHours = 2.0,
    remainingHours = 2.0,
    hourlyRate = 1000.0,
    overtimePremiumAmount = 6000.0
)

@Composable
private fun M11PaymentsReviewSurface(dark: Boolean = false) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            FinanceTab(
                selectedSubTab = FinanceSubTab.PAYMENTS,
                onSelectSubTab = {},
                summaryState = reviewFinanceState(),
                payrollContent = { Text("Расчёт") },
                paymentsContent = {
                    PaymentsTab(
                        currentMonth = YearMonth.of(2026, 9),
                        onPrevMonth = {},
                        onNextMonth = {},
                        onPickMonth = {},
                        payroll = reviewFinancePayroll(),
                        annualOvertime = reviewFinanceOvertime(),
                        paymentDates = PaymentDates(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)),
                        payMode = PayMode.HOURLY.name,
                        paymentScheduleMode = PaymentScheduleMode.TWICE_MONTHLY.name,
                        housingPaymentLabel = "Выплата на квартиру",
                        additionalPayments = emptyList(),
                        resolvedAdditionalPaymentsBreakdown = emptyList(),
                        detailedShiftStats = reviewFinanceShiftStats(),
                        onAddPayment = {},
                        onEditPayment = {},
                        onDeletePayment = {},
                        onOpenMonthlyReport = {},
                        onOpenVisibilitySettings = {},
                        visibilitySettings = ReportVisibilitySettings(),
                        actualAdvanceNet = 70000.0,
                        actualSalaryNet = 110000.0,
                        paymentDifferenceToleranceRub = 100.0,
                        onSaveActualPayments = { _, _ -> },
                        modifier = Modifier.fillMaxSize()
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@PreviewTest
@Preview(name = "Finance payments mismatch", widthDp = 412, heightDp = 1100, showBackground = true)
@Composable
fun m11FinancePaymentsMismatch() = M11PaymentsReviewSurface(false)
