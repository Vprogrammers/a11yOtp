# A11yOtp project instructions

A11yOtp is an accessibility-first OTP input library for Android and iOS.

## Architecture

Read `.claude/skills/kmp-library-engineering/SKILL.md` for the authoritative module boundaries and public API rules. Do not duplicate those boundaries elsewhere.

## Project facts

- Kotlin package root: `me.vprogrammers.a11yotp`
- Shared production code: `sharedLogic/src/commonMain/kotlin/me/vprogrammers/a11yotp`
- Android Compose production code: `sharedUI/src/commonMain/kotlin/me/vprogrammers/a11yotp`
- Shared tests: `sharedLogic/src/commonTest`
- Android host tests: `sharedLogic/src/androidHostTest` and `sharedUI/src/androidHostTest`
- Kotlin/Native tests: `sharedLogic/src/iosTest`
- Native SwiftUI app: `iosApp/iosApp`
- Xcode project and scheme: `iosApp/iosApp.xcodeproj`, scheme `iosApp`
- Kotlin framework: `SharedLogic`, built by Xcode through `:sharedLogic:embedAndSignAppleFrameworkForXcode`
- iOS deployment target: 18.2
- There is currently no Xcode test target. Do not claim Swift/XCUITest execution until one is added.

## Project skills

Read only the relevant skill:

- `.claude/skills/otp-accessibility/SKILL.md`: screen-reader behavior, focus, announcements, privacy, autofill, paste, and visual accessibility.
- `.claude/skills/otp-testing/SKILL.md`: shared, Android host, Kotlin/Native, and future native iOS tests.
- `.claude/skills/kmp-library-engineering/SKILL.md`: authoritative module boundaries, public APIs, Swift interop, dependencies, and KMP changes.
- `.claude/skills/gradle-ci-quality/SKILL.md`: Gradle, ktlint, CI gates, caches, toolchain, artifacts, and publishing preparation.

## Accessibility requirements

Accessibility is a release requirement.

- Expose the OTP input as one logical field to TalkBack and VoiceOver.
- Support paste and platform OTP autofill.
- Keep decorative digit cells out of the accessibility tree.
- Announce errors without unnecessarily moving accessibility focus.
- Never communicate state through color alone.
- Respect large text, reduced motion, contrast, and minimum target sizes.
- Verify platform behavior on real devices when automated tests cannot prove it.

## Commands

Run the narrowest applicable command first:

```bash
./gradlew :sharedLogic:testAndroidHostTest
./gradlew :sharedUI:testAndroidHostTest
./gradlew :sharedLogic:iosSimulatorArm64Test
./gradlew :sharedLogic:linkDebugFrameworkIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew ktlintCheck
./gradlew test
```

Build the native iOS app on an Apple Silicon Mac with an installed iOS 18.2+ simulator:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Xcode invokes `:sharedLogic:embedAndSignAppleFrameworkForXcode` automatically through its `Compile Kotlin Framework` build phase. Do not invoke that task manually without the Xcode environment variables it requires.

The current Intel macOS host cannot execute ARM64 simulator tests. Report that limitation and run ARM64 Kotlin/iOS validation on Apple Silicon CI; do not add deprecated `iosX64`.

## Working rules

- Read the relevant module and skill before editing.
- Delegate focused work to the matching project subagent.
- Keep generated files under `build/` untouched.
- Never read or commit `local.properties`, credentials, signing material, `.env`, or local Claude settings.
- Do not change platform minimums, toolchain versions, publishing coordinates, or public API compatibility without explaining the impact.
- Prefer the smallest implementation that provides real application value.
- Add behavior-focused tests and run targeted checks before broad checks.

## Git workflow

- Work from `feat/<issue>-description`, `fix/<issue>-description`, or `chore/<issue>-description`.
- Keep commits focused.
- Do not push, force-push, merge, publish, or create releases unless explicitly requested.
- Reference the issue in the PR and use `Closes #<issue>` when appropriate.
