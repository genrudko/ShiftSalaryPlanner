package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.app.ports.FinanceDataPort
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryItem
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import com.vigilante.shiftsalaryplanner.settings.WorkplacePayrollSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceDataPortTest {
    @Test
    fun featureCanUseFakeFinancePortWithoutConcreteStores() {
        val port = FakeFinanceDataPort()
        val payroll = PayrollSettings(baseSalary = 123.0)
        val workplace = WorkplacePayrollSettingsState(emptyMap())
        val payment = AdditionalPayment(id = "p")
        val deduction = PayrollDeduction(id = "d")
        val visibility = ReportVisibilitySettings(showPayrollSummaryCard = false)
        val history = ReportHistoryItem(
            id = "h",
            timestampMillis = 1L,
            title = "Report",
            periodLabel = "2026-09",
            workplaceLabel = "Main",
            gross = 1.0,
            ndfl = 0.0,
            net = 1.0,
            fileName = "r.csv",
            format = "CSV"
        )

        port.savePayrollSettings(payroll)
        port.saveWorkplacePayrollSettings(workplace)
        port.upsertAdditionalPayment(payment)
        port.deleteAdditionalPayment("p")
        port.upsertDeduction(deduction)
        port.deleteDeduction("d")
        port.setDeductionActive("d", false)
        port.saveReportVisibility(visibility)
        port.addReportHistory(history)
        port.clearReportHistory()

        assertEquals(
            listOf(
                "savePayroll:123.0",
                "saveWorkplace:0",
                "upsertPayment:p",
                "deletePayment:p",
                "upsertDeduction:d",
                "deleteDeduction:d",
                "activeDeduction:d:false",
                "saveVisibility:false",
                "addHistory:h",
                "clearHistory"
            ),
            port.calls
        )
    }

    private class FakeFinanceDataPort : FinanceDataPort {
        override val payrollSettings: Flow<PayrollSettings> = MutableStateFlow(PayrollSettings())
        override val workplacePayrollSettings: Flow<WorkplacePayrollSettingsState> = MutableStateFlow(WorkplacePayrollSettingsState(emptyMap()))
        override val additionalPayments: Flow<List<AdditionalPayment>> = MutableStateFlow(emptyList())
        override val deductions: Flow<List<PayrollDeduction>> = MutableStateFlow(emptyList())
        override val reportVisibility: Flow<ReportVisibilitySettings> = MutableStateFlow(ReportVisibilitySettings())
        override val reportHistory: Flow<List<ReportHistoryItem>> = MutableStateFlow(emptyList())
        val calls = mutableListOf<String>()

        override fun savePayrollSettings(settings: PayrollSettings) { calls += "savePayroll:${settings.baseSalary}" }
        override fun saveWorkplacePayrollSettings(state: WorkplacePayrollSettingsState) { calls += "saveWorkplace:${state.settingsByWorkplaceId.size}" }
        override fun upsertAdditionalPayment(item: AdditionalPayment) { calls += "upsertPayment:${item.id}" }
        override fun deleteAdditionalPayment(id: String) { calls += "deletePayment:$id" }
        override fun upsertDeduction(item: PayrollDeduction) { calls += "upsertDeduction:${item.id}" }
        override fun deleteDeduction(id: String) { calls += "deleteDeduction:$id" }
        override fun setDeductionActive(id: String, active: Boolean) { calls += "activeDeduction:$id:$active" }
        override fun saveReportVisibility(settings: ReportVisibilitySettings) { calls += "saveVisibility:${settings.showPayrollSummaryCard}" }
        override fun addReportHistory(item: ReportHistoryItem) { calls += "addHistory:${item.id}" }
        override fun clearReportHistory() { calls += "clearHistory" }
    }
}
