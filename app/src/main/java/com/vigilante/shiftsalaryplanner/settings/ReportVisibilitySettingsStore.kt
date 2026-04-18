package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReportVisibilitySettings(
    val showPayrollWorkedStatsRow: Boolean = true,
    val showPayrollPaymentsStatsRow: Boolean = true,
    val showPayrollSummaryCard: Boolean = true,
    val showPayrollStickyTotalsBar: Boolean = true,
    val showPayrollHeaderSection: Boolean = true,
    val showPayrollAccrualSection: Boolean = true,
    val showPayrollDeductionSection: Boolean = true,
    val showPayrollPriorPaymentSection: Boolean = true,
    val showPayrollPayoutSection: Boolean = true,
    val showPayrollReferenceSection: Boolean = true,
    val showPaymentsActionTiles: Boolean = true,
    val showPaymentsMainSummary: Boolean = true,
    val showPaymentsMainSummaryTopRow: Boolean = true,
    val showPaymentsMainSummaryBottomRow: Boolean = true,
    val showPaymentsPayoutAndTotals: Boolean = true,
    val showPaymentsPayoutCard: Boolean = true,
    val showPaymentsTotalsCard: Boolean = true,
    val showPaymentsShiftCosts: Boolean = true,
    val showPaymentsShiftStatsCard: Boolean = true,
    val showPaymentsShiftCostCard: Boolean = true,
    val showPaymentsAdditionalPayments: Boolean = true,
    val showPaymentsBaseAllowanceCard: Boolean = true,
    val showPaymentsMonthAdditionalCard: Boolean = true,
    val showPaymentsConfiguredAdditionalCard: Boolean = true,
    val showPaymentsAbsenceAndOvertime: Boolean = true,
    val showPaymentsAbsenceCard: Boolean = true,
    val showPaymentsOvertimeCard: Boolean = true
) {
    fun isPayrollSectionVisible(section: PayrollSheetSection): Boolean {
        return when (section) {
            PayrollSheetSection.HEADER -> showPayrollHeaderSection
            PayrollSheetSection.ACCRUAL -> showPayrollAccrualSection
            PayrollSheetSection.DEDUCTION -> showPayrollDeductionSection
            PayrollSheetSection.PRIOR_PAYMENT -> showPayrollPriorPaymentSection
            PayrollSheetSection.PAYOUT -> showPayrollPayoutSection
            PayrollSheetSection.REFERENCE -> showPayrollReferenceSection
        }
    }

    fun hasVisiblePaymentsBlocks(): Boolean {
        return (showPaymentsMainSummary && (showPaymentsMainSummaryTopRow || showPaymentsMainSummaryBottomRow)) ||
            (showPaymentsPayoutAndTotals && (showPaymentsPayoutCard || showPaymentsTotalsCard)) ||
            (showPaymentsShiftCosts && (showPaymentsShiftStatsCard || showPaymentsShiftCostCard)) ||
            (showPaymentsAdditionalPayments && (showPaymentsBaseAllowanceCard || showPaymentsMonthAdditionalCard || showPaymentsConfiguredAdditionalCard)) ||
            (showPaymentsAbsenceAndOvertime && (showPaymentsAbsenceCard || showPaymentsOvertimeCard))
    }
}

class ReportVisibilitySettingsStore(context: Context) {

    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _settingsFlow = MutableStateFlow(loadFromPrefs())
    val settingsFlow: Flow<ReportVisibilitySettings> = _settingsFlow.asStateFlow()

    fun save(settings: ReportVisibilitySettings) {
        prefs.edit {
            putBoolean(KEY_PAYROLL_WORKED_STATS_ROW, settings.showPayrollWorkedStatsRow)
            putBoolean(KEY_PAYROLL_PAYMENTS_STATS_ROW, settings.showPayrollPaymentsStatsRow)
            putBoolean(KEY_PAYROLL_SUMMARY_CARD, settings.showPayrollSummaryCard)
            putBoolean(KEY_PAYROLL_STICKY_TOTALS_BAR, settings.showPayrollStickyTotalsBar)
            putBoolean(KEY_PAYROLL_HEADER, settings.showPayrollHeaderSection)
            putBoolean(KEY_PAYROLL_ACCRUAL, settings.showPayrollAccrualSection)
            putBoolean(KEY_PAYROLL_DEDUCTION, settings.showPayrollDeductionSection)
            putBoolean(KEY_PAYROLL_PRIOR_PAYMENT, settings.showPayrollPriorPaymentSection)
            putBoolean(KEY_PAYROLL_PAYOUT, settings.showPayrollPayoutSection)
            putBoolean(KEY_PAYROLL_REFERENCE, settings.showPayrollReferenceSection)
            putBoolean(KEY_PAYMENTS_ACTION_TILES, settings.showPaymentsActionTiles)
            putBoolean(KEY_PAYMENTS_MAIN_SUMMARY, settings.showPaymentsMainSummary)
            putBoolean(KEY_PAYMENTS_MAIN_SUMMARY_TOP_ROW, settings.showPaymentsMainSummaryTopRow)
            putBoolean(KEY_PAYMENTS_MAIN_SUMMARY_BOTTOM_ROW, settings.showPaymentsMainSummaryBottomRow)
            putBoolean(KEY_PAYMENTS_PAYOUT_TOTALS, settings.showPaymentsPayoutAndTotals)
            putBoolean(KEY_PAYMENTS_PAYOUT_CARD, settings.showPaymentsPayoutCard)
            putBoolean(KEY_PAYMENTS_TOTALS_CARD, settings.showPaymentsTotalsCard)
            putBoolean(KEY_PAYMENTS_SHIFT_COSTS, settings.showPaymentsShiftCosts)
            putBoolean(KEY_PAYMENTS_SHIFT_STATS_CARD, settings.showPaymentsShiftStatsCard)
            putBoolean(KEY_PAYMENTS_SHIFT_COST_CARD, settings.showPaymentsShiftCostCard)
            putBoolean(KEY_PAYMENTS_ADDITIONAL, settings.showPaymentsAdditionalPayments)
            putBoolean(KEY_PAYMENTS_BASE_ALLOWANCE_CARD, settings.showPaymentsBaseAllowanceCard)
            putBoolean(KEY_PAYMENTS_MONTH_ADDITIONAL_CARD, settings.showPaymentsMonthAdditionalCard)
            putBoolean(KEY_PAYMENTS_CONFIGURED_ADDITIONAL_CARD, settings.showPaymentsConfiguredAdditionalCard)
            putBoolean(KEY_PAYMENTS_ABSENCE_OVERTIME, settings.showPaymentsAbsenceAndOvertime)
            putBoolean(KEY_PAYMENTS_ABSENCE_CARD, settings.showPaymentsAbsenceCard)
            putBoolean(KEY_PAYMENTS_OVERTIME_CARD, settings.showPaymentsOvertimeCard)
        }
        _settingsFlow.value = loadFromPrefs()
    }

    private fun loadFromPrefs(): ReportVisibilitySettings {
        return ReportVisibilitySettings(
            showPayrollWorkedStatsRow = prefs.getBoolean(KEY_PAYROLL_WORKED_STATS_ROW, true),
            showPayrollPaymentsStatsRow = prefs.getBoolean(KEY_PAYROLL_PAYMENTS_STATS_ROW, true),
            showPayrollSummaryCard = prefs.getBoolean(KEY_PAYROLL_SUMMARY_CARD, true),
            showPayrollStickyTotalsBar = prefs.getBoolean(KEY_PAYROLL_STICKY_TOTALS_BAR, true),
            showPayrollHeaderSection = prefs.getBoolean(KEY_PAYROLL_HEADER, true),
            showPayrollAccrualSection = prefs.getBoolean(KEY_PAYROLL_ACCRUAL, true),
            showPayrollDeductionSection = prefs.getBoolean(KEY_PAYROLL_DEDUCTION, true),
            showPayrollPriorPaymentSection = prefs.getBoolean(KEY_PAYROLL_PRIOR_PAYMENT, true),
            showPayrollPayoutSection = prefs.getBoolean(KEY_PAYROLL_PAYOUT, true),
            showPayrollReferenceSection = prefs.getBoolean(KEY_PAYROLL_REFERENCE, true),
            showPaymentsActionTiles = prefs.getBoolean(KEY_PAYMENTS_ACTION_TILES, true),
            showPaymentsMainSummary = prefs.getBoolean(KEY_PAYMENTS_MAIN_SUMMARY, true),
            showPaymentsMainSummaryTopRow = prefs.getBoolean(KEY_PAYMENTS_MAIN_SUMMARY_TOP_ROW, true),
            showPaymentsMainSummaryBottomRow = prefs.getBoolean(KEY_PAYMENTS_MAIN_SUMMARY_BOTTOM_ROW, true),
            showPaymentsPayoutAndTotals = prefs.getBoolean(KEY_PAYMENTS_PAYOUT_TOTALS, true),
            showPaymentsPayoutCard = prefs.getBoolean(KEY_PAYMENTS_PAYOUT_CARD, true),
            showPaymentsTotalsCard = prefs.getBoolean(KEY_PAYMENTS_TOTALS_CARD, true),
            showPaymentsShiftCosts = prefs.getBoolean(KEY_PAYMENTS_SHIFT_COSTS, true),
            showPaymentsShiftStatsCard = prefs.getBoolean(KEY_PAYMENTS_SHIFT_STATS_CARD, true),
            showPaymentsShiftCostCard = prefs.getBoolean(KEY_PAYMENTS_SHIFT_COST_CARD, true),
            showPaymentsAdditionalPayments = prefs.getBoolean(KEY_PAYMENTS_ADDITIONAL, true),
            showPaymentsBaseAllowanceCard = prefs.getBoolean(KEY_PAYMENTS_BASE_ALLOWANCE_CARD, true),
            showPaymentsMonthAdditionalCard = prefs.getBoolean(KEY_PAYMENTS_MONTH_ADDITIONAL_CARD, true),
            showPaymentsConfiguredAdditionalCard = prefs.getBoolean(KEY_PAYMENTS_CONFIGURED_ADDITIONAL_CARD, true),
            showPaymentsAbsenceAndOvertime = prefs.getBoolean(KEY_PAYMENTS_ABSENCE_OVERTIME, true),
            showPaymentsAbsenceCard = prefs.getBoolean(KEY_PAYMENTS_ABSENCE_CARD, true),
            showPaymentsOvertimeCard = prefs.getBoolean(KEY_PAYMENTS_OVERTIME_CARD, true)
        )
    }

    companion object {
        private const val PREFS_NAME = "report_visibility_settings"
        private const val KEY_PAYROLL_WORKED_STATS_ROW = "payroll_worked_stats_row"
        private const val KEY_PAYROLL_PAYMENTS_STATS_ROW = "payroll_payments_stats_row"
        private const val KEY_PAYROLL_SUMMARY_CARD = "payroll_summary_card"
        private const val KEY_PAYROLL_STICKY_TOTALS_BAR = "payroll_sticky_totals_bar"
        private const val KEY_PAYROLL_HEADER = "payroll_header"
        private const val KEY_PAYROLL_ACCRUAL = "payroll_accrual"
        private const val KEY_PAYROLL_DEDUCTION = "payroll_deduction"
        private const val KEY_PAYROLL_PRIOR_PAYMENT = "payroll_prior_payment"
        private const val KEY_PAYROLL_PAYOUT = "payroll_payout"
        private const val KEY_PAYROLL_REFERENCE = "payroll_reference"
        private const val KEY_PAYMENTS_ACTION_TILES = "payments_action_tiles"
        private const val KEY_PAYMENTS_MAIN_SUMMARY = "payments_main_summary"
        private const val KEY_PAYMENTS_MAIN_SUMMARY_TOP_ROW = "payments_main_summary_top_row"
        private const val KEY_PAYMENTS_MAIN_SUMMARY_BOTTOM_ROW = "payments_main_summary_bottom_row"
        private const val KEY_PAYMENTS_PAYOUT_TOTALS = "payments_payout_totals"
        private const val KEY_PAYMENTS_PAYOUT_CARD = "payments_payout_card"
        private const val KEY_PAYMENTS_TOTALS_CARD = "payments_totals_card"
        private const val KEY_PAYMENTS_SHIFT_COSTS = "payments_shift_costs"
        private const val KEY_PAYMENTS_SHIFT_STATS_CARD = "payments_shift_stats_card"
        private const val KEY_PAYMENTS_SHIFT_COST_CARD = "payments_shift_cost_card"
        private const val KEY_PAYMENTS_ADDITIONAL = "payments_additional"
        private const val KEY_PAYMENTS_BASE_ALLOWANCE_CARD = "payments_base_allowance_card"
        private const val KEY_PAYMENTS_MONTH_ADDITIONAL_CARD = "payments_month_additional_card"
        private const val KEY_PAYMENTS_CONFIGURED_ADDITIONAL_CARD = "payments_configured_additional_card"
        private const val KEY_PAYMENTS_ABSENCE_OVERTIME = "payments_absence_overtime"
        private const val KEY_PAYMENTS_ABSENCE_CARD = "payments_absence_card"
        private const val KEY_PAYMENTS_OVERTIME_CARD = "payments_overtime_card"
    }
}
