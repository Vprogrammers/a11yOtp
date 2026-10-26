---
name: otp-testing
description: Create or review tests for A11yOtp shared logic, Android Compose UI, Kotlin/Native integration, and accessibility behavior. Use when implementing behavior, fixing regressions, changing public APIs, preparing a PR, or working on unit, semantics, UI, state-restoration, autofill, paste, or keyboard tests.
---

# OTP testing

Test the component contract, not implementation details.

## Locations

Use package `me.vprogrammers.a11yotp`.

- Shared tests: `a11yotp/src/commonTest/kotlin/me/vprogrammers/a11yotp`
- Android host (JVM) tests, including Compose-typed style tests: `a11yotp/src/androidHostTest/kotlin/me/vprogrammers/a11yotp`
- Android Compose device tests: `a11yotp/src/androidDeviceTest/kotlin/me/vprogrammers/a11yotp`
- Kotlin/Native tests, including non-visual UIKit component logic: `a11yotp/src/iosTest/kotlin/me/vprogrammers/a11yotp`
- SMS Retriever host tests (code extraction and result mapping): `a11yotp-sms-retriever/src/androidHostTest/kotlin/me/vprogrammers/a11yotp/smsretriever`

- iOS UI tests (XCUITest, drive the SwiftUI sample): `iosApp/iosAppUITests`, target `iosAppUITests` in the shared `iosApp` scheme. The sample accepts `-resendSeconds <n>` and `-manualVerify YES` launch arguments for deterministic tests.

## Commands

```bash
./gradlew :a11yotp:testAndroidHostTest
./gradlew :a11yotp:compileAndroidDeviceTest
./gradlew :a11yotp:connectedAndroidDeviceTest   # needs emulator or device
./gradlew :a11yotp:compileTestKotlinIosSimulatorArm64
./gradlew :a11yotp:iosSimulatorArm64Test        # Apple Silicon only
./gradlew :a11yotp:allTests
./gradlew :a11yotp-sms-retriever:testAndroidHostTest
./gradlew ktlintCheck
```

Use `./gradlew :<module>:tasks --all` when a module task name must be confirmed. ARM64 iOS simulator tests require Apple Silicon; report them as unavailable on Intel rather than adding `iosX64`.

## Current limitations

- Compose UI tests use AndroidX `ui-test-junit4` in `androidDeviceTest`. Run them with `:a11yotp:connectedAndroidDeviceTest` on an emulator or device.
- Use `androidHostTest` for non-UI JVM tests. Introduce Robolectric Compose tests only when their speed provides clear value and their platform limitations do not affect the behavior being verified.
- Instrumentation cannot send the SMS Retriever broadcast (it is protected by a Play Services signature permission). End-to-end SMS Retriever delivery is a manual check on a device with Play Services.
- `iosAppUITests` runs on a real iPhone (also from the Intel Mac) with `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'id=<device id>' -allowProvisioningUpdates`; the phone must be unlocked and `TEAM_ID` set in the git-ignored `Local.xcconfig`. Simulator runs need Apple Silicon.
- XCUITest lists decorative cells even though they are not accessibility elements, so assert one text field rather than the absence of cell elements. Paste, Messages autofill, and VoiceOver speech remain manual device checks.

## Workflow

1. Inspect affected production code, version catalog, and existing test style.
2. Choose the narrowest source set that proves the behavior.
3. Prefer deterministic fakes over mocks.
4. Run the targeted module task before broad checks.
5. Report exact commands, results, and device-only gaps.
6. Never report a test as passing unless it ran successfully.

## Test matrix

For implemented behavior, cover empty state, supported lengths, typing, partial/full/overlong paste, deletion, replacement, unsupported characters, configured normalization, completion exactly once per transition, error clearing, and restoration. Use table-driven tests where cases share behavior.

For Android Compose, find the logical input through semantics and assert decorative cells are not independently exposed. Test label, progress/error state, paste, keyboard input, normalization, completion, and restoration. Assert the keyboard type is `Number` when unmasked and `NumberPassword` only when masked, and for `OtpCharset.ALPHANUMERIC` `Ascii` with capital letters when unmasked and `Password` only when masked. For alphanumeric codes also test lowercase and fullwidth letters, rejected look-alikes (`ı`, `ſ`, Cyrillic `А`), stable cell width with a proportional font, and that the invisible width glyph never reaches semantics.

Use screenshots only for visual regressions such as font scale, themes, RTL, focus, and errors—not interaction behavior.

Test iOS component logic that does not need a window (state mapping, accessibility values, masking, contrast warnings) with Kotlin/Native tests. Document VoiceOver, Voice Control, paste, focus, keyboard, and one-time-code autofill as manual device checks.
