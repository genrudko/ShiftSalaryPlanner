# M7 UI/Data Boundary Inventory

Canonical base: `d009796cb6ff179d46327f2228620ae239d3b8f9` (`master`, M6 COMPLETE).

## Contract

M7 implements the approved `docs/superpowers/specs/2026-09-11-m4-m7-refactor-design.md` boundary: presentation/orchestration must consume stable feature-facing operations/data streams instead of concrete DAO/store/service construction. Existing Room DAOs, stores and repositories stay as production backing implementations. No generic repository layer is introduced.

Out of scope: Room schema/version changes, backup schema changes, payroll formula changes, new network/cloud behavior, UX/IA changes, Hilt/Koin/MVI/Navigation Compose, Wear/widget redesign, release/deploy.

## Current leak inventory

`ShiftSalaryApp` still aliases concrete dependencies from `AppDependencies` / `ProfileDependencies`. Reference counts in `MainActivity.kt` at the M7 baseline show the highest coupling around schedule/calendar and alarm workflows:

| Concrete dependency | Approx. refs in `MainActivity.kt` | M7 owner/boundary |
| --- | ---: | --- |
| `ShiftDayDao` | 21 | `ScheduleDataPort` |
| `WorkAssignmentsStore` | 20 | `ScheduleDataPort` |
| `ShiftTemplateDao` | 18 | `ScheduleDataPort` |
| `ShiftAlarmStore` | 15 | `AlarmDataPort` |
| `AppEventLogStore` | 19 | `ActivityLogPort` |
| `PatternTemplatesStore` | 7 | `SettingsDataPort` |
| `AdditionalPaymentsStore` | 7 | `FinanceDataPort` |
| `DeductionsStore` | 6 | `FinanceDataPort` |
| `PayrollSettingsStore` | 5 | `FinanceDataPort` |
| `WorkplacePayrollSettingsStore` | 4 | `FinanceDataPort` |
| `AppNotesStore` | 6 | `NotesDataPort` |
| `AppWorkflowSettingsStore` | 8 | `SettingsDataPort` |
| `ReportHistoryStore` | 7 | `FinanceDataPort` |
| `GoogleDriveSyncStore` | 7 | `ServiceOperationsPort` |
| `HolidayDao` | 3 | `ScheduleDataPort` |
| `HolidaySyncRepository` | 5 | `ServiceOperationsPort` |
| `ExcelScheduleImporter` | 3 | `ServiceOperationsPort` |
| `AppProfileStore` | 6 | `ProfileDataPort` |

Counts are navigation aids, not acceptance metrics; structural acceptance is based on concrete-type references/imports and behavior gates.

## Boundary decisions

### 1. Schedule/calendar/shift data

`ScheduleDataPort` owns only persistence-facing schedule streams and mutations currently delegated directly to `ShiftDayDao`, `ShiftTemplateDao`, `HolidayDao`, and `WorkAssignmentsStore`. It does not calculate payroll, alarm times, patterns, or UI state. The production adapter delegates 1:1 to existing implementations.

**Status:** COMPLETE locally at `1b6a7c550a62539a4d597f6d8f3fb108d8001cdd`; presentation-side references to all four concrete dependencies are zero and full JVM is 98/98.

### 2. Alarm settings data vs Android alarm platform

`AlarmDataPort` hides `ShiftAlarmStore` settings/config persistence. Static Android scheduling/permission/suppression calls remain a separate `AlarmPlatformPort`; this avoids pretending SharedPreferences persistence and Android platform scheduling are one repository.

**Status:** COMPLETE locally at `23c328849eea9310a12ded23f819ffb415faba28`; presentation/effects references to `ShiftAlarmStore` and `ShiftAlarmScheduler` are zero, targeted alarm gates are GREEN, and full JVM is 100/100.

### 3. Finance/payments/report data

`FinanceDataPort` exposes payroll/workplace settings, additional payments, deductions, report visibility/history streams and exact mutation operations already used by UI orchestration. Payroll calculations continue to use the existing payroll engine unchanged.

**Status:** COMPLETE locally at `f59c8e1444100f58d5a5c81a7da1776e28119cc5`; all six concrete finance-store references are removed from presentation/ProfileDependencies, Finance + payroll characterization is GREEN, and full JVM is 102/102.

### 4. Notes and user-facing settings

`NotesDataPort` stays separate because Notes is a coherent feature and has date-query/save/delete behavior. `SettingsDataPort` groups small settings-only stores (`AppWorkflowSettingsStore`, `AssistantAiSettingsStore`, `TodayLayoutSettingsStore`, `PatternTemplatesStore`) whose contract is read stream + save/CRUD; it does not absorb profile, finance, alarm, or external sync operations.

### 5. Activity log

`ActivityLogPort` wraps `AppEventLogStore` because event logging is cross-feature infrastructure used by many workflows. It is intentionally not folded into each feature port.

### 6. Profiles

`ProfileDataPort` wraps `AppProfileStore` state and profile lifecycle operations. Profile-scoped dependency creation remains composition-root work in `AppDependencies.kt`.

### 7. Backup/import/external sync

`ServiceOperationsPort` is the last slice because it touches the most integration-sensitive code. It hides holiday sync, Excel import/parser coordination, Drive sync metadata and backup/import operations behind explicit operations while preserving raw backup JSON format and existing Google Drive behavior byte-for-byte/operation-for-operation. No cloud provider abstraction is added beyond what the UI consumes.

## Platform consumers outside `ShiftSalaryApp`

Wear sync, widgets, alarm receiver/ring activity and other Android entry points also instantiate stores directly. They are not screen composables and are not automatically rewritten in M7. M7 changes them only if a newly extracted shared boundary is required to keep behavior consistent; otherwise M16 remains the integration-hardening phase for those platform entry points.

## Exit structural target

At M7 verification:

- `ShiftSalaryApp` does not alias or invoke concrete `ShiftDayDao`, `ShiftTemplateDao`, `HolidayDao`, feature `*Store`, `HolidaySyncRepository`, or `ExcelScheduleImporter` instances;
- feature-facing ports are injected from the composition layer and backed by existing production objects;
- screen composables construct no persistence/service objects;
- no generic `Repository<T>`/service-locator/DI framework is introduced;
- M3 behavior fixtures and fresh JVM/build/lint remain green.
