package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M7ScheduleBoundaryStructureTest {
    @Test
    fun presentationUsesSchedulePortInsteadOfConcreteScheduleDependencies() {
        val root = generateSequence(File(".").canonicalFile) { it.parentFile }
            .first { File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists() }
        val main = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val dependencies = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt").readText()

        assertTrue(
            "ProfileDependencies must expose ScheduleDataPort",
            dependencies.contains("val scheduleData: ScheduleDataPort")
        )
        listOf("shiftDayDao", "shiftTemplateDao", "holidayDao", "workAssignmentsStore").forEach { legacy ->
            assertFalse(
                "MainActivity still references concrete schedule dependency $legacy",
                Regex("\\b${legacy}\\b").containsMatchIn(main)
            )
        }
    }
}
