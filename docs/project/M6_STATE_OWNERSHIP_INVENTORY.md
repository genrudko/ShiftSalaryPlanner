# M6 State Ownership Inventory

Date: 2026-09-11
Canonical baseline: `1858b68aa06ef7416d147df1bca5f55f247668b0`
Milestone: **M6 — Feature State Ownership**

## Purpose

M6 moves feature-specific UI/workflow state out of `ShiftSalaryApp` without changing persistence, payroll, navigation, visible information architecture, or user workflows. M5 already centralized root navigation; M6 now gives the remaining interaction state explicit feature owners.

## Baseline measurement

At the M6 baseline, `ShiftSalaryApp` contains **67 root-level remembered mutable variables**:

- 51 created with `rememberSaveable`;
- 16 created with plain `remember`;
- the count includes the M5 `navigationState`;
- it does not count derived `val` values or state already owned inside child screens such as `ShiftAlarmsTabUiState`, `PayrollTabUiState`, and `TemplatesScreenUiState`.

The M6 structural target is **3 root remembered `var` values**:

1. `currentMonth` — deliberately shared by Calendar, Finance/Payments and production-calendar workflows;
2. `activeWorkplaceId` — deliberately shared by Calendar, Shifts and Settings/payroll entry;
3. `navigationState` — the M5 root navigation owner.

Those are the smallest sensible common owners. M6 must not hide them inside a generic `AppState` just to reduce a count.

## Ownership map

### Calendar feature owner

New owner: `CalendarFeatureState` in `ui/calendar`.

Moves from root:

- transient, intentionally **not saveable**: `selectedDate`, `dayAssignmentsPreviewDate`;
- saveable: `quickPickerOpen`, `activeBrushCode`, `isLegendExpanded`, `clearRangeModeActive`, `clearRangeStartIso`, `pendingClearRangeStartIso`, `pendingClearRangeEndIso`, `showClearMonthConfirm`, `showClearAllCalendarConfirm`, `calendarWorkplaceFilterId`.

The default workplace-filter key behavior must remain equivalent to the existing `rememberSaveable(appearanceSettings.calendarDefaultWorkplaceMode.name)` call. Changing the default mode still resets that feature state to the corresponding default filter; rotation restores the saveable fields; the two dialog dates remain transient and therefore close on recreation as they do today.

### Pattern feature owner

New owner: `PatternFeatureState` in `ui/patterns`.

Moves from root:

- `showPatternListDialog`;
- `showPatternEditDialog` / `editingPatternId`;
- `showPatternApplyDialog` / `applyingPatternId`;
- `showPatternQuickPicker`;
- `activePatternId`;
- `patternRangeStartIso`;
- `pendingPatternRangeStartIso`;
- `pendingPatternRangeEndIso`;
- `showPatternPreviewDialog`.

Pattern management is shared by the Shifts screen and Calendar quick-apply flow, so it is its own feature boundary rather than being nested under either Calendar or Shifts.

### Finance feature owner

New owner: `FinanceFeatureState` in `ui/finance`.

Moves saveable state:

- `showAdditionalPaymentDialog`;
- `editingAdditionalPaymentId`;
- `editingDeductionId`;
- `isSummaryExpanded`;
- `payrollPeriodModeName`;
- `payrollWorkplaceFilterId`;
- `settingsWorkplaceId`;
- `payrollSelectedYear`;
- `payrollRangeStartIso`;
- `payrollRangeEndIso`.

Moves transient, intentionally non-saveable export payloads:

- `pendingReportCsvContent` / `pendingReportCsvFileName`;
- `pendingReportPdfBytes` / `pendingReportPdfFileName`.

The initial year/range values still derive from the initial `currentMonth`; once created, they retain the same save/restore behavior as today. Existing payroll formulas and stores are not changed.

### Shifts feature owner

New owner: `ShiftFeatureState` in `ui/templates`.

Moves:

- `editingShiftTemplateCode`;
- `creatingSystemStatus`;
- `templateModeName`.

Opening a new shift, new system status, or existing shift becomes an explicit state-holder operation. Closing the editor clears the same payloads as the current root back/toolbar behavior.

### Notes feature owner

New owner: `NotesFeatureState` in `ui/notes`.

Moves:

- `editingNoteId`;
- `noteDraftDateIso`;
- `noteDraftWorkplaceId`;
- `noteDraftShiftCode`.

All four remain saveable. Calendar, Today, Notes and DayAssignments may initiate the editor, but the draft itself belongs to Notes.

### Settings feature owner

New owner: `SettingsFeatureState` in `ui/settings`.

Moves saveable settings/workflow state:

- `isHolidaySyncing`;
- `holidaySyncMessage`;
- `showManualHolidayDialog`;
- `editingManualHolidayDate`;
- `showWorkplaceRenameDialog`;
- `customFontStatusMessage`.

A separate `WidgetSettingsRuntimeState` in `ui/settings/widget` owns `widgetSettingsRefreshToken` and is recreated with `activeProfileId`, preserving the current `remember(activeProfileId)` reset behavior without resetting the saveable Settings workflow state.

### Backup/import/service workflow owner

New owner: `ServiceWorkflowState` in `ui/settings`.

Moves saveable state:

- `showPostUpdateCheckDialog`;
- `excelImportStatusMessage`;
- `pendingExcelFileName`;
- `backupRestoreStatusMessage`;
- `autoUploadCheckedForAccount`.

Moves transient, intentionally non-saveable state:

- `pendingBackupJsonContent` / `pendingBackupFileName`;
- `pendingExcelFileBytes`;
- `excelImportPreview`;
- `excelImportCandidates`;
- `googleSignedInAccount`.

This owner is UI/workflow state only. Google Drive clients, backup coordinators, Excel parser/import services and stores remain M4 dependencies and are **not** wrapped in a new data abstraction until M7.

### Alarm runtime owner

New owner: `AlarmRuntimeState` in `ui/alarms`.

Moves:

- `alarmPermissionRefreshToken`;
- `shiftAlarmRescheduleResult`.

The holder is recreated with `activeProfileId`, preserving the current profile-keyed `remember(activeProfileId)` semantics. The large alarm editor state already lives in `ShiftAlarmsTabUiState` and is not duplicated.

## Save/restore contract

M6 must preserve whether each existing value survives recreation:

- values currently using `rememberSaveable` stay saveable;
- values currently using plain `remember` stay transient;
- profile-keyed runtime state continues resetting when `activeProfileId` changes;
- Calendar's default-workplace-mode key continues resetting the Calendar feature owner when that setting changes.

Feature holders may use Compose `Saver`/`listSaver`, `mutableStateOf`, and focused `remember...State()` factories. A holder must not persist data that was previously transient merely because it is convenient.

## Event ownership rule

M6 moves **UI-state transitions** into the feature owner: opening/closing editors and dialogs, choosing filters/modes, range-selection state, clearing draft payloads, and similar interaction rules.

M6 does **not** move concrete persistence/network/platform side effects behind new interfaces. Calls to Room DAOs, settings stores, alarm scheduler, Drive, file launchers and import services may still be wired from `ShiftSalaryApp` in M6. Their dependency-direction cleanup is M7.

This keeps the milestones distinct:

```text
M6: who owns UI/workflow state?
M7: what stable operation does that owner call instead of a concrete DAO/store/service?
```

## Existing state boundaries to reuse

M6 must reuse rather than replace the state models already present in the repository:

- `AppNavigationState` from M5;
- `ShiftAlarmsTabUiState` / `ShiftAlarmsTabState`;
- `PayrollTabUiState` / `PayrollTabState`;
- `TemplatesScreenUiState` / `TemplatesScreenState`.

The new feature holders own root workflow state around those screen contracts; they do not create duplicate copies of screen-local state.

## Exit criteria

M6 is complete only when:

- `ShiftSalaryApp` has exactly the three deliberate root remembered mutable owners: `currentMonth`, `activeWorkplaceId`, `navigationState`;
- feature-specific state is accessed through focused owners listed above;
- there is no new generic `AppState`/god-state container;
- all current saveable/transient/profile-keyed semantics are preserved;
- root navigation remains exclusively M5 `AppNavigationState`;
- no payroll formula, Room schema, backup schema or visible UI/IA intentionally changes;
- targeted state-transition tests are green;
- full JVM tests, phone/Wear debug builds and both lint gates are green;
- independent review reports no actionable behavior regression.
