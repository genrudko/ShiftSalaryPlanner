# ShiftSalaryPlanner — Current State

Обновлено: **2026-09-11**

> Это живой документ текущей границы. Долговременный план и решения: [`VNEXT_MODERNIZATION.md`](./VNEXT_MODERNIZATION.md). Подробное M0-доказательство: [`recovery/M0_RECOVERY_EVIDENCE.md`](./recovery/M0_RECOVERY_EVIDENCE.md).

## Current phase

**M0 — Recovery & Canonical Baseline: COMPLETE.**

**M1 — Bridge Onboarding: COMPLETE.**

**M2 — Reproducible Build: COMPLETE.**

**M3 — Behavioral Safety Net: COMPLETE ON CANONICAL `master`.**

**M4 — App Shell Extraction: COMPLETE ON CANONICAL `master`.**

**M5 — Navigation Rewrite: COMPLETE ON CANONICAL `master`.**

M5 заменил root-навигацию из строк и 22 отдельных fullscreen boolean-флагов на один типизированный `AppNavigationState` с вкладкой, Finance sub-tab и предсказуемым fullscreen stack. Внешний вид и существующие восемь вкладок не перестраивались; 11 modal/feature-флагов намеренно оставлены для M6. Первый независимый review нашёл один реальный сценарий после recreation, где Quick Start мог нарушить соответствие видимого экрана и вершины back-stack; он исправлен через RED→GREEN regression tests. Повторный review не нашёл actionable regressions. После явного разрешения владельца M5 fast-forward отправлен в canonical `master` без merge-коммита и без force. Следующая активная фаза — **M6 — State & Feature Boundaries**. Release/deploy остаются отдельными owner-gate.

**M7 — UI/Data Boundary: IN PROGRESS on `refactor/m7-ui-data-boundary`.**

M7 starts from canonical `master` `d009796cb6ff179d46327f2228620ae239d3b8f9` with the approved dependency-direction contract: narrow feature-facing ports only where concrete DAO/store/service details leak into presentation orchestration; existing persistence/service implementations stay in place. **Tasks 1–6 are now complete locally.** Schedule, Alarm, Finance, Notes/Settings/Activity Log, Profile, and Service Operations boundaries are all wired through named feature-facing ports without changing Room schema, payroll formulas, backup format, alarm semantics, or cloud behavior. Task 6 implementation is `3a99dfe31d1122ad1e867cab8c6bd9d909681790`: `ShiftSalaryApp` no longer aliases `HolidaySyncRepository`, `ExcelScheduleParser`, `ExcelScheduleImporter`, `GoogleDriveSyncStore`, Google sign-in client/scope, direct Drive backup helpers, or raw backup restore schedule callbacks. Existing backup helper/parser files and fixtures are unchanged. Targeted service/backup/holiday tests and full JVM are GREEN; current full JVM evidence is **108/108**, zero failures/errors/skips across 34 suites. **Task 7 final qualification is active.** Structural Steps 1–2 are GREEN after repair `1b6098c03e78fe4d933537d85bf1baad3ada060a`: presentation has zero M7 concrete DAO/store/service type references or constructors, and no generic repository/service-locator/DI facade was introduced. Next gate is fresh `clean :app:testDebugUnitTest`, followed on the unchanged tree by phone/Wear assemble+lint and independent whole-M7 review.


**M6 — Feature State Ownership: COMPLETE on canonical `master`.**

The M6 implementation slices now have focused owners for Calendar interaction, pattern/clear-range workflow, Notes, Finance/payments/report workflow, Shift/template editing, Settings, service/backup/import workflow, widget runtime and alarm runtime state. The root remembered mutable owner count has reached the planned **3**: `currentMonth`, `activeWorkplaceId`, and `navigationState` (baseline before M6: 67; after verified M6A: 44).

Current implementation evidence: all bounded M6 state-holder slices are integrated on canonical `master`. Final Task 8 qualification passed fresh `clean :app:testDebugUnitTest` with **94/94**, zero failures/errors/skips; phone/Wear `assembleDebug + lintDebug` passed with **zero lint errors**; structural ownership is exactly `currentMonth`, `activeWorkplaceId`, and `navigationState`; independent Antigravity `gemini-3.8-flash-medium` review returned `NO ACTIONABLE CRITICAL/IMPORTANT/P2 FINDINGS`. Owner-authorized rebase/reconciliation preserved byte-identical production/config trees and a clean merge-tree. Final integration used a **non-force fast-forward only**: feature ref and canonical `master` reached integration SHA `121763aa50f4de3d125d446b9d86832a31109c99`. Fresh clean JVM gates were run both immediately before integration and again on the merged canonical master, each **94/94** with zero failures/errors/skips. **M6 is COMPLETE.** Next active milestone: **M7 — UI/Data Boundary**. Release/deploy remains a separate owner gate.

## Repository state

- Repository: `genrudko/ShiftSalaryPlanner`
- Default branch: `master`
- M6 integration boundary on canonical `master` = `origin/master`: `121763aa50f4de3d125d446b9d86832a31109c99` via owner-authorized non-force fast-forward; this final docs-only closeout commit follows on master.
- Canonical recovered source commit: `304bf96cec26c4e7c5fe94a03f2579ceeef5996b`
- Recovery commit subject: `recovery: materialize last working source baseline`
- Recovery commit is pushed to `origin/master`; verified local/remote state: `ahead=0`, `behind=0`, clean tree.
- Last pre-recovery code commit: `3ece60f6ee694ef1baf251e7eec5beb29f146e45`
- Pre-recovery commit date: 2026-06-04
- Pre-recovery commit subject: `Use workplace payroll settings for all-work filter`
- Recovered build metadata: `versionCode 201`, `versionName 7.1`.
- Development Bridge project/repository: `shift-salary-planner / shift-salary-planner`.
- Writable VPS workspace: `/home/eodadmin/codex-workspace/ShiftSalaryPlanner`.

## M0 recovery result

The owner provided the last full Android Studio project folder as `ShiftSalaryPlanner.zip`.

Archive fingerprint:

```text
SHA-256 426a7720d37a092d819860d9613408d6d44a27f96f75ae87e2c816a1c5983e19
```

The archive contains `.git` metadata and reports:

```text
branch: master
HEAD: 3ece60f6ee694ef1baf251e7eec5beb29f146e45
staged changes: 0
```

Raw Windows working-tree status showed 155 modified tracked files plus one untracked test. After excluding CRLF-only noise with an EOL-insensitive comparison, the substantive recovery delta is:

- **24 tracked files with semantic changes**;
- **1 untracked test**;
- **1434 insertions / 182 deletions**.

A normalized semantic patch was generated and successfully applied to a clean checkout of `3ece60f6...`; all 25 recovered files then matched the archive after CRLF→LF normalization.

Normalized patch verification SHA-256:

```text
4300e29d4fcc6a8d94cdb0508895da45780795bdf90efe7447fd13aaed873b5e
```

### Durable recovery staging

The normalized recovery patch is now durably stored in GitHub on the isolated branch:

```text
branch: recovery/m0-materialize
branch head: 263c69d5abfc6c511552b774a4f483a2c82f06f1
storage: docs/project/recovery/.m0/chunk-00 ... chunk-03
```

Those four files contain the gzip-compressed patch encoded as base64 chunks. Reconstructing them in lexical order, then `base64 -d | gzip -d`, must yield the patch SHA-256 above before it is applied.

**Do not merge `recovery/m0-materialize` into `master`.** It is a transport/recovery branch only. The final recovery commit must contain the recovered source tree, not the transport chunks.

The transport branch was intentionally not merged. Development Bridge was later made available and the patch was reconstructed directly from its four GitHub chunks on the VPS. The reconstructed patch SHA-256 matched the value above, `git apply --check` succeeded, and the patch was applied to the canonical workspace.

Before commit, all 25 recovered semantic paths were independently checked with Git blob hashes against the recovery manifest and reported `ALL_25_BLOBS_MATCH`. The resulting recovery commit is `304bf96cec26c4e7c5fe94a03f2579ceeef5996b` and is now pushed to `origin/master`. **M0 is complete.**

## APK correspondence

Owner-provided APK:

```text
SHA-256 665888705c888c73ebc809e8a75cf46f586f115af7789a0412adfc700f572ba5
versionCode 201
versionName 7.1
embedded Git revision 3ece60f6ee694ef1baf251e7eec5beb29f146e45
```

The recovered local `app/build.gradle.kts` also contains `201 / 7.1`.

Compiled DEX in the APK contains identifiers/strings introduced by the recovered local source delta, including:

```text
overrideStartTime
legislationProfile
overtimePercentOfHourly
clearSuppressedAlarmsForRange
Индивидуальная правка смены
За смену/день
```

Therefore the recovered dirty working tree is strongly tied to the same post-`3ece60f6` development generation as the provided working APK. Exact binary reproducibility has not yet been proven and is not required to establish that GitHub `3ece60f6` alone is incomplete.

## Important recovered functionality

The missing local delta includes behavior that must be preserved before vNext work:

- Room DB version **6** and migration 5→6 for individual-day shift overrides;
- individual shift-day overrides for time/hours/pay/note;
- payroll legislation profile and configurable overtime payment modes;
- per-shift additional payments;
- fixes for resolved additional-payment double multiplication;
- alarm suppression clearing/rescheduling helpers;
- backup compatibility for new day-override fields;
- expanded payroll/holiday tests and new `PaymentEnhancementsTest`;
- related calendar/payroll/settings UI changes.

This is not cosmetic drift. It changes persistence and payroll behavior.

## Security / local-only recovery material

The archive includes machine-local build configuration (`local.properties`) and a stable debug keystore. Treat both as local recovery/build artifacts only:

- do not commit them;
- do not paste their secrets into issues/docs;
- do not use them as production release signing material by default;
- M2 will explicitly separate CI/debug signing from production signing.

## Known architectural findings

- `MainActivity` / root `ShiftSalaryApp` owns excessive Compose/app state and orchestration.
- Root navigation contains many boolean screen/dialog flags and manual back handling.
- Several UI files are very large; feature isolation is incomplete.
- Payroll/domain code already has useful decomposition and must not be rewritten casually.
- Test coverage exists but is too small for the product surface, especially as protection for a large refactor.
- M2 branch removes the mandatory owner stable-debug keystore from debug configuration; both modules can use the normal VPS debug identity.
- Release signing is separately opt-in and remains unqualified for production until the later release-qualification phase.

## Product findings

The current application is functionally mature. Core value to preserve includes:

- readable shift calendar;
- multiple workplaces/profiles;
- payroll and payslip calculations;
- alarms tied to shifts;
- Today screen;
- shift templates/statuses;
- notes/media;
- backup/restore and Drive sync;
- widgets;
- Wear OS companion;
- AI assistant;
- advanced appearance/settings.

UX issue is primarily information hierarchy/navigation density, not lack of capability. Redesign is allowed to change information architecture after architecture stabilization.

## Completed gates

### M0 — Recovery & Canonical Baseline

Completed evidence:

1. source archive and APK fingerprints recorded;
2. pre-recovery base identified as `3ece60f6...`;
3. semantic delta isolated to exactly 24 modified tracked files plus one new test;
4. CRLF-only churn excluded;
5. recovery transport patch reconstructed from GitHub and verified at SHA-256 `4300e29d4fcc6a8d94cdb0508895da45780795bdf90efe7447fd13aaed873b5e`;
6. `git apply --check` succeeded;
7. applied tree matched all 25 expected Git blob hashes;
8. dedicated recovery commit created and pushed: `304bf96cec26c4e7c5fe94a03f2579ceeef5996b`;
9. remote `master` verified clean and synchronized (`ahead=0`, `behind=0`).

### M1 — Bridge Onboarding

Completed evidence:

- Development Bridge project: `shift-salary-planner`;
- repository id: `shift-salary-planner`;
- workspace: `/home/eodadmin/codex-workspace/ShiftSalaryPlanner`;
- capabilities verified after Bridge restart: read/write/git-read/git-write/execute;
- exact guarded `git_push_plan → git_push` successfully pushed the M0 recovery commit.

## M2 — Reproducible Build verification

Status: **COMPLETE ON CANONICAL `master`**.

Implementation branch/worktree:

```text
branch: infra/m2-reproducible-build
Development Bridge repository: shift-salary-planner-m2
worktree: /home/eodadmin/.local/state/development-bridge/worktrees/shift-salary-planner-m2
base includes: 4ad9f68d458f901164dda240366974d55abaf7ea
```

The branch now provides a pinned user-space JDK/Android SDK bootstrap, a Bridge-safe environment contract, Linux-executable Gradle wrapper, optional stable-debug signing, separately opt-in release signing, and bounded API/lint compatibility repairs needed for the existing target/API surface. No payroll, Room schema, navigation or dependency-upgrade work is included.

Fresh qualification on 2026-09-11, with no `local.properties` and no owner stable-debug keystore present:

```text
JDK: Temurin 21.0.12.1+1
Gradle: 9.4.1
full gate: clean + app unit tests + app/wear assembleDebug + app/wear lintDebug + app/wear signingReport
Gradle result: BUILD SUCCESSFUL in 14m 12s
Gradle tasks: 103 executed
unit tests: 26 tests, 0 failures, 0 errors, 0 skipped (7 suites)
app lint: 0 errors, 58 warnings
wear lint: 0 errors, 22 warnings
app debug APK: 38,782,813 bytes
app debug APK SHA-256: e75fa6a67e0b21b38833e4ec4224554cb6ee80ab94c04a07a8f396dbc7d031c1
wear debug APK: 70,439,979 bytes
wear debug APK SHA-256: 0747d838f4ff3eaa73d32f90b9592d891be1aaaef18f5ac7766ada5172ee711a
debug SHA-1 (phone + Wear): 3E:42:3B:21:14:15:AF:5C:B1:EE:97:FD:F4:EC:EA:FA:CF:A1:BF:C4
debug SHA-256 (phone + Wear): D6:64:F8:E8:A1:D6:E7:F9:4C:22:01:AA:1D:BD:73:1D:F5:22:60:3A:45:3A:4C:9A:62:5C:CD:91:04:35:0A:3D
release signing: Config null for both modules
```

A fresh pre-merge rerun on the same code tree also completed Gradle successfully in **12m 59s** (`103 actionable tasks: 102 executed, 1 up-to-date`), with the same 26/26 unit-test result, the same APK SHA-256 values, matching phone/Wear debug certificates, and lint reports of `0 errors, 58 warnings, 12 hints` for app and `0 errors, 22 warnings, 3 hints` for Wear. The wrapper job nonzero status came only from an over-strict post-build grep; a separate post-check confirmed both lint summaries and clean `git diff --check`.

The durable gate process itself returned exit code 2 only after all Gradle work and evidence collection had succeeded, because `git diff --check` detected one extra blank line at EOF in each module build script. Those two whitespace-only EOF defects were then normalized; no executable/build semantics changed.

A Gradle 9.4.1 / AGP 9.2.1 configuration-cache reload defect was also reproduced specifically for AGP `SigningReportTask`. M2 marks only `signingReport` as configuration-cache-incompatible. Ordinary `signingReport` was then run twice consecutively and succeeded both times; other tasks retain configuration-cache support.

Independent Codex review initially found a valid API 27–28 NumberPicker readability regression in the first lint repair. The repair was corrected to keep the pre-Q compatibility path while avoiding blocked private API access on Android 16. A second independent review of the resulting M2 diff returned no findings.

Detailed environment/operator contract: [`M2_BUILD_ENVIRONMENT.md`](./M2_BUILD_ENVIRONMENT.md).

### M2 completion and M3 boundary

The verified implementation commit is `7bd0a29206f65b1b48656f064bd4625fb5e94534`; branch closeout commit is `8257437fbb3f12dda7095bee6b23ac7ee647c28b`. The owner explicitly authorized finishing the milestone, and `master` was advanced by `git merge --ff-only infra/m2-reproducible-build` before this final canonical-state documentation commit.

M2 is complete. The exact next bounded phase is **M3 — Behavioral Safety Net**. Do not begin architecture refactor or redesign until M3 is completed. Do not release or deploy without separate owner authorization.

## M3 — Behavioral Safety Net verification

Status: **COMPLETE ON CANONICAL `master`.**

Branch/worktree:

```text
branch: test/m3-behavioral-safety-net
worktree: /home/eodadmin/.local/state/development-bridge/worktrees/shift-salary-planner-m3
base master: 01a9bf47d96eeeb3babb2e8c98057a5871759f5c
verified code/docs head: e2fc424e23a419808665cae4a9561cab2180bb2e
```

M3 added regression protection without redesigning the product or changing Room/backup schema versions:

- payroll characterization for monthly salary, per-shift pay, vacation/sick coexistence and the existing multi-workplace payroll-settings selection policy;
- real in-memory SQLite migration fixtures for Room 4→5 and 5→6;
- backup schema-v1 fixtures for recovered override fields and legacy backups;
- a deterministic alarm-planning seam with fixed-clock tests while leaving Android PendingIntent/delivery/permission behavior untouched;
- a device qualification contract at [`M3_DEVICE_QUALIFICATION.md`](./M3_DEVICE_QUALIFICATION.md) for checks that cannot be proven on the VPS JVM.

Fresh final verification was split into two terminal-success jobs on the same unchanged `e2fc424...` tree after an earlier all-in-one wrapper hit its 40-minute timeout. The timeout was not a source/test failure; the split gates provide the final evidence:

```text
unit-test gate: BUILD SUCCESSFUL in 4m 23s
unit tests: 42 tests, 0 failures, 0 errors, 0 skipped (12 suites)

build/lint gate: BUILD SUCCESSFUL in 10m 52s
app lint: 0 errors, 58 warnings, 12 hints
wear lint: 0 errors, 22 warnings, 3 hints
app debug APK: 38,782,813 bytes
app debug APK SHA-256: 4d8f50f4b29ba3883de7aca43229c16330fb2d33c480267565601365235cb027
wear debug APK: 70,439,979 bytes
wear debug APK SHA-256: 0747d838f4ff3eaa73d32f90b9592d891be1aaaef18f5ac7766ada5172ee711a
git diff --check: clean
```

Independent Codex review of the whole M3 diff against `master` returned: **no actionable regressions identified**. The reviewer classified the production changes as narrowly scoped testability extractions with device-only limitations explicitly documented.

Device-only checks are intentionally **NOT EXECUTED** in M3 automated qualification: in-place database upgrade on a physical device, user-facing backup export→restore, reboot reschedule, exact-alarm/notification/full-screen permissions, locked/doze delivery+snooze, and Wear mirror smoke test. They remain an explicit later physical-device qualification obligation, not an implied automated pass.

M3 branch was pushed to `origin/test/m3-behavioral-safety-net` after verification and closed at `d272abb2c400e61806ea7b0f8f6a3f81441a4650`. After explicit owner authorization, canonical `master` was advanced to that exact commit with `git merge --ff-only`; no merge commit or code rewrite was introduced. This docs-only closeout records that completed boundary. The next phase is **M4 — App Shell Extraction**; refactor implementation begins only after its architecture/design is explicitly approved.

## M4 — App Shell Extraction verification

Status: **COMPLETE ON CANONICAL `master`.**

Branch/worktree and verified code boundary:

```text
branch: refactor/m4-app-shell-extraction
worktree: /home/eodadmin/.local/state/development-bridge/worktrees/shift-salary-planner-m4
base master: 7c0a3e8d78ae428ba4b8b3fd0c026a5671a0268b
verified production code head: 3ce3abe24d46c00087bce86b5d8b3a17f0ab1b1b
qualification/review docs checkpoint: 2bee342d221e95acb71e93ee6cf15964c37db429
```

M4 is deliberately behavior-preserving. It added `AppDependencies` and `ProfileDependencies`, introduced `ShiftSalaryPlannerRoot`, moved app/profile dependency construction out of `ShiftSalaryApp`, and reduced `MainActivity.onCreate` to Android entry/intents plus one root-composable call. Boolean navigation and feature state remain for M5/M6. No Room/backup/payroll schema or semantics changed.

Fresh qualification on the unchanged production code tree:

```text
clean JVM gate: BUILD SUCCESSFUL in 4m 03s
unit tests: 42 tests, 0 failures, 0 errors, 0 skipped (12 suites)

app/wear build+lint gate: BUILD SUCCESSFUL in 11m 34s
app lint: 0 errors, 58 warnings, 12 hints
wear lint: 0 errors, 22 warnings, 3 hints
app debug APK: 38,782,813 bytes
app debug APK SHA-256: 772da544a72e98b192d547fa9ae942c9940c23d3ec79ce78034d7f8828baa31f
wear debug APK: 70,439,979 bytes
wear debug APK SHA-256: 0747d838f4ff3eaa73d32f90b9592d891be1aaaef18f5ac7766ada5172ee711a
git diff --check: clean
```

Independent Codex review of the whole M4 branch against its `master` base returned **no actionable regressions** and explicitly found profile-switch behavior preserved. The reviewer was read-only and used the already-produced 42-test evidence; tests/build/lint were run independently by the M4 qualification jobs above.

The verified M4 branch was pushed through `98c72e901601836695c20faeea38d679885fbb9b` and, after explicit owner authorization, canonical `master` was advanced by fast-forward to that exact commit. M4 is complete. The next active phase is **M5 — Navigation Rewrite**: inventory the existing routes/actions first, then replace root boolean screen flags with a typed destination/back-stack model while preserving the current visible navigation structure 1:1. M6 state extraction, M7 domain/data hardening, redesign, release and deploy remain out of scope.

M4 branch was first pushed and remotely verified at `00c03e8e04d4fe5fa24846720c2a9afc89d8d1b3`; after owner authorization the completed M4 history was fast-forwarded into canonical `master`. Later docs/inventory commits advanced canonical `master` to the M5 base `456a6ec6a390f064bd4d1069b33b4edcc4ac51cd`. Release/deploy were not part of M4.

## M5 — Navigation Rewrite verification

Status: **COMPLETE ON CANONICAL `master`.**

Branch/worktree and verified boundary:

```text
branch: refactor/m5-typed-navigation
worktree: /home/eodadmin/.local/state/development-bridge/worktrees/shift-salary-planner-m5
base master: 456a6ec6a390f064bd4d1069b33b4edcc4ac51cd
verified production fix head: a018e6e5cf50ae5d24154bba9b0b5e0bf8ff67a5
pre-closeout branch head: 660699e517c049d72d85afdaced965993567cf59
```

M5 preserved the current eight-tab user interface while centralizing root navigation. `PAYROLL`/`PAYMENTS` widget aliases and direct tab entry remain supported, Finance sub-tab selection is typed, all 22 standalone fullscreen destinations now use one saveable typed stack, and the nested Deductions → editor → Back behavior is covered explicitly. The old root `selectedTabName` / `financeSubTabName` strings and 22 root fullscreen boolean flags are gone. Exactly 11 modal/feature-state flags remain intentionally for M6 rather than being mixed into root navigation.

Fresh post-review verification on the corrected production tree:

```text
clean JVM gate: BUILD SUCCESSFUL in 3m 52s
unit tests: 54 tests, 0 failures, 0 errors, 0 skipped
app/wear build+lint gate: BUILD SUCCESSFUL
app lint: 0 errors, 58 warnings, 12 hints
wear lint: 0 errors, 22 warnings, 3 hints
app debug APK: 38,766,429 bytes
app debug APK SHA-256: f8f0b27e5bbdcf79a1140bf3c8d5d054c2e7aa65f6afdba1cf12354860746750
wear debug APK: 70,439,979 bytes
wear debug APK SHA-256: 0747d838f4ff3eaa73d32f90b9592d891be1aaaef18f5ac7766ada5172ee711a
legacy root fullscreen flags: 0 / 22 remain
modal/feature flags intentionally preserved: 11 / 11
git diff --check: clean
```

The first independent Codex review found one valid P2 recreation/back-stack regression: Quick Start could auto-open after a restored fullscreen destination and make system Back disagree with the visible foreground screen. Two regression tests were added RED-first; `applyQuickStartNavigation` now opens Quick Start only from an idle root stack. The affected clean test gate then passed 54/54. A second independent Codex review of the full M5 branch against canonical `master` reported **no actionable correctness regressions**.

M5 was technically verified before merge. The feature branch was guarded-pushed and remotely verified at `6bab1a28ef4b9e8037733e5d47c3a17cf89e45bb`; later ledger/closeout commits remained in the same linear M5 history. After explicit owner authorization, Bridge produced a non-force fast-forward plan from canonical `master` `456a6ec6a390f064bd4d1069b33b4edcc4ac51cd` to `12916ce98601affbda318ec6cb0c9658b431b219`, and the guarded push succeeded. No merge commit or history rewrite was introduced. M5 is therefore complete on canonical `master`. The next active phase is **M6 — State & Feature Boundaries**; M7 domain/data hardening, redesign, release and deploy remain outside the completed M5 scope.

## Work rules until state changes

- GitHub `master` at/after recovered baseline `304bf96...` is canonical source truth.
- Keep the owner recovery archive and transport branch as forensic evidence until a later explicit cleanup task; do not merge the transport branch.
- Do not decompile the whole APK; current source/APK correspondence evidence is already sufficient.
- Do not touch payroll semantics during recovery except to preserve the recovered implementation exactly.
- Do not upgrade dependencies during M0–M3 unless a specific dependency blocks reproducible build and the change is isolated.
- Do not merge/release/deploy without explicit owner permission/task authorization.
- After each completed milestone, update this file with actual branch/HEAD, evidence, completed gate and exact next boundary.
