# ShiftSalaryPlanner — Current State

Обновлено: **2026-09-11**

> Это живой документ текущей границы. Долговременный план и решения: [`VNEXT_MODERNIZATION.md`](./VNEXT_MODERNIZATION.md). Подробное M0-доказательство: [`recovery/M0_RECOVERY_EVIDENCE.md`](./recovery/M0_RECOVERY_EVIDENCE.md).

## Current phase

**M0 — Recovery & Canonical Baseline: COMPLETE.**

**M1 — Bridge Onboarding: COMPLETE.**

Следующая разрешённая работа: **M2 — Reproducible Build**.

Не начинать архитектурный refactor или redesign до завершения M2 и последующего M3 safety-net gate. Текущий recovered source baseline уже материализован в Git и является канонической отправной точкой.

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
- `app` and `wear` builds currently require a stable debug keystore at Gradle configuration time.
- Build signing needs CI/debug vs production separation before VPS development is considered reproducible.

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

## Next task: M2 — Reproducible Build

### Current environment evidence

A fresh build/test run was attempted only after the exact recovery commit had been created. The durable executor environment did not expose `HOME`, `java`, `ANDROID_HOME` or `ANDROID_SDK_ROOT`; a host-side search also found no installed JDK or Android SDK in the expected locations. This is an **M2 infrastructure blocker**, not a project test failure.

No claim is made yet that the recovered commit compiles or that its full unit suite passes freshly on the VPS. Historical archived Gradle test output remains forensic evidence only.

### M2 scope

1. install/pin a suitable JDK and Android SDK/toolchain on the VPS;
2. make clean-checkout Gradle invocation reproducible in Development Bridge jobs;
3. separate CI/debug signing from owner/local signing so builds do not require the archived keystore;
4. run fresh unit tests and record exact counts/results;
5. run the agreed lint/build gate (`assembleDebug` plus feasible lint/check tasks);
6. record any genuine source failures without changing recovered business semantics merely to force a green build;
7. update this document with the exact verified build/test baseline and M3 boundary.

### M2 stop condition

M2 is complete only when a clean checkout of canonical `master` can be built/tested on the VPS without the owner's computer or machine-local signing material.

## Work rules until state changes

- GitHub `master` at/after recovered baseline `304bf96...` is canonical source truth.
- Keep the owner recovery archive and transport branch as forensic evidence until a later explicit cleanup task; do not merge the transport branch.
- Do not decompile the whole APK; current source/APK correspondence evidence is already sufficient.
- Do not touch payroll semantics during recovery except to preserve the recovered implementation exactly.
- Do not upgrade dependencies during M0–M3 unless a specific dependency blocks reproducible build and the change is isolated.
- Do not merge/release/deploy without explicit owner permission/task authorization.
- After each completed milestone, update this file with actual branch/HEAD, evidence, completed gate and exact next boundary.
