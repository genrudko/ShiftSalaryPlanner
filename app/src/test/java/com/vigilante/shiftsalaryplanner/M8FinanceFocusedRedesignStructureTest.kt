package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M8FinanceFocusedRedesignStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    @Test fun summaryLeadsWithTruePayableAmountAfterDeductions() {
        val finance = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()
        assertTrue(finance.contains("FinancePayableHeroCard("))
        assertTrue(finance.contains("value = formatMoney(state.payroll.netAfterDeductions)"))
        assertTrue(finance.contains("Ожидается к выплате"))
    }

    @Test fun summaryKeepsPayrollDepthOneActionAway() {
        val finance = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()
        assertTrue(finance.contains("onOpenPayroll: () -> Unit"))
        assertTrue(finance.contains("FinancePayoutPlanCard("))
        assertTrue(finance.contains("Открыть расчёт"))
        assertTrue(finance.contains("ActualPaymentsComparisonCard("))
    }

    @Test fun summarySeparatesSupportingMetricsFromHero() {
        val finance = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt").readText()
        assertTrue(finance.contains("FinanceKeyMetrics("))
        assertTrue(finance.contains("state.payroll.deductionsTotal"))
        assertTrue(finance.contains("state.payroll.workedHours"))
        assertFalse(finance.contains("title = \"На руки\",\n                value = formatMoney(state.payroll.netTotal)"))
    }
}
