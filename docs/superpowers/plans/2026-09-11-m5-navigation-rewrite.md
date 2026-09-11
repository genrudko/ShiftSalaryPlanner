# M5 Navigation Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Replace root string/boolean navigation in `ShiftSalaryApp` with one typed, saveable tab + fullscreen-stack model while preserving the current eight-tab UI and all existing navigation behavior 1:1.

**Architecture:** Add a pure navigation model/reducer in the existing `ui/navigation` area and make Compose own a single `AppNavigationState`. Migrate bottom tab/Finance sub-tab first, then the 22 fullscreen overlay flags in bounded groups. Feature dialogs, editor payloads and screen-specific state stay where they are until M6.

**Tech Stack:** Kotlin, Jetpack Compose `rememberSaveable`/`Saver`, existing `BottomTab`, existing `FinanceSubTab`, JUnit JVM tests, existing app/Wear Gradle gates.

**Spec:** `docs/superpowers/specs/2026-09-11-m4-m7-refactor-design.md`
**Behavioral inventory:** `docs/project/M5_NAVIGATION_INVENTORY.md`

## Global Constraints

- Preserve the current visible eight-tab structure, labels and ordering exactly through M5.
- Preserve widget aliases `PAYROLL` and `PAYMENTS` exactly.
- No M8 information-architecture changes.
- No M6 feature-state/ViewModel extraction.
- No M7 repository/domain/data hardening.
- Do not change payroll, Room schema, backup format or alarm delivery semantics.
- Do not add Navigation Compose, Hilt, Koin or broad dependency upgrades.
- Existing M3 tests remain authoritative; new M5 tests characterize navigation semantics.
- Release/deploy are out of scope.

---

### Task 1: Typed navigation model and reducer

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationState.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/AppNavigationStateTest.kt`

**Interfaces:**
- Consumes: existing `BottomTab`, existing `FinanceSubTab`.
- Produces: `AppScreen`, `AppNavigationState`, `initialAppNavigationState`, pure navigation operations and `AppNavigationStateSaver`.

Define exactly these root screen identities:

```kotlin
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
```

State contract:

```kotlin
data class AppNavigationState(
    val selectedTab: BottomTab = BottomTab.CALENDAR,
    val financeSubTab: FinanceSubTab = FinanceSubTab.SUMMARY,
    val screenStack: List<AppScreen> = emptyList()
) {
    val currentScreen: AppScreen?
        get() = screenStack.lastOrNull()
}
```

- [x] **Step 1: Write RED tests** for default/invalid external route, `PAYROLL`, `PAYMENTS`, direct valid tab, tab/sub-tab selection, push, duplicate-open, nested push, replace, close and pop semantics.
- [x] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*AppNavigationStateTest' --no-daemon`. Expected: compile/test failure because the model does not exist.
- [x] **Step 3: Implement the minimal pure model**. `openScreen(screen)` must remove any existing occurrence before appending it. `replaceScreen(from, to)` must remove `from` and any existing `to`, then append `to`. `closeScreen(screen)` removes only that identity. `popScreen()` removes only the last stack element.
- [x] **Step 4: Implement external parsing** in `initialAppNavigationState(rawTab: String?)`: `PAYROLL` -> `FINANCE/PAYROLL`, `PAYMENTS` -> `FINANCE/PAYMENTS`, valid tab name -> that tab with `SUMMARY`, invalid/blank -> `CALENDAR/SUMMARY`.
- [x] **Step 5: Implement `AppNavigationStateSaver`** using only enum-name strings and one delimiter-joined screen-stack string so the saved payload is Bundle-safe. Restore unknown enum names defensively to the documented defaults and ignore unknown screen names.
- [x] **Step 6: Re-run targeted test**; expected all M5 navigation-model tests PASS.
- [x] **Step 7: Run** `git diff --check` and commit `refactor: add typed app navigation state`.

### Task 2: Migrate launcher and bottom-tab selection

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ShiftSalaryPlannerRoot.kt`
- Use: `ui/navigation/AppNavigationState.kt`
- Test: `AppNavigationStateTest.kt`

**Interfaces:**
- `MainActivity` produces one typed `initialNavigationState` from `EXTRA_OPEN_TAB`.
- `ShiftSalaryPlannerRoot(initialNavigationState: AppNavigationState)` forwards typed initial navigation.
- `ShiftSalaryApp(..., initialNavigationState: AppNavigationState, ...)` owns `var navigationState by rememberSaveable(stateSaver = AppNavigationStateSaver)`.

- [x] **Step 1:** Extend RED tests for `PAYROLL`, `PAYMENTS`, every valid `BottomTab.name`, invalid and null external values.
- [x] **Step 2:** Replace private `parseInitialWidgetTab` / `parseInitialWidgetFinanceSubTab` with `initialAppNavigationState(intent?.getStringExtra(EXTRA_OPEN_TAB))`; preserve `intent.removeExtra(EXTRA_OPEN_TAB)`.
- [x] **Step 3:** Change root/app signatures to accept typed initial navigation; replace `selectedTabName` and `financeSubTabName` with `navigationState.selectedTab` and `navigationState.financeSubTab`.
- [x] **Step 4:** Replace tab callbacks with `navigationState = navigationState.selectTab(...)`; Finance sub-tab callbacks use `selectFinanceSubTab(...)`. Existing callbacks that select FINANCE + SUMMARY must do both explicitly through the typed operations.
- [x] **Step 5:** Source assertion: `MainActivity.kt` contains no `selectedTabName` or `financeSubTabName`; old parse helpers are absent.
- [x] **Step 6:** Run targeted navigation tests and full `:app:testDebugUnitTest`; expected all tests green.
- [x] **Step 7:** Commit `refactor: migrate root tab navigation state`.

### Task 3: Migrate standalone fullscreen routes

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `AppNavigationStateTest.kt`

**Interfaces:**
- Consumes: `AppNavigationState.openScreen`, `closeScreen`, `replaceScreen`, `popScreen`, `currentScreen`.
- Produces: typed root navigation for standalone screens while leaving modal feature state untouched.

Migrate these first because they do not intentionally nest under another fullscreen route:

```text
MONTHLY_REPORT
APP_HEALTH_CHECK
APP_EVENT_LOG
REPORT_HISTORY
QUICK_ACTIONS_SETTINGS
QUICK_START_GUIDE
REPORT_CENTER
PAYROLL_DIAGNOSTICS
REPORT_VISIBILITY_SETTINGS
PAYROLL_SETTINGS
APPEARANCE_SETTINGS
CURRENT_PARAMETERS
PROFILES
MANUAL_HOLIDAYS
BACKUP_RESTORE
EXCEL_IMPORT
WIDGET_SETTINGS
ADDITIONAL_PAYMENTS
DEDUCTIONS
SHIFT_TEMPLATE_EDITOR
NOTE_EDITOR
```

`DEDUCTION_EDITOR` is deliberately deferred to Task 4 because it proves stack nesting.

- [x] **Step 1:** Add/confirm reducer tests for open/close/replace without duplicates.
- [x] **Step 2:** Replace every listed `showX = true` opener with `navigationState = navigationState.openScreen(AppScreen.X)` unless the old flow explicitly closed one fullscreen and opened another; those become `replaceScreen(old, new)`.
- [x] **Step 3:** Replace every corresponding `AnimatedFullscreenOverlay(visible = showX)` with `visible = AppScreen.X in navigationState.screenStack`.
- [x] **Step 4:** Replace each screen `onBack`/dismiss close with `closeScreen(AppScreen.X)`, preserving editor-payload cleanup beside the navigation call.
- [x] **Step 5:** Convert `hasFullscreenUi` to `navigationState.screenStack.isNotEmpty()`. Convert system `BackHandler` to inspect `navigationState.currentScreen`, clear payload only for `SHIFT_TEMPLATE_EDITOR`, `NOTE_EDITOR` and later `DEDUCTION_EDITOR`, then `popScreen()`.
- [x] **Step 6:** Do not touch modal flags from `M5_NAVIGATION_INVENTORY.md`.
- [x] **Step 7:** Run `:app:testDebugUnitTest`; expected all tests green.
- [x] **Step 8:** Commit `refactor: migrate fullscreen root destinations`.

### Task 4: Preserve nested fullscreen back stack

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Modify: `app/src/test/java/com/vigilante/shiftsalaryplanner/AppNavigationStateTest.kt`

**Interfaces:**
- Deductions opener -> stack `[DEDUCTIONS]`.
- Deductions editor opener -> stack `[DEDUCTIONS, DEDUCTION_EDITOR]`.
- Back/close editor -> stack `[DEDUCTIONS]`.
- Back/close deductions -> empty stack.

- [x] **Step 1:** Write the explicit nested-stack regression test above and a duplicate-editor-open test.
- [x] **Step 2:** Migrate `showDeductionEditorScreen` to `AppScreen.DEDUCTION_EDITOR` while leaving `editingDeductionId` in existing feature state.
- [x] **Step 3:** Verify all Deductions -> editor add/edit callbacks push `DEDUCTION_EDITOR` without closing `DEDUCTIONS`.
- [x] **Step 4:** Verify editor save/back closes only the editor and retains the deductions parent.
- [x] **Step 5:** Run targeted navigation test + full JVM suite.
- [x] **Step 6:** Commit `refactor: preserve nested navigation stack`.

### Task 5: Remove obsolete root navigation booleans

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Modify: `docs/project/M5_NAVIGATION_INVENTORY.md` evidence only if an inventory correction is proven by code.

**Interfaces:**
- Root navigation state is exclusively `AppNavigationState`.
- Modal/feature booleans remain intentionally present.

- [x] **Step 1:** Source assertion requires absence of all 22 legacy root flags listed in the inventory.
- [x] **Step 2:** Assert modal flags such as `showAdditionalPaymentDialog`, `showManualHolidayDialog`, `showPattern*`, clear confirmations, workplace rename and post-update dialog were not opportunistically migrated.
- [x] **Step 3:** Run `git diff --check` and full `:app:testDebugUnitTest`.
- [x] **Step 4:** Commit `refactor: remove obsolete root navigation flags`.

### Task 6: M5 qualification and closeout

**Files:**
- Modify: `docs/project/CURRENT_STATE.md`
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify: `docs/superpowers/plans/2026-09-11-m5-navigation-rewrite.md` evidence/checkmarks only.

**Interfaces:**
- Produces a verified M5 branch ready for owner-authorized merge; does not merge/release/deploy.

- [x] **Step 1:** Fresh `clean + :app:testDebugUnitTest`; require zero failures/errors/skips and record the exact count.
- [x] **Step 2:** On unchanged tree run `:app:assembleDebug :wear:assembleDebug :app:lintDebug :wear:lintDebug`; require both APKs and zero lint errors.
- [x] **Step 3:** Re-run source assertions for the 22 removed root flags and preserved modal feature state.
- [x] **Step 4:** Independent Codex review of the whole M5 diff against its canonical `master` base. Repair only proven behavior/correctness findings and repeat affected gates after production changes.
- [x] **Step 5:** Update `CURRENT_STATE.md` to `M5 VERIFIED / READY FOR MERGE`, with exact branch/head/evidence and M6 boundary.
- [x] **Step 6:** Append current `TURN END` to `WORK_PROGRESS.md`, commit docs-only closeout, guarded-push M5 branch, verify remote SHA = local SHA and canonical `master` unchanged.

## Plan self-review checklist

- [x] M5 preserves the existing eight-tab IA; redesign is deferred to M8.
- [x] All 22 current fullscreen root booleans are explicitly accounted for.
- [x] Nested Deductions -> editor behavior has a dedicated regression task.
- [x] Widget `PAYROLL`/`PAYMENTS` compatibility is explicitly tested.
- [x] Feature/modal state is intentionally excluded and reserved for M6.
- [x] No M7 data/domain abstractions or dependency upgrades are introduced.
- [x] Every code-changing task has a targeted/full regression gate and a commit boundary.
