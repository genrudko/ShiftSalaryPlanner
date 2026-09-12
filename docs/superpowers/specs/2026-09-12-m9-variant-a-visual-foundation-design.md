# M9 — Variant A Visual Foundation Design

**Status:** proposed implementation baseline after M8 Calendar/Finance IA proof  
**Branch:** `design/m8-focused-redesign-a`  
**Visual direction:** Variant A / Evolution, approved in product review  

## 1. Problem

M8 proved the information architecture and interaction hierarchy, but the real Compose screenshots still look like the legacy ShiftSalaryPlanner with cleaner content ordering. The reason is structural: current expressive surfaces inherit the old outlined panel language (`1dp` borders, gray/green panels, flat zero-elevation cards) and add a light gradient/glass wash on top. This cannot reproduce the approved Variant A composition.

M9 therefore introduces a reusable visual foundation before more screen-by-screen redesign work. It must make the real application recognizably belong to the approved Variant A family while preserving existing data density, payroll correctness, shift semantics, accessibility, and appearance customization.

## 2. Visual target

Variant A is **expressive, friendly, readable, calendar-first**. It is not a pixel-perfect contract with the concept image, but the real app must match its visual grammar:

- airy cool-white / faint blue-lilac background rather than gray-green slabs;
- mostly borderless white or softly tinted surfaces, with subtle depth instead of constant outlines;
- large rounded expressive shapes with clear hierarchy;
- strong near-black typography and larger section titles;
- colorful squircle icon tiles used as semantic anchors;
- selective gradients and abstract/wave decoration in hero surfaces only;
- global navigation accent in a confident blue/indigo family;
- semantic finance/success accent in green rather than recoloring the whole app green;
- calendar color remains data: shift colors/badges stay stronger than surrounding chrome;
- tabs are lightweight text + active indicator, not rows of heavy segmented buttons;
- bottom navigation contains the approved three primary destinations and uses generous touch targets.

## 3. Scope

### In scope

1. **Visual tokens / primitives**
   - background and surface roles;
   - semantic accent roles;
   - spacing, radii, elevation and stroke policy;
   - typography roles;
   - `EvolutionSurface` / card primitives;
   - semantic icon tile;
   - hero card;
   - list/action row;
   - lightweight tabs;
   - compact segmented option control;
   - shift badge / calendar cell chrome;
   - three-destination bottom navigation chrome.

2. **Reference screens**
   - Calendar month + selected-day detail + quick assignment;
   - Finance Summary.

3. **Visual proof**
   - deterministic Compose screenshots for Light, Dark and fontScale 1.3;
   - complex Calendar stress fixture already established in M8;
   - deterministic Finance fixture already established in M8;
   - before/after comparison against current real Compose screenshots.

### Out of scope for this slice

- payroll formulas or data structures;
- schedule assignment semantics;
- Room/backup/network changes;
- removal of appearance settings;
- redesign of every secondary screen before the reference screens are accepted;
- AI functionality;
- dependency modernization unrelated to screenshot tooling already present.

## 4. Token model

M9 adds a semantic token layer for the new visual mode instead of scattering raw colors and dimensions through screens.

### Color roles

- `appBackground`: cool neutral canvas with a faint blue/lilac bias in Light; deep neutral-navy in Dark.
- `surfacePrimary`: clean main card surface.
- `surfaceSoft`: low-contrast tinted grouping surface.
- `surfaceAccent`: restrained brand-tinted card.
- `brandPrimary`: confident blue/indigo for navigation, selection and focus.
- `brandSecondary`: violet/lilac for supportive expressive accents.
- `financePositive`: semantic emerald/green for money hero and positive finance emphasis.
- `warningAccent`: warm amber/orange.
- `dangerAccent`: red for deductions/errors only.
- `contentPrimary` / `contentSecondary`: high-contrast near-black / muted neutral text.

User-defined **shift colors remain untouched semantic data** and must never be recolored by this visual system.

Existing palette choices remain supported. Geometry and hierarchy are stable; palette choices can remap brand roles. The approved default expressive appearance should remain recognizably Variant A.

### Shape / stroke / elevation

- primary card radius: approximately 20–24dp;
- smaller control / icon tile radius: approximately 12–16dp;
- pill / badge uses capsule or small rounded rectangle where semantically appropriate;
- default cards are **borderless**;
- outlines are reserved for selection, focus, input affordance, accessibility/high-contrast mode, and special calendar states;
- subtle elevation/shadow separates floating surfaces from the canvas;
- hero surfaces may use soft gradient/wave layers, never the whole screen.

### Spacing

Use a coherent 4/8/12/16/20/24dp rhythm. Main screen horizontal padding targets 16dp. Section gaps must be visibly larger than within-card gaps.

### Typography

The existing Manrope resource is suitable for the approved direction. Expressive reference screens use a clean sans hierarchy:

- screen title: ~22–24sp, semibold/bold;
- section title: ~17–20sp, semibold;
- hero money: ~34–40sp, bold;
- body: ~14–16sp;
- metadata: ~12–13sp.

Typography must continue to respect existing font customization and fontScale. No layout may depend on a fixed line count that breaks at fontScale 1.3.

## 5. Core primitives

### 5.1 Evolution surface

Replaces the old assumption that every panel is an outlined box. Supports roles `PRIMARY`, `SOFT`, `ACCENT`, `FLOATING`, and `HERO`. Border is opt-in. Dark mode gets deliberate role colors rather than a simple opacity inversion.

### 5.2 Semantic icon tile

A 40–48dp rounded square/squircle containing an icon, emoji, or short glyph. It carries semantic color (calendar/workplace/finance/action) and provides the visual anchors present in Variant A.

### 5.3 Hero card

Used sparingly for the one dominant answer on a screen. Finance hero contains:

- finance icon tile;
- label `Ожидается на руки` / appropriate pay-mode label;
- dominant amount;
- optional supportive microcopy;
- restrained decorative finance graphic/wave;
- no table-like chrome.

### 5.4 Action/list row

White/clean row with semantic icon tile, title, optional subtitle, trailing value and chevron/action. This is the default finance summary detail pattern instead of nested tables/cards.

### 5.5 Lightweight tabs

Text tabs with active brand color and thin/soft active indicator. `Сводка / Расчёт / Выплаты` must visually read as navigation, not form controls.

### 5.6 Calendar shift badge

The badge is a first-class data element:

- supports text, emoji, Material icon or custom glyph;
- uses shift-defined color;
- preserves multi-workplace segmented days;
- off-day / special states remain distinguishable;
- selected/today states are separate overlays and never destroy shift identity.

## 6. Reference screen — Calendar

The calendar remains the visual and functional home of the product.

Hierarchy:

1. lightweight month header and navigation;
2. workplace/profile context card with semantic icon tile;
3. month grid with minimal chrome;
4. selected day detail card;
5. quick shift assignment row.

Calendar cells use subtle neutral/tinted day surfaces. Shift identity appears as a strong colored badge inside each day. Heavy per-cell outlines are avoided. Selection uses a confident blue/indigo outline/ring. Note, override and holiday markers remain secondary metadata and must not compete with shift identity.

The established stress fixture (text codes, emoji, Material icon, multiple workplaces, note, override, holiday, selection, today and range preview) remains the visual acceptance fixture.

## 7. Reference screen — Finance Summary

Hierarchy:

1. screen title `Финансы` with semantic finance icon;
2. lightweight period selector;
3. `Сводка / Расчёт / Выплаты` text tabs;
4. finance hero answering `сколько денег?`;
5. four expressive action/value rows: `Начислено`, `НДФЛ`, `Аванс`, `Остаток` (or pay-mode equivalents);
6. prominent `Расчётный лист` row;
7. optional soft contextual/motivational panel, lower visual weight;
8. three-destination bottom navigation.

The current M8 calculation/payroll hierarchy remains valid below this Summary layer. M9 changes its visual primitives later only after Calendar and Finance Summary establish the system.

## 8. Dark mode

Dark mode is a first-class design, not a color inversion:

- deep neutral background;
- slightly lifted surfaces;
- accents retain saturation but reduce glare;
- finance green and brand blue remain distinct;
- calendar shift colors retain identity and contrast;
- no white glass borders around every component;
- deductions/errors remain readable without oversaturating the entire screen.

## 9. Appearance compatibility

Existing settings are preserved during M9 foundation work:

- theme Light/Dark/Auto/Schedule;
- palette selection;
- custom colors;
- font choice and per-section font choice;
- Comfortable/Compact density;
- contrast mode;
- corner style;
- animation speed;
- `Classic / Expressive / Expressive Glass` choices.

Variant A defines the **new Expressive baseline**. Classic remains a legacy visual mode for now; Expressive Glass may reuse the same geometry/components with a different surface treatment. Default/migration policy for existing users is not changed silently in this slice and must be an explicit release decision later.

## 10. Implementation boundaries

Expected files/components:

- theme semantic token definitions under `ui/theme`;
- evolution primitives under `ui/common`;
- existing `AppExpressiveSurface` either becomes a compatibility wrapper or delegates to the new tokenized surface system;
- Calendar and Finance Summary adopt primitives first;
- payroll calculation logic, repositories, ViewModels and domain models remain untouched.

Avoid a giant rewrite. Introduce primitives, migrate the two reference screens, prove visually, then extend vertically.

## 11. Testing and visual acceptance

Each reference screen requires:

1. existing behavior/unit/characterization tests remain green;
2. structure tests ensure screens use the new primitives instead of legacy panel patterns;
3. Light screenshot golden;
4. Dark screenshot golden;
5. fontScale 1.3 screenshot golden;
6. visual inspection of actual Compose PNGs before accepting goldens;
7. full `testDebugUnitTest + assembleDebug + lintDebug + validateDebugScreenshotTest` gate.

### Acceptance criteria

The M9 foundation is not accepted merely because tests pass. Calendar and Finance Summary must be **immediately recognizable as the approved Variant A family** when viewed beside the concept. Specifically:

- no sea of outlined gray/green containers;
- icon tiles are present and meaningful;
- typography and spacing establish clear hierarchy;
- Calendar shift pattern remains faster to read than the concept image, not worse;
- Finance hero is visually dominant and uses semantic green without tinting the whole app green;
- Summary details use expressive rows rather than a table dump;
- Light/Dark/1.3 renders have no clipping or overlap;
- the redesign preserves all previously proven M8 behavior.

## 12. Rollout order

1. semantic tokens and primitives;
2. Calendar reference migration + screenshots + review;
3. Finance Summary reference migration + screenshots + review;
4. joint visual foundation checkpoint;
5. only then migrate Payroll Calculation, More, Today and secondary screens.

This sequence intentionally pauses broad screen work until the visual language itself is proven.
