package com.vigilante.shiftsalaryplanner

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceFeatureStateTest {
    @Test
    fun `initial year and range derive from initial month`() {
        val state = FinanceFeatureState(YearMonth.of(2026, 9))
        assertEquals(2026, state.payrollSelectedYear)
        assertEquals("2026-09-01", state.payrollRangeStartIso)
        assertEquals("2026-09-30", state.payrollRangeEndIso)
    }

    @Test
    fun `changing period mode preserves legacy year and range initialization`() {
        val state = FinanceFeatureState(YearMonth.of(2026, 9))
        val currentStart = LocalDate.parse("2025-12-15")
        val currentEnd = LocalDate.parse("2026-01-14")
        state.changePeriodMode(PayrollPeriodMode.YEAR, currentStart, currentEnd)
        assertEquals(PayrollPeriodMode.YEAR.name, state.payrollPeriodModeName)
        assertEquals(2026, state.payrollSelectedYear)
        state.changePeriodMode(PayrollPeriodMode.RANGE, currentStart, currentEnd)
        assertEquals(PayrollPeriodMode.RANGE.name, state.payrollPeriodModeName)
        assertEquals("2025-12-15", state.payrollRangeStartIso)
        assertEquals("2026-01-14", state.payrollRangeEndIso)
    }

    @Test
    fun `range start after end swaps boundaries exactly like legacy root`() {
        val state = FinanceFeatureState(YearMonth.of(2026, 9))
        state.pickRangeStart(LocalDate.parse("2026-09-20"), LocalDate.parse("2026-09-10"))
        assertEquals("2026-09-10", state.payrollRangeStartIso)
        assertEquals("2026-09-20", state.payrollRangeEndIso)
    }

    @Test
    fun `range end before start swaps boundaries exactly like legacy root`() {
        val state = FinanceFeatureState(YearMonth.of(2026, 9))
        state.pickRangeEnd(LocalDate.parse("2026-09-05"), LocalDate.parse("2026-09-10"))
        assertEquals("2026-09-05", state.payrollRangeStartIso)
        assertEquals("2026-09-10", state.payrollRangeEndIso)
    }

    @Test
    fun `payment editor uses normalized workplace input from caller`() {
        val state = FinanceFeatureState(YearMonth.of(2026, 9))
        state.openNewPayment("main")
        assertEquals("main", state.settingsWorkplaceId)
        assertNull(state.editingAdditionalPaymentId)
        assertTrue(state.showAdditionalPaymentDialog)
        state.closePaymentDialog()
        assertFalse(state.showAdditionalPaymentDialog)
        assertNull(state.editingAdditionalPaymentId)
        state.openPayment("payment-7", "work-2")
        assertEquals("work-2", state.settingsWorkplaceId)
        assertEquals("payment-7", state.editingAdditionalPaymentId)
        assertTrue(state.showAdditionalPaymentDialog)
    }

    @Test
    fun `summary toggle and year controls retain legacy mutations`() {
        val state = FinanceFeatureState(YearMonth.of(2026, 9))
        state.toggleSummary()
        assertTrue(state.isSummaryExpanded)
        state.previousYear()
        assertEquals(2025, state.payrollSelectedYear)
        state.nextYear()
        assertEquals(2026, state.payrollSelectedYear)
        state.selectYear(2030)
        assertEquals(2030, state.payrollSelectedYear)
    }

    @Test
    fun `restore keeps saveable fields but drops transient report payloads`() {
        val original = FinanceFeatureState(YearMonth.of(2026, 9))
        original.changePeriodMode(PayrollPeriodMode.RANGE, LocalDate.parse("2026-08-15"), LocalDate.parse("2026-09-14"))
        original.selectWorkplace("work-2")
        original.openSettingsFor("work-3")
        original.openPayment("payment-9", "work-3")
        original.startDeductionEdit("deduction-4")
        original.toggleSummary()
        original.selectYear(2031)
        original.stageCsv("csv-content", "custom.csv")
        original.stagePdf(byteArrayOf(1, 2, 3), "custom.pdf")
        val restored = restoreFinanceFeatureState(original.toSaveableStrings(), YearMonth.of(2024, 1))
        assertEquals(original.toSaveableStrings(), restored.toSaveableStrings())
        assertNull(restored.pendingReportCsvContent)
        assertEquals("report.csv", restored.pendingReportCsvFileName)
        assertNull(restored.pendingReportPdfBytes)
        assertEquals("report.pdf", restored.pendingReportPdfFileName)
    }
}
