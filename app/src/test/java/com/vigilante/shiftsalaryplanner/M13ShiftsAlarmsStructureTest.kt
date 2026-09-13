package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M13ShiftsAlarmsStructureTest {
    private fun root(): File = sequenceOf(File("."), File(".."))
        .first { File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists() }

    @Test
    fun `M13 preserves released Shifts Alarms relationship and alarm boundaries`() {
        val root = root()
        val main = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val editor = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/shifts/ShiftTemplateEditorScreen.kt").readText()
        val alarms = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt").readText()

        assertTrue(main.contains("BottomTab.SHIFTS"))
        assertTrue(main.contains("BottomTab.ALARMS"))
        assertTrue(editor.contains("currentAlarmTemplateConfig: ShiftTemplateAlarmConfig?"))
        assertTrue(editor.contains("onSave: (ShiftTemplateEntity, ShiftTemplateAlarmConfig, String) -> Unit"))
        assertFalse(alarms.contains("ShiftAlarmScheduler."))
        assertFalse(alarms.contains("ShiftAlarmStore("))
    }

    @Test
    fun `M13 Templates root uses Evolution tabs`() {
        val templates = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/templates/TemplatesScreen.kt").readText()
        assertTrue("Templates must use Evolution tabs", templates.contains("EvolutionTextTabs"))
        assertTrue("Templates must use Evolution surfaces", templates.contains("EvolutionSurface"))
    }


    @Test
    fun `M13 Shift editor exposes alarm context without changing save contract`() {
        val editor = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/shifts/ShiftTemplateEditorScreen.kt").readText()
        assertTrue(editor.contains("currentAlarmTemplateConfig: ShiftTemplateAlarmConfig?"))
        assertTrue(editor.contains("onSave: (ShiftTemplateEntity, ShiftTemplateAlarmConfig, String) -> Unit"))
        assertTrue("Shift editor must expose the existing alarm config as UI context", editor.contains("ShiftTemplateAlarmContext"))
    }
}
