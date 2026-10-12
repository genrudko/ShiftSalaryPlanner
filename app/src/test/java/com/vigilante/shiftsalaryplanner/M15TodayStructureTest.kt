package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.TodayBubbleBackground
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleId
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleSettings
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleSize
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleWidth
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import com.vigilante.shiftsalaryplanner.settings.defaultTodayBubbles
import com.vigilante.shiftsalaryplanner.settings.parseTodayLayoutSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun `M15 normalization excludes fixed top legacy IDs from editable lower modules`() {
        val legacyLayout = TodayLayoutSettings(
            bubbles = listOf(
                TodayBubbleSettings(TodayBubbleId.NEXT_STEP, width = TodayBubbleWidth.HALF),
                TodayBubbleSettings(
                    TodayBubbleId.HERO,
                    size = TodayBubbleSize.LARGE,
                    background = TodayBubbleBackground.ACCENT
                ),
                TodayBubbleSettings(TodayBubbleId.QUICK_ACTIONS),
                TodayBubbleSettings(TodayBubbleId.PULSE, width = TodayBubbleWidth.HALF),
                TodayBubbleSettings(TodayBubbleId.MONTH_STATUS, width = TodayBubbleWidth.HALF),
                TodayBubbleSettings(TodayBubbleId.QUICK_ACTIONS, visible = false)
            )
        )

        val normalized = legacyLayout.normalized()

        assertTrue(
            "fixed-top legacy IDs must not remain independently reorderable lower modules",
            normalized.bubbles.none { bubble -> bubble.id.isFixedTopModule }
        )
        assertEquals(5, normalized.bubbles.size)
        assertEquals(
            setOf(
                TodayBubbleId.TOMORROW,
                TodayBubbleId.NEXT_ALARM,
                TodayBubbleId.MONTH_STATUS,
                TodayBubbleId.PAYMENTS,
                TodayBubbleId.NOTES
            ),
            normalized.bubbles.map { it.id }.toSet()
        )
    }

    @Test
    fun `M15 default Today layout contains only configurable lower modules`() {
        assertEquals(
            listOf(
                TodayBubbleId.TOMORROW,
                TodayBubbleId.NEXT_ALARM,
                TodayBubbleId.MONTH_STATUS,
                TodayBubbleId.PAYMENTS,
                TodayBubbleId.NOTES
            ),
            defaultTodayBubbles().map { it.id }
        )
        assertTrue(defaultTodayBubbles().none { it.id.isFixedTopModule })
    }

    @Test
    fun `M15 historical serialized Today layout with fixed top IDs remains parseable`() {
        val legacyJson = """
            [
              {"id":"HERO","visible":true,"size":"LARGE","width":"FULL","background":"ACCENT"},
              {"id":"PULSE","visible":true,"size":"NORMAL","width":"HALF","background":"DEFAULT"},
              {"id":"NEXT_STEP","visible":true,"size":"NORMAL","width":"HALF","background":"DEFAULT"},
              {"id":"QUICK_ACTIONS","visible":true,"size":"NORMAL","width":"FULL","background":"DEFAULT"},
              {"id":"TOMORROW","visible":true,"size":"COMPACT","width":"HALF","background":"DEFAULT"},
              {"id":"MONTH_STATUS","visible":false,"size":"NORMAL","width":"HALF","background":"DEFAULT"},
              {"id":"NOTES","visible":true,"size":"NORMAL","width":"FULL","background":"SOFT"}
            ]
        """.trimIndent()

        val parsed = parseTodayLayoutSettings(legacyJson)

        assertNotNull("legacy Today layout JSON must remain parseable", parsed)
        val parsedIds = parsed.bubbles.map { it.id }
        assertTrue(
            "legacy fixed-top IDs must remain parseable on read",
            parsedIds.containsAll(
                listOf(
                    TodayBubbleId.HERO,
                    TodayBubbleId.PULSE,
                    TodayBubbleId.NEXT_STEP,
                    TodayBubbleId.QUICK_ACTIONS
                )
            )
        )
        val normalized = parsed.normalized()
        assertTrue(
            "normalized view must exclude fixed-top IDs",
            normalized.bubbles.none { it.id.isFixedTopModule }
        )
        assertTrue(
            "normalized view must keep Tomorrow",
            normalized.bubbles.any { it.id == TodayBubbleId.TOMORROW }
        )
        assertTrue(
            "normalized view must keep Month status even when previously hidden",
            normalized.bubbles.any { it.id == TodayBubbleId.MONTH_STATUS }
        )
    }

    @Test
    fun `M15 TodayScreen consumes normalized timeline input and renders fixed hierarchy`() {
        val screen = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt").readText()
        assertTrue("TodayScreen must consume timelineItems", screen.contains("timelineItems: List<TimelineItem>"))
        assertTrue("TodayScreen must consume onTimelineAction", screen.contains("onTimelineAction"))
        assertTrue("TodayScreen must render a fixed timeline section", screen.contains("TodayTimelineSection("))
        val hero = screen.indexOf("TodayHeroCard(")
        val timeline = screen.indexOf("TodayTimelineSection(")
        val lower = screen.indexOf("TodayBubbleTileRows(")
        assertTrue("fixed Hero must be present", hero >= 0)
        assertTrue("fixed Timeline must be present", timeline >= 0)
        assertTrue("configurable lower modules must be present", lower >= 0)
        assertTrue("fixed hierarchy must be Hero -> Timeline -> lower modules", hero < timeline && timeline < lower)
    }

    @Test
    fun `M15 TodayScreen accepts deterministic today snapshot`() {
        val screen = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt").readText()
        assertTrue(
            "TodayScreen must accept an injectable today for wall-clock-independent screenshots",
            screen.contains("today: LocalDate = LocalDate.now()")
        )
    }

    @Test
    fun `M15 layout settings declare fixed top legacy modules`() {
        val store = File(
            root(),
            "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/TodayLayoutSettingsStore.kt"
        ).readText()
        assertTrue("TodayBubbleId must declare isFixedTopModule", store.contains("isFixedTopModule"))
        assertTrue(
            "store must keep legacy fixed-top enum IDs parseable",
            store.contains("HERO") && store.contains("PULSE") && store.contains("NEXT_STEP") && store.contains("QUICK_ACTIONS")
        )
        assertTrue(
            "default bubbles must not seed fixed-top IDs",
            !store.substringAfter("fun defaultTodayBubbles").contains("TodayBubbleId.HERO")
        )
    }

    @Test
    fun `M15 MainActivity supplies normalized timeline input and action callbacks`() {
        val main = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        assertTrue("MainActivity must build the normalized timeline", main.contains("buildTodayTimeline("))
        assertTrue("MainActivity must pass ordered Today and Weather timeline items",
            main.contains("timelineItems = buildTodayTimeline(") &&
                main.contains("todayTimelineItems + weatherTransitionsToTimeline("))
        assertTrue("MainActivity must pass onTimelineAction", main.contains("onTimelineAction ="))
    }

    // --- M15.1 Task 5: external-calendar layer settings, runtime permission, + Event ---

    @Test
    fun `M15 Task5 external calendar settings store persists layer flag and calendar ids separately`() {
        val settingsPath = File(
            root(),
            "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/TodayExternalCalendarSettingsStore.kt"
        )
        assertTrue("focused Today external-calendar settings store must exist", settingsPath.exists())
        val text = settingsPath.readText()
        assertTrue(
            "store must declare the TodayExternalCalendarSettings model",
            text.contains("data class TodayExternalCalendarSettings")
        )
        assertTrue(
            "model must default the layer to disabled",
            text.contains("layerEnabled: Boolean = false")
        )
        assertTrue(
            "store must keep its own profile prefs file",
            text.contains("today_external_calendar_settings")
        )
        assertTrue(
            "store must persist the enabled flag via SharedPreferences",
            text.contains("putBoolean(KEY_LAYER_ENABLED")
        )
        assertTrue(
            "store must persist enabled calendar ids",
            text.contains("enabled_calendar_ids")
        )
        assertTrue("store must expose save", text.contains("fun save"))
        assertTrue("store must expose settingsFlow", text.contains("settingsFlow"))
        assertTrue(
            "store must expose pure serializer for enabled calendar ids",
            text.contains("fun serializeEnabledCalendarIds")
        )
        assertTrue(
            "store must expose pure parser for enabled calendar ids",
            text.contains("fun parseEnabledCalendarIds")
        )
        assertFalse(
            "focused settings store must not duplicate Today layout responsibilities",
            text.contains("class TodayLayoutSettings")
        )
        val layoutStore = File(
            root(),
            "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/TodayLayoutSettingsStore.kt"
        ).readText()
        assertTrue(
            "existing Today layout store must remain intact",
            layoutStore.contains("today_layout_settings")
        )
    }

    @Test
    fun `M15 Task5 enabled calendar ids feed the read only external calendar source`() {
        val main = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        assertTrue(
            "MainActivity must instantiate the read-only Android calendar source",
            main.contains("AndroidCalendarSource(")
        )
        assertTrue("MainActivity must query source occurrences", main.contains(".occurrences("))
        assertTrue(
            "MainActivity must restrict occurrences to enabled calendar ids",
            main.contains("enabledCalendarIds = todayExternalCalendarSettings.enabledCalendarIds")
        )
        assertTrue(
            "MainActivity must read the settings state from the port flow",
            main.contains("todayExternalCalendarSettings")
        )
    }

    @Test
    fun `M15 Task5 MainActivity requests runtime READ_CALENDAR when enabling the layer`() {
        val main = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        assertTrue(
            "runtime permission flow must use the activity result permission contract",
            main.contains("ActivityResultContracts.RequestPermission()")
        )
        assertTrue(
            "enabling the layer must launch READ_CALENDAR",
            main.contains("Manifest.permission.READ_CALENDAR")
        )
        assertTrue(
            "permission state must be re-checked on toggle",
            main.contains("checkSelfPermission")
        )
        assertFalse(
            "MainActivity must never reference WRITE_CALENDAR",
            main.contains("Manifest.permission.WRITE_CALENDAR")
        )
    }

    @Test
    fun `M15 Task5 Today exposes layer control and plus event action`() {
        val screen = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt").readText()
        assertTrue(
            "TodayScreen must accept the layer enabled flag",
            screen.contains("externalCalendarLayerEnabled: Boolean = false")
        )
        assertTrue(
            "TodayScreen must accept a layer toggle callback",
            screen.contains("onToggleExternalCalendarLayer: () -> Unit")
        )
        assertTrue(
            "TodayScreen must accept an actionable permission-denied flag",
            screen.contains("externalCalendarPermissionDenied: Boolean = false")
        )
        assertTrue(
            "TodayScreen must accept a permission re-request callback",
            screen.contains("onRequestExternalCalendarPermission: () -> Unit")
        )
        assertTrue(
            "TodayScreen must accept the plus Event callback",
            screen.contains("onAddExternalEvent: () -> Unit")
        )
        assertTrue(
            "TodayScreen must render the calendar layer control",
            screen.contains("TodayExternalCalendarControl(")
        )
    }

    @Test
    fun `M15 Task5 plus event delegates to system calendar insert intent`() {
        val intentPath = File(
            root(),
            "app/src/main/java/com/vigilante/shiftsalaryplanner/integrations/calendar/SystemEventInsertIntent.kt"
        )
        assertTrue("system event insert intent helper must exist", intentPath.exists())
        val intent = intentPath.readText()
        assertTrue(
            "plus Event must use ACTION_INSERT",
            intent.contains("Intent.ACTION_INSERT")
        )
        assertTrue(
            "plus Event must target CalendarContract.Events.CONTENT_URI",
            intent.contains("CalendarContract.Events.CONTENT_URI")
        )
        assertTrue(
            "plus Event must seed the current date/time",
            intent.contains("EXTRA_EVENT_BEGIN_TIME")
        )
        assertFalse(
            "intent helper must not write into the provider",
            intent.contains("ContentValues")
        )
        val main = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        assertTrue("MainActivity must build the system insert intent", main.contains("buildSystemEventInsertIntent("))
        assertTrue("MainActivity must delegate via startActivity", main.contains("startActivity("))
    }

    @Test
    fun `M15 Task5 no local event persistence and no write calendar permission`() {
        val manifest = File(root(), "app/src/main/AndroidManifest.xml").readText()
        assertEquals(
            "manifest must declare READ_CALENDAR exactly once",
            1,
            Regex("android.permission.READ_CALENDAR").findAll(manifest).count()
        )
        assertEquals(
            "manifest must never declare WRITE_CALENDAR",
            0,
            Regex("android.permission.WRITE_CALENDAR").findAll(manifest).count()
        )

        val sourceDir = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner")
        val declarations = listOf("class PlannedEvent", "class PlannedEventEntity", "interface PlannedEventDao")
        val violations = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().let { text -> declarations.any { text.contains(it) } } }
            .map { it.name }
            .toList()
        assertTrue(
            "no app-owned calendar event model may be declared, found in: $violations",
            violations.isEmpty()
        )

        val calendarAdapters = listOf(
            File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/integrations/calendar/AndroidCalendarSource.kt"),
            File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/integrations/calendar/SystemEventInsertIntent.kt")
        )
        for (file in calendarAdapters) {
            val text = file.readText()
            assertFalse("${file.name} must not insert provider rows", text.contains(".insert("))
            assertFalse("${file.name} must not update provider rows", text.contains(".update("))
            assertFalse("${file.name} must not delete provider rows", text.contains(".delete("))
            assertFalse("${file.name} must not batch-write provider rows", text.contains("applyBatch"))
            assertFalse("${file.name} must not build ContentValues", text.contains("ContentValues"))
        }
    }
}
