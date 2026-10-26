# a11y-otp

**An accessibility-first one-time code field for Android and iOS.**
One real text field, drawn as boxes. Native on each platform: Jetpack Compose on Android and UIKit on iOS. Add it once to your Kotlin Multiplatform shared module; no Swift package needed.

[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS-lightgrey)](#requirements)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

> **Status: 0.2.0.** While the version is 0.x, minor versions may still change the API; every change is listed in the [changelog](CHANGELOG.md). Behavior is claimed as supported only after it is verified (see [Accessibility](docs/accessibility.md)).

## Why

Most OTP components render **one text field per digit**. It looks right, but it breaks the things that matter:

| | One field per digit | **a11y-otp** (one field) |
|---|---|---|
| Screen reader | "Text field, text field, text field…" six times | One field with a label, progress and the entered code |
| SMS autofill | Often fills only the first box | Fills the whole code |
| Paste | Breaks or lands in one box | Works, including `482-913` and `482 913` |
| Backspace | Fragile focus jumping | Native behavior |

The boxes are decoration. Underneath is a single platform text input, so TalkBack, VoiceOver, autofill and paste treat it as one ordinary field.

## What you get

- `OtpField`: the code input, with a label, progress, an error state and a rejection message for codes of the wrong length.
- `OtpVerifyButton` and `OtpResendButton` (with a countdown you drive).
- `OtpState`: an immutable state you own, shared by both platforms.
- An optional Android module, `a11yotp-sms-retriever`, that fills the code from an SMS without permissions.

Your app owns the state, the server verification, the resend timer and all text (so everything can be localized). The library never makes network calls and never asks for SMS permissions.

## Requirements

| | Minimum |
|---|---|
| Kotlin | 2.4.21 (Kotlin/Native libraries need a compiler at least as new as the one that built them) |
| Android | minSdk 24, compileSdk 37, Jetpack Compose |
| iOS | 18.2, arm64 device and arm64 simulator; UIKit components, embeddable in SwiftUI |

## Installation

Add the library to your **KMP shared module** only. The Android app gets the Compose components through it, and the iOS app gets the UIKit components through the shared framework.

```kotlin
// shared/build.gradle.kts
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            export("me.vprogrammers:a11yotp:0.2.0") // makes the iOS components visible to Swift
        }
    }
    sourceSets.commonMain.dependencies {
        api("me.vprogrammers:a11yotp:0.2.0") // api, so androidApp sees the Compose components
    }
}
```

The artifacts are on Maven Central (`mavenCentral()`):

| Artifact | Contents |
|---|---|
| `me.vprogrammers:a11yotp` | `OtpState` and the Android and iOS components |
| `me.vprogrammers:a11yotp-sms-retriever` | optional, Android only: [SMS Retriever](docs/sms-retriever.md) |

## Quick start

Android (Compose):

```kotlin
var otp by rememberSaveable(stateSaver = OtpStateSaver) { mutableStateOf(OtpState(OtpLength.SIX)) }

OtpField(
    state = otp,
    onStateChange = { otp = it },
    onComplete = { code -> viewModel.verify(code) },
    accessibility = OtpFieldAccessibility(
        label = "Verification code",
        errorMessage = "Incorrect code. Try again.",
        progressDescription = { "${it.entered} of ${it.total} digits entered" },
        inputRejectedMessage = { "Code not entered: it has ${it.receivedDigitCount} digits, but ${it.expectedDigitCount} are expected." },
    ),
)
```

iOS (Swift):

```swift
import Shared

let field = OtpField(
    state: OtpState(length: .six),
    accessibility: OtpFieldAccessibility(
        label: "Verification code",
        errorMessage: "Incorrect code. Try again.",
        progressDescription: { "\($0.entered) of \($0.total) digits entered" },
        inputRejectedMessage: { "Code not entered: it has \($0.receivedDigitCount) digits, but \($0.expectedDigitCount) are expected." }
    ),
    doneButtonTitle: "Done"
)
field.onStateChange = { [weak field] newState in field?.state = newState }
field.onComplete = { code in verify(code) }
// Add field.view to your view hierarchy and keep `field` alive while it is on screen.
```

When verification fails, set `otp = otp.withError()` (Android) or `field.state = field.state.withError()` (iOS). The field shows and announces `errorMessage`.

## Documentation

| Document | Read it for |
|---|---|
| [Getting started](docs/getting-started.md) | The complete integration on both platforms, SwiftUI, the buttons, the error flow |
| [Input rules](docs/input-rules.md) | What the field accepts: typing, paste, autofill, normalization, lengths, rejection, completion |
| [Customization](docs/customization.md) | Styles, colors, dark mode, masks, alignment, grouping, the iOS Done key |
| [Accessibility](docs/accessibility.md) | What screen readers announce, WCAG coverage, what is verified, a checklist for your app |
| [The OTP message](docs/otp-message.md) | How to write the SMS so iOS and Android offer the code, for numeric and alphanumeric codes |
| [SMS Retriever](docs/sms-retriever.md) | Optional automatic SMS delivery on Android, the app hash and the message format |
| [Contributing](CONTRIBUTING.md) | Building, local checks, CI, the public API baseline, releasing |
| [Changelog](CHANGELOG.md) | Changes per version |

The `androidApp` and `iosApp` sample apps (with `sample/shared` as their shared module) show every option, and `iosApp/iosApp/Components/` has ready-to-copy SwiftUI wrappers.

## What this library does not do

- No network calls or code verification
- No resend timer: the resend button shows the seconds you pass in
- No SMS reading in the core library and no SMS permissions
- No rate limiting: your server must limit attempts
- Codes of 4, 6 or 8 characters: digits, or digits and the letters A–Z (autofill suggestions for codes with letters depend on the platform and keyboard)

## Roadmap

- [x] Shared OTP state and normalization
- [x] Android Compose component
- [x] iOS UIKit component (Kotlin/Native)
- [x] Masked mode
- [x] Optional SMS Retriever module for Android
- [x] Real-device verification with TalkBack and VoiceOver
- [x] Maven Central publication (0.1.0, 0.2.0)
- [ ] Real-device verification with Voice Control, Switch Access and Switch Control
- [x] Alphanumeric codes
- [ ] Optional Compose Multiplatform UI module
- [ ] Optional SMS User Consent support for Android

## Contributing

Issues and pull requests are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md). For accessibility bugs, please include the platform and OS version, the assistive technology and its version, and what was announced compared with what you expected.

## License

MIT. See [LICENSE](LICENSE).
