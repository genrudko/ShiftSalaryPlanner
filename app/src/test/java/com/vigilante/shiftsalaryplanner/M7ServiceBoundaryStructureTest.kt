package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M7ServiceBoundaryStructureTest {
    @Test
    fun presentationUsesServiceOperationsInsteadOfConcreteServiceBackends() {
        val root = sequenceOf(File("."), File("..")).first {
            File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
        }
        val main = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val dependencies = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt").readText()

        assertTrue(dependencies.contains("val serviceOperations: ServiceOperationsPort"))
        listOf(
            "holidaySyncRepository",
            "excelScheduleParser",
            "excelScheduleImporter",
            "googleDriveSyncStore",
            "googleDriveScope",
            "googleSignInClient"
        ).forEach { leak ->
            assertFalse("MainActivity still references $leak", Regex("\\b$leak\\b").containsMatchIn(main))
        }
        assertFalse(main.contains("GoogleSignIn."))
        assertFalse(main.contains("buildBackupJsonForExport("))
        assertFalse(main.contains("uploadBackupToGoogleDriveAppData("))
        assertFalse(main.contains("downloadBackupFromGoogleDriveAppData("))

        val rawRestoreCallbacks = Regex(
            "serviceOperations\\.restoreBackupFrom(?:Uri|RawJson)\\([\\s\\S]{0,1200}(?:upsertShiftTemplate|deleteShiftTemplate|upsertShiftDay|deleteShiftDayByDate)\\s*="
        )
        assertFalse("backup restore still exposes raw persistence callbacks", rawRestoreCallbacks.containsMatchIn(main))
    }
}
