package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationStateTest {

    @Test
    fun `primary navigation is calendar finance more only`() {
        assertEquals(
            listOf(BottomTab.CALENDAR, BottomTab.FINANCE, BottomTab.SETTINGS),
            primaryBottomTabs
        )
        assertEquals(BottomTab.CALENDAR, primaryTabFor(BottomTab.TODAY))
        assertEquals(BottomTab.FINANCE, primaryTabFor(BottomTab.FINANCE))
        assertEquals(BottomTab.SETTINGS, primaryTabFor(BottomTab.ALARMS))
        assertEquals(BottomTab.SETTINGS, primaryTabFor(BottomTab.SHIFTS))
    }

    @Test
    fun `default and invalid external routes fall back to calendar summary`() {
        assertEquals(
            AppNavigationState(),
            initialAppNavigationState(null)
        )
        assertEquals(
            AppNavigationState(),
            initialAppNavigationState("")
        )
        assertEquals(
            AppNavigationState(),
            initialAppNavigationState("NOT_A_ROUTE")
        )
    }

    @Test
    fun `legacy payroll and payments aliases open finance sub tabs`() {
        assertEquals(
            AppNavigationState(
                selectedTab = BottomTab.FINANCE,
                financeSubTab = FinanceSubTab.PAYROLL
            ),
            initialAppNavigationState("PAYROLL")
        )
        assertEquals(
            AppNavigationState(
                selectedTab = BottomTab.FINANCE,
                financeSubTab = FinanceSubTab.PAYMENTS
            ),
            initialAppNavigationState("PAYMENTS")
        )
    }

    @Test
    fun `every direct bottom tab name remains a valid external route`() {
        BottomTab.entries.forEach { tab ->
            assertEquals(
                AppNavigationState(
                    selectedTab = tab,
                    financeSubTab = FinanceSubTab.SUMMARY
                ),
                initialAppNavigationState(tab.name)
            )
        }
    }

    @Test
    fun `tab and finance sub tab selection are typed and independent`() {
        val initial = AppNavigationState(
            selectedTab = BottomTab.CALENDAR,
            financeSubTab = FinanceSubTab.PAYMENTS
        )

        val finance = initial.selectTab(BottomTab.FINANCE)
        val payroll = finance.selectFinanceSubTab(FinanceSubTab.PAYROLL)

        assertEquals(BottomTab.FINANCE, payroll.selectedTab)
        assertEquals(FinanceSubTab.PAYROLL, payroll.financeSubTab)
    }

    @Test
    fun `opening screens pushes in order and duplicate open moves existing screen to top`() {
        val state = AppNavigationState()
            .openScreen(AppScreen.DEDUCTIONS)
            .openScreen(AppScreen.DEDUCTION_EDITOR)
            .openScreen(AppScreen.DEDUCTIONS)

        assertEquals(
            listOf(AppScreen.DEDUCTION_EDITOR, AppScreen.DEDUCTIONS),
            state.screenStack
        )
        assertEquals(AppScreen.DEDUCTIONS, state.currentScreen)
    }

    @Test
    fun `replace removes source and existing target then places target on top`() {
        val state = AppNavigationState(
            screenStack = listOf(
                AppScreen.REPORT_HISTORY,
                AppScreen.REPORT_CENTER,
                AppScreen.MONTHLY_REPORT
            )
        )

        val replaced = state.replaceScreen(
            from = AppScreen.REPORT_CENTER,
            to = AppScreen.MONTHLY_REPORT
        )

        assertEquals(
            listOf(AppScreen.REPORT_HISTORY, AppScreen.MONTHLY_REPORT),
            replaced.screenStack
        )
    }

    @Test
    fun `close removes only named screen and pop removes only top`() {
        val initial = AppNavigationState(
            screenStack = listOf(
                AppScreen.DEDUCTIONS,
                AppScreen.DEDUCTION_EDITOR
            )
        )

        val closedEditor = initial.closeScreen(AppScreen.DEDUCTION_EDITOR)
        assertEquals(listOf(AppScreen.DEDUCTIONS), closedEditor.screenStack)
        assertEquals(AppScreen.DEDUCTIONS, closedEditor.currentScreen)

        val popped = initial.popScreen()
        assertEquals(listOf(AppScreen.DEDUCTIONS), popped.screenStack)

        val empty = popped.popScreen()
        assertEquals(emptyList<AppScreen>(), empty.screenStack)
        assertNull(empty.currentScreen)
    }


    @Test
    fun `deductions editor back returns to deductions parent`() {
        val deductions = AppNavigationState().openScreen(AppScreen.DEDUCTIONS)
        val editor = deductions.openScreen(AppScreen.DEDUCTION_EDITOR)

        assertEquals(
            listOf(AppScreen.DEDUCTIONS, AppScreen.DEDUCTION_EDITOR),
            editor.screenStack
        )

        val afterBack = editor.closeScreen(AppScreen.DEDUCTION_EDITOR)

        assertEquals(listOf(AppScreen.DEDUCTIONS), afterBack.screenStack)
        assertEquals(AppScreen.DEDUCTIONS, afterBack.currentScreen)
    }

    @Test
    fun `opening deduction editor twice keeps one editor above deductions`() {
        val state = AppNavigationState()
            .openScreen(AppScreen.DEDUCTIONS)
            .openScreen(AppScreen.DEDUCTION_EDITOR)
            .openScreen(AppScreen.DEDUCTION_EDITOR)

        assertEquals(
            listOf(AppScreen.DEDUCTIONS, AppScreen.DEDUCTION_EDITOR),
            state.screenStack
        )
        assertEquals(AppScreen.DEDUCTION_EDITOR, state.currentScreen)
    }

    @Test
    fun `quick start effect does not cover restored fullscreen destination`() {
        val restoredEditor = AppNavigationState(
            screenStack = listOf(AppScreen.SHIFT_TEMPLATE_EDITOR)
        )

        val afterEffect = applyQuickStartNavigation(
            state = restoredEditor,
            quickStartDismissed = false
        )

        assertEquals(restoredEditor, afterEffect)
        assertEquals(AppScreen.SHIFT_TEMPLATE_EDITOR, afterEffect.currentScreen)
    }

    @Test
    fun `quick start effect opens guide only from idle root`() {
        val state = AppNavigationState()

        val afterEffect = applyQuickStartNavigation(
            state = state,
            quickStartDismissed = false
        )

        assertEquals(listOf(AppScreen.QUICK_START_GUIDE), afterEffect.screenStack)
    }

    @Test
    fun `restore ignores unknown screens and defaults unknown tab names`() {
        val restored = restoreAppNavigationState(
            selectedTabName = "UNKNOWN_TAB",
            financeSubTabName = "UNKNOWN_FINANCE",
            screenStackNames = "DEDUCTIONS,UNKNOWN_SCREEN,DEDUCTION_EDITOR"
        )

        assertEquals(BottomTab.CALENDAR, restored.selectedTab)
        assertEquals(FinanceSubTab.SUMMARY, restored.financeSubTab)
        assertEquals(
            listOf(AppScreen.DEDUCTIONS, AppScreen.DEDUCTION_EDITOR),
            restored.screenStack
        )
    }
    @Test
    fun `restore keeps workplaces contextual screen`() {
        val restored = restoreAppNavigationState(
            selectedTabName = BottomTab.SETTINGS.name,
            financeSubTabName = FinanceSubTab.SUMMARY.name,
            screenStackNames = "WORKPLACES"
        )

        assertEquals(listOf(AppScreen.WORKPLACES), restored.screenStack)
        assertEquals(AppScreen.WORKPLACES, restored.currentScreen)
    }

}
