# M8 Focused Redesign A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the approved Variant A / Evolution direction into a reviewable, repository-backed UX contract for Calendar-first navigation, Finance progressive disclosure and grouped More navigation without changing production behavior during M8.

**Architecture:** M8 is intentionally a UX/IA milestone, not a production UI migration. It grounds each proposed screen in the current feature behavior, records the target navigation and screen hierarchy, and defines exact behavior-preservation constraints that M9–M15 must implement. Production Compose changes begin only after the M8 contract is reviewed and accepted.

**Tech Stack:** Android / Jetpack Compose product surface, repository documentation, existing M5 typed navigation model, existing M6 feature-state boundaries, existing M7 UI/data ports.

**Spec:** `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

## Global Constraints

- Canonical baseline is `master@cbb3c1f56c36f7056b5043f8372622adce180691`.
- Calendar remains the default home and must preserve fast visual reading of the schedule.
- Primary IA target is `Calendar / Finance / More`.
- Today remains supported but is not assumed to be a permanent primary tab.
- AI remains available but is optional/experimental in the redesigned IA.
- Shift identity must continue to support text codes, built-in glyphs, emoji and Material icons plus per-template color.
- Multi-workplace assignments must remain visible and semantically intact.
- Finance must retain Summary → Calculation → full Payslip depth without changing payroll arithmetic.
- M8 does not change Room schema, backup semantics, alarm scheduling semantics, payroll rules, Wear contracts or dependencies.
- M8 does not merge/release/deploy without owner authorization.
- Production code changes are forbidden until this M8 UX contract is accepted; implementation work belongs to M9+.

---

### Task 1: Ground the redesign in the current product surface

**Files:**
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/shifts/ShiftTemplateEditorScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkflowServiceScreens.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/notes/AppNotesScreens.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/assistant/AssistantTabScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/appearance/AppearanceSettingsScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppShellComponents.kt`
- Modify: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

**Interfaces:**
- Consumes: existing visible UI behavior and current M5–M7 boundaries.
- Produces: explicit product invariants and redesign constraints used by Tasks 2–6.

- [x] **Step 1:** Inventory Calendar's real information and editing capacity rather than relying on generic mockups.

Evidence to record: month/profile/workplace controls, arbitrary shift visual identity, multi-workplace day segmentation, note/override/holiday state, brush/eraser/pattern/range workflows.

- [x] **Step 2:** Inventory Today, Finance, Notes, Alarms, Assistant and Appearance depth.

Evidence to record: Today configurable bubbles; Finance summary/calculation/payments and detailed payslip; rich notes/media; full alarm subsystem; Assistant action capability; existing appearance axes.

- [x] **Step 3:** Confirm that the existing eight-item bottom bar is structurally over-dense.

Expected evidence: `AppBottomBar` enters ultra-dense layout with eight tabs, reduces icon/label size and abbreviates Russian labels.

- [x] **Step 4:** Write the findings into the M8 spec without changing production code.

- [x] **Step 5:** Commit the grounded M8 direction.

Completed commit: `f4a6ac6808a0a858b9523c5eed694412798f7484`.

---

### Task 2: Lock the primary navigation and destination ownership

**Files:**
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationModels.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationState.kt`
- Read: `docs/project/M5_NAVIGATION_INVENTORY.md`
- Modify: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

**Interfaces:**
- Consumes: current eight-tab typed navigation and existing external/widget routing aliases.
- Produces: M9 target navigation contract: `Calendar / Finance / More`, plus supported non-primary destinations.

- [x] **Step 1:** Set Calendar as normal default landing surface.

- [x] **Step 2:** Set phone primary navigation target to exactly `Calendar / Finance / More` for the focused redesign.

- [x] **Step 3:** Keep Today as a supported configurable overview but not a required permanent primary tab.

- [x] **Step 4:** Move Notes, Alarms, Shift templates, Assistant and global Settings entry under grouped More/contextual flows at the IA level.

- [ ] **Step 5:** Before production implementation, enumerate every existing `EXTRA_OPEN_TAB`/widget/deep-entry behavior and define its compatibility mapping into the new shell. External compatibility must not be guessed during implementation.

Expected mapping document content:

```text
CALENDAR -> Calendar
FINANCE -> Finance Summary or preserved requested Finance sub-tab
PAYROLL -> Finance Calculation
PAYMENTS -> Finance Payments
TODAY -> supported Today route/overview
ASSISTANT -> More > Assistant direct route
NOTES -> More > Notes direct route
ALARMS -> More > Alarms direct route
SHIFTS -> More > Shift templates direct route
SETTINGS -> More > Settings/appropriate global destination
```

- [ ] **Step 6:** Review the mapping against `AppNavigationStateTest.kt` and widgets before M9 production edits.

---

### Task 3: Define the Calendar focused redesign contract

**Files:**
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarInteractionState.kt`
- Read: `app/src/test/java/com/vigilante/shiftsalaryplanner/CalendarInteractionStateTest.kt`
- Modify: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

**Interfaces:**
- Consumes: actual Calendar assignments, template metadata and interaction state.
- Produces: month-view, selected-day and quick-assignment contracts for M9/M11.

- [x] **Step 1:** Define month view as pattern-recognition-first rather than event-list-first.

- [x] **Step 2:** Define ten independent calendar-cell semantic layers: date, color, shift identity, multi-assignment segmentation, today, selection, note, override, holiday/special event and range/preview state.

- [x] **Step 3:** Explicitly forbid replacing shift identity with tiny generic dots.

- [x] **Step 4:** Define selected-day detail ownership for time, workplace, pay estimate, alarm, note and override/special state.

- [x] **Step 5:** Define quick assignment as a first-class brush workflow with active-workplace templates, arbitrary visual identities, eraser, normal mode, cycle/pattern and More.

- [ ] **Step 6:** During the next visual-validation pass, render the same representative month in at least these cases:

```text
single workplace + text codes
single workplace + emoji/material icons
multiple workplaces on the same date
note + override + holiday markers present
active brush/pattern preview state
compact/high-font-scale stress case
```

Acceptance: every case remains understandable without opening day detail.

---

### Task 4: Define Finance progressive disclosure

**Files:**
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt`
- Read: `app/src/test/java/com/vigilante/shiftsalaryplanner/FinanceFeatureStateTest.kt`
- Modify: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

**Interfaces:**
- Consumes: current Finance sub-tabs and payroll result/payslip capabilities.
- Produces: three-depth Finance information hierarchy.

- [x] **Step 1:** Define Summary to answer `How much money?` first.

- [x] **Step 2:** Preserve gross, tax, net, advance/salary or per-shift payout, worked shifts/hours, next payment and fact-vs-plan data without showing every accounting line at once.

- [x] **Step 3:** Define Calculation as the explanation layer preserving period/workplace/gross-net/compact-detailed controls.

- [x] **Step 4:** Define full Payslip as the intentionally dense payroll-document layer with accrual/deduction/base/rate/hours/coefficient/amount detail where the current data supports it.

- [x] **Step 5:** Explicitly prohibit payroll arithmetic changes for visual convenience.

- [ ] **Step 6:** In M12 implementation, protect all amount-producing paths with the existing payroll regression suite before any view-model/UI transformation.

---

### Task 5: Define More taxonomy and contextual settings

**Files:**
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/SettingsTabScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/appearance/AppearanceSettingsScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsTabScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/notes/AppNotesScreens.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/assistant/AssistantTabScreen.kt`
- Modify: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

**Interfaces:**
- Consumes: all former secondary root tabs and settings/service screens.
- Produces: grouped More root and contextual-settings ownership rules.

- [x] **Step 1:** Group More into `Work`, `Tools`, `App`, `Data`, and advanced/service destinations.

- [x] **Step 2:** Put Workplaces and Shift templates under Work.

- [x] **Step 3:** Put Alarms, Notes and Assistant under Tools while keeping direct deep-entry possible.

- [x] **Step 4:** Put Appearance, Widgets and Wear under App.

- [x] **Step 5:** Put Backup/Restore, Drive and Import/Export under Data.

- [x] **Step 6:** Define contextual settings policy: settings that belong to a workplace, shift, date/note, alarm or finance/report surface should be discoverable from that entity instead of depending on a giant global Settings list.

- [ ] **Step 7:** Before M15 implementation, inventory every current Settings destination and classify it as contextual, app-global, data/service or obsolete-by-duplication. No setting is deleted merely because the new taxonomy has fewer root rows.

---

### Task 6: Validate Today and AI positioning

**Files:**
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkflowServiceScreens.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/assistant/AssistantTabScreen.kt`
- Modify: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

**Interfaces:**
- Consumes: existing Today layout customization and Assistant action surface.
- Produces: explicit non-primary roles that M10/M14 can implement without reopening top-level IA.

- [x] **Step 1:** Preserve Today's configurable blocks as product capability.

- [x] **Step 2:** Declare Calendar the default home; Today complements rather than replaces Calendar.

- [x] **Step 3:** Require usage evidence before promoting Today to an optional fourth persistent primary destination.

- [x] **Step 4:** Keep Assistant available but remove the assumption that it deserves a permanent primary slot.

- [x] **Step 5:** Treat provider cost/free-tier assumptions as outside M8; AI value is judged by useful workflows, not model availability marketing.

- [ ] **Step 6:** For M14, evaluate at minimum these Assistant value scenarios before deciding long-term prominence:

```text
assign a repeating/complex shift pattern by natural language
explain a finance difference using deterministic app data
answer upcoming-shift queries
create/update shift alarm and note actions safely
```

Acceptance: retain/promote AI only where it materially improves a workflow over deterministic UI/actions.

---

### Task 7: M8 review gate and M9 handoff

**Files:**
- Modify: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`
- Modify: `docs/project/CURRENT_STATE.md`
- Modify: `docs/project/WORK_PROGRESS.md`
- Create: `docs/superpowers/plans/2026-09-12-m9-design-system.md` only after M8 review is accepted.

**Interfaces:**
- Consumes: completed Tasks 1–6 and the focused screen review set.
- Produces: M8 COMPLETE evidence and an implementation-ready M9 design-system plan.

- [ ] **Step 1:** Review the focused screen set against the real application behavior, not only against concept renders.

Required screens/states:

```text
Calendar month normal
Calendar selected-day detail
Calendar quick assignment / brush
Calendar multi-workplace representative day
Finance Summary
Finance Payments / fact-vs-plan
Finance Calculation entry
Finance full Payslip
More grouped root
Workplace contextual detail/settings
Appearance reorganized surface
Today overview + layout editor
```

- [ ] **Step 2:** Perform an accessibility stress review for font scaling, high contrast, non-color shift recognition and touch targets.

- [ ] **Step 3:** Verify that no accepted UX decision requires changing payroll/Room/backup/alarm semantics.

- [ ] **Step 4:** Update `CURRENT_STATE.md` to `M8 COMPLETE` only after owner review of the focused redesign set.

- [ ] **Step 5:** Append final M8 evidence and exact next operation to `WORK_PROGRESS.md`.

- [ ] **Step 6:** Write M9 Design System plan from the accepted M8 contract, covering typography, spacing, surfaces, shapes, calendar-cell components, navigation chrome, money values, sheets/dialogs, accessibility and mapping of existing appearance customization.

## Self-review

### Spec coverage

- Calendar-first: covered by Tasks 2–3.
- Three-item primary shell: Task 2.
- Today supported/non-primary: Task 6.
- AI optional: Task 6.
- Calendar information capacity and arbitrary visual identity: Task 3.
- Finance Summary → Calculation → Payslip: Task 4.
- More/settings taxonomy: Task 5.
- Variant A / Evolution visual direction: spec plus Task 7 review set.
- Accessibility: Task 7.
- No production behavior change during M8: Global Constraints and Task 7.

### Placeholder scan

No TBD/TODO/"implement later" placeholders are used as task requirements. Deferred work is assigned to explicit later milestone tasks with concrete acceptance boundaries.

### Type/interface consistency

M8 does not introduce production types. Existing names referenced in this plan are repository names or documented IA destinations. Production type changes are intentionally deferred until the accepted M9 implementation plan.
