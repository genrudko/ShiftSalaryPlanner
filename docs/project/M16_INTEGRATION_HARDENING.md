# M16 — Integration Hardening

Canonical entry: `origin/master` / `c30f7fe4512a605c63a0a7b9cbb23e9281a7a1b6` on 2026-10-12.
Implementation worktree: `work/m16-integration-hardening`. M16 is **IN PROGRESS**, not a release gate.

## Scope and boundaries

- Validate local backup and Google Drive appData restore, including legacy JSON and Notes media ZIP, before accepting destructive state updates.
- Qualify Excel import, PDF/export flows, Android widget providers and background updates.
- Qualify Android notification channels, permissions, exact/full-screen alarms, boot/package/time-change receivers, foreground playback and persisted weather jobs.
- Preserve Room schema, old supported backup formats, payroll calculations, Notes content, shift/template identity and existing external entry points. Wear DTO work is reserved for M17, dependency upgrades for M18, device upgrade/release signing for M20.

## Bounded sequence

| Slice | Verification goal | Status |
|---|---|---|
| M16.1 Backup input integrity | Reject incomplete/foreign/unsupported backup before DB/prefs mutation and Notes media replacement; preserve supported v1/ZIP fixtures | Code `d048a14`; focused 10/10 GREEN; full Android/Wear gate GREEN (425/425 JVM, 58 screenshots, both APKs and zero lint errors) |
| M16.2 Restore failure isolation / Drive | Fix Drive appData paging (first-page truncation); then review partial-write safety, duplicate files, cloud service failures and archive size limits | Paging implemented/tested; remaining items OPEN |
| M16.3 Import/export | Corrupt/empty/successful Excel import, export provider/FileProvider contracts, non-destructive cancellation | NOT STARTED |
| M16.4 Widgets | Five providers, all installed sizes, notes/shift action PendingIntents, profile-switch/date/time updates and system rebind | NOT STARTED |
| M16.5 Alarms/notifications | Alarm scheduling/permission/reboot/full-screen, Notes reminders, notification channels and foreground service behavior | NOT STARTED |
| M16.6 Weather/platform | JobScheduler opt-in/persisted intervals/roaming/network restrictions and system calendar/location permissions | NOT STARTED |
| M16.7 Cross-feature qualification | Full Android/Wear JVM/build/lint/screenshots; documented device-only matrix and unresolved risks | NOT STARTED |

## Evidence from source inspection (not device acceptance)

1. `parseAppBackupJson` formerly mapped missing core sections to empty collections; `restoreBackupFromRawJson` subsequently deleted absent local shift rows and templates. M16.1 fails closed instead. ZIP Notes media formerly replaced live files before app-level JSON validation; M16.1 adds a validation hook before directory replacement for both local and Google Drive imports.
2. Google Drive `findBackupFile` requests only the first 20 files (`setPageSize(20)`) and does not follow `nextPageToken`. A matching ZIP/JSON older than that page cannot be discovered. Fixed via paginated scan in M16.2; four focused tests GREEN. Network integration remains device-only.
3. Google Drive download unpacks Notes media *before* database/preferences restore. Even after input validation, an I/O or storage failure later in the restore can leave parts applied. A truly atomic multi-store restore is **not proven**. **OPEN**.
4. `backup_rules.xml` and `data_extraction_rules.xml` are still template/sample configurations rather than an explicitly reviewed OS Auto Backup policy. **OPEN**.
5. Android `ShiftAlarmSystemReceiver`, `WeatherRefreshJobService`, Notes reminder notification and five widget receiver registrations exist, but no physical reboot, permission or OEM background measurements are recorded for the new integrated source. **DEVICE ACCEPTANCE OPEN**.

## Required result

A code review, deterministic tests and build/lint gate must pass for each changed slice. Device-only requirements are stated as unverified until measured on representative Android versions. No production merge, signed build, tag or deployment follows automatically from M16 completion.
