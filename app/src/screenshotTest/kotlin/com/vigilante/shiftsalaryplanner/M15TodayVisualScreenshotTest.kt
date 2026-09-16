package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import com.vigilante.shiftsalaryplanner.ui.today.TimelineAction
import com.vigilante.shiftsalaryplanner.ui.today.TimelineItem
import com.vigilante.shiftsalaryplanner.ui.today.TimelineKind
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSeverity
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSource
import com.vigilante.shiftsalaryplanner.ui.today.TodayScreen
import com.vigilante.shiftsalaryplanner.ui.today.TodayShiftPreview
import com.vigilante.shiftsalaryplanner.ui.today.UpcomingPaymentItem
import com.vigilante.shiftsalaryplanner.ui.today.buildTodayTimeline
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles
import java.time.Instant
import java.time.LocalDate

/**
 * M15.1 Task 3 deterministic screenshot states for the fixed operational Today
 * hierarchy (Hero -> Timeline -> configurable lower modules).
 *
 * All fixtures are independent of wall-clock time: [reviewToday] pins the date and
 * every timeline item uses explicit epoch millis, so references stay reproducible.
 */
private val reviewToday = LocalDate.of(2026, 9, 12)

private val reviewTodayShifts = listOf(
    TodayShiftPreview(
        title = "Дневная",
        workplace = "Северный парк",
        code = "Д",
        iconKey = "TEXT",
        badgeColor = 0xFFA9D6FF.toInt(),
        timeLabel = "08:00-20:00",
        hoursLabel = "11 ч"
    )
)

private val reviewUpcomingPayments = listOf(
    UpcomingPaymentItem(
        title = "Аванс",
        periodLabel = "за сентябрь",
        date = LocalDate.of(2026, 9, 15),
        amount = 70000.0
    ),
    UpcomingPaymentItem(
        title = "Зарплата",
        periodLabel = "за август",
        date = LocalDate.of(2026, 9, 30),
        amount = 114511.0
    )
)

private val reviewNotes = listOf(
    AppNote(
        id = "m15-review-note",
        date = "2026-09-12",
        title = "Сдать отчёт",
        body = "Проверить смену и отразить фактическую выплату.",
        colorHex = "#FFF3BF",
        createdAtMillis = Instant.parse("2026-09-11T09:00:00Z").toEpochMilli(),
        updatedAtMillis = Instant.parse("2026-09-11T09:00:00Z").toEpochMilli()
    )
)

private val reviewMonthAudit = CalendarMonthAudit(
    emptyDayCount = 6,
    multiWorkDayCount = 2,
    overlappingWorkDayCount = 0
)

private fun reviewPayroll() = PayrollResult(
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
    taxableIncomeYtdBeforeCurrentMonth = 0.0,
    taxableIncomeYtdAfterCurrentMonth = 0.0
)
private val reviewTimelineItems = buildTodayTimeline(
    items = listOf(
        TimelineItem(
            stableId = "preview-alarm",
            source = TimelineSource.ALARM,
            kind = TimelineKind.ALARM,
            startMillis = Instant.parse("2026-09-12T02:45:00Z").toEpochMilli(),
            title = "На дневную",
            subtitle = "Дневная • 2026-09-12 • начало 08:00",
            severity = TimelineSeverity.NORMAL,
            sourceLabel = "Будильник",
            action = TimelineAction.OpenAlarms
        ),
        TimelineItem(
            stableId = "preview-shift-day",
            source = TimelineSource.SHIFT,
            kind = TimelineKind.SHIFT_START,
            startMillis = Instant.parse("2026-09-12T05:00:00Z").toEpochMilli(),
            endMillis = Instant.parse("2026-09-12T17:00:00Z").toEpochMilli(),
            title = "Дневная",
            subtitle = "Северный парк",
            severity = TimelineSeverity.HIGH,
            colorArgb = 0xFFA9D6FF.toInt(),
            sourceLabel = "Смена",
            action = TimelineAction.OpenCalendar
        ),
        TimelineItem(
            stableId = "preview-payment",
            source = TimelineSource.FINANCE,
            kind = TimelineKind.PAYMENT,
            startMillis = Instant.parse("2026-09-12T09:00:00Z").toEpochMilli(),
            title = "Аванс",
            subtitle = "15 сентября · за сентябрь",
            severity = TimelineSeverity.NORMAL,
            sourceLabel = "Выплата",
            action = TimelineAction.OpenFinance
        )
    ),
    nowMillis = 1_752_000_000_000L
)

private val reviewTimelineNoShift = buildTodayTimeline(
    items = listOf(
        TimelineItem(
            stableId = "preview-alarm",
            source = TimelineSource.ALARM,
            kind = TimelineKind.ALARM,
            startMillis = Instant.parse("2026-09-12T02:45:00Z").toEpochMilli(),
            title = "На дневную",
            subtitle = "Дневная • 2026-09-12 • начало 08:00",
            severity = TimelineSeverity.NORMAL,
            sourceLabel = "Будильник",
            action = TimelineAction.OpenAlarms
        ),
        TimelineItem(
            stableId = "preview-payment",
            source = TimelineSource.FINANCE,
            kind = TimelineKind.PAYMENT,
            startMillis = Instant.parse("2026-09-12T09:00:00Z").toEpochMilli(),
            title = "Аванс",
            subtitle = "15 сентября · за сентябрь",
            severity = TimelineSeverity.NORMAL,
            sourceLabel = "Выплата",
            action = TimelineAction.OpenFinance
        )
    ),
    nowMillis = 1_752_000_000_000L
)


@Composable
private fun M15TodayThemeSurface(dark: Boolean, content: @Composable () -> Unit) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        val roles = evolutionColorRoles()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = roles.appBackground,
            contentColor = roles.contentPrimary
        ) { content() }
    }
}

@Composable
private fun M15TodayReview(dark: Boolean, noShift: Boolean = false) = M15TodayThemeSurface(dark) {
    TodayScreen(
        today = reviewToday,
        todaySummary = if (noShift) "смен нет" else "Северный парк: Дневная · 11 ч",
        tomorrowSummary = "Дневная · 11 ч",
        nextAlarmSummary = "05:45 · На дневную",
        paymentDates = PaymentDates(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)),
        payroll = reviewPayroll(),
        todayShifts = if (noShift) emptyList() else reviewTodayShifts,
        upcomingPayments = reviewUpcomingPayments,
        notes = reviewNotes,
        onAddNote = {},
        onEditNote = {},
        monthAudit = reviewMonthAudit,
        onOpenCalendar = {},
        onOpenAlarms = {},
        onOpenFinance = {},
        onOpenMonthCheck = {},
        todayLayoutSettings = TodayLayoutSettings(),
        onChangeTodayLayoutSettings = {},
        timelineItems = if (noShift) reviewTimelineNoShift else reviewTimelineItems,
        onTimelineAction = {},
        modifier = Modifier.fillMaxSize()
    )
}

@PreviewTest
@Preview(name = "M15 Today normal shift", widthDp = 412, heightDp = 1900, showBackground = true)
@Composable
fun m15TodayNormalShift() = M15TodayReview(dark = false)

@PreviewTest
@Preview(name = "M15 Today no shift", widthDp = 412, heightDp = 1900, showBackground = true)
@Composable
fun m15TodayNoShift() = M15TodayReview(dark = false, noShift = true)

@PreviewTest
@Preview(
    name = "M15 Today dark",
    widthDp = 412,
    heightDp = 1900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun m15TodayDark() = M15TodayReview(dark = true)

@PreviewTest
@Preview(name = "M15 Today large font", widthDp = 412, heightDp = 2300, fontScale = 1.3f, showBackground = true)
@Composable
fun m15TodayLargeFont() = M15TodayReview(dark = false)
