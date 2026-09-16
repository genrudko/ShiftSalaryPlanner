# M15.1 Today Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current free-form Today top section with a fixed operational Hero + normalized timeline, add an optional read-only Android calendar layer and system event-creation entry point, while retaining lower-module customization and all existing business semantics.

**Architecture:** Move Today-specific UI/models out of `WorkflowServiceScreens.kt` into `ui/today`, introduce pure timeline normalization/ordering before Android integrations, then add an Android Calendar Provider adapter behind a narrow interface. `MainActivity` remains composition/integration glue; Today UI receives normalized data and callbacks rather than querying providers/stores.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android CalendarContract, existing StateFlow/settings ports, JVM unit tests, Compose Preview Screenshot Testing, Gradle/VPS environment from `scripts/android/vps-env.sh`.

**Spec:** `docs/superpowers/specs/2026-09-16-m15-today-weather-design.md`

## Global Constraints

- Work only on `feature/m15-today-weather`, based on `e7ac501`; do not modify `master`, push, release or deploy.
- Follow RED -> verified failure -> minimal GREEN for production behavior changes.
- External calendar is read-only: `READ_CALENDAR` only; never add `WRITE_CALENDAR`.
- `+ Event` delegates to the system calendar insert UI; no local `PlannedEvent` persistence.
- Preserve payroll arithmetic, Room schema/migrations, alarm scheduling semantics, Notes v2 authority/compatibility, Wear sync and backup compatibility.
- Legacy Today layout JSON must remain readable; historical fixed-top bubble IDs may not become data-loss failures.
- The fixed operational hierarchy is Hero -> Timeline -> lower configurable modules.
- Do not begin M15.2 Weather provider implementation in this plan.

---

### Task 1: Today timeline pure model and ordering

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayTimeline.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15TodayTimelineTest.kt`

**Interfaces:**
- Produces `TimelineSource`, `TimelineKind`, `TimelineSeverity`, `TimelineAction`, `TimelineItem`, and `buildTodayTimeline(items, nowMillis)`.
- Later tasks consume the ordered immutable list and stable identifiers only.

- [ ] **Step 1: Write failing JVM tests** covering chronological ordering, deterministic tie-breaking, retention of distinct recurring external occurrences, and source/action payload preservation.
- [ ] **Step 2: Run** `:app:testDebugUnitTest --tests '*M15TodayTimelineTest*'` and verify RED because the timeline types/functions do not exist.
- [ ] **Step 3: Implement minimal pure timeline model and deterministic `buildTodayTimeline`.** Do not reference Android APIs.
- [ ] **Step 4: Re-run the focused test and require GREEN.**
- [ ] **Step 5: Commit** as `feat: add M15 Today timeline model`.

### Task 2: Characterize and extract Today presentation

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayModels.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkflowServiceScreens.kt`
- Modify: imports/callsite only as needed in `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15TodayStructureTest.kt`

**Interfaces:**
- Moves `TodayShiftPreview` and `UpcomingPaymentItem` into `ui.today` without changing field semantics.
- Produces `TodayScreen(...)` that consumes existing Today state plus `timelineItems` and callbacks.

- [ ] **Step 1: Add RED structural tests** requiring Today-specific screen/model declarations under `ui/today`, requiring `WorkflowServiceScreens.kt` not to own `TodayTabScreen`, and preserving existing callsite types.
- [ ] **Step 2: Verify RED** with the focused test.
- [ ] **Step 3: Mechanically extract the existing Today presentation and data classes** without redesigning it yet; preserve behavior and current layout settings.
- [ ] **Step 4: Run structure test plus production Kotlin compile** and require GREEN.
- [ ] **Step 5: Commit** as `refactor: extract M15 Today presentation`.

### Task 3: Fixed Hero + timeline hierarchy with lower-module compatibility

**Files:**
- Modify: `ui/today/TodayScreen.kt`
- Modify: `settings/TodayLayoutSettingsStore.kt`
- Modify: `app/MainActivity.kt` only for normalized timeline input/action callbacks
- Test: `M15TodayStructureTest.kt`
- Test: `M15TodayTimelineTest.kt`
- Add screenshot test file under existing app screenshot-test source set following M8-M14 conventions.

**Interfaces:**
- `TodayScreen` renders fixed Hero then Timeline then lower configurable modules.
- `TodayLayoutSettings.normalized()` keeps parsing all historical IDs but fixed-top IDs are excluded from the editable lower-module list.

- [ ] **Step 1: Extend RED tests** so `HERO`, `PULSE`, `NEXT_STEP`, `QUICK_ACTIONS` cannot appear as independently reorderable lower modules while old serialized values remain parseable.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement compatibility normalization** and compose the fixed top hierarchy using existing Evolution primitives. Initially timeline may contain app-derived items only and an empty external-calendar contribution.
- [ ] **Step 4: Add deterministic screenshot states** for normal shift day, no-shift day, dark theme and fontScale 1.3. Keep fixtures independent of wall-clock time.
- [ ] **Step 5: Run focused behavior/structure tests and screenshot RED/update/validate loop; manually inspect generated PNGs before accepting references.**
- [ ] **Step 6: Commit** as `feat: add M15 Today operational hierarchy`.

### Task 4: Read-only external calendar source

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/integrations/calendar/ExternalCalendarSource.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/integrations/calendar/AndroidCalendarSource.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15ExternalCalendarMappingTest.kt`
- Modify: `app/src/main/AndroidManifest.xml` for `READ_CALENDAR` only.

**Interfaces:**
- `ExternalCalendarSource.occurrences(startMillis, endMillis, enabledCalendarIds): List<ExternalCalendarOccurrence>`.
- `ExternalCalendarOccurrence` carries stable calendar/event/instance identity, start/end, all-day, title, calendar label/color.
- Mapping function converts occurrences into `TimelineItem` with source `EXTERNAL_CALENDAR`.

- [ ] **Step 1: Write mapping RED tests** for distinct recurrence instances, all-day entries, calendar label/color propagation and deterministic stable IDs.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement pure occurrence/mapping model first, then Android CalendarContract adapter using `Instances` query.** No write APIs.
- [ ] **Step 4: Run focused tests and production Kotlin compile.**
- [ ] **Step 5: Assert manifest contains `READ_CALENDAR` and does not contain `WRITE_CALENDAR`.**
- [ ] **Step 6: Commit** as `feat: add read-only Today calendar source`.

### Task 5: Calendar layer settings, runtime permission, and + Event delegation

**Files:**
- Create or extend focused Today settings model/store under `settings/` without replacing existing `TodayLayoutSettingsStore` responsibilities unnecessarily.
- Modify: `ui/today/TodayScreen.kt`
- Modify: `app/MainActivity.kt`
- Test: focused settings/structure tests.

**Interfaces:**
- Persist global external-calendar layer enabled state and enabled calendar IDs.
- Today exposes a layer control and `+ Event` action.
- `+ Event` uses `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI`; no app-owned event is created.

- [ ] **Step 1: RED tests** for default calendar layer disabled, persisted enable/disable/calendar filtering, and structural absence of local event persistence/write-calendar permission.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement settings and runtime permission flow:** enabling the layer requests `READ_CALENDAR`; denial leaves the layer disabled and shows actionable UI state.
- [ ] **Step 4: Implement system event-create intent** with current date/time seed and no write permission.
- [ ] **Step 5: Run focused tests, production compile and screenshot validation.**
- [ ] **Step 6: Commit** as `feat: connect external calendar to M15 Today`.

### Task 6: M15.1 regression and visual qualification

**Files:**
- Update `docs/project/WORK_PROGRESS.md`
- Update `docs/project/CURRENT_STATE.md`
- Update M15 spec only if implementation reveals an explicit accepted ruling.

**Interfaces:** none; qualification boundary.

- [ ] **Step 1: Run focused M15 tests** plus relevant navigation, Notes/settings port, alarm and backup compatibility tests.
- [ ] **Step 2: Run full app JVM tests and independent screenshot validation.**
- [ ] **Step 3: Run app `assembleDebug + lintDebug`; run Wear compile/assemble/lint regression because shared navigation/data contracts must remain intact.**
- [ ] **Step 4: Run `git diff --check`, inspect diff for forbidden payroll/Room/alarm/Wear semantic changes, and record APK hashes.**
- [ ] **Step 5: Perform whole-M15.1 review against this plan/spec; repair only proven findings with RED -> GREEN evidence.**
- [ ] **Step 6: Write durable closeout ledger and commit docs-only checkpoint.** Do not push/merge/release/deploy.
