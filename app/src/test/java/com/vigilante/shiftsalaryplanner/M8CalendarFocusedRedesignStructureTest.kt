package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
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
    @Test fun dayCellUsesExpressiveDateAndShiftIdentityBadges() {
        val cell = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt").readText()
        assertTrue(cell.contains("DayDateBadge("))
        assertTrue(cell.contains("SingleShiftIdentityBadge("))
        assertTrue(cell.contains("workplaceBadgeLabel(assignmentWorkplaceIds.firstOrNull())"))
    }

    @Test fun dayMetadataUsesOneTopClusterInsteadOfCompetingWithShiftIdentity() {
        val cell = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt").readText()
        assertTrue(cell.contains("DayMetadataCluster("))
        assertTrue(cell.contains("Alignment.TopEnd"))
    }

    @Test fun multiWorkplaceSegmentsKeepReadableIdentityChips() {
        val cell = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt").readText()
        assertTrue(cell.contains("segmentIdentityBackground"))
        assertTrue(cell.contains("workplaceBadge"))
    }

    @Test fun selectedDaySummaryShowsEveryAssignmentInsteadOfCollapsingThem() {
        val calendar = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt").readText()
        assertTrue(calendar.contains("SelectedDayAssignmentRow("))
        assertTrue(calendar.contains("assignments.forEach { assignment ->"))
        assertFalse(calendar.contains("if (assignments.size > 1) append("))
    }

    @Test fun selectedDaySummaryUsesCompactSemanticMarkerPills() {
        val calendar = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt").readText()
        assertTrue(calendar.contains("SelectedDayMarkerPill("))
        assertTrue(calendar.contains("FilledTonalButton"))
    }

    @Test fun calendarTodayIsInjectableForDeterministicVisualReview() {
        val calendar = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt").readText()
        val cell = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt").readText()
        assertTrue(calendar.contains("today: LocalDate = LocalDate.now()"))
        assertTrue(calendar.contains("today = today"))
        assertTrue(cell.contains("today: LocalDate"))
        assertTrue(cell.contains("val isToday = date == today"))
    }

    @Test fun calendarCornerBadgesReserveNonOverlappingSpace() {
        val cell = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt").readText()
        assertTrue(cell.contains("val dateBadgeSize = if (compactMode) 22.dp else 24.dp"))
        assertTrue(cell.contains("val metadataClusterSize = if (compactMode) 15.dp else 17.dp"))
        assertTrue(cell.contains("modifier = modifier.size(metadataClusterSize)"))
    }

}
