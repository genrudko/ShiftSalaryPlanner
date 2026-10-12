# M16 — Integration Hardening: implementation and qualification

Entry baseline: `origin/master=c30f7fe4512a605c63a0a7b9cbb23e9281a7a1b6` (2026-10-12).
Isolated worktree: `work/m16-integration-hardening`. Implementation commit: `1c67546` after the earlier M16 input-validation and Drive paging commits.
**State:** M16 implementation COMPLETE / VERIFIED LOCALLY on isolated branch, including Android 36 instrumentation. Real Google-account and physical-device acceptance remain explicitly unverified for M20.

## Delivered integration fixes

| Area | Actual implementation | Verified evidence |
|---|---|---|
| Local backup JSON | Required core sections, supported schema/app, duplicate shift identifiers, safe SharedPreferences section names/entry types before mutation | Old v1 fixture and malformed cases green |
| Local/Drive ZIP | Two-phase Notes media staging; active media not changed on incomplete archive or aborted main restore; shared multi-profile files preserved conservatively on success | ZIP roundtrip, corrupt ZIP, path traversal, stage/install/abort JVM tests |
| Restore persistence | Room transaction for template/day changes, one synchronous SharedPreferences editor per section and best-effort settings rollback on an ordinary exception; widget refresh after success | JVM structural/codec tests; Android Room runtime qualification separate |
| Google Drive appData | Paginated newest-first file discovery with loop guard, legacy JSON fallback, streaming temporary-file ZIP upload/download with cleanup | Four paging tests, codec and source-contract checks; live OAuth/network NOT tested |
| Excel import | Validate nonempty/unique/in-period preview; reject missing requested worksheet months; replace selected months plus templates/days inside one Room transaction | Pure preview tests + dedicated Android Room instrumentation scenario |
| Five home-screen widgets | Refresh all three calendar variants and both Notes widgets after source/profile changes, preserving PendingIntent actions | Manifest/source contract tests; Android receiver invocation separate; live launcher widgets NOT tested |
| Alarm/notifications | Review of shift-alarm scheduler/reboot receiver, Notes reminder and full-screen/foreground components; exact-alarm manifest declarations separated by SDK (SCHEDULE only through 32, USE from 33) | Static contract, existing alarm planning tests and build/lint; device permission/OEM acceptance NOT tested |
| Weather/platform | Manifest/JobScheduler review: background weather opt-in, persisted inexact jobs and saved-location-only constraints preserved | Existing weather rules + M16 contract tests; actual OEM scheduling/GPS NOT tested |
| Android OS backup | Require encryption capability for API 31+ OS cloud Auto Backup; retain D2D transfer semantics and legacy pre-31 backup setup | Android manifest/resources compile and lint; system restore behavior not tested |

## Whole-source qualification

- VPS full Gradle job `0f41c0aee016ecd5beb94e10d846356f`: **BUILD SUCCESSFUL in 8m 14s** (136 tasks: 48 executed, 88 up-to-date).
- Android JVM: **83 suites, 439/439 tests, 0 failures, 0 errors, 0 skipped**.
- Screenshot reference validation: **58/58**.
- Android debug app APK: SHA-256 `4f38df2f51563602ac744d763d36dfe15b4095230df2f3efde1e2d148ea5f6bc` (89 186 514 bytes).
- Wear debug APK: SHA-256 `6e833d46cfd5b6303ea87058690ca5a0f41d655deca58fb3c233aceb891da07a` (70 407 211 bytes).
- Both lint gates: **0 errors** (Android 77 warnings/12 hints; Wear 22 warnings/3 hints). Wear JVM test task remains `NO-SOURCE`, not misrepresented as tested.
- `:app:assembleDebugAndroidTest` GREEN; Android 36 emulator: **4/4 instrumentation tests GREEN** (room replacement, invalid preview preservation, five widget receiver dispatch, app context).

## Explicit residual risks and release acceptance (not silently passed)

1. **Multi-store crash atomicity:** the Room portion is transactional, and SharedPreferences have an ordinary-failure rollback, but an unexpected process death between persisted settings, DB and media promotion cannot be made universally atomic by Room. The restore procedure should be re-run using the same verified backup on recovery; release-device fault injection is still an M20 obligation. Local Notes media promotion now happens after the main restore.
2. **Google Drive OAuth/network:** physical sign-in scope, offline/expired tokens, 401/403 handling, malformed remote archive, live upload/download/restore and cloud quota/storage must be tested with a user account. No credentials were accessed here.
3. **Android permission policy:** `USE_EXACT_ALARM` app-store eligibility is a release/compliance requirement, not proven by compile/lint; `USE_FULL_SCREEN_INTENT` permission and notification channel presentation depend on Android version and device defaults.
4. **OEM lifecycle:** reboot/package replace/timezone, alarm exactness and lock-screen/full-screen ring, Notes reminders, notification runtime permission and weather background jobs need interactive execution on a supported phone.
5. **Installed widgets:** launcher widget placement/resizing, RemoteViews collections and battery optimizations require an Android widget host, not only receiver invocation.
6. **Older installed builds:** upgrade over existing app data, old Room DB and old manual/cloud backup are covered by fixtures but production upgrade qualification is reserved for M20. Wear data-layer compatibility is M17.
7. **Android Auto Backup:** Android's independent OS backup is not a substitute for application-owned full media ZIP/Google Drive backup; storage quotas and backup transport settings may exclude large archives.

M16 software implementation and local/emulator qualification are complete; untestable real-account/OEM acceptance is explicitly deferred to M20 rather than claimed. No release, tag, APK publication, deployment, master fast-forward or dependency modernization is performed in M16.

## Device execution matrix for M20

| Device scenario | Expected result |
|---|---|
| Android 12/13/14/16, notification access denied/granted | Alarm readiness explicit, Notes/weather notifications only where authorized |
| Exact-alarm permission change + reboot/timezone | Shift alarm and Notes reminders survive permitted rescheduling paths; no silent duplicate |
| Lock screen + full-screen allowed/denied | Reliable supported ring/fallback behavior, explicit system diagnostics |
| Google sign-in, stale auth, offline, cloud ZIP and legacy JSON | Known recoverable messages; no destruction on malformed backup; attachments restored |
| Excel corrupt/empty/partial months and valid multi-month workbook | Invalid input changes no schedule; valid imported months are replaced atomically |
| Five widget variants on launcher with active profile swap | Current profile content/size and actions refresh without stale information |
| Weather JobScheduler opt-in/default-off and network restrictions | No unexpected background GPS; background execution may be deferred, not treated as exact |
