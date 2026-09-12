package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M7ProfileBoundaryStructureTest {
    @Test
    fun appShellUsesProfilePortInsteadOfConcreteProfileStore() {
        val root = sequenceOf(File("."), File("..")).first {
            File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
        }
        val main = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val appRoot = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/ShiftSalaryPlannerRoot.kt").readText()
        val dependencies = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt").readText()
        assertTrue(dependencies.contains("val profileData: ProfileDataPort"))
        assertFalse(main.contains("AppProfileStore"))
        assertFalse(main.contains("profileStore"))
        assertFalse(appRoot.contains("AppProfileStore"))
        assertFalse(appRoot.contains("profileStore"))
    }
}
