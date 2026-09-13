# ShiftSalaryPlanner Semantic Iconography + M13 Declutter Design

## Status

Accepted owner direction on 2026-09-13: use one coherent iconography across the app and recompose M13 with **Variant B** — a light operational Alarms root plus separate Settings and System Status drill-down screens. Preserve functionality.

## Goals

1. Replace interface-letter/glyph placeholders (`Р`, `С`, `Б`, `Aa`, `G`, `X`, etc.) with stable semantic Material Rounded icons.
2. Make one app-level semantic icon vocabulary reusable by navigation, More and later migrated screens.
3. Keep shift-template identity separate: user-controlled `iconKey`, emoji/glyph/code and template color remain data identity and may still render text where intentional.
4. Keep every M13 alarm capability, persistence source and callback while reducing simultaneous visual density.

## Non-goals / invariants

- No new icon dependency and no custom SVG pack in this slice.
- No Room schema/migration, backup payload, payroll arithmetic, alarm scheduler, `ShiftAlarmStore`, Wear sync protocol or persistence-format change.
- No removal of alarm settings or permission actions.
- Existing external/direct `ALARMS` entry continues to open the operational Alarms root.
- `ShiftTemplateAlarmConfig` remains the link between shift templates and per-shift alarm configuration.

## Semantic icon catalog

Create `ui/common/AppSemanticIcons.kt` with `AppSemanticIconKey` and `appSemanticIcon(key): ImageVector`. UI navigation stores semantic keys instead of ad-hoc one-letter glyphs.

Initial vocabulary:

- Calendar → `CalendarMonth`
- Today → `Today`
- Finance → `Paid`
- More → `MoreHoriz`
- Workplaces → `WorkHistory`
- Shift templates → `Badge`
- Production calendar → `CalendarMonth`
- Manual holidays → `BeachAccess`
- Alarms → `Alarm`
- Notes → `EventNote`
- Assistant → `AutoAwesome`
- Appearance → `Palette`
- Widgets → `Widgets`
- Quick actions → `Bolt`
- Wear → `Watch`
- Profiles → `AccountCircle`
- Quick start → `Lightbulb`
- Backup → `Backup`
- Cloud backup → `Cloud`
- Import/export → `SwapVert`
- Reports → `Assessment`
- Health check → `FactCheck`
- Event log → `History`
- Current parameters/settings → `Tune`
- Alarm settings → `Settings`
- System status → `VerifiedUser`
- Test alarm → `NotificationsActive`
- Reschedule → `Autorenew`

If one exact Material symbol is unavailable in the current dependency set, choose the closest Material Rounded synonym in this same semantic family; do not fall back to letters.

`EvolutionIconTile` keeps optional `glyph` support because it is valid for user-owned shift identity, but application-menu destinations use `icon` exclusively.

## M12 / More presentation

`MoreDestination` stores `AppSemanticIconKey` rather than `glyph: String`. `MoreDestinationRow` resolves the icon through the catalog. Grouping and all callbacks remain unchanged. The result should preserve the accepted M12 composition while making each row visually recognizable at a glance.

## M13 information architecture — Variant B

### Operational root: `Будильники`

The root answers only “what is going to ring and what do I need to do now?”

Order:

1. Header.
2. `Ближайшие будильники`.
3. Readiness warning only when readiness is not OK. When ready, no full readiness card occupies root space; status is summarized in the System Status entry.
4. Compact master controls: `Будильники` and `Автоперепланировка`.
5. Compact quick actions: `Тест звонка` and `Перепланировать`.
6. Template summary + existing shift-template alarm config list.
7. Two Evolution action rows:
   - `Настройки будильников` — Wear, planning horizon, ring appearance, behavior/sound.
   - `Системный статус` — permissions/readiness and last reschedule result.

The old giant inline `Общие настройки` and `Системный статус` sections are removed from the root only; their capabilities move to the drill-down pages.

### Drill-down: `Настройки будильников`

This screen reuses the same state/actions and contains:

1. Wear OS mirror switch and Wear sound mode.
2. Planning horizon field.
3. `Экран звонка` entry opening the existing ring appearance dialog.
4. `Поведение звонка` entry opening the existing behavior dialog.

No setting is duplicated with a second persistence source.

### Drill-down: `Системный статус`

This screen contains:

1. Notification permission state + existing request action when missing.
2. Exact alarm capability + existing Android settings action when missing.
3. Full-screen intent capability + existing Android settings action when missing.
4. Last reschedule result/feedback when present.

The root action row subtitle becomes `Готово` when all readiness requirements pass, otherwise a concise `Нужны разрешения` summary.

### Navigation model

Use an internal M13 presentation page enum/state (`AlarmScreenPage.ROOT / SETTINGS / SYSTEM_STATUS`) inside `ShiftAlarmsTabScreen`. Back from either drill-down returns to ROOT; direct external `ALARMS` entry starts at ROOT. This avoids adding public navigation destinations solely for local alarm settings and keeps existing callbacks/persistence intact.

## Visual qualification

Required final deterministic screenshots:

- M12 More Light, Dark, fontScale 1.3 after semantic icon migration.
- M13 Alarms root Light/upcoming, Dark, fontScale 1.3, permission-warning.
- M13 Alarm Settings Light and fontScale 1.3.
- M13 System Status ready and permission-warning.
- Existing Templates/editor states continue to validate unless intentionally changed by catalog adoption.

Visual acceptance criteria:

- no interface-letter placeholders in More;
- no clipped switch/action at fontScale 1.3;
- root Alarms is materially shorter and does not inline Wear/horizon/ring/diagnostic details;
- every previous alarm capability remains reachable within at most one drill-down from root;
- Dark uses the same Evolution low-noise surface grammar.
