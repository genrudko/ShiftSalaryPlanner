package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollDetailedResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineItem
import com.vigilante.shiftsalaryplanner.payroll.PayrollLineKind
import com.vigilante.shiftsalaryplanner.payroll.PayrollQuantityUnit
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetSection
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.YearMonth

private fun payrollReviewResult() = PayrollResult(
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

private fun payrollReviewStats() = DetailedShiftStats(
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

private fun payrollReviewDetailed(): PayrollDetailedResult {
    val p = payrollReviewResult()
    return PayrollDetailedResult(
        summary = p,
        lineItems = listOf(
            PayrollLineItem("hours", PayrollSheetSection.HEADER, PayrollLineKind.REFERENCE_VALUE, title = "Оплачиваемые часы", quantity = 168.0, unit = PayrollQuantityUnit.HOURS, amount = 168.0, sortOrder = 1),
            PayrollLineItem("shifts", PayrollSheetSection.HEADER, PayrollLineKind.REFERENCE_VALUE, title = "Рабочие смены", quantity = 14.0, unit = PayrollQuantityUnit.TIMES, amount = 14.0, sortOrder = 2),
            PayrollLineItem("base", PayrollSheetSection.ACCRUAL, PayrollLineKind.BASE_SALARY, title = "Оплата по часовой ставке", quantity = 152.0, unit = PayrollQuantityUnit.HOURS, amount = 152000.0, sortOrder = 10, ndflAmount = 19760.0, netAmount = 132240.0, expandableDetails = true),
            PayrollLineItem("night", PayrollSheetSection.ACCRUAL, PayrollLineKind.NIGHT_EXTRA, title = "Доплата за ночные часы", quantity = 48.0, unit = PayrollQuantityUnit.HOURS, amount = 14400.0, sortOrder = 20),
            PayrollLineItem("holiday", PayrollSheetSection.ACCRUAL, PayrollLineKind.SPECIAL_DAY_EXTRA, title = "Праздничные / выходные", quantity = 12.0, unit = PayrollQuantityUnit.HOURS, amount = 12000.0, sortOrder = 30),
            PayrollLineItem("premium", PayrollSheetSection.ACCRUAL, PayrollLineKind.OPERATION_PREMIUM, title = "Премия и доплаты", amount = 33682.0, sortOrder = 40),
            PayrollLineItem("housing", PayrollSheetSection.ACCRUAL, PayrollLineKind.HOUSING_COMPENSATION, title = "Выплата на квартиру", amount = 10000.0, sortOrder = 50),
            PayrollLineItem("ndfl", PayrollSheetSection.DEDUCTION, PayrollLineKind.NDFL, title = "НДФЛ", amount = 27571.0, sortOrder = 60),
            PayrollLineItem("other-deduction", PayrollSheetSection.DEDUCTION, PayrollLineKind.EXECUTIVE_DEDUCTION, title = "Прочие удержания", amount = 10000.0, sortOrder = 70),
            PayrollLineItem("advance", PayrollSheetSection.PRIOR_PAYMENT, PayrollLineKind.ADVANCE_PAID, title = "Аванс", amount = 70000.0, sortOrder = 80),
            PayrollLineItem("payout", PayrollSheetSection.PAYOUT, PayrollLineKind.FINAL_PAYOUT, title = "К выплате с зарплатой", amount = 114511.0, sortOrder = 90),
            PayrollLineItem("gross", PayrollSheetSection.REFERENCE, PayrollLineKind.REFERENCE_VALUE, title = "Начислено всего", amount = 222082.0, sortOrder = 100),
            PayrollLineItem("net", PayrollSheetSection.REFERENCE, PayrollLineKind.REFERENCE_VALUE, title = "К выплате за период", amount = 184511.0, sortOrder = 110)
        )
    )
}

private fun payrollReviewOvertime() = AnnualOvertimeResult(
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

private fun payrollReviewState() = PayrollTabState(
    currentMonth = YearMonth.of(2026, 9),
    periodMode = PayrollPeriodMode.MONTH,
    selectedWorkplaceId = WORKPLACE_MAIN_ID,
    workplaceOptions = listOf(PayrollWorkplaceOption(WORKPLACE_MAIN_ID, "Северный парк")),
    periodStartDate = LocalDate.of(2026, 9, 1),
    periodEndDate = LocalDate.of(2026, 9, 30),
    periodLabel = "Сентябрь 2026",
    periodFileLabel = "2026-09",
    summary = MonthSummary(14, 168.0, 48.0),
    payroll = payrollReviewResult(),
    payrollDetailedResult = payrollReviewDetailed(),
    annualOvertime = payrollReviewOvertime(),
    paymentDates = PaymentDates(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)),
    payMode = PayMode.HOURLY.name,
    paymentScheduleMode = PaymentScheduleMode.TWICE_MONTHLY.name,
    housingPaymentLabel = "Выплата на квартиру",
    detailedShiftStats = payrollReviewStats(),
    isSummaryExpanded = false,
    reportVisibilitySettings = ReportVisibilitySettings()
)

private val payrollNoopActions = PayrollTabActions(
    onChangePeriodMode = {}, onChangeWorkplace = {}, onPrevMonth = {}, onNextMonth = {}, onPickMonth = {},
    onPrevYear = {}, onNextYear = {}, onPickYear = {}, onShiftRangeBackward = {}, onShiftRangeForward = {},
    onPickRangeStart = {}, onPickRangeEnd = {}, onToggleSummary = {}, onOpenSettings = {}, onOpenDiagnostics = {},
    onOpenVisibilitySettings = {}, onExportSheetPdf = { _, _, _ -> }
)

@Composable
private fun PayrollReviewSurface(dark: Boolean) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PayrollTab(state = payrollReviewState(), actions = payrollNoopActions, modifier = Modifier.fillMaxSize())
        }
    }
}

@PreviewTest
@Preview(name = "Payroll calculation light", widthDp = 412, heightDp = 1450, showBackground = true)
@Composable
fun m8PayrollCalculationLight() = PayrollReviewSurface(false)

@PreviewTest
@Preview(name = "Payroll calculation dark", widthDp = 412, heightDp = 1450, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m8PayrollCalculationDark() = PayrollReviewSurface(true)

@PreviewTest
@Preview(name = "Payroll calculation large font", widthDp = 412, heightDp = 1700, fontScale = 1.3f, showBackground = true)
@Composable
fun m8PayrollCalculationLargeFont() = PayrollReviewSurface(false)
