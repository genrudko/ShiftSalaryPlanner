# M4 App Shell Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove dependency/service construction and root profile orchestration from the giant `ShiftSalaryApp` composable while preserving the current UI, navigation, data formats and user workflows 1:1.

**Architecture:** Introduce explicit app-wide and profile-scoped dependency containers plus a new `ShiftSalaryPlannerRoot` composable. `MainActivity` becomes an Android entry point; `ShiftSalaryPlannerRoot` owns theme, profile observation and dependency lifetimes; the existing `ShiftSalaryApp` keeps feature/navigation state for now and receives already-created dependencies. No DI framework or navigation rewrite is introduced in M4.

**Tech Stack:** Kotlin, Jetpack Compose, Room, existing settings stores, Google Sign-In/Drive scope, existing Gradle/JVM/lint gates.

**Spec:** `docs/superpowers/specs/2026-09-11-m4-m7-refactor-design.md`

## Global Constraints

- User-visible UI and navigation behavior remain functionally 1:1 through M4.
- Do not change payroll semantics, Room schema/version, backup format, alarm delivery semantics, bottom-tab structure or user-facing text for style reasons.
- Do not add Hilt, Koin, Navigation Compose, or unrelated dependency upgrades.
- App/profile dependency construction must leave `ShiftSalaryApp`.
- `rememberSaveable` feature/navigation state stays in `ShiftSalaryApp` until M5/M6 unless moving a value is strictly required to preserve a dependency lifetime.
- Existing M3 characterization/migration/backup/alarm tests remain authoritative regression protection.
- Release/deploy are out of scope.

---

### Task 1: Explicit dependency containers

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt`

**Interfaces:**
- Produces: `AppDependencies`, `ProfileDependencies`, `createAppDependencies(context: Context)`, `createProfileDependencies(context: Context, activeProfileId: String)`.
- Consumers: Task 2 root/wiring.

`AppDependencies` owns objects whose lifetime does not depend on the selected profile:

```kotlin
data class AppDependencies(
    val profileStore: AppProfileStore,
    val googleDriveScope: Scope,
    val googleSignInClient: GoogleSignInClient,
    val excelScheduleParser: ExcelScheduleParser
)
```

`ProfileDependencies` owns the objects currently rebuilt under `remember(activeProfileId)`:

```kotlin
data class ProfileDependencies(
    val payrollSettingsStore: PayrollSettingsStore,
    val reportVisibilitySettingsStore: ReportVisibilitySettingsStore,
    val workAssignmentsStore: WorkAssignmentsStore,
    val workplacePayrollSettingsStore: WorkplacePayrollSettingsStore,
    val shiftAlarmStore: ShiftAlarmStore,
    val patternTemplatesStore: PatternTemplatesStore,
    val additionalPaymentsStore: AdditionalPaymentsStore,
    val deductionsStore: DeductionsStore,
    val appEventLogStore: AppEventLogStore,
    val reportHistoryStore: ReportHistoryStore,
    val appWorkflowSettingsStore: AppWorkflowSettingsStore,
    val assistantAiSettingsStore: AssistantAiSettingsStore,
    val appNotesStore: AppNotesStore,
    val todayLayoutSettingsStore: TodayLayoutSettingsStore,
    val googleDriveSyncStore: GoogleDriveSyncStore,
    val database: AppDatabase,
    val shiftDayDao: ShiftDayDao,
    val shiftTemplateDao: ShiftTemplateDao,
    val holidayDao: HolidayDao,
    val holidaySyncRepository: HolidaySyncRepository,
    val excelScheduleImporter: ExcelScheduleImporter
)
```

- [x] **Step 1:** Create `AppDependencies.kt`. Use `context.applicationContext` for long-lived constructors. `createAppDependencies` creates `AppProfileStore`, `Scope(DriveScopes.DRIVE_APPDATA)`, the current `GoogleSignInClient` configuration, and `ExcelScheduleParser`.
- [x] **Step 2:** Implement `createProfileDependencies`. It must call `AppDatabase.getDatabase(appContext, activeProfileId)` exactly once, derive all three DAOs from that instance, and create `HolidaySyncRepository(holidayDao)` plus `ExcelScheduleImporter(shiftTemplateDao, shiftDayDao)` from those DAOs. Construct the same 15 profile-keyed stores that the old root constructs.
- [x] **Step 3:** Run `./gradlew :app:compileDebugKotlin --no-daemon`. Expected: compile success; the new file is not wired yet.
- [x] **Step 4:** Run `git diff --check` and commit: `refactor: define app dependency boundaries`.

### Task 2: Application root and existing UI wiring

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ShiftSalaryPlannerRoot.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Use: Task 1 `AppDependencies.kt`.

**Interfaces:**

```kotlin
@Composable
fun ShiftSalaryPlannerRoot(
    initialTabName: String? = null,
    initialFinanceSubTabName: String? = null
)
```

`ShiftSalaryApp` becomes:

```kotlin
fun ShiftSalaryApp(
    initialTabName: String? = null,
    initialFinanceSubTabName: String? = null,
    appearanceSettings: AppearanceSettings,
    onSaveAppearanceSettings: (AppearanceSettings) -> Unit,
    profilesState: AppProfilesState,
    appDependencies: AppDependencies,
    profileDependencies: ProfileDependencies
)
```

Root responsibilities are exactly:

1. get `LocalContext.current`;
2. create/observe `AppearanceSettingsStore` and apply `ShiftSalaryPlannerTheme`;
3. create app-wide dependencies once for the application context;
4. observe `appDependencies.profileStore.stateFlow` with the same fallback initial profile semantics as current code;
5. derive `activeProfileId` from that observed state;
6. `remember(activeProfileId)` the result of `createProfileDependencies(context, activeProfileId)`;
7. render the existing UI by calling `ShiftSalaryApp(...)` with appearance settings, save callback, `profilesState`, `appDependencies` and `profileDependencies`.

- [x] **Step 1:** Create `ShiftSalaryPlannerRoot.kt` implementing those seven responsibilities and nothing feature-specific.
- [x] **Step 2:** Change `ShiftSalaryApp` to receive the three new inputs (`profilesState`, `appDependencies`, `profileDependencies`). Remove its `AppProfileStore` construction and profile `stateFlow.collectAsState`; derive `activeProfileId` and `activeProfileName` from supplied `profilesState` exactly as before.
- [x] **Step 3:** Replace M4-owned constructors inside `ShiftSalaryApp` with aliases from the supplied containers. Preserve the old local variable names (`profileStore`, `payrollSettingsStore`, `shiftDayDao`, `googleDriveScope`, `googleSignInClient`, `excelScheduleParser`, etc.) so downstream code is mechanically unchanged.
- [x] **Step 4:** Keep `googleSignedInAccount`, activity-result launchers, coroutine/snackbar state, all navigation booleans and all feature `remember*` state in `ShiftSalaryApp`. Those belong to M5/M6.
- [x] **Step 5:** Run a source-boundary assertion limited to the `ShiftSalaryApp` body and require no occurrences of `AppDatabase.getDatabase(`, `PayrollSettingsStore(`, `ShiftAlarmStore(`, `GoogleSignIn.getClient(`, `Scope(DriveScopes` or `ExcelScheduleImporter(`.
- [x] **Step 6:** Run `./gradlew :app:testDebugUnitTest --no-daemon`. Expected: all M3 JVM tests pass.
- [x] **Step 7:** Run `git diff --check` and commit: `refactor: extract application root dependencies`.

### Task 3: Slim Android entry point

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`

**Interfaces:**
- Consumes: `ShiftSalaryPlannerRoot` from Task 2.
- `MainActivity.onCreate` retains `enableEdgeToEdge`, initial intent extraction/mapping/removal and `setContent`.

Target body inside `setContent`:

```kotlin
ShiftSalaryPlannerRoot(
    initialTabName = initialWidgetTab,
    initialFinanceSubTabName = initialFinanceSubTabName
)
```

- [x] **Step 1:** Replace the current appearance/theme wrapper in `MainActivity.onCreate` with the root call above. Do not change `parseInitialWidgetTab` or `parseInitialWidgetFinanceSubTab` behavior.
- [x] **Step 2:** Remove only imports made unused by M4; do not broad-format or opportunistically clean the 5k-line file.
- [x] **Step 3:** Assert the `MainActivity.onCreate` section contains none of `AppearanceSettingsStore(`, `AppDatabase.getDatabase(`, `GoogleSignIn.getClient(` or any `*SettingsStore(` constructor.
- [x] **Step 4:** Run `./gradlew :app:testDebugUnitTest --no-daemon` and `git diff --check`.
- [x] **Step 5:** Commit: `refactor: slim Android app entry point`.

### Task 4: M4 qualification, review and branch closeout

**Files:**
- Modify: `docs/project/CURRENT_STATE.md`
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify: `docs/superpowers/plans/2026-09-11-m4-app-shell-extraction.md` checkboxes/evidence only.

**Interfaces:**
- Produces: a verified M4 branch ready for owner-authorized merge; does not merge or release.

- [x] **Step 1:** Re-run source-boundary assertions: `MainActivity.onCreate` does not construct app stores/Room/Google clients; `ShiftSalaryApp` does not construct M4 dependency objects; `ShiftSalaryPlannerRoot` owns profile observation and keyed profile-dependency creation.
- [x] **Step 2:** Run fresh unit gate: `./gradlew clean :app:testDebugUnitTest --no-daemon`; count JUnit XML and require zero failures/errors/skips.
- [x] **Step 3:** On the unchanged tree run `./gradlew :app:assembleDebug :wear:assembleDebug :app:lintDebug :wear:lintDebug --no-daemon`; require both APKs and zero lint errors.
- [x] **Step 4:** Run an independent Codex review of the whole M4 diff against its `master` base. Repair only proven correctness/behavior findings; after any production change repeat affected targeted tests and the final gates.
- [x] **Step 5:** Update `CURRENT_STATE.md` to `M4 VERIFIED / READY FOR MERGE`, recording exact branch/head and evidence. Append the current TURN END to `WORK_PROGRESS.md` with exact next operation.
- [x] **Step 6:** Commit docs-only closeout and guarded-push the M4 branch. Verify remote branch SHA equals local SHA and canonical `master` is unchanged.

## Plan self-review checklist

- [x] No task changes navigation architecture (reserved for M5).
- [x] No task moves feature UI state (reserved for M6).
- [x] No task introduces repository/facade interfaces (reserved for M7).
- [x] Dependency lifetime ownership is explicit: app-wide once, profile-scoped keyed by active profile.
- [x] No Room/backup/payroll schema or semantics changes.
- [x] Verification uses the M3 safety net plus build/lint and independent review.
