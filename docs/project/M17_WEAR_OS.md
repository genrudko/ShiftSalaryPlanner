# M17 — Wear OS / Data Layer compatibility

Date: 2026-10-12
Milestone: **SOFTWARE IMPLEMENTATION COMPLETE / VERIFIED ON VPS; NOT MERGED OR RELEASED**.

## Provenance and boundaries

- Source: `f401c859244b7f3aa4243f5a85f749b46de4bd28`, the final M16 isolated tip.
- M17 isolated tree: `/home/admin/workspaces/shift-salary-planner/m17-wear-os`, branch `work/m17-wear-os`.
- First implementation: `ccf01aa`. The canonical `origin/master` remains `c30f7fe4512a605c63a0a7b9cbb23e9281a7a1b6`; M16 and M17 are **not merged/pushed or released**.
- No payroll algorithm, shift schema, Room migration, Notes v2 document format, backup wire format, dependency version upgrades, or phone alarm scheduler behavior is changed.
- Phone and Wear share a versioned **wire DTO boundary**, not Compose/ViewModel/Room types. Paths/constants remain mirrored in the two existing modules to avoid adding a Gradle module in a compatibility milestone.

## Data Layer contract

The canonical v1 snapshot DataItem remains at `/shift_salary_planner/snapshot`, `snapshot_json` and `generated_at`. The JSON includes `schemaVersion=1`, `generatedAt`, `today` (`YYYY-MM-DD`), `month` (`YYYY-MM`), full current-month calendar with workplaces/shift identifiers, payroll summary, alarm status/templates/upcoming, and the newest 16 legacy-compatible Notes v2 projections. Existing message paths for snapshot request, note creation, global/template alarm controls and ring/dismiss/snooze remain unchanged. This preserves existing older paired-client vocabulary.

Phone production:
- Snapshot sends are serialized with a coroutine Mutex and assigned strictly increasing in-process `generatedAt` timestamps; out-of-order parallel publishes cannot race.
- Exact duplicate content is not urgently re-published on every UI recomposition. An explicit watch request forces a new published snapshot even when unchanged, supporting newly connected companions.
- Wear note records contain previews only (`title <= 96`, `body <= 420` UTF-16 code units) instead of unlimited rich text/media. The app enforces a 95,000-byte UTF-8 envelope budget below the 100-KB DataItem limit. A pathological over-budget payload fails explicitly rather than being silently truncated inside the transport. Full notes remain authoritative on the phone.

Wear consumption:
- The v1 parser accepts already deployed v1 payloads without an explicit `schemaVersion`, and ignores forward-compatible extra JSON fields. It rejects unknown schema versions, invalid/nonpositive generation timestamps, malformed day/month, absent calendar/payroll/alarms/notes and malformed JSON.
- The background listener and foreground repository use one synchronized persisted cache; older/equal generations never roll back the cache. A foreground Data Layer scan selects the newest valid item across candidates rather than whichever item was encountered last.
- Successful durable cache changes invalidate both Today Tile and the shift Complication, even with the Watch Activity closed.
- The Today screen, Tile and Complication do not present an old snapshot's shift as today's when its `today` differs from the watch's current local day. Previous data remains available offline with a stale-state indication.
- The Watch money-visibility setting persists across restarts, applies to the Tile as well as Activity, and invalidates passive surfaces when changed.
- Watch Notes no longer silently discard the draft immediately after a MessageClient transport ACK. A fresh note with a new ID must be observed in the next phone snapshot before the draft is cleared. Failed/offline actions keep editable text; the UI displays the pending state.
- Removed the obsolete AI label from Wear voice note service description; the cancelled Assistant is not revived.

## Verification

- Focused pre-final compilation job `f7d5677e67f34190870da11cd6977eaa`: Wear test/APK + phone Kotlin compile **GREEN in 2m55s**. This run preceded the last Tile/privacy/notes UI edits, therefore is not the final complete-source acceptance.
- New Wear JVM tests: v1 DTO parsing, implicit-v1 compatibility, future-schema rejection, malformed/incomplete payload rejection, monotonic cache policy, and stale-day detection; prior Wear JVM was NO-SOURCE. Local JVM tests are not a paired Google Play services Data Layer integration test.
- **FINAL CROSS-FEATURE GATE PASS:** VPS job `f064089acc98da495fa4df7839c8524d`, terminal success, **BUILD SUCCESSFUL in 8m42s**, 109 Gradle tasks (68 executed, 41 up-to-date). `:app:testDebugUnitTest` **439/439** in 83 suites; `:wear:testDebugUnitTest` **6/6** in 1 suite, 0 errors/failures/skips. `:app:validateDebugScreenshotTest` success against **58** committed references; both APK assemblies and lint tasks success. Android lint **0 errors, 79 warnings, 12 hints**; Wear lint **0 errors, 23 warnings, 3 hints**. Android APK SHA256 `80cc0c6541865bc303b39f8062b4f54cb71fa8290e5ae23eb781861ec4cb62b4`; Wear APK SHA256 `eee9e831bf2ebd9421aadd5309bd0b702c1a53446e85a4421577914674f67ae6`. No instrumentation or paired-watch tests are implied by these Gradle tasks.
- Set `ANDROID_HOME=/opt/project-workbench/android/sdk` for this isolated worktree. The initial misconfigured invocation did not build and did not indicate a code failure.

## Explicit physical-device / M20 acceptance boundary

Production acceptance still requires a real paired Android phone + Wear OS watch, correct package identity and release signature on both, transport with Bluetooth/Wi-Fi disconnections and reconnects, wake-from-background Data Layer delivery, today rollover, Tile/Complication update in the launcher, notification permission/full-screen alarm behavior, note-creation confirmation and unsupported-version handling. Watch-originated alarm snooze/dismiss must be tested against a ringing phone. Restore/upgrade from installed old builds is separate M20 work.

**Standalone Wear OS 6 emulator smoke PASS:** created AVD `ssp_m17_wear_api36` using installed API 36 `android-wear-signed/x86_64` image. Emulator booted (`sys.boot_completed=1`); installed the final `wear-debug.apk` via ADB (`Success`), launched `com.vigilante.shiftsalaryplanner/.wear.MainActivity`, and confirmed the process remained alive (PID 2611), `topResumedActivity` was the app after virtual battery unplug, and no AndroidRuntime FATAL exception was reported. Native `uiautomator dump` confirmed the `androidx.compose.ui.platform.ComposeView` / active "Сегодня" TextView nodes in the 384×384 Wear window. This is a **real headless standalone Wear emulator screen/launch smoke** but not a paired Data Layer, Tiles/Complications rendering, real-watch Bluetooth/notification, or a UI screenshot quality review. The AVD can be recreated from the SDK image for subsequent tests.

## Known constraints

- **Payroll parity limitation established by source audit:** Wear currently calculates its monthly salary preview from the global `PayrollSettingsStore`, whereas the phone Finance feature additionally applies workplace-specific overrides, selected-workplace filters and effective normative hours. Therefore, Wear amounts cannot be claimed equivalent to every Finance filter or payroll configuration. M17 deliberately does not rewrite this protected accounting logic without an independently verified common calculation source; reconciliation is a separate bounded payroll-safety task before production M20.
- A full rich-text/media note editor remains on the phone; Wear only receives bounded plain-text preview and can create a new plain note through the Notes v2 compatibility adapter.
- The Watch note draft is retained if a phone-side write cannot be confirmed. A user-initiated repeat before an eventual delayed success may still create a duplicate; no cross-device idempotency token is introduced in the legacy v1 message vocabulary.
- `generatedAt` is wall-clock epoch milliseconds; a major backwards phone clock adjustment across app process restarts can temporarily delay acceptance on a Watch with a newer cached stamp. It is safer than accepting arbitrary out-of-order items, and a further protocol revision would need a persistent device identity/sequence design.
- The snapshot budget check is an explicit guard, not a proof every possible user-created calendar/title combination is within 95 KB. No silent deletion or replacement of phone data takes place on oversize.
