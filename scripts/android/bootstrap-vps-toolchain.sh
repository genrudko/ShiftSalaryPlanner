#!/usr/bin/env bash
set -euo pipefail

SSP_HOME="${SSP_HOME:-${HOME:-/home/eodadmin}}"
TOOLCHAIN_ROOT="${SSP_TOOLCHAIN_ROOT:-${SSP_HOME}/.local/share/shift-salary-planner}"
DOWNLOAD_DIR="${TOOLCHAIN_ROOT}/downloads"
JDK_NAME="jdk-21.0.12.1+1"
JDK_DIR="${TOOLCHAIN_ROOT}/${JDK_NAME}"
JDK_ARCHIVE="${DOWNLOAD_DIR}/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz"
JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz"
JDK_SHA256="ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94"

ANDROID_SDK_ROOT="${TOOLCHAIN_ROOT}/android-sdk"
ANDROID_CLI_VERSION="23.0"
ANDROID_CLI_ARCHIVE="${DOWNLOAD_DIR}/commandlinetools-linux-16111833_latest.zip"
ANDROID_CLI_URL="https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip"
ANDROID_CLI_SHA1="e025545c62a8e64c7559119566a569fb1dec5f60"
ANDROID_CLI_DIR="${ANDROID_SDK_ROOT}/cmdline-tools/${ANDROID_CLI_VERSION}"

mkdir -p "${DOWNLOAD_DIR}" "${ANDROID_SDK_ROOT}" "${SSP_HOME}/.android"
touch "${SSP_HOME}/.android/repositories.cfg"

fetch() {
    local url="$1"
    local destination="$2"
    if [[ -f "${destination}" ]]; then
        return 0
    fi
    local temp="${destination}.part"
    rm -f "${temp}"
    curl --fail --location --retry 4 --retry-delay 2 --connect-timeout 20 \
        --user-agent 'ShiftSalaryPlanner-M2/1.0' \
        --output "${temp}" "${url}"
    mv "${temp}" "${destination}"
}

verify_sha256() {
    local expected="$1"
    local file="$2"
    printf '%s  %s\n' "${expected}" "${file}" | sha256sum --check --status
}

verify_sha1() {
    local expected="$1"
    local file="$2"
    printf '%s  %s\n' "${expected}" "${file}" | sha1sum --check --status
}

fetch "${JDK_URL}" "${JDK_ARCHIVE}"
if ! verify_sha256 "${JDK_SHA256}" "${JDK_ARCHIVE}"; then
    echo "JDK checksum mismatch: ${JDK_ARCHIVE}" >&2
    exit 1
fi

if [[ ! -x "${JDK_DIR}/bin/java" ]]; then
    temp_dir="$(mktemp -d "${TOOLCHAIN_ROOT}/.jdk-install.XXXXXX")"
    trap 'rm -rf "${temp_dir:-}"' EXIT
    tar -xzf "${JDK_ARCHIVE}" -C "${temp_dir}"
    extracted="$(find "${temp_dir}" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
    if [[ -z "${extracted}" || ! -x "${extracted}/bin/java" ]]; then
        echo "Unexpected JDK archive layout" >&2
        exit 1
    fi
    rm -rf "${JDK_DIR}"
    mv "${extracted}" "${JDK_DIR}"
    rm -rf "${temp_dir}"
    trap - EXIT
fi

fetch "${ANDROID_CLI_URL}" "${ANDROID_CLI_ARCHIVE}"
if ! verify_sha1 "${ANDROID_CLI_SHA1}" "${ANDROID_CLI_ARCHIVE}"; then
    echo "Android command-line tools checksum mismatch: ${ANDROID_CLI_ARCHIVE}" >&2
    exit 1
fi

if [[ ! -x "${ANDROID_CLI_DIR}/bin/sdkmanager" ]]; then
    temp_dir="$(mktemp -d "${TOOLCHAIN_ROOT}/.android-cli-install.XXXXXX")"
    trap 'rm -rf "${temp_dir:-}"' EXIT
    unzip -q "${ANDROID_CLI_ARCHIVE}" -d "${temp_dir}"
    if [[ ! -x "${temp_dir}/cmdline-tools/bin/sdkmanager" ]]; then
        echo "Unexpected Android command-line tools archive layout" >&2
        exit 1
    fi
    rm -rf "${ANDROID_CLI_DIR}"
    mkdir -p "$(dirname "${ANDROID_CLI_DIR}")"
    mv "${temp_dir}/cmdline-tools" "${ANDROID_CLI_DIR}"
    rm -rf "${temp_dir}"
    trap - EXIT
fi

ln -sfn "${ANDROID_CLI_VERSION}" "${ANDROID_SDK_ROOT}/cmdline-tools/latest"

export HOME="${SSP_HOME}"
export JAVA_HOME="${JDK_DIR}"
export ANDROID_HOME="${ANDROID_SDK_ROOT}"
export ANDROID_SDK_ROOT
export PATH="${JAVA_HOME}/bin:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH:-/usr/bin:/bin}"

# sdkmanager may close stdin after all licences are accepted; do not let yes(1)'s SIGPIPE
# obscure a successful licence run.
set +o pipefail
yes | sdkmanager --licenses >/dev/null
license_status=${PIPESTATUS[1]}
set -o pipefail
if [[ ${license_status} -ne 0 ]]; then
    echo "Android SDK licence acceptance failed with exit code ${license_status}" >&2
    exit "${license_status}"
fi

sdkmanager \
    'platform-tools' \
    'platforms;android-36' \
    'platforms;android-36.1' \
    'build-tools;36.0.0' \
    'build-tools;36.1.0'

printf 'JDK_DIR=%s\n' "${JDK_DIR}"
printf 'ANDROID_SDK_ROOT=%s\n' "${ANDROID_SDK_ROOT}"
"${JAVA_HOME}/bin/java" -version
sdkmanager --version
