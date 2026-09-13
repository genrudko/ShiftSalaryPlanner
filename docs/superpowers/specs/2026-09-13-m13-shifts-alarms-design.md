# M13 — Shifts & Alarms Design

Date: 2026-09-13
Branch: `feature/m13-shifts-alarms`
Canonical base: `master@f10baa8685c62aa9b2b2782865ab16c1c42ba9f3`
Status: owner-approved design; implementation authorized

## Goal

Migrate the existing Shift templates/editor and shift-alarm flows to the accepted Variant A / Evolution visual system without changing the product model already shipped in `v6.4`.

The shipped model is the M13 architecture: **Shifts and Alarms remain separate first-class destinations, but a shift template owns its shift-time/alarm context.** M13 improves hierarchy, discoverability and visual consistency; it does not invent a new alarm subsystem.

## Product decision

Keep the existing Variant A relationship:

- `More > Shift templates` is workplace-centric configuration for shift identity, schedule facts, payroll-related shift fields, special-day behavior, cycles/statuses and the shift's start/end time used by alarm planning.
- `More > Alarms` is the operational alarm center for upcoming alarms, Android capability/permission state, template-level alarm enablement/details, global ring behavior and Wear mirroring.
- A shift editor may summarize the alarm state that belongs to that template and retain start/end time editing, but does not duplicate the complete global alarm center.
- Contextual navigation may connect the two surfaces; the independent legacy/direct `SHIFTS` and `ALARMS` routes remain valid.

## Existing contracts that remain authoritative

M13 must not change these semantics for visual convenience:

- `AlarmDataPort` persistence behavior or `ShiftAlarmStore` serialized preference format.
- `AlarmPlatformPort`, `ShiftAlarmScheduler`, exact/inexact scheduling fallback, suppression keys, reschedule horizon, reboot/system reschedule behavior or snooze semantics.
- `WearSyncContract` paths/payload contract, Wear ring/stop/snooze actions or template-alarm toggle semantics.
- workplace-scoped shift codes and the existing `shiftCode` binding between templates and alarm configs.
- Room schema/migrations, backup payload compatibility, payroll formulas/legislation or dependency graph.
- valid legacy `BottomTab.name` direct-entry values, especially `SHIFTS` and `ALARMS`.

## Surface 1 — Shift templates

The current capability set stays intact: workplace switcher, shifts, cycles/patterns, system statuses, add/edit/duplicate/delete/reorder and manage-workplaces entry.

Evolution hierarchy:

1. Compact header: title + add action.
2. Workplace context becomes the strongest scope indicator.
3. A compact summary communicates shift/cycle/status counts without competing with content.
4. `Shifts / Cycles` uses the common Evolution tab grammar.
5. Regular templates use low-noise Evolution panel rows; code/icon/color and time remain scan-readable without relying on color alone.
6. System statuses remain a distinct secondary route, not mixed into the regular shift list.
7. Destructive swipe/delete affordances keep their current guarded behavior.

The list must remain usable with multiple workplace-scoped template codes and at increased font scale.

## Surface 2 — Shift template editor

No field is silently removed. The editor is reorganized into progressive sections:

1. **Identity** — workplace, title, code, icon/emoji, color, active state.
2. **Schedule** — start/end time, duration, break, night hours. Start/end remain persisted through `ShiftTemplateAlarmConfig` exactly as today.
3. **Pay / special day** — existing shift-pay and special-day fields with unchanged domain calculations.
4. **Alarm context** — concise template alarm status derived from the existing `ShiftTemplateAlarmConfig` (enabled/disabled and active alarm count). It explains that detailed signals live in `Alarms`; it does not create a second copy of the global behavior editor.
5. Save/delete and unsaved-exit semantics remain unchanged.

Protected/system template restrictions remain unchanged.

## Surface 3 — Alarms operational center

Reorder the current content by operational importance without changing its state model:

1. **Upcoming alarms hero** — next scheduled alarms and open-all/cancel actions first.
2. **System readiness** — alarm master state plus missing notification/exact/full-screen capability actions are visible before decorative configuration. Permission state must never be represented by color only.
3. **Shift alarm templates** — template cards remain driven by `ShiftAlarmsTabUiState.templateConfigs`; template enable/disable, alarm editing, duplicate/delete behavior and auto-save remain unchanged.
4. **Global behavior** — auto-reschedule, horizon, sound/vibration/snooze/ring duration and manual reschedule/test actions become a lower-priority settings group.
5. **Wear** — mirroring and Wear sound mode remain in the alarm domain because they control alarm delivery.
6. **Ring appearance** — existing ring visual/animation/action configuration remains available, but is visually secondary to readiness and actual scheduled alarms.

The existing `AllUpcomingShiftAlarmsScreen`, alarm edit dialog, ring-appearance dialog and behavior dialog remain feature-complete.

## Visual language

Use the already accepted Evolution primitives where they fit:

- `EvolutionSurface` for grouped content and stateful panels;
- `EvolutionHeroCard` for the next-alarm operational summary when composition fits;
- `EvolutionActionRow` for navigable/configuration rows;
- `EvolutionTextTabs` for the Shifts/Cycles mode switch;
- existing semantic shift identity rendering for code/icon/emoji/color;
- existing adaptive `appScreenPadding`, `appBlockSpacing`, `appScaledSpacing` and font-scale behavior.

Avoid bright universal outlines, unnecessary nested cards, or making every setting equally prominent.

## Accessibility and compatibility acceptance

- shift code/icon remains readable without color;
- active/inactive alarm and missing-permission states have textual semantics;
- Light, Dark and fontScale 1.3 have no clipped primary identity or unusable action;
- multi-workplace template identity is not flattened;
- direct `SHIFTS` and `ALARMS` entries still reach their intended screens;
- no alarm scheduler, Wear or backup contract change is required to satisfy the UI.

## Deterministic visual qualification

Create Compose Preview Screenshot Testing fixtures for at least:

- Templates Light;
- Templates Dark;
- Templates fontScale 1.3;
- Templates multi-workplace;
- Shift editor with an existing template/alarm context;
- Alarms Light with upcoming alarms;
- Alarms Dark;
- Alarms fontScale 1.3;
- Alarms permission/readiness warning.

References are accepted only after real PNG inspection. A screenshot harness failure is not a visual pass.

## Regression gates

Focused M13 gates must include:

- `ShiftFeatureStateTest`;
- `TemplatesScreenUiStateReducerTest`;
- `ShiftAlarmsTabUiStateReducerTest`;
- `ShiftAlarmPlanningTest`;
- `AlarmPortsTest`;
- `M7AlarmBoundaryStructureTest`;
- `AppNavigationStateTest` / M12 structure compatibility where relevant;
- `BackupCompatibilityTest`;
- new M13 structure tests;
- new M13 screenshot tests.

Final gate: `testDebugUnitTest + assembleDebug + lintDebug + validateDebugScreenshotTest + git diff --check` on a clean tree.

## Non-goals

M13 does not redesign the ringing Activity/service itself beyond any strictly shared visual primitive required by the migrated settings flow; does not add new alarm algorithms, alarm types, recurrence models, workplace CRUD, Room migrations or Wear protocol fields; and does not change payroll arithmetic.
