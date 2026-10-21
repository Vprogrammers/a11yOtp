#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly CONFIG_FILE="${REPOSITORY_ROOT}/.swiftlint.yml"
readonly MINT_FILE="${REPOSITORY_ROOT}/Mintfile"
readonly SWIFTLINT_PACKAGE="$(awk -F'@' '$1 == "realm/SwiftLint" { print; exit }' "${MINT_FILE}")"

if [[ -z "${SWIFTLINT_PACKAGE}" || "${SWIFTLINT_PACKAGE}" != *@* ]]; then
    echo "error: Mintfile must pin realm/SwiftLint with realm/SwiftLint@<version>." >&2
    exit 1
fi

readonly SWIFTLINT_VERSION="${SWIFTLINT_PACKAGE##*@}"

cd "${REPOSITORY_ROOT}"

if command -v mint >/dev/null 2>&1; then
    exec mint run "${SWIFTLINT_PACKAGE}" swiftlint lint \
        --config "${CONFIG_FILE}" \
        "$@"
fi

if command -v swiftlint >/dev/null 2>&1; then
    installed_version="$(swiftlint version)"
    if [[ "${installed_version}" != "${SWIFTLINT_VERSION}" ]]; then
        echo "error: SwiftLint ${SWIFTLINT_VERSION} is required, but ${installed_version} is installed." >&2
        echo "Install Mint with: brew install mint" >&2
        exit 1
    fi

    exec swiftlint lint --config "${CONFIG_FILE}" "$@"
fi

echo "error: SwiftLint is unavailable. Install Mint with: brew install mint" >&2
exit 1
