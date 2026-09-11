# M4–M7 Refactor Design

## Status

Approved architectural direction from the owner on 2026-09-11. This document turns that direction into an explicit implementation boundary before code changes begin.

## Goal

Refactor ShiftSalaryPlanner incrementally so the application shell, navigation, feature UI state, and persistence/service access are separated into clear boundaries **without changing user-visible workflows or redesigning the UI before M8**.

The recovered application remains the behavioral reference. M3 is the regression safety net for this work.

## Why this is the starting point

`MainActivity.kt` is currently about 4,959 lines / 244 KB. The Android `MainActivity` entry point itself is small; the real concentration of responsibility is `ShiftSalaryApp`, which currently owns at the same time:

- roughly 75 `rememberSaveable` values and additional transient Compose state;
- root tab and fullscreen-screen navigation;
- Android back handling;
- profile selection;
- construction of many settings stores;
- Room database and DAO construction;
- Google Drive / sign-in client construction;
- Excel import services;
- alarm-related integration;
- report/backup/import orchestration;
- rendering of the application UI.

Moving that function to a different file without changing ownership would only hide the problem. The refactor therefore follows responsibility boundaries, not file-size cosmetics.

## Non-goals for M4–M7

The following are explicitly out of scope until M8 or a separately approved task:

- visual redesign;
- changing the bottom-tab information architecture;
- renaming/rewording user-facing flows for style reasons;
- changing payroll semantics;
- changing Room schema or backup format;
- dependency modernization unrelated to the refactor;
- release/deploy work;
- replacing the whole application with a rewrite;
- adding a DI framework such as Hilt/Koin merely to perform extraction;
- adding Navigation Compose merely because navigation is being cleaned up.

Small code changes needed to preserve behavior, test extracted boundaries, or fix a proven regression are allowed. They must remain behavior-preserving.

## Architecture strategy

Use an incremental strangler-style extraction around the existing application. Each milestone leaves a buildable, testable app and becomes the base for the next milestone.

The sequence is deliberately:

1. **M4 — App Shell Extraction**: move construction/orchestration out of the giant UI root.
2. **M5 — Navigation Rewrite**: replace boolean-driven fullscreen navigation with typed navigation state.
3. **M6 — Feature State Ownership**: group feature-specific Compose state behind focused state holders/controllers.
4. **M7 — UI/Data Boundary**: stop presentation code from depending directly on concrete stores/DAOs/services.
5. **M8 — UX/Redesign**: only after M4–M7 are complete.

This order keeps the visual product stable while reducing the cost and risk of the later redesign.

---

# M4 — App Shell Extraction

## Intent

Make `MainActivity` a small Android entry point and make dependency construction explicit. Do **not** redesign screens or rewrite navigation yet.

## Target responsibilities

### `MainActivity`

Own only Android-entry concerns:

- `enableEdgeToEdge()`;
- reading/removing initial intent extras;
- `setContent`;
- invoking one application-root composable.

It must no longer construct application stores, Room objects, importers, repositories, Google clients, or application-level feature services.

### `ShiftSalaryPlannerRoot` / application root

Own top-level composition concerns:

- appearance settings and theme;
- process/app-wide dependency creation;
- active-profile selection;
- profile-scoped dependency creation;
- passing an explicit dependency object into the existing application UI.

### Dependency containers

Use small explicit Kotlin containers/factories rather than a DI framework.

Two lifetimes are required:

1. **App-wide dependencies** — stable for the application composition lifetime, e.g. profile store and app-level services.
2. **Profile-scoped dependencies** — recreated when `activeProfileId` changes, e.g. payroll settings, work assignments, alarm settings, notes, Google Drive sync metadata, Room database/DAOs, holiday repository and Excel importer.

A dependency object is a composition boundary, not a service locator. Consumers receive the exact container they need; production code must not expose a global singleton registry.

## M4 behavior rules

- Existing `BottomTab` values, labels, order and rendering remain unchanged.
- Existing fullscreen flows remain driven by their current state during M4.
- Existing `rememberSaveable` feature state may remain in the large UI root during M4; moving it is M6.
- Existing back behavior remains unchanged.
- Existing active-profile switching semantics remain unchanged.
- Existing Google Drive, import, backup, alarm and report behavior remains unchanged.
- No Room schema/version change.

## M4 exit criteria

M4 is complete when all of the following are true:

- `MainActivity` is only a small Android entry point plus narrowly related intent parsing;
- `ShiftSalaryApp` no longer calls `AppDatabase.getDatabase(...)` directly;
- `ShiftSalaryApp` no longer directly constructs the current set of concrete settings stores/services that belong to the app/profile dependency lifetime;
- dependency lifetime changes on profile switch are explicit and testable;
- no visual/navigation behavior intentionally changes;
- M3 JVM tests remain green;
- phone + Wear debug APKs build;
- app + Wear lint have zero errors;
- independent review finds no important behavior regression.

---

# M5 — Navigation Rewrite

## Intent

Replace the current root navigation pattern — a tab enum plus many `showSomething` booleans and a long back-handler priority chain — with typed navigation state while keeping the same screens and visual transitions.

## Chosen approach

Do **not** introduce Navigation Compose in M5. The application already has custom bottom-tab/rail behavior, animated fullscreen overlays, widget entry routes and a number of modal flows. Introducing a navigation framework at the same time as changing route ownership would multiply risk without solving a user problem.

Instead introduce a small typed navigation model under the existing `ui/navigation` package.

The model must represent at least:

- selected `BottomTab`;
- selected finance sub-tab;
- optional fullscreen destination;
- destination arguments such as editing IDs/date/shift code where those arguments are logically navigation data;
- a single back operation with deterministic behavior.

Dialogs that are genuinely local to one feature may remain local feature state; fullscreen app destinations must not remain one-boolean-per-screen.

## M5 behavior rules

- Bottom navigation remains visually identical.
- Widget/deep-link inputs continue mapping to the same destination/sub-tab.
- Existing fullscreen animations remain unless an animation itself prevents the typed navigation model.
- Back returns through the same user-visible path as before.
- No feature-state redesign yet beyond moving navigation-owned arguments into navigation state.

## M5 testing

Add pure JVM tests for:

- widget raw-tab mapping;
- root tab selection;
- finance sub-tab selection;
- opening/closing fullscreen destinations;
- navigation arguments;
- deterministic back behavior for the former priority chain.

## M5 exit criteria

- the root `hasFullscreenUi` boolean expression is gone;
- the root long `BackHandler` `when` chain for fullscreen screens is gone;
- fullscreen root navigation uses one typed model;
- user-visible navigation remains functionally 1:1;
- full M3+M5 regression/build/lint gate is green.

---

# M6 — Feature State Ownership

## Intent

Reduce the giant root composable by moving feature-specific state and event handling into focused feature state holders without changing persistence semantics or user workflows.

## Chosen approach

Use plain Kotlin/Compose state holders and focused `remember...State()` factories first. Do not introduce ViewModel/Hilt/Koin simply as a refactor fashion choice.

State holders may internally use `remember`, `rememberSaveable`, `mutableStateOf`, `mutableStateListOf` or `mutableStateMapOf` as required to preserve the current behavior. Where save/restore behavior matters, it must remain equivalent to the existing root state.

Likely initial state boundaries are:

- calendar/pattern editing state;
- finance/payroll/report state;
- shifts/templates/manual-holiday state;
- alarms state;
- notes state;
- backup/import/settings workflow state.

The exact grouping is allowed to follow actual coupling found during implementation; it must not create one giant replacement `AppState` class.

## M6 behavior rules

- State is grouped by feature, not by technical primitive.
- Cross-feature state remains at the smallest common owner.
- Navigation state from M5 is not duplicated inside feature state holders.
- Persistence/store objects from M4 are dependencies, not fields recreated by feature UI.
- Existing save/restore behavior is preserved where currently provided by `rememberSaveable`.

## M6 exit criteria

- the application root is primarily composition/wiring rather than hundreds of state mutations;
- major feature screens receive a coherent state slice + event callbacks/controller instead of reaching into unrelated root variables;
- no new giant god-state object replaces the old god-composable;
- regression/build/lint gates remain green.

---

# M7 — UI/Data Boundary

## Intent

Make UI/features depend on stable feature-facing operations and data streams rather than concrete Room DAOs, DataStore-like stores, Android clients or service constructors.

## Chosen approach

Do not add a generic `Repository` layer everywhere. Add a boundary only where concrete storage/service details currently leak into feature orchestration.

Prefer small feature-facing ports/facades such as:

- calendar/shift data access;
- payroll/settings access;
- alarms access;
- notes access;
- backup/import operations;
- external sync operations.

Existing stores, Room DAOs and repositories remain the production backing implementations. M7 is about dependency direction, not rewriting persistence.

## M7 behavior rules

- no Room schema/version change;
- no backup schema change;
- no payroll formula change;
- no new network/cloud behavior;
- screen composables do not construct persistence/services;
- feature state/controllers consume interfaces or narrow facades where that isolation buys testability;
- avoid “Clean Architecture” boilerplate that has no concrete consumer.

## M7 testing

Add focused tests for the extracted orchestration boundaries using fakes where useful. Existing M3 fixtures remain the authoritative protection for payroll/persistence/alarm behavior.

## M7 exit criteria

- presentation code is no longer tied to concrete database/store/service construction;
- feature behavior can be exercised with bounded fake dependencies for the main workflows;
- existing production persistence implementations remain in place behind the new boundaries;
- M3–M7 regression/build/lint gate is green;
- independent review finds no important behavior regression.

---

# Cross-milestone workflow

Each milestone M4, M5, M6 and M7 must use its own isolated branch/worktree and be reviewable independently.

For every milestone:

1. start from canonical `master`;
2. update `docs/project/WORK_PROGRESS.md` at turn start/end;
3. use characterization/TDD for every extracted decision boundary;
4. keep changes bounded to the milestone;
5. run targeted tests during development;
6. run the full JVM/build/lint gate before claiming verification;
7. run independent review;
8. repair only proven findings;
9. record evidence in `CURRENT_STATE.md`;
10. push the milestone branch;
11. merge into `master` only after owner authorization.

Release/deploy remain separate owner-gates.

# M8 boundary

M8 may begin only after M4–M7 are complete on canonical `master`.

M8 is the first phase where information architecture, tab structure, visual hierarchy, appearance controls and broader UX may intentionally change. The target is still recognizably ShiftSalaryPlanner, but no longer constrained to pixel/flow parity with the recovered UI.
