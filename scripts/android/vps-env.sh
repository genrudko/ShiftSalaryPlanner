#!/usr/bin/env bash
# Source this file before running Gradle on the ShiftSalaryPlanner VPS.

SSP_HOME="${SSP_HOME:-${HOME:-/home/eodadmin}}"
SSP_TOOLCHAIN_ROOT="${SSP_TOOLCHAIN_ROOT:-${SSP_HOME}/.local/share/shift-salary-planner}"
SSP_JDK_DIR="${SSP_TOOLCHAIN_ROOT}/jdk-21.0.12.1+1"
SSP_ANDROID_SDK_ROOT="${SSP_TOOLCHAIN_ROOT}/android-sdk"

if [[ ! -x "${SSP_JDK_DIR}/bin/java" ]]; then
    echo "ShiftSalaryPlanner JDK is missing. Run scripts/android/bootstrap-vps-toolchain.sh first." >&2
    return 1 2>/dev/null || exit 1
fi

if [[ ! -x "${SSP_ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" ]]; then
    echo "ShiftSalaryPlanner Android SDK is missing. Run scripts/android/bootstrap-vps-toolchain.sh first." >&2
    return 1 2>/dev/null || exit 1
fi

export HOME="${SSP_HOME}"
export JAVA_HOME="${SSP_JDK_DIR}"
export ANDROID_HOME="${SSP_ANDROID_SDK_ROOT}"
export ANDROID_SDK_ROOT="${SSP_ANDROID_SDK_ROOT}"
export GRADLE_USER_HOME="${SSP_GRADLE_USER_HOME:-${SSP_TOOLCHAIN_ROOT}/gradle-home}"
export PATH="${JAVA_HOME}/bin:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH:-/usr/bin:/bin}"
