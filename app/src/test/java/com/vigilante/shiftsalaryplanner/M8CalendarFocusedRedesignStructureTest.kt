package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class M8CalendarFocusedRedesignStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first { File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists() }

    @Test fun monthSelectionIsSeparateFromLegacyShiftPicker() {
        val main = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        assertTrue(main.contains("calendarInteractionState.selectedDate = date"))
        assertTrue(main.contains("calendarInteractionState.shiftPickerDate?.let { date ->"))
    }

    @Test fun calendarMonthExposesSelectedDaySummaryAndSelectedCellState() {
        val calendar = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt").readText()
        assertTrue(calendar.contains("selectedDate: LocalDate?"))
        assertTrue(calendar.contains("SelectedDaySummaryCard("))
        assertTrue(calendar.contains("isSelected = date == selectedDate"))
    }

    @Test fun dayCellDistinguishesSelectedFromToday() {
        val cell = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt").readText()
        assertTrue(cell.contains("isSelected: Boolean"))
        assertTrue(cell.contains("isSelected -> MaterialTheme.colorScheme.primary"))
    }
}
