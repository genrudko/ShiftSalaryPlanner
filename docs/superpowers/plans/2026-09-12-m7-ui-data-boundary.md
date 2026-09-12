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

**Completed 2026-09-12:** foundation `bdc640bb05374e676ec93d7f821b3abae3ae98f7` plus wiring commit `1b6a7c550a62539a4d597f6d8f3fb108d8001cdd`. RED→GREEN covered clear-all operations and callback-based pattern persistence; `CalendarLogicUtils` no longer depends on Room, `ProfileDependencies` exposes only `scheduleData`, and `ShiftSalaryApp` has zero `shiftDayDao` / `shiftTemplateDao` / `holidayDao` / `workAssignmentsStore` references. M3 targeted characterization is GREEN and full JVM is **98/98**, zero failures/errors/skips, 24 suites.

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
    suspend fun clearAllShiftDays()
    suspend fun upsertShiftTemplate(item: ShiftTemplateEntity)
    suspend fun upsertShiftTemplates(items: List<ShiftTemplateEntity>)
    suspend fun deleteShiftTemplate(item: ShiftTemplateEntity)
    suspend fun upsertHolidays(items: List<HolidayEntity>)

    fun setWorkplaceShift(workplaceId: String, date: LocalDate, shiftCode: String?)
    fun clearWorkplaceAssignments(startDate: LocalDate, endDate: LocalDate)
    fun clearAllWorkplaceAssignments()
    fun renameWorkplace(workplaceId: String, newName: String): Boolean
    fun replaceShiftCode(oldShiftCode: String, newShiftCode: String)
    fun removeShiftCode(shiftCode: String): List<WorkplaceDateShiftAssignment>
    fun restoreAssignments(assignments: List<WorkplaceDateShiftAssignment>)
}
```

Production implementation: `DefaultScheduleDataPort`, pure 1:1 delegation to the four existing concrete dependencies; no validation or ordering changes.

- [x] **Step 1: Write RED delegation/flow tests** using in-memory fakes for the four consumed contracts. Assert each port method delegates once with exact arguments and each exposed flow is the backing flow.
- [x] **Step 2: Run `ScheduleDataPortTest` and require RED** because `ScheduleDataPort` / `DefaultScheduleDataPort` do not exist.
- [x] **Step 3: Implement the interface and thin production adapter** with no business logic.
- [x] **Step 4: Run targeted test and require GREEN.**
- [x] **Step 5: Add `scheduleData: ScheduleDataPort` to `ProfileDependencies` and construct it from the existing DAOs/store.** Keep concrete objects locally in `createProfileDependencies`; do not expose duplicate DAO/store fields once MainActivity no longer needs them.
- [x] **Step 6: Rewire only schedule/calendar/shift persistence call sites in `ShiftSalaryApp`** to `scheduleData`, preserving coroutine boundaries and callback ordering.
- [x] **Step 7: Run targeted tests + M3 persistence/payroll/alarm characterization + full app JVM.**
- [x] **Step 8: Structural assertion:** `MainActivity.kt` has no `shiftDayDao`, `shiftTemplateDao`, `holidayDao`, or `workAssignmentsStore` aliases/usages.
- [x] **Step 9: `git diff --check` and commit `refactor: add schedule data port`.**

---

### Task 2: AlarmDataPort + AlarmPlatformPort

**Progress checkpoint 2026-09-12:** COMPLETE locally. Foundation RED `job_cf4ad4a18d59435389ed9ee289809976` → GREEN `job_6d51202ec9754ca08a0da0767f923a79` established the two ports at `0f6439ed0dd36f394280cf02cec3353009a0425e`. Structural RED `job_f1218a33e11845acb8b045825140cba4` then proved the presentation/effects wiring target. Implementation commit `23c328849eea9310a12ded23f819ffb415faba28` wires `ProfileDependencies.alarmData` and app-level `alarmPlatform`, removes direct `ShiftAlarmStore` / `ShiftAlarmScheduler` use from `MainActivity` and `ShiftAlarmsEffects`, adapts the shared Wear reschedule helper, and preserves save→optional suppressed-clear→reschedule ordering. Targeted alarm/structural gate `job_e6a03cb6d6334e2e8d0cca276046cf61` and full JVM `job_d0159641646b4b159cbb0f5b0ea7c2b0` are GREEN; full JVM is 100/100.

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

- [x] **Step 1:** write RED delegation tests for settings/config operations and pure adapter tests for any extractable platform argument mapping.
- [x] **Step 2:** implement minimal ports/adapters.
- [x] **Step 3:** wire `ProfileDependencies.alarmData` and app-level `alarmPlatform`; replace direct store/static scheduler use in presentation orchestration only.
- [x] **Step 4:** run `ShiftAlarmPlanningTest`, `ShiftAlarmsTabUiStateReducerTest`, `AlarmRuntimeStateTest`, targeted port tests and full JVM.
- [x] **Step 5:** assert `MainActivity.kt` no longer references `ShiftAlarmStore` or `ShiftAlarmScheduler` directly; commit `refactor: add alarm data boundary`.

---

### Task 3: FinanceDataPort

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/FinanceDataPort.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/FinanceDataPortTest.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`

**Interface responsibilities:** expose existing flows and exact save/CRUD operations for `PayrollSettingsStore`, `WorkplacePayrollSettingsStore`, `AdditionalPaymentsStore`, `DeductionsStore`, `ReportVisibilitySettingsStore`, and `ReportHistoryStore`. The port returns existing model types unchanged and never invokes payroll calculation functions.

**Progress checkpoint 2026-09-12:** COMPLETE locally at `f59c8e1444100f58d5a5c81a7da1776e28119cc5`. Clean RED `job_e21b55295fac4911802f37bf0c4c2fdb` proved the port API was absent; targeted GREEN `job_2072dc82933f4c869d37bc60fba78ac1` established the interface + thin adapter. Structural RED `job_017c65a9a1c844aab242879081975c49` then proved the six concrete finance stores still leaked into presentation. Final wiring removes all six aliases/fields from `ShiftSalaryApp` / `ProfileDependencies`, keeps existing model types and payroll calculations unchanged, passes Finance + payroll characterization, and full JVM `job_c049f139e6964caa854b1adbbcdb6e14` is 102/102.

- [x] **Step 1:** RED tests for flow identity and exact mutation delegation.
- [x] **Step 2:** implement `DefaultFinanceDataPort` as delegation only.
- [x] **Step 3:** wire through `ProfileDependencies` and remove concrete finance-store aliases from `ShiftSalaryApp`.
- [x] **Step 4:** run M3 payroll characterization, `FinanceFeatureStateTest`, targeted port tests and full JVM.
- [x] **Step 5:** assert no direct concrete finance stores in `MainActivity.kt`; commit `refactor: add finance data port`.

---

### Task 4: NotesDataPort, SettingsDataPort and ActivityLogPort

**Status 2026-09-12:** COMPLETE locally at `b4e3367538389bfcad632c3da8eda6d036d891eb`; clean contract RED `job_2d32d755579045718f7e594c2831a3e9`, structural RED `job_dafdfa7017ac4f349ac6385213188081`, targeted GREEN `job_723e10dab0954bc28062cf3fc1fc9523`, full JVM `job_3cde9169dac340b88791be8df410c582` = **104/104**, 0 failures/errors/skips, 30 suites.

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

- [x] **Step 1:** RED delegation/flow tests.
- [x] **Step 2:** minimal adapters and `ProfileDependencies` wiring.
- [x] **Step 3:** rewire presentation orchestration without callback/order changes.
- [x] **Step 4:** run `NotesFeatureStateTest`, settings characterization/state tests, targeted port tests and full JVM.
- [x] **Step 5:** structural concrete-store assertion; commit `refactor: add notes and settings data ports`.

---

### Task 5: ProfileDataPort

**Status 2026-09-12:** COMPLETE locally at `bd1e0bcfbebbea104c098f096a28a33a2c50b73b`. Clean port RED `job_ec8a780b2bd843fa9b1da0b560d39780` failed only because `ProfileDataPort` was absent. `DefaultProfileDataPort` now hides the existing `AppProfileStore`, including the Root initial snapshot needed to eliminate concrete-store constants/resolution from presentation. Targeted profile/structural + full JVM job `job_75893bdfe1074ebe85d1c1b88150f615` is GREEN; full JVM = **106/106**, 0 failures/errors/skips, 32 suites.

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/ProfileDataPort.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/ProfileDataPortTest.kt`
- Modify: `AppDependencies.kt`, `MainActivity.kt`

**Interface:** state flow plus exact `setActiveProfile`, `createProfile`, `renameProfile`, `deleteProfile`, and `clearProfileData` delegation. `createProfileDependencies(context, activeProfileId)` remains the composition-root factory and is not moved into the port.

- [x] **Step 1:** RED delegation test.
- [x] **Step 2:** implement/wire adapter.
- [x] **Step 3:** rewire profile lifecycle calls and run targeted + full JVM.
- [x] **Step 4:** assert `MainActivity.kt` no longer references `AppProfileStore`; commit `refactor: add profile data port`.

---

### Task 6: ServiceOperationsPort — holiday sync, Excel import, backup/Drive coordination

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/ServiceOperationsPort.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/ServiceOperationsPortTest.kt`
- Modify: `AppDependencies.kt`, `MainActivity.kt`

**Boundary:** hide `HolidaySyncRepository`, `ExcelScheduleParser`, `ExcelScheduleImporter`, `GoogleDriveSyncStore`, Google sign-in/Drive helper invocation and backup restore persistence callbacks behind explicit UI-consumed operations. Existing helper functions and backup JSON builders/parsers stay unchanged and are delegated to; no new cloud behavior or backup format is introduced.

The interface must expose named operations matching current workflows (holiday sync, parse/import/clear Excel period, Drive account/meta settings, upload/download backup, restore backup) rather than a generic `execute()` or provider abstraction.

- [x] **Step 1:** characterize current success/failure/status ordering around holiday sync, Excel import and backup restore with focused tests where pure seams exist; require RED for the new port contract.
- [x] **Step 2:** implement delegation adapter using the existing helpers/repository/importer/store.
- [x] **Step 3:** rewire one workflow at a time: holiday sync → Excel import → backup/Drive; run affected tests after each.
- [x] **Step 4:** run `BackupCompatibilityTest` plus full JVM; verify backup JSON fixtures unchanged.
- [x] **Step 5:** structural assertion that `ShiftSalaryApp` no longer aliases `HolidaySyncRepository`, `ExcelScheduleImporter`, `GoogleDriveSyncStore`, Google sign-in client/scope, or raw DAO callbacks for backup restore; commit `refactor: add service operations boundary`.


**Progress checkpoint 2026-09-12:** named-operation contract RED `job_f424f4c4b6d64c9abebd7696402de582` failed only because `ServiceOperationsPort.kt` did not yet exist. Thin helper/repository/store delegation foundation is committed at `03ea1e2` (`refactor: define service operations port`). Final presentation/composition-root wiring is committed at `3a99dfe31d1122ad1e867cab8c6bd9d909681790` (`refactor: add service operations boundary`): holiday sync, Excel parse/import, Google sign-in/Drive metadata/upload/download and backup build/restore are consumed through named port operations; raw backup restore schedule callbacks no longer leak into `ShiftSalaryApp`. Existing backup helper/parser files and backup test fixtures are byte-identical to the pre-Task-6 checkpoint. Targeted service/backup/holiday gate `job_f635aa7b04f24b009f15bedff7b9883c` GREEN; full JVM `job_0145e3f2fc554b15aeecf4a8411b9b5d` **108/108**, zero failures/errors/skips across 34 suites. `git diff --check` and service structural grep are clean.

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
