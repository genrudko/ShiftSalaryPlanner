# M9 Design System Implementation Plan

> **Execution rule:** implement task-by-task with tests before behavior-bearing changes and fresh verification before each commit/closeout claim.

**Goal:** turn the accepted M8 Variant A / Evolution UX contract into a reusable, accessibility-aware Compose design system that can support Calendar-first M10 and the later Finance/More slices without changing product/domain behavior in M9.

**Base:** `design/m8-focused-redesign-a` after M8 closeout.

**Primary references:**
- `docs/project/M8_FOCUSED_SCREEN_CONTRACTS.md`
- `docs/project/M8_FOCUSED_REDESIGN_REVIEW.md`
- `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

**Architecture:** reuse the existing Material 3 theme and current appearance settings rather than introducing a new UI framework. M9 may add shared tokens/components and preview/test fixtures, but does not migrate whole Calendar/Finance/More screens; those production vertical slices begin at M10.

## Global invariants

- No payroll arithmetic changes.
- No Room schema/migration changes.
- No backup/restore payload changes.
- No alarm scheduling/reboot semantic changes.
- No Wear contract changes.
- No dependency upgrades unless separately approved and required by a proven blocker.
- Existing appearance capabilities are mapped or deliberately deprecated only with explicit evidence; they are not silently ignored.
- Shift color remains semantic data and is never the sole identifier.
- M9 components must support light/dark, Comfortable/Compact, increased font scale and high contrast.
- The accepted primary shell is `Calendar / Finance / More`; M9 creates its visual primitive but does not yet remove legacy direct routes.

---

## Task 1 — Inventory and freeze the existing appearance contract

**Read:**
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Color.kt`
- `ui/theme/AppearanceSettings.kt`
- `ui/common/AppExpressiveSurfaces.kt`
- appearance settings UI/store files

**Create:** `docs/project/M9_DESIGN_SYSTEM_INVENTORY.md`

- [ ] Record every existing appearance axis: light/dark/system/schedule, palette presets/custom/Material You, density, contrast, font/section typography, animation speed, corner style, Classic/Expressive/Expressive Glass and bubble/surface customization.
- [ ] Record which current helpers/components already implement those axes.
- [ ] Classify each axis as `core token`, `component variant`, `legacy compatibility`, or `later rationalization`.
- [ ] Add a structural regression test that prevents M9 from bypassing the appearance contract with hard-coded screen-local colors/radii for the new shared components.
- [ ] Run targeted test RED then GREEN after the minimum contract scaffolding exists.

**Acceptance:** M9 has an explicit compatibility map before new tokens are introduced.

---

## Task 2 — Introduce semantic design tokens without duplicating MaterialTheme

**Create/modify:** shared design/theme files under `ui/design/` or the existing `ui/theme/` boundary, whichever produces the smaller coherent diff.

Token groups:

```text
spacing: xs / sm / md / lg / xl
shape: compact / control / card / sheet
surface roles: base / soft / panel / emphasized / destructive
content roles: primary / secondary / muted / positive / warning / negative
motion: short / standard / emphasized, mapped to existing animation-speed setting
```

- [ ] Write focused tests for deterministic mapping from existing density/corner/contrast/style settings to semantic token values.
- [ ] Implement the smallest token layer that delegates to existing MaterialTheme/appearance values rather than replacing them.
- [ ] Verify no screen-local migration is required yet.
- [ ] Run focused tests and `:app:compileDebugKotlin`.

**Acceptance:** shared components can request semantic roles without knowing a palette preset or raw corner preference.

---

## Task 3 — Calendar-critical cell primitives

M10 depends on these first; do not start with generic buttons.

**Create:** reusable Calendar visual primitives or refactor-neutral helpers that can be previewed/tested without changing current Calendar behavior.

Required semantic inputs:

```text
date
current-month flag
today
selected
single/multiple assignment segments
per-assignment color
text / built-in glyph / emoji / Material-icon identity
workplace disambiguation
note marker
override marker
holiday/special marker
preview/range state
compact/comfortable density
```

- [ ] Add a pure semantic model/mapper only if it reduces duplicated visual-state logic; do not move schedule/domain ownership into the design layer.
- [ ] Add representative fixtures for text, emoji, icon, multi-workplace, note+override+holiday and today+selected.
- [ ] Build a reusable cell visual component/primitive preserving all semantic layers from M8.
- [ ] Ensure selected and today are simultaneous/distinct states.
- [ ] Ensure multi-assignment rendering never overwrites an assignment.
- [ ] Add content descriptions/non-color identity hooks.
- [ ] Verify Comfortable and Compact dimensions.

**Acceptance:** one component family can represent the full CAL-04 stress matrix without opening day detail.

---

## Task 4 — Calendar contextual primitives

Create reusable primitives for:

- selected-day compact summary;
- quick-template tile;
- active-brush/mode banner;
- range/pattern preview affordance;
- compact workplace/profile context row.

- [ ] Define interaction-neutral component APIs; callbacks are supplied by feature code.
- [ ] Preserve configured template identity (text/emoji/icon/color) in quick-template tiles.
- [ ] Keep repetitive actions at practical touch-target size (~48dp where layout permits).
- [ ] Add state fixtures for normal/brush/eraser/pattern/clear-range modes.
- [ ] Compile and run focused tests.

**Acceptance:** M10 can assemble Month Home and Quick Assignment from design primitives without inventing local styling.

---

## Task 5 — Primary navigation chrome

Create the visual primitive for the accepted three-item phone shell:

```text
Calendar | Finance | More
```

- [ ] Support selected/unselected state, labels, Material icons, light/dark and high contrast.
- [ ] Do not delete or invalidate legacy typed destinations/direct-entry routing in M9.
- [ ] Add a structural test that the new primary-navigation component exposes exactly the three accepted persistent items.
- [ ] Keep Today/Notes/Assistant/Alarms/Shifts/Settings available to later shell routing rather than encoding them as permanent items.

**Acceptance:** M10 can adopt the new shell without re-solving bottom-navigation design.

---

## Task 6 — Money and Finance primitives

Create shared components for the M8 Finance hierarchy:

- hero money value;
- aligned money metric row;
- positive/negative/neutral semantic amount;
- payment status row;
- compact fact-vs-plan summary;
- section/group header suitable for Calculation and Payslip.

- [ ] Money formatting remains supplied by existing domain/UI formatting logic; design components do not calculate money.
- [ ] Positive/negative meaning has text/icon semantics in addition to color.
- [ ] Large values and font scaling do not clip in representative fixtures.
- [ ] Full Payslip density is supported by a compact variant without forcing the entire app into compact density.

**Acceptance:** M11 Finance can implement progressive disclosure using shared components while existing payroll tests remain untouched.

---

## Task 7 — Grouped More / entity-detail primitives

Create reusable presentation patterns for:

- grouped settings/tool list;
- context/entity hero/header;
- sectioned entity settings;
- destructive action zone;
- compact appearance option tile/selector.

- [ ] Avoid a universal nested-card-everywhere abstraction.
- [ ] Support Work / Tools / App / Data / Advanced group hierarchy.
- [ ] Provide a component pattern suitable for Workplace detail and Appearance without coupling those features together.
- [ ] Verify touch targets and font scaling.

**Acceptance:** M12 can build More and Workplace detail without copying one-off list/card styles.

---

## Task 8 — Component showcase / stress fixture

Create a developer-only Compose preview/showcase source (not a production destination) covering one coherent representative data set.

Mandatory states:

```text
Calendar text-code month
Calendar emoji/icon month
Calendar multi-workplace day
note + override + holiday
Today + selected
brush / pattern preview
Comfortable + Compact
light + dark
high contrast
large-font representative components
Money hero/rows
three-item primary navigation
More grouped rows
```

- [ ] Showcase must not introduce fake domain calculations or require Room/network access.
- [ ] Where Compose preview limitations prevent a real accessibility case, document the limitation and keep a deterministic fixture/test.
- [ ] Run compile and lint for new preview/showcase code.

**Acceptance:** reviewers can inspect the design language as a system, not only as disconnected screen mockups.

---

## Task 9 — Qualification and M10 handoff

- [ ] Run focused design-system tests.
- [ ] Run fresh `:app:testDebugUnitTest`.
- [ ] Run phone/Wear `assembleDebug` and `lintDebug` if shared theme changes can affect both modules; otherwise document why Wear is unaffected and still compile the relevant shared boundary.
- [ ] Run `git diff --check`.
- [ ] Review the whole M9 diff for hard-coded local styling, semantic regressions and accidental feature migration.
- [ ] Independently review Calendar cell semantics and appearance-setting compatibility.
- [ ] Update `CURRENT_STATE.md` and `WORK_PROGRESS.md` only after gates are green.
- [ ] Write the M10 Calendar Vertical Slice implementation plan against the final M9 component APIs.

**M9 exit:** Variant A / Evolution is represented by reusable, tested design primitives and a stress-review fixture; no whole feature has been prematurely migrated and no domain/persistence/integration semantics changed.

## Planned implementation sequence

```text
appearance inventory
  -> semantic tokens
  -> Calendar cell primitives
  -> Calendar contextual primitives
  -> primary navigation chrome
  -> money/Finance primitives
  -> More/entity primitives
  -> showcase/stress matrix
  -> qualification
  -> M10 Calendar slice
```

This order is intentional: Calendar is the primary product surface, so it defines the hardest information-density requirements for the design system. A generic component library designed before Calendar would risk optimizing for prettier but less capable screens.
