package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M9FinanceVariantAStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }
    private fun finance() = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()

    @Test fun financeSummaryUsesVariantAHeroRowsAndLightweightTabs() {
        val source = finance()
        assertTrue(source.contains("EvolutionTextTabs("))
        assertTrue(source.contains("EvolutionHeroCard("))
        assertTrue(source.contains("EvolutionActionRow("))
        assertTrue(source.contains("EvolutionIconTone.FINANCE"))
        assertFalse(source.contains("FinanceKeyMetrics("))
    }

    @Test fun financeHeroKeepsCorrectPayableAmount() {
        val source = finance()
        assertTrue(source.contains("state.payroll.netAfterDeductions"))
        assertTrue(source.contains("formatFinanceMoney(state.payroll.netAfterDeductions)"))
    }

    @Test fun financeSummaryKeepsPerShiftSemanticsExplicit() {
        val source = finance()
        assertTrue(source.contains("if (isPerShiftPayment)"))
        assertTrue(source.contains("title = \"За смены\""))
        assertTrue(source.contains("title = \"Аванс\""))
        assertTrue(source.contains("title = \"Остаток\""))
    }

    @Test fun financeSummaryHasExplicitPayrollActionRow() {
        val source = finance()
        assertTrue(source.contains("title = \"Расчётный лист\""))
        assertTrue(source.contains("onClick = onOpenPayroll"))
    }
}
