# M11 Finance Vertical Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate Finance Summary, Payments/fact-vs-plan, Calculation, full payslip, and contextual payroll settings to the M9 Evolution design system without changing payroll arithmetic, persistence, payment semantics, or report data.

**Architecture:** Keep the M4–M7 data/state boundaries and all existing payroll result objects as the source of truth. M11 is a presentation migration: Evolution surfaces/tabs/action rows establish hierarchy, while `PayrollResult`, `PayrollDetailedResult`, payment dates, actual-payment persistence, visibility settings, and payroll actions remain unchanged. Finance Summary from M9 is the reference style; Payments, Calculation, Payslip, and payroll-settings surfaces converge on it.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing Evolution visual primitives, Compose Preview Screenshot Testing, JUnit structure/state/domain regression tests.

**Spec:** `docs/project/M8_FOCUSED_SCREEN_CONTRACTS.md` (`FIN-01`…`FIN-04`) plus `docs/superpowers/specs/2026-09-12-m9-variant-a-visual-foundation-design.md`.

## Global Constraints

- Payroll formulas, legislation logic, `PayrollResult`, and `PayrollDetailedResult` arithmetic must not change for UI reasons.
- Do not change Room schema/migrations, backup format, alarm semantics, Wear contracts, or dependencies.
- Preserve Finance subtabs: Summary / Calculation / Payments.
- Preserve period/workplace, Net/Gross, Compact/Detailed, visibility, diagnostics, PDF/export, actual-payment save, and settings actions.
- Per-shift payment mode must never fabricate advance/salary semantics.
- Error/warning emphasis for fact-vs-plan appears only outside configured tolerance.
- UI renders existing payroll/domain results; it does not recompute a second result.
- TDD RED→GREEN for each production slice; deterministic screenshot references update only after actual PNG inspection.

---

### Task 1: Add the M11 structural safety net

**Files:**
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M11FinanceVerticalSliceStructureTest.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/PaymentsTab.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt`
- Read: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsComponents.kt`

**Interfaces:**
- Consumes: current production composables and M9 `EvolutionSurface`, `EvolutionHeroCard`, `EvolutionTextTabs`, `EvolutionActionRow`.
- Produces: source-level regression contract proving the M11 presentation scope no longer relies on the legacy panel grammar in its primary surfaces.

- [ ] **Step 1: Write failing structure tests**

```kotlin
@Test
fun paymentsUsesEvolutionSurfaces() {
    val source = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/PaymentsTab.kt")
    assertTrue(source.contains("EvolutionSurface("))
    assertFalse(source.contains("appPanelBorderColor()"))
}

@Test
fun calculationUsesEvolutionGrammar() {
    val source = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt")
    assertTrue(source.contains("EvolutionHeroCard("))
    assertFalse(source.contains("AppExpressiveSurface("))
    assertFalse(source.contains("appPanelColor()"))
}

@Test
fun payslipAndPayrollSettingsUseEvolutionSurfaces() {
    val sheet = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt")
    val settings = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsComponents.kt")
    assertTrue(sheet.contains("EvolutionSurface("))
    assertFalse(sheet.contains("appPanelBorderColor()"))
    assertTrue(settings.contains("EvolutionSurface("))
    assertFalse(settings.contains("appPanelColor()"))
    assertFalse(settings.contains("appPanelBorderColor()"))
}
```

- [ ] **Step 2: Run the test and verify RED**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest --tests '*M11FinanceVerticalSliceStructureTest' --no-daemon
```

Expected: RED on Payments/Calculation/Payslip/settings legacy surface assertions; Finance Summary remains the already-migrated reference.

- [ ] **Step 3: Commit the RED safety net only after confirming the expected failures are attributable to the legacy surface grammar**

---

### Task 2: Migrate Finance Payments / fact-vs-plan presentation

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/PaymentsTab.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M11FinanceVerticalSliceStructureTest.kt`
- Regression: `FinanceFeatureStateTest`, `FinanceDataPortTest`, `PaymentEnhancementsTest`, `PayrollCharacterizationTest`

**Interfaces:**
- Consumes: `PayrollResult`, `PaymentDates`, `AdditionalPayment`, `ReportVisibilitySettings` and existing callbacks unchanged.
- Produces: the same Payments data/actions with Evolution hierarchy.

- [ ] **Step 1: Convert month header to the Evolution path**

Use the existing header contract exactly:

```kotlin
MonthHeader(
    currentMonth = currentMonth,
    onPrevMonth = onPrevMonth,
    onNextMonth = onNextMonth,
    onPickMonth = onPickMonth,
    useEvolution = true
)
```

- [ ] **Step 2: Replace `PaymentsPanelCard` with `EvolutionSurface(role = SOFT)` and zero default shadow**

```kotlin
EvolutionSurface(
    modifier = modifier.fillMaxWidth(),
    role = EvolutionSurfaceRole.SOFT,
    shape = RoundedCornerShape(appCardRadius()),
    shadowElevation = 0.dp
) { /* existing content unchanged */ }
```

- [ ] **Step 3: Convert `PaymentsStatTile`, `PaymentsReportTile`, and `PaymentsVisibilityTile` from outlined `Surface` cards to Evolution soft/accent action surfaces without changing values or callbacks**

Selected/emphasized money uses `EvolutionSurfaceRole.ACCENT`; ordinary blocks use `SOFT`. Semantic green stays limited to finance/success meaning, not every card.

- [ ] **Step 4: Run targeted GREEN**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*M11FinanceVerticalSliceStructureTest' \
  --tests '*FinanceFeatureStateTest' \
  --tests '*FinanceDataPortTest' \
  --tests '*PaymentEnhancementsTest' \
  --tests '*PayrollCharacterizationTest' --no-daemon
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/PaymentsTab.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M11FinanceVerticalSliceStructureTest.kt
git commit -m "feat: migrate M11 finance payments"
```

---

### Task 3: Migrate Calculation hierarchy and detailed summary

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M11FinanceVerticalSliceStructureTest.kt`
- Regression: `PayrollTabUiStateReducerTest`, `PayrollCharacterizationTest`, `ShiftHolidayPayrollMappingTest`

**Interfaces:**
- Consumes: existing `PayrollTabState`, `PayrollTabActions`, reducer state, period/workplace controls and payroll results unchanged.
- Produces: calmer period/workplace controls, a Finance-style hero result, Evolution view toggles, detailed summary and sticky totals.

- [ ] **Step 1: Put `PayrollTopHeader` on the Evolution surface/header path**

Use `EvolutionSurfaceRole.SOFT`, `MonthHeader(useEvolution = true)`, and `CalendarWorkplaceSwitcher(useEvolution = true)`; keep all period actions intact.

- [ ] **Step 2: Replace `PayrollCalculationOverviewCard` with `EvolutionHeroCard`**

Hero source remains `grossTotal` for Gross mode and `netAfterDeductions` for Net mode. Do not derive a new amount.

- [ ] **Step 3: Convert `PayrollDisplayOptionsBar`, period/amount/view controls, navigation buttons, and `PayrollStickyTotalsBar` to Evolution soft/accent surfaces**

Keep reducer transitions and labels unchanged.

- [ ] **Step 4: Migrate `SummaryCard`, `PayrollStatTile`, and `SummaryPanelCard` to Evolution surfaces while preserving expand/collapse and settings callbacks**

- [ ] **Step 5: Run targeted GREEN**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*M11FinanceVerticalSliceStructureTest' \
  --tests '*PayrollTabUiStateReducerTest' \
  --tests '*PayrollCharacterizationTest' \
  --tests '*ShiftHolidayPayrollMappingTest' --no-daemon
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollTabScreen.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSummaryComponents.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M11FinanceVerticalSliceStructureTest.kt
git commit -m "feat: migrate M11 finance calculation"
```

---

### Task 4: Migrate the full payslip and contextual payroll settings

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsComponents.kt`
- Modify only if shell styling is required: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsDialog.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M11FinanceVerticalSliceStructureTest.kt`
- Regression: backup/payroll/payment characterization tests.

**Interfaces:**
- Consumes: `PayrollDetailedResult`, `ReportVisibilitySettings`, export/diagnostics/settings callbacks and existing payroll settings values unchanged.
- Produces: document-like payslip density and contextual settings cards in the Evolution grammar.

- [ ] **Step 1: Convert `PayrollSheetCard` and section blocks to Evolution surfaces**

Main sheet uses `EvolutionSurfaceRole.FLOATING` or `SOFT` according to hierarchy; section blocks use `SOFT`. Keep row content, expansion, quantity/rate/amount, diagnostics, visibility, PDF and settings actions byte-for-byte semantically equivalent.

- [ ] **Step 2: Convert `SettingsSectionCard` and `CollapsibleSettingsSectionCard` to Evolution surfaces**

```kotlin
EvolutionSurface(
    modifier = Modifier.fillMaxWidth(),
    role = EvolutionSurfaceRole.SOFT,
    shape = cardShape,
    shadowElevation = 0.dp
) { /* existing title/summary/content */ }
```

- [ ] **Step 3: Convert payroll choice cards (`PayModeChoiceCard`, `NormModeChoiceCard`, `AnnualNormSourceChoiceCard`, `AdvanceModeChoiceCard`, `ExtraSalaryModeChoiceCard`) to one Evolution selected/unselected grammar**

Selected = `ACCENT`, unselected = `SOFT`; callbacks and setting values remain untouched.

- [ ] **Step 4: Run targeted GREEN**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*M11FinanceVerticalSliceStructureTest' \
  --tests '*BackupCompatibilityTest' \
  --tests '*PaymentEnhancementsTest' \
  --tests '*PayrollCharacterizationTest' --no-daemon
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/PayrollSheetComponents.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsComponents.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsDialog.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M11FinanceVerticalSliceStructureTest.kt
git commit -m "feat: migrate M11 payslip and payroll settings"
```

---

### Task 5: Expand deterministic M11 Finance visual qualification

**Files:**
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8FinanceVisualScreenshotTest.kt`
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8PayrollVisualScreenshotTest.kt`
- Add/update references only after inspection under `app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/`.

**Interfaces:**
- Consumes: the existing deterministic M8/M9 Finance and Payroll fixtures.
- Produces: a representative M11 visual matrix.

- [ ] **Step 1: Retain Summary Light/Dark/fontScale 1.3/PER_SHIFT and Calculation Light/Dark/fontScale 1.3**

- [ ] **Step 2: Add deterministic previews for Payments/fact-vs-plan, full Payslip, and one contextual payroll-settings state**

Payments fixture must include an actual-vs-expected mismatch outside tolerance; Payslip must contain multiple accrual and deduction rows; settings must show selected and unselected options.

- [ ] **Step 3: Run screenshot validation and require RED for changed/new references**

```bash
source scripts/android/vps-env.sh
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

- [ ] **Step 4: Inspect rendered/diff PNGs before updating references**

Reject if money hierarchy is weaker than M9 Summary, labels/amounts clip at 1.3, warning emphasis appears inside tolerance, per-shift mode invents advance/salary rows, dense payslip rows lose alignment, or Dark restores bright universal outlines.

- [ ] **Step 5: Update and validate references only after clean visual review**

```bash
source scripts/android/vps-env.sh
./gradlew :app:updateDebugScreenshotTest --no-daemon
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

- [ ] **Step 6: Commit**

```bash
git add app/src/screenshotTest app/src/screenshotTestDebug/reference
git commit -m "test: qualify M11 finance visual states"
```

---

### Task 6: M11 compatibility qualification and durable checkpoint

**Files:**
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify after qualification: `docs/project/CURRENT_STATE.md`

**Interfaces:**
- Consumes: Tasks 1–5 committed tree.
- Produces: M11 `COMPLETE / VERIFIED locally (integration pending)` checkpoint.

- [ ] **Step 1: Run focused finance/payroll behavior and boundary gate from a clean tree**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest \
  --tests '*FinanceFeatureStateTest' \
  --tests '*FinanceDataPortTest' \
  --tests '*M7FinanceBoundaryStructureTest' \
  --tests '*M8FinanceFocusedRedesignStructureTest' \
  --tests '*M8PayrollFocusedRedesignStructureTest' \
  --tests '*M9FinanceVariantAStructureTest' \
  --tests '*M11FinanceVerticalSliceStructureTest' \
  --tests '*PayrollTabUiStateReducerTest' \
  --tests '*PaymentEnhancementsTest' \
  --tests '*PayrollCharacterizationTest' --no-daemon
```

- [ ] **Step 2: Run unchanged-tree full phone qualification**

```bash
source scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:validateDebugScreenshotTest --no-daemon
git diff --check
git status --short --branch
```

- [ ] **Step 3: Record exact commits, job IDs, visual findings, APK hash, and explicit no-domain-change statement in `WORK_PROGRESS.md` and `CURRENT_STATE.md`**

- [ ] **Step 4: Commit checkpoint**

```bash
git add docs/project/WORK_PROGRESS.md docs/project/CURRENT_STATE.md
git commit -m "docs: checkpoint M11 finance vertical slice"
```

Do not push, merge, release, or deploy without a new owner gate.
