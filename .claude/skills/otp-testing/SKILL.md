---
name: otp-testing
description: Create or review tests for A11yOtp shared logic, Android Compose UI, SwiftUI integration, and accessibility behavior. Use for unit, semantics, UI, screenshot, state-restoration, autofill, paste, and regression testing or when defining CI test coverage.
---

# OTP testing

Test the component contract, not implementation details.

## Workflow

1. Inspect the affected module and its current test stack.
2. Choose the narrowest test level that can prove the behavior.
3. Prefer deterministic fakes over mocks. Add a mocking library only when a fake is impractical.
4. Run targeted tests before broad project checks.
5. Report the exact commands, results, and behavior that still requires manual device verification.
6. Never report a test as passing unless it ran successfully.

## Shared logic matrix

Cover:

- empty state and configurable code length;
- single-character input and full completion;
- partial, full, and overlong paste;
- deletion, replacement, and clearing;
- unsupported characters and configured digit normalization;
- completion callback exactly once per completion transition;
- error clearing and state restoration;
- no platform or UI types in shared tests.

Use table-driven tests where cases share the same behavior.

## Android Compose

- Find the logical input through semantics first.
- Assert that decorative digit cells are not independently exposed.
- Test label, editable state, error state, paste, input normalization, completion, and state restoration.
- Use `testTag` only when a clear semantics matcher would be unreasonably complex.
- Add screenshot tests only for visual regressions such as font scale, themes, RTL, focus, and error presentation. Do not use screenshots to prove interaction behavior.

## Native SwiftUI

Test shared interop and state transitions independently of rendering. Add XCUITest coverage for VoiceOver-facing identifiers, paste, focus, and one-time-code integration where practical. Treat real VoiceOver output and system autofill as manual/device checks unless an automated test proves them.

## Required configurations

Exercise 4, 6, and 8 digit modes, masked and unmasked presentation, RTL, large text, error state, restored state, and repeated completion attempts.

Do not add a universal coverage percentage. If coverage is measured, report the actual tool, command, and result.

## Sources

Methodology adapted from Google's Android testing setup guidance:
https://github.com/android/skills/blob/main/testing/testing-setup/SKILL.md
