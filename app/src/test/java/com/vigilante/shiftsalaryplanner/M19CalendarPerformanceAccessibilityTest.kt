package com.vigilante.shiftsalaryplanner

import java.io.File
import androidx.compose.ui.graphics.Color
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

    @Test fun minimalFebruaryDoesNotCreateExtraRows() {
        val weeks = calendarVisibleWeeks(YearMonth.of(2021, 2))
        assertEquals(4, weeks.size)
        assertEquals(LocalDate.of(2021, 2, 1), weeks.first().first())
        assertEquals(LocalDate.of(2021, 2, 28), weeks.last().last())
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

    @Test fun calendarCellsGrowWithAccessibilityFontScalingUpToAndBeyondTwoHundredPercent() {
        assertEquals(72f, calendarDayCellHeightDp(compactMode = false, fontScale = 1f), 0.001f)
        assertEquals(90f, calendarDayCellHeightDp(compactMode = false, fontScale = 1.5f), 0.001f)
        assertEquals(108f, calendarDayCellHeightDp(compactMode = false, fontScale = 2f), 0.001f)
        assertEquals(82f, calendarDayCellHeightDp(compactMode = true, fontScale = 2f), 0.001f)
        assertTrue(calendarDayCellHeightDp(compactMode = false, fontScale = 2.5f) > 108f)
    }

    @Test fun selectedAndTodayBadgesMeetReadableTextContrast() {
        val brandLight = Color(0xFF5267E8)
        val brandDark = Color(0xFFA9B8FF)
        val lightForeground = calendarAccessibleBadgeText(Color.White, brandLight)
        val darkForeground = calendarAccessibleBadgeText(Color.White, brandDark)
        assertEquals(Color.White, lightForeground)
        assertEquals(Color.Black, darkForeground)
        assertTrue(calendarTextContrast(lightForeground, brandLight) >= 4.5f)
        assertTrue(calendarTextContrast(darkForeground, brandDark) >= 4.5f)
        for (background in listOf(Color.White, Color.Black, Color(0xFF777777), Color(0xFFB4B4B4))) {
            val chosen = calendarAccessibleBadgeText(Color(0xFF8267D8), background)
            assertTrue("Insufficient contrast at $background", calendarTextContrast(chosen, background) >= 4.5f)
        }
    }

    @Test fun monthGestureGeometryRemainsNonObservableAndDoesNotRestartOnBoundsChanges() {
        val file = File("src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt").readText()
        assertTrue(file.contains("remember(currentMonth) { mutableMapOf<LocalDate, Rect>() }"))
        assertTrue(file.contains(".pointerInput(activeBrushCode, currentMonth)"))
        assertTrue(file.contains("rememberUpdatedState(onDayClick)"))
        assertTrue(file.contains("rememberUpdatedState(onEraseDate)"))
        assertTrue(file.contains("currentDayClick.value(hitDate)"))
        assertTrue(file.contains("currentEraseDate.value(hitDate)"))
        assertFalse(file.contains(".pointerInput(activeBrushCode, cellBounds.size)"))
    }
}
