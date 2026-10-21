---
name: gradle-ci-quality
description: Configure or review A11yOtp Gradle builds and GitHub Actions quality gates. Use for version catalogs, KMP and Android build configuration, ktlint, tests, caching, dependency verification, CI workflows, artifacts, publishing preparation, JVM targets, or toolchain upgrades.
---

# Gradle and CI quality

Keep build changes compatible with the existing KMP project and avoid unrelated upgrades.

## Current state

- No `.github/workflows` directory exists yet.
- The first CI workflow should gate ktlint, shared logic tests, Android host/Compose tests, Android sample build, and Apple Silicon iOS framework/app validation.
- `kotlin.native.ignoreDisabledTargets=true` suppresses warnings for native targets that cannot run on the current host; it does not add compatibility or make skipped tests pass.
- Gradle runs on JDK 21.
- `androidApp` Kotlin bytecode target is JVM 21, while its Java `sourceCompatibility` and `targetCompatibility` are 11.
- `sharedLogic` and `sharedUI` Android targets use JVM 11.
- Do not silently change these targets. Align them only in a dedicated build change after verifying AGP/Kotlin compatibility and consumer requirements.

## Build rules

- Use `gradle/libs.versions.toml` as the version source.
- Use the committed Gradle wrapper.
- Keep generated `build/` sources out of ktlint and source control.
- Preserve configuration-cache compatibility.
- Never use `clean` as routine verification.
- Do not apply Android-only migration guidance blindly to KMP modules.
- Do not change minimum SDKs, Apple targets, publishing coordinates, or public API compatibility without explaining impact.
- Do not reintroduce deprecated `iosX64` to suppress Intel-host warnings.

## Intended CI gates

1. `ktlintCheck`;
2. `:sharedLogic:testAndroidHostTest`;
3. `:sharedUI:testAndroidHostTest` (runs `commonTest` now; Compose semantics tests join once their dependency exists);
4. `:androidApp:assembleDebug`;
5. `:sharedLogic:iosSimulatorArm64Test` and iOS app build on Apple Silicon;
6. publication validation once publishing exists.

Before the first iOS CI job, mark the `iosApp` scheme as Shared in Xcode and commit `iosApp/iosApp.xcodeproj/xcshareddata`. The scheme currently exists only in ignored `xcuserdata`.

Cache Gradle dependencies and supported build/configuration-cache state. Never skip verification with convenience flags.

## Supply chain and artifacts

- Never commit signing keys, credentials, tokens, `.env`, `local.properties`, keystores, or provisioning data.
- Use CI secret storage for future signing and publishing.
- Add dependency verification and automated dependency updates deliberately.
- Restrict retention for signed or publication artifacts.

## Verification

Run the narrowest relevant task from `CLAUDE.md`. Before a PR, run all applicable gates and report actual output. Never claim coverage, compatibility, CI, or publication success without executing it.
