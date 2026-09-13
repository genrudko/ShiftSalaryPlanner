# Semantic Iconography + M13 Declutter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace application-menu glyph placeholders with semantic icons and recompose M13 Alarms into a light operational root with Settings and System Status drill-downs while preserving all behavior.

**Architecture:** Add a centralized UI semantic-icon catalog and make More/navigation consume semantic keys. Keep the existing M13 alarm state/actions and callbacks, adding only local presentation-page state so the current general/system controls move from the root into internal drill-down composables.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 / Material Rounded icons, Preview Screenshot Testing, JUnit, existing Evolution components.

**Spec:** `docs/superpowers/specs/2026-09-13-app-iconography-m13-declutter-design.md`

## Global Constraints

- No new icon dependency or custom SVG pack.
- User-owned shift `iconKey` / emoji / glyph / template color remain unchanged.
- No Room, backup payload, payroll arithmetic, alarm scheduler, `ShiftAlarmStore`, Wear protocol or persistence-format changes.
- Existing direct `ALARMS` entry opens the operational root.
- All current M13 capabilities remain reachable.

---

### Task 1: App semantic icon catalog and M12 More migration

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/AppSemanticIcons.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/MoreTab.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationModels.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/AppSemanticIconsStructureTest.kt`

**Interfaces:**
- Produces: `enum class AppSemanticIconKey` and `fun appSemanticIcon(key: AppSemanticIconKey): ImageVector`.
- More destinations consume `AppSemanticIconKey` and render `icon = appSemanticIcon(destination.iconKey)`.

- [ ] **Step 1:** Write RED structure tests for the catalog, `MoreDestination.iconKey`, and removal of interface glyphs.
- [ ] **Step 2:** Run `./gradlew --no-daemon :app:testDebugUnitTest --tests '*AppSemanticIconsStructureTest*'`; verify RED on missing catalog / old glyph field.
- [ ] **Step 3:** Implement the catalog and migrate More/navigation without callback or grouping changes.
- [ ] **Step 4:** Run icon test + `*M12MoreWorkplacesStructureTest*` + `*AppNavigationStateTest*` + `git diff --check`; require GREEN.
- [ ] **Step 5:** Commit `feat: add semantic app iconography`.

### Task 2: M13 presentation-page state and declutter root

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13AlarmDeclutterStructureTest.kt`

**Interfaces:**
- Produces local `AlarmScreenPage` values `ROOT`, `SETTINGS`, `SYSTEM_STATUS` and root action rows that change page.
- Consumes existing alarm state/actions and permission callbacks unchanged.

- [ ] **Step 1:** Write RED tests for root order and absence of inline general/system sections.
- [ ] **Step 2:** Run focused test; verify RED against current root.
- [ ] **Step 3:** Implement local page state/back handling and shorter ROOT, showing full readiness on root only when not ready.
- [ ] **Step 4:** Run declutter + M13 structure + alarms reducer + planning tests + `git diff --check`; require GREEN.
- [ ] **Step 5:** Commit `feat: declutter M13 alarms root`.

### Task 3: Alarm Settings drill-down

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13AlarmDeclutterStructureTest.kt`

**Interfaces:**
- Produces private `AlarmSettingsScreen(...)` reusing current Wear, horizon, ring appearance and behavior state/actions.

- [ ] **Step 1:** Extend RED test requiring Wear mirror/sound, horizon, ring appearance, alarm behavior and back-to-root.
- [ ] **Step 2:** Run focused test; verify RED.
- [ ] **Step 3:** Move existing Wear/horizon/ring appearance/behavior UI into `AlarmSettingsScreen` without changing parsing limits or dispatch actions.
- [ ] **Step 4:** Run structure + reducer + runtime/planning tests + `git diff --check`; require GREEN.
- [ ] **Step 5:** Commit `feat: add M13 alarm settings screen`.

### Task 4: System Status drill-down

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13AlarmDeclutterStructureTest.kt`

**Interfaces:**
- Produces private `AlarmSystemStatusScreen(...)` with all three readiness states/actions and optional last-reschedule feedback.

- [ ] **Step 1:** Extend RED test for notification/exact/full-screen status/actions and last result.
- [ ] **Step 2:** Run focused test; verify RED.
- [ ] **Step 3:** Move current service-information/permission UI into the system page; root keeps concise status only.
- [ ] **Step 4:** Run M13 structure + ports + M7 boundary + planning/runtime + backup compatibility + `git diff --check`; require GREEN.
- [ ] **Step 5:** Commit `feat: add M13 alarm system status screen`.

### Task 5: Deterministic visual matrix and real PNG review

**Files:**
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesVisualScreenshotTest.kt`
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsVisualScreenshotTest.kt`
- Modify matching screenshot references.

- [ ] **Step 1:** Add/adjust deterministic previews for M12 More Light/Dark/1.3 and M13 ROOT Light/Dark/1.3/warning, Settings Light/1.3, System Status ready/warning.
- [ ] **Step 2:** Run `:app:validateDebugScreenshotTest`; require only intentional M12/M13 changed/missing refs.
- [ ] **Step 3:** Run `:app:updateDebugScreenshotTest`, export actual PNGs as immutable Bridge artifacts and inspect before acceptance.
- [ ] **Step 4:** Repair visible clipping/density/icon ambiguity via RED→GREEN tests and repeat visual review as needed.
- [ ] **Step 5:** Run `:app:validateDebugScreenshotTest && git diff --check`; commit `test: qualify semantic icons and M13 declutter visuals` only after GREEN.

### Task 6: Compatibility gate and durable checkpoint

**Files:**
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify: `docs/project/CURRENT_STATE.md`

- [ ] **Step 1:** Run focused compatibility suite covering icon structure, M12, M13 declutter/structure, navigation, template state, alarm reducers/planning/ports/M7 boundary and backup compatibility.
- [ ] **Step 2:** Run full clean-tree phone gate: unit + assemble + lint + screenshot validate + diff/clean assertion + APK SHA-256.
- [ ] **Step 3:** Record branch/head, lineage, job IDs, real PNG verdict and invariants in durable docs.
- [ ] **Step 4:** Commit `docs: checkpoint semantic iconography and M13 declutter`.
- [ ] **Step 5:** Re-run full phone gate on checkpoint HEAD and stop at owner integration boundary.
