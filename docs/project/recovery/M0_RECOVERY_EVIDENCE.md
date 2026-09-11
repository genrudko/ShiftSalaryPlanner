# M0 — Recovery Evidence

Дата фиксации: **2026-09-11**  
Статус: **forensic evidence complete; recovered source snapshot proven, not yet materialized into Git**

Этот документ фиксирует результат сравнения последней предоставленной владельцем полной папки проекта с GitHub baseline и последней рабочей APK. Он нужен, чтобы новый чат/исполнитель не восстанавливал историю из переписки.

## 1. Recovery artifacts

### Full project archive

- logical name: `ShiftSalaryPlanner.zip`
- SHA-256: `426a7720d37a092d819860d9613408d6d44a27f96f75ae87e2c816a1c5983e19`
- archive contains the complete Android Studio project directory including `.git` metadata;
- archive also contains machine-local build material (`local.properties` and a local stable debug keystore). These are recovery/build evidence only and **must not be committed to GitHub**.

### Owner-provided APK

- logical name: `ShiftSalaryPlanner.apk`
- SHA-256: `665888705c888c73ebc809e8a75cf46f586f115af7789a0412adfc700f572ba5`
- APK metadata: `versionCode 201`, `versionName 7.1`;
- embedded AGP VCS revision: `3ece60f6ee694ef1baf251e7eec5beb29f146e45`.

Version numbers are not used to infer source chronology because the owner assigned them manually.

## 2. Git evidence from the archived project

The archived project reports:

```text
branch: master
HEAD: 3ece60f6ee694ef1baf251e7eec5beb29f146e45
origin: https://github.com/genrudko/ShiftSalaryPlanner.git
staged changes: 0
```

The base commit is therefore exactly the last pre-recovery GitHub code commit known at project-planning time.

Raw `git status` reports 155 tracked files as modified plus one untracked test. Most tracked changes are line-ending noise caused by the archived Windows working tree. Comparing with `git diff --ignore-cr-at-eol` reduces the substantive delta to:

- **24 tracked files with semantic changes**;
- **1 untracked test file**;
- semantic diff size: **1434 insertions / 182 deletions**.

A normalized recovery patch was generated from only the semantic delta and verified by applying it to a clean detached checkout of `3ece60f6...`. The resulting recovered files matched the archived files byte-for-byte after CRLF→LF normalization for all 25 semantic paths.

Normalized recovery patch SHA-256 at verification time:

```text
ede38d105ca3cc4b21f4af9138ef7b3896c072a976b48e8595ba9deac3d1d9dc
```

The patch itself is not currently stored in GitHub; the source archive remains the recovery artifact until the snapshot is materialized in a dedicated recovery task.

## 3. Semantic file inventory

Tracked semantic changes:

```text
.gitignore
app/build.gradle.kts
app/src/main/java/com/vigilante/shiftsalaryplanner/alarms/ShiftAlarmScheduler.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/data/AppDatabase.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/data/ShiftDayEntity.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/domain/shift/ShiftWorkMappingUtils.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/payroll/AdditionalPayment.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/payroll/PayrollCalculator.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/settings/PayrollSettingsStore.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WorkplacePayrollSettingsStore.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/alarms/ShiftAlarmsEffects.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarDayCell.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarHeaderComponents.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/CalendarTab.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/calendar/DayAssignmentsDialog.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/AdditionalPaymentDialog.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payments/PaymentEnhancements.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsDialog.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/payroll/settings/PayrollSettingsHelpers.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/CurrentParametersScreen.kt
app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/backup/BackupRestore.kt
app/src/test/java/com/vigilante/shiftsalaryplanner/ShiftHolidayPayrollMappingTest.kt
app/src/test/java/com/vigilante/shiftsalaryplanner/payroll/PayrollCalculatorTest.kt
```

Untracked semantic file:

```text
app/src/test/java/com/vigilante/shiftsalaryplanner/PaymentEnhancementsTest.kt
```

## 4. What changed at a product level

The recovered delta is not merely version-number churn. Confirmed changes include:

- per-day shift overrides persisted in Room, including migration **5→6** and fields for start/end time, total/break/night/paid hours, shift pay amount and note;
- UI for editing/resetting individual-day overrides;
- payroll legislation/profile and overtime-payment configuration, including custom/percentage modes;
- `AdditionalPaymentType.PER_SHIFT` and corresponding payment-resolution logic;
- fixes preventing already-resolved hourly/% additional payments from being multiplied a second time;
- expanded payroll and holiday mapping tests plus the new `PaymentEnhancementsTest`;
- clearing/rescheduling suppressed alarms for dates/ranges;
- backup compatibility handling for the newly introduced day-override fields;
- related calendar/payroll/settings UI changes;
- build metadata changed from GitHub `194 / 6.4` to local `201 / 7.1`.

These changes include payroll and persistence semantics and therefore must be preserved before any vNext refactor.

## 5. APK correspondence evidence

The APK and recovered source share the same embedded Git base revision `3ece60f6...` and the same local build metadata `201 / 7.1`.

Additionally, compiled DEX content in the provided APK contains identifiers/strings introduced by the recovered local delta and absent from the GitHub `3ece60f6...` source, including:

```text
overrideStartTime
legislationProfile
overtimePercentOfHourly
clearSuppressedAlarmsForRange
Индивидуальная правка смены
За смену/день
```

This is strong evidence that the archived dirty working tree belongs to the same post-`3ece60f6` development generation as the provided working APK. Exact reproducible-binary equivalence has not yet been demonstrated and is not claimed.

## 6. Chronology signal

The semantic source files in the archive carry filesystem timestamps on **2026-06-07**, after the Git commit on 2026-06-04. The latest of the substantive source changes is around the local build/version update on 2026-06-07. Timestamps are supporting evidence only, not source-of-truth identifiers.

## 7. Recovery decision

M0 outcome is **B: same HEAD, dirty tree**.

The recovered working-tree snapshot is considered the best known last source state. GitHub commit `3ece60f6...` alone is **not** sufficient as the application baseline because it lacks the 24+1 semantic changes described above.

Before M1 Bridge onboarding or any refactor, perform a bounded **M0.1 Materialize Recovered Baseline** task:

1. start from current GitHub `master` (which contains only canonical documentation changes on top of `3ece60f6...`);
2. import exactly the 24 tracked semantic changes plus the one new test from the recovery archive;
3. normalize line endings and exclude the other CRLF-only changes;
4. do not import `local.properties`, keystores, build directories, IDE state, credentials or other machine-local artifacts;
5. review the resulting diff against this inventory;
6. run whatever tests/build are feasible without altering semantics;
7. commit the recovered source as a dedicated recovery commit;
8. update `CURRENT_STATE.md` with the resulting Git SHA.

Do not combine M0.1 with architecture refactor, dependency modernization or redesign.
