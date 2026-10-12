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
| Font scaling | Calendar cell-height growth previously stopped at 150% even though Android font scaling can reach 200%; text then risked clipping | Remove cap and add scale 1.0/1.5/2.0/2.5 unit assertions | JVM pass and emulator 200% inspection |\n| Contrast | Dark calendar selected-date badge was white on light lavender (~1.9:1), below WCAG AA 4.5:1 for small text | Select preferred text if readable or guaranteed black/white, with explicit relative-luminance contrast tests | Visually review changed goldens and validate all 58 |
| Battery and alarms | Scheduler uses AlarmManager alarm-clock/inexact fallback and cancels/recreates keys on reschedule; weather uses persisted periodic JobScheduler >=15 minutes | No speculative alarm/notification semantic changes; audit scheduling frequency and receivers | Static review plus emulator system behavior; OEM physical device deferred to M20 |

## Verification ledger

- M18 baseline: app JVM 439, Wear JVM 6, screenshot 58, app/Wear lint zero errors. M18 Android 36 instrumentation 4 tests.
- M19 first focused JVM and compile: 5/5 new tests, job `100637fe341754e4e2f94e61dad2a213`, `BUILD SUCCESSFUL`. Later Android full JVM **446/446, 84 suites**, no failures/errors/skips, as of split job `423579b847033526df022d005758a0a8`; after adding one more font 200% test expect 447, requalification pending.
- M19 split screenshot update/Android APK + JVM job `423579b847033526df022d005758a0a8`: `BUILD SUCCESSFUL in 2m49s`. Two reviewed calendar refs changed exactly 390 pixels each (34x23 at +781+673, selected day numeral only) and accepted `057536d`; other 56 refs unchanged. Validate all 58 against accepted tree pending. Wear JVM 6/6 + APK and Wear lint success as tasks in previous combined gate `4a5502e8395f45091c265976505a0566`; combined job was cancelled because its Android lint stage contested with Android emulator and prolonged CPU contention. Independent Android lint job `24269b1ecad25a5a8ccda888efd98635` active; do not claim result until terminal.
- M19 Android 36 emulator instrumentation, startup/semantics: pending.
- Notes/alarms backup and payroll contracts: pending regression gate.
- Final integration: not authorized by starting M19; branch remains isolated until separate owner instruction.
