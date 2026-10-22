---
name: gradle-ci-quality
description: Configure or review A11yOtp Gradle builds and GitHub Actions quality gates. Use for version catalogs, KMP and Android build configuration, ktlint, tests, caching, dependency verification, CI workflows, artifacts, publishing preparation, JVM targets, or toolchain upgrades.
---

# Gradle and CI quality

Keep build changes compatible with the existing KMP project and avoid unrelated upgrades.

## Current state

- A manually triggered `.github/workflows/ios-swiftlint.yml` workflow runs the pinned SwiftLint ruleset.
- `./scripts/check.sh` runs the full local check set (ktlint, host tests, Kotlin/Native compile and framework link, Android device test compile and sample, SwiftLint, iOS sample and UI test builds, optional device tests) and reports skipped steps.
- `.github/workflows/validation.yml` runs the same checks on pull requests and pushes to `main`: a `jvm` job on `ubuntu-latest` and an `ios` job on `macos-15` (Apple Silicon: `:a11yotp:iosSimulatorArm64Test`, framework link, SwiftLint, XCUITests on a simulator). The `android-device` emulator job runs only on manual dispatch. Keep the workflow and `scripts/check.sh` in sync.
- `kotlin.native.ignoreDisabledTargets=true` suppresses warnings for native targets that cannot run on the current host; it does not add compatibility or make skipped tests pass.
- Publishing uses `com.vanniktech.maven.publish` with POM metadata in `gradle.properties` (root and per module). Publications are signed only when `signingInMemoryKey` is passed. `./scripts/verify-publication.sh` publishes to Maven Local and builds `scripts/consumer-check`, a separate Gradle build that resolves `me.vprogrammers` only from Maven Local; the JVM validation job runs it.
- `.github/workflows/publish.yml` is manual only, refuses non-`main` refs and existing versions, reuses `validation.yml` (`workflow_call`, with emulator device tests), waits for the `release` environment, uploads without automatic release, and drafts the GitHub release. Its actions are pinned to commit SHAs.
- Gradle runs on JDK 21.
- `androidApp` Kotlin and Java (`sourceCompatibility`/`targetCompatibility`) targets are both JVM 21.
- The `a11yotp` and `sampleShared` Android targets use JVM 11.
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
- Before the first publication, remove unused starter dependencies, especially Lifecycle Compose and alpha Material3 artifacts, unless the public component implementation demonstrably requires them.

## Intended CI gates

1. `ktlintCheck`;
2. `:a11yotp:testAndroidHostTest` (runs `commonTest` and JVM tests);
3. `:a11yotp:compileTestKotlinIosSimulatorArm64` (Kotlin/Native test compilation);
4. `:a11yotp:connectedAndroidDeviceTest` on a managed emulator;
5. `:androidApp:assembleDebug`;
6. `:a11yotp:iosSimulatorArm64Test`, `:sampleShared:linkDebugFrameworkIosSimulatorArm64`, and the iOS app build on Apple Silicon;
7. SwiftLint through `./scripts/swiftlint.sh`;
8. publication validation: `./scripts/verify-publication.sh`.

Before the first iOS CI job, mark the `iosApp` scheme as Shared in Xcode and commit `iosApp/iosApp.xcodeproj/xcshareddata`. The scheme currently exists only in ignored `xcuserdata`.

Cache Gradle dependencies and supported build/configuration-cache state. Never skip verification with convenience flags.

## Supply chain and artifacts

- Never commit signing keys, credentials, tokens, `.env`, `local.properties`, keystores, or provisioning data.
- Use CI secret storage for future signing and publishing.
- Add dependency verification and automated dependency updates deliberately.
- Restrict retention for signed or publication artifacts.

## Verification

Run the narrowest relevant task from `CLAUDE.md`. Before a PR, run all applicable gates and report actual output. Never claim coverage, compatibility, CI, or publication success without executing it.
