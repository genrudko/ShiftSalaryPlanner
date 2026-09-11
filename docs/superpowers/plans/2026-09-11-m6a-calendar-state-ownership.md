# M6A Calendar/Pattern State Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Move Calendar and pattern/clear-range UI workflow state out of the 4,800-line `ShiftSalaryApp` root into focused, testable state holders while preserving the current UI, navigation, persistence behavior and save/restore semantics 1:1.

**Architecture:** M6 is executed as several independently reviewable slices. M6A extracts only Calendar-owned interaction state and pattern/clear-range workflow state. Cross-feature `currentMonth` stays in the root because Finance, reports and holiday synchronization still consume it; Room/store writes and pattern-application business operations also stay in existing orchestration until M7. Two focused state holders are used instead of one replacement `AppState`.

**Tech Stack:** Kotlin, Jetpack Compose runtime state, `remember`/`rememberSaveable`, JUnit JVM tests, existing M3–M5 regression/build/lint gates.

**Spec:** `docs/superpowers/specs/2026-09-11-m4-m7-refactor-design.md`

## Global Constraints

- Preserve current user-visible Calendar behavior and the current eight-tab navigation exactly.
- Preserve the distinction between existing non-saveable `remember` state and `rememberSaveable` state; M6A must not silently make transient selected-day dialogs survive recreation if they did not before.
- `currentMonth` remains at the root in M6A because it is shared by Calendar, Finance/reports and holiday logic.
- M5 `AppNavigationState` remains the only root navigation model and is not duplicated in either Calendar state holder.
- Pattern application, shift assignment writes, clear-range persistence operations and Room/store access remain in existing orchestration; state holders own UI/workflow state only.
- No payroll semantic changes, Room schema/version changes, backup-format changes or alarm-delivery changes.
- No ViewModel/Hilt/Koin/Navigation Compose or broad dependency upgrades.
- No visual redesign or M8 information-architecture changes.
- Release/deploy are out of scope.
- Later M6 slices are intentionally separate: Finance/report state; shifts/templates/manual-holiday state; notes state; alarms state; backup/import/settings workflow state.

---

### Task 1: Extract pattern and clear-range workflow state

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarPatternWorkflowState.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/CalendarPatternWorkflowStateTest.kt`

**Interfaces:**
- Produces: `CalendarPatternWorkflowState` and `rememberCalendarPatternWorkflowState()`.
- The holder owns exactly these currently-root `rememberSaveable` values:

```text
showPatternListDialog
showPatternEditDialog
editingPatternId
showPatternApplyDialog
applyingPatternId
showPatternQuickPicker
activePatternId
patternRangeStartIso
pendingPatternRangeStartIso
pendingPatternRangeEndIso
showPatternPreviewDialog
clearRangeModeActive
clearRangeStartIso
pendingClearRangeStartIso
pendingClearRangeEndIso
showClearMonthConfirm
showClearAllCalendarConfirm
```

- The holder exposes mutable properties with the same value types plus small UI-state operations for repeated transitions; it does not receive a DAO/store/repository.
- `rememberCalendarPatternWorkflowState()` uses one explicit `Saver` so all 17 values retain the same recreation behavior as the previous individual `rememberSaveable` declarations.

- [x] **Step 1: Write RED tests** that instantiate `CalendarPatternWorkflowState` and assert these exact transitions:
  - create/edit pattern opens editor with the expected nullable id;
  - apply pattern opens apply dialog with the expected id and cancel clears both;
  - selecting a quick pattern sets `activePatternId`, clears `patternRangeStartIso` and closes the quick picker;
  - opening the manager from the quick picker closes quick picker and opens list;
  - beginning clear-range mode clears active pattern/range and pending clear range;
  - cancel/reset clear-range mode clears all clear-range fields;
  - closing preview clears preview visibility and pending pattern range without inventing persistence effects.
- [x] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*CalendarPatternWorkflowStateTest' --no-daemon`. Expected: compile failure because `CalendarPatternWorkflowState` does not exist.
- [x] **Step 3: Implement the minimal holder** with no Android `Context`, store, DAO or coroutine scope. Repeated reset/open/close operations are pure state transitions only.
- [x] **Step 4: Implement the explicit Saver** using Bundle-safe primitives only (`Boolean` and nullable/non-null `String` represented with a stable sentinel or indexed nullable-safe list format). Restoration must reproduce all 17 fields exactly.
- [x] **Step 5: Add a Saver round-trip test** using helper serialization/restoration functions exposed `internal` for JVM testing; assert every non-default field round-trips exactly.
- [x] **Step 6: Re-run the targeted test**; require all tests green.
- [x] **Step 7: Run** `git diff --check` and commit `refactor: extract calendar pattern workflow state`.

### Task 2: Wire pattern/clear workflow state into ShiftSalaryApp

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Use: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarPatternWorkflowState.kt`
- Test: `CalendarPatternWorkflowStateTest.kt`

**Interfaces:**
- `ShiftSalaryApp` creates exactly one `val patternWorkflowState = rememberCalendarPatternWorkflowState()`.
- Existing dialogs/screens continue receiving the same primitive values and callbacks; only ownership changes.
- Existing persistence/coroutine logic remains in `ShiftSalaryApp` and reads/writes state through the holder.

- [x] **Step 1: Replace the 17 root declarations** with one `patternWorkflowState` holder. Do not alter `currentMonth`, `activeBrushCode`, `quickPickerOpen`, selected-day state or navigation in this task.
- [x] **Step 2: Mechanically redirect reads/writes** to holder properties or its transition methods. Preserve existing order where callbacks both mutate UI state and perform persistence.
- [x] **Step 3: Source assertions:** `MainActivity.kt` must contain no root declarations matching the 17 names above; dialog/render callbacks may still reference those concepts only through `patternWorkflowState`.
- [x] **Step 4: Run targeted holder tests and full** `./gradlew :app:testDebugUnitTest --no-daemon`; require zero failures/errors/skips.
- [x] **Step 5: Run** `git diff --check` and commit `refactor: wire calendar pattern workflow state`.

### Task 3: Extract Calendar interaction state while preserving saveability

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarInteractionState.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/CalendarInteractionStateTest.kt`

**Interfaces:**
- Produces: `CalendarInteractionState` and `rememberCalendarInteractionState(workplaceFilterKey: String, initialWorkplaceFilterId: String)`.
- Owns exactly:

```text
selectedDate                  // currently remember: transient across recreation
dayAssignmentsPreviewDate     // currently remember: transient across recreation
quickPickerOpen               // currently rememberSaveable
activeBrushCode               // currently rememberSaveable
isLegendExpanded              // currently rememberSaveable
calendarWorkplaceFilterId     // currently keyed rememberSaveable
```

- The factory must preserve that distinction: selected-date and preview-date backing `MutableState`s come from `remember`; the other four backing states come from `rememberSaveable`, with workplace filter keyed by `workplaceFilterKey` exactly as today.
- The class may expose direct mutable properties plus focused methods `toggleQuickPicker()`, `closeQuickPicker()`, `selectBrush(code)`, `clearBrush()` and `toggleLegend()`; these methods may not touch pattern state or persistence.

- [x] **Step 1: Write RED JVM tests** for quick-picker toggle/close, brush select/clear, legend toggle and direct selected/preview date mutation.
- [x] **Step 2: Run** the targeted test and confirm compile failure because the holder does not exist.
- [x] **Step 3: Implement `CalendarInteractionState`** as a wrapper around injected `MutableState` objects so the Compose factory can preserve each field's existing saveability without a global Saver that changes semantics.
- [x] **Step 4: Implement `rememberCalendarInteractionState(...)`** with `remember` for the two date states, `rememberSaveable` for picker/brush/legend/filter, and a keyed workplace-filter state using `workplaceFilterKey`.
- [x] **Step 5: Re-run targeted tests**; require green.
- [x] **Step 6: Commit** `refactor: extract calendar interaction state` after `git diff --check`.

### Task 4: Wire Calendar interaction state and remove root declarations

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Use: `CalendarInteractionState.kt`
- Test: `CalendarInteractionStateTest.kt`, existing Calendar/navigation/payroll tests.

**Interfaces:**
- `ShiftSalaryApp` creates one `calendarInteractionState` using the current appearance-default workplace mode as the same saveable key/initial filter source used before M6A.
- Cross-feature `currentMonth` remains a root variable.
- Existing workplace-filter correction `LaunchedEffect`s continue to run, mutating `calendarInteractionState.calendarWorkplaceFilterId` rather than a root variable.

- [x] **Step 1: Replace the six root declarations** with `calendarInteractionState`; keep the existing `currentMonth` declaration untouched.
- [x] **Step 2: Redirect Calendar callbacks and selected-day/day-preview dialogs** to the holder with no user-visible behavior change.
- [x] **Step 3: Preserve cross-feature behavior:** Finance/report month callbacks still use root `currentMonth`; calendar workplace-filter correction effects keep the same conditions and selected ids.
- [x] **Step 4: Source assertions:** the six old root `var` declarations are absent; `currentMonth` is still root-owned; M5 navigation state is unchanged.
- [x] **Step 5: Run targeted holder tests plus full** `:app:testDebugUnitTest`; require all tests green.
- [x] **Step 6: Commit** `refactor: wire calendar interaction state` after `git diff --check`.

### Task 5: M6A qualification, review and milestone sequencing evidence

**Files:**
- Modify: `docs/project/CURRENT_STATE.md`
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify: this plan for evidence/checkmarks only.

**Interfaces:**
- Produces a verified M6A checkpoint inside the ongoing `refactor/m6-feature-state-ownership` milestone branch.
- Does not merge M6 into `master`; later M6 slices continue on the same M6 branch only after M6A is green/reviewed.

- [x] **Step 1: Fresh clean JVM gate:** `./gradlew clean :app:testDebugUnitTest --no-daemon`; record exact test count and require zero failures/errors/skips.
- [x] **Step 2: On the unchanged tree run** `:app:assembleDebug :wear:assembleDebug :app:lintDebug :wear:lintDebug`; require both APKs and zero lint errors.
- [x] **Step 3: Structural assertions:** all 17 pattern/clear root declarations and all 6 Calendar-interaction root declarations are gone; root `currentMonth` remains; M5 root navigation booleans remain absent.
- [x] **Step 4: Independent Codex review** of the M6A diff against `300ecbf49c583bb1cc313700256496fca5a01562`, scoped to save/restore parity, callback ordering and accidental behavior changes. Repair only proven correctness findings and repeat affected gates after production changes.
- [x] **Step 5: Update `CURRENT_STATE.md`** to record `M6 IN PROGRESS — M6A VERIFIED`, exact evidence and next slice: M6B Finance/report state ownership. Do not claim M6 complete.
- [x] **Step 6: Append durable `TURN END`/checkpoint** to `WORK_PROGRESS.md`, commit docs, and keep the M6 branch ready for the next slice. Push may occur at a stable M6A checkpoint, but merge to `master` remains owner-gated until the whole M6 milestone is complete.

## Plan self-review checklist

- [x] M6A changes ownership, not user-visible behavior.
- [x] `currentMonth` remains cross-feature state at the smallest common owner.
- [x] The existing transient-vs-saveable distinction for selected-day state is explicitly preserved.
- [x] Pattern/clear persistence operations remain outside the state holder.
- [x] The 17 pattern/clear values and 6 Calendar interaction values are fully accounted for.
- [x] M5 navigation is not duplicated.
- [x] No M7 repository/DAO abstraction is introduced.
- [x] No ViewModel/DI/navigation framework or dependency upgrade is introduced.
- [x] Later M6 feature slices are explicitly separated instead of becoming one god-state changeset.
- [x] Every code-changing task has RED→GREEN or focused regression verification plus a commit boundary.


## M6A VERIFIED evidence

Status: **VERIFIED on `refactor/m6-feature-state-ownership`**. This is a slice checkpoint, not completion of M6 and not authorization to merge/release.

- Production code boundary: `300ecbf49c583bb1cc313700256496fca5a01562..c0af0c4c7f2de90e581d251698eb41ae31051d30`.
- Clean JVM qualification: `job_b629c9c9649d4723a8fa5b46de842c47` — **BUILD SUCCESSFUL**, 67 tests, 0 failures, 0 errors, 0 skipped.
- Unchanged-tree phone/Wear build+lint: `job_ba5a3353a93149368304e0d2af28da54` — **BUILD SUCCESSFUL in 10m 59s**; app lint 0 errors / 58 warnings / 12 hints; Wear lint 0 errors / 22 warnings / 3 hints.
- APK fingerprints: app `ce399db6228aa748b985e3f70f8c8da3b05f1e0c189784927ccc7a6775d6f168`; Wear `0747d838f4ff3eaa73d32f90b9592d891be1aaaef18f5ac7766ada5172ee711a`.
- Structural gate: `job_8e74a357c3a9446fb2fae0ee2a542943` — exactly **44** root remembered mutable variables remain; no M6A legacy root declarations; `currentMonth`, `activeWorkplaceId`, and M5 `navigationState` remain; `git diff --check` clean.
- Independent Codex review: `job_6a14afb88cc746bf8b123ae569f2268b` — **no actionable Critical/Important/P2 correctness findings**.
- The milestone-wide plan authored concurrently on canonical `master` used provisional Calendar/Pattern holder names/grouping. This verified two-holder M6A implementation is accepted as the concrete equivalent and the milestone plan/inventory are reconciled to it instead of rewriting tested code for naming symmetry.
- Next bounded slice: **Notes state ownership**.
