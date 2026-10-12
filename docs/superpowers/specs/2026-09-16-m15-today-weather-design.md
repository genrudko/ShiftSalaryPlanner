# M15 Today + Weather Design

**Date:** 2026-09-16
**Status:** owner-approved design; implementation starts with M15.1
**Baseline:** `feature/m14-notes-assistant` at `e7ac501`
**Working branch:** `feature/m15-today-weather`

## 1. Product intent

`Today` becomes the operational center for the current day. It complements the Calendar-first workflow instead of becoming a second calendar or a generic launcher dashboard.

The screen answers three questions in order:

1. **What is happening now?** — date, active workplace/location, current/next shift, time-to-start/end, compact weather, approximate current-shift earnings when already available from existing payroll state.
2. **What happens next?** — one chronological timeline assembled from independent sources.
3. **What else do I want visible today?** — configurable lower modules such as Notes, Tomorrow, Payments and Month status.

The existing free-form bubble engine is retained for lower modules, but the top operational hierarchy is fixed so personalization cannot bury the current-day context under secondary cards.

## 2. Fixed screen hierarchy

### 2.1 Hero / Now

The Hero is always the first operational block. It may show:

- date;
- active workplace and weather-location label;
- current or next shift summary;
- countdown to shift start/end;
- existing payroll-derived current-shift amount when available without new payroll arithmetic;
- compact weather summary when M15.2 is available.

The Hero must not contain the full weather table or every secondary metric.

### 2.2 Timeline

`TodayTimelineAggregator` consumes typed source entries and produces a single ordered list of `TimelineItem` values.

Initial source kinds:

- shift start/end;
- shift alarms;
- Notes v2 reminders;
- Finance/payment events already derivable from existing state;
- read-only external calendar occurrences;
- weather threshold/significant-change events from M15.2/M15.3.

The UI does not query source-specific stores directly. It renders normalized timeline items and invokes typed actions supplied by the integration layer.

### 2.3 Expandable Weather

M15.2 adds an expandable weather module below the timeline. Collapsed mode is concise; expanded mode can expose current, hourly and daily layers according to a preset or custom configuration.

### 2.4 Configurable lower modules

Existing `TodayLayoutSettings` remains the persistence boundary for secondary Today modules. Legacy bubble IDs remain parseable for compatibility, while `HERO`, `PULSE`, `NEXT_STEP` and historical `QUICK_ACTIONS` are no longer independent movable modules after migration.

Supported lower modules initially remain:

- Tomorrow;
- Next alarm shortcut if still useful after timeline qualification;
- Month status;
- Payments;
- Notes.

Weather is added as its own configurable lower/expanded module in M15.2 without making the fixed Hero dependent on the lower-module visibility switch.

## 3. Timeline contract

The normalized model uses stable source identity and does not copy source persistence models into UI state.

```text
TimelineItem
- stableId: String
- source: TimelineSource
- kind: TimelineKind
- startMillis: Long
- endMillis: Long?
- title: String
- subtitle: String?
- severity: TimelineSeverity
- sourceLabel: String?
- colorArgb: Int?
- action: TimelineAction?
```

`TimelineSource` includes `SHIFT`, `ALARM`, `NOTE`, `FINANCE`, `EXTERNAL_CALENDAR`, `WEATHER`.

`TimelineAction` is deliberately narrow: open Calendar/day, open Alarms, open Note, open Finance, open external event. Creation of external events is a separate Android intent action and does not create an app-owned event.

Ordering rules:

1. chronological by `startMillis`;
2. deterministic tie-break by source priority and `stableId`;
3. expired items may be retained in a visually de-emphasized earlier-today section or filtered according to a Today setting, but ordering remains deterministic;
4. weather sources emit only significant events, never every raw forecast point.

## 4. External calendar layer

External calendar data is **read-only**.

- Android Calendar Provider is the first implementation boundary.
- `READ_CALENDAR` is requested only when the user enables the layer.
- No `WRITE_CALENDAR` permission is added.
- `+ Event` launches the system calendar insert UI via `ACTION_INSERT`; the system calendar remains the source of truth.
- Recurrence, exceptions, account synchronization and conflict handling remain owned by the external calendar provider.
- The layer can be disabled globally and individual visible calendars can be enabled/disabled.
- External occurrence identity retains provider calendar/event/instance identity sufficiently to avoid collapsing recurring instances.

No local `PlannedEvent` database is introduced in M15.

## 5. Weather architecture

M15.2 introduces a provider-independent weather subsystem:

```text
WeatherProvider(primary) ----\
                              -> WeatherRepository -> NormalizedWeather
WeatherProvider(fallback) ---/
```

The UI never branches on provider identity. Provider-specific payloads are normalized first.

### 5.1 Provider behavior

- configurable primary provider;
- configurable fallback provider;
- fallback on transport/server failure or absence of required capability;
- source and fallback state are visible but unobtrusive;
- last successful payload is cached with acquisition/update timestamps;
- stale data remains visible with a stale marker instead of clearing the screen.

### 5.2 Locations

Supported sources:

- active Workplace location — default;
- current device location;
- SavedLocation;
- manual search/map selection for Workplace or SavedLocation.

Coordinates are authoritative; human-readable names/addresses are display metadata. Temporary Today override does not destroy the Workplace default.

### 5.3 Normalized units

Internal canonical values are provider-independent; formatting converts at the presentation boundary.

User-configurable display families include:

- temperature: C/F;
- wind/gust: m/s, km/h, mph, knots;
- pressure: mmHg, hPa, mbar, inHg;
- precipitation: mm/in;
- visibility: km/miles;
- wind direction: compass, degrees, or both.

A single Metric/Imperial switch is insufficient and is not the primary model.

### 5.4 Weather presentation presets

Two built-in presets plus Custom:

- **Basic** — temperature, feels-like, precipitation, wind, humidity, pressure;
- **Work/Wind** — wind, gusts, direction, pressure + trend, precipitation, temperature, visibility;
- **Custom** — visibility, ordering, units/format and supported display style per metric.

Editing a built-in preset produces a Custom configuration rather than mutating the preset definition.

### 5.5 Forecast layers

Independent configurable layers:

- Current;
- Hourly horizon: 6/12/24/48 h;
- Daily horizon: 3/5/7/10 days;
- Significant weather events for timeline.

Layers can be enabled/disabled and ordered. Complex customization lives under Custom so default setup remains simple.

## 6. Weather rules

M15.3 introduces rules rather than scattered threshold toggles.

```text
WeatherRule
- ruleId
- metric
- comparator/change condition
- threshold
- duration/window
- severity
- timelineEnabled
- weatherHighlightEnabled
- notificationEnabled (default false)
```

Examples include sustained/gust wind thresholds, precipitation intensity, and pressure change over a time window.

Rules generate significant timeline events only when a semantic transition occurs (start/end/threshold crossing or other explicitly modeled change). They do not mirror every hourly forecast sample.

### 6.1 Inheritance

- global rules are the base;
- Workplace and SavedLocation can inherit, override, disable or add rules;
- stable `ruleId` ties an override to its base rule;
- reset-to-global removes local overrides without altering global rules.

System notifications are off by default and enabled explicitly per rule.

## 7. Weather refresh policy

Refresh is configurable, not hard-coded:

- 15 min;
- 30 min;
- 1 h;
- 2 h;
- only on opening Today;
- Auto;
- background off.

Manual refresh is always available. Today can refresh on open when cached data exceeds a configurable freshness threshold. Wi-Fi-only/roaming constraints remain optional settings. Android scheduling limitations are treated as scheduling constraints, not exact-timer promises.

## 8. Settings rationalization

M15.4 finishes remaining settings cleanup using progressive disclosure:

- top-level global settings contain only cross-entity defaults;
- Workplace weather location/rule overrides live with Workplace;
- SavedLocation weather overrides live with SavedLocation;
- Today layout customization is reachable from Today;
- provider, units, refresh and global weather rules live in Weather settings;
- external calendar layer configuration lives with Today/calendar integrations rather than a new calendar product section.

## 9. Compatibility and non-goals

Must preserve:

- payroll formulas and existing payroll source values;
- schedule persistence and Room schema unless a later weather/location persistence task explicitly requires a migration;
- alarm scheduling behavior;
- Notes v2 authority and compatibility projection;
- backup compatibility;
- Wear sync contract unless explicitly changed in a later bounded Wear task;
- old Today layout JSON must continue to parse.

Explicit non-goals:

- no app-owned second calendar;
- no editing external calendar events;
- no external-calendar bidirectional sync;
- no collaborative calendars/attendees/RSVP implementation;
- no weather radar/maps in the first weather slice;
- no provider-specific conditionals in Today UI;
- no incidental payroll, Room, alarm or dependency modernization.

## 10. Milestone slices

### M15.1 — Today foundation

- extract Today presentation from `WorkflowServiceScreens.kt` into a focused package;
- introduce timeline model/aggregator;
- build fixed Hero + Timeline hierarchy;
- preserve lower module customization;
- add read-only external calendar source/layer;
- add system `+ Event` action;
- deterministic visual qualification.

### M15.2 — Weather subsystem

- provider interface/repository/normalization;
- primary+fallback;
- Workplace/current/saved locations;
- cache/stale semantics;
- units;
- Basic/Work/Custom presentation;
- current/hourly/daily expandable UI.

### M15.3 — Weather rules + background behavior

- rule engine;
- global + location overrides;
- significant timeline weather events;
- optional notifications default-off;
- configurable refresh/background schedule.

### M15.4 — Today customization + settings rationalization

- final lower-module customization polish;
- remove obsolete Today concepts only after compatibility is proven;
- consolidate remaining global settings according to entity ownership;
- final M15 visual/regression qualification.

## 11. Acceptance direction

M15 is complete only when Today has a clear operational hierarchy, external calendar remains an optional read-only source, weather is provider-independent and deeply configurable without overwhelming default users, and existing payroll/schedule/alarm/Notes behavior remains protected by regression gates.
