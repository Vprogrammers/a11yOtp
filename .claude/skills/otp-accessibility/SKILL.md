---
name: otp-accessibility
description: Design, implement, or review accessibility behavior for the A11yOtp Android Compose and native SwiftUI OTP components. Use for screen-reader semantics, focus, announcements, privacy, autofill, paste, keyboard input, RTL, large text, reduced motion, contrast, target size, and accessibility regression work.
---

# OTP accessibility

Treat accessibility behavior as part of the public component contract.

## Core model

- Render multiple digit cells visually, but expose one logical editable field to TalkBack and VoiceOver.
- Keep decorative cells out of the accessibility tree.
- Give the logical field a localized label, expected code length, current progress, and error state.
- When the visual code is unmasked, let screen-reader users review the entered digits. When masking is enabled, expose masked progress instead of the digits. Keep visual and assistive-technology privacy behavior consistent.
- Keep accessibility focus stable while input changes.
- Expose ordinary progress through the field value or state description, such as “3 of 6 digits entered”. Do not force an announcement after every keystroke.
- Use forced announcements only for state transitions that require attention, primarily errors and optionally completion.
- Never move focus merely to announce an error.

## Input behavior

Verify:

- typing, backspace, selection replacement, complete-code paste, partial paste, and overlong paste;
- normalization and rejection of unsupported characters;
- visual digit order remains left-to-right inside the OTP field even when the surrounding locale is RTL;
- software numeric keyboard and hardware keyboard input;
- Android keyboard type: `KeyboardType.Number` when unmasked and `KeyboardType.NumberPassword` only when masked, because password input types suppress TalkBack key echo unless the user enables "Speak passwords";
- Android OTP autofill behavior, including API 24–25 fallback because the Autofill Framework starts at API 26;
- SwiftUI one-time-code autofill with `.textContentType(.oneTimeCode)`;
- Voice Control visible labels match the accessible name;
- restoration without duplicate callbacks or announcements;
- configurable 4, 6, and, once implemented, 8 digit codes.

Avoid one editable field per digit. It produces fragile focus, paste, autofill, and screen-reader behavior.

## Android Compose

- Build semantics from the single logical input.
- Merge or clear decorative descendant semantics deliberately.
- Expose errors with `semantics { error(message) }` on the logical input and a polite live region (`liveRegion = LiveRegionMode.Polite`) on the visible error text. Do not use `View.announceForAccessibility`, which is deprecated from API 36.
- Verify on a device that error semantics and the polite live region produce one useful announcement per transition into the error state. Do not add a separate forced announcement when the live region already announces the message.
- Prefer semantics matchers in tests; use test tags only when semantics cannot identify the node cleanly.
- Verify TalkBack, Switch Access, hardware keyboard navigation, software keyboard behavior, and autofill on a device or emulator when host tests are insufficient.

## Native SwiftUI

- Hide decorative digit cells from the accessibility tree and expose the backing input as the single logical editable accessibility element.
- Provide an intentional label, value/state, useful hint, and invalid-state feedback.
- Announce errors with `AccessibilityNotification.Announcement(message).post()` only on the transition into the error state, without moving focus.
- Use `.keyboardType(.numberPad)` for numeric input and provide an explicit completion/dismissal affordance because number pad has no Return key.
- Verify VoiceOver, Voice Control, hardware keyboard, Dynamic Type, and one-time-code autofill on a simulator or device.

## Visual accessibility

- Support large text without clipping or hiding state.
- Respect Reduce Motion and avoid animation as the only state signal.
- Never use color alone for focus, error, completion, or disabled state.
- Preserve sufficient contrast and an effective touch target of at least 44x44 points on iOS and 48x48 dp on Android.

## Review output

Separate findings into:

1. defects proven from code or tests;
2. risks requiring platform verification;
3. claims that lack evidence.

Map WCAG 2.2 only where direct. Prioritize actual assistive-technology behavior over checklist compliance.
