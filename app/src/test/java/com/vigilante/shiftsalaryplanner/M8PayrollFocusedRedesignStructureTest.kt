package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M8PayrollFocusedRedesignStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun source(path: String) = File(root(), path).readText()

    @Test fun calculationUsesOneOverviewInsteadOfLegacyTileGrid() {
        val payroll = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt")
        assertTrue(payroll.contains("PayrollCalculationOverviewCard("))
        assertFalse(payroll.contains("PayrollStatTile("))
        assertTrue(payroll.contains("PayrollDisplayOptionsBar("))
    }

    @Test fun payableLabelsUseAfterDeductionAmount() {
        val payroll = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt")
        val summary = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt")
        assertTrue(payroll.contains("payrollPayable = state.payroll.netAfterDeductions"))
        assertTrue(summary.contains("formatFinanceMoney(payroll.netAfterDeductions)"))
    }

    @Test fun payrollSheetActionsFitWithoutHorizontalScrolling() {
        val sheet = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt")
        assertTrue(sheet.contains("PayrollSheetActionBar("))
        assertTrue(sheet.contains("DropdownMenu("))
        assertTrue(sheet.contains("Text(\"Ещё\")"))
        assertFalse(sheet.contains(".horizontalScroll(rememberScrollState())"))
    }

    @Test fun payrollPresentationUsesReadableFinanceMoneyFormatter() {
        val files = listOf(
            "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt",
            "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt",
            "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt"
        )
        files.forEach { path ->
            val text = source(path)
            assertFalse("legacy money formatter remains in $path", text.contains("formatMoney("))
        }
    }

    @Test fun collapsedSummaryIsDisclosureNotPillStack() {
        val summary = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt")
        assertTrue(summary.contains("PayrollSummaryCollapsedOverview("))
        assertFalse(summary.contains("SummaryCollapsedPill("))
    }
    @Test fun displayOptionsExplainTheirIndependentDimensions() {
        val payroll = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt")
        assertTrue(payroll.contains("title = \"Вид\""))
        assertTrue(payroll.contains("title = \"Суммы\""))
        assertTrue(payroll.contains("value = if (viewMode == PayrollViewMode.DETAILED)"))
        assertTrue(payroll.contains("value = if (amountViewMode == PayrollAmountViewMode.NET)"))
    }

}
