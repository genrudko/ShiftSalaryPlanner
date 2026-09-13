# M12 More / Workplaces / Contextual Settings Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the eight-item permanent phone navigation with the accepted three-destination `Calendar / Finance / More` shell, group More by the M8 taxonomy, and make existing workplaces first-class navigable entities with contextual payroll access without changing persistence or business semantics.

**Architecture:** Keep the existing `BottomTab` enum as the external/deep-entry compatibility contract, but introduce an explicit three-item `primaryBottomTabs` presentation list. `BottomTab.SETTINGS` becomes the visible More root while legacy `TODAY/ASSISTANT/NOTES/ALARMS/SHIFTS` remain typed direct routes. Add a focused `MoreTab` and a fullscreen `WorkplacesScreen`; both consume existing callbacks/state and use Evolution surfaces. The existing workplace store, rename dialog, payroll settings state and hidden feature tabs remain authoritative.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing Evolution Compose primitives, JUnit source/behavior tests, Android Compose Preview Screenshot Testing, Gradle.

**Spec:** `docs/project/M8_FOCUSED_REDESIGN_REVIEW.md` and `docs/project/VNEXT_MODERNIZATION.md` M12 section.

## Global Constraints

- Visible primary navigation is exactly Calendar / Finance / More (`Ещё` in the Russian UI).
- Preserve all existing `BottomTab.name` values and the `PAYROLL` / `PAYMENTS` aliases as valid direct-entry routes.
- Do not add/remove/rename persisted workplace IDs or change `WorkAssignmentsStore` serialization; M12 surfaces the existing workplace model only.
- Existing workplace rename and workplace-specific payroll persistence remain the source of truth.
- Do not change payroll formulas/legislation, Room schema/migrations, backup payload compatibility, alarm scheduling semantics, Wear sync contract or dependency graph.
- Do not invent a standalone Wear settings backend. The More `Wear OS` entry routes to the existing Alarms surface where Wear mirror/sound controls already live; M13 owns alarm/Wear UI migration.
- Old capabilities remain reachable. M15 still owns final global Settings inventory/deletion/rationalization.
- Use Evolution surfaces for new M12 root/detail UI and preserve Light/Dark/fontScale 1.3 readability.

---

### Task 1: Define M12 structural and navigation safety net

**Files:**
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesStructureTest.kt`
- Modify: `app/src/test/java/com/vigilante/shiftsalaryplanner/AppNavigationStateTest.kt`

**Interfaces:**
- Consumes: existing `BottomTab`, `AppNavigationState`, source files for shell/settings.
- Produces: RED contracts for three visible primary destinations, compatibility routing, grouped More taxonomy and first-class Workplaces screen.

- [ ] **Step 1: Add behavior assertions for primary mapping while preserving direct routes**

Add tests expecting these exact semantics:

```kotlin
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
```

Keep the existing `every direct bottom tab name remains a valid external route` test unchanged.

- [ ] **Step 2: Add source-structure assertions**

Create `M12MoreWorkplacesStructureTest` asserting:

```kotlin
assertTrue(shell.contains("primaryBottomTabs.forEach"))
assertFalse(shell.contains("BottomTab.entries.forEach"))
assertTrue(models.contains("SETTINGS(\"Ещё\""))
assertTrue(more.contains("Work"))
assertTrue(more.contains("Tools"))
assertTrue(more.contains("App"))
assertTrue(more.contains("Data"))
assertTrue(more.contains("Advanced"))
assertTrue(more.contains("EvolutionSurface("))
assertTrue(workplaces.contains("fun WorkplacesScreen("))
assertTrue(workplaces.contains("onOpenPayrollSettings: (String) -> Unit"))
assertTrue(navigation.contains("WORKPLACES"))
```

Also assert no new storage/domain references are introduced in `MoreTab.kt` or `WorkplacesScreen.kt`.

- [ ] **Step 3: Run RED**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*AppNavigationStateTest' \
  --tests '*M12MoreWorkplacesStructureTest' --no-daemon
```

Expected: M12 tests fail because `primaryBottomTabs`, `primaryTabFor`, `MoreTab`, `WorkplacesScreen` and `AppScreen.WORKPLACES` do not exist yet.

- [ ] **Step 4: Commit the safety net**

```bash
git add app/src/test/java/com/vigilante/shiftsalaryplanner/AppNavigationStateTest.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesStructureTest.kt
git commit -m "test: define M12 More and Workplaces structure"
```

---

### Task 2: Reduce the visible shell to Calendar / Finance / More

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationModels.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppShellComponents.kt`
- Test: `AppNavigationStateTest.kt`, `M12MoreWorkplacesStructureTest.kt`

**Interfaces:**
- Consumes: all eight legacy `BottomTab` values unchanged.
- Produces: `primaryBottomTabs: List<BottomTab>` and `primaryTabFor(BottomTab): BottomTab` used by phone bottom bar and landscape rail.

- [ ] **Step 1: Add primary presentation mapping without altering enum names**

```kotlin
val primaryBottomTabs = listOf(
    BottomTab.CALENDAR,
    BottomTab.FINANCE,
    BottomTab.SETTINGS
)

fun primaryTabFor(tab: BottomTab): BottomTab = when (tab) {
    BottomTab.CALENDAR, BottomTab.TODAY -> BottomTab.CALENDAR
    BottomTab.FINANCE -> BottomTab.FINANCE
    else -> BottomTab.SETTINGS
}
```

Change only the `SETTINGS` presentation label/icon to More semantics (`"Ещё"`, rounded More icon); keep the enum constant `SETTINGS` intact.

- [ ] **Step 2: Render only `primaryBottomTabs` in bottom bar and rail**

Use `primaryTabFor(selectedTab)` for selected state so hidden direct routes still highlight their logical primary destination. Remove density decisions based on all eight enum entries; size the three-item shell as normal navigation.

- [ ] **Step 3: Run GREEN**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*AppNavigationStateTest' \
  --tests '*M12MoreWorkplacesStructureTest' --no-daemon
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationModels.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppShellComponents.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/AppNavigationStateTest.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesStructureTest.kt
git commit -m "feat: reduce M12 primary navigation"
```

---

### Task 3: Add grouped More root without deleting legacy capabilities

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/MoreTab.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `M12MoreWorkplacesStructureTest.kt`

**Interfaces:**
- Consumes: existing navigation callbacks/screens and `activeProfileName`, `appearanceSettingsSummary`.
- Produces: a More root grouped as Work / Tools / App / Data / Advanced.

- [ ] **Step 1: Implement an Evolution More root**

`MoreTab` receives callbacks only; it owns no data stores. Required groups and routes:

```text
Work      -> Workplaces, Shift templates
Tools     -> Alarms, Notes, Assistant
App       -> Appearance, Widgets, Wear OS, Profiles, Quick Start
Data      -> Backup / Restore, Google Drive, Import, Report Center
Advanced  -> Health Check, Event Log, Current Parameters
```

Use `EvolutionSurface(role = EvolutionSurfaceRole.SOFT)` for group containers and reusable clickable rows. `Wear OS` invokes the existing Alarms route and explains that mirror/sound settings live there. `Google Drive` invokes the existing Backup/Restore screen where cloud sync already exists.

- [ ] **Step 2: Wire hidden legacy feature tabs from More**

In the `BottomTab.SETTINGS` branch of `MainActivity`, replace `SettingsTab(...)` with `MoreTab(...)`. Direct callbacks for Alarms/Notes/Assistant/Shifts call `navigationState.selectTab(BottomTab.X)`. Fullscreen settings callbacks keep using existing `AppScreen` values.

- [ ] **Step 3: Preserve Finance-owned settings outside More**

Do not duplicate payroll amount configuration, deductions, additional payments or report-visibility controls as new More-owned state. Existing Finance routes remain the canonical contextual access; Workplaces will add a workplace-specific payroll link in Task 4.

- [ ] **Step 4: Run targeted GREEN and navigation regressions**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*M12MoreWorkplacesStructureTest' \
  --tests '*AppNavigationStateTest' \
  --tests '*M7NotesSettingsBoundaryStructureTest' \
  --tests '*SettingsFeatureStateTest' --no-daemon
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/MoreTab.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesStructureTest.kt
git commit -m "feat: add grouped M12 More root"
```

---

### Task 4: Make Workplaces a first-class contextual screen

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkplacesScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationState.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Keep and reuse: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkplacesRenameDialog.kt`
- Test: `AppNavigationStateTest.kt`, `M12MoreWorkplacesStructureTest.kt`, `FinanceFeatureStateTest.kt`, `ScheduleDataPortTest.kt`

**Interfaces:**
- Consumes: `List<Workplace>`, `activeWorkplaceId`, existing rename callback and `financeFeatureState.openSettingsFor(workplaceId)`.
- Produces: `AppScreen.WORKPLACES` and `WorkplacesScreen(...)` with selection, rename and contextual payroll actions.

- [ ] **Step 1: Add `AppScreen.WORKPLACES` and saver regression**

Extend the existing navigation enum only. Add a test restoring a stack containing `WORKPLACES`; no persistence format change is needed because the saver stores enum names as strings.

- [ ] **Step 2: Implement `WorkplacesScreen`**

Signature:

```kotlin
@Composable
fun WorkplacesScreen(
    workplaces: List<Workplace>,
    activeWorkplaceId: String,
    onBack: () -> Unit,
    onSelectWorkplace: (String) -> Unit,
    onRenameWorkplaces: () -> Unit,
    onOpenPayrollSettings: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

Render one Evolution card per existing workplace. Show the active workplace textually, not by color alone. Each card exposes `Выбрать` when inactive and `Расчёт` for contextual payroll settings. A top action opens the existing rename dialog. Do not add create/delete/reorder because the current persisted model does not support those semantics.

- [ ] **Step 3: Wire More and existing manage-workplace entry points**

More > Workplaces opens `AppScreen.WORKPLACES`. The fullscreen overlay uses the existing `workplaces` and `activeWorkplaceId`. `onOpenPayrollSettings(id)` calls `financeFeatureState.openSettingsFor(id)` and opens `PAYROLL_SETTINGS`. Calendar/Templates manage-workplace actions may now open `WORKPLACES` instead of jumping directly to rename, while the rename dialog remains reachable from the new screen.

- [ ] **Step 4: Run focused regressions**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*AppNavigationStateTest' \
  --tests '*M12MoreWorkplacesStructureTest' \
  --tests '*FinanceFeatureStateTest' \
  --tests '*ScheduleDataPortTest' --no-daemon
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkplacesScreen.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationState.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/AppNavigationStateTest.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesStructureTest.kt
git commit -m "feat: add M12 Workplaces context"
```

---

### Task 5: Add deterministic M12 visual qualification

**Files:**
- Create: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesVisualScreenshotTest.kt`
- Add/update references under: `app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/`

**Interfaces:**
- Consumes: real `MoreTab` and `WorkplacesScreen` composables.
- Produces: deterministic More Light/Dark/fontScale 1.3 and Workplaces contextual references.

- [ ] **Step 1: Add real preview fixtures**

Create four previews:
- More Light, 412×1000
- More Dark, 412×1000
- More fontScale 1.3, 412×1200
- Workplaces, 412×900 with three workplaces and the second one active

Callbacks are no-ops; fixtures use production composables rather than copied mock layouts.

- [ ] **Step 2: Require visual RED before references**

```bash
source scripts/android/vps-env.sh
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

Expected: only intentional M12 missing/changed references fail.

- [ ] **Step 3: Inspect rendered PNGs before accepting refs**

Reject if the five More groups are hard to scan, the three-item primary concept is visually ambiguous, Workplaces does not show active state textually, action labels clip, or Dark/1.3 restores bright universal outlines/clipping.

- [ ] **Step 4: Update and revalidate only after review**

```bash
source scripts/android/vps-env.sh
./gradlew :app:updateDebugScreenshotTest --no-daemon
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

- [ ] **Step 5: Commit**

```bash
git add app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M12MoreWorkplacesVisualScreenshotTest.kt \
        app/src/screenshotTestDebug/reference
git commit -m "test: qualify M12 More and Workplaces visuals"
```

---

### Task 6: M12 compatibility qualification and durable checkpoint

**Files:**
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify after qualification: `docs/project/CURRENT_STATE.md`

**Interfaces:**
- Consumes: Tasks 1–5 committed tree.
- Produces: M12 `COMPLETE / VERIFIED locally (integration pending)` checkpoint.

- [ ] **Step 1: Run focused shell/settings compatibility gate from a clean tree**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*AppNavigationStateTest' \
  --tests '*M12MoreWorkplacesStructureTest' \
  --tests '*SettingsFeatureStateTest' \
  --tests '*FinanceFeatureStateTest' \
  --tests '*ScheduleDataPortTest' \
  --tests '*M7NotesSettingsBoundaryStructureTest' \
  --tests '*BackupCompatibilityTest' --no-daemon
```

- [ ] **Step 2: Run unchanged-tree full phone qualification**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:validateDebugScreenshotTest --no-daemon
git diff --check
test -z "$(git status --porcelain -uall)"
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Record exact commits, job IDs, visual findings and no-domain-change statement**

`WORK_PROGRESS.md` and `CURRENT_STATE.md` must state that legacy direct `BottomTab` names remain parseable, visible primary navigation is now three items, existing Workplace storage semantics are unchanged, and M13 is the next roadmap milestone.

- [ ] **Step 4: Commit checkpoint**

```bash
git add docs/project/WORK_PROGRESS.md docs/project/CURRENT_STATE.md
git commit -m "docs: checkpoint M12 More Workplaces shell"
```

Do not push, merge, release or deploy without a new owner gate.
