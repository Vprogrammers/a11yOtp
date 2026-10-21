# A11yOtp project instructions

A11yOtp is an accessibility-first OTP input library for Android and iOS.

## Architecture

Read `.claude/skills/kmp-library-engineering/SKILL.md` for the authoritative module boundaries, current distribution status, and public API rules. Do not duplicate those boundaries elsewhere.

## Project facts

- Kotlin package root: `me.vprogrammers.a11yotp`
- Shared production code: `sharedLogic/src/commonMain/kotlin/me/vprogrammers/a11yotp`
- Android Compose production code: `sharedUI/src/commonMain/kotlin/me/vprogrammers/a11yotp`
- Shared tests: `sharedLogic/src/commonTest`
- Android UI common tests: `sharedUI/src/commonTest`
- Android host tests: `sharedLogic/src/androidHostTest` and future `sharedUI/src/androidHostTest`
- Android device tests: future `sharedUI/src/androidDeviceTest`, run with `:sharedUI:connectedAndroidDeviceTest` on an emulator or device
- Kotlin/Native tests: `sharedLogic/src/iosTest`
- Native SwiftUI app: `iosApp/iosApp`
- Xcode project and scheme: `iosApp/iosApp.xcodeproj`, scheme `iosApp`
- Kotlin framework: `SharedLogic`, built by Xcode through `:sharedLogic:embedAndSignAppleFrameworkForXcode`
- iOS deployment target: 18.2
- No Xcode test target, publication configuration, or GitHub Actions workflow exists yet.
- Gradle uses JDK 21. Android bytecode targets are currently mixed; see `gradle-ci-quality/SKILL.md`.

## Project skills

Read only the relevant skill:

- `.claude/skills/otp-accessibility/SKILL.md`: screen readers, focus, announcements, privacy, autofill, paste, keyboards, RTL, and visual accessibility.
- `.claude/skills/otp-testing/SKILL.md`: shared, Android host, Android device, Kotlin/Native, and future native iOS tests.
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

Run the narrowest applicable command first:

```bash
./gradlew :sharedLogic:testAndroidHostTest
./gradlew :sharedUI:testAndroidHostTest
./gradlew :sharedLogic:iosSimulatorArm64Test
./gradlew :sharedLogic:linkDebugFrameworkIosSimulatorArm64
./gradlew :sharedLogic:allTests
./gradlew :sharedUI:allTests
./gradlew :sharedUI:connectedAndroidDeviceTest   # needs emulator or device
./gradlew :androidApp:assembleDebug
./gradlew ktlintCheck
./scripts/swiftlint.sh   # pinned SwiftLint; configured errors fail
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

Xcode invokes `:sharedLogic:embedAndSignAppleFrameworkForXcode` through its `Compile Kotlin Framework` phase. Do not invoke it manually without the Xcode environment variables it requires.

The current Intel macOS host cannot execute ARM64 simulator tests. Report that limitation and use Apple Silicon CI; do not add deprecated `iosX64`.

## Working rules

- Read the relevant module and skill before editing.
- Delegate focused work to the matching subagent. Handle Gradle/CI and documentation changes in the main session using the matching skill.
- Typical flow:
  1. `kmp-architect` when public APIs, module structure, or dependencies change;
  2. `android-compose-a11y` and/or `ios-swiftui-a11y` to implement;
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
- Run `./scripts/swiftlint.sh` after native Swift or SwiftUI changes.
- Do not merge, publish, or create releases unless explicitly requested.
- Reference the issue in the PR and use `Closes #<issue>` when appropriate.
