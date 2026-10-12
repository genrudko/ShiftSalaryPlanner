# M18 — Dependency Modernization

Date: 2026-10-12
Status: **SOFTWARE COMPLETE / VERIFIED IN CANONICAL `master` / INTEGRATED ON GITHUB**, no production release

## Baseline and hard boundaries

- Base: locally verified, unmerged M17 `c9c7738` (includes M16 from `f401c85`); new isolated branch `work/m18-dependency-modernization`.
- Initial M18 branch was created when canonical `origin/master=c30f7fe` still represented M15. On 2026-10-12 the owner authorized guarded M16–M18 integration: fast-forwarded to `e71c011`, passed post-merge full gate, and pushed via non-force `master` update. Production release, version bump and signing change were not performed.
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

First compilation of the upgraded versions **PASS** (VPS job `53680efb17022d7c3c10087f2e4d8373`, `BUILD SUCCESSFUL in 3m13s`; app+Wear Kotlin compilers and the resolved app runtime dependency graph). The source changes and deprecated icon/tooltip/Wear button cleanup were committed as `496e63a`. The first combined Gradle job `896e29e677ea2e3a24588d030646fbaf` compiled successfully and generated Android JVM 439/439 plus Wear JVM 6/6 (zero failures/errors/skips), but failed its initial screenshot comparison (16/58 references rendered differently under new Compose) and stalled before app lint reporting. It was terminated after confirmed inactivity; NOT called a successful gate. Precisely 16 new Material3 render images (0.03–1.101% changed pixels, representative M13/M14/M15 layouts manually reviewed) were accepted as references in `bf3c945` without changing the other 42. Independent rerun `23943ad790454d34cdbb9100a4684823` **BUILD SUCCESSFUL in 2m02s**, all 58 screenshot cases pass. Separate app lint job `aeee2f9fce520a9951ce459abac67d18` **BUILD SUCCESSFUL in 29s**, Android lint 0 errors/79 warnings/12 hints, Wear 0 errors/24 warnings/3 hints. Final unified full gate `c7362dde0aa6dd7ac512b1ea51b8f96a` **COMPLETED / BUILD SUCCESSFUL in 17s** (139 tasks, 8 executed / 131 up-to-date); current M18 code+refs: Android 439/439 JVM (83 suites), Wear 6/6 JVM (1 suite), screenshots 58/58, both APKs and Android instrumentation APK, zero lint errors. Android lint 79 warnings/12 hints, Wear lint 24 warnings/3 hints. Independently executed actual Android 36 AVD `ssp_m16_api36` instrumentation job `c1985fc9bf52c6bc3f9ec91a0d84ca61`: installed both APKs, `OK (4 tests)` (Room transactional import, invalid preview preservation, all five widget broadcasts and app context), `INSTRUMENTATION_CODE: -1`. No real Wear pairing/OEM/Google OAuth/Drive testing implied. Final debug APK SHA256: app `a0862431161653f32afd9d46ea59d08637848ec3ee911c810a976242860eba83`; Wear `4286bb75ab57b4df29628868664614954fb8b522453de4867e5bfe0e5d568bf0`. Android 36 AVD was shut down after the test. Owner explicitly authorized integrating M16–M18; remote integration verification documented separately.

Primary references:
- https://developer.android.com/build/releases/agp-9-2-0-release-notes
- https://developer.android.com/blog/posts/what-s-new-in-the-jetpack-compose-august-26-release
- https://developer.android.com/jetpack/androidx/versions
- https://github.com/Kotlin/kotlinx.serialization/blob/master/CHANGELOG.md

## Verified post-integration gate

- Canonical source `master=e71c011` (same tree as M18 isolated source after final documentation); VPS merged-result Gradle job `13bde19b44636024fcf36bdf0106d873` **BUILD SUCCESSFUL in 9m03s**: 139 tasks, 100 executed /39 up-to-date. Android 439/439 (83 suites); Wear 6/6 (1 suite); 58/58 screenshots; app/Wear APK plus instrumentation APK; app lint 0 errors/79 warnings/12 hints, Wear lint 0 errors/24 warnings/3 hints.
- Canonical merged APK SHA256 phone `1213281c68acc867a76d28cb473ce7cf805bfcb01ad17d63531d6cc8a820ea48`; Wear `6a6a2d3d6d0f9da79959a43925846ecf0d91851111119b5b8da5430df6e8a049`. Debug APK hashes differ between the isolated and merged worktree builds; the precise cause was not independently isolated. Matching Git source tree IDs, passing independent test suites and both successful compilations are separate evidence, not proof of bit-identical APK output.
- GitHub fast-forward push `c30f7fe..e71c011` verified by fresh `git ls-remote origin refs/heads/master` == `e71c011e430770e9b1ccaa7dd5485da350cf4bb2`. Final docs-only reconciliation commit follows this proven code tip.
