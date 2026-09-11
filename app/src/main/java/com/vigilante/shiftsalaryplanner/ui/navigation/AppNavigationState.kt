package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.saveable.Saver

enum class AppScreen {
    MONTHLY_REPORT,
    APP_HEALTH_CHECK,
    APP_EVENT_LOG,
    REPORT_HISTORY,
    QUICK_ACTIONS_SETTINGS,
    QUICK_START_GUIDE,
    REPORT_CENTER,
    PAYROLL_DIAGNOSTICS,
    REPORT_VISIBILITY_SETTINGS,
    PAYROLL_SETTINGS,
    APPEARANCE_SETTINGS,
    CURRENT_PARAMETERS,
    PROFILES,
    MANUAL_HOLIDAYS,
    BACKUP_RESTORE,
    EXCEL_IMPORT,
    WIDGET_SETTINGS,
    ADDITIONAL_PAYMENTS,
    DEDUCTIONS,
    DEDUCTION_EDITOR,
    SHIFT_TEMPLATE_EDITOR,
    NOTE_EDITOR
}

data class AppNavigationState(
    val selectedTab: BottomTab = BottomTab.CALENDAR,
    val financeSubTab: FinanceSubTab = FinanceSubTab.SUMMARY,
    val screenStack: List<AppScreen> = emptyList()
) {
    val currentScreen: AppScreen?
        get() = screenStack.lastOrNull()

    fun selectTab(tab: BottomTab): AppNavigationState = copy(selectedTab = tab)

    fun selectFinanceSubTab(tab: FinanceSubTab): AppNavigationState = copy(financeSubTab = tab)

    fun openScreen(screen: AppScreen): AppNavigationState = copy(
        screenStack = screenStack.filterNot { it == screen } + screen
    )

    fun replaceScreen(from: AppScreen, to: AppScreen): AppNavigationState = copy(
        screenStack = screenStack.filterNot { it == from || it == to } + to
    )

    fun closeScreen(screen: AppScreen): AppNavigationState = copy(
        screenStack = screenStack.filterNot { it == screen }
    )

    fun popScreen(): AppNavigationState = copy(
        screenStack = screenStack.dropLast(1)
    )
}

fun initialAppNavigationState(rawTab: String?): AppNavigationState {
    if (rawTab.isNullOrBlank()) return AppNavigationState()

    return when (rawTab) {
        "PAYROLL" -> AppNavigationState(
            selectedTab = BottomTab.FINANCE,
            financeSubTab = FinanceSubTab.PAYROLL
        )
        "PAYMENTS" -> AppNavigationState(
            selectedTab = BottomTab.FINANCE,
            financeSubTab = FinanceSubTab.PAYMENTS
        )
        else -> AppNavigationState(
            selectedTab = runCatching { BottomTab.valueOf(rawTab) }
                .getOrDefault(BottomTab.CALENDAR),
            financeSubTab = FinanceSubTab.SUMMARY
        )
    }
}

fun restoreAppNavigationState(
    selectedTabName: String?,
    financeSubTabName: String?,
    screenStackNames: String?
): AppNavigationState {
    val selectedTab = selectedTabName
        ?.let { runCatching { BottomTab.valueOf(it) }.getOrNull() }
        ?: BottomTab.CALENDAR
    val financeSubTab = financeSubTabName
        ?.let { runCatching { FinanceSubTab.valueOf(it) }.getOrNull() }
        ?: FinanceSubTab.SUMMARY
    val screenStack = screenStackNames
        .orEmpty()
        .split(',')
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { name -> runCatching { AppScreen.valueOf(name) }.getOrNull() }
        .distinct()
        .toList()

    return AppNavigationState(
        selectedTab = selectedTab,
        financeSubTab = financeSubTab,
        screenStack = screenStack
    )
}

val AppNavigationStateSaver: Saver<AppNavigationState, ArrayList<String>> = Saver(
    save = { state ->
        arrayListOf(
            state.selectedTab.name,
            state.financeSubTab.name,
            state.screenStack.joinToString(",") { it.name }
        )
    },
    restore = { saved ->
        restoreAppNavigationState(
            selectedTabName = saved.getOrNull(0),
            financeSubTabName = saved.getOrNull(1),
            screenStackNames = saved.getOrNull(2)
        )
    }
)
