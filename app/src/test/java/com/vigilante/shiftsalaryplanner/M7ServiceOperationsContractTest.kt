package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M7ServiceOperationsContractTest {
    @Test
    fun servicePortExposesNamedWorkflowOperationsInsteadOfGenericExecution() {
        val root = sequenceOf(File("."), File("..")).first {
            File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app").exists()
        }
        val file = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/ServiceOperationsPort.kt")
        assertTrue("ServiceOperationsPort.kt must exist", file.exists())
        val source = file.readText()
        listOf(
            "syncFederalCalendar",
            "parseExcelSchedule",
            "importExcelSchedule",
            "buildBackupJson",
            "restoreBackupFromUri",
            "restoreBackupFromRawJson",
            "resolveGoogleAccount",
            "googleSignInIntent",
            "parseGoogleSignInResult",
            "signOutGoogleAccount",
            "uploadBackupToCloud",
            "downloadBackupFromCloud",
            "setGoogleAccountEmail",
            "markGoogleUpload",
            "markGoogleRestore",
            "setAutoUploadEnabled",
            "setAutoUploadIntervalHours"
        ).forEach { operation ->
            assertTrue("missing named operation $operation", source.contains(operation))
        }
        assertFalse(source.contains("fun execute("))
    }
}
