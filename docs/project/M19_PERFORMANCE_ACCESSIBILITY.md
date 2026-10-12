# M19 — Performance & Accessibility

Date: 2026-10-12
Status: **IN PROGRESS — qualification not yet final**

## Source and immutable boundaries

- Canonical base: clean GitHub/local `master` at `77423899482e99e2ee1a7ab6152a6391e66f1412` (M18 integrated/verified).
- Isolated worktree: `/home/admin/workspaces/shift-salary-planner/m19-performance-accessibility`; branch `work/m19-performance-accessibility`.
- No payroll algorithm, Room schema version, backup format, profile serialization, release signing, Android target SDK, alarm scheduling semantics, or Wear wire-contract change is authorized by M19.

## Findings and bounded changes

| Area | Observation | Intervention | Evidence needed |
|---|---|---|---|
| Startup | App signing certificate was decoded/digested eagerly from root Compose state despite being needed only by backup/OAuth diagnostics | Remember a lazy certificate diagnostic, evaluate on actual backup screen access | Android launch profiling and compile |
| Calendar geometry | Cell bounds were written to Compose observable `mutableStateMapOf` from layout callbacks, with `cellBounds.size` in pointerInput keys; 35–42 writes can invalidate state and restart drag recognition | Replace with plain month-scoped mutable map, remove size-dependent restart | Geometry/gesture unit tests + screenshots |
| Calendar layout | Calendar weeks reconstructed during each grid composition | Pure `calendarVisibleWeeks` cached by month | Leap/six-week tests |
| Widget DB | Month RemoteViews factory loaded `observeAll().first()` for all historical shifts to display 42 cells | Indexed ISO-date `BETWEEN` DAO query covering exactly 42 widget dates | Android Room boundary instrumentation |
| Room DB ownership | Volatile reference to mutable hash map allowed concurrent unsynchronized reads/writes by widgets and main UI | ConcurrentHashMap with same guarded builder semantics | Android instrumentation |
| TalkBack | Calendar cell visually encoded notes, overrides, multi-workplace and selection without a complete combined spoken label; Notes grid/list and pin relied on color/immutable label | Add deterministic Russian spoken day label, selected semantics and labelled actions for Notes | JVM assertions and real Android accessibility-tree inspection |
| Font/contrast | Large-font screenshots exist from M9–M18 at scale 1.3, but these are not evidence of every screen at maximum scale or of WCAG contrast | Qualify representative scaling and report remaining limits | Screenshot/device checks |
| Battery and alarms | Scheduler uses AlarmManager alarm-clock/inexact fallback and cancels/recreates keys on reschedule; weather uses persisted periodic JobScheduler >=15 minutes | No speculative alarm/notification semantic changes; audit scheduling frequency and receivers | Static review plus emulator system behavior; OEM physical device deferred to M20 |

## Verification ledger

- M18 baseline: app JVM 439, Wear JVM 6, screenshot 58, app/Wear lint zero errors. M18 Android 36 instrumentation 4 tests.
- M19 focused JVM and compile: pending.
- M19 full app/Wear + screenshots + lint: pending.
- M19 Android 36 emulator instrumentation, startup/semantics: pending.
- Notes/alarms backup and payroll contracts: pending regression gate.
- Final integration: not authorized by starting M19; branch remains isolated until separate owner instruction.
