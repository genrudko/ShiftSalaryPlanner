# ShiftSalaryPlanner — Current State

Обновлено: **2026-09-11**

> Это живой документ текущей границы. Долговременный план и решения: [`VNEXT_MODERNIZATION.md`](./VNEXT_MODERNIZATION.md). Подробное M0-доказательство: [`recovery/M0_RECOVERY_EVIDENCE.md`](./recovery/M0_RECOVERY_EVIDENCE.md).

## Current phase

**M0 — Recovery & Canonical Baseline: evidence complete, recovered source snapshot proven, materialization pending.**

Следующая разрешённая работа: **M0.1 — Materialize Recovered Baseline**.

Не начинать M1 Bridge onboarding, refactor или redesign до того, как recovered snapshot будет внесён в Git отдельным recovery-коммитом и зафиксирован здесь новым SHA.

## Repository state

- Repository: `genrudko/ShiftSalaryPlanner`
- Default branch: `master`
- Last pre-recovery code commit: `3ece60f6ee694ef1baf251e7eec5beb29f146e45`
- Commit date: 2026-06-04
- Commit subject: `Use workplace payroll settings for all-work filter`
- Git build metadata at that commit: `versionCode 194`, `versionName 6.4`
- Canonical planning/docs commits now sit on top of that code state.
- Repository is not yet onboarded as its own Development Bridge managed repo.

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
ede38d105ca3cc4b21f4af9138ef7b3896c072a976b48e8595ba9deac3d1d9dc
```

The patch is verification evidence only and is not currently stored in GitHub. The archive remains the recovery source until M0.1 imports the exact semantic delta.

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

## Next task: M0.1 — Materialize Recovered Baseline

### Scope

Start from current GitHub `master` and import exactly the semantic delta documented in `recovery/M0_RECOVERY_EVIDENCE.md` from the owner recovery archive.

### Required behavior

- preserve the 24 tracked semantic changes and the one untracked test;
- ignore CRLF-only modifications;
- exclude `.idea`, local build output, `local.properties`, keystores and other machine-local artifacts;
- do not alter payroll semantics beyond what already exists in the recovered snapshot;
- do not refactor recovered code during import;
- do not upgrade dependencies during import;
- do not redesign UI during import.

### Verification

Before committing:

1. recovered semantic file inventory matches the evidence document;
2. resulting source diff contains no mass line-ending churn;
3. `app/build.gradle.kts` contains the recovered `201 / 7.1` metadata unless a later explicit release-version task changes it;
4. Room version/migration and recovered payroll behavior are present;
5. recovered tests are present;
6. no local signing secret/config is committed;
7. run feasible tests/build checks in the available environment and record any environmental blocker rather than silently changing code to satisfy it.

### M0 stop condition

M0 is complete only when the recovered source snapshot is represented by a clean Git commit and that commit SHA is recorded here.

After that, the next phase is **M1 — Bridge Onboarding**.

## Work rules until state changes

- GitHub remains the intended canonical source, but the code baseline is incomplete until M0.1 materializes the recovered snapshot.
- Do not modify/delete the owner recovery archive or original local source as part of recovery.
- Do not decompile the whole APK; current source/APK correspondence evidence is already sufficient.
- Do not touch payroll semantics during recovery except to preserve the recovered implementation exactly.
- Do not upgrade dependencies during M0–M3 unless a specific dependency blocks reproducible build and the change is isolated.
- Do not merge/release/deploy without explicit owner permission/task authorization.
- After each completed milestone, update this file with actual branch/HEAD, evidence, completed gate and exact next boundary.
