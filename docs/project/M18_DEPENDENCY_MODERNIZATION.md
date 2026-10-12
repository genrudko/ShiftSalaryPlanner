# M18 — Dependency Modernization

Date: 2026-10-12
Status: implementation committed, full regression gate in progress

## Baseline and hard boundaries

- Base: locally verified, unmerged M17 `c9c7738` (includes M16 from `f401c85`); new isolated branch `work/m18-dependency-modernization`.
- Remote canonical `origin/master=c30f7fe` remains M15. No merge, push, release, version bump, signing change or production deployment authorized.
- Preserve Android applicationId, minSdk, targetSdk=36, release signing, Room schema and database migrations, JSON/ZIP backup compatibility, Wear Data Layer v1 protocol, exact alarm behavior and all payroll calculations.
- Build environment on VPS v10360: Android Gradle Plugin 9.2.1, Gradle wrapper 9.4.1 (SHA-256 distribution verification), Kotlin/Compose compiler 2.2.10, JDK 21 runtime. These are *intentionally pinned*: AGP 9.2 officially supports Gradle 9.4.1 and SDK API 37.
- Android compileSdk now API 37.0 for both modules; installed separately in the VPS Android SDK `/opt/project-workbench/android/sdk/platforms/android-37.0`. This is a compile-time API level only, not a targetSdk switch.
- Stable Compose BOM was independently verified in Google's Maven metadata: `2026.09.00` is the newest published stable 2026 BOM as of the audit. Official August 2026 Compose 1.12 notes require compileSdk 37 and AGP >= 9.1.2. No alpha/beta Compose BOM.

## M18 version pin changes

| Component | Before | Selected | Rationale |
|---|---|---|---|
| Compose BOM | 2024.09.00 | 2026.09.00 | Single coordinated stable Compose surface, both phone and Wear |
| AndroidX Core KTX | 1.17.0 | 1.19.1 | Stable |
| Lifecycle Runtime KTX | 2.10.0 | 2.11.0 | Stable |
| Activity Compose | 1.12.4 | 1.13.0 | Stable |
| Room | 2.8.4 | 2.8.5 | Same stable 2.8 family; compiler/runtime aligned |
| DataStore | 1.1.5 | 1.2.1 | Stable, all core/preference artifacts aligned |
| Wear Compose | 1.5.0 | 1.7.1 | Latest 2026-10-07 stable; material3/foundation/tooling aligned |
| Wear Tiles | 1.6.0 | 1.6.2 | Stable |
| Kotlin Serialization JSON | 1.6.3 | 1.9.0 | Latest chosen for Kotlin 2.2 compatibility: newer 1.10/1.11 are built with Kotlin 2.3 |
| Play Services Auth | 21.2.0 | 21.6.0 | Same stable API family; avoid 22.0 major OAuth migration |
| Google API Client Android | 2.8.1 | 2.9.1 | Current stable; no OAuth flow rearchitecture |
| Guava Android | 33.4.8-android | 33.7.2-android | Android variant, not JRE variant |
| SQLite JDBC (tests only) | 3.46.0.0 | 3.53.4.0 | Current stable; test driver only |

Intentionally pinned: AGP 9.2.1, Kotlin 2.2.10, Gradle 9.4.1, screenshot plugin alpha15 (instrumented screenshot workflow integration), Wear Play Services 20.0.1, Wear Watchface 1.3.0, POI 5.5.1, MapLibre Android 13.6.1, generated Drive v3 API, AndroidX test libraries already stable. Avoid silently bringing arbitrary prerelease variants into production.

## API cleanup

- Replace deprecated RTL-sensitive Compose Material icons with `Icons.AutoMirrored.Rounded.*` while retaining the same semantics.
- Replace deprecated Wear Compose `ButtonDefaults.CompactButtonContentPadding` with `CompactButtonDefaults.ContentPadding`.
- Do not automatically migrate Google Sign-In to Credential Manager/AuthorizationClient in this dependency-only milestone: it would change account consent, token behavior and existing Drive backup integration; requires separate Google OAuth flow acceptance.
- Do not replace status/navigation bar color APIs, Android location source or historical reflective icon scanner without isolated behavior/visual tests. The existing M15 Android color/system UI behavior, M16 backup and permissions, and M17 watch contract must remain protected.

## Qualification

First compilation of the upgraded versions **PASS** (VPS job `53680efb17022d7c3c10087f2e4d8373`, `BUILD SUCCESSFUL in 3m13s`; app+Wear Kotlin compilers and the resolved app runtime dependency graph). The source changes and deprecated icon/tooltip/Wear button cleanup were committed as `496e63a`. Final full regression job `896e29e677ea2e3a24588d030646fbaf` is **running**: phone JVM regression including payroll/backup/migrations, Compose screenshot validation (58 refs), phone assemble/lint, Android instrumentation APK, Wear JVM (6 tests) and assemble/lint. Must inspect terminal result, test XML counts, APK hashes, lint warnings/errors, and any failing visual references before qualification. No live device acceptance implied. Claim no production/device acceptance from JVM tasks.

Primary references:
- https://developer.android.com/build/releases/agp-9-2-0-release-notes
- https://developer.android.com/blog/posts/what-s-new-in-the-jetpack-compose-august-26-release
- https://developer.android.com/jetpack/androidx/versions
- https://github.com/Kotlin/kotlinx.serialization/blob/master/CHANGELOG.md
