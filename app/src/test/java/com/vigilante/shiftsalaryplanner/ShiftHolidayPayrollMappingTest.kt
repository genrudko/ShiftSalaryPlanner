package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayCompensation
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShiftHolidayPayrollMappingTest {

    @Test
    fun toWorkShiftItemForDate_marksManualNonWorkingHolidayAsDoublePay() {
        val date = LocalDate.of(2026, 5, 9)
        val template = ShiftTemplateEntity(
            code = "D",
            title = "Дневная",
            iconKey = "SUN",
            totalHours = 12.0,
            breakHours = 1.0,
            nightHours = 0.0,
            colorHex = "#1E88E5",
            isWeekendPaid = false,
            active = true,
            sortOrder = 1
        )
        val manualHoliday = HolidayEntity(
            id = "manual|2026-05-09",
            date = date.toString(),
            title = "Тестовый праздник",
            scopeCode = "MANUAL",
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )

        val item = template.toWorkShiftItemForDate(
            date = date,
            holidayMap = mapOf(date to manualHoliday),
            applyShortDayReduction = false,
            specialRule = null
        )

        assertEquals(SpecialDayType.WEEKEND_HOLIDAY.name, item.specialDayType)
        assertEquals(SpecialDayCompensation.DOUBLE_PAY.name, item.specialDayCompensation)
        assertTrue(item.isWeekendPaid)
    }

    @Test
    fun toWorkShiftItemForDate_splitsNightShiftHolidayHoursByCalendarDay() {
        val date = LocalDate.of(2026, 4, 21)
        val template = ShiftTemplateEntity(
            code = "N",
            title = "Ночная",
            iconKey = "NIGHT",
            totalHours = 12.0,
            breakHours = 0.5,
            nightHours = 8.0,
            colorHex = "#3949AB",
            isWeekendPaid = false,
            active = true,
            sortOrder = 2
        )
        val manualHoliday = HolidayEntity(
            id = "manual|2026-04-21",
            date = date.toString(),
            title = "Тестовый праздник",
            scopeCode = "MANUAL",
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )

        val item = template.toWorkShiftItemForDate(
            date = date,
            holidayMap = mapOf(date to manualHoliday),
            applyShortDayReduction = false,
            specialRule = null
        )

        assertEquals(11.5, item.paidHours, 0.001)
        assertEquals(4.0, item.holidayPaidHours ?: 0.0, 0.001)
        assertEquals(SpecialDayType.WEEKEND_HOLIDAY.name, item.specialDayType)
        assertEquals(SpecialDayCompensation.DOUBLE_PAY.name, item.specialDayCompensation)
    }

    @Test
    fun toWorkShiftItemForDate_usesConfiguredShiftStartTimeFromTemplateSettings() {
        val date = LocalDate.of(2026, 5, 1)
        val template = ShiftTemplateEntity(
            code = "D2",
            title = "Поздняя",
            iconKey = "SUN",
            totalHours = 6.0,
            breakHours = 0.0,
            nightHours = 0.0,
            colorHex = "#00897B",
            isWeekendPaid = false,
            active = true,
            sortOrder = 1
        )
        val holiday = HolidayEntity(
            id = "manual|2026-05-01",
            date = date.toString(),
            title = "Праздник",
            scopeCode = "MANUAL",
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )
        val timing = ShiftTemplateAlarmConfig(
            shiftCode = "D2",
            enabled = false,
            startHour = 22,
            startMinute = 0,
            endHour = 4,
            endMinute = 0
        )

        val item = template.toWorkShiftItemForDate(
            date = date,
            holidayMap = mapOf(date to holiday),
            applyShortDayReduction = false,
            specialRule = null,
            shiftTiming = timing
        )

        assertEquals(6.0, item.paidHours, 0.001)
        assertEquals(2.0, item.holidayPaidHours ?: 0.0, 0.001)
    }
}
