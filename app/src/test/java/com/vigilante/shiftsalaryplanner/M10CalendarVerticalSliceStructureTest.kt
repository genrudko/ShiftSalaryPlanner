package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M10CalendarVerticalSliceStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun calendarMonthShellUsesEvolutionSurfacesForOperationalCards() {
        val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
        assertTrue(tab.contains("fun ActiveBrushCard("))
        assertTrue(tab.contains("fun PatternApplyModeCard("))
        assertTrue(tab.contains("fun ClearRangeModeCard("))
        assertTrue(tab.contains("EvolutionSurface("))
        val brush = tab.substringAfter("fun ActiveBrushCard(").substringBefore("private fun MonthCheckInlineCard(")
        val pattern = tab.substringAfter("fun PatternApplyModeCard(").substringBefore("private fun ClearRangeModeCard(")
        val clearRange = tab.substringAfter("private fun ClearRangeModeCard(")
        assertFalse(brush.contains("AppExpressiveSurface("))
        assertFalse(pattern.contains("AppExpressiveSurface("))
        assertFalse(clearRange.contains("AppExpressiveSurface("))
    }

    @Test
    fun calendarSecondaryMonthPanelsDoNotReturnToOutlinedPanelSea() {
        val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
        val holidays = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/MonthHolidayInfoCard.kt")
        val secondary = tab.substringAfter("private fun MonthCheckInlineCard(").substringBefore("fun PatternApplyModeCard(")
        assertFalse(secondary.contains("AppExpressiveSurface("))
        assertFalse(holidays.contains("AppExpressiveSurface("))
    }
    @Test
    fun quickAssignmentAndPickerUseEvolutionGrammar() {
        val quick = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt")
        val picker = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/ShiftPickerDialog.kt")
        assertTrue(quick.contains("EvolutionSurface("))
        assertFalse(quick.contains("readableContentColor(appPanelColor())"))
        assertTrue(picker.contains("EvolutionSurface("))
        assertFalse(picker.contains("AppExpressiveSurface("))
    }

    @Test
    fun dayDetailUsesEvolutionSurfacesWithoutChangingOverrideContract() {
        val day = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/DayAssignmentsDialog.kt")
        assertTrue(day.contains("fun DayAssignmentsDialog("))
        assertTrue(day.contains("onSaveShiftDayOverride"))
        assertTrue(day.contains("EvolutionSurface("))
        assertFalse(day.contains("AppExpressiveSurface("))
        assertFalse(day.contains("containerColor = appPanelColor()"))
    }

    @Test
    fun dayDetailExposesRealContentForDeterministicVisualQualification() {
        val day = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/DayAssignmentsDialog.kt")
        assertTrue(day.contains("internal fun DayAssignmentsContent("))
        assertTrue(day.contains("text = {\n            DayAssignmentsContent("))
    }

}
