# M5 Navigation Inventory

Date: 2026-09-11
Canonical baseline: `d7af9ec4104947e3c64cfeab7aaacd711dd9d760`
Milestone: **M5 — Navigation Rewrite**

## Purpose

This inventory is the behavioral map that M5 must preserve. M5 replaces root navigation state, not the information architecture. The visible bottom tabs, labels, screen contents and existing user flows stay 1:1 until M8.

## Root tabs — preserve exactly in M5

The current `BottomTab` contract contains eight destinations:

1. `CALENDAR` — «Календарь»
2. `TODAY` — «Сегодня»
3. `ASSISTANT` — «ИИ»
4. `NOTES` — «Заметки»
5. `FINANCE` — «Финансы»
6. `ALARMS` — «Будильники»
7. `SHIFTS` — «Смены»
8. `SETTINGS` — «Настройки»

`FinanceSubTab` remains a typed sub-selection inside `FINANCE`; M5 does not redesign Finance.

### External/widget entry compatibility

The current launcher accepts `EXTRA_OPEN_TAB`. Compatibility that must remain:

- `PAYROLL` -> `FINANCE` + `FinanceSubTab.PAYROLL`
- `PAYMENTS` -> `FINANCE` + `FinanceSubTab.PAYMENTS`
- a valid `BottomTab.name` -> that tab
- invalid/blank values -> existing default behavior (`CALENDAR`, `SUMMARY`)

## Root fullscreen routes — migrate in M5

These 22 booleans are root navigation, because each renders an `AnimatedFullscreenOverlay` and participates in root back handling:

| Current flag | Typed M5 screen |
| --- | --- |
| `showMonthlyReport` | `MONTHLY_REPORT` |
| `showAppHealthCheck` | `APP_HEALTH_CHECK` |
| `showAppEventLog` | `APP_EVENT_LOG` |
| `showReportHistory` | `REPORT_HISTORY` |
| `showQuickActionsSettings` | `QUICK_ACTIONS_SETTINGS` |
| `showQuickStartGuide` | `QUICK_START_GUIDE` |
| `showReportCenter` | `REPORT_CENTER` |
| `showPayrollDiagnostics` | `PAYROLL_DIAGNOSTICS` |
| `showReportVisibilitySettings` | `REPORT_VISIBILITY_SETTINGS` |
| `showPayrollSettings` | `PAYROLL_SETTINGS` |
| `showAppearanceSettings` | `APPEARANCE_SETTINGS` |
| `showCurrentParameters` | `CURRENT_PARAMETERS` |
| `showProfilesScreen` | `PROFILES` |
| `showManualHolidaysScreen` | `MANUAL_HOLIDAYS` |
| `showBackupRestoreScreen` | `BACKUP_RESTORE` |
| `showExcelImportScreen` | `EXCEL_IMPORT` |
| `showWidgetSettingsScreen` | `WIDGET_SETTINGS` |
| `showAdditionalPaymentsScreen` | `ADDITIONAL_PAYMENTS` |
| `showDeductionsScreen` | `DEDUCTIONS` |
| `showDeductionEditorScreen` | `DEDUCTION_EDITOR` |
| `showShiftTemplateEditDialog` | `SHIFT_TEMPLATE_EDITOR` |
| `showNoteEditor` | `NOTE_EDITOR` |

### Existing nesting that requires a real stack

`DEDUCTIONS -> DEDUCTION_EDITOR` intentionally keeps the deductions screen alive underneath the editor. The old implementation represents this with two true booleans and a priority `BackHandler`. M5 must represent it as a typed stack and pop the editor first.

Most other cross-fullscreen transitions explicitly close the old screen before opening the new one and are therefore **replace** operations, for example:

- `REPORT_CENTER -> MONTHLY_REPORT`
- `REPORT_CENTER -> REPORT_HISTORY`
- `APP_HEALTH_CHECK -> BACKUP_RESTORE`
- `APP_HEALTH_CHECK -> REPORT_HISTORY`
- `QUICK_START_GUIDE -> PAYROLL_SETTINGS`
- `QUICK_START_GUIDE -> SHIFT_TEMPLATE_EDITOR`

`QUICK_START_GUIDE -> CALENDAR/ALARMS` closes the fullscreen screen and changes the selected tab.

## Modal/feature state — explicitly NOT root navigation in M5

The following stays in the existing feature state until M6 because it is a dialog, picker, confirmation, editor payload or temporary interaction rather than a root route:

- `selectedDate` / `ShiftPickerDialog`
- `dayAssignmentsPreviewDate` / `DayAssignmentsDialog`
- `quickPickerOpen`
- `showAdditionalPaymentDialog`
- `showPatternListDialog`
- `showPatternEditDialog`
- `showPatternApplyDialog`
- `showPatternQuickPicker`
- `showPatternPreviewDialog`
- `showClearMonthConfirm`
- `showClearAllCalendarConfirm`
- `showManualHolidayDialog`
- `showWorkplaceRenameDialog`
- `showPostUpdateCheckDialog`
- editor payloads such as `editingNoteId`, `editingDeductionId`, `editingShiftTemplateCode`, draft dates/workplaces and pattern IDs

M5 may continue to update those payload variables immediately before opening a typed root screen. Moving their ownership belongs to M6.

## M5 target navigation semantics

M5 introduces one typed root state:

```kotlin
data class AppNavigationState(
    val selectedTab: BottomTab,
    val financeSubTab: FinanceSubTab,
    val screenStack: List<AppScreen>
)
```

Required operations are pure and testable:

- select a bottom tab;
- select Finance sub-tab;
- open/push a fullscreen screen;
- replace one fullscreen screen with another;
- close a named fullscreen screen;
- pop the top fullscreen screen for system Back;
- restore/save the state through Compose `rememberSaveable`;
- parse the old widget/external aliases.

Duplicate copies of the same fullscreen screen are not allowed in the stack. Reopening an existing screen moves that screen to the top rather than duplicating it.

## M5 exit criteria

M5 is complete only when:

- the 8-tab visible structure is unchanged;
- widget aliases still route correctly;
- all 22 root fullscreen booleans are gone from `ShiftSalaryApp`;
- root Back handling uses the typed stack;
- intentional nesting (especially Deductions -> editor) is preserved;
- modal/feature booleans listed above remain intentionally untouched;
- all M3 tests pass plus new navigation tests;
- app/Wear build and lint gates are green;
- independent review finds no actionable navigation regression.
