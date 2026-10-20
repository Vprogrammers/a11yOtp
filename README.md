# a11y-otp

**An accessible one-time code field for Compose Multiplatform.**
One real text field, drawn as boxes. VoiceOver, TalkBack, SMS autofill and paste work the way users expect, on Android and iOS, from one `commonMain` call.

[![Maven Central](https://img.shields.io/maven-central/v/com.vprogrammers/a11y-otp?label=Maven%20Central)](https://central.sonatype.com/artifact/com.vprogrammers/a11y-otp)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.8+-4285F4)](https://www.jetbrains.com/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS-lightgrey)](#platforms)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

<!-- Replace with your demo GIF: VoiceOver on iOS + TalkBack on Android, same code -->
<p align="center">
  <img src="docs/demo.gif" alt="OTP field filled by SMS autofill, read by VoiceOver on iOS and TalkBack on Android" width="640">
</p>

---

## Why

Most OTP components render **one text field per digit**. It looks right, but it breaks the things that matter:

| | One field per digit | **a11y-otp** (one field) |
|---|---|---|
| Screen reader | "Text field, text field, text field…" six times | One field: *"Verification code, 3 of 6 digits entered"* |
| SMS autofill | Often fills only the first box | Fills the whole code |
| Paste | Breaks or lands in one box | Works, including `482-913` and `482 913` |
| Backspace | Fragile focus jumping | Native behaviour |
| iOS 26 autofill | Known issue with multi-field OTP | Not affected |

The boxes in **a11y-otp** are decoration. Underneath is a single `BasicTextField`, so the platform treats it as one ordinary input.

---

## Features

- ✅ One `commonMain` composable for Android and iOS
- ✅ VoiceOver and TalkBack: one element, live progress, announced errors
- ✅ SMS autofill: `ContentType.SmsOtpCode` on Android, `.oneTimeCode` on iOS
- ✅ Paste cleanup: spaces, dashes and non-ASCII digits are normalized
- ✅ Scales with Dynamic Type and Android font scale
- ✅ Respects Reduce Motion (no shake when it is on)
- ✅ Errors are never shown by colour alone
- ✅ Custom cell slot: change the look without forking
- ✅ No Google Play services dependency, no SMS permission

---

## Platforms

| Platform | Support |
|---|---|
| Android | ✅ minSdk 23 |
| iOS | ✅ iOS 15+ (arm64, simulator arm64) |
| Desktop / Web | Planned |

---

## Installation

Add the dependency to your shared module:

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.vprogrammers:a11y-otp:1.0.0")
        }
    }
}
```

That's it. No CocoaPods, SPM or Tuist step for iOS. The library is linked into your shared framework.

For an Android-only project:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.vprogrammers:a11y-otp:1.0.0")
}
```

---

## Quick start

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

Your Android `setContent { App() }` and iOS `ComposeUIViewController { App() }` stay exactly as they are.

---

## Usage

### State

```kotlin
val otp = rememberOtpState(length = 6)

otp.value          // "4829"
otp.isComplete     // false
otp.error          // null

otp.setError("Wrong code. 2 attempts left.")
otp.clear()
```

`OtpState` is hoisted: you own it, the field only updates it from user input.

### Handling a wrong code

```kotlin
val otp = rememberOtpState(6)
val scope = rememberCoroutineScope()

OtpField(
    state = otp,
    onComplete = { code ->
        scope.launch {
            if (!api.verify(code)) {
                otp.setError("Wrong code. Try again.")
            }
        }
    },
)
```

When `error` is set, the field:
- shows error colours **and** an icon with the message below,
- announces the message to VoiceOver and TalkBack,
- plays a haptic,
- shakes, unless Reduce Motion is on.

Typing again clears the error automatically.

### Custom cells

```kotlin
OtpField(
    state = otp,
    onComplete = ::verify,
) { cell ->
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = if (cell.isActive) 2.dp else 1.dp,
                color = if (cell.isError) Color.Red else Color.Gray,
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(cell.char?.toString() ?: "", style = MaterialTheme.typography.headlineSmall)
    }
}
```

Your cell is always hidden from screen readers. Accessibility is handled by the field, whatever you draw.

`OtpCellState`:

| Property | Type | Meaning |
|---|---|---|
| `index` | `Int` | Position, 0-based |
| `char` | `Char?` | Entered character, or `null` |
| `isActive` | `Boolean` | Next cell to be filled |
| `isError` | `Boolean` | Field is in error state |

### Theming

```kotlin
OtpField(
    state = otp,
    onComplete = ::verify,
    colors = OtpDefaults.colors(
        border = Color(0xFF8A8A8A),
        activeBorder = Color(0xFF3A4AA0),
        errorBorder = Color(0xFFB3261E),
        text = Color(0xFF1C1B1F),
    ),
)
```

Default colours meet a contrast ratio of at least 4.5:1 in light and dark themes.

### Localization

```kotlin
OtpField(
    state = otp,
    onComplete = ::verify,
    label = "Verifikacioni kod",
    strings = OtpDefaults.strings(
        progress = { entered, total -> "$entered od $total cifara uneto" },
    ),
)
```

### All parameters

| Parameter | Default | Description |
|---|---|---|
| `state` | — | `OtpState` from `rememberOtpState()` |
| `onComplete` | — | Called once when the last cell is filled |
| `modifier` | `Modifier` | Applied to the field |
| `label` | `"Verification code"` | Accessible name, read by screen readers |
| `colors` | `OtpDefaults.colors()` | Border, text and error colours |
| `strings` | `OtpDefaults.strings()` | Progress text for screen readers |
| `enabled` | `true` | Disables input and dims the field |
| `autoFocus` | `false` | Focus and open the keyboard on first composition |
| `cell` | `DefaultOtpCell` | Slot to draw each box |

---

## Autofill

| | Android | iOS |
|---|---|---|
| Mechanism | `ContentType.SmsOtpCode` | `textContentType = .oneTimeCode` |
| Where the code comes from | Autofill service, keyboard suggestion | Messages, Mail (iOS 17+) |
| Extra setup | None | None |
| SMS permission | Not used | Not available on iOS |

> **Testing on iOS:** `.oneTimeCode` suggestions only appear on a real device, not in the simulator.

> **Google Play:** this library never uses `READ_SMS` or `RECEIVE_SMS`. Those permissions are heavily restricted on Google Play and can get an app rejected.

---

## Accessibility

### What screen readers hear

| Moment | VoiceOver / TalkBack |
|---|---|
| Focus | *"Verification code, text field, 0 of 6 digits entered"* |
| Typing | *"3 of 6 digits entered"* |
| Complete | *"6 of 6 digits entered"* |
| Error | *"Wrong code. Try again."* (announced immediately) |

### App Store Accessibility Nutrition Labels

This component **does not block** any of these labels for your app:

| Label | Status | How |
|---|---|---|
| VoiceOver | ✅ | One element, live progress value, announced errors |
| Voice Control | ✅ | Visible label matches the accessible name |
| Larger Text | ✅ | Cells grow with Dynamic Type, no fixed heights |
| Dark Interface | ✅ | Light and dark default colours |
| Differentiate Without Color Alone | ✅ | Error = colour + icon + text |
| Sufficient Contrast | ✅ | Defaults ≥ 4.5:1 |
| Reduced Motion | ✅ | Shake animation is disabled |

> Labels are declared for your **whole app**, not per component. This library supports them, but your other screens must too. Only declare what your app actually meets.

### Android

| Check | Status |
|---|---|
| TalkBack | ✅ |
| Switch Access | ✅ |
| Font scale up to 2.0 | ✅ |
| Touch target ≥ 48dp | ✅ |
| Remove animations setting | ✅ |
| Accessibility Scanner | No warnings |

### WCAG 2.2

| Criterion | |
|---|---|
| 1.3.1 Info and Relationships | One labelled input |
| 1.4.1 Use of Color | Errors are not colour-only |
| 1.4.3 Contrast (Minimum) | Default colours ≥ 4.5:1 |
| 1.4.4 Resize Text | Scales with system font size |
| 2.3.3 Animation from Interactions | Respects Reduce Motion |
| 2.5.3 Label in Name | Visible label = accessible name |
| 2.5.8 Target Size (Minimum) | Whole row is the tap target |
| 3.3.1 Error Identification | Errors described in text and announced |
| 3.3.8 Accessible Authentication | Paste and autofill always allowed |

---

## Using it from SwiftUI

If your iOS UI is SwiftUI and only your logic is shared, the library ships a `UIViewController` you can wrap.

**1. Export the library from your shared framework:**

```kotlin
// shared/build.gradle.kts
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            export("com.vprogrammers:a11y-otp:1.0.0")
        }
    }
    sourceSets {
        commonMain.dependencies {
            api("com.vprogrammers:a11y-otp:1.0.0")
        }
    }
}
```

**2. Wrap it once:**

```swift
import SwiftUI
import Shared

struct OtpView: UIViewControllerRepresentable {
    var length: Int32 = 6
    var label: String = "Verification code"
    let onComplete: (String) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        OtpFieldViewControllerKt.OtpFieldViewController(
            length: length,
            label: label,
            onComplete: onComplete
        )
    }

    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
```

**3. Use it:**

```swift
OtpView { code in viewModel.verify(code) }
    .frame(height: 64)
```

> Embedding Compose in SwiftUI adds the Compose runtime to your app. If you don't already use Compose Multiplatform, a native SwiftUI component such as [CBPinEntryView](https://github.com/Fawxy/CBPinEntryView) is a better fit.

---

## What this library does not do

By design, it stays a UI component:

- ❌ No network calls or code verification
- ❌ No resend timer
- ❌ No SMS reading (no SMS Retriever, no permissions)
- ❌ No rate limiting. Your server must enforce attempt limits.

---

## Testing your integration

Before you ship, check the field with assistive technology on real devices:

- [ ] VoiceOver (iOS): focus is one element, progress is read while typing
- [ ] TalkBack (Android): same as above
- [ ] Wrong code: the error is announced without moving focus
- [ ] Largest text size: nothing is clipped
- [ ] Reduce Motion / Remove animations: no shake
- [ ] SMS autofill on a real device, both platforms
- [ ] Paste `123-456` from the clipboard

---

## Requirements

| | Minimum |
|---|---|
| Kotlin | 2.2 |
| Compose Multiplatform | 1.8 |
| Android | minSdk 23 |
| iOS | 15.0 |

---

## Roadmap

- [ ] Secure mode (dots instead of digits)
- [ ] Alphanumeric codes
- [ ] Desktop and Web targets
- [ ] Optional SMS User Consent module for Android

---

## Contributing

Issues and pull requests are welcome. For accessibility bugs, please include:
- platform and OS version,
- assistive technology and its version,
- what was announced vs. what you expected.

---

## License

```
MIT License

Copyright (c) 2026 VProgrammers
```

See [LICENSE](LICENSE) for the full text.
