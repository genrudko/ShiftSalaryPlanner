# M13 Shifts & Alarms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the shipped Variant A Shifts + Alarms relationship to the Evolution UI while preserving scheduler, persistence, workplace, Wear, backup and direct-entry contracts.

**Architecture:** Keep `TemplatesScreen`/`ShiftTemplateEditorScreen` and `ShiftAlarmsTab` as separate feature surfaces connected by the existing `ShiftTemplateAlarmConfig.shiftCode` relationship. Change presentation hierarchy and add deterministic visual coverage; do not alter `ShiftAlarmStore`, `ShiftAlarmScheduler`, `AlarmDataPort`, `AlarmPlatformPort` or `WearSyncContract` behavior.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing Evolution components, Compose Preview Screenshot Testing, JUnit4/Gradle.

**Spec:** `docs/superpowers/specs/2026-09-13-m13-shifts-alarms-design.md`

## Global Constraints

- Canonical base is `master@f10baa8685c62aa9b2b2782865ab16c1c42ba9f3`.
- Preserve the v6.4 product model: separate `SHIFTS` and `ALARMS` destinations with per-template alarm context.
- Do not change alarm scheduling, reschedule, suppression, snooze, reboot, exact/inexact fallback or system-clock semantics.
- Do not change `ShiftAlarmStore` serialization, `WearSyncContract`, Room schema/migrations, backup payload semantics, payroll formulas or dependency graph.
- Keep workplace-scoped shift codes and legacy/direct `BottomTab.SHIFTS` / `BottomTab.ALARMS` compatibility.
- Accept new screenshot references only after inspecting rendered PNGs.

---

### Task 1: Lock M13 structural and compatibility invariants

**Files:**
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsStructureTest.kt`
- Modify only if required by testable navigation surface: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`

**Interfaces:**
- Consumes: `BottomTab.SHIFTS`, `BottomTab.ALARMS`, `TemplatesScreen`, `ShiftTemplateEditorScreen`, `ShiftAlarmsTab`, existing alarm ports.
- Produces: regression guard proving M13 remains a UI migration over existing domain/platform boundaries.

- [ ] **Step 1: Write failing structure tests**

Create tests that read production sources and assert:

```kotlin
assertTrue(main.contains("BottomTab.SHIFTS"))
assertTrue(main.contains("BottomTab.ALARMS"))
assertTrue(editor.contains("currentAlarmTemplateConfig: ShiftTemplateAlarmConfig?"))
assertTrue(editor.contains("ShiftTemplateAlarmConfig"))
assertFalse(alarms.contains("ShiftAlarmScheduler."))
assertFalse(alarms.contains("ShiftAlarmStore("))
assertTrue(templates.contains("EvolutionTextTabs"))
assertTrue(alarms.contains("EvolutionSurface"))
```

The final two assertions intentionally define the visual migration and must fail on the M12 baseline.

- [ ] **Step 2: Run RED**

Run:

```bash
./gradlew --no-daemon :app:testDebugUnitTest --tests '*M13ShiftsAlarmsStructureTest*'
```

Expected: FAIL because Templates/Alarms do not yet use the required Evolution primitives; compatibility assertions must already pass.

- [ ] **Step 3: Keep the RED test committed with the first minimal UI migration only**

Do not weaken compatibility assertions to make the test green.

---

### Task 2: Migrate Shift Templates to Evolution hierarchy

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/templates/TemplatesScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/templates/TemplatesComponents.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsStructureTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/TemplatesScreenUiStateReducerTest.kt`

**Interfaces:**
- Consumes: `TemplatesScreenState`, `TemplatesScreenActions`, `CalendarWorkplaceSwitcher`, `TemplateMode`, template reorder/delete callbacks.
- Produces: same callback behavior with Evolution surface/tab hierarchy.

- [ ] **Step 1: Preserve behavior while replacing the mode switch and top-level panels**

Use the existing enum values directly:

```kotlin
EvolutionTextTabs(
    items = listOf(
        EvolutionTabItem(TemplateMode.SHIFTS, "Смены"),
        EvolutionTabItem(TemplateMode.CYCLES, "Циклы")
    ),
    selected = mode,
    onSelected = onModeChange
)
```

Wrap regular list/status summary in `EvolutionSurface` and keep `CalendarWorkplaceSwitcher` as the workplace scope control. Keep all existing reorder, swipe, add/edit/duplicate/delete and system-status callbacks unchanged.

- [ ] **Step 2: Run focused GREEN**

```bash
./gradlew --no-daemon :app:testDebugUnitTest --tests '*M13ShiftsAlarmsStructureTest*' --tests '*TemplatesScreenUiStateReducerTest*' --tests '*ShiftFeatureStateTest*'
```

Expected: structure requirement for Templates is GREEN; Alarms Evolution assertion may remain RED until Task 4 if tested separately. Run Templates state tests independently if necessary to keep each task's gate unambiguous.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/templates app/src/test/java/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsStructureTest.kt
git commit -m "feat: migrate M13 shift templates to Evolution"
```

---

### Task 3: Reorganize Shift Template Editor without domain changes

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/shifts/ShiftTemplateEditorScreen.kt`
- Modify only for reusable presentation helpers: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/shifts/ShiftsUiComponents.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsStructureTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/ShiftFeatureStateTest.kt`

**Interfaces:**
- Consumes: `currentTemplate`, `currentAlarmTemplateConfig`, workplaces, save/delete callbacks and existing editor state.
- Produces: same `onSave(ShiftTemplateEntity, ShiftTemplateAlarmConfig, String)` payload with clearer Evolution sections.

- [ ] **Step 1: Add a structural guard for alarm context and save contract**

Assert the editor retains both:

```kotlin
assertTrue(editor.contains("onSave: (ShiftTemplateEntity, ShiftTemplateAlarmConfig, String) -> Unit"))
assertTrue(editor.contains("currentAlarmTemplateConfig"))
assertTrue(editor.contains("AlarmContext"))
```

The `AlarmContext` assertion is RED before the new presentation helper/content is added.

- [ ] **Step 2: Build the alarm-context summary from existing config**

Derive presentation only:

```kotlin
val templateAlarmEnabled = currentAlarmTemplateConfig?.enabled == true
val activeAlarmCount = currentAlarmTemplateConfig?.alarms?.count { it.enabled } ?: 0
val alarmSummary = when {
    !templateAlarmEnabled -> "Будильники выключены для этой смены"
    activeAlarmCount == 0 -> "Будильники включены • сигналов нет"
    else -> "Будильники включены • сигналов: $activeAlarmCount"
}
```

Do not mutate alarm lists from this summary. Start/end fields continue to produce the existing copied `ShiftTemplateAlarmConfig` in `performSave()`.

- [ ] **Step 3: Recompose editor hierarchy**

Use Evolution/expressive surfaces for Identity, Schedule, Pay/Special day, and Alarm context; retain current validation, protected-template restrictions, unsaved-exit confirmation, color/icon pickers and save/delete actions.

- [ ] **Step 4: Run focused GREEN**

```bash
./gradlew --no-daemon :app:testDebugUnitTest --tests '*M13ShiftsAlarmsStructureTest*' --tests '*ShiftFeatureStateTest*' --tests '*ShiftAlarmPlanningTest*'
```

Expected: PASS; planning results remain unchanged.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/shifts app/src/test/java/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsStructureTest.kt
git commit -m "feat: migrate M13 shift editor hierarchy"
```

---

### Task 4: Reorder the Alarms operational center onto Evolution

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsComponents.kt`
- Preserve unless a presentation-only call site requires it: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabUiState.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsStructureTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/ShiftAlarmsTabUiStateReducerTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/ShiftAlarmPlanningTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/AlarmPortsTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M7AlarmBoundaryStructureTest.kt`

**Interfaces:**
- Consumes: `ShiftAlarmsTabState`, `ShiftAlarmsTabActions`, reducer/autosave effect, existing dialogs and upcoming-alarm actions.
- Produces: reordered Evolution presentation with identical save/platform callbacks.

- [ ] **Step 1: Make readiness a deterministic presentation model**

Add a UI-only helper/data class in the alarm presentation package, for example:

```kotlin
data class AlarmReadinessUi(
    val ready: Boolean,
    val issues: List<String>
)
```

Build issues from `canScheduleExactAlarms`, notification permission and full-screen permission, respecting existing Android-version checks in the Composable. This helper must not request permissions or schedule alarms.

- [ ] **Step 2: Put operational content first**

Order root sections as: upcoming alarms, readiness/master enablement, template configs, behavior/Wear/horizon/manual actions, ring appearance. Reuse existing `UpcomingShiftAlarmsCard`, `ShiftTemplateAlarmConfigCard`, edit dialogs and callbacks.

- [ ] **Step 3: Replace top-level equal-weight cards with Evolution surfaces/actions**

At least the readiness and global behavior groups must use `EvolutionSurface`/`EvolutionActionRow`. Keep text labels for enabled/disabled and permission status; do not rely on tint alone.

- [ ] **Step 4: Run alarm regression gate**

```bash
./gradlew --no-daemon :app:testDebugUnitTest \
  --tests '*M13ShiftsAlarmsStructureTest*' \
  --tests '*ShiftAlarmsTabUiStateReducerTest*' \
  --tests '*ShiftAlarmPlanningTest*' \
  --tests '*AlarmPortsTest*' \
  --tests '*M7AlarmBoundaryStructureTest*'
```

Expected: PASS with no scheduler/store boundary regression.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms app/src/test/java/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsStructureTest.kt
git commit -m "feat: migrate M13 alarms operational center"
```

---

### Task 5: Add deterministic M13 visual qualification

**Files:**
- Create: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsVisualScreenshotTest.kt`
- Create via screenshot update only: `app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsVisualScreenshotTestKt/*.png`

**Interfaces:**
- Consumes: production `TemplatesScreen`, editor content that can be rendered deterministically, and `ShiftAlarmsTab`/extractable production content.
- Produces: deterministic references for the nine required M13 states.

- [ ] **Step 1: Add deterministic fixtures**

Use fixed workplaces/templates/configs and fixed upcoming trigger timestamps. Do not call `LocalDate.now()` or `System.currentTimeMillis()` inside fixture construction. Required test names/states: Templates Light, Dark, 1.3, multi-workplace; Shift Editor alarm context; Alarms Light/upcoming, Dark, 1.3, permission warning.

- [ ] **Step 2: Run screenshot RED**

```bash
./gradlew --no-daemon :app:validateDebugScreenshotTest
```

Expected: FAIL only for missing M13 references. Any pre-existing screenshot mismatch is a regression and must be fixed before updating references.

- [ ] **Step 3: Generate references**

```bash
./gradlew --no-daemon :app:updateDebugScreenshotTest
```

- [ ] **Step 4: Inspect the actual PNGs**

Review all nine images for clipping, dark-theme outlines, readable shift identity, workplace segmentation, alarm readiness text, upcoming-alarm hierarchy and fontScale 1.3 usability. Do not accept blank/dialog-window harness artifacts.

- [ ] **Step 5: Revalidate**

```bash
./gradlew --no-daemon :app:validateDebugScreenshotTest
git diff --check
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/screenshotTest app/src/screenshotTestDebug/reference
git commit -m "test: qualify M13 Shifts and Alarms visuals"
```

---

### Task 6: Full compatibility gate and durable M13 checkpoint

**Files:**
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify: `docs/project/CURRENT_STATE.md`

**Interfaces:**
- Consumes: all prior M13 commits and qualification evidence.
- Produces: exact recoverable M13 completion boundary; no push/merge/release.

- [ ] **Step 1: Run focused compatibility suite**

```bash
./gradlew --no-daemon :app:testDebugUnitTest \
  --tests '*M13ShiftsAlarmsStructureTest*' \
  --tests '*ShiftFeatureStateTest*' \
  --tests '*TemplatesScreenUiStateReducerTest*' \
  --tests '*ShiftAlarmsTabUiStateReducerTest*' \
  --tests '*ShiftAlarmPlanningTest*' \
  --tests '*AlarmPortsTest*' \
  --tests '*M7AlarmBoundaryStructureTest*' \
  --tests '*AppNavigationStateTest*' \
  --tests '*BackupCompatibilityTest*'
```

- [ ] **Step 2: Run full clean-tree phone gate**

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:validateDebugScreenshotTest
git diff --check
test -z "$(git status --porcelain -uall)"
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

Expected: exit 0 for all commands.

- [ ] **Step 3: Record exact evidence**

Write branch, commit lineage, focused/full job IDs, visual artifact job/PNG verdict and APK SHA-256 into `WORK_PROGRESS.md` and summarize M13 status in `CURRENT_STATE.md`. State explicitly that scheduler, store serialization, Wear contract, Room, backup payload and payroll arithmetic were not changed.

- [ ] **Step 4: Commit checkpoint**

```bash
git add docs/project/WORK_PROGRESS.md docs/project/CURRENT_STATE.md
git commit -m "docs: checkpoint M13 Shifts and Alarms"
```

- [ ] **Step 5: Re-run the final verification on checkpoint HEAD**

Run the same full phone gate once more after the docs-only checkpoint, then verify branch/head/clean tree. Stop at the owner integration boundary; do not push/merge/release without explicit authorization.
