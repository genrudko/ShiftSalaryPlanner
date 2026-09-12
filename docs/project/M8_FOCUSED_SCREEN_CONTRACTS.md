# M8 — Focused Screen Contracts (Variant A / Evolution)

Date: 2026-09-12
Status: **focused redesign working contract**
Spec: `docs/superpowers/specs/2026-09-12-m8-focused-redesign-a.md`

This document translates the approved M8 IA into screen-by-screen behavior and information hierarchy. It is deliberately more concrete than the M8 product spec but stops short of M9 pixel/design-token decisions.

## Shared screen rules

### Product chrome

Phone primary navigation:

```text
Calendar | Finance | More
```

- Calendar is first and default.
- Finance is second.
- More is third.
- Today is supported outside the mandatory three-item shell.
- External/widget direct-entry routes may open a secondary screen without forcing the user through More first.

### Visual character

Variant A / Evolution is the baseline:

- light, expressive, friendly and readable;
- rounded geometry without excessive nested cards;
- strong information hierarchy;
- large important numbers;
- shift color remains semantic data;
- white/soft surfaces dominate, with accent tint reserved for important states;
- animations are subtle and state-explanatory rather than ornamental;
- Glass is not the default, although an appearance option may preserve it later.

### Density

The UI must remain usable in both existing density modes:

- Comfortable;
- Compact.

Calendar must remain information-dense even in Comfortable mode. Other screens should gain whitespace from hierarchy, not from hiding important data.

### Accessibility

Every screen contract assumes:

- increased system font scale;
- high-contrast appearance option;
- color is never the sole shift identifier;
- touch targets remain suitable for repetitive daily use;
- selected/today/holiday/override states do not collapse into one color-only distinction.

---

# CAL-01 — Calendar / Month Home

## Job to be done

In one glance:

- see the work pattern for the month;
- understand the next week;
- distinguish shift types;
- notice unusual/special days;
- start editing quickly.

## Entry

Normal app launch opens this screen unless an external/deep route requests something else.

## Vertical hierarchy

### 1. Month header

Primary line:

```text
<   September 2026   >   [pick month]
```

Requirements:

- swipe month remains supported when not in an editing mode;
- previous/next controls remain explicit for discoverability;
- month title is tappable/direct-pick capable;
- today-return affordance may be included if the user is far from the current month, but must not compete in normal state.

### 2. Context row

Compact selectors for:

- workplace filter (`All workplaces` or selected workplace);
- profile.

Rules:

- workplace selector has higher operational relevance than profile management;
- management actions are secondary; selector itself is fast;
- filter state must be visible without opening a menu.

### 3. Calendar grid

Seven columns, Monday-first as current product behavior.

The calendar grid is the dominant visual object.

#### Cell semantics

Each date can carry:

- date number;
- one or more assignment segments;
- per-assignment color;
- per-assignment visual identity (text, built-in glyph, emoji or Material icon);
- workplace identity where multiple workplaces require disambiguation;
- note indicator;
- individual-override indicator;
- holiday/special event indicator;
- today state;
- selected state;
- brush/pattern/range preview state.

#### Single-assignment cell

Recommended information order inside the cell:

```text
[date number]
[large shift visual identity]
```

The shift identity may be `D`, `N`, `8D`, a Material icon, an emoji or another configured representation.

The shift color may occupy the lower identity zone or a substantial soft fill. It must be visually strong enough for pattern recognition but must not reduce date/marker legibility.

#### Multiple assignments

If two or more assignments exist:

- split the assignment region into visible segments;
- every visible segment carries its own color;
- where space permits, each segment also carries its visual identity;
- no assignment may simply overwrite another.

The exact split direction is a M9 component decision; semantic capacity is mandatory.

#### Today vs selected

- today: persistent but quiet marker;
- selected day: stronger outline/surface state;
- selected state must not erase the shift color/identity;
- today + selected must remain distinguishable as two simultaneous facts.

#### Notes / override / holiday

Use small secondary markers with stable positions. Avoid adding several full badges competing with shift identity.

Suggested stable marker zones:

```text
upper-right: special/holiday
lower-right: note
lower-left: individual override
```

M9 may adjust exact positions after stress testing.

### 4. Selected-day compact summary

When a day is selected, show a concise summary directly below the month grid.

Normal compact form:

```text
Today, 12 September                         >
[N icon] Night shift · 20:00–08:00
```

If multiple assignments exist, show a compact stacked summary or explicit count with the leading assignments.

This is not the full detail screen; it is the bridge between month scanning and day operations.

### 5. Quick assignment launcher / strip

The most-used templates for the active workplace are immediately reachable.

Normal compact representation:

```text
Quickly mark shift
[D] [N] [8] [custom] [More]
```

Each template uses its configured color and visual identity.

Include entry to:

- eraser;
- normal/no-brush mode;
- patterns/cycles;
- all templates/actions.

## Behavior states

### Normal

Swipe month enabled.

### Brush active

- month swipe disabled to prevent accidental navigation;
- active brush visually obvious;
- tapping/dragging dates applies brush according to current behavior;
- quick bar stays accessible;
- clear way to return to Normal.

### Pattern active

- pattern identity and selected range shown above/beside grid;
- preview range visually distinct from applied assignments;
- confirm/cancel visible without entering Settings.

### Clear-range active

- same interaction grammar as Pattern where possible;
- destructive color/wording reserved for actual clear confirmation.

## What not to do

- generic event dots as primary shift representation;
- giant day cards that reduce the month to a small header;
- month screen dominated by Today summary instead of the grid;
- hiding workplace filter in More;
- forcing a separate editor for every single-day assignment.

---

# CAL-02 — Day Detail

## Job to be done

Understand and operate on one selected date without losing calendar context.

## Presentation

Preferred phone behavior:

- expandable detail surface below Calendar when space allows;
- or bottom sheet / secondary surface that preserves easy return to the month.

A full-screen route is acceptable for deep editing, not required for simple inspection.

## Header

```text
12 September 2026, Saturday
```

Optional compact week strip above details may show neighboring days if it improves navigation, but must not duplicate the entire month grid.

## Assignment section

For each assignment:

- visual identity;
- shift title;
- time;
- workplace;
- paid/total hours if useful;
- expected day/shift pay if available;
- special compensation label if relevant.

Multiple assignments are separate rows/cards, not merged into an ambiguous total.

## Context rows

When applicable:

- alarm: next configured alarm/time + enabled state;
- note: short preview + open/edit;
- individual override: explicit marker + open details;
- holiday/special day: explicit type/status;
- expected earnings: one high-priority finance row, not a miniature payslip.

## Primary actions

Recommended first layer:

```text
Edit assignment | Alarm | Note | More
```

Secondary/destructive under More or lower action row:

- copy day;
- clear assignment/day;
- individual override;
- advanced details.

## Rule

Day Detail owns **operational detail**. Month cells own **pattern recognition**.

---

# CAL-03 — Quick Assignment / Brush

## Job to be done

Assign or clear many dates with minimum taps.

## Structure

### Header

- current month context;
- active mode;
- explicit close/back to normal.

### Template groups

First section: active workplace.

Each template tile includes:

- configured identity;
- color;
- short code/title;
- optional time if space permits in expanded view.

Additional sections:

- other workplaces when relevant;
- system statuses;
- recent templates;
- user favorites if the product later proves value for favorites.

### Tool row

Required:

- Eraser;
- Normal;
- Cycle/Pattern;
- More.

`More` may expose:

- new template;
- clear month;
- clear range;
- clear all calendar.

Destructive operations require current confirmations/behavior preservation.

## Interaction rule

A selected template turns the calendar into a brush surface. The UI must make this mode unmistakable and easy to leave.

---

# CAL-04 — Multi-workplace Representative State

This is a mandatory test state for all Calendar designs.

Representative scenario:

- one date has two assignments from different workplaces;
- another date has note + override;
- another has holiday/special marker;
- another uses emoji identity;
- another uses a Material icon;
- remaining dates use text codes.

Acceptance:

- month pattern remains readable;
- no assignment is hidden;
- non-color identity remains visible;
- selected/today states remain clear;
- markers do not overwhelm the cell.

---

# FIN-01 — Finance / Summary

## Job to be done

Answer `How much money?` in seconds.

## Header

```text
Finance                    [September 2026]
Summary | Calculation | Payments
```

Subtabs remain semantically valid; styling becomes lighter and clearer.

## Hero amount

Highest priority element:

```text
Expected net
184,511 ₽
```

Supporting micro-data may include:

- comparison with previous period;
- status vs expected;
- subtle trend visualization.

Avoid celebratory/motivational copy as required information. If such copy exists, it is optional personalization, not a payroll fact.

## Key breakdown

Immediately below:

- Gross;
- Tax/withholdings;
- Advance;
- Remaining salary / per-shift payout total.

Use strong value alignment and consistent money formatting.

## Work stats

Compact secondary block:

- worked shifts;
- paid hours;
- night/special counts only if useful as summary.

## Next payment

If payment scheduling applies:

- type;
- date;
- expected amount;
- status.

If per-shift payment mode applies, show the appropriate current payout logic rather than fake advance/salary rows.

## Trend

Optional short trend chart for recent periods.

Rule: chart supports understanding; it must not compete with the current amount.

## Entry to depth

A clear `Payslip / detailed calculation` action leads to FIN-03/FIN-04.

---

# FIN-02 — Finance / Payments

## Job to be done

See what should arrive, what actually arrived, and whether something is off.

## Primary card

Next/nearest payment:

- date;
- type;
- expected amount;
- days/time until payment when helpful.

## Period payment timeline/list

Each item:

- payment type;
- date;
- expected amount;
- actual amount when known;
- status: received / expected / planned / mismatch.

## Fact vs plan

Compact comparison:

```text
Expected: X
Received: Y
Difference: Z
```

Use error/warning emphasis only outside configured tolerance.

## Workplace breakdown

When multiple workplaces contribute:

- workplace name;
- amount;
- share of period;
- drill-down.

Do not imply workplace attribution where current payroll data cannot support it.

---

# FIN-03 — Finance / Calculation

## Job to be done

Explain how the result was formed without immediately becoming a dense company payslip.

## Controls

Top controls preserve:

- Month / Range / Year;
- period selection;
- workplace filter;
- Net / Gross;
- Compact / Detailed.

These controls should become visually calmer than the current all-in-one dense header.

## Primary result

```text
To receive
184,511 ₽
```

with optional delta/trend context.

## Accrual groups

Examples where supported by current result:

- base salary/hourly;
- day shifts;
- night premium;
- holidays/special days;
- overtime;
- vacation;
- sick leave;
- additions/bonuses.

Each row includes amount and may expose base/rate/hours through expansion.

## Deductions

Separate group:

- NDFL;
- other deductions;
- total deductions.

## Footer total

Persistent or repeated clear total:

```text
Total to receive 184,511 ₽
```

## Actions

- open full Payslip;
- export/report;
- diagnostics;
- calculation settings in contextual location.

---

# FIN-04 — Full Payslip

## Job to be done

Provide payroll-grade transparency for users who want the full calculation.

## Tone

This screen is allowed to be denser and more document-like than the rest of the app.

## Employee/work context

Where existing data supports it:

- profile/person;
- workplace;
- period;
- schedule/work mode.

Do not invent HR fields not present in product data merely because traditional payslips contain them.

## Accrual table

Columns/fields as supported:

- item;
- base/hours/days;
- rate;
- coefficient;
- amount.

## Deduction table

- tax base;
- tax/rate;
- amount;
- other deductions.

## Final net

Strong final row/card.

## Actions

- PDF;
- share/export where existing behavior supports it;
- diagnostics;
- visibility/customization if retained.

## Invariant

UI must display the current payroll result; it must not recompute differently from domain/payroll code.

---

# MORE-01 — More / Root

## Job to be done

Find less-frequent capabilities without making them permanent bottom-navigation competitors.

## Header

```text
More                                      [Search]
```

Search is desirable because the product has many advanced features, but its implementation can be deferred if it expands M9 scope.

## Group: Work

- Workplaces;
- Shift templates / statuses;
- Profiles if retained as separate management.

## Group: Tools

- Alarms;
- Notes;
- Assistant.

Assistant may carry a small `Experimental`/provider-config status only if product wording is appropriate; do not visually promote it above deterministic core tools.

## Group: App

- Appearance;
- Widgets;
- Wear / Watches;
- global notifications/preferences where truly app-level.

## Group: Data

- Backup / Restore;
- Google Drive;
- Import / Export;
- report history if Finance does not own the entry.

## Group: Advanced / Service

- health check;
- event log;
- diagnostics/service utilities.

## Rule

More is grouped and scannable. It is not one undifferentiated list of every old Settings row.

---

# WORK-01 — Workplace Detail

## Job to be done

Manage one workplace as a first-class entity.

## Hero/header

- workplace name;
- active status;
- optional color/identity;
- edit/rename.

## Payroll context

- pay mode/rate summary;
- additions/special rules entry;
- payroll configuration entry;
- calculated current-period snapshot when useful.

## Shift templates

List templates belonging to this workplace:

- configured visual identity;
- title/code;
- time;
- edit.

## Default schedule/pattern

Expose relevant workplace schedule defaults/pattern entry where existing behavior supports it.

## Destructive

Archive/remove workplace is visually separated and guarded.

## Rule

Workplace-specific settings should not require navigating through global Settings first.

---

# APP-01 — Appearance

## Job to be done

Customize the application without presenting every visual axis as an equally large card.

## First level

High-value appearance controls:

- theme: Light / Dark / System/Auto / Schedule;
- palette: existing presets, Dynamic/Material You, Custom;
- density: Comfortable / Compact;
- contrast;
- interface style: Classic / Expressive / Expressive Glass.

## Second level / advanced

- corner style;
- animation speed;
- custom colors;
- font selection;
- per-section font overrides if retained after M9 review.

## Live preview

Retain a compact live preview, but make it representative of the real design system rather than a decorative isolated widget.

Ideal preview includes:

- one calendar cell/row;
- one money value;
- one surface/button.

## M9 review requirement

Every existing appearance option must be classified as:

- retained as-is;
- retained with changed scope;
- merged into another option;
- removed with an explicit migration/default decision.

No silent disappearance.

---

# TODAY-01 — Personalized Overview

## Job to be done

See a personalized snapshot of the day without replacing the schedule-planning role of Calendar.

## Entry

Not required as a persistent primary nav item.

Possible entry mechanisms to validate later:

- Calendar header/current-day affordance;
- selected today's day-detail;
- optional shortcut;
- widget/deep link;
- optional fourth nav item only if usage justifies it.

## Default block order proposal

1. current shift / earnings hero;
2. next step / alarm;
3. tomorrow;
4. month progress;
5. upcoming payments;
6. notes.

The actual order remains user-configurable.

## Hero

May show:

- today shift visual identity;
- workplace;
- time;
- expected day earnings when available.

Avoid generic motivational content as a mandatory product element.

## Customization

Preserve:

- reorder;
- show/hide;
- width/size where still compatible with new design system;
- background/accent choice subject to M9 simplification review.

---

# TODAY-02 — Overview Layout Editor

## Job to be done

Customize Today without entering a large global Settings tree.

## Row model

Each block row:

- drag handle;
- block identity;
- size/width selector where retained;
- visibility toggle.

Hidden blocks shown in a separate section for restoration.

## Visual-style controls

Block-style selection may remain contextual here rather than in global Appearance if it affects only Today cards.

---

# Focused visual validation matrix

Before M8 is declared complete, the selected Variant A direction must be visually validated against this exact matrix.

| Screen | Required state |
| --- | --- |
| Calendar Month | ordinary text-code shifts |
| Calendar Month | emoji + Material icon shifts |
| Calendar Month | multi-workplace assignment on same day |
| Calendar Month | note + override + holiday markers |
| Calendar Month | active brush / range preview |
| Calendar Day | one assignment + pay/alarm/note |
| Calendar Day | multiple assignments |
| Finance Summary | ordinary twice-monthly payroll |
| Finance Summary | per-shift pay mode |
| Finance Payments | expected vs actual mismatch |
| Finance Calculation | compact and detailed modes |
| Payslip | dense full calculation |
| More | full grouped taxonomy |
| Workplace | workplace with multiple templates |
| Appearance | current personalization categories mapped |
| Today | standard personalized overview |
| Today Editor | reorder/hide/size state |

## M8 visual acceptance questions

For every render/prototype, reviewers should answer:

1. Can the next week's work pattern be understood in roughly one glance?
2. Are text/icon/emoji shifts equally viable?
3. Can multiple workplaces coexist without ambiguity?
4. Are notes/overrides/holidays visible but secondary?
5. Is Calendar clearly the product home rather than merely one card among many?
6. Does Finance give the main amount immediately?
7. Can a user reach full payroll detail without cluttering the Summary?
8. Is More structured enough that removal of five old bottom tabs does not make features feel lost?
9. Does the design feel personal and modern rather than industrial/enterprise?
10. Does visual expressiveness preserve information clarity?

A render that looks attractive but fails questions 1–8 is not accepted as the M8 design direction.
