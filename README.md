# a11y-otp

**An accessibility-first one-time code field for Android and iOS.**
One real text field, drawn as boxes. Native on each platform: Jetpack Compose on Android, SwiftUI on iOS, with OTP state and input normalization shared in Kotlin Multiplatform.

[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS-lightgrey)](#platforms)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

> **Status: pre-release.** The library is under active development and is not published yet. This README describes the design target. Behavior marked as a goal is verified on real devices before it is claimed as supported.

---

## Why

Most OTP components render **one text field per digit**. It looks right, but it breaks the things that matter:

| | One field per digit | **a11y-otp** (one field) |
|---|---|---|
| Screen reader | "Text field, text field, text field…" six times | One field with a label, progress and the entered code |
| SMS autofill | Often fills only the first box | Fills the whole code |
| Paste | Breaks or lands in one box | Works, including `482-913` and `482 913` |
| Backspace | Fragile focus jumping | Native behavior |

The boxes in **a11y-otp** are decoration. Underneath is a single platform text input, so TalkBack, VoiceOver, autofill and paste treat it as one ordinary field.

---

## Architecture

| Module | Contents |
|---|---|
| `sharedLogic` | Platform-neutral Kotlin: OTP state, normalization, validation, completion |
| `sharedUI` | Android Jetpack Compose component and TalkBack integration |
| `iosApp` | Native SwiftUI component, VoiceOver integration and iOS sample app |
| `androidApp` | Android sample app |

iOS UI is native SwiftUI. Compose is not used on iOS, so iOS apps do not take on the Compose runtime. Only `sharedLogic` is shared, as the `SharedLogic` framework.

---

## Goals

- One logical input per platform; digit cells are hidden from assistive technology
- TalkBack and VoiceOver: label, progress, entered digits (unless masked) and announced errors
- Platform OTP autofill: `ContentType.SmsOtpCode` on Android, `.textContentType(.oneTimeCode)` on iOS
- Paste cleanup: spaces, dashes and non-ASCII digits are normalized
- Scales with Android font scale and Dynamic Type
- Respects Reduce Motion / Remove animations
- Errors are never shown by color alone
- Code digits stay left-to-right in right-to-left layouts
- No SMS permissions and no Google Play services dependency

---

## Platforms

| Platform | Minimum (current project settings) | UI |
|---|---|---|
| Android | minSdk 24 | Jetpack Compose |
| iOS | 18.2 (Xcode deployment target), arm64 device and arm64 simulator | SwiftUI |

Minimum versions are not final and may change before the first release.

---

## Installation

Not published yet. The distribution format is not decided:

- **Android:** Maven, as one artifact or separate `sharedLogic` and `sharedUI` artifacts.
- **iOS:** Swift Package Manager and/or an XCFramework containing the SwiftUI component and the `SharedLogic` framework.

Coordinates and package URLs will be added with the first release.

---

## Planned usage

The API below is the design target and may change before release.

### Android (Compose)

```kotlin
@Composable
fun VerifyScreen(viewModel: VerifyViewModel) {
    val otp = rememberOtpState(length = 6)

    OtpField(
        state = otp,
        onComplete = { code -> viewModel.verify(code) },
        label = "Verification code",
    )
}
```

### iOS (SwiftUI)

```swift
struct VerifyView: View {
    @State private var code = ""
    @State private var error: String?

    var body: some View {
        OtpField(
            code: $code,
            length: 6,
            label: "Verification code",
            error: error,
            onComplete: { code in viewModel.verify(code) }
        )
    }
}
```

### Handling a wrong code

The host app sets an error message after verification fails. The field then:

- shows error colors **and** an icon with the message below,
- announces the message to TalkBack and VoiceOver without moving focus,
- shakes, unless Reduce Motion / Remove animations is on.

Typing again clears the error.

### Masked mode

By default the entered digits are visible and readable by screen readers, so users can check what they typed. Masked mode (dots instead of digits) also hides the digits from assistive technology and announces only progress.

### Custom cells

Both platforms accept a cell slot to change the look of each box. Custom cells are always hidden from screen readers: accessibility is handled by the field, whatever you draw.

---

## Accessibility

### What screen readers hear (target behavior)

| Moment | Unmasked | Masked |
|---|---|---|
| Focus | *"Verification code, text field, 482, 3 of 6 digits entered"* | *"Verification code, text field, 3 of 6 digits entered"* |
| Typing | The typed digit, using the platform's normal key echo | Progress only |
| Error | *"Wrong code. Try again."*, announced without moving focus | Same |

The exact wording and order depend on the platform screen reader.

### Requirements

| Requirement | Approach | WCAG 2.2 |
|---|---|---|
| One labelled input | Single text input, decorative cells hidden | 1.3.1, 4.1.2 |
| Errors not by color alone | Color + icon + text | 1.4.1 |
| Contrast | Default colors ≥ 4.5:1 for text, ≥ 3:1 for cell borders | 1.4.3, 1.4.11 |
| Large text | No fixed heights; cells grow with font scale | 1.4.4 |
| Motion | No shake when Reduce Motion / Remove animations is on | 2.3.3 |
| Voice Control | Visible label matches accessible name | 2.5.3 |
| Target size | Whole row is the tap target (≥ 48dp / 44pt) | 2.5.8 |
| Error messages | Described in text and announced | 3.3.1 |
| Accessible authentication | Paste and autofill are always allowed | 3.3.8 |

These are goals. Support will be documented per platform after real-device checks with TalkBack, Switch Access, VoiceOver and Voice Control.

### App Store Accessibility Nutrition Labels

Labels are declared for your **whole app**, not per component. The goal is for this component not to block any label, but your other screens must also meet them. Only declare what your app actually meets.

---

## Autofill

| | Android | iOS |
|---|---|---|
| Mechanism | `ContentType.SmsOtpCode` | `.textContentType(.oneTimeCode)` |
| Where the code comes from | Autofill service, keyboard suggestion | Messages, Mail |
| SMS permission | Not used | Not available |

> **Testing:** OTP autofill suggestions need a real device. They do not appear in the iOS simulator.

> **Google Play:** this library never uses `READ_SMS` or `RECEIVE_SMS`. Those permissions are heavily restricted on Google Play.

---

## What this library does not do

By design, it stays a UI component:

- No network calls or code verification
- No resend timer
- No SMS reading (no SMS Retriever, no permissions)
- No rate limiting. Your server must enforce attempt limits.

---

## Testing your integration

Before you ship, check the field with assistive technology on real devices:

- [ ] TalkBack and VoiceOver: focus lands on one element, and the label and progress are read
- [ ] Unmasked: entered digits can be reviewed; masked: digits are not read
- [ ] Wrong code: the error is announced without moving focus
- [ ] Largest text size: nothing is clipped
- [ ] Reduce Motion / Remove animations: no shake
- [ ] OTP autofill on a real device, both platforms
- [ ] Paste `123-456` from the clipboard
- [ ] Right-to-left locale: digits stay in order

---

## Development

Requirements: JDK 21, Android SDK, and Xcode for iOS.

```bash
./gradlew ktlintCheck                       # formatting
./gradlew :sharedLogic:testAndroidHostTest  # shared logic tests on the JVM
./gradlew test                              # all JVM tests
./gradlew :androidApp:assembleDebug         # Android sample app
```

Open `iosApp/iosApp.xcodeproj` in Xcode to run the iOS sample. The build phase runs `:sharedLogic:embedAndSignAppleFrameworkForXcode`.

> **Intel Macs** cannot run the ARM64 iOS simulator tests (`:sharedLogic:iosSimulatorArm64Test`). Run them on Apple Silicon. The deprecated `iosX64` target is intentionally not added.

---

## Roadmap

- [ ] Shared OTP state and normalization
- [ ] Android Compose component
- [ ] Native SwiftUI component
- [ ] Masked mode
- [ ] Alphanumeric codes
- [ ] Android and iOS distribution
- [ ] Optional SMS User Consent module for Android

---

## Contributing

Issues and pull requests are welcome. For accessibility bugs, please include:
- platform and OS version,
- assistive technology and its version,
- what was announced vs. what you expected.

---

## License

MIT. See [LICENSE](LICENSE).
