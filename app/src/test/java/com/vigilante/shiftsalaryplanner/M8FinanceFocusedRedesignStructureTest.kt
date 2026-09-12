package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class M8FinanceFocusedRedesignStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    @Test fun summaryLeadsWithTruePayableAmountAfterDeductions() {
        val finance = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()
        assertTrue(finance.contains("FinancePayableHeroCard("))
        assertTrue(finance.contains("value = formatFinanceMoney(state.payroll.netAfterDeductions)"))
        assertTrue(finance.contains("Ожидается на руки"))
    }

    @Test fun summaryKeepsPayrollDepthOneActionAway() {
        val finance = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()
        assertTrue(finance.contains("onOpenPayroll: () -> Unit"))
        assertTrue(finance.contains("FinancePayoutPlanCard("))
        assertTrue(finance.contains("Расчётный лист"))
        assertTrue(finance.contains("ActualPaymentsComparisonCard("))
    }

    @Test fun summarySeparatesSupportingMetricsFromHero() {
        val finance = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()
        assertTrue(finance.contains("FinanceSummaryMetricRows("))
        assertTrue(finance.contains("EvolutionActionRow("))
        assertTrue(finance.contains("state.payroll.deductionsTotal"))
        assertTrue(finance.contains("state.payroll.workedHours"))
        assertFalse(finance.contains("title = \"На руки\",\n                value = formatMoney(state.payroll.netTotal)"))
    }
    @Test fun financeMoneyFormattingIsHumanReadable() {
        assertEquals("184\u00A0511 ₽", formatFinanceMoney(184511.0))
        assertEquals("1\u00A0234,50 ₽", formatFinanceMoney(1234.5))
        assertEquals("-611 ₽", formatFinanceMoney(-611.0))
    }

    @Test fun summaryDoesNotMixScheduleContextIntoMoneyFlow() {
        val finance = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()
        assertFalse(finance.contains("Контекст графика"))
        assertFalse(finance.contains("FinanceWorkContextCard("))
    }

}
