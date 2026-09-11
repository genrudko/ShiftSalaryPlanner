# M3 — Device Qualification Contract

Updated: **2026-09-11**

## Purpose

M3 adds JVM-level behavioral protection for payroll, persistence/backup compatibility, and deterministic alarm planning. The checks below deliberately remain **device-only**: they depend on Android framework state, package upgrade behavior, permissions, reboot/doze delivery, or a paired Wear device and cannot be proven by the VPS JVM gate.

This file is an evidence checklist. An unchecked item means **NOT EXECUTED**, not implicitly passed.

## Qualification prerequisites

- Use an APK built from the exact commit being qualified.
- Record phone model, Android version/API, app version/commit, and whether the install is clean or an in-place upgrade.
- Do not use production signing material merely to execute this checklist; use the qualified debug/test identity unless release qualification explicitly requires otherwise.
- Preserve a copy of the pre-test database/backup when the scenario mutates user data.

## Device-only checks

### 1. In-place database upgrade

**Status:** NOT EXECUTED

- Install/run a build containing a pre-v6 database (or restore a known pre-v6 app data fixture) with at least one shift template and one saved shift day.
- Upgrade in place to the M3/current build without clearing app data.
- Confirm the app opens normally and the existing template/day remain present.
- Confirm recovered v6 per-day override fields are available and default to unset/null for old rows.

**Evidence to record:** source build/database version, target commit, device/API, before/after row or UI screenshots, crash/logcat result.

### 2. Backup v1 export → destructive restore

**Status:** NOT EXECUTED

- On the current build, create representative data including a shift template, a saved day with individual overrides, payroll/preferences data, and a non-zero per-shift amount where applicable.
- Export the normal app backup.
- Mutate or clear the represented app data so restore success is observable.
- Restore the exported backup through the user-facing restore flow.
- Confirm restored templates, days, recovered override values, `shiftPayAmount`, and representative typed preferences match the exported state.

**Evidence to record:** backup schema/version, target commit, before/export/after-restore values, result message, exceptions if any.

### 3. Reboot alarm reschedule

**Status:** NOT EXECUTED

- Configure at least one enabled future shift alarm inside the configured horizon.
- Confirm it appears in the app before reboot.
- Reboot the phone and allow normal boot completion.
- Confirm the app's boot/reschedule path restores the expected future alarm without creating an unintended duplicate.

**Evidence to record:** scheduled date/time/key before reboot, post-boot state, relevant logcat/alarm dump if available.

### 4. Exact-alarm permission behavior

**Status:** NOT EXECUTED

- Test on an Android version where exact-alarm access is user-controlled.
- Verify the app reports/handles the denied state without crashing.
- Grant exact-alarm access and reschedule.
- Confirm the expected alarm is accepted in exact/alarm-clock mode; if platform fallback is used, record it explicitly.

**Evidence to record:** Android/API, permission state before/after, app status text, scheduling result.

### 5. Notification permission behavior

**Status:** NOT EXECUTED

- On Android 13+ test notification permission denied and granted states.
- Confirm denied state is surfaced/handled without crash.
- Grant permission and confirm an alarm notification/ringing flow can be presented.

**Evidence to record:** Android/API, permission state, notification/ringing result, screenshots/logcat if useful.

### 6. Full-screen intent permission behavior

**Status:** NOT EXECUTED

- On an Android version that restricts full-screen intents, test with full-screen intent access disabled and enabled.
- Confirm the app handles the unavailable state without crash or silent corruption of scheduled alarms.
- With access enabled, confirm the alarm UI can be presented through the intended full-screen path.

**Evidence to record:** Android/API, permission state, observed UI path, logcat result.

### 7. Locked-screen / doze delivery

**Status:** NOT EXECUTED

- Schedule a near-future alarm, then lock the device and leave it idle long enough for doze/idle behavior to matter.
- Do not keep the app in foreground.
- Confirm the alarm arrives at the expected time and the configured sound/vibration/ring UI behavior is usable from the locked state.
- Exercise one snooze cycle and confirm the follow-up alarm is delivered.

**Evidence to record:** planned trigger, actual trigger, lock/doze conditions, snooze result, any timing deviation.

### 8. Wear mirror smoke test

**Status:** NOT EXECUTED

- Pair/connect the supported Wear companion build.
- Enable Wear mirroring in ShiftSalaryPlanner.
- Schedule one representative future shift alarm.
- Confirm the phone-side change reaches the watch and the watch presents the expected mirrored alarm metadata/behavior.
- Disable mirroring or remove the alarm and confirm stale state is not left active unintentionally.

**Evidence to record:** phone/watch models and OS versions, app/Wear commit/build, mirrored title/time, remove/disable result.

## M3 automated/device boundary

A green M3 VPS gate proves only the automated contracts committed in the repository. It does **not** prove any unchecked device scenario above. Device execution can be completed later on a physical test setup, and results should be appended here without rewriting historical automated evidence.
