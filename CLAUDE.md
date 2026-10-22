# A11yOtp project instructions

A11yOtp is an accessibility-first OTP input library for Android and iOS.

## Architecture

Read `.claude/skills/kmp-library-engineering/SKILL.md` for the authoritative module boundaries, current distribution status, and public API rules. Do not duplicate those boundaries elsewhere.

## Project facts

- Kotlin package root: `me.vprogrammers.a11yotp`
- Library module: `a11yotp` (consumers add it only through Gradle in their KMP shared module)
- Optional Android-only module: `a11yotp-sms-retriever` (Google SMS Retriever; host tests in `a11yotp-sms-retriever/src/androidHostTest`)
- Shared production code: `a11yotp/src/commonMain/kotlin/me/vprogrammers/a11yotp`
- Android Compose production code: `a11yotp/src/androidMain/kotlin/me/vprogrammers/a11yotp`
- iOS UIKit production code (Kotlin/Native): `a11yotp/src/iosMain/kotlin/me/vprogrammers/a11yotp`
- Shared tests: `a11yotp/src/commonTest`
- Android host tests: `a11yotp/src/androidHostTest`
- Android device tests: `a11yotp/src/androidDeviceTest`, run with `:a11yotp:connectedAndroidDeviceTest` on an emulator or device
- Kotlin/Native tests: `a11yotp/src/iosTest`
- Consumer-like sample shared module: `sample/shared` (`:sampleShared`), exports `a11yotp` in the `Shared` framework
- SwiftUI sample app: `iosApp/iosApp` (embeds the Kotlin UIKit components; no library code)
- Xcode project and scheme: `iosApp/iosApp.xcodeproj`, scheme `iosApp`
- Kotlin framework: `Shared`, built by Xcode through `:sampleShared:embedAndSignAppleFrameworkForXcode`
- iOS deployment target: 18.2
- Xcode UI test target: `iosAppUITests` (`iosApp/iosAppUITests`), in the shared `iosApp` scheme. Device signing reads `TEAM_ID` from the git-ignored `iosApp/Configuration/Local.xcconfig`; never commit a team ID.
- No publication configuration exists yet. A manually triggered SwiftLint workflow exists; broader CI does not.
- Gradle uses JDK 21. Library modules target JVM 11 and `androidApp` targets JVM 21; see `gradle-ci-quality/SKILL.md`.

## Project skills

Read only the relevant skill:

- `.claude/skills/otp-accessibility/SKILL.md`: screen readers, focus, announcements, privacy, autofill, paste, keyboards, RTL, and visual accessibility.
- `.claude/skills/otp-testing/SKILL.md`: shared, Android host, Android device, and Kotlin/Native tests.
- `.claude/skills/kmp-library-engineering/SKILL.md`: authoritative module boundaries, public APIs, Swift interop, dependencies, and distribution decisions.
- `.claude/skills/gradle-ci-quality/SKILL.md`: Gradle, ktlint, CI gates, caches, JVM targets, artifacts, and publishing preparation.

## Accessibility requirements

- Expose the OTP input as one logical field to TalkBack and VoiceOver.
- Support paste and platform OTP autofill.
- Keep decorative digit cells out of the accessibility tree.
- Expose progress through field value/state; force announcements only for important state transitions.
- Keep visual and screen-reader masking behavior consistent.
- Never communicate state through color alone.
- Respect large text, reduced motion, contrast, and minimum target sizes.
- Verify platform behavior on real devices when automated tests cannot prove it.

## Commands

`./scripts/check.sh [all|kmp|android|ios] [--device [<iphone-id>]]` runs the local checks below and prints a pass/fail/skip summary; it uses no GitHub Actions. Run it before a PR.

Run the narrowest applicable command first:

```bash
./gradlew :a11yotp:testAndroidHostTest
./gradlew :a11yotp-sms-retriever:testAndroidHostTest
./gradlew :a11yotp:compileAndroidDeviceTest
./gradlew :a11yotp:compileTestKotlinIosSimulatorArm64
./gradlew :a11yotp:iosSimulatorArm64Test          # Apple Silicon only
./gradlew :sampleShared:linkDebugFrameworkIosSimulatorArm64
./gradlew :a11yotp:allTests
./gradlew :a11yotp:connectedAndroidDeviceTest     # needs emulator or device
./gradlew :androidApp:assembleDebug
./gradlew ktlintCheck
./scripts/swiftlint.sh   # pinned SwiftLint; configured errors fail
```

Run the iOS UI tests on a connected, unlocked iPhone (works from the Intel Mac):

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'id=<device id>' -allowProvisioningUpdates
```

Confirm an uncertain module task with:

```bash
./gradlew :<module>:tasks --all
```

Build the native iOS app on Apple Silicon with an installed iOS 18.2+ simulator:

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

Xcode invokes `:sampleShared:embedAndSignAppleFrameworkForXcode` through its `Compile Kotlin Framework` phase. Do not invoke it manually without the Xcode environment variables it requires.

The current Intel macOS host cannot execute ARM64 simulator tests. Report that limitation and use Apple Silicon CI; do not add deprecated `iosX64`.

## Working rules

- Read the relevant module and skill before editing.
- Delegate focused work to the matching subagent. Handle Gradle/CI and documentation changes in the main session using the matching skill.
- Typical flow:
  1. `kmp-architect` when public APIs, module structure, or dependencies change;
  2. `android-compose-a11y` and/or `ios-swiftui-a11y` (iOS UIKit in Kotlin/Native) to implement;
  3. `test-engineer` to add and run tests;
  4. `accessibility-reviewer` when UI, semantics, or accessibility claims change;
  5. full checks before the PR.
- Platform implementation agents may add focused tests for their own changes. `test-engineer` reviews coverage, adds missing regression and edge-case tests, and runs the applicable verification tasks.
- Keep generated files under `build/` untouched.
- Never read or commit `local.properties`, credentials, signing material, `.env`, keystores, or local Claude settings.
- Do not change platform minimums, toolchain versions, publishing coordinates, distribution format, or public API compatibility without explaining impact.
- Prefer the smallest implementation that provides real application value.
- Add behavior-focused tests and run targeted checks before broad checks.

## Git workflow

- Work from `feat/<issue>-description`, `fix/<issue>-description`, or `chore/<issue>-description`.
- Keep commits focused.
- Claude never pushes or force-pushes; `git push` is denied in `.claude/settings.json`. When a push is requested, ask the user to run the exact command with `!`.
- Run `./scripts/swiftlint.sh` after Swift changes in `iosApp`.
- Do not merge, publish, or create releases unless explicitly requested.
- Reference the issue in the PR and use `Closes #<issue>` when appropriate.
