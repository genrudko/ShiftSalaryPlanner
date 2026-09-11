# M2 Build Environment

Updated: **2026-09-11**

This document defines the reproducible VPS build environment for ShiftSalaryPlanner. The toolchain is installed outside the repository and contains no production signing material.

## Pinned toolchain

- OS used for qualification: Ubuntu 24.04 x86_64.
- JDK: Eclipse Temurin `21.0.12.1+1`.
- JDK Linux x64 archive SHA-256: `ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94`.
- Gradle: repository wrapper `9.4.1` with its existing wrapper checksum.
- Android command-line tools: `23.0`, archive build `16111833`.
- Android CLI archive SHA-1 from Google's SDK repository metadata: `e025545c62a8e64c7559119566a569fb1dec5f60`.
- Android SDK packages: `platform-tools`, `platforms;android-36`, `platforms;android-36.1`, `build-tools;36.0.0`, `build-tools;36.1.0`.

The canonical scripts are:

```text
scripts/android/bootstrap-vps-toolchain.sh
scripts/android/vps-env.sh
```

## VPS paths

Default toolchain root:

```text
/home/eodadmin/.local/share/shift-salary-planner
```

Contents include:

```text
jdk-21.0.12.1+1/
android-sdk/
gradle-home/
downloads/
```

These directories are deliberately outside the Git checkout and are not committed.

## Bootstrap

From the repository root:

```bash
scripts/android/bootstrap-vps-toolchain.sh
```

The bootstrap is idempotent: downloaded archives are checksum-verified, existing installed tools are reused, and required SDK packages are reconciled by `sdkmanager`.

Before a Gradle command:

```bash
. scripts/android/vps-env.sh
```

The environment script exports `HOME`, `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME`, and `PATH`. It deliberately has a `/home/eodadmin` fallback because Development Bridge durable jobs may start without `HOME`.

## Signing boundary

No keystore is installed by this bootstrap. Debug/CI signing and release signing are repository build-policy concerns handled separately in M2. Production signing secrets must never be committed or copied from the recovery archive merely to make VPS builds pass.


## Debug and release signing configuration

Debug builds require no owner keystore on the VPS. If `stableDebug.storeFile` / `SSP_STABLE_DEBUG_STORE_FILE` is not explicitly set and the legacy file `~/.android/shift-salary-stable-debug.keystore` is absent, both modules use Android's normal default debug keystore. Because phone and Wear are built under the same user/environment, they resolve to the same debug certificate.

An existing developer machine that already has `~/.android/shift-salary-stable-debug.keystore` keeps using it for debug builds. An explicitly configured stable-debug path that does not exist fails fast instead of silently changing identity.

Release signing is separate and opt-in. The supported keys are:

```text
releaseSigning.storeFile      / SSP_RELEASE_STORE_FILE
releaseSigning.storePassword  / SSP_RELEASE_STORE_PASSWORD
releaseSigning.keyAlias       / SSP_RELEASE_KEY_ALIAS
releaseSigning.keyPassword    / SSP_RELEASE_KEY_PASSWORD
```

All four values must be supplied together. M2 does not choose or rotate the production certificate; later release qualification must point these values at the certificate required for store update compatibility. No release key is stored on the VPS by this project bootstrap.

## Qualification result — 2026-09-11

Qualification was performed in the isolated `infra/m2-reproducible-build` worktree with neither `local.properties` nor the archived owner stable-debug keystore present. The bootstrap was rerun successfully before the final gate, confirming idempotent user-space provisioning.

Final Gradle gate:

```bash
./gradlew clean \
  :app:testDebugUnitTest \
  :app:assembleDebug :wear:assembleDebug \
  :app:lintDebug :wear:lintDebug \
  :app:signingReport :wear:signingReport \
  --no-daemon --stacktrace
```

Gradle completed successfully in 14m 12s with 103 executed tasks. Generated test XML reported exactly 26 tests, 0 failures, 0 errors and 0 skipped across 7 suites.

Artifacts from that gate:

```text
app/build/outputs/apk/debug/app-debug.apk
  size: 38,782,813 bytes
  SHA-256: e75fa6a67e0b21b38833e4ec4224554cb6ee80ab94c04a07a8f396dbc7d031c1

wear/build/outputs/apk/debug/wear-debug.apk
  size: 70,439,979 bytes
  SHA-256: 0747d838f4ff3eaa73d32f90b9592d891be1aaaef18f5ac7766ada5172ee711a
```

Lint result:

```text
app:  0 errors, 58 warnings
wear: 0 errors, 22 warnings
```

Warnings are non-blocking existing/deprecation findings and are not blanket-suppressed by M2. The blocking notification/API/private-API findings encountered during qualification were repaired narrowly and re-linted.

Debug signing identity is aligned between phone and Wear:

```text
SHA-1:   3E:42:3B:21:14:15:AF:5C:B1:EE:97:FD:F4:EC:EA:FA:CF:A1:BF:C4
SHA-256: D6:64:F8:E8:A1:D6:E7:F9:4C:22:01:AA:1D:BD:73:1D:F5:22:60:3A:45:3A:4C:9A:62:5C:CD:91:04:35:0A:3D
```

Both release variants report `Config: null`, as intended until production signing is explicitly qualified later.

### Configuration-cache exception for `signingReport`

With `org.gradle.configuration-cache=true`, Gradle 9.4.1 can fail while reloading cached AGP 9.2.1 `SigningReportTask` state (`SigningConfig$AgpDecorated` class-loading failure). This was isolated by proving the same reports succeed with configuration cache disabled.

The build scripts therefore mark only the two `signingReport` tasks as `notCompatibleWithConfigurationCache`. After that change, the normal signing command was executed twice consecutively and succeeded both times. Gradle explicitly discards configuration-cache entries for these incompatible tasks; the rest of the build keeps configuration-cache support.

### Compatibility repairs required by the gate

M2 did not perform product refactoring. The source edits outside build policy are bounded compatibility repairs required for the existing target/minSdk surface:

- Wear notification dispatch now checks runtime `POST_NOTIFICATIONS` permission before `notify()` on Android 13+;
- `Ringtone.isLooping` is guarded for API 28+ while the existing keep-alive path remains available below it;
- NumberPicker theming uses public `setTextColor()` on API 29+, preserves the legacy text-color path on API 27–28, and does not execute blocked private divider access on Android 16.

An independent Codex review caught the initial pre-Q NumberPicker readability regression; after the compatibility correction, a second independent review returned no findings.

## Final pre-merge rerun

Immediately before the owner-authorized M2 merge, the full Gradle gate was rerun on the unchanged implementation tree. Gradle reported `BUILD SUCCESSFUL in 12m 59s` with 103 actionable tasks (102 executed, 1 up-to-date). Test XML again reported 26 tests with zero failures/errors/skips, both APK SHA-256 values matched the qualification values above, and phone/Wear debug certificates matched.

The job wrapper exited nonzero only because its post-build grep expected a lint summary ending at `warnings`, while current lint text includes a trailing `hints` count. Direct report inspection confirmed `app: 0 errors, 58 warnings, 12 hints` and `wear: 0 errors, 22 warnings, 3 hints`. `git diff --check` was then run separately and passed. No source/build-policy changes followed this rerun; only M2 closeout documentation changed.
