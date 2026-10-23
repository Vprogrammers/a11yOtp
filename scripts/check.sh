#!/usr/bin/env bash
# Runs the project checks on this machine. Uses no GitHub Actions.
#
#   ./scripts/check.sh [all|kmp|android|ios] [--device [<iphone-id>]]
#
# --device also runs the device tests: Android on a connected, unlocked device or emulator,
# iOS UI tests on the connected, unlocked iPhone with the given id
# (find it with: xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations).
set -uo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly PROJECT="iosApp/iosApp.xcodeproj"

usage() {
    sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
}

target="all"
device=false
iphone_id=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        all | kmp | android | ios) target="$1" ;;
        --device)
            device=true
            if [[ $# -gt 1 && "$2" != -* && "$2" != all && "$2" != kmp && "$2" != android && "$2" != ios ]]; then
                iphone_id="$2"
                shift
            fi
            ;;
        -h | --help)
            usage
            exit 0
            ;;
        *)
            echo "error: unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
    shift
done

cd "${REPOSITORY_ROOT}"

passed=()
failed=()
skipped=()

# step <name> <command...>: runs one check and records the result without stopping the others.
step() {
    local name="$1"
    shift
    echo
    echo "==> ${name}"
    if "$@"; then
        passed+=("${name}")
    else
        failed+=("${name}")
    fi
}

skip() {
    echo
    echo "==> $1: skipped ($2)"
    skipped+=("$1 ($2)")
}

require() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "error: $1 is required for '${target}' checks. $2" >&2
        exit 1
    fi
}

java_ok() {
    java -version >/dev/null 2>&1
}

is_apple_silicon() {
    [[ "$(uname -m)" == "arm64" ]] || [[ "$(sysctl -n sysctl.proc_translated 2>/dev/null)" == "1" ]]
}

run_kmp() {
    java_ok || {
        echo "error: a JDK is required (JDK 21, see README)." >&2
        exit 1
    }
    step "ktlint" ./gradlew ktlintCheck
    step "shared and Android host tests" ./gradlew :a11yotp:testAndroidHostTest
    step "SMS Retriever host tests" ./gradlew :a11yotp-sms-retriever:testAndroidHostTest
    if [[ "$(uname)" == "Darwin" ]]; then
        # The ABI dump covers the iOS targets, which only build on macOS.
        step "public API (ABI) check" ./gradlew :a11yotp:checkKotlinAbi :a11yotp-sms-retriever:checkKotlinAbi
        step "compile Kotlin/Native tests" ./gradlew :a11yotp:compileTestKotlinIosSimulatorArm64
        step "link the Shared framework" ./gradlew :sampleShared:linkDebugFrameworkIosSimulatorArm64
        if is_apple_silicon; then
            step "Kotlin/Native tests on the iOS simulator" ./gradlew :a11yotp:iosSimulatorArm64Test
        else
            skip "Kotlin/Native tests on the iOS simulator" "needs Apple Silicon"
        fi
    else
        skip "Kotlin/Native checks" "needs macOS"
    fi
}

android_device_connected() {
    local adb_path
    adb_path="$(command -v adb || echo "${ANDROID_HOME:-${HOME}/Library/Android/sdk}/platform-tools/adb")"
    [[ -x "${adb_path}" ]] && "${adb_path}" devices | awk 'NR > 1 && $2 == "device"' | grep -q .
}

run_android() {
    java_ok || {
        echo "error: a JDK is required (JDK 21, see README)." >&2
        exit 1
    }
    step "compile Android device tests" ./gradlew :a11yotp:compileAndroidDeviceTest
    step "Android sample" ./gradlew :androidApp:assembleDebug
    step "publication and consumer" ./scripts/verify-publication.sh
    if [[ "${device}" == true ]]; then
        if android_device_connected; then
            step "Android device tests" ./gradlew :a11yotp:connectedAndroidDeviceTest
        else
            skip "Android device tests" "no device or emulator in 'adb devices'"
        fi
    else
        skip "Android device tests" "pass --device"
    fi
}

run_ios() {
    if [[ "$(uname)" != "Darwin" ]]; then
        skip "iOS checks" "needs macOS"
        return
    fi
    require xcodebuild "Install Xcode."
    step "SwiftLint" ./scripts/swiftlint.sh
    step "iOS sample for the simulator" xcodebuild build -project "${PROJECT}" -scheme iosApp \
        -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO -quiet
    step "iOS UI tests build" xcodebuild build-for-testing -project "${PROJECT}" -scheme iosApp \
        -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO -quiet
    if [[ "${device}" == true && -n "${iphone_id}" ]]; then
        step "iOS UI tests on the iPhone" xcodebuild test -project "${PROJECT}" -scheme iosApp \
            -destination "id=${iphone_id}" -allowProvisioningUpdates -quiet
    else
        skip "iOS UI tests on the iPhone" "pass --device <iphone-id>"
    fi
}

case "${target}" in
    kmp) run_kmp ;;
    android) run_android ;;
    ios) run_ios ;;
    all)
        run_kmp
        run_android
        run_ios
        ;;
esac

echo
echo "==> Summary (${target})"
for name in ${passed[@]+"${passed[@]}"}; do echo "  passed:  ${name}"; done
for name in ${skipped[@]+"${skipped[@]}"}; do echo "  skipped: ${name}"; done
for name in ${failed[@]+"${failed[@]}"}; do echo "  FAILED:  ${name}"; done

[[ ${#failed[@]} -eq 0 ]]
