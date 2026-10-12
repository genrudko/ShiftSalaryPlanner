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
| Font scaling | Calendar cell-height growth previously stopped at 150% even though Android font scaling can reach 200%; text then risked clipping | Remove cap and add scale 1.0/1.5/2.0/2.5 unit assertions | JVM pass and emulator 200% inspection |
| Contrast | Dark calendar selected-date badge was white on light lavender (~1.9:1), below WCAG AA 4.5:1 for small text | Select preferred text if readable or guaranteed black/white, with explicit relative-luminance contrast tests | Visually review changed goldens and validate all 58 |
| Battery and alarms | Scheduler uses AlarmManager alarm-clock/inexact fallback and cancels/recreates keys on reschedule; weather uses persisted periodic JobScheduler >=15 minutes | No speculative alarm/notification semantic changes; audit scheduling frequency and receivers | Static review plus emulator system behavior; OEM physical device deferred to M20 |

## Verification ledger

- M18 baseline: app JVM 439, Wear JVM 6, screenshot 58, app/Wear lint zero errors. M18 Android 36 instrumentation 4 tests.
- Source code tested through calendar date and header accessibility consolidation: split Gradle job `d42b3e016f12a07442957dc549572e68` **BUILD SUCCESSFUL in 2m25s**: Android JVM **449/449 (84 suites)**, Wear JVM **6/6**, all **58/58 screenshot references**, Android+Wear APK, Android instrumentation APK, Wear lint. Follow-up navigation semantics source `NavigationBarItem/NavigationRailItem` plus extra JVM regression test requires one final current-tree gate (`01a4061cea5cb9514d372fb33bd7ec62`, in progress).
- Original screenshot references updated **exactly two** cases, each 390 changed pixels in selected-date text glyph at 34x23 region; 56 unchanged. Reviewed old/new pixel crop before commit `057536d`; full subsequent 58/58 visual validation passed.
- App lint independent `24269b1ecad25a5a8ccda888efd98635` **BUILD SUCCESSFUL in 5m09s**, 0 errors, 79 warnings, 12 hints. Wear lint 0 errors (previous final gate). Fresh post-navigation app lint included in current-tree gate; final completion still pending.
- Real Android 36 instrumentation `42809c00e8a7c3bb7d075a5b45260ffa`: **OK (5 tests)** including M19 Room inclusive range, sorted results, unrelated history excluded. Device 200% system font confirmed calendar cells scale.
- Android accessibility UIAutomator inspection after `clearAndSetSemantics`: 31/31 visible October dates each with text description and clickable action **on the same node**. Separate previous/next month, workplace selector, profile selector and quick picker all named and clickable. Three navigation tabs `Календарь/Финансы/Ещё` then named and clickable; no blank clickable nodes remained. Native `selected` attributes reported false for all despite functional tab switching, so explicit spoken `Текущий раздел` was added to the active navigation label; pending final device recheck.
- Cold-start five-trial emulator M18 baseline: 8242/7642/5853/5227/4393 ms; M19: 4751/5033/4517/4489/4537 ms (force-stop, same AVD, sequential installs). Large monotonic M18 warmup and order confounding prevent asserting statistically supported startup speedup. Root signature digest is now lazy; no cold-start benchmark claim is made.
- Alarm exactness, weather job battery behavior and actual TalkBack speech/large-font visual usability on OEM hardware remain M20 device-acceptance obligations; no scheduling behavior changed. Financial calculation, Room schema version, backup/restore serialization and Wear wire compatibility are covered by passing regression suite without modification.
- No merge, remote push, signing or production release authorized. Final branch closeout after current-tree terminal qualification.
