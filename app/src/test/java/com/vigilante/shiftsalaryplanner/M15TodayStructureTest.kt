package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Focused JVM structure tests for M15.1 Task 2 — Today presentation extraction.
 *
 * Contract under verification:
 * - Today-specific screen and model declarations live under `ui/today`
 *   (`TodayScreen.kt`, `TodayModels.kt`).
 * - `WorkflowServiceScreens.kt` no longer owns `TodayTabScreen`,
 *   `TodayShiftPreview` or `UpcomingPaymentItem` after the extraction.
 * - The `MainActivity` Today callsite keeps its parameter/type contract.
 */
class M15TodayStructureTest {
    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    @Test
    fun `M15 declares Today models under ui today`() {
        val models = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayModels.kt")
        assertTrue("ui/today/TodayModels.kt must exist after extraction", models.exists())
        val text = models.readText()
        assertTrue(
            "ui/today/TodayModels.kt must declare TodayShiftPreview",
            text.contains("data class TodayShiftPreview")
        )
        assertTrue(
            "ui/today/TodayModels.kt must declare UpcomingPaymentItem",
            text.contains("data class UpcomingPaymentItem")
        )
    }

    @Test
    fun `M15 declares Today screen under ui today`() {
        val screen = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt")
        assertTrue("ui/today/TodayScreen.kt must exist after extraction", screen.exists())
        assertTrue(
            "ui/today/TodayScreen.kt must declare the Today screen composable",
            screen.readText().contains("fun TodayScreen(")
        )
    }

    @Test
    fun `M15 WorkflowServiceScreens no longer owns today screen and models`() {
        val workflow = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkflowServiceScreens.kt").readText()
        assertFalse(
            "WorkflowServiceScreens.kt must not declare TodayTabScreen after extraction",
            workflow.contains("TodayTabScreen")
        )
        assertFalse(
            "WorkflowServiceScreens.kt must not declare TodayShiftPreview after extraction",
            workflow.contains("data class TodayShiftPreview")
        )
        assertFalse(
            "WorkflowServiceScreens.kt must not declare UpcomingPaymentItem after extraction",
            workflow.contains("data class UpcomingPaymentItem")
        )
    }

    @Test
    fun `M15 preserves MainActivity today callsite and type contract`() {
        val main = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        assertTrue("MainActivity must call the extracted TodayScreen", main.contains("TodayScreen("))
        assertTrue("MainActivity must pass todaySummary", main.contains("todaySummary = todayWorkSummary"))
        assertTrue("MainActivity must pass tomorrowSummary", main.contains("tomorrowSummary = tomorrowWorkSummary"))
        assertTrue("MainActivity must pass nextAlarmSummary", main.contains("nextAlarmSummary = nextAlarmSummary"))
        assertTrue("MainActivity must pass paymentDates", main.contains("paymentDates = paymentDates"))
        assertTrue("MainActivity must pass payroll", main.contains("payroll = payroll"))
        assertTrue("MainActivity must pass todayShifts as list input", main.contains("todayShifts = todayShiftPreviews"))
        assertTrue("MainActivity must pass upcomingPayments as list input", main.contains("upcomingPayments = upcomingPaymentItems"))
        assertTrue("MainActivity must pass notes", main.contains("notes = todayNotes"))
        assertTrue("MainActivity must pass monthAudit", main.contains("monthAudit = calendarMonthAudit"))
        assertTrue("MainActivity must keep the calendar callback", main.contains("onOpenCalendar = { navigationState = navigationState.selectTab(BottomTab.CALENDAR) }"))
        assertTrue("MainActivity must keep the alarms callback", main.contains("onOpenAlarms = { navigationState = navigationState.selectTab(BottomTab.ALARMS) }"))
        assertTrue("MainActivity must keep the layout settings input", main.contains("todayLayoutSettings = todayLayoutSettings"))
        assertTrue("MainActivity must keep the layout change callback", main.contains("onChangeTodayLayoutSettings = { settingsData.saveTodayLayoutSettings(it) }"))
        assertTrue("MainActivity must keep the fullscreen modifier", main.contains("modifier = Modifier.fillMaxSize()"))
    }
}
