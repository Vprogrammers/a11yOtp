# a11y-otp

**An accessibility-first one-time code field for Android and iOS.**
One real text field, drawn as boxes. Native on each platform: Jetpack Compose on Android and UIKit on iOS. Add it once to your Kotlin Multiplatform shared module; no Swift package needed.

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
| `a11yotp` | The library. `commonMain`: OTP state, normalization, contrast checks, masks, alignment and accessibility text. `androidMain`: Jetpack Compose components. `iosMain`: UIKit components written in Kotlin/Native. |
| `sample/shared` | A consumer-style KMP shared module that adds `a11yotp` once and exports it to iOS |
| `androidApp` | Android sample app |
| `iosApp` | SwiftUI sample app that embeds the UIKit components |

Both platforms use native UI and native accessibility APIs. Compose is not used on iOS, so iOS apps do not take on the Compose runtime. Colors and fonts are platform-native (Compose `Color`, `UIColor`), so dark mode and Increase Contrast work as usual.

Why UIKit and not SwiftUI on iOS: the library is delivered through Gradle, and a Kotlin dependency cannot carry Swift source. Kotlin/Native can build UIKit views, and SwiftUI apps embed them with a short `UIViewRepresentable` wrapper (see below).

---

## Goals

- One logical input per platform; digit cells are hidden from assistive technology
- TalkBack and VoiceOver: label, progress, reviewable entered digits (unless masked) and announced errors; the exact speech is verified per platform on devices
- Platform OTP autofill: `ContentType.SmsOtpCode` on Android, `oneTimeCode` text content type on iOS
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
| iOS | 18.2 (Xcode deployment target), arm64 device and arm64 simulator | UIKit (embeddable in SwiftUI) |

Minimum versions are not final and may change before the first release.

---

## Installation

Not published yet. The library will ship as one Maven artifact that you add only to your KMP shared module. Coordinates will be added with the first release.

```kotlin
// shared/build.gradle.kts
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            export("<coordinates>:a11yotp:<version>") // makes the iOS components visible to Swift
        }
    }
    sourceSets.commonMain.dependencies {
        api("<coordinates>:a11yotp:<version>") // api, so androidApp sees the Compose components
    }
}
```

`sample/shared/build.gradle.kts` is a working example that uses a project dependency.

---

## Usage

The API may change before the first release. The host app owns the state and verifies the code with its server. All text is supplied by the host, so it can be localized.

### Android (Compose)

```kotlin
var state by remember { mutableStateOf(OtpState(OtpLength.SIX)) }

OtpField(
    state = state,
    onStateChange = { state = it },
    onComplete = { code -> viewModel.verify(code) },
    accessibility =
        OtpFieldAccessibility(
            label = "Verification code",
            errorMessage = "Incorrect code. Try again.",
            progressDescription = { "${it.entered} of ${it.total} digits entered" },
        ),
)
OtpVerifyButton(state = state, onVerify = viewModel::verify, accessibility = OtpVerifyAccessibility("Verify"))
OtpResendButton(
    secondsRemaining = seconds,
    onResend = viewModel::resend,
    accessibility = OtpResendAccessibility("Resend code") { "Resend code in ${it.secondsRemaining} s" },
)
```

### iOS (SwiftUI)

The components are UIKit classes that own a `view`. Embed them with a small wrapper; `iosApp/iosApp/Components/` contains ready-to-copy wrappers for the field and both buttons.

```swift
import Shared

let field = OtpField(
    state: OtpState(length: .six),
    accessibility: OtpFieldAccessibility(
        label: "Verification code",
        errorMessage: "Incorrect code. Try again.",
        progressDescription: { "\($0.entered) of \($0.total) digits entered" }
    ),
    doneButtonTitle: "Done"
)
field.onStateChange = { [weak field] newState in field?.state = newState }
field.onComplete = { code in verify(code) }
// Add field.view to your view hierarchy, or use OtpFieldView in SwiftUI:
// OtpFieldView(state: $otp, accessibility: accessibility, onComplete: verify)
```

Keep a strong reference to the Kotlin object (`OtpField`, `OtpVerifyButton`, `OtpResendButton`) for as long as its view is on screen. The sample wrappers keep it in their coordinator.

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
| Mechanism | `ContentType.SmsOtpCode` | `UITextContentTypeOneTimeCode` |
| Where the code comes from | Autofill service, keyboard suggestion | Messages, Mail |
| SMS permission | Not used | Not available |

> **Testing:** OTP autofill suggestions need a real device. They do not appear in the iOS simulator.

> **Google Play:** this library never uses `READ_SMS` or `RECEIVE_SMS`. Those permissions are heavily restricted on Google Play.

---

## What this library does not do

By design, it stays a UI component:

- No network calls or code verification
- No resend timer: the resend button shows the seconds you pass in
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
./gradlew ktlintCheck                              # formatting
./gradlew :a11yotp:testAndroidHostTest             # shared and Android tests on the JVM
./gradlew :a11yotp:compileTestKotlinIosSimulatorArm64  # compile Kotlin/Native tests
./gradlew :androidApp:assembleDebug                # Android sample app
```

Open `iosApp/iosApp.xcodeproj` in Xcode to run the iOS sample. The build phase runs `:sampleShared:embedAndSignAppleFrameworkForXcode`.

> **Intel Macs** cannot run the ARM64 iOS simulator tests (`:a11yotp:iosSimulatorArm64Test`). Run them on Apple Silicon. The deprecated `iosX64` target is intentionally not added.

### SwiftLint

Swift sources in the sample app are checked with the version pinned in `Mintfile`, so local and CI checks use the same ruleset.

Install [Mint](https://github.com/yonaskolb/Mint) once:

```bash
brew install mint
```

Run lint from the repository root:

```bash
./scripts/swiftlint.sh
```

The script reads the required version from `Mintfile` and installs it through Mint when needed. If Mint is unavailable, it accepts an existing `swiftlint` executable only when its version matches the pin.

Xcode integration is optional. To show violations while building locally, add a Run Script build phase containing:

```bash
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
"$SRCROOT/../scripts/swiftlint.sh"
```

For an Xcode Run Script phase, set `ENABLE_USER_SCRIPT_SANDBOXING = NO`; otherwise Xcode can prevent the script from reading project files. This integration is optional. CI does not depend on an individual developer's Xcode configuration and never modifies Swift source files automatically.


---

## Roadmap

- [x] Shared OTP state and normalization
- [x] Android Compose component
- [x] iOS UIKit component (Kotlin/Native)
- [x] Masked mode
- [ ] Real-device verification with TalkBack, VoiceOver and Voice Control
- [ ] Alphanumeric codes
- [ ] Maven publication
- [ ] Optional Compose Multiplatform UI module
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
