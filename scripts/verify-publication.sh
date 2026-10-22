#!/usr/bin/env bash
# Publishes both library modules to Maven Local and builds a clean consumer against them.
#
#   ./scripts/verify-publication.sh
#
# The consumer (scripts/consumer-check) is a separate Gradle build that resolves
# me.vprogrammers artifacts only from Maven Local: Android always, iOS framework on macOS.
set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPOSITORY_ROOT}"

version="$(sed -n 's/^VERSION_NAME=//p' gradle.properties)"
[[ -n "${version}" ]] || {
    echo "error: VERSION_NAME is missing from gradle.properties" >&2
    exit 1
}

if [[ -z "${ANDROID_HOME:-}" && -d "${HOME}/Library/Android/sdk" ]]; then
    export ANDROID_HOME="${HOME}/Library/Android/sdk"
fi

echo "==> Publishing ${version} to Maven Local"
./gradlew publishToMavenLocal

tasks=(":compileAndroidMain")
if [[ "$(uname)" == "Darwin" ]]; then
    tasks+=(":compileKotlinIosArm64" ":linkDebugFrameworkIosSimulatorArm64")
else
    echo "==> iOS consumer checks skipped (needs macOS)"
fi

echo "==> Building the consumer against ${version}: ${tasks[*]}"
./gradlew -p scripts/consumer-check "-Pa11yOtpVersion=${version}" "${tasks[@]}"

echo "==> Consumer resolved me.vprogrammers ${version} from Maven Local"
