package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSemanticIconsStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `application menus use semantic icon catalog instead of interface glyphs`() {
        val catalogFile = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/AppSemanticIcons.kt")
        assertTrue("semantic icon catalog must exist", catalogFile.exists())
        val catalog = if (catalogFile.exists()) catalogFile.readText() else ""
        val more = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/MoreTab.kt")
        val navigation = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationModels.kt")

        assertTrue(catalog.contains("enum class AppSemanticIconKey"))
        listOf(
            "CALENDAR", "TODAY", "FINANCE", "MORE", "WORKPLACES", "SHIFT_TEMPLATES",
            "PRODUCTION_CALENDAR", "MANUAL_HOLIDAYS", "ALARMS", "NOTES",
            "APPEARANCE", "WIDGETS", "QUICK_ACTIONS", "WEAR", "PROFILES", "QUICK_START",
            "BACKUP", "CLOUD_BACKUP", "IMPORT_EXPORT", "REPORTS", "HEALTH_CHECK",
            "EVENT_LOG", "CURRENT_PARAMETERS", "ALARM_SETTINGS", "SYSTEM_STATUS"
        ).forEach { key ->
            assertTrue("catalog missing $key", catalog.contains(key))
        }

        assertTrue(more.contains("val iconKey: AppSemanticIconKey"))
        assertFalse(more.contains("val glyph: String"))
        assertTrue(more.contains("icon = appSemanticIcon(destination.iconKey)"))
        listOf(
            "\"Р\", onOpenWorkplaces",
            "\"С\", onOpenShiftTemplates",
            "\"Б\", onOpenAlarms",
            "\"Aa\", onOpenAppearance",
            "\"G\", onOpenGoogleDrive",
            "\"X\", onOpenImport"
        ).forEach { legacy -> assertFalse("legacy menu glyph remains: $legacy", more.contains(legacy)) }

        assertTrue(navigation.contains("appSemanticIcon(AppSemanticIconKey.CALENDAR)"))
        assertTrue(navigation.contains("appSemanticIcon(AppSemanticIconKey.FINANCE)"))
        assertTrue(navigation.contains("appSemanticIcon(AppSemanticIconKey.MORE)"))
    }
}
