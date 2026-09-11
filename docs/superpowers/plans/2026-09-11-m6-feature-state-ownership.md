# M6 Feature State Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move feature-specific Compose/workflow state out of `ShiftSalaryApp` into focused feature owners while preserving all existing save/restore behavior and user-visible workflows.

**Architecture:** Keep `currentMonth`, `activeWorkplaceId`, and M5 `navigationState` at the root because they are genuinely cross-feature. Every other root remembered mutable value moves to a feature-specific state holder with an explicit Saver/transient contract. UI-state transitions move into those holders; Room/DataStore/network/platform side effects remain wired in the root until M7.

**Tech Stack:** Kotlin, Jetpack Compose runtime/saveable state, JUnit 4, existing Gradle/Android VPS toolchain.

**Spec:** `docs/superpowers/specs/2026-09-11-m4-m7-refactor-design.md`

**State inventory:** `docs/project/M6_STATE_OWNERSHIP_INVENTORY.md`

**M6A reconciliation (2026-09-12):** the Calendar/Pattern bounded slice was implemented and independently verified in parallel with the first draft of this milestone-wide plan. Its concrete owners are `CalendarInteractionState` and `CalendarPatternWorkflowState`. They satisfy the approved M6 feature-ownership contract and supersede the provisional `CalendarFeatureState` / `PatternFeatureState` class split in Tasks 2–3 below. **Do not create duplicate replacement holders merely to match those provisional names.** The execution order therefore differs from the original numbered order: M6A (Calendar/Pattern) is verified first; the next unimplemented bounded slice is Notes.

## Global Constraints

- Preserve the current eight-tab UI and all visible workflows 1:1 through M6.
- Do not change payroll semantics, Room schema/version, backup format, navigation architecture, dependency versions, release signing, or production deployment.
- Do not introduce Hilt, Koin, Navigation Compose, MVI framework, or a generic root `AppState`.
- Values currently using `rememberSaveable` remain saveable; plain-`remember` values remain transient; profile-keyed values retain their profile reset behavior.
- Concrete stores/DAOs/services remain M4 dependencies in M6; introducing feature-facing data ports is M7.
- Characterization/TDD precedes each non-trivial state-transition extraction.
- Every task ends at a buildable/testable checkpoint and is committed independently.

---

## Verified predecessor slice — M6A Calendar/Pattern

- Code boundary: `300ecbf49c583bb1cc313700256496fca5a01562..c0af0c4c7f2de90e581d251698eb41ae31051d30`.
- Fresh clean JVM gate: **67/67**, 0 failures, 0 errors, 0 skipped.
- Phone + Wear assemble/lint: **BUILD SUCCESSFUL**; app lint `0 errors, 58 warnings, 12 hints`; Wear lint `0 errors, 22 warnings, 3 hints`.
- APK evidence: app SHA-256 `ce399db6228aa748b985e3f70f8c8da3b05f1e0c189784927ccc7a6775d6f168`; Wear SHA-256 `0747d838f4ff3eaa73d32f90b9592d891be1aaaef18f5ac7766ada5172ee711a`.
- Structural assertion: root remembered mutable variables reduced from 67 to **44**; the 23 M6A variables are absent from root, `currentMonth`/`activeWorkplaceId`/`navigationState` remain root-owned, and M5 legacy navigation flags remain absent.
- Independent Codex review: **no actionable Critical/Important/P2 findings**.
- Next unimplemented bounded slice under this milestone plan: **Notes state ownership**.

---

### Task 1: Notes state holder pilot

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/notes/NotesFeatureState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/NotesFeatureStateTest.kt`

**Interfaces:**
- Produces: `NotesFeatureState`, `NotesFeatureStateSaver`, `rememberNotesFeatureState()`.
- `NotesFeatureState` exposes read-only properties `editingNoteId`, `noteDraftDateIso`, `noteDraftWorkplaceId`, `noteDraftShiftCode` plus `openEditor(noteId, dateIso, workplaceId, shiftCode)` and `clearEditor()`.
- All four payload fields are included in the Saver because all four are `rememberSaveable` at baseline.

- [ ] **Step 1: Write RED tests for note draft ownership**

```kotlin
@Test
fun `open editor replaces complete note draft atomically`() {
    val state = NotesFeatureState()
    state.openEditor("n1", "2026-09-11", "work-2", "N")
    assertEquals("n1", state.editingNoteId)
    assertEquals("2026-09-11", state.noteDraftDateIso)
    assertEquals("work-2", state.noteDraftWorkplaceId)
    assertEquals("N", state.noteDraftShiftCode)
}

@Test
fun `clear editor clears identity but keeps draft context until next open`() {
    val state = NotesFeatureState()
    state.openEditor("n1", "2026-09-11", "work-2", "N")
    state.clearEditor()
    assertNull(state.editingNoteId)
    assertEquals("2026-09-11", state.noteDraftDateIso)
}
```

- [ ] **Step 2: Run the targeted test and require RED because the holder does not yet exist**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest --tests '*NotesFeatureStateTest*' --no-daemon
```

Expected: compilation failure naming `NotesFeatureState`.

- [ ] **Step 3: Implement `NotesFeatureState` and its Saver**

Use one stable feature object with `mutableStateOf` properties and a `listSaver`/equivalent saver that serializes the four baseline-saveable values. `clearEditor()` must reproduce the current Back behavior: clear `editingNoteId`; do not invent extra draft resets.

- [ ] **Step 4: Replace the four root note variables and repeated open/edit payload assignments**

Create once near the root state declarations:

```kotlin
val notesState = rememberNotesFeatureState()
```

Calendar, Today, Notes, DayAssignments and NoteEditor callbacks call `notesState.openEditor(...)`; NoteEditor back calls `notesState.clearEditor()`.

- [ ] **Step 5: Run targeted test plus full app JVM suite**

Require all tests green and `git diff --check` clean.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/notes/NotesFeatureState.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/NotesFeatureStateTest.kt
git commit -m "refactor: move notes workflow state"
```

---

### Task 2: Calendar interaction state — SATISFIED BY VERIFIED M6A

> Superseded implementation detail: use the existing verified `CalendarInteractionState` plus the clear-range portion of `CalendarPatternWorkflowState`; do not introduce a duplicate `CalendarFeatureState`. M6A code head: `c0af0c4c7f2de90e581d251698eb41ae31051d30`.

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarFeatureState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/CalendarFeatureStateTest.kt`

**Interfaces:**
- Produces: `CalendarFeatureState`, `CalendarFeatureStateSaver`, `rememberCalendarFeatureState(defaultWorkplaceFilterId, saveKey)`.
- Saveable fields: `quickPickerOpen`, `activeBrushCode`, `isLegendExpanded`, clear-range fields/confirmations, `calendarWorkplaceFilterId`.
- Transient fields deliberately excluded from Saver: `selectedDate`, `dayAssignmentsPreviewDate`.
- Methods include `toggleQuickPicker()`, `closeQuickPicker()`, `selectBrush(code)`, `selectClearBrush()`, `disableBrush()`, `startClearRangeMode()`, `cancelClearRangeMode()`, `selectClearRangeDate(date)`, `confirmClearRangeComplete()`, `requestClearMonth()`, `requestClearAll()`, `dismissClearMonth()`, `dismissClearAll()`.

- [ ] **Step 1: Write RED tests for multi-field transitions and transient restoration contract**

```kotlin
@Test
fun `starting clear range disables brush and clears previous range`() {
    val state = CalendarFeatureState(defaultWorkplaceFilterId = "ALL")
    state.selectBrush("D")
    state.startClearRangeMode()
    assertNull(state.activeBrushCode)
    assertTrue(state.clearRangeModeActive)
    assertNull(state.clearRangeStartIso)
    assertNull(state.pendingClearRangeStartIso)
    assertNull(state.pendingClearRangeEndIso)
}

@Test
fun `second clear range date normalizes start and end`() {
    val state = CalendarFeatureState(defaultWorkplaceFilterId = "ALL")
    state.startClearRangeMode()
    state.selectClearRangeDate(LocalDate.parse("2026-09-12"))
    state.selectClearRangeDate(LocalDate.parse("2026-09-10"))
    assertEquals("2026-09-10", state.pendingClearRangeStartIso)
    assertEquals("2026-09-12", state.pendingClearRangeEndIso)
}
```

Also test a pure restore helper/Saver payload that restores saveable fields but yields `selectedDate == null` and `dayAssignmentsPreviewDate == null`.

- [ ] **Step 2: Run targeted test and require RED**

- [ ] **Step 3: Implement the holder and exact save/restore behavior**

The `remember...` factory must use `appearanceSettings.calendarDefaultWorkplaceMode.name` as the save-key and compute the default filter exactly as the current root does. Do not include transient dialog dates in the Saver.

- [ ] **Step 4: Wire Calendar callbacks to holder operations**

Move the current repeated brush/clear-range reset sequences into holder methods. DAO/store/alarm-scheduler calls remain in `ShiftSalaryApp`; after successful destructive operations the root calls the holder's completion/reset method.

- [ ] **Step 5: Verify targeted tests and full JVM suite**

- [ ] **Step 6: Commit**

```bash
git commit -m "refactor: move calendar interaction state"
```

---

### Task 3: Pattern workflow state — SATISFIED BY VERIFIED M6A

> Superseded implementation detail: use the existing verified `CalendarPatternWorkflowState`; do not introduce a duplicate `PatternFeatureState`. All 17 pattern/clear-range saveable fields round-trip through its explicit Saver.

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/patterns/PatternFeatureState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/PatternFeatureStateTest.kt`

**Interfaces:**
- Produces: `PatternFeatureState`, `PatternFeatureStateSaver`, `rememberPatternFeatureState()`.
- Owns every M6 inventory pattern value and exposes operations `openList()`, `openNewEditor()`, `openEditor(id)`, `openApply(id)`, `openQuickPicker()`, `activatePattern(id)`, `cancelPatternMode()`, `selectRangeDate(date)`, `dismissPreview()`, `clearPendingRange()`, `closeEditor()`, `closeApply()`.

- [ ] **Step 1: Write RED tests**

Cover editor payload reset, duplicate-free active pattern selection, normalized two-date range selection, and `cancelPatternMode()` clearing both pattern ID and range start.

- [ ] **Step 2: Run targeted test and require RED**

- [ ] **Step 3: Implement holder/Saver**

All pattern fields listed in the inventory are baseline-saveable and therefore must be included in restoration.

- [ ] **Step 4: Replace root pattern variables**

Calendar and Shifts both reference the same `patternState` instance. Do not nest it inside `CalendarFeatureState` or `ShiftFeatureState`.

- [ ] **Step 5: Run targeted + full JVM tests**

- [ ] **Step 6: Commit**

```bash
git commit -m "refactor: move pattern workflow state"
```

---

### Task 4: Finance/payments/report workflow state

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceFeatureState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/FinanceFeatureStateTest.kt`

**Interfaces:**
- Produces: `FinanceFeatureState`, `FinanceFeatureStateSaver`, `rememberFinanceFeatureState(initialMonth)`.
- Saveable fields are exactly the Finance fields listed in `M6_STATE_OWNERSHIP_INVENTORY.md`.
- Report CSV/PDF content, bytes and file names are transient and excluded from Saver.
- Methods: `changePeriodMode(mode, currentStart, currentEnd)`, `selectWorkplace(id)`, `previousYear()`, `nextYear()`, `selectYear(year)`, `shiftRange(start, end, days)`, `pickRangeStart(date, currentEnd)`, `pickRangeEnd(date, currentStart)`, `toggleSummary()`, `openSettingsFor(workplaceId)`, `openNewPayment(workplaceId)`, `openPayment(id, workplaceId)`, `closePaymentDialog()`, `startDeductionEdit(id)`, `clearDeductionEdit()`, `stageCsv(content, fileName)`, `stagePdf(bytes, fileName)`.

- [ ] **Step 1: Write RED characterization tests for period/range behavior**

```kotlin
@Test
fun `range start after end swaps boundaries exactly like legacy root`() {
    val state = FinanceFeatureState(YearMonth.of(2026, 9))
    state.pickRangeStart(LocalDate.parse("2026-09-20"), LocalDate.parse("2026-09-10"))
    assertEquals("2026-09-10", state.payrollRangeStartIso)
    assertEquals("2026-09-20", state.payrollRangeEndIso)
}
```

Also cover YEAR-mode initialization from current period end, payment editor workplace normalization input, summary toggle, and restore preserving saveable fields while transient report payloads reset.

- [ ] **Step 2: Run targeted test and require RED**

- [ ] **Step 3: Implement holder/Saver without touching payroll calculation code**

- [ ] **Step 4: Wire Finance, Payments, Payroll settings, deductions and report export callbacks**

All calls to `PayrollCalculator`, stores, report builders, launchers and history/event logging remain where they are; only their UI/workflow payload state moves.

- [ ] **Step 5: Run Finance tests plus existing payroll characterization suite and then full app JVM suite**

- [ ] **Step 6: Commit**

```bash
git commit -m "refactor: move finance workflow state"
```

---

### Task 5: Shift/template editor state

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/templates/ShiftFeatureState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/ShiftFeatureStateTest.kt`

**Interfaces:**
- Produces: `ShiftFeatureState`, Saver, `rememberShiftFeatureState()`.
- Methods: `setMode(mode)`, `openNewShift()`, `openNewSystemStatus()`, `openExistingShift(code, isSystemStatus)`, `clearEditor()`.

- [ ] **Step 1: Write RED tests proving the three editor entry modes and clear behavior**

- [ ] **Step 2: Run targeted RED**

- [ ] **Step 3: Implement holder/Saver with the three baseline-saveable fields**

- [ ] **Step 4: Replace root editor/mode variables and Back cleanup**

M5 navigation continues to open/close `AppScreen.SHIFT_TEMPLATE_EDITOR`; the feature holder owns only its payload.

- [ ] **Step 5: Run targeted + full JVM tests**

- [ ] **Step 6: Commit**

```bash
git commit -m "refactor: move shift editor state"
```

---

### Task 6: Settings and service workflow state

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/SettingsFeatureState.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/ServiceWorkflowState.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/widget/WidgetSettingsRuntimeState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/SettingsFeatureStateTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/ServiceWorkflowStateTest.kt`

**Interfaces:**
- `SettingsFeatureState`: holiday sync status/message, manual-holiday editor payload/dialog, workplace-rename dialog, custom-font status; Saver contains all six baseline-saveable fields.
- `WidgetSettingsRuntimeState`: one mutable refresh token; factory is keyed by `activeProfileId` and is not saveable.
- `ServiceWorkflowState`: saveable post-update/import/backup/auto-upload fields plus transient backup content, Excel bytes/preview/candidates and signed-in account.

- [ ] **Step 1: Write RED settings-state tests**

Cover manual holiday new/edit entry, close cleanup, holiday sync start/finish message behavior, and workplace rename open/close.

- [ ] **Step 2: Write RED service-state tests**

Cover staging/clearing a backup export, staging an Excel import result, saveable status restoration with transient payload reset, and account/auto-upload bookkeeping remaining distinct.

- [ ] **Step 3: Run targeted tests and require RED**

- [ ] **Step 4: Implement both holders plus widget runtime**

Do not move Drive/Excel/backup implementation objects into these state classes. They contain UI/workflow state only.

- [ ] **Step 5: Wire Settings, Manual Holidays, BackupRestore, ExcelImport, Appearance custom-font status, post-update dialog and widget refresh**

Profile change must create a new `WidgetSettingsRuntimeState`, preserving the current token reset behavior.

- [ ] **Step 6: Run targeted + backup compatibility + full app JVM tests**

- [ ] **Step 7: Commit**

```bash
git commit -m "refactor: move settings service workflow state"
```

---

### Task 7: Alarm runtime state

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/AlarmRuntimeState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/AlarmRuntimeStateTest.kt`

**Interfaces:**
- Produces `AlarmRuntimeState` with `permissionRefreshToken`, `lastRescheduleResult`, `refreshPermissions()` and `recordReschedule(result)`.
- `rememberAlarmRuntimeState(activeProfileId)` is keyed by profile and intentionally not saveable.

- [ ] **Step 1: Write RED test for default/mutation contract**

- [ ] **Step 2: Run targeted RED**

- [ ] **Step 3: Implement holder and profile-keyed remember factory**

- [ ] **Step 4: Replace the two remaining alarm root variables**

Do not alter `ShiftAlarmsTabUiState`, scheduler semantics, permissions or PendingIntent behavior.

- [ ] **Step 5: Run alarm planner/characterization tests plus full JVM suite**

- [ ] **Step 6: Commit**

```bash
git commit -m "refactor: move alarm runtime state"
```

---

### Task 8: Structural cleanup and M6 qualification

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt` only for proven orphan imports/local aliases.
- Modify: `docs/project/CURRENT_STATE.md`
- Modify: `docs/superpowers/plans/2026-09-11-m6-feature-state-ownership.md`
- Modify: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- No new production behavior. This task verifies the M6 boundary.

- [ ] **Step 1: Assert the root remembered mutable owner count**

Run a source assertion that extracts root `var ... by remember/rememberSaveable` declarations from `ShiftSalaryApp`. Require exactly:

```text
currentMonth
activeWorkplaceId
navigationState
```

Fail if any feature variable remains or if a generic `AppState`/`ShiftSalaryAppState` class was introduced.

- [ ] **Step 2: Run fresh clean JVM gate**

```bash
source scripts/android/vps-env.sh
./gradlew clean :app:testDebugUnitTest --no-daemon
```

Require zero failures/errors/skips except any explicitly pre-existing skip documented in the XML evidence; record exact test count.

- [ ] **Step 3: On the unchanged code tree run phone/Wear build + lint**

```bash
source scripts/android/vps-env.sh
./gradlew \
  :app:assembleDebug :wear:assembleDebug \
  :app:lintDebug :wear:lintDebug --no-daemon
```

Require both APKs and zero lint errors. Record warnings/hints and APK SHA-256 values.

- [ ] **Step 4: Run `git diff --check` and independent Codex review of the whole M6 branch against its canonical master base**

Repair only proven correctness/behavior findings. Any production repair requires affected targeted tests and a repeated final gate on the corrected tree.

- [ ] **Step 5: Update canonical milestone evidence**

Set M6 to `VERIFIED / READY FOR MERGE` on the feature branch, record exact branch/base/head/test/build/lint/review evidence and set the next boundary to M7 — UI/Data Boundary. Do not claim M6 COMPLETE until it is merged to canonical `master` after owner authorization.

- [ ] **Step 6: Append final TURN END, commit docs closeout and guarded-push `refactor/m6-feature-state-ownership`**

Verify remote branch SHA equals local branch SHA and canonical `master` is unchanged. No release/deploy.

---

## M6 acceptance checklist

- [ ] Existing UI/IA is intentionally unchanged.
- [ ] Exactly three root remembered mutable owners remain: `currentMonth`, `activeWorkplaceId`, `navigationState`.
- [x] Calendar interaction and Calendar pattern/clear-range workflow state have focused verified owners from M6A.
- [ ] Finance, Shift, Notes, Settings/service and Alarm runtime state have focused owners.
- [ ] Baseline saveable/transient/profile-keyed semantics are preserved.
- [ ] M5 navigation state is not duplicated.
- [ ] Concrete stores/DAOs/services have not been wrapped in generic M6 repositories.
- [ ] Payroll, Room and backup semantics are unchanged.
- [ ] Full tests/build/lint and independent review are green before push.
