# ShiftSalaryPlanner — Current State

Обновлено: **2026-09-11**

> Это живой документ текущей границы. Долговременный план и решения: [`VNEXT_MODERNIZATION.md`](./VNEXT_MODERNIZATION.md).

## Current phase

**PRE-M0 / planning complete, implementation not started.**

Следующая разрешённая работа: **M0 — Recovery & Canonical Baseline**.

Не начинать refactor/redesign до завершения M0–M3 в порядке, установленном roadmap.

## Repository state at plan creation

- Repository: `genrudko/ShiftSalaryPlanner`
- Default branch: `master`
- Known Git head at plan creation: `3ece60f6ee694ef1baf251e7eec5beb29f146e45`
- Commit date: 2026-06-04
- Commit subject: `Use workplace payroll settings for all-work filter`
- Git build metadata at this commit: `versionCode 194`, `versionName 6.4`
- Current repo is not yet onboarded as its own Development Bridge managed repo.

## External recovery artifact

Owner-provided APK fingerprint:

- SHA-256: `665888705c888c73ebc809e8a75cf46f586f115af7789a0412adfc700f572ba5`
- embedded AGP Git revision: `3ece60f6ee694ef1baf251e7eec5beb29f146e45`

Do **not** infer source freshness from APK `versionName`/`versionCode`: the owner assigned versions manually.

The owner still has the local Android Studio/project folder that was used near the last development period. It has not yet been forensically compared with GitHub in this project track.

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

## Next task: M0

M0 must collect evidence from the owner's local project **before modifying it**:

```text
git rev-parse HEAD
git status --short --branch
git diff
git diff --staged
untracked file inventory
```

Then compare with known GitHub commit `3ece60f6...`.

### M0 outcomes

**A. Local tree clean on `3ece60f6...`**  
Declare that GitHub commit the recovered canonical baseline.

**B. Same HEAD, dirty tree**  
Preserve full local diff/untracked files, determine whether changes belong to the last working APK/state, then commit/recover deliberately.

**C. Different HEAD/branch/history**  
Preserve evidence and compare histories before choosing baseline. Do not force-reset or overwrite anything.

### M0 stop condition

Stop when a single canonical source baseline is proven and documented. Do not roll directly into refactor/redesign in the same task.

## Work rules until state changes

- GitHub is the intended future canonical source, but final baseline status is pending M0 verification.
- Do not modify/delete owner local source before recovery evidence exists.
- Do not decompile the whole APK unless source recovery actually requires it.
- Do not touch payroll semantics during baseline recovery.
- Do not upgrade dependencies during M0–M3 unless a specific dependency blocks reproducible build and the change is isolated.
- Do not merge/release/deploy without explicit owner permission/task authorization.
- After each completed milestone, update this file with actual branch/HEAD, evidence, completed gate and exact next boundary.
