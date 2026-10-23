# Accessibility

The field is one real text input with a label, a value and a state. The boxes are decoration and are hidden from screen readers. This page lists what users hear, which WCAG 2.2 criteria the component covers, what has been verified, and what you should check in your own app.

## What screen readers announce

| Moment | Unmasked | Masked |
|---|---|---|
| Focus | *"Verification code, text field, 482, 3 of 6 digits entered"* | *"Verification code, text field, 3 of 6 digits entered"* |
| Typing | The typed digit, using the platform's normal key echo | Progress only |
| Wrong code (`withError()`) | `errorMessage`, announced once without moving focus | Same |
| Rejected paste or autofill | `inputRejectedMessage`, announced without moving focus; never the digits | Same |

The exact wording and order depend on the platform screen reader. `label`, `progressDescription` and the messages are your text.

- **On iOS** the progress and any error or rejection text are part of the field's value, so VoiceOver users can re-read them.
- **On Android** they are the field's state description, and the error is its semantics error.

The library sets no accessibility hint, because the progress and the error are already part of the value.

## WCAG 2.2 coverage

| Requirement | How the component meets it | WCAG |
|---|---|---|
| One labelled input | Single text input, decorative cells hidden from assistive technology | 1.3.1, 4.1.2 |
| Errors not by color alone | Error color plus a thicker border and the error text; on iOS also an error icon | 1.4.1 |
| Contrast | Default colors ≥ 4.5:1 for text and ≥ 3:1 for borders and focus, in light and dark; your colors are checked and a warning is logged; the iOS Done key is darkened to ≥ 4.5:1 | 1.4.3, 1.4.11 |
| Text size | No fixed heights; cells and error text grow with font scale and Dynamic Type | 1.4.4 |
| Motion | The only animation, the focused cell lifting, is skipped under Reduce Motion / Remove animations | 2.3.3 |
| Keyboard | Standard text input; the keyboard dismisses with Done, Return or Escape (iOS hardware keyboard) | 2.1.1 |
| Voice Control | Visible label and accessible name match; the countdown text starts with the button label | 2.5.3 |
| Target size | The whole row is the tap target, at least 48 dp / 44 pt | 2.5.8 |
| Error identification | The error is shown in text and announced | 3.3.1 |
| Accessible authentication | Paste and autofill are always allowed | 3.3.8 |

The field vibrates once when it enters the error state (`hapticFeedbackOnError`), as an addition to the text, never instead of it.

## What is verified

**Automatically, on every change:**

- **Android:** the Accessibility Test Framework checks every field state and both buttons, in the light and dark theme and at 200% font scale. It covers contrast, touch target size, labels and more.
- **iOS:** Apple's accessibility audit runs in the UI tests: with the keyboard open, and in the error state with it dismissed, also in dark mode and at the largest accessibility text size.

**On real devices:**

- TalkBack on Android 17 and VoiceOver on iOS 26:
  - the field is one element with its label and progress;
  - errors and rejections are announced;
  - in masked mode the digits are not read;
  - the iOS Done key and the two-finger scrub work.
- SMS autofill on both platforms, including codes of the wrong length.

**Not verified on devices yet:** Voice Control, Switch Access and Switch Control, a hardware keyboard on iOS (Escape cannot be automated), right-to-left locales, and iPad keyboard variants. Please report what you find.

## Your part

The component cannot know everything about your screen. You decide:

- **The text.** Make `label` match any visible label, and localize every message with plural rules. Never put the digits in a message.
- **Timing.** Your resend countdown must not make resending permanently unavailable; let users request more time or a new code (WCAG 2.2.1).
- **Announcements for automatic fills.** When the SMS Retriever fills the code, the library makes no announcement; see [SMS Retriever](sms-retriever.md#accessibility).
- **Success.** Show and announce the result of verification in your UI.

## Checklist for your app

Check with assistive technology on real devices before you ship:

- [ ] TalkBack and VoiceOver: focus lands on one element, and the label and progress are read
- [ ] Unmasked: the entered digits can be reviewed; masked: the digits are not read
- [ ] Wrong code: the error is announced without moving focus
- [ ] A paste with the wrong number of digits: the rejection is announced, and nothing is entered
- [ ] Largest text size: nothing is clipped
- [ ] Reduce Motion / Remove animations: the focused cell does not animate
- [ ] OTP autofill on a real device, on both platforms
- [ ] Paste `123-456` from the clipboard
- [ ] Right-to-left locale: the digits stay in order
- [ ] iOS: Done, Return and Escape (hardware keyboard), and the two-finger scrub dismiss the keyboard

## App Store Accessibility Nutrition Labels

Labels are declared for your **whole app**, not per component. This component is designed not to block any label, but your other screens must also meet them. Only declare what your app actually meets.
