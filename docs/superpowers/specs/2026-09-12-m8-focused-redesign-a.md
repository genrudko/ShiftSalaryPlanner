# M8 Focused Redesign A — UX / Information Architecture Spec

Date: 2026-09-12
Status: **APPROVED PRODUCT DIRECTION / IMPLEMENTATION NOT YET STARTED**
Canonical base: `master@cbb3c1f56c36f7056b5043f8372622adce180691`
Working branch: `design/m8-focused-redesign-a`

## Purpose

M8 fixes the target information architecture and screen hierarchy for the vNext redesign before production UI migration begins.

This is not a cosmetic reskin. ShiftSalaryPlanner is already functionally mature; the redesign must improve information hierarchy, navigation density and progressive disclosure while preserving the product's strongest existing capabilities.

The selected visual/product direction is **Variant A / Evolution**:

- calendar-first;
- expressive and friendly rather than strictly utilitarian;
- high information density where the domain requires it;
- clear hierarchy instead of nested card overload;
- finance starts simple and can become a real detailed payslip;
- secondary tools move out of primary navigation without losing capability;
- AI is optional/experimental, not a mandatory primary destination;
- existing appearance flexibility informs M9 rather than being silently discarded.

## Product truths discovered before redesign

The current product already contains richer behavior than a generic shift-planner mockup. M8 must design around the actual product rather than around simplified concept screens.

### Calendar is the primary product surface

The current calendar supports:

- month navigation and direct month picking;
- profile switching;
- workplace filtering including all-workplaces mode;
- multiple workplace assignments in the same day;
- color-coded shift templates;
- arbitrary shift presentation through `iconKey`;
- text codes;
- emoji through `EMOJI:...`;
- Material icons and built-in glyphs;
- notes on dates;
- individual-day overrides;
- holiday/special-day markers;
- brush-style fast assignment;
- eraser;
- pattern/cycle application;
- range clearing;
- quick creation of shift templates.

Calendar cells are therefore information-bearing objects, not decorative date tiles. A redesign that replaces this model with tiny dots or generic event markers is a regression even if it looks cleaner.

### Shift identity is user-configurable

A shift template is not just `D/N`.

The existing model supports:

- required short code;
- title;
- color;
- text/icon/emoji visual identity;
- total/break/night hours;
- start/end time;
- pay-per-shift amount;
- workplace ownership;
- special-day compensation rules;
- linked alarm configuration.

The redesign must not hardcode one universal visual vocabulary for shifts.

### Today is already a configurable dashboard

The current Today screen supports user-configurable blocks including:

- Hero / main card;
- day pulse;
- next step;
- tomorrow;
- next alarm;
- month status;
- upcoming payments;
- notes.

Blocks can be reordered, hidden, restored and use different widths/sizes/background treatments.

Therefore M8 does **not** delete Today by default. It reclassifies Today from an assumed equal primary destination to an optional personalized overview whose final placement depends on navigation validation.

### Finance already contains the required depth

Finance currently has three semantic levels:

- Summary;
- Payroll / Calculation;
- Payments.

Payroll already supports month/year/range periods, workplace filtering, gross/net presentation, compact/detailed modes, detailed payslip, diagnostics, visibility configuration and PDF export.

M8 must reorganize this depth; it must not flatten or rewrite the payroll engine.

### Notes and alarms are real subsystems

Notes support date association, rich formatting, checklist content, colors/background patterns and media including image/video/audio/recording.

Alarms support shift-template configuration, upcoming alarms, automatic rescheduling, Wear mirroring, sounds, snooze/vibration behavior, ring-screen appearance and Android permission flows.

They can move out of primary navigation without being reduced to minor features.

### AI is optional product value

The current assistant is capable of more than chat: it can interpret commands, create action drafts, assign shifts/ranges/dates, configure alarms, clear a day, create notes, navigate, use voice/TTS/images and call external providers.

This capability is expensive in complexity and provider dependency. M8 therefore does not grant AI a permanent primary-navigation slot merely because the feature already exists.

AI remains available under `More` / contextual entry points during redesign. A later explicit value/maintenance review may promote, demote or remove it. No provider or free-tier assumption is part of M8.

## Approved IA direction

### Primary principle

**Calendar is the first and main screen.**

Opening the app normally lands in Calendar unless a valid external/widget destination explicitly requests another route.

### Primary navigation

Target phone primary navigation for the focused redesign:

```text
Calendar | Finance | More
```

`Today` is intentionally **not** fixed as a permanent primary tab in the initial focused slice.

It remains a supported destination and is treated as a personalized overview. During validation it may be exposed through one of these mechanisms without changing the three-item primary shell:

- top-level Calendar affordance;
- contextual entry from current-day detail;
- configurable shortcut;
- optional fourth primary item only if real use proves it deserves permanent placement.

The focused redesign must not invent a fourth primary item merely to preserve the old eight-tab symmetry.

### Why three primary items

The current eight-tab shell is already forced into ultra-dense mode, shrinking icons and labels and abbreviating long Russian labels. This is a structural navigation-density problem, not a styling problem.

The three primary items correspond to the three recurring top-level jobs:

1. **Calendar** — when do I work and how do I change my schedule?
2. **Finance** — how much did/will I earn and how is it calculated?
3. **More** — manage supporting tools, entities and application settings.

## Calendar hierarchy

### Calendar month is the home screen

The month view must answer in one glance:

- which days are work/non-work;
- what shift type occurs on each work day;
- how the next week looks as a pattern;
- where multiple workplaces/assignments coexist;
- where notable annotations exist.

It must preserve **pattern recognition** as the primary goal.

### Calendar-cell information model

The month cell must support these independent layers:

1. date number;
2. shift/status color;
3. shift/status visual identity: text code, glyph, emoji or Material icon;
4. multi-assignment/workplace segmentation when needed;
5. today state;
6. selected-day state;
7. note indicator;
8. individual-override indicator;
9. holiday/special-event indicator;
10. preview/range state used by patterns and clearing flows.

The redesign may simplify visual treatment, but may not collapse these semantics into a single ambiguous dot.

### Selected-day detail

Selecting a day reveals a detail surface below the month or as an equivalent responsive sheet depending on available height.

The detail surface is responsible for operational depth rather than the calendar cell itself.

Required content when applicable:

- date and weekday;
- assigned shift(s);
- user-selected shift visual identity;
- start/end time;
- workplace;
- expected pay for the shift/day when calculation data exists;
- linked alarm status/time;
- date/shift note preview;
- holiday/special/override state;
- edit/action entry points.

### Quick assignment

Quick assignment remains a first-class calendar interaction.

The focused redesign keeps:

- active-workplace templates first;
- up to several high-frequency templates immediately reachable;
- arbitrary text/icon/emoji identities;
- color as a strong recognition channel;
- eraser;
- normal mode;
- pattern/cycle entry;
- additional templates/actions behind `More` inside the assignment surface.

The design goal is to make rapid multi-day editing easier, not replace it with repeated full-screen editors.

### Multiple workplaces

Multiple workplaces remain a core feature.

When more than one assignment exists in one date, the month must retain a compact representation of multiple assignments. The implementation may use vertical/horizontal color segments or another tested representation, but one assignment must not silently hide another.

Workplace filtering remains available at calendar level.

## Finance hierarchy

Finance follows progressive disclosure.

### Level 1 — Summary

The first view answers:

**How much money?**

Priority order:

1. expected net amount / amount to receive;
2. gross amount;
3. tax/withholdings;
4. advance / remaining salary or per-shift payout status;
5. worked shifts/hours;
6. next payment;
7. short recent trend when useful;
8. fact-vs-plan comparison when actual payment data exists.

Language should be human-first. Accounting detail is one tap deeper.

### Level 2 — Calculation

The Calculation view explains **where the amount came from**.

It preserves:

- month/year/range period modes;
- workplace filtering;
- gross/net switch;
- compact/detailed presentation;
- accrual categories;
- deductions;
- special-day/overtime/night/vacation/sick/addition semantics already supported by the engine.

### Level 3 — Payslip

A full payslip is intentionally allowed to look like a serious payroll document.

It may expose table-like fields such as:

- accrual type;
- period/base;
- hours/days;
- rate;
- coefficient;
- amount;
- deductions/taxes;
- final net amount.

Existing PDF/report/diagnostics capabilities remain reachable.

The redesign must **not** change payroll arithmetic in order to make UI numbers fit a mockup.

## More hierarchy

`More` is not a miscellaneous dump. It is a structured map of less-frequent capabilities.

### Work

- Workplaces;
- Shift templates / statuses;
- profile management where applicable.

### Tools

- Alarms;
- Notes;
- Assistant (experimental/optional positioning).

### App

- Appearance;
- Widgets;
- Wear / watch integration;
- notification/application-level preferences.

### Data

- Backup/restore;
- Google Drive;
- import/export;
- reports/history where not better owned by Finance.

### Service / advanced

Diagnostics, event log and other service utilities remain reachable without competing with daily workflows.

## Contextual ownership of settings

M8 rejects the old pattern where every option is discoverable mainly through one giant Settings destination.

Entity-specific settings live with the entity whenever possible.

Examples:

- workplace payroll configuration near the workplace and/or Finance;
- shift alarms near the shift template and Alarms;
- date note actions from Calendar day detail;
- shift identity/color/icon in shift-template editing;
- report visibility near reporting/calculation surfaces;
- appearance remains an app-level setting.

A smaller Settings entry may remain inside More for truly global options.

## Today role

Today remains supported but is no longer assumed to be the product home.

Approved constraints:

- Calendar remains the default landing screen;
- Today may remain configurable;
- existing block customization is product value and should not be casually removed;
- Today must complement Calendar, not duplicate the month view;
- Today promotion to a permanent fourth primary destination requires real use evidence, not historical inertia.

Focused-redesign concept for Today:

- personalized overview;
- today shift/pay summary;
- next alarm / next step;
- tomorrow;
- month progress;
- upcoming payments;
- notes;
- user controls ordering/visibility/size.

## Visual direction — Variant A / Evolution

### Character

The app should feel:

- personal;
- modern;
- friendly;
- confident;
- expressive without becoming decorative noise.

It should **not** feel like an industrial control panel, generic enterprise settings app or glass-effect showcase.

### Visual rules

- strong type hierarchy;
- generous but not wasteful spacing;
- rounded surfaces with restrained nesting;
- color used strongly where it carries meaning, especially shift identity;
- expressive accent surfaces for important money/status information;
- subtle motion and haptics where they clarify state;
- secondary actions visually quieter than primary information;
- avoid a border/card around every two lines of content;
- avoid full-screen decorative gradients that reduce data readability;
- Glass may survive as an optional appearance style, but is not the default design language.

### Existing personalization

M9 must explicitly review rather than silently delete the existing appearance axes:

- light/dark/auto/scheduled theme;
- Mint/Ocean/Sunset/Graphite/Custom/Material You palettes;
- section fonts;
- comfortable/compact density;
- normal/high contrast;
- animation speed;
- corner style;
- Classic / Expressive / Expressive Glass;
- custom colors.

M8 does not decide every final token. It defines the behavior and hierarchy the design system must serve.

## Accessibility and scaling constraints

The focused redesign must be viable with:

- increased Android font scale;
- high-contrast mode;
- Russian labels without forced 7–8sp ultra-dense text;
- touch targets appropriate for repeated calendar use;
- color not being the only carrier of shift meaning;
- selected/today/holiday/override states remaining distinguishable without relying only on hue.

## Focused redesign screen set

The first design/implementation contract consists of these screens/states:

1. Calendar — month, normal state;
2. Calendar — selected-day detail;
3. Calendar — quick assignment / brush state;
4. Calendar — representative multi-workplace day;
5. Finance — Summary;
6. Finance — Payments status/fact-vs-plan;
7. Finance — Calculation compact/detailed entry;
8. Finance — full Payslip;
9. More — grouped root;
10. Workplace — contextual detail/settings;
11. Appearance — reduced/reorganized app-level appearance surface;
12. Today — optional personalized overview and layout editor.

These screens form the review set before broader M9/M10+ production migration.

## Explicit non-goals for M8

M8 does not:

- rewrite payroll calculations;
- change Room schema;
- change backup formats;
- change alarm scheduling semantics;
- upgrade dependencies;
- migrate Wear UI wholesale;
- remove notes/media capabilities;
- delete the assistant merely because it is not primary;
- remove Today merely because Calendar is home;
- hardcode `D/N` as the only shift notation;
- replace multi-workplace calendar semantics with one visible shift per day;
- implement a new DI/navigation/state framework;
- merge/release/deploy anything without the established owner gate.

## Acceptance criteria for M8

M8 is complete when all of the following are true:

- the three-item primary IA `Calendar / Finance / More` is documented and reviewed;
- Calendar-first/default-home behavior is documented;
- Today has an explicit non-primary-but-supported role;
- AI has an explicit optional/experimental role;
- month-cell information semantics are documented including text/icon/emoji and multi-workplace cases;
- day-detail and quick-assignment flows are documented;
- Finance has explicit Summary → Calculation → Payslip depth;
- More taxonomy and contextual-settings policy are documented;
- Variant A / Evolution is the selected visual direction;
- focused redesign screens are reviewed against the current application rather than generic mockups;
- no production behavior is changed as part of the M8 spec itself;
- an M9 implementation/design-system plan can be written without unresolved top-level IA questions.

## Decision log

### D1 — Calendar-first

**Decision:** Calendar is default landing surface and first primary destination.

**Reason:** schedule pattern recognition is the most frequent/core product job and the existing calendar is already the strongest UX asset.

### D2 — Three-item primary navigation

**Decision:** initial target is `Calendar / Finance / More`.

**Reason:** eight tabs are structurally too dense; Today/Notes/AI/Alarms/Shifts/Settings do not all deserve equal persistent navigation weight.

### D3 — Today retained, not primary by default

**Decision:** preserve the configurable dashboard and validate its entry point separately.

**Reason:** deleting it would throw away existing customization value, but keeping it permanently primary by inertia is not justified.

### D4 — Finance uses progressive disclosure

**Decision:** simple Summary, explanatory Calculation, serious full Payslip.

**Reason:** daily comprehension and payroll-grade detail are both required but should not compete on one screen.

### D5 — AI not a primary requirement

**Decision:** Assistant moves to optional/experimental positioning during redesign.

**Reason:** provider/cost/maintenance complexity must be justified by real user value; existing capability remains available.

### D6 — Calendar information capacity is an invariant

**Decision:** preserve custom shift visual identities, colors, multi-workplace assignments and metadata indicators.

**Reason:** visual scan speed is core product value; simplification to generic dots would be a functional UX regression.

### D7 — Variant A / Evolution selected

**Decision:** use the expressive light, readable, friendly direction as the design basis; borrow individual ideas from other variants only when they improve the approved behavior.

**Reason:** it best balances readability, personality, finance clarity and long-term daily use.
