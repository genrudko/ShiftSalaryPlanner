package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M7NotesSettingsBoundaryStructureTest {
    @Test
    fun presentationUsesNotesSettingsAndActivityPortsInsteadOfConcreteStores() {
        val root = sequenceOf(File("."), File("..")).first {
            File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
        }
        val main = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val dependencies = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt").readText()
        assertTrue(dependencies.contains("val notesData: NotesDataPort"))
        assertTrue(dependencies.contains("val settingsData: SettingsDataPort"))
        assertTrue(dependencies.contains("val activityLog: ActivityLogPort"))
        listOf(
            "appNotesStore",
            "appWorkflowSettingsStore",
            "assistantAiSettingsStore",
            "todayLayoutSettingsStore",
            "patternTemplatesStore",
            "appEventLogStore"
        ).forEach { legacy ->
            assertFalse("MainActivity still references $legacy", Regex("\\b$legacy\\b").containsMatchIn(main))
        }
    }
}
