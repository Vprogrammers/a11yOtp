---
name: otp-testing
description: Create or review tests for A11yOtp shared logic, Android Compose UI, Kotlin/Native integration, and accessibility behavior. Use when implementing behavior, fixing regressions, changing public APIs, preparing a PR, or working on unit, semantics, UI, state-restoration, autofill, paste, or keyboard tests.
---

# OTP testing

Test the component contract, not implementation details.

## Locations

Use package `me.vprogrammers.a11yotp`.

- Shared tests: `sharedLogic/src/commonTest/kotlin/me/vprogrammers/a11yotp`
- Android shared-logic host tests: `sharedLogic/src/androidHostTest/kotlin/me/vprogrammers/a11yotp`
- Android Compose host tests: `sharedUI/src/androidHostTest/kotlin/me/vprogrammers/a11yotp`
- Kotlin/Native tests: `sharedLogic/src/iosTest/kotlin/me/vprogrammers/a11yotp`

There is no Xcode test target yet. Do not create Swift/XCUITest files outside a configured test target or claim they ran.

## Commands

```bash
./gradlew :sharedLogic:testAndroidHostTest
./gradlew :sharedUI:testAndroidHostTest
./gradlew :sharedLogic:iosSimulatorArm64Test
./gradlew :sharedLogic:allTests
./gradlew :sharedUI:allTests
./gradlew ktlintCheck
```

Use `./gradlew :<module>:tasks --all` when a module task name must be confirmed. ARM64 iOS simulator tests require Apple Silicon; report them as unavailable on Intel rather than adding `iosX64`.

## Current limitations

- Compose UI test dependencies are not configured yet. Before writing Compose semantics tests, add the compatible Compose test dependency and choose the supported host-test API for the current AGP/KMP stack; do not assume `runComposeUiTest` or `createComposeRule` is already available.
- Masked presentation and 8-digit mode are roadmap behavior. Add their tests when those features are implemented; do not make the initial suite fail for absent roadmap features.
- There is no Xcode test target.

## Workflow

1. Inspect affected production code, version catalog, and existing test style.
2. Choose the narrowest source set that proves the behavior.
3. Prefer deterministic fakes over mocks.
4. Run the targeted module task before broad checks.
5. Report exact commands, results, and device-only gaps.
6. Never report a test as passing unless it ran successfully.

## Test matrix

For implemented behavior, cover empty state, supported lengths, typing, partial/full/overlong paste, deletion, replacement, unsupported characters, configured normalization, completion exactly once per transition, error clearing, and restoration. Use table-driven tests where cases share behavior.

For Android Compose, find the logical input through semantics and assert decorative cells are not independently exposed. Test label, progress/error state, paste, keyboard input, normalization, completion, and restoration.

Use screenshots only for visual regressions such as font scale, themes, RTL, focus, and errors—not interaction behavior.

Until an Xcode test target exists, test shared interop through Kotlin/Native tests and document VoiceOver, Voice Control, paste, focus, keyboard, and one-time-code autofill as manual checks.
