# M10 Calendar Vertical Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the Calendar vertical slice on the accepted Variant A / Evolution design system across month scanning, selected-day detail, quick assignment/brush, pattern/clear states, workplace/status interactions, and representative multi-workplace states without changing schedule semantics.

**Architecture:** Build directly on the verified M9 `EvolutionSurface`/token foundation and the existing M6/M7 Calendar state/data boundaries. Keep `CalendarTab` callbacks, `CalendarInteractionState`, `CalendarPatternWorkflowState`, `ScheduleDataPort`, assignment persistence, and existing pattern/clear semantics intact; M10 changes presentation composition and screenshot coverage only. The M9 month grid remains the semantic anchor while remaining Calendar-owned legacy `AppExpressiveSurface`/panel styling is migrated to Evolution primitives.

**Tech Stack:** Android, Jetpack Compose Material 3, Kotlin 2.2.10, AGP 9.2.1, existing Compose Preview Screenshot Testing, JUnit4 structure/state tests, existing M3/M6/M7 characterization coverage.

**Spec:** `docs/project/M8_FOCUSED_SCREEN_CONTRACTS.md` (CAL-01 through CAL-04), `docs/project/M8_FOCUSED_REDESIGN_REVIEW.md`, and `docs/superpowers/specs/2026-09-12-m9-variant-a-visual-foundation-design.md`.

## Global Constraints

- Calendar remains the default/primary product surface and must answer “what is my work pattern?” without opening a day.
- Preserve text codes, built-in glyphs, emoji, Material icons, per-template colors, multiple assignments, note/override/holiday markers, today, selection and preview semantics.
- Shift/template colors are user/domain data; do not recolor them to the brand palette.
- Today, selected, brush, pattern preview and clear-range preview remain distinct facts/states.
- Month swipe is enabled only in normal mode; existing brush/pattern/clear gating remains unchanged.
- Keep existing Comfortable/Compact behavior and landscape support.
- Large-font and high-contrast behavior are acceptance requirements, not deferred cleanup.
- Do not invent alarm/payroll/day-detail data that current Calendar inputs do not provide.
- No changes to Room schema, backup format, alarm scheduling, payroll arithmetic, Wear contracts, dependencies, schedule assignment semantics or persistence ordering.
- No root-navigation redesign beyond the already accepted M8/M9 contracts.
- No push, merge into canonical `master`, release or deploy in M10 implementation/qualification without a separate owner gate.

---

### Task 1: Finish the Calendar month-shell visual migration

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarHeaderComponents.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/MonthHolidayInfoCard.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M10CalendarVerticalSliceStructureTest.kt`

**Interfaces:**
- Consumes: M9 `EvolutionSurface`, `EvolutionSurfaceRole`, `EvolutionIconTile`, `EvolutionIconTone`, `evolutionColorRoles`, existing `CalendarTab` callback/state contract.
- Produces: a month screen whose visible supporting surfaces use the Variant A grammar while leaving cell/assignment behavior unchanged.

- [ ] **Step 1: Write a failing structure test for the remaining month-shell legacy styling**

Create `M10CalendarVerticalSliceStructureTest.kt` with a source helper matching the existing M8/M9 structure tests and assertions equivalent to:

```kotlin
@Test fun calendarMonthShellUsesEvolutionSurfacesForOperationalCards() {
    val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
    assertTrue(tab.contains("fun ActiveBrushCard("))
    assertTrue(tab.contains("fun PatternApplyModeCard("))
    assertTrue(tab.contains("fun ClearRangeModeCard("))
    assertTrue(tab.contains("EvolutionSurface("))
    assertFalse(tab.substringAfter("fun ActiveBrushCard(").substringBefore("private fun MonthCheckInlineCard(").contains("AppExpressiveSurface("))
}

@Test fun calendarSecondaryMonthPanelsDoNotReturnToOutlinedPanelSea() {
    val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
    val holidays = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/MonthHolidayInfoCard.kt")
    assertFalse(tab.substringAfter("private fun MonthCheckInlineCard(").contains("AppExpressiveSurface("))
    assertFalse(holidays.contains("AppExpressiveSurface("))
}
```

- [ ] **Step 2: Run the M10 structure test and verify RED**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest --tests '*M10CalendarVerticalSliceStructureTest' --no-daemon
```

Expected: FAIL because active brush/pattern/clear and secondary month cards still use legacy `AppExpressiveSurface`/panel styling.

- [ ] **Step 3: Migrate active brush, pattern and clear-range mode surfaces**

In `CalendarTab.kt`, preserve the existing callbacks/text/state decisions, but replace the legacy bordered/glass wrappers with `EvolutionSurface`:

```kotlin
EvolutionSurface(
    modifier = Modifier.fillMaxWidth(),
    role = EvolutionSurfaceRole.ACCENT,
    shape = RoundedCornerShape(appCornerRadius(18.dp)),
    shadowElevation = 1.dp
) {
    // existing mode content and explicit cancel/apply actions
}
```

Use `EvolutionIconTile` with `BRAND`/`WARNING`/`DANGER` only for semantic mode emphasis. Do not apply destructive red to preview-only selection; reserve it for clear confirmation/action affordances.

- [ ] **Step 4: Migrate month check, today-notes, legend and month-holiday supporting panels**

Use `PRIMARY` or `SOFT` Evolution surfaces, keep actions and content identical, and remove decorative one-pixel panel borders in normal contrast. High-contrast borders remain supplied by `EvolutionSurface` itself.

- [ ] **Step 5: Keep Calendar header/workplace/profile controls on the Evolution path**

Do not change selector callbacks or options. Ensure the Calendar call sites opt into/use the M9 Evolution treatment and do not reintroduce `appPanelColor()`/`appPanelBorderColor()` on the Calendar path.

- [ ] **Step 6: Run targeted Calendar structure/state tests**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*M10CalendarVerticalSliceStructureTest' \
  --tests '*M9CalendarVariantAStructureTest' \
  --tests '*M8CalendarFocusedRedesignStructureTest' \
  --tests '*CalendarInteractionStateTest' \
  --tests '*CalendarPatternWorkflowStateTest' --no-daemon
```

Expected: PASS.

- [ ] **Step 7: Commit the month-shell migration**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarHeaderComponents.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/MonthHolidayInfoCard.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M10CalendarVerticalSliceStructureTest.kt
git commit -m "feat: complete M10 calendar month shell"
```

---

### Task 2: Complete quick assignment and single-day assignment surfaces

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/ShiftPickerDialog.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/ShiftPickerOptionCard.kt`
- Modify: `app/src/test/java/com/vigilante/shiftsalaryplanner/M10CalendarVerticalSliceStructureTest.kt`

**Interfaces:**
- Consumes: existing `QuickShiftBar` template grouping, workplace scoping, system-status filtering, eraser/normal/cycle/more callbacks, and current ShiftPicker selection callbacks.
- Produces: Variant A quick tools and single-day assignment surfaces with unchanged assignment behavior.

- [ ] **Step 1: Extend the structure test and verify RED**

Add assertions equivalent to:

```kotlin
@Test fun quickAssignmentAndPickerUseEvolutionGrammar() {
    val quick = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt")
    val picker = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/ShiftPickerDialog.kt")
    assertTrue(quick.contains("EvolutionSurface("))
    assertFalse(quick.contains("readableContentColor(appPanelColor())"))
    assertTrue(picker.contains("EvolutionSurface("))
    assertFalse(picker.contains("AppExpressiveSurface("))
}
```

Run only `*M10CalendarVerticalSliceStructureTest`; expected RED on remaining legacy quick/picker styling.

- [ ] **Step 2: Finish QuickShiftBar migration without changing the tool grammar**

Keep exactly these first-layer semantics: up to four active-workplace templates, Eraser, Normal, More and optional Cycle. Expanded mode still groups templates by workplace and exposes system statuses. Convert content colors/surfaces to Evolution roles and retain a visible selected outline/tint only for the active tool/template.

- [ ] **Step 3: Migrate ShiftPickerDialog and ShiftPickerOptionCard**

Keep workplace sections, current selection semantics, text/glyph/emoji/icon identity, and existing confirm/dismiss callbacks. Use Evolution surfaces and semantic selected state instead of generic outlined panels. Do not change assignment persistence or workplace scoping.

- [ ] **Step 4: Run quick/picker + state regression**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*M10CalendarVerticalSliceStructureTest' \
  --tests '*CalendarInteractionStateTest' \
  --tests '*CalendarPatternWorkflowStateTest' \
  --tests '*ScheduleDataPortTest' \
  --tests '*M7ScheduleBoundaryStructureTest' --no-daemon
```

Expected: PASS.

- [ ] **Step 5: Commit quick assignment migration**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/ShiftPickerDialog.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/ShiftPickerOptionCard.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M10CalendarVerticalSliceStructureTest.kt
git commit -m "feat: migrate M10 calendar quick assignment"
```

---

### Task 3: Migrate selected-day detail and override presentation

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/DayAssignmentsDialog.kt`
- Modify: `app/src/test/java/com/vigilante/shiftsalaryplanner/M10CalendarVerticalSliceStructureTest.kt`

**Interfaces:**
- Consumes: `CalendarDayAssignment`, `ShiftDayEntity`, `ShiftTemplateEntity`, notes, template alarm config/time labels and current per-day override save callback.
- Produces: CAL-02 day detail using Variant A hierarchy without fabricating unsupported data.

- [ ] **Step 1: Extend the structure test and verify RED**

Add:

```kotlin
@Test fun dayDetailUsesEvolutionSurfacesWithoutChangingOverrideContract() {
    val day = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/DayAssignmentsDialog.kt")
    assertTrue(day.contains("fun DayAssignmentsDialog("))
    assertTrue(day.contains("onSaveShiftDayOverride"))
    assertTrue(day.contains("EvolutionSurface("))
    assertFalse(day.contains("AppExpressiveSurface("))
    assertFalse(day.contains("containerColor = appPanelColor()"))
}
```

Run only `*M10CalendarVerticalSliceStructureTest`; expected RED.

- [ ] **Step 2: Restyle the day-detail dialog hierarchy**

Use the Evolution floating/primary surface colors for the dialog. Each assignment remains a separate row with shift identity, title, workplace, hours/time and override marker. Notes remain a separate context section. Do not invent alarm status or earnings when the composable does not receive that data.

- [ ] **Step 3: Restyle the individual-day override editor**

Keep all existing fields, fallback-to-template behavior, parsing, save/reset semantics and `ShiftDayEntity.copy(...)` contract. Only the surrounding surfaces/action hierarchy change; text fields remain explicit inputs.

- [ ] **Step 4: Run day-detail + persistence/payroll safety tests**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*M10CalendarVerticalSliceStructureTest' \
  --tests '*ScheduleDataPortTest' \
  --tests '*BackupCompatibilityTest' \
  --tests '*PaymentEnhancementsTest' \
  --tests '*PayrollCharacterizationTest' --no-daemon
```

Expected: PASS; no persistence/payroll behavior changes.

- [ ] **Step 5: Commit day-detail migration**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/DayAssignmentsDialog.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M10CalendarVerticalSliceStructureTest.kt
git commit -m "feat: migrate M10 calendar day detail"
```

---

### Task 4: Expand deterministic Calendar visual acceptance coverage

**Files:**
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTest.kt`
- Update only after inspection: `app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTestKt/*.png`

**Interfaces:**
- Consumes: the existing fixed September 2026 fixture (`reviewMonth`, `reviewToday`, `reviewTemplates`, `reviewAssignments`, `reviewHolidays`).
- Produces: one deterministic visual matrix covering the complete M10 Calendar slice.

- [ ] **Step 1: Add deterministic preview states without changing the representative month**

Retain existing Light/Dark/fontScale 1.3/range previews and add at minimum:

```text
Calendar brush active
Calendar quick picker / active tool
Calendar multi-workplace selected day
Calendar clear-range or pattern mode surface
Calendar selected-day detail
```

Reuse September 2026 and the existing multi-workplace assignments on the 15th/22nd. Do not create unrelated fixture months.

- [ ] **Step 2: Validate against current goldens and require visual RED for new/changed states**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

Expected: existing unchanged references pass; new/changed states report missing/different references until intentionally approved.

- [ ] **Step 3: Export and inspect actual/diff PNGs before updating references**

Reject/fix if any of these occur:

- month pattern is slower to scan than the accepted M9 reference;
- text/glyph/emoji/Material-icon identity is clipped or subordinated to decoration;
- multiple assignments overwrite each other;
- today and selected collapse into one state;
- note/override/holiday markers crowd the shift identity;
- active brush/tool is ambiguous;
- preview looks already applied;
- clear mode looks destructive before confirmation;
- large font hides the day number or primary identity;
- Dark/high-contrast restores a universal bright-outline sea;
- day-detail rows merge multiple workplaces into one ambiguous result.

- [ ] **Step 4: Update screenshot references only after the visual review is clean**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:updateDebugScreenshotTest --no-daemon
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

Expected: PASS.

- [ ] **Step 5: Commit visual references**

```bash
git add app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTest.kt \
        app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTestKt
git commit -m "test: qualify M10 calendar visual states"
```

---

### Task 5: M10 compatibility qualification and durable checkpoint

**Files:**
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify after qualification: `docs/project/CURRENT_STATE.md`

**Interfaces:**
- Consumes: Tasks 1-4 committed tree.
- Produces: a local M10 `COMPLETE / VERIFIED` checkpoint, still integration-pending.

- [ ] **Step 1: Run Calendar behavior/structure gate from a clean tree**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*CalendarInteractionStateTest' \
  --tests '*CalendarPatternWorkflowStateTest' \
  --tests '*ScheduleDataPortTest' \
  --tests '*M7ScheduleBoundaryStructureTest' \
  --tests '*M8CalendarFocusedRedesignStructureTest' \
  --tests '*M9CalendarVariantAStructureTest' \
  --tests '*M10CalendarVerticalSliceStructureTest' --no-daemon
```

Expected: PASS.

- [ ] **Step 2: Run the unchanged-tree full phone qualification**

Run:

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:validateDebugScreenshotTest --no-daemon
git diff --check
git status --short --branch
```

Expected: `BUILD SUCCESSFUL`, lint completes, all screenshot tests validate, `git diff --check` is empty and the worktree is clean after the docs checkpoint commit.

- [ ] **Step 3: Inspect/export the final M10 Calendar PNG set**

At minimum retain user-visible artifacts for Light, Dark, fontScale 1.3, range/pattern preview, brush/quick tools, representative multi-workplace state and selected-day detail.

- [ ] **Step 4: Record M10 evidence**

Append `WORK_PROGRESS.md` with task commits, exact gate jobs/results and visual findings. Update `CURRENT_STATE.md` to `M10 — Calendar Vertical Slice: COMPLETE / VERIFIED locally (integration pending)` only after all behavior + visual gates are green. Record explicitly that Finance Calculation/Payments, More, Today and secondary non-Calendar migrations remain future work.

- [ ] **Step 5: Commit the M10 checkpoint**

```bash
git add docs/project/WORK_PROGRESS.md docs/project/CURRENT_STATE.md
git commit -m "docs: checkpoint M10 calendar vertical slice"
```

Do not push, merge, release or deploy.
