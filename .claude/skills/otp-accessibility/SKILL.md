---
name: otp-accessibility
description: Design, implement, or review accessibility behavior for the A11yOtp Android Compose and iOS UIKit (Kotlin/Native) OTP components. Use for screen-reader semantics, focus, announcements, privacy, autofill, paste, keyboard input, RTL, large text, reduced motion, contrast, target size, and accessibility regression work.
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

- typing, backspace, selection replacement, exact-length paste, partial paste, and overlong paste. A single input inserting several digits is accepted only with exactly the field's digit count (it replaces the input); any other count is rejected without changing state, reported to the host as `OtpInputRejection` (counts only, never digits), and never truncated. On Android the field shows the host-localized `inputRejectedMessage` in the error-message slot in a polite live region, announced on every rejection (the node is re-created, so an identical repeat is announced again, never per keystroke), with no focus move, no error styling or haptic of its own, and no `onStateChange` when the state is unchanged. While `state.isError` the newest message wins: the rejection replaces the error text but the error styling and semantics stay, and the error text returns when the rejection clears (next edit or state change). A blank host message shows nothing but still reports the rejection. Extra single digits typed into a full field are ignored silently;
- normalization and rejection of unsupported characters;
- visual digit order remains left-to-right inside the OTP field even when the surrounding locale is RTL;
- software numeric keyboard and hardware keyboard input;
- Android keyboard type: `KeyboardType.Number` when unmasked and `KeyboardType.NumberPassword` only when masked, because password input types suppress TalkBack key echo unless the user enables "Speak passwords";
- Android OTP autofill behavior; on API 24–25, where the Autofill Framework is unavailable, degrade gracefully to typing, paste, and keyboard-provided suggestions. Do not add SMS permissions or Play Services as an implicit fallback;
- iOS one-time-code autofill with `textContentType = UITextContentTypeOneTimeCode`;
- the optional Android SMS Retriever (`a11yotp-sms-retriever`) hands the whole code to the host, which replaces the input with `state.cleared().withInput(code)` so completion is reported even over a full typed code. The field's progress semantics apply and the library forces no announcement; the host announces the fill or verification result (polite live region), because TalkBack may not speak a value change on an unfocused field. If a message has a plausible code (4 to 8 digits) of another length, `SmsRetrieverFailure.CODE_LENGTH_MISMATCH` is reported at once. The message and code are never logged;
- Voice Control visible labels match the accessible name;
- restoration without duplicate callbacks or announcements;
- configurable 4, 6, and, once implemented, 8 digit codes.

Avoid one editable field per digit. It produces fragile focus, paste, autofill, and screen-reader behavior.

## Android Compose

- Build semantics from the single logical input.
- Merge or clear decorative descendant semantics deliberately.
- Declare OTP autofill with `semantics { contentType = ContentType.SmsOtpCode }` on the logical input.
- Expose errors with `semantics { error(message) }` on the logical input and a polite live region (`liveRegion = LiveRegionMode.Polite`) on the visible error text. Do not use `View.announceForAccessibility`, which is deprecated from API 36.
- Verify on a device that error semantics and the polite live region produce one useful announcement per transition into the error state. Do not add a separate forced announcement when the live region already announces the message.
- Prefer semantics matchers in tests; use test tags only when semantics cannot identify the node cleanly.
- Verify TalkBack, Switch Access, hardware keyboard navigation, software keyboard behavior, and autofill on a device or emulator when host tests are insufficient.
- Default colors follow the system theme: a style with `colors = null` resolves to `OtpFieldDefaults.systemColors()` / `OtpButtonDefaults.systemColors()` (`isSystemInDarkTheme`). Dark focus is a white border, because blue would not contrast 3:1 with the gray cell border. Keep both palettes free of contrast warnings.
- TalkBack draws its focus outline on the focusable node's bounds and ignores shapes, so the field and buttons keep `OtpFieldDefaults.FocusOutlineGap` between their bounds and what they draw.

## iOS UIKit (Kotlin/Native)

The iOS components are UIKit views written in Kotlin in `a11yotp/src/iosMain`. Hosts embed them in SwiftUI through `UIViewRepresentable`.

- Use one real `UITextField` as the single logical editable accessibility element. Set `isAccessibilityElement = false` on the container and the decorative digit cells.
- Give the text field the frame of the visible cell row plus a small gap (more at the top for the lifted focused cell), so the VoiceOver focus outline surrounds the cells instead of cutting through them, and Voice Control and touch match the visible field. Do not shrink it to a 1-point frame or hide it; hidden views cannot become first responder.
- Provide an intentional `accessibilityLabel`, `accessibilityValue` (progress or masked progress), useful hint, and invalid-state feedback.
- Announce errors with `UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, message)` only on the transition into the error state, without moving focus.
- A rejected multi-digit input shows `inputRejectedMessage` in the error-text slot (neutral text, no icon), announces it once after 300 ms (generation-guarded, no focus move), and appends it to `accessibilityValue` so it can be re-read; it never contains digits. Any following input, including no-op input (ignored digit, unsupported character), and any state change clears it. While `state.isError` the newest message wins in label and value and the error text returns when it clears; a rejection cancels a still-pending error announcement (later error transitions announce again). A blank host message shows and announces nothing.
- iOS one-time-code autofill inserts the code one character at a time within one run-loop turn (after two empty replacements of an empty range). Recognize it by the empty replacement of an empty range sent first (the signature; it is no change, not Backspace) and apply that turn's insertions as one input so the length rule rejects a mismatched code instead of filling some cells or cutting it. Without the signature, apply every replacement individually in order: typed keys (hardware keyboard, Voice Control) must never be merged or rejected.
- Use `keyboardType = UIKeyboardTypeNumberPad`. The number pad has no Return key; the text field's `inputAccessoryView` is a transparent view with one system prominent `UIButton` (title `doneButtonTitle`) shaped like a number pad key and placed above the "3" key inside the left/right safe area (on iPad it keeps the iPhone key width, right-aligned); metrics measured on iOS 26, an approximation. Only the button takes touches so the one-time-code suggestion and content behind stay tappable. The accessory adds key height + 12 pt (about 60 pt) above the keyboard (row spacing; transparent). The fill is the host tint (dark mode, Increase Contrast) darkened until the white title reaches 4.5:1 (7:1 with Increase Contrast). The two-finger scrub (`accessibilityPerformEscape`) and the hardware Escape key (`UIKeyCommand` with `wantsPriorityOverSystemBehavior`) on the field also dismiss the keyboard. XCUITest cannot send a hardware Escape on iOS, so verify it with a hardware keyboard.
- Use dynamic `UIColor`s so dark mode and Increase Contrast work, `UIFontMetrics` for Dynamic Type, and `UIAccessibilityIsReduceMotionEnabled()` for motion.
- Verify VoiceOver, Voice Control, hardware keyboard, Dynamic Type, and one-time-code autofill on a device; an Intel Mac cannot run the ARM64 simulator.

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
