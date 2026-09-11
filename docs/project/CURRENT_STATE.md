# ShiftSalaryPlanner — Current State

Обновлено: **2026-09-11**

> Это живой документ текущей границы. Долговременный план и решения: [`VNEXT_MODERNIZATION.md`](./VNEXT_MODERNIZATION.md). Подробное M0-доказательство: [`recovery/M0_RECOVERY_EVIDENCE.md`](./recovery/M0_RECOVERY_EVIDENCE.md).

## Current phase

**M0 — Recovery & Canonical Baseline: COMPLETE.**

**M1 — Bridge Onboarding: COMPLETE.**

**M2 — Reproducible Build: COMPLETE.**

M2 включён в канонический `master` fast-forward merge после явного разрешения владельца. Следующая разрешённая фаза — **M3 — Behavioral Safety Net**. Архитектурный refactor и redesign до завершения M3 не начинать. Release/deploy остаются отдельными owner-gate.

## Repository state

- Repository: `genrudko/ShiftSalaryPlanner`
- Default branch: `master`
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

## Work rules until state changes

- GitHub `master` at/after recovered baseline `304bf96...` is canonical source truth.
- Keep the owner recovery archive and transport branch as forensic evidence until a later explicit cleanup task; do not merge the transport branch.
- Do not decompile the whole APK; current source/APK correspondence evidence is already sufficient.
- Do not touch payroll semantics during recovery except to preserve the recovered implementation exactly.
- Do not upgrade dependencies during M0–M3 unless a specific dependency blocks reproducible build and the change is isolated.
- Do not merge/release/deploy without explicit owner permission/task authorization.
- After each completed milestone, update this file with actual branch/HEAD, evidence, completed gate and exact next boundary.
