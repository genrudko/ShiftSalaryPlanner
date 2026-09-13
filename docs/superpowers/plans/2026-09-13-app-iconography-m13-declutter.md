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

### Task 2: M13 operational root + Settings + System Status drill-downs

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M13AlarmDeclutterStructureTest.kt`

**Interfaces:**
- Produces local `AlarmScreenPage` values `ROOT`, `SETTINGS`, `SYSTEM_STATUS`.
- Produces internal `AlarmSettingsScreen(...)` and `AlarmSystemStatusScreen(...)` using the same existing UI state/actions.
- Direct external `ALARMS` entry remains ROOT; back from either drill-down returns ROOT.

- [ ] **Step 1:** Write RED tests for page model, root order, conditional readiness, quick actions, two semantic drill-down entries, complete Settings capability set and complete System Status permission/feedback set.
- [ ] **Step 2:** Run focused test and verify RED on the current single long root.
- [ ] **Step 3:** Implement page state/back handling; move Wear/horizon/ring appearance/behavior into Settings and permission/readiness/last-result UI into System Status; preserve all existing callbacks/parsing limits.
- [ ] **Step 4:** Run declutter + M13 structure + reducer + planning/runtime + ports + M7 boundary + backup compatibility tests and `git diff --check`; require GREEN.
- [ ] **Step 5:** Commit `feat: recompose M13 alarm navigation`.

### Task 3: Deterministic visual matrix and real PNG review

**Files:**
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesVisualScreenshotTest.kt`
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M13ShiftsAlarmsVisualScreenshotTest.kt`
- Modify matching screenshot references.

- [ ] **Step 1:** Add/adjust deterministic previews for M12 More Light/Dark/1.3 and M13 ROOT Light/Dark/1.3/warning, Settings Light/1.3, System Status ready/warning.
- [ ] **Step 2:** Run `:app:validateDebugScreenshotTest`; require only intentional M12/M13 changed/missing refs.
- [ ] **Step 3:** Run `:app:updateDebugScreenshotTest`, export actual PNGs as immutable Bridge artifacts and inspect before acceptance.
- [ ] **Step 4:** Repair visible clipping/density/icon ambiguity via RED→GREEN tests and repeat visual review as needed.
- [ ] **Step 5:** Run `:app:validateDebugScreenshotTest && git diff --check`; commit `test: qualify semantic icons and M13 declutter visuals` only after GREEN.

### Task 4: Compatibility gate and durable checkpoint

**Files:**
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify: `docs/project/CURRENT_STATE.md`

- [ ] **Step 1:** Run focused compatibility suite covering icon structure, M12, M13 declutter/structure, navigation, template state, alarm reducers/planning/ports/M7 boundary and backup compatibility.
- [ ] **Step 2:** Run full clean-tree phone gate: unit + assemble + lint + screenshot validate + diff/clean assertion + APK SHA-256.
- [ ] **Step 3:** Record branch/head, lineage, job IDs, real PNG verdict and invariants in durable docs.
- [ ] **Step 4:** Commit `docs: checkpoint semantic iconography and M13 declutter`.
- [ ] **Step 5:** Re-run full phone gate on checkpoint HEAD and stop at owner integration boundary.
