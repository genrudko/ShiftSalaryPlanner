package com.vigilante.shiftsalaryplanner

import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M19CalendarPerformanceAccessibilityTest {
    @Test fun leapYearCalendarHasMondayFirstCompleteWeeks() {
        val weeks = calendarVisibleWeeks(YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 1, 29), weeks.first().first())
        assertEquals(LocalDate.of(2024, 3, 3), weeks.last().last())
        assertEquals(35, weeks.flatten().size)
        assertTrue(weeks.all { it.size == 7 })
    }

    @Test fun sixWeekMonthHasFortyTwoUniqueDates() {
        val dates = calendarVisibleWeeks(YearMonth.of(2026, 8)).flatten()
        assertEquals(42, dates.size)
        assertEquals(dates.size, dates.toSet().size)
        assertEquals(LocalDate.of(2026, 7, 27), dates.first())
        assertEquals(LocalDate.of(2026, 9, 6), dates.last())
    }

    @Test fun spokenDayIncludesAllNonVisualStateAndMultipleWorkplaces() {
        val label = calendarDayAccessibilityLabel(
            date = LocalDate.of(2026, 10, 12),
            today = LocalDate.of(2026, 10, 12),
            isCurrentMonthCell = true,
            isSpecialDay = true,
            isSelected = true,
            isInPreviewRange = true,
            shiftLabels = listOf("Работа 1: Д", "Работа 2: Н"),
            hasNote = true,
            hasShiftOverride = true
        )
        assertTrue(label.contains("12 октября 2026"))
        listOf("Сегодня", "Выбран", "Нерабочий", "выделенном", "Работа 1: Д", "Работа 2: Н", "Есть заметка", "Индивидуальная").forEach {
            assertTrue("Missing status: $it", label.contains(it))
        }
    }

    @Test fun spokenDayDoesNotInventAbsentShiftsOrBadges() {
        val label = calendarDayAccessibilityLabel(
            LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 12),
            false, false, false, false, emptyList(), false, false
        )
        assertTrue(label.contains("Другой месяц"))
        assertFalse(label.contains("Смена:"))
        assertFalse(label.contains("Есть заметка"))
    }

    @Test fun monthGestureGeometryRemainsNonObservableAndDoesNotRestartOnBoundsChanges() {
        val file = File("src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt").readText()
        assertTrue(file.contains("remember(currentMonth) { mutableMapOf<LocalDate, Rect>() }"))
        assertTrue(file.contains(".pointerInput(activeBrushCode, currentMonth)"))
        assertFalse(file.contains(".pointerInput(activeBrushCode, cellBounds.size)"))
    }
}
