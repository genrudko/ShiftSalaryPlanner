# M9 Variant A Visual Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the real Compose Calendar and Finance Summary immediately recognizable as the approved Variant A / Evolution design family by introducing a reusable visual foundation and migrating only the two reference screens first.

**Architecture:** Add semantic Evolution tokens and small reusable primitives under `ui/theme` and `ui/common`, then migrate Calendar and Finance Summary without changing schedule or payroll behavior. Existing appearance customization remains authoritative; `EXPRESSIVE` maps to the new Variant A geometry/surface grammar, `CLASSIC` remains a compatibility mode, and `EXPRESSIVE_GLASS` reuses the same geometry with a different treatment. Screenshot fixtures are the visual acceptance harness and must be inspected before new goldens are accepted.

**Tech Stack:** Android, Jetpack Compose Material 3, Kotlin 2.2.10, AGP 9.2.1, existing Compose Preview Screenshot Testing, existing Manrope resource, existing M8 structure/characterization tests.

**Spec:** `docs/superpowers/specs/2026-09-12-m9-variant-a-visual-foundation-design.md`

## Global Constraints

- Variant A / Evolution is the approved visual direction: expressive, friendly, readable, calendar-first.
- Shift colors are semantic user data and must never be recolored by the visual system.
- Finance green is semantic; the whole app must not become green.
- Default expressive cards are borderless; outlines remain for selection/focus/input/high-contrast/special calendar states.
- Preserve Light/Dark/Auto/Schedule, palette selection, custom colors, per-section fonts, density, contrast, corner style, animation speed, Classic/Expressive/Expressive Glass.
- No payroll arithmetic/domain/repository changes.
- No schedule assignment semantic changes.
- No Room/backup/network/dependency modernization.
- Calendar and Finance Summary are the only production reference-screen migrations in this plan.
- Payroll Calculation, More, Today and secondary screens remain unchanged except where a shared primitive must keep compatibility.
- Three-destination navigation chrome may be implemented as a reusable primitive in this plan; production routing remains on the existing shell until the More destination exists, so no deep-link behavior is broken by visual-foundation work.
- Every visual acceptance step uses real Compose PNGs in Light, Dark and `fontScale=1.3`; tests alone do not approve a screen.
- No push/merge/release/deploy.

---

### Task 1: Introduce Variant A semantic visual tokens and core primitives

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/theme/EvolutionVisualTokens.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/EvolutionComponents.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/AppExpressiveSurfaces.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/theme/Theme.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M9EvolutionVisualFoundationStructureTest.kt`

**Interfaces:**
- Consumes: `LocalAppAppearanceSettings`, `AppVisualStyleMode`, current `MaterialTheme.colorScheme`, `appCornerRadius`, `appScaledSpacing`.
- Produces:
  - `EvolutionColorRoles`
  - `@Composable fun evolutionColorRoles(): EvolutionColorRoles`
  - `enum class EvolutionSurfaceRole { PRIMARY, SOFT, ACCENT, FLOATING, HERO }`
  - `@Composable fun EvolutionSurface(...)`
  - `@Composable fun EvolutionIconTile(...)`
  - `@Composable fun EvolutionHeroCard(...)`
  - `@Composable fun EvolutionActionRow(...)`
  - `@Composable fun EvolutionTextTabs(...)`
  - `@Composable fun EvolutionOptionControl(...)`
  - `@Composable fun EvolutionBottomNavigation(...)`
  - compatibility delegation from `AppExpressiveSurface` in non-Classic modes.

- [ ] **Step 1: Write the failing structure test for the token/primitives contract**

Create `M9EvolutionVisualFoundationStructureTest.kt` with assertions equivalent to:

```kotlin
@Test
fun expressiveFoundationDefinesVariantATokensAndPrimitives() {
    val tokens = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/theme/EvolutionVisualTokens.kt")
    val components = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/EvolutionComponents.kt")
    assertTrue(tokens.contains("data class EvolutionColorRoles"))
    assertTrue(tokens.contains("brandPrimary"))
    assertTrue(tokens.contains("financePositive"))
    assertTrue(tokens.contains("appBackground"))
    assertTrue(components.contains("fun EvolutionSurface("))
    assertTrue(components.contains("fun EvolutionIconTile("))
    assertTrue(components.contains("fun EvolutionHeroCard("))
    assertTrue(components.contains("fun EvolutionActionRow("))
    assertTrue(components.contains("fun EvolutionTextTabs("))
}

@Test
fun expressiveFoundationDoesNotDefaultEveryCardToOutline() {
    val components = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/EvolutionComponents.kt")
    assertTrue(components.contains("border: BorderStroke? = null"))
    assertFalse(components.contains("border = BorderStroke(1.dp, roles"))
}
```

- [ ] **Step 2: Run the new structure test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests '*M9EvolutionVisualFoundationStructureTest' --no-daemon
```

Expected: FAIL because the new token/component files and symbols do not exist.

- [ ] **Step 3: Implement semantic tokens with appearance-aware role mapping**

Create `EvolutionVisualTokens.kt` with the real role model. The implementation must derive from `MaterialTheme.colorScheme` and current appearance mode rather than hard-code one fixed palette. The light default expressive grammar should bias toward blue/indigo brand roles and reserve green for finance:

```kotlin
data class EvolutionColorRoles(
    val appBackground: Color,
    val surfacePrimary: Color,
    val surfaceSoft: Color,
    val surfaceAccent: Color,
    val surfaceFloating: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val financePositive: Color,
    val warningAccent: Color,
    val dangerAccent: Color,
    val contentPrimary: Color,
    val contentSecondary: Color
)

@Composable
fun evolutionColorRoles(): EvolutionColorRoles {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return EvolutionColorRoles(
        appBackground = if (dark) lerp(scheme.background, Color(0xFF101629), 0.32f)
            else lerp(scheme.background, Color(0xFFF5F7FF), 0.68f),
        surfacePrimary = if (dark) lerp(scheme.surface, Color.White, 0.045f) else Color.White,
        surfaceSoft = if (dark) lerp(scheme.surface, scheme.primary, 0.08f)
            else lerp(Color.White, Color(0xFFEEF1FF), 0.58f),
        surfaceAccent = if (dark) lerp(scheme.surface, scheme.primary, 0.15f)
            else lerp(Color.White, Color(0xFFE9EDFF), 0.72f),
        surfaceFloating = if (dark) lerp(scheme.surface, Color.White, 0.065f) else Color.White,
        brandPrimary = if (dark) lerp(scheme.primary, Color(0xFF9FB0FF), 0.55f)
            else lerp(scheme.primary, Color(0xFF536DFE), 0.62f),
        brandSecondary = if (dark) Color(0xFFB9A7FF) else Color(0xFF8468E8),
        financePositive = if (dark) Color(0xFF69D8A0) else Color(0xFF168C62),
        warningAccent = if (dark) Color(0xFFFFC46B) else Color(0xFFE58B18),
        dangerAccent = scheme.error,
        contentPrimary = scheme.onBackground,
        contentSecondary = scheme.onSurfaceVariant
    )
}
```

If the current selected palette/custom colors supply a usable primary/secondary, blend toward them rather than replacing them. High-contrast mode may re-enable explicit outlines through component call sites.

- [ ] **Step 4: Implement reusable Evolution primitives**

Create `EvolutionComponents.kt`. Keep each primitive small and composable. Required signatures:

```kotlin
enum class EvolutionSurfaceRole { PRIMARY, SOFT, ACCENT, FLOATING, HERO }

enum class EvolutionIconTone { BRAND, SECONDARY, FINANCE, WARNING, DANGER, NEUTRAL }

@Composable
fun EvolutionSurface(
    modifier: Modifier = Modifier,
    role: EvolutionSurfaceRole = EvolutionSurfaceRole.PRIMARY,
    shape: Shape = RoundedCornerShape(appCornerRadius(22.dp)),
    border: BorderStroke? = null,
    shadowElevation: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
)

@Composable
fun EvolutionIconTile(
    icon: ImageVector? = null,
    glyph: String? = null,
    contentDescription: String?,
    tone: EvolutionIconTone,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
)

@Composable
fun EvolutionHeroCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconDescription: String,
    semanticTone: EvolutionIconTone,
    supportingText: String?,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null
)

data class EvolutionTabItem<T>(val value: T, val label: String)

@Composable
fun <T> EvolutionTextTabs(
    items: List<EvolutionTabItem<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
)
```

`EvolutionSurface` rules:
- `PRIMARY`, `SOFT`, `ACCENT` default `border = null`;
- `FLOATING` gets subtle elevation;
- `HERO` may render a restrained gradient/wave overlay;
- `CLASSIC` compatibility is handled by `AppExpressiveSurface`, not by duplicating old outlines inside the new primitive.

- [ ] **Step 5: Convert `AppExpressiveSurface` into a compatibility wrapper for non-Classic modes**

Keep the public API stable because secondary screens already depend on it. For `CLASSIC`, preserve current behavior. For `EXPRESSIVE` / `EXPRESSIVE_GLASS`, delegate role/geometry to the new surface model:

```kotlin
val mode = LocalAppAppearanceSettings.current.visualStyleMode
if (mode == AppVisualStyleMode.CLASSIC) {
    LegacyExpressiveSurface(...)
} else {
    EvolutionSurface(
        modifier = modifier,
        role = tone.toEvolutionRole(),
        shape = shape,
        border = border,
        shadowElevation = shadowElevation,
        content = content
    )
}
```

`EXPRESSIVE_GLASS` may add the existing liquid/glass wash inside the same Evolution geometry; it must not restore a universal white outline.

- [ ] **Step 6: Add a reusable three-destination navigation chrome primitive without changing production routing**

Implement `EvolutionBottomNavigation` for exactly three visual items. Do not replace `BottomTab.entries` or deep-link routing in this task. The primitive is available for screenshots and future More-shell wiring:

```kotlin
data class EvolutionNavItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector
)
```

Selected state uses `brandPrimary`; the container is a floating borderless surface with generous touch targets and readable labels.

- [ ] **Step 7: Run targeted tests to verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests '*M9EvolutionVisualFoundationStructureTest' --no-daemon
```

Expected: PASS.

- [ ] **Step 8: Run compile + existing M8 visual structure tests**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests '*M8CalendarFocusedRedesignStructureTest' \
  --tests '*M8FinanceFocusedRedesignStructureTest' \
  --tests '*M8PayrollFocusedRedesignStructureTest' \
  :app:assembleDebug --no-daemon
```

Expected: PASS; no production behavior regressions.

- [ ] **Step 9: Commit the visual foundation**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/theme/EvolutionVisualTokens.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/EvolutionComponents.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/AppExpressiveSurfaces.kt \
        app/src/main/java/com/vigilante/shiftsalaryplanner/ui/theme/Theme.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M9EvolutionVisualFoundationStructureTest.kt
git commit -m "feat: add M9 Variant A visual foundation"
```

---

### Task 2: Migrate Calendar reference screen to Variant A primitives

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarHeaderComponents.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt`
- Modify: `app/src/test/java/com/vigilante/shiftsalaryplanner/M8CalendarFocusedRedesignStructureTest.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M9CalendarVariantAStructureTest.kt`
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTest.kt`
- Update after visual inspection only: `app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTestKt/*.png`

**Interfaces:**
- Consumes: `EvolutionSurface`, `EvolutionIconTile`, semantic roles from Task 1; existing `DayCell` schedule inputs and M8 interaction state.
- Produces: real Calendar month reference screen with Variant A chrome while keeping the exact M8 day-assignment semantics.

- [ ] **Step 1: Write Calendar Variant A structure tests**

Required assertions:

```kotlin
@Test
fun calendarUsesEvolutionReferencePrimitives() {
    val tab = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt")
    val header = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarHeaderComponents.kt")
    val quick = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/QuickShiftBar.kt")
    assertTrue(tab.contains("EvolutionSurface("))
    assertTrue(header.contains("EvolutionIconTile("))
    assertTrue(quick.contains("EvolutionSurface("))
    assertFalse(quick.contains(".border(1.dp, appPanelBorderColor()"))
}

@Test
fun calendarCellsKeepShiftColorAsDataAndUseSelectionOverlay() {
    val cell = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt")
    assertTrue(cell.contains("assignmentBackgroundColors"))
    assertTrue(cell.contains("EvolutionShiftIdentityBadge("))
    assertTrue(cell.contains("isSelected"))
    assertTrue(cell.contains("isToday"))
}
```

- [ ] **Step 2: Run Calendar M9 structure test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests '*M9CalendarVariantAStructureTest' --no-daemon
```

Expected: FAIL because Calendar still uses legacy visual primitives.

- [ ] **Step 3: Recompose Calendar page hierarchy without changing interaction callbacks**

Keep all existing `CalendarTab(...)` parameters. Change only visual composition:
- use the Evolution background role;
- make month navigation lightweight and spacious;
- place workplace/profile context in a soft clean surface with semantic icon tile(s);
- avoid additional outer outlined cards around the month grid;
- section spacing must be larger than cell/internal spacing.

Do not remove or rename `onDayClick`, `onDayLongPress`, brush/pattern/clear callbacks, selected-date detail callbacks, profile/workplace filters, or audit/note flows.

- [ ] **Step 4: Refine `DayCell` into neutral day chrome + first-class shift badge**

Preserve all current segment calculations. Change visual layering so empty/current-month cells use a neutral/tinted surface while shift identity remains dominant. Add an internal reusable badge helper:

```kotlin
@Composable
private fun EvolutionShiftIdentityBadge(
    glyph: String,
    iconVector: ImageVector?,
    workplaceBadge: String?,
    shiftColor: Color,
    compactMode: Boolean,
    modifier: Modifier = Modifier
)
```

Rules:
- text/emoji/Material icon all remain supported;
- multi-workplace days still segment by assignment color;
- selection = brand blue/indigo ring/overlay, not a recolor of shift data;
- today = separate compact indicator;
- note/override/holiday remain secondary metadata;
- no universal 1dp outline for every normal day.

- [ ] **Step 5: Restyle selected-day detail and Quick Shift Bar**

Use `EvolutionSurface(FLOATING/SOFT)` without nested legacy border. Shift template buttons keep their own template colors. Utility actions use semantic icon tiles / soft action buttons. The bar must remain usable at `fontScale=1.3` and in compact mode.

- [ ] **Step 6: Run Calendar behavior + structure tests**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests '*CalendarInteractionStateTest' \
  --tests '*CalendarPatternWorkflowStateTest' \
  --tests '*M8CalendarFocusedRedesignStructureTest' \
  --tests '*M9CalendarVariantAStructureTest' --no-daemon
```

Expected: PASS.

- [ ] **Step 7: Render Calendar Light/Dark/1.3/range-preview screenshots without updating goldens**

Run screenshot validation first against existing M8 goldens. Expected: RED visual diffs because the design intentionally changed.

```bash
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

Capture the actual/diff report. Inspect all Calendar states before updating references.

- [ ] **Step 8: Perform visual review against Variant A target**

Reject the render if any are true:
- calendar still reads as outlined gray/green legacy panels;
- shift codes become harder to scan than M8;
- multiple workplaces lose clear color separation;
- selected/today/note/override collide;
- Quick Shift looks like a legacy settings panel;
- large font clips or overlaps.

Fix code and rerender until none apply.

- [ ] **Step 9: Update Calendar screenshot references and validate**

After visual approval only:

```bash
./gradlew :app:updateDebugScreenshotTest --no-daemon
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

Expected: PASS.

- [ ] **Step 10: Commit Calendar reference migration**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M9CalendarVariantAStructureTest.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M8CalendarFocusedRedesignStructureTest.kt \
        app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTest.kt \
        app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/M8CalendarVisualScreenshotTestKt
git commit -m "feat: migrate M9 Calendar to Variant A"
```

---

### Task 3: Migrate Finance Summary reference screen to Variant A primitives

**Files:**
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt`
- Modify: `app/src/test/java/com/vigilante/shiftsalaryplanner/M8FinanceFocusedRedesignStructureTest.kt`
- Create: `app/src/test/java/com/vigilante/shiftsalaryplanner/M9FinanceVariantAStructureTest.kt`
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8FinanceVisualScreenshotTest.kt`
- Update after visual inspection only: `app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/M8FinanceVisualScreenshotTestKt/*.png`

**Interfaces:**
- Consumes: Task 1 Evolution primitives; current `FinanceSummaryState`; M8 Finance hierarchy and correct `netAfterDeductions` semantics.
- Produces: Variant A Finance Summary; `PAYROLL` and `PAYMENTS` contents remain functionally routed as before.

- [ ] **Step 1: Write Finance Variant A structure test**

```kotlin
@Test
fun financeSummaryUsesVariantAHeroRowsAndLightweightTabs() {
    val finance = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt")
    assertTrue(finance.contains("EvolutionTextTabs("))
    assertTrue(finance.contains("EvolutionHeroCard("))
    assertTrue(finance.contains("EvolutionActionRow("))
    assertTrue(finance.contains("EvolutionIconTone.FINANCE"))
    assertFalse(finance.contains("FinanceKeyMetrics("))
}

@Test
fun financeHeroKeepsCorrectPayableAmount() {
    val finance = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt")
    assertTrue(finance.contains("state.payroll.netAfterDeductions"))
}
```

- [ ] **Step 2: Run Finance M9 structure test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests '*M9FinanceVariantAStructureTest' --no-daemon
```

Expected: FAIL because the current Summary still uses M8-specific hero/metric/card primitives.

- [ ] **Step 3: Replace heavy sub-tab switcher with lightweight text tabs**

Use:

```kotlin
EvolutionTextTabs(
    items = listOf(
        EvolutionTabItem(FinanceSubTab.SUMMARY, "Сводка"),
        EvolutionTabItem(FinanceSubTab.PAYROLL, "Расчёт"),
        EvolutionTabItem(FinanceSubTab.PAYMENTS, "Выплаты")
    ),
    selected = selectedSubTab,
    onSelected = onSelectSubTab
)
```

The active tab uses brand blue/indigo. Do not wrap tabs in a heavy outlined panel.

- [ ] **Step 4: Build the real Variant A finance hero**

Replace `FinancePayableHeroCard` internals with `EvolutionHeroCard` and finance semantic tone. Hero must contain:
- money/finance icon tile;
- label `Ожидается на руки` for normal twice-monthly mode, or correct per-shift wording;
- value `formatFinanceMoney(state.payroll.netAfterDeductions)`;
- low-weight supporting text;
- restrained decorative wave/gradient;
- no table chrome.

- [ ] **Step 5: Replace `FinanceKeyMetrics` block with expressive action/value rows**

Use four rows in the Summary hierarchy where applicable:

```text
Начислено        gross total
НДФЛ             tax
Аванс            advance or per-shift equivalent
Остаток          remaining salary/payable equivalent
```

Use `EvolutionActionRow` with distinct semantic icon tiles. Preserve per-shift semantics from M8: never fabricate advance/salary rows for `PER_SHIFT`; use shift count/hours/payment rows instead.

- [ ] **Step 6: Add a prominent `Расчётный лист` row and demote context panels**

`Расчётный лист` should be an explicit clean row with icon tile and chevron calling `onOpenPayroll`. Payment plan / actual-vs-expected remain available but visually secondary using `SOFT` surfaces or rows, not equal-weight bordered cards.

- [ ] **Step 7: Run Finance + payroll regression tests**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests '*M8FinanceFocusedRedesignStructureTest' \
  --tests '*M9FinanceVariantAStructureTest' \
  --tests '*FinanceFeatureStateTest' \
  --tests '*PayrollCalculatorTest' \
  --tests '*PayrollCharacterizationTest' --no-daemon
```

Expected: PASS; no payroll arithmetic changes.

- [ ] **Step 8: Render Finance Light/Dark/1.3/PER_SHIFT before updating goldens**

Run validation against old references and inspect actual/diff images. Expected visual RED until references are intentionally updated.

- [ ] **Step 9: Perform visual review against Variant A**

Reject if:
- entire screen is tinted finance green;
- hero is just a colored rectangle with text;
- rows look like a table dump;
- tabs still look like form segmented buttons;
- per-shift mode shows advance/salary semantics;
- large font clips or causes row collisions;
- Dark mode restores universal bright borders.

Fix and rerender until none apply.

- [ ] **Step 10: Update Finance screenshot goldens and validate**

```bash
./gradlew :app:updateDebugScreenshotTest --no-daemon
./gradlew :app:validateDebugScreenshotTest --no-daemon
```

Expected: PASS.

- [ ] **Step 11: Commit Finance reference migration**

```bash
git add app/src/main/java/com/vigilante/shiftsalaryplanner/ui/finance/FinanceTab.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M8FinanceFocusedRedesignStructureTest.kt \
        app/src/test/java/com/vigilante/shiftsalaryplanner/M9FinanceVariantAStructureTest.kt \
        app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M8FinanceVisualScreenshotTest.kt \
        app/src/screenshotTestDebug/reference/com/vigilante/shiftsalaryplanner/M8FinanceVisualScreenshotTestKt
git commit -m "feat: migrate M9 Finance Summary to Variant A"
```

---

### Task 4: Joint visual acceptance and compatibility qualification

**Files:**
- Modify: `docs/project/WORK_PROGRESS.md`
- Modify after acceptance: `docs/project/CURRENT_STATE.md`
- Read/test: all M8 Calendar/Finance/Payroll screenshot references and behavior suites.

**Interfaces:**
- Consumes: Tasks 1–3.
- Produces: a durable M9 Visual Foundation checkpoint proving that Calendar and Finance Summary share one recognizable Variant A language.

- [ ] **Step 1: Export the final real Compose PNGs for user review**

Export at minimum:

```text
Calendar Light
Calendar Dark
Calendar fontScale 1.3
Calendar range preview
Finance Summary Light
Finance Summary Dark
Finance Summary fontScale 1.3
Finance Summary PER_SHIFT
```

Also retain before/baseline renders for side-by-side comparison.

- [ ] **Step 2: Inspect the final reference screens together**

Acceptance must be visual, not only automated. Both screens must visibly share:
- cool airy canvas;
- mostly borderless soft surfaces;
- blue/indigo brand selection/navigation;
- semantic icon tiles;
- strong hierarchy/typography;
- semantic finance green only where money/success warrants it;
- no return to the legacy gray-green outlined-panel sea.

- [ ] **Step 3: Run full phone gate from a clean tree**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:validateDebugScreenshotTest --no-daemon
git diff --check
git status --short --branch
```

Expected:
- `BUILD SUCCESSFUL`;
- lint completes successfully;
- all Calendar/Finance/Payroll screenshot tests validate;
- `git diff --check` empty;
- worktree clean after checkpoint commit.

- [ ] **Step 4: Record M9 visual proof**

Append `WORK_PROGRESS.md` with:
- token/primitives commit;
- Calendar commit and screenshot findings;
- Finance commit and screenshot findings;
- exact final gate job/result;
- explicit note that Payroll Calculation/More/Today remain future vertical migrations.

Update `CURRENT_STATE.md` only once both reference screens are visually accepted.

- [ ] **Step 5: Commit M9 reference-screen checkpoint**

```bash
git add docs/project/WORK_PROGRESS.md docs/project/CURRENT_STATE.md
git commit -m "docs: checkpoint M9 Variant A visual foundation"
```

Do not push, merge, release or deploy.
