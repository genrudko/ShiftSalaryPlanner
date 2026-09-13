package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M11FinanceVerticalSliceStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun paymentsUsesEvolutionSurfaces() {
        val source = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/PaymentsTab.kt")
        assertTrue(source.contains("EvolutionSurface("))
        assertTrue(source.contains("useEvolution = true"))
        assertFalse(source.contains("appPanelBorderColor()"))
    }

    @Test
    fun paymentsOwnsFactVsPlanPresentation() {
        val source = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/PaymentsTab.kt")
        assertTrue(source.contains("actualAdvanceNet: Double"))
        assertTrue(source.contains("actualSalaryNet: Double"))
        assertTrue(source.contains("paymentDifferenceToleranceRub: Double"))
        assertTrue(source.contains("onSaveActualPayments: (Double, Double) -> Unit"))
        assertTrue(source.contains("PaymentsFactVsPlanCard("))
        assertTrue(source.contains("Ожидалось / пришло"))
    }

    @Test
    fun calculationUsesEvolutionGrammar() {
        val screen = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt")
        val summary = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt")
        assertTrue(screen.contains("EvolutionHeroCard("))
        assertTrue(screen.contains("EvolutionSurface("))
        assertFalse(screen.contains("AppExpressiveSurface("))
        assertFalse(screen.contains("appPanelColor()"))
        assertFalse(screen.contains("appPanelBorderColor()"))
        assertTrue(summary.contains("EvolutionSurface("))
        assertFalse(summary.contains("appPanelBorderColor()"))
    }

    @Test
    fun payslipAndPayrollSettingsUseEvolutionSurfaces() {
        val sheet = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt")
        val settings = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsComponents.kt")
        assertTrue(sheet.contains("EvolutionSurface("))
        assertFalse(sheet.contains("appPanelBorderColor()"))
        assertTrue(settings.contains("EvolutionSurface("))
        assertFalse(settings.contains("appPanelColor()"))
        assertFalse(settings.contains("appPanelBorderColor()"))
    }
}
