package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M12MoreWorkplacesStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun shellUsesThreePrimaryDestinationsWithoutChangingCompatibilityEnum() {
        val shell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppShellComponents.kt")
        val models = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationModels.kt")

        assertTrue(shell.contains("primaryBottomTabs.forEach"))
        assertFalse(shell.contains("BottomTab.entries.forEach"))
        assertTrue(models.contains("SETTINGS(\"Ещё\""))
        assertTrue(models.contains("CALENDAR("))
        assertTrue(models.contains("TODAY("))
        assertTrue(models.contains("ASSISTANT("))
        assertTrue(models.contains("NOTES("))
        assertTrue(models.contains("FINANCE("))
        assertTrue(models.contains("ALARMS("))
        assertTrue(models.contains("SHIFTS("))
        assertTrue(models.contains("SETTINGS("))
    }

    @Test
    fun moreRootUsesAcceptedTaxonomyAndEvolutionSurfaces() {
        val more = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/MoreTab.kt")

        assertTrue(more.contains("Work"))
        assertTrue(more.contains("Tools"))
        assertTrue(more.contains("App"))
        assertTrue(more.contains("Data"))
        assertTrue(more.contains("Advanced"))
        assertTrue(more.contains("EvolutionSurface("))
        assertFalse(more.contains("Store("))
        assertFalse(more.contains("Repository("))
    }

    @Test
    fun workplacesAreFirstClassContextWithPayrollAccess() {
        val workplaces = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkplacesScreen.kt")
        val navigation = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationState.kt")

        assertTrue(workplaces.contains("fun WorkplacesScreen("))
        assertTrue(workplaces.contains("onOpenPayrollSettings: (String) -> Unit"))
        assertTrue(workplaces.contains("EvolutionSurface("))
        assertFalse(workplaces.contains("WorkAssignmentsStore"))
        assertFalse(workplaces.contains("WorkplacePayrollSettingsStore"))
        assertTrue(navigation.contains("WORKPLACES"))
    }
    @Test
    fun moreMigrationKeepsLegacyCapabilitiesReachableAndFinanceOwnsDeductions() {
        val more = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/MoreTab.kt")
        val main = source("app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt")
        val payrollContract = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabContract.kt")
        val payrollSheet = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt")

        assertTrue(more.contains("onOpenManualHolidays: () -> Unit"))
        assertTrue(more.contains("onOpenQuickActions: () -> Unit"))
        assertTrue(more.contains("onSyncProductionCalendar: () -> Unit"))
        assertTrue(more.contains("isHolidaySyncing: Boolean"))
        assertTrue(more.contains("holidaySyncMessage: String?"))
        assertTrue(main.contains("onOpenManualHolidays = { navigationState = navigationState.openScreen(AppScreen.MANUAL_HOLIDAYS) }"))
        assertTrue(main.contains("onOpenQuickActions = { navigationState = navigationState.openScreen(AppScreen.QUICK_ACTIONS_SETTINGS) }"))

        assertTrue(payrollContract.contains("val onOpenDeductions: () -> Unit"))
        assertTrue(payrollSheet.contains("onOpenDeductions: () -> Unit"))
        assertTrue(payrollSheet.contains("Text(\"Удержания\")"))
        assertTrue(main.contains("onOpenDeductions = {"))
        assertTrue(main.contains("navigationState = navigationState.openScreen(AppScreen.DEDUCTIONS)"))
    }

}
