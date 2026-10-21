---
name: otp-testing
description: Create or review tests for A11yOtp shared logic, Android Compose UI, Kotlin/Native integration, and accessibility behavior. Use when implementing behavior, fixing regressions, changing public APIs, preparing a PR, or working on unit, semantics, UI, state-restoration, autofill, or paste tests.
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
./gradlew ktlintCheck
./gradlew test
```

The ARM64 iOS simulator task requires Apple Silicon. On Intel macOS, report it as unavailable rather than adding `iosX64`.

## Workflow

1. Inspect the affected production code and existing test style.
2. Choose the narrowest test source set that proves the behavior.
3. Prefer deterministic fakes over mocks.
4. Run the targeted module task before broad checks.
5. Report exact commands, results, and device-only gaps.
6. Never report a test as passing unless it ran successfully.

## Shared logic matrix

Cover empty state, configurable length, typing, partial/full/overlong paste, deletion, replacement, unsupported characters, configured digit normalization, completion exactly once per transition, error clearing, and restoration. Use table-driven tests where cases share behavior.

## Android Compose

Find the logical input through semantics first. Assert that decorative cells are not independently exposed. Test label, editable/error state, paste, normalization, completion, and restoration. Use `testTag` only when semantics cannot identify the node cleanly.

Use screenshots only for visual regressions such as font scale, themes, RTL, focus, and error presentation—not interaction behavior.

## Native iOS

Until an Xcode test target exists, test shared interop through Kotlin/Native tests and document VoiceOver, Voice Control, paste, focus, and one-time-code autofill as manual simulator/device checks.

Exercise 4, 6, and 8 digit modes, masked/unmasked presentation, RTL, large text, error state, restored state, and repeated completion attempts.

## Sources

https://github.com/android/skills/blob/main/testing/testing-setup/SKILL.md
