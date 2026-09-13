package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M13AlarmDeclutterStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun alarms(): String = File(
        root(),
        "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt"
    ).readText()

    @Test
    fun `alarm root is operational and drills into settings and system status`() {
        val source = alarms()
        assertTrue(source.contains("enum class AlarmScreenPage"))
        assertTrue(source.contains("ROOT"))
        assertTrue(source.contains("SETTINGS"))
        assertTrue(source.contains("SYSTEM_STATUS"))
        assertTrue(source.contains("initialPage: AlarmScreenPage = AlarmScreenPage.ROOT"))
        assertTrue(source.contains("mutableStateOf(initialPage)"))
        assertTrue(source.contains("if (!readiness.ready)"))
        assertTrue(source.contains("AppSemanticIconKey.TEST_ALARM"))
        assertTrue(source.contains("AppSemanticIconKey.RESCHEDULE"))
        assertTrue(source.contains("AppSemanticIconKey.ALARM_SETTINGS"))
        assertTrue(source.contains("AppSemanticIconKey.SYSTEM_STATUS"))
        assertTrue(source.contains("Настройки будильников"))
        assertTrue(source.contains("Системный статус"))
        assertFalse(source.contains("alarms-section-general"))
        assertFalse(source.contains("alarms-section-system"))
    }

    @Test
    fun `alarm settings drilldown owns wear horizon ring and behavior controls`() {
        val source = alarms()
        val settings = source.substringAfter("internal fun AlarmSettingsScreen(", "")
            .substringBefore("internal fun AlarmSystemStatusScreen(")
        assertTrue(settings.isNotBlank())
        assertTrue(settings.contains("BackHandler(onBack = onBack)"))
        assertTrue(settings.contains("Дублировать на Wear OS"))
        assertTrue(settings.contains("SetWearMirrorEnabled"))
        assertTrue(settings.contains("SetWearSoundMode"))
        assertTrue(settings.contains("Горизонт планирования"))
        assertTrue(settings.contains("SetScheduleHorizonDaysText"))
        assertTrue(settings.contains("Экран звонка"))
        assertTrue(settings.contains("Поведение звонка"))
    }

    @Test
    fun `system status drilldown owns permission actions and reschedule feedback`() {
        val source = alarms()
        val system = source.substringAfter("internal fun AlarmSystemStatusScreen(", "")
            .substringBefore("private fun AlarmReadinessEvolutionCard")
        assertTrue(system.isNotBlank())
        assertTrue(system.contains("BackHandler(onBack = onBack)"))
        assertTrue(system.contains("Уведомления:"))
        assertTrue(system.contains("Точные будильники:"))
        assertTrue(system.contains("Полноэкранный режим:"))
        assertTrue(system.contains("onRequestNotificationPermission"))
        assertTrue(system.contains("onOpenExactAlarmSettings"))
        assertTrue(system.contains("onOpenFullScreenIntentSettings"))
        assertTrue(system.contains("lastRescheduleResult"))
        assertTrue(system.contains("AppFeedbackCard("))
    }
}
