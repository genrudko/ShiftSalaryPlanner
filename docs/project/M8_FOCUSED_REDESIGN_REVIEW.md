# M8 — Focused Redesign Review (Variant A / Evolution)

Date: 2026-09-12
Branch: `design/m8-focused-redesign-a`
Canonical base: `master@cbb3c1f56c36f7056b5043f8372622adce180691`
Status: **accepted UX/IA baseline; ready for M9 design-system handoff**

## Decision

The owner reviewed the focused Variant A / Evolution screen set and chose it as the working redesign direction. M8 locks product structure and information hierarchy, not final pixels. M9 owns exact typography, spacing, component geometry, tokens, state styling and accessibility stress refinement.

Locked product decisions:

- Calendar is the default launch surface and first primary destination.
- Phone primary navigation target is exactly `Calendar / Finance / More`.
- Today remains a supported configurable overview, but is not a required permanent primary tab.
- Assistant remains available as a secondary/experimental tool; it is not entitled to primary navigation by default.
- Variant A / Evolution is the baseline visual character: expressive, friendly, modern Material, readable and personal rather than corporate/industrial.
- Calendar keeps its current information capacity: text codes, built-in glyphs, emoji, Material icons, per-template color, multi-workplace assignments, notes, individual overrides, holidays/special days and edit/preview modes.
- Finance uses progressive disclosure: Summary -> Calculation -> full Payslip, with Payments as a first-class finance flow.
- Payroll arithmetic, Room schema, backup semantics, alarm scheduling semantics and Wear contracts are outside M8 and must not be altered for visual convenience.

## Focused screen set reviewed

The accepted set covers the following target surfaces:

1. Calendar month home.
2. Calendar selected-day detail and contextual actions.
3. Calendar quick assignment / brush workflow.
4. Finance Summary and Payments/fact-vs-plan.
5. Finance Calculation and full Payslip.
6. More grouped root, Workplace contextual detail and Appearance.
7. Today personalized overview and layout editor as an optional/supporting route.

The screen-by-screen behavior contract is canonical in `docs/project/M8_FOCUSED_SCREEN_CONTRACTS.md`.

## Visual-review caveats

Generated concept boards are directional references, not literal UI specifications. The following generator artifacts are explicitly rejected and must not leak into implementation:

- one Calendar concept accidentally showed a fourth primary item `Plans`; the accepted primary shell is exactly three destinations: Calendar, Finance, More;
- the Today concept showed Today as a persistent bottom tab; this only illustrates the Today screen, not its primary-navigation status;
- finance numbers, employee names, workplace names and motivational copy in renders are illustrative, not product/domain truth;
- decorative motivational copy is optional personalization and never substitutes for payroll facts;
- Glass/dark atmospheric concepts are not the default design direction; existing appearance options may preserve Glass as an optional style later.

## Calendar validation matrix

M9/M10 must use one representative month across all component/state previews instead of validating each case on a different invented screen.

| Case | M8 verdict | M9/M10 requirement |
| --- | --- | --- |
| Single workplace + text code (`D`, `N`, `8`, custom text) | Accepted | Code remains readable without relying on color |
| Emoji identity | Accepted | Emoji must fit without shrinking date/markers into noise |
| Material/built-in icon identity | Accepted | Icon identity remains visually equal to text/emoji identities |
| Multiple workplaces on one date | Accepted | No assignment overwrites another; visible segmented identity is mandatory |
| Note + individual override + holiday/special marker | Accepted | Stable secondary marker zones; markers must not compete with shift identity |
| Today + selected simultaneously | Accepted | Two distinct facts; selection must not erase shift color or today state |
| Brush active | Accepted behavior | Active tool unmistakable, month swipe disabled, exit obvious |
| Pattern/range preview | Accepted behavior | Preview differs from applied schedule; confirm/cancel explicit |
| Clear-range mode | Accepted behavior | Reuses pattern grammar; destructive emphasis only at confirmation |
| Comfortable / Compact | Required | Pattern remains scan-readable in both existing densities |
| Increased font scale | Required | No clipped primary shift identity or unusable actions |
| High contrast | Required | Non-color semantics survive contrast mode |
| Dark theme | Required | Semantic hierarchy must match light theme, not become a Glass-only design |
| Landscape | Required | Existing functional landscape behavior remains usable |

Calendar acceptance invariant: **the month itself must answer “what is my work pattern?” without opening a day.** Day detail answers operational questions about one selected date.

## Finance validation matrix

- Summary first answers `How much money?` with the current period net result as the strongest number.
- Gross/accruals, tax/withholdings, received/advance and remaining amount are immediately understandable without exposing every payroll line.
- Payment mode must follow actual workplace/payroll settings; the UI must not fake advance/salary semantics for per-shift payment modes.
- Calculation explains how the result was formed and keeps period/workplace/net-gross/compact-detailed capabilities where currently supported.
- Full Payslip is intentionally denser and may be document-like, but only displays data actually supported by the app; it does not invent HR fields.
- Existing payroll regression tests remain the authority for every amount-producing path.

## More / contextual-settings validation

Accepted root taxonomy:

```text
More
|- Work
|  |- Workplaces
|  `- Shift templates / statuses
|- Tools
|  |- Alarms
|  |- Notes
|  `- Assistant
|- App
|  |- Appearance
|  |- Widgets
|  `- Wear / watches
|- Data
|  |- Backup / Restore
|  |- Google Drive
|  `- Import / Export
`- Advanced / Service
   |- Health check
   |- Event log
   `- Diagnostics / service utilities
```

Entity-owned settings move toward their owning context where practical: workplace payroll settings near Workplace/Finance, shift alarm settings near shift/alarm flows, note-specific controls near Notes. M15 must still inventory every old Settings destination before deleting or relocating anything.

## External/widget/deep-entry compatibility mapping

M5 deliberately preserved all eight `BottomTab` names plus legacy `PAYROLL`/`PAYMENTS` aliases. M8 changes visible primary IA, not the external compatibility contract. M9+ must keep direct-entry semantics even when a destination is no longer a permanent bottom item.

| Existing external value | Redesigned destination |
| --- | --- |
| `CALENDAR` | Calendar |
| `FINANCE` | Finance Summary |
| `PAYROLL` | Finance Calculation |
| `PAYMENTS` | Finance Payments |
| `TODAY` | Today direct route/overview, non-primary |
| `ASSISTANT` | More > Assistant direct route |
| `NOTES` | More > Notes direct route |
| `ALARMS` | More > Alarms direct route |
| `SHIFTS` | More > Shift templates direct route |
| `SETTINGS` | More > Settings / appropriate global-settings destination |
| invalid / blank | Calendar default |

Compatibility rule: valid legacy `BottomTab.name` inputs remain parseable until a later explicit compatibility/deprecation decision. Widgets/shortcuts must not be broken merely because the corresponding feature leaves permanent bottom navigation.

## Accessibility and density gate for M9/M10

M8 accepts the information hierarchy subject to these implementation gates:

- color is never the sole shift identifier;
- primary repetitive touch targets target at least approximately 48 dp where the layout allows;
- selected, today, holiday, note, override and preview states are semantically distinct;
- large font scale cannot hide the day number or shift identity;
- high contrast preserves assignment segmentation and selected state;
- Calendar remains usable in existing Comfortable and Compact modes;
- Finance money values use consistent alignment/formatting and do not rely on color alone for positive/negative meaning;
- TalkBack/content descriptions are part of component acceptance, not a final M19-only cleanup.

## No-domain-change check

Nothing accepted in M8 requires changing:

- payroll formulas or legislation rules;
- Room entities/schema/migrations;
- backup/restore payload compatibility;
- alarm scheduling/reboot behavior;
- Wear data contract;
- dependency graph.

The redesign can therefore proceed through UI/design-system work against the stabilized M4-M7 boundaries.

## Revised Phase III implementation order

Calendar is now the product's primary surface, so the old `Today before Calendar` vertical-slice order is superseded. The accepted order is:

```text
M8  UX / IA                         [this milestone]
M9  Design System
M10 Calendar Vertical Slice
M11 Finance Vertical Slice
M12 More / Workplaces / contextual settings shell
M13 Shifts & Alarms
M14 Notes & Assistant
M15 Today + remaining Settings rationalization
M16+ integration/release roadmap continues
```

Today is intentionally later because it is a configurable summary over capabilities whose primary Calendar/Finance/More surfaces should establish the new component language first.

## M8 exit verdict

**PASS for UX/IA.** Variant A / Evolution and the focused screen contracts are sufficiently concrete to hand off to M9. M8 does not claim final pixel approval or production UI implementation.

Exact next operation: write and execute M9 Design System plan against these contracts, beginning with Calendar-critical primitives rather than generic component-library work.
