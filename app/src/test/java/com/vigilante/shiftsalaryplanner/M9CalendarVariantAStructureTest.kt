package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M9CalendarVariantAStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test fun calendarUsesEvolutionReferencePrimitives() {
        val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
        val header = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarHeaderComponents.kt")
        val quick = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt")
        assertTrue(tab.contains("EvolutionSurface("))
        assertTrue(header.contains("EvolutionIconTile("))
        assertTrue(quick.contains("EvolutionSurface("))
        assertFalse(quick.contains(".border(1.dp, appPanelBorderColor()"))
    }

    @Test fun calendarCellsKeepShiftColorAsDataAndUseSelectionOverlay() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        assertTrue(cell.contains("assignmentBackgroundColors"))
        assertTrue(cell.contains("EvolutionShiftIdentityBadge("))
        assertTrue(cell.contains("isSelected"))
        assertTrue(cell.contains("isToday"))
        assertTrue(cell.contains("evolutionColorRoles()"))
    }

    @Test fun normalDayCellsDoNotRequireLegacyOutline() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        assertTrue(cell.contains("val selectionBorder"))
        assertFalse(cell.contains("else -> 1.dp"))
    }

    @Test fun selectedDaySummaryUsesEvolutionSurfaceWithoutChangingAssignments() {
        val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
        assertTrue(tab.contains("SelectedDaySummaryCard("))
        assertTrue(tab.contains("assignments.forEach { assignment ->"))
        assertTrue(tab.contains("role = EvolutionSurfaceRole.PRIMARY"))
    }
    @Test fun selectedDayUsesDateEmphasisAndTintInsteadOfFullCellFrame() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        assertFalse(cell.contains("isSelected -> 2.dp to roles.brandPrimary"))
        assertTrue(cell.contains("isSelectedTint"))
    }

    @Test fun sharedCalendarHeaderDefaultsLegacyWhileCalendarOptsIntoEvolution() {
        val header = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarHeaderComponents.kt")
        val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
        assertTrue(header.contains("useEvolution: Boolean = false"))
        assertTrue(tab.contains("useEvolution = true"))
    }

    @Test fun selectedDayMetadataStaysACompactCornerMarker() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        val metadata = cell.substringAfter("private fun DayMetadataCluster(")
        assertTrue(metadata.contains("val metadataClusterSize = if (compactMode) 11.dp else 12.dp"))
        assertFalse(metadata.contains(".fillMaxSize()"))
    }

    @Test fun shiftIdentityUsesFullWidthRowsForSingleAndMultiWorkplaceDays() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        val singleBadge = cell.substringAfter("private fun EvolutionShiftIdentityBadge(")
            .substringBefore("private fun DayMetadataCluster(")
        assertFalse(cell.contains("fillMaxWidth(0.76f)"))
        assertFalse(singleBadge.contains("fillMaxWidth(0.72f)"))
        assertTrue(singleBadge.contains(".fillMaxWidth()"))
    }

    @Test fun monthCellCapsVisibleAssignmentsAtTwo() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        assertTrue(cell.contains("val visibleAssignments = assignmentShiftCodes.take(2)"))
        assertTrue(cell.contains("visibleAssignments.forEachIndexed"))
        assertFalse(cell.contains("assignmentShiftCodes.forEachIndexed"))
    }

    @Test fun monthCellShowsOverflowInsideSecondAssignmentRow() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        assertTrue(cell.contains("val hiddenAssignmentCount = (assignmentCount - visibleAssignments.size).coerceAtLeast(0)"))
        assertTrue(cell.contains("index == visibleAssignments.lastIndex && hiddenAssignmentCount > 0"))
        assertTrue(cell.contains("text = \"+\$hiddenAssignmentCount\""))
    }

    @Test fun overflowIndicatorReservesHorizontalSpaceInsteadOfClipping() {
        val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
        assertTrue(cell.contains("val isOverflowRow = index == visibleAssignments.lastIndex && hiddenAssignmentCount > 0"))
        assertTrue(cell.contains("val overflowEndPadding = if (isOverflowRow)"))
        assertTrue(cell.contains(".align(Alignment.CenterEnd)"))
    }

}
