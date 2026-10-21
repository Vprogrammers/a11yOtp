# A11yOtp project instructions

A11yOtp is an accessibility-first OTP input library for Android and iOS.

## Architecture

- `sharedLogic`: platform-neutral OTP state, normalization, validation, and shared tests.
- `sharedUI`: Android Jetpack Compose OTP component and Android accessibility behavior.
- `androidApp`: Android sample application.
- `iosApp`: native SwiftUI OTP component and iOS sample application.
- Do not introduce Compose Multiplatform UI on iOS.
- Keep public APIs small and framework-independent where possible.

## Project skills

Read the smallest relevant skill before starting work:

- `.claude/skills/otp-accessibility/SKILL.md`: screen-reader behavior, focus, announcements, privacy, autofill, paste, and visual accessibility.
- `.claude/skills/otp-testing/SKILL.md`: shared, Compose, SwiftUI, accessibility, and regression tests.
- `.claude/skills/kmp-library-engineering/SKILL.md`: module boundaries, public API design, Swift interop, dependencies, and KMP changes.
- `.claude/skills/gradle-ci-quality/SKILL.md`: Gradle, ktlint, CI gates, caches, toolchain, artifacts, and publishing preparation.

Do not load every skill by default. Use only those relevant to the task.

## Accessibility requirements

Accessibility is a release requirement, not an optional enhancement.

- Expose the OTP input as one logical field to TalkBack and VoiceOver.
- Support paste and platform OTP autofill.
- Never expose decorative digit cells as separate accessibility elements.
- Announce errors without unnecessarily moving accessibility focus.
- Never communicate state through color alone.
- Respect large text, reduced motion, sufficient contrast, and minimum target sizes.
- Verify behavior on real devices when platform APIs cannot be validated in unit tests.

## Working rules

- Read the relevant module before editing.
- Delegate focused work to the matching project subagent.
- Keep generated files under `build/` untouched.
- Never read or commit `local.properties`, credentials, signing material, `.env` files, or local Claude settings.
- Do not change minimum platform versions, toolchain versions, publishing coordinates, or public API compatibility without explaining the tradeoff.
- Prefer the smallest implementation that provides real application value.
- Add tests for shared state transitions, normalization, completion, and regressions.
- Do not add tests that only duplicate implementation details.
- Run the narrowest relevant checks first, then the full checks before a PR.

## Validation

Run as applicable:

```bash
./gradlew ktlintCheck
./gradlew test
./gradlew :androidApp:assembleDebug
```

The current Intel macOS host cannot execute ARM64 iOS simulator tests. Report that limitation clearly; do not add deprecated `iosX64` targets to silence it.

## Git workflow

- Work from an issue branch such as `feat/<issue>-description`, `fix/<issue>-description`, or `chore/<issue>-description`.
- Keep commits focused.
- Do not push, force-push, merge, publish, or create releases unless explicitly requested.
- Reference the issue in the PR description and use `Closes #<issue>` when appropriate.
