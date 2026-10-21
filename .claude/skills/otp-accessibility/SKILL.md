---
name: otp-accessibility
description: Design, implement, or review accessibility behavior for the A11yOtp Android Compose and native SwiftUI OTP components. Use for screen-reader semantics, focus, announcements, privacy, autofill, paste, large text, reduced motion, contrast, target size, and accessibility regression work.
---

# OTP accessibility

Treat accessibility behavior as part of the public component contract.

## Core model

- Render multiple digit cells visually, but expose one logical editable field to TalkBack and VoiceOver.
- Keep decorative cells out of the accessibility tree.
- Give the logical field a localized label, current state, expected code length, and error state.
- Do not reveal entered digits to assistive technology by default. Make any value-reading behavior explicit and documented.
- Keep accessibility focus stable while digits change.
- Announce completion or errors only when the state transitions. Never announce on every recomposition or render.
- Never move focus merely to announce an error.

## Input behavior

Verify:

- typing, backspace, selection replacement, complete-code paste, partial paste, and overlong paste;
- normalization and rejection of unsupported characters;
- Android SMS OTP autofill without SMS permissions;
- SwiftUI one-time-code autofill with `.textContentType(.oneTimeCode)`;
- state restoration without duplicate callbacks or announcements;
- correct behavior for configurable 4, 6, and 8 digit codes.

Avoid one editable field per digit. It produces fragile focus, paste, autofill, and screen-reader behavior.

## Android Compose

- Build the semantics from the single logical input.
- Merge or clear decorative descendant semantics deliberately.
- Prefer semantics matchers in tests; use test tags only when semantics cannot identify the node cleanly.
- Verify TalkBack output, Switch Access, keyboard navigation, and autofill on a device or emulator when unit tests are insufficient.

## Native SwiftUI

- Combine decorative cells into one accessibility element or hide them and expose the backing input.
- Provide an intentional label, value/state, hint only when it adds information, and invalid-state feedback.
- Verify VoiceOver, Voice Control, Dynamic Type, and one-time-code autofill on a simulator or device when available.

## Visual accessibility

- Support large text without clipping or hiding the input state.
- Respect Reduce Motion and avoid animation as the only state signal.
- Never use color alone for focus, error, completion, or disabled state.
- Preserve sufficient contrast and an effective touch target of at least 44x44 points on iOS and 48x48 dp on Android.

## Review output

Separate findings into:

1. defects proven from code or tests;
2. risks requiring platform verification;
3. claims that lack evidence.

Map WCAG 2.2 criteria only where the mapping is direct. Prioritize actual assistive-technology behavior over checklist compliance.
