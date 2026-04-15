package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.WorkShiftItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class OvertimePeriodInfo(
    val label: String,
    val startMonth: YearMonth,
    val endMonth: YearMonth
) {
    val startDate: LocalDate get() = startMonth.atDay(1)
    val endDate: LocalDate get() = endMonth.atEndOfMonth()
}

fun formatMonthYearTitle(month: YearMonth): String {
    val locale = Locale.forLanguageTag("ru-RU")
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    return month.atDay(1).format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
    }
}

fun resolveOvertimePeriodInfo(
    currentMonth: YearMonth,
    overtimePeriodName: String
): OvertimePeriodInfo {
    val period = runCatching { OvertimePeriod.valueOf(overtimePeriodName) }.getOrElse { OvertimePeriod.YEAR }
    return when (period) {
        OvertimePeriod.MONTH -> OvertimePeriodInfo(
            label = formatMonthYearTitle(currentMonth),
            startMonth = currentMonth,
            endMonth = currentMonth
        )

        OvertimePeriod.QUARTER -> {
            val startMonthNumber = ((currentMonth.monthValue - 1) / 3) * 3 + 1
            val startMonth = YearMonth.of(currentMonth.year, startMonthNumber)
            val endMonth = startMonth.plusMonths(2)
            val quarterNumber = ((currentMonth.monthValue - 1) / 3) + 1
            OvertimePeriodInfo(
                label = "$quarterNumber квартал ${currentMonth.year}",
                startMonth = startMonth,
                endMonth = endMonth
            )
        }

        OvertimePeriod.HALF_YEAR -> {
            val firstHalf = currentMonth.monthValue <= 6
            val startMonth = if (firstHalf) YearMonth.of(currentMonth.year, 1) else YearMonth.of(currentMonth.year, 7)
            val endMonth = if (firstHalf) YearMonth.of(currentMonth.year, 6) else YearMonth.of(currentMonth.year, 12)
            OvertimePeriodInfo(
                label = if (firstHalf) "1 полугодие ${currentMonth.year}" else "2 полугодие ${currentMonth.year}",
                startMonth = startMonth,
                endMonth = endMonth
            )
        }

        OvertimePeriod.YEAR -> OvertimePeriodInfo(
            label = "${currentMonth.year} год",
            startMonth = YearMonth.of(currentMonth.year, 1),
            endMonth = YearMonth.of(currentMonth.year, 12)
        )
    }
}

fun calculateNormHoursForMonth(
    month: YearMonth,
    payrollSettings: PayrollSettings,
    normMode: NormMode,
    annualNormSourceMode: AnnualNormSourceMode,
    holidayMap: Map<LocalDate, HolidayEntity>
): Double {
    return when (normMode) {
        NormMode.MANUAL -> payrollSettings.monthlyNormHours.coerceAtLeast(0.0)
        NormMode.PRODUCTION_CALENDAR -> calculateProductionCalendarMonthInfo(
            month = month,
            holidayMap = holidayMap,
            workdayHours = payrollSettings.workdayHours
        ).normHours

        NormMode.AVERAGE_ANNUAL -> when (annualNormSourceMode) {
            AnnualNormSourceMode.WORKDAY_HOURS -> calculateAverageAnnualNormHours(
                year = month.year,
                holidayMap = holidayMap,
                workdayHours = payrollSettings.workdayHours
            )

            AnnualNormSourceMode.YEAR_TOTAL_HOURS -> (payrollSettings.annualNormHours / 12.0).coerceAtLeast(0.0)
        }

        NormMode.AVERAGE_QUARTERLY -> calculateAverageQuarterNormHours(
            month = month,
            holidayMap = holidayMap,
            workdayHours = payrollSettings.workdayHours
        )
    }
}

fun calculateNormHoursForPeriod(
    periodInfo: OvertimePeriodInfo,
    payrollSettings: PayrollSettings,
    normMode: NormMode,
    annualNormSourceMode: AnnualNormSourceMode,
    holidayMap: Map<LocalDate, HolidayEntity>
): Double {
    var month = periodInfo.startMonth
    var total = 0.0
    while (!month.isAfter(periodInfo.endMonth)) {
        total += calculateNormHoursForMonth(
            month = month,
            payrollSettings = payrollSettings,
            normMode = normMode,
            annualNormSourceMode = annualNormSourceMode,
            holidayMap = holidayMap
        )
        month = month.plusMonths(1)
    }
    return total
}

fun calculateExpectedWorkHoursForDate(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double,
    applyShortDayReduction: Boolean
): Double {
    if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return 0.0
    val holiday = holidayMap[date]
    if (holiday?.isNonWorking == true) return 0.0
    if (holiday?.kind == HolidayKinds.SHORT_DAY) {
        return if (applyShortDayReduction) (workdayHours - 1.0).coerceAtLeast(0.0) else workdayHours.coerceAtLeast(0.0)
    }
    return workdayHours.coerceAtLeast(0.0)
}

fun calculateAdjustedNormHoursForPeriod(
    basePeriodNormHours: Double,
    shifts: List<WorkShiftItem>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double,
    applyShortDayReduction: Boolean
): Double {
    val excludedNormHours = shifts
        .filter { (it.isVacation || it.isSickLeave) && it.date != null }
        .sumOf { shift ->
            calculateExpectedWorkHoursForDate(
                date = shift.date!!,
                holidayMap = holidayMap,
                workdayHours = workdayHours,
                applyShortDayReduction = applyShortDayReduction
            )
        }
    return (basePeriodNormHours - excludedNormHours).coerceAtLeast(0.0)
}

data class ProductionCalendarMonthInfo(
    val normHours: Double,
    val workingDays: Int,
    val shortDays: Int
)

fun calculateProductionCalendarMonthInfo(
    month: YearMonth,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double
): ProductionCalendarMonthInfo {
    var normHours = 0.0
    var workingDays = 0
    var shortDays = 0

    var date = month.atDay(1)
    val endDate = month.atEndOfMonth()
    while (!date.isAfter(endDate)) {
        val holiday = holidayMap[date]
        val isWeekend = isWeekendDay(date)

        when {
            isWeekend -> Unit
            holiday?.kind == HolidayKinds.SHORT_DAY -> {
                workingDays += 1
                shortDays += 1
                normHours += (workdayHours - 1.0).coerceAtLeast(0.0)
            }

            holiday?.isNonWorking == true -> Unit
            else -> {
                workingDays += 1
                normHours += workdayHours
            }
        }
        date = date.plusDays(1)
    }

    return ProductionCalendarMonthInfo(
        normHours = normHours,
        workingDays = workingDays,
        shortDays = shortDays
    )
}

fun calculateAverageAnnualNormHours(
    year: Int,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double
): Double {
    val total = (1..12).sumOf { monthNumber ->
        calculateProductionCalendarMonthInfo(
            month = YearMonth.of(year, monthNumber),
            holidayMap = holidayMap,
            workdayHours = workdayHours
        ).normHours
    }
    return total / 12.0
}

fun calculateAverageQuarterNormHours(
    month: YearMonth,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double
): Double {
    val quarterStartMonth = ((month.monthValue - 1) / 3) * 3 + 1
    val total = (quarterStartMonth until quarterStartMonth + 3).sumOf { monthNumber ->
        calculateProductionCalendarMonthInfo(
            month = YearMonth.of(month.year, monthNumber),
            holidayMap = holidayMap,
            workdayHours = workdayHours
        ).normHours
    }
    return total / 3.0
}

fun calculateAnnualNormHoursForYear(
    year: Int,
    payrollSettings: PayrollSettings,
    normMode: NormMode,
    annualNormSourceMode: AnnualNormSourceMode,
    holidayMap: Map<LocalDate, HolidayEntity>
): Double {
    return when (normMode) {
        NormMode.MANUAL -> payrollSettings.monthlyNormHours.coerceAtLeast(0.0) * 12.0
        NormMode.PRODUCTION_CALENDAR,
        NormMode.AVERAGE_QUARTERLY -> {
            (1..12).sumOf { monthNumber ->
                calculateProductionCalendarMonthInfo(
                    month = YearMonth.of(year, monthNumber),
                    holidayMap = holidayMap,
                    workdayHours = payrollSettings.workdayHours
                ).normHours
            }
        }

        NormMode.AVERAGE_ANNUAL -> when (annualNormSourceMode) {
            AnnualNormSourceMode.WORKDAY_HOURS -> {
                calculateAverageAnnualNormHours(
                    year = year,
                    holidayMap = holidayMap,
                    workdayHours = payrollSettings.workdayHours
                ) * 12.0
            }

            AnnualNormSourceMode.YEAR_TOTAL_HOURS -> payrollSettings.annualNormHours.coerceAtLeast(0.0)
        }
    }
}

