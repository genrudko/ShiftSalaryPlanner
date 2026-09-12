package com.vigilante.shiftsalaryplanner.app.ports

import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.settings.AdditionalPaymentsStore
import com.vigilante.shiftsalaryplanner.settings.DeductionsStore
import com.vigilante.shiftsalaryplanner.settings.PayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryItem
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryStore
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettingsStore
import com.vigilante.shiftsalaryplanner.settings.WorkplacePayrollSettingsState
import com.vigilante.shiftsalaryplanner.settings.WorkplacePayrollSettingsStore
import kotlinx.coroutines.flow.Flow

interface FinanceDataPort {
    val payrollSettings: Flow<PayrollSettings>
    val workplacePayrollSettings: Flow<WorkplacePayrollSettingsState>
    val additionalPayments: Flow<List<AdditionalPayment>>
    val deductions: Flow<List<PayrollDeduction>>
    val reportVisibility: Flow<ReportVisibilitySettings>
    val reportHistory: Flow<List<ReportHistoryItem>>

    fun savePayrollSettings(settings: PayrollSettings)
    fun saveWorkplacePayrollSettings(state: WorkplacePayrollSettingsState)
    fun upsertAdditionalPayment(item: AdditionalPayment)
    fun deleteAdditionalPayment(id: String)
    fun upsertDeduction(item: PayrollDeduction)
    fun deleteDeduction(id: String)
    fun setDeductionActive(id: String, active: Boolean)
    fun saveReportVisibility(settings: ReportVisibilitySettings)
    fun addReportHistory(item: ReportHistoryItem)
    fun clearReportHistory()
}

class DefaultFinanceDataPort(
    private val payrollSettingsStore: PayrollSettingsStore,
    private val workplacePayrollSettingsStore: WorkplacePayrollSettingsStore,
    private val additionalPaymentsStore: AdditionalPaymentsStore,
    private val deductionsStore: DeductionsStore,
    private val reportVisibilitySettingsStore: ReportVisibilitySettingsStore,
    private val reportHistoryStore: ReportHistoryStore
) : FinanceDataPort {
    override val payrollSettings: Flow<PayrollSettings> = payrollSettingsStore.settingsFlow
    override val workplacePayrollSettings: Flow<WorkplacePayrollSettingsState> = workplacePayrollSettingsStore.stateFlow
    override val additionalPayments: Flow<List<AdditionalPayment>> = additionalPaymentsStore.paymentsFlow
    override val deductions: Flow<List<PayrollDeduction>> = deductionsStore.deductionsFlow
    override val reportVisibility: Flow<ReportVisibilitySettings> = reportVisibilitySettingsStore.settingsFlow
    override val reportHistory: Flow<List<ReportHistoryItem>> = reportHistoryStore.itemsFlow

    override fun savePayrollSettings(settings: PayrollSettings) = payrollSettingsStore.save(settings)
    override fun saveWorkplacePayrollSettings(state: WorkplacePayrollSettingsState) = workplacePayrollSettingsStore.save(state)
    override fun upsertAdditionalPayment(item: AdditionalPayment) = additionalPaymentsStore.addOrUpdate(item)
    override fun deleteAdditionalPayment(id: String) = additionalPaymentsStore.deleteById(id)
    override fun upsertDeduction(item: PayrollDeduction) = deductionsStore.addOrUpdate(item)
    override fun deleteDeduction(id: String) = deductionsStore.deleteById(id)
    override fun setDeductionActive(id: String, active: Boolean) = deductionsStore.setActive(id, active)
    override fun saveReportVisibility(settings: ReportVisibilitySettings) = reportVisibilitySettingsStore.save(settings)
    override fun addReportHistory(item: ReportHistoryItem) = reportHistoryStore.add(item)
    override fun clearReportHistory() = reportHistoryStore.clear()
}
