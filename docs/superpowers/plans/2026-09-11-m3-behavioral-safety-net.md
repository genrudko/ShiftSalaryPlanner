# M3 — Behavioral Safety Net Implementation Plan

**Goal:** turn the recovered v7.1 behavior into an executable regression boundary before any architecture/refactor/redesign work.

**Status:** implementation + automated verification complete on branch; push/merge boundary pending.

**Base:** canonical `master` at `01a9bf47d96eeeb3babb2e8c98057a5871759f5c`.

**Branch/worktree:** `test/m3-behavioral-safety-net` at `/home/eodadmin/.local/state/development-bridge/worktrees/shift-salary-planner-m3`.

## Scope rules

- Do not redesign UI or navigation.
- Do not change payroll semantics to make tests prettier.
- Do not upgrade production dependencies.
- Small production seams are allowed only when required to test existing behavior deterministically.
- New dependencies must be test-only and justified by a concrete compatibility test.
- Preserve Room schema version 6 and backup schema version 1.
- Any newly discovered behavior mismatch is reported as a finding first; it is not silently corrected unless it is a proven regression/blocker introduced by M3 itself.

## Baseline

Fresh `:app:testDebugUnitTest` from the M3 base must pass before changes. Expected historical baseline: 26 tests, 0 failures/errors/skips.

## Task 1 — Payroll characterization matrix

**Files:**
- add `app/src/test/java/com/vigilante/shiftsalaryplanner/payroll/PayrollCharacterizationTest.kt`
- add `app/src/main/java/com/vigilante/shiftsalaryplanner/payroll/PayrollWorkplacePolicy.kt` only for the current multi-workplace selection seam
- add `app/src/test/java/com/vigilante/shiftsalaryplanner/payroll/PayrollWorkplacePolicyTest.kt`
- minimally replace the corresponding private `MainActivity` selection block with the tested helper

**Protect:**
- monthly salary proration;
- per-shift pay and its taxable/non-taxable behavior;
- vacation/sick coexistence with worked shifts;
- current all-workplaces policy: explicit workplace wins; one effective workplace in the period uses that workplace settings; multiple workplaces fall back to root/all-workplaces settings; system-status entries do not select payroll workplace.

**Gate:** targeted payroll tests then full app unit tests.

## Task 2 — Room migration fixtures

**Files:**
- add test-only SQLite JDBC dependency in version catalog/app test configuration;
- add a tiny internal migration-SQL contract used by `AppDatabase` (no schema/version change);
- add `app/src/test/java/com/vigilante/shiftsalaryplanner/data/AppDatabaseMigrationContractTest.kt`.

**Protect:**
- migration 4→5 preserves an existing shift template and gives `shiftPayAmount = 0`;
- migration 5→6 preserves an existing shift-day row and adds all eight recovered override columns as nullable/default-null.

**Gate:** migration test executes SQL against in-memory SQLite; then full app unit tests.

## Task 3 — Backup schema-v1 compatibility fixtures

**Files:**
- add test-only real `org.json` implementation if required by local JVM tests;
- add `app/src/test/resources/backup/v1-recovered-overrides.json`;
- add `app/src/test/resources/backup/v1-legacy-minimal.json`;
- add `app/src/test/java/com/vigilante/shiftsalaryplanner/BackupCompatibilityTest.kt`.

**Protect:**
- recovered per-day override fields survive parse;
- `shiftPayAmount` survives parse;
- legacy v1 backups without recovered fields remain readable with null/zero defaults;
- typed SharedPreferences snapshot payload remains present and addressable.

Do not change backup schema semantics merely to satisfy tests.

## Task 4 — Deterministic alarm planning rules

**Files:**
- minimally extract/inject the pure upcoming-alarm planning clock in `ShiftAlarmScheduler.kt`;
- add `app/src/test/java/com/vigilante/shiftsalaryplanner/ShiftAlarmPlanningTest.kt`.

**Protect:**
- disabled scheduler/auto-reschedule produces no plans;
- past triggers and dates outside horizon are excluded;
- missing/disabled templates/configs/alarms are excluded;
- suppression keys exclude only matching alarms;
- output is chronological and limit is honored;
- alarm key remains `date|shiftCode|alarmId`.

Production alarm delivery/PendingIntent/Android permission behavior remains untouched.

## Task 5 — Manual/device qualification contract

**File:** `docs/project/M3_DEVICE_QUALIFICATION.md`.

Record checks that JVM tests cannot prove: upgrade from old DB on device, backup export→restore, reboot reschedule, exact-alarm permission, notification permission, full-screen intent, locked/doze alarm, Wear mirror smoke check. This is a checklist/evidence contract, not a claim that unavailable live-device checks were executed.

## Task 6 — Final M3 qualification

Run on one unchanged tree:

```bash
source scripts/android/vps-env.sh
./gradlew --no-daemon clean   :app:testDebugUnitTest   :app:assembleDebug :wear:assembleDebug   :app:lintDebug :wear:lintDebug
```

Then:
- count test XML exactly;
- `git diff --check`;
- verify only M3 test/seam/docs/test-dependency changes;
- independent Codex review of the whole M3 diff;
- repair only proven Critical/Important/P2 findings;
- rerun affected/full gate after any repair;
- commit and guarded-push `test/m3-behavioral-safety-net`;
- update `CURRENT_STATE.md` to `M3 VERIFIED/PUSHED — READY FOR MERGE`;
- stop before merge/release/deploy unless owner explicitly authorizes that gate.

## Exit criterion

M3 is ready for merge only when a future refactor would get a red automated signal before it can silently alter core payroll outcomes, recovered persistence/backup compatibility, or deterministic alarm-planning rules, with device-only gaps explicitly documented rather than implied covered.
