# M7 UI/Data Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `ShiftSalaryApp` and feature presentation consume narrow feature-facing data/operation ports instead of concrete DAO/store/service implementations, without changing behavior or persistence semantics.

**Architecture:** Keep the existing Room DAOs, SharedPreferences/DataStore-like stores, repositories and Android integrations as production backing implementations. Introduce small ports only for concrete operation clusters that `ShiftSalaryApp` actually consumes, with thin production adapters created in the composition layer. Do not create a generic repository layer or move business formulas into the ports.

**Tech Stack:** Kotlin, Jetpack Compose, Room, kotlinx.coroutines `Flow`, existing Android/Google Drive/SharedPreferences integrations, JUnit.

**Spec:** `docs/superpowers/specs/2026-09-11-m4-m7-refactor-design.md`

## Global Constraints

- Preserve the current eight-tab UI, visible workflows and navigation 1:1; redesign begins only in M8.
- No Room schema/version change.
- No backup schema change.
- No payroll formula change.
- No new network/cloud behavior.
- Existing stores, Room DAOs and repositories remain production backing implementations.
- No Hilt, Koin, MVI, Navigation Compose, service locator, generic `Repository<T>`, or framework-for-framework's-sake abstraction.
- Every extracted decision/orchestration boundary gets characterization/TDD before production rewiring.
- M3 payroll/persistence/alarm fixtures remain authoritative.
- Push/merge/release/deploy remain separately owner-gated.

---

### Task 1: ScheduleDataPort — calendar/shift persistence boundary

**Progress checkpoint 2026-09-12:** interface + thin production adapter + fake-port contract test are GREEN and committed as `bdc640bb05374e676ec93d7f821b3abae3ae98f7`. `ProfileDependencies` / `ShiftSalaryApp` wiring is intentionally still pending; the next RED must cover clear-all schedule operations and callback-based pattern persistence so `CalendarLogicUtils` does not depend on `app.ports`.

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/ScheduleDataPort.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/ScheduleDataPortTest.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`

**Interfaces:**
- Consumes: existing `ShiftDayDao`, `ShiftTemplateDao`, `HolidayDao`, `WorkAssignmentsStore`.
- Produces:

```kotlin
interface ScheduleDataPort {
    val shiftDays: Flow<List<ShiftDayEntity>>
    val shiftTemplates: Flow<List<ShiftTemplateEntity>>
    val holidays: Flow<List<HolidayEntity>>
    val workAssignments: Flow<WorkAssignmentsState>

    suspend fun upsertShiftDay(item: ShiftDayEntity)
    suspend fun deleteShiftDay(date: String)
    suspend fun deleteShiftDays(startDate: String, endDate: String)
    suspend fun upsertShiftTemplate(item: ShiftTemplateEntity)
    suspend fun upsertShiftTemplates(items: List<ShiftTemplateEntity>)
    suspend fun deleteShiftTemplate(item: ShiftTemplateEntity)
    suspend fun upsertHolidays(items: List<HolidayEntity>)

    fun setWorkplaceShift(workplaceId: String, date: LocalDate, shiftCode: String?)
    fun clearWorkplaceAssignments(startDate: LocalDate, endDate: LocalDate)
    fun renameWorkplace(workplaceId: String, newName: String): Boolean
    fun replaceShiftCode(oldShiftCode: String, newShiftCode: String)
    fun removeShiftCode(shiftCode: String): List<WorkplaceDateShiftAssignment>
    fun restoreAssignments(assignments: List<WorkplaceDateShiftAssignment>)
}
```

Production implementation: `DefaultScheduleDataPort`, pure 1:1 delegation to the four existing concrete dependencies; no validation or ordering changes.

- [ ] **Step 1: Write RED delegation/flow tests** using in-memory fakes for the four consumed contracts. Assert each port method delegates once with exact arguments and each exposed flow is the backing flow.
- [ ] **Step 2: Run `ScheduleDataPortTest` and require RED** because `ScheduleDataPort` / `DefaultScheduleDataPort` do not exist.
- [ ] **Step 3: Implement the interface and thin production adapter** with no business logic.
- [ ] **Step 4: Run targeted test and require GREEN.**
- [ ] **Step 5: Add `scheduleData: ScheduleDataPort` to `ProfileDependencies` and construct it from the existing DAOs/store.** Keep concrete objects locally in `createProfileDependencies`; do not expose duplicate DAO/store fields once MainActivity no longer needs them.
- [ ] **Step 6: Rewire only schedule/calendar/shift persistence call sites in `ShiftSalaryApp`** to `scheduleData`, preserving coroutine boundaries and callback ordering.
- [ ] **Step 7: Run targeted tests + M3 persistence/payroll/alarm characterization + full app JVM.**
- [ ] **Step 8: Structural assertion:** `MainActivity.kt` has no `shiftDayDao`, `shiftTemplateDao`, `holidayDao`, or `workAssignmentsStore` aliases/usages.
- [ ] **Step 9: `git diff --check` and commit `refactor: add schedule data port`.**

---

### Task 2: AlarmDataPort + AlarmPlatformPort

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/AlarmPorts.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/AlarmPortsTest.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`

**Interfaces:**

```kotlin
interface AlarmDataPort {
    val settings: Flow<ShiftAlarmSettings>
    fun save(settings: ShiftAlarmSettings)
    fun synchronizeTemplates(templates: List<ShiftTemplateEntity>)
    fun upsertTemplateConfig(config: ShiftTemplateAlarmConfig)
    fun removeTemplateConfig(shiftCode: String)
}
```

`AlarmPlatformPort` exposes only the scheduler/permission/suppression operations currently called by `ShiftSalaryApp`; its default implementation delegates to `ShiftAlarmScheduler` with the existing `Context`. It must not change PendingIntent identity, alarm keys, permission rules, or scheduling horizons.

- [ ] **Step 1:** write RED delegation tests for settings/config operations and pure adapter tests for any extractable platform argument mapping.
- [ ] **Step 2:** implement minimal ports/adapters.
- [ ] **Step 3:** wire `ProfileDependencies.alarmData` and app-level `alarmPlatform`; replace direct store/static scheduler use in presentation orchestration only.
- [ ] **Step 4:** run `ShiftAlarmPlanningTest`, `ShiftAlarmsTabUiStateReducerTest`, `AlarmRuntimeStateTest`, targeted port tests and full JVM.
- [ ] **Step 5:** assert `MainActivity.kt` no longer references `ShiftAlarmStore` or `ShiftAlarmScheduler` directly; commit `refactor: add alarm data boundary`.

---

### Task 3: FinanceDataPort

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/FinanceDataPort.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/FinanceDataPortTest.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`

**Interface responsibilities:** expose existing flows and exact save/CRUD operations for `PayrollSettingsStore`, `WorkplacePayrollSettingsStore`, `AdditionalPaymentsStore`, `DeductionsStore`, `ReportVisibilitySettingsStore`, and `ReportHistoryStore`. The port returns existing model types unchanged and never invokes payroll calculation functions.

- [ ] **Step 1:** RED tests for flow identity and exact mutation delegation.
- [ ] **Step 2:** implement `DefaultFinanceDataPort` as delegation only.
- [ ] **Step 3:** wire through `ProfileDependencies` and remove concrete finance-store aliases from `ShiftSalaryApp`.
- [ ] **Step 4:** run M3 payroll characterization, `FinanceFeatureStateTest`, targeted port tests and full JVM.
- [ ] **Step 5:** assert no direct concrete finance stores in `MainActivity.kt`; commit `refactor: add finance data port`.

---

### Task 4: NotesDataPort, SettingsDataPort and ActivityLogPort

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/NotesDataPort.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/SettingsDataPort.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/ActivityLogPort.kt`
- Create: corresponding focused tests under `app/src/test/java/com/vigilante/shiftsalaryplanner/`
- Modify: `AppDependencies.kt`, `MainActivity.kt`

**Interfaces:**
- `NotesDataPort`: notes flow, `notesForDate`, `save`, `delete`.
- `SettingsDataPort`: workflow/assistant/today-layout/pattern streams plus exact save/CRUD operations.
- `ActivityLogPort`: event stream, `add(title, message, category)`, `clear()`.

No port owns M6 UI state holders.

- [ ] **Step 1:** RED delegation/flow tests.
- [ ] **Step 2:** minimal adapters and `ProfileDependencies` wiring.
- [ ] **Step 3:** rewire presentation orchestration without callback/order changes.
- [ ] **Step 4:** run `NotesFeatureStateTest`, settings characterization/state tests, targeted port tests and full JVM.
- [ ] **Step 5:** structural concrete-store assertion; commit `refactor: add notes and settings data ports`.

---

### Task 5: ProfileDataPort

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/ProfileDataPort.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/ProfileDataPortTest.kt`
- Modify: `AppDependencies.kt`, `MainActivity.kt`

**Interface:** state flow plus exact `setActiveProfile`, `createProfile`, `renameProfile`, `deleteProfile`, and `clearProfileData` delegation. `createProfileDependencies(context, activeProfileId)` remains the composition-root factory and is not moved into the port.

- [ ] **Step 1:** RED delegation test.
- [ ] **Step 2:** implement/wire adapter.
- [ ] **Step 3:** rewire profile lifecycle calls and run targeted + full JVM.
- [ ] **Step 4:** assert `MainActivity.kt` no longer references `AppProfileStore`; commit `refactor: add profile data port`.

---

### Task 6: ServiceOperationsPort — holiday sync, Excel import, backup/Drive coordination

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/ServiceOperationsPort.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/ServiceOperationsPortTest.kt`
- Modify: `AppDependencies.kt`, `MainActivity.kt`

**Boundary:** hide `HolidaySyncRepository`, `ExcelScheduleParser`, `ExcelScheduleImporter`, `GoogleDriveSyncStore`, Google sign-in/Drive helper invocation and backup restore persistence callbacks behind explicit UI-consumed operations. Existing helper functions and backup JSON builders/parsers stay unchanged and are delegated to; no new cloud behavior or backup format is introduced.

The interface must expose named operations matching current workflows (holiday sync, parse/import/clear Excel period, Drive account/meta settings, upload/download backup, restore backup) rather than a generic `execute()` or provider abstraction.

- [ ] **Step 1:** characterize current success/failure/status ordering around holiday sync, Excel import and backup restore with focused tests where pure seams exist; require RED for the new port contract.
- [ ] **Step 2:** implement delegation adapter using the existing helpers/repository/importer/store.
- [ ] **Step 3:** rewire one workflow at a time: holiday sync → Excel import → backup/Drive; run affected tests after each.
- [ ] **Step 4:** run `BackupCompatibilityTest` plus full JVM; verify backup JSON fixtures unchanged.
- [ ] **Step 5:** structural assertion that `ShiftSalaryApp` no longer aliases `HolidaySyncRepository`, `ExcelScheduleImporter`, `GoogleDriveSyncStore`, Google sign-in client/scope, or raw DAO callbacks for backup restore; commit `refactor: add service operations boundary`.

---

### Task 7: M7 structural cleanup and final qualification

**Files:**
- Modify only proven orphan imports/aliases in `MainActivity.kt` / `AppDependencies.kt`.
- Modify: `docs/project/CURRENT_STATE.md`
- Modify: this plan and `docs/project/WORK_PROGRESS.md`

- [ ] **Step 1:** source assertion: `ShiftSalaryApp` contains no direct concrete feature DAO/store/service aliases from the M7 inventory and no persistence/service constructors.
- [ ] **Step 2:** assert no generic `Repository<T>`, service locator, Hilt/Koin container, or replacement god facade was introduced.
- [ ] **Step 3:** fresh `clean :app:testDebugUnitTest`; record exact XML test/suite counts.
- [ ] **Step 4:** on unchanged tree run `:app:assembleDebug :wear:assembleDebug :app:lintDebug :wear:lintDebug`; require zero lint errors and record APK SHA-256.
- [ ] **Step 5:** `git diff --check` and independent whole-M7 review against canonical M7 base `d009796cb6ff179d46327f2228620ae239d3b8f9`; repair only proven Critical/Important/P2 findings via targeted TDD and repeat affected gates.
- [ ] **Step 6:** update canonical evidence to `M7 VERIFIED / READY FOR MERGE`; M8 remains blocked until owner-authorized integration of M7 to `master`.
- [ ] **Step 7:** append TURN END and commit docs closeout. Push/merge only under explicit owner authorization.

---

## M7 acceptance checklist

- [ ] UI/IA and business behavior remain intentionally unchanged.
- [ ] `ShiftSalaryApp` depends on feature-facing ports instead of concrete feature DAOs/stores/services.
- [ ] Existing persistence/service implementations remain production backends.
- [ ] No Room schema/version, backup schema, payroll formula or network/cloud behavior changes.
- [ ] Main workflows are exercisable with bounded fake ports.
- [ ] No generic repository/service-locator/DI boilerplate is introduced.
- [ ] M3–M7 regression/build/lint gates are green.
- [ ] Independent review has no actionable Critical/Important/P2 findings.
