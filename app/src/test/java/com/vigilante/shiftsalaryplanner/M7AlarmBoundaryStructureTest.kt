package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M7AlarmBoundaryStructureTest {
    @Test
    fun presentationUsesAlarmPortsInsteadOfConcreteAlarmStoreOrScheduler() {
        val root = sequenceOf(File("."), File("..")).first {
            File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
        }
        val main = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val dependencies = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt").readText()
        val effects = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsEffects.kt").readText()

        assertTrue(dependencies.contains("val alarmPlatform: AlarmPlatformPort"))
        assertTrue(dependencies.contains("val alarmData: AlarmDataPort"))
        assertFalse("MainActivity still references shiftAlarmStore", Regex("\\bshiftAlarmStore\\b").containsMatchIn(main))
        assertFalse("MainActivity still references ShiftAlarmScheduler", Regex("\\bShiftAlarmScheduler\\b").containsMatchIn(main))
        assertFalse("ShiftAlarmsEffects still references ShiftAlarmStore", Regex("\\bShiftAlarmStore\\b").containsMatchIn(effects))
        assertFalse("ShiftAlarmsEffects still references ShiftAlarmScheduler", Regex("\\bShiftAlarmScheduler\\b").containsMatchIn(effects))
    }
}
