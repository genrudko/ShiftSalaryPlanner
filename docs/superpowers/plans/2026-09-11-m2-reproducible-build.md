# M2 Reproducible Build Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make canonical ShiftSalaryPlanner build and unit-test reproducibly on the VPS without the owner's computer, archived keystore, or machine-local Android Studio state.

**Architecture:** Keep toolchain provisioning user-local and pinned, with no system-package or sudo dependency. Debug builds use Android's normal generated debug signing unless an explicit stable-debug key is configured; production/release signing is separately opt-in and never stored in the repository. Both phone and Wear modules use the same debug-signing policy so Data Layer development builds remain signature-compatible.

**Tech Stack:** Gradle 9.4.1 wrapper, Android Gradle Plugin 9.2.1, Temurin JDK 21, Android SDK 36 / Build Tools 36.1.0, Kotlin/Compose, Bash.

**Spec:** `docs/project/VNEXT_MODERNIZATION.md` and `docs/project/CURRENT_STATE.md`

## Global Constraints

- Canonical recovered baseline before M2: `304bf96cec26c4e7c5fe94a03f2579ceeef5996b`; current branch base includes docs commit `4ad9f68d458f901164dda240366974d55abaf7ea`.
- Do not alter payroll, Room schema/data semantics, alarm behavior, navigation, UI or dependencies in M2.
- Do not commit `local.properties`, any keystore, credentials, Android SDK, JDK binaries, Gradle caches or build outputs.
- Do not rotate the production signing identity. M2 only separates configuration; M20 will qualify the actual production certificate/release flow.
- A missing optional stable-debug key must not block debug configuration/build.
- Phone and Wear debug APKs must resolve to the same signing identity in one VPS environment.
- Toolchain is user-local because passwordless sudo is unavailable on the VPS.
- Pinned JDK: Eclipse Temurin `jdk-21.0.12.1+1`, Linux x64 archive SHA-256 `ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94`.
- Pinned Android command-line tools: `23.0`, Linux archive `commandlinetools-linux-16111833_latest.zip`, repository checksum `e025545c62a8e64c7559119566a569fb1dec5f60` (SHA-1 as published in Google's SDK repository metadata).
- Required SDK packages: `platform-tools`, `platforms;android-36`, `build-tools;36.1.0`.

---

### Task 1: Pinned user-space Android toolchain

**Files:**
- Create: `scripts/android/bootstrap-vps-toolchain.sh`
- Create: `scripts/android/vps-env.sh`
- Create: `docs/project/M2_BUILD_ENVIRONMENT.md`

**Interfaces:**
- Produces: user-local JDK at `${SSP_TOOLCHAIN_ROOT:-$HOME/.local/share/shift-salary-planner}/jdk-21.0.12.1+1`.
- Produces: Android SDK at `${SSP_TOOLCHAIN_ROOT:-$HOME/.local/share/shift-salary-planner}/android-sdk`.
- `vps-env.sh` exports `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME` and prepends Java/SDK tools to `PATH`.

- [ ] **Step 1: Add the bootstrap script with pinned download/checksum validation.**

The script must use `set -euo pipefail`, reject a bad checksum before extraction, install only under the user-local toolchain root, accept Android SDK licenses non-interactively, and install exactly the required SDK packages.

JDK archive:

```text
https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz
```

Android CLI archive:

```text
https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
```

- [ ] **Step 2: Add `vps-env.sh`.**

It must fail clearly when bootstrap has not been run and must not depend on the caller already having `HOME`, because Development Bridge durable jobs may start with a minimal environment. Use `/home/eodadmin` only as the VPS fallback when `HOME` is unset; allow `SSP_HOME`/`SSP_TOOLCHAIN_ROOT` overrides.

- [ ] **Step 3: Bootstrap the toolchain and verify versions.**

Run:

```bash
scripts/android/bootstrap-vps-toolchain.sh
. scripts/android/vps-env.sh
java -version
sdkmanager --version
sdkmanager --list_installed
./gradlew --version
```

Expected: JDK 21.0.12.1, SDK packages present, Gradle 9.4.1 starts successfully.

- [ ] **Step 4: Record exact environment contract in `M2_BUILD_ENVIRONMENT.md`.**

Document pinned versions, local paths, bootstrap command, env command, and the fact that binaries/caches are intentionally not committed.

### Task 2: Separate debug/CI signing from release signing

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `wear/build.gradle.kts`

**Interfaces:**
- Optional developer stable-debug properties: existing `stableDebug.storeFile`, `stableDebug.storePassword`, `stableDebug.keyAlias`, `stableDebug.keyPassword`.
- Optional production/release properties: `releaseSigning.storeFile`, `releaseSigning.storePassword`, `releaseSigning.keyAlias`, `releaseSigning.keyPassword`.
- Environment aliases: `SSP_STABLE_DEBUG_STORE_FILE`, `SSP_STABLE_DEBUG_STORE_PASSWORD`, `SSP_STABLE_DEBUG_KEY_ALIAS`, `SSP_STABLE_DEBUG_KEY_PASSWORD`; analogous `SSP_RELEASE_*` names.
- When no stable-debug config is supplied, Android's normal default debug signing is used.
- When release signing is absent, release remains unsigned; no production key is invented or copied to the VPS.

- [ ] **Step 1: Capture the current failure.**

After Task 1, run without owner keystore/local signing properties:

```bash
. scripts/android/vps-env.sh
./gradlew :app:tasks :wear:tasks --no-daemon
```

Expected before fix: configuration fails with `Stable signing keystore not found`.

- [ ] **Step 2: Implement optional stable-debug signing.**

For each module, only create/assign `stableDebug` when an explicit stable-debug path resolves to an existing file. If a path was explicitly supplied but does not exist, fail with an actionable `GradleException`. If no path was supplied, do not configure a custom debug signing config and allow AGP's default debug keystore behavior.

- [ ] **Step 3: Implement separately named release signing.**

Resolve all four `releaseSigning.*` fields from `local.properties` with environment fallbacks. Only create/assign the release signing config when all required values are supplied and the file exists. Never fall back to the stable-debug key for release automatically.

- [ ] **Step 4: Make signing diagnostics lazy.**

`printSigningSha1` must no longer make the whole project configuration depend on a stable-debug file. It may print the explicitly configured stable key when present, otherwise explain that default debug signing is used and direct the operator to Gradle `signingReport` for the generated debug key.

- [ ] **Step 5: Verify configuration without secrets.**

Run:

```bash
. scripts/android/vps-env.sh
./gradlew :app:tasks :wear:tasks --no-daemon
```

Expected: configuration succeeds with no `local.properties` and no archived keystore.

### Task 3: Fresh VPS verification gate

**Files:**
- No source changes expected unless a genuine M2-only build-infrastructure defect is proven.

- [ ] **Step 1: Run recovered unit tests.**

```bash
. scripts/android/vps-env.sh
./gradlew :app:testDebugUnitTest --no-daemon --stacktrace
```

Record exact test count/failures from generated XML; do not rely only on Gradle's final line.

- [ ] **Step 2: Assemble phone and Wear debug APKs.**

```bash
./gradlew :app:assembleDebug :wear:assembleDebug --no-daemon --stacktrace
```

Expected: both APKs produced with no owner keystore.

- [ ] **Step 3: Verify signing identity alignment.**

```bash
./gradlew :app:signingReport :wear:signingReport --no-daemon
```

Expected: phone and Wear `debug` variants report the same SHA-1/SHA-256 certificate.

- [ ] **Step 4: Run lint/build gate.**

```bash
./gradlew :app:lintDebug :wear:lintDebug --no-daemon --stacktrace
```

Any pre-existing lint findings must be classified; do not perform unrelated UI/domain cleanup in M2.

### Task 4: Canonicalize M2 result

**Files:**
- Modify: `docs/project/CURRENT_STATE.md`
- Modify: `docs/project/M2_BUILD_ENVIRONMENT.md`

- [ ] **Step 1: Record evidence.**

Capture JDK/SDK versions, exact Gradle commands, unit-test totals, APK outputs, signing fingerprint comparison, lint result, and any bounded residual blocker.

- [ ] **Step 2: Verify branch diff.**

Run:

```bash
git diff --check master...HEAD
git diff --stat master...HEAD
git status --short --branch
```

Expected: only M2 toolchain/build-signing/docs changes; no payroll/UI/domain semantics.

- [ ] **Step 3: Commit and push the M2 branch.**

Use Development Bridge guarded `git_push_plan → git_push`; never force-push.

- [ ] **Step 4: Advance the canonical boundary only after fresh verification.**

M2 is complete only if a clean checkout can bootstrap/use the toolchain and pass the agreed build/test gate without owner-local signing material. If genuine source/test/lint failures remain, record M2 as blocked/partial rather than falsely advancing to M3.
