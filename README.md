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

Both platforms use native UI and native accessibility APIs. Compose is not used on iOS, so iOS apps do not take on the Compose runtime. Colors and fonts are platform-native (Compose `Color`, `UIColor`). The default colors follow the system light or dark theme on both platforms, and Increase Contrast works as usual on iOS.

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
- No SMS permissions. The core library has no Google Play services dependency; the optional `a11yotp-sms-retriever` module is the only part that uses it.

---

## Platforms

| Platform | Minimum (current project settings) | UI |
|---|---|---|
| Android | minSdk 24 | Jetpack Compose |
| iOS | 18.2 (Xcode deployment target), arm64 device and arm64 simulator | UIKit (embeddable in SwiftUI) |

Minimum versions are not final and may change before the first release.

---

## Installation

Not on Maven Central yet; the first release will be `0.1.0`. You add the library only to your KMP shared module; the Android app gets the Compose components through it, and the iOS app gets the UIKit components through its framework.

| Artifact | Contents |
|---|---|
| `me.vprogrammers:a11yotp` | OTP state and the Android and iOS components (`android`, `iosArm64`, `iosSimulatorArm64`) |
| `me.vprogrammers:a11yotp-sms-retriever` | optional Android-only SMS Retriever integration |

Requirements: Kotlin 2.4.21 or newer (Kotlin/Native libraries need a compiler at least as new as the one that built them), Android minSdk 24 and compileSdk 37 (built with AGP 9.3.1), iOS 18.2.

```kotlin
// shared/build.gradle.kts
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            export("me.vprogrammers:a11yotp:0.1.0") // makes the iOS components visible to Swift
        }
    }
    sourceSets.commonMain.dependencies {
        api("me.vprogrammers:a11yotp:0.1.0") // api, so androidApp sees the Compose components
    }
}
```

`sample/shared/build.gradle.kts` is a working example that uses a project dependency, and `scripts/consumer-check` builds the same setup against the published artifacts.

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
            inputRejectedMessage = { context.resources.getQuantityString(R.plurals.otp_rejected, it.receivedDigitCount, it.receivedDigitCount, it.expectedDigitCount) },
        ),
    onInputRejected = { rejection -> analytics.log("otp_length_mismatch", rejection.expectedDigitCount, rejection.receivedDigitCount) },
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
        progressDescription: { "\($0.entered) of \($0.total) digits entered" },
        inputRejectedMessage: { "Code not entered: \($0.receivedDigitCount) digits, \($0.expectedDigitCount) expected." }
    ),
    doneButtonTitle: "Done"
)
field.onStateChange = { [weak field] newState in field?.state = newState }
field.onComplete = { code in verify(code) }
// Add field.view to your view hierarchy, or use OtpFieldView in SwiftUI:
// OtpFieldView(state: $otp, accessibility: accessibility, onComplete: verify)
```

Keep a strong reference to the Kotlin object (`OtpField`, `OtpVerifyButton`, `OtpResendButton`) for as long as its view is on screen. The sample wrappers keep it in their coordinator.

### Paste, autofill and the field length

Typing one digit at a time is unchanged; a digit typed into a full field is ignored. A single input that inserts several digits at once (paste, platform autofill, a keyboard suggestion) is accepted **only if it contains exactly as many digits as the field** (`OtpLength.FOUR`, `SIX` or `EIGHT`). It then replaces what is in the field, and `onComplete` fires. Separators such as `123 456` or `123-456` do not count as digits.

Any other multi-digit input is rejected, never truncated or filled in partially: the state stays as it was, nothing completes, and the host learns the expected and received digit counts (never the digits):

- `OtpState.withInput(text)` returns `result.rejection` (`OtpInputRejection(expectedDigitCount, receivedDigitCount)`).
- The Android `OtpField` calls `onInputRejected` once and shows `OtpFieldAccessibility.inputRejectedMessage(rejection)` below the field, in a polite live region that TalkBack announces on every rejection (also an identical repeat, not per keystroke), without moving focus. The rejection itself is not an error state (no error border or vibration) and `onStateChange` is not called when the state is unchanged. The message disappears on the next edit and when `state` changes. While `state.isError`, the newest message wins: the rejection text replaces the error text and the error styling and semantics stay; the error text returns once the rejection clears. A blank `inputRejectedMessage` shows nothing (a warning is logged) but `onInputRejected` still fires.
- The iOS `OtpField` calls `onInputRejected` once and shows the message in the error-text slot (neutral text, no error icon), posts one VoiceOver announcement 300 ms later without moving focus, and appends the message to the field's `accessibilityValue` so VoiceOver users can re-read it (never with digits, also when unmasked). The message ends on any following input, including an ignored extra digit or an unsupported character, and when `state` changes. While `state.isError` the newest message wins: it replaces the error text in the label and in the value, the error cell styling stays, and the error text returns once the rejection clears. A blank `inputRejectedMessage` shows and announces nothing (a warning is logged) but `onInputRejected` still fires. In SwiftUI, pluralize with automatic grammar agreement, for example `AttributedString(localized: "^[\(count) digit](inflect: true)")`.

`inputRejectedMessage` is required and must be localized by the host. Use plural rules for the counts; on Android the plural quantity is `receivedDigitCount`, for example `strings.xml`:

```xml
<plurals name="otp_rejected">
    <item quantity="one">Code not entered: it has %1$d digit, but %2$d are expected.</item>
    <item quantity="other">Code not entered: it has %1$d digits, but %2$d are expected.</item>
</plurals>
```

or iOS `Localizable.stringsdict` entries with `NSStringLocalizedFormatKey`. Do not put the digits in the message.

**The field length must match your backend.** A rejection means the configured length differs from the code length your backend sends. That is a configuration error to fix, not something users can work around. Typing one digit at a time cannot detect a wrong-length code: the field simply completes at its own length.

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

### Dark mode

The default colors follow the system theme. On iOS they are dynamic system colors. On Android, a style without `colors` uses light or dark colors depending on the system setting, also when you change other style values:

```kotlin
// Follows the system theme
OtpFieldDefaults.style(groupSize = 3)

// Your app has its own theme switch: pick the colors yourself
OtpFieldDefaults.style(colors = if (appIsDark) OtpFieldDefaults.darkColors() else OtpFieldDefaults.colors())
OtpButtonDefaults.style(colors = if (appIsDark) OtpButtonDefaults.darkColors() else OtpButtonDefaults.colors())
```

Both palettes meet WCAG contrast (4.5:1 for text, 3:1 for borders and focus). In dark mode the focused cell has a white border and a white shadow, because a blue border would be too close to the normal border and a black shadow would not show.

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

> **Message text:** iOS and Android choose which number in the message to suggest; the app cannot influence it. Keep the code the only number in the message. A brand name with digits (`A11yOtp`), a phone number or an amount can be suggested instead of the code.

> **Sending:** send codes from your backend through a registered SMS provider (application-to-person, for example Twilio or Vonage). Carriers may block or silently drop code-like messages such as "Your code is …" when they come from a private number, so a test SMS sent from a personal phone can fail to arrive even when the sender is a saved contact. For manual tests, a short text such as `Code is 482915` usually gets through.

> **Google Play:** this library never uses `READ_SMS` or `RECEIVE_SMS`. Those permissions are heavily restricted on Google Play.

### Android SMS Retriever (optional)

Platform autofill above needs no setup: the keyboard or autofill service offers the code and the user taps it. If your backend can change the SMS text, Android can also deliver the code automatically, without a tap and without SMS permissions, through Google's [SMS Retriever API](https://developers.google.com/identity/sms-retriever/overview).

It lives in a separate, Android-only module so apps that don't use it never get Google Play services:

```kotlin
// shared/build.gradle.kts
sourceSets.androidMain.dependencies {
    implementation("me.vprogrammers:a11yotp-sms-retriever:0.1.0")
}
```

Start listening on the OTP screen. The SMS carries the whole code, so replace what the user typed and still go through `withInput`:

```kotlin
SmsRetrieverEffect(
    length = otp.length,
    restartKey = resendCount, // listen again after each resend
    onCode = { code ->
        val result = otp.cleared().withInput(code) // reports completion even over a full typed code
        otp = result.state
        if (result.didComplete) viewModel.verify(result.state.code)
    },
    onFailure = { /* TIMEOUT after 5 minutes, UNAVAILABLE without Play services, or CODE_LENGTH_MISMATCH */ },
)
```

The retriever listens for one message for up to 5 minutes. Change `restartKey` to listen again, for example when the user requests a new code. A message with no code, or with two different codes of the field's length, is ignored. If the message has no standalone run of the field's digit count but has one of another plausible length (4 to 8 digits), `onFailure` reports `CODE_LENGTH_MISMATCH` right away instead of waiting for `TIMEOUT`; no code is delivered, and the digits are never passed or logged. It means the field length differs from your backend's.

**Accessibility.** The field's value and progress update like after typing, but the library makes no announcement. TalkBack usually speaks a state change only on the focused element, so a user whose focus is elsewhere hears nothing when the code arrives. Announce the fill or the verification result in your UI, for example with a polite live region (`Code filled automatically, verifying`). The library never logs the message or the code.

**Backend message format.** The SMS must contain the code and end with your app's 11-character hash, and must be no longer than 140 bytes:

```text
Your Example code is 123456

FA+9qCX9VSu
```

The hash depends on the package name and the signing key, so debug, release and Play App Signing builds each have their own hash. Compute it once per signing key and configure it on your backend:

```bash
keytool -exportcert -alias <alias> -keystore <keystore> \
  | xxd -p | tr -d "[:space:]" \
  | echo -n "<applicationId> $(cat)" \
  | shasum -a 256 | tr -d "[:space:]-" \
  | xxd -r -p | base64 | cut -c1-11
```

If the hash from the keystore doesn't work, compute it from the app that is actually installed. The retriever checks that app's certificate, so this hash is always the one to use. It needs `apksigner` from the Android SDK build tools:

```bash
APK=$(adb shell pm path <applicationId> | head -1 | sed 's/package://' | tr -d '\r')
adb pull "$APK" app.apk
"$ANDROID_HOME"/build-tools/<version>/apksigner verify --print-certs-pem app.apk \
  | awk '/BEGIN CERT/,/END CERT/' | openssl x509 -outform der \
  | xxd -p | tr -d "[:space:]" \
  | (echo -n "<applicationId> $(cat)") \
  | shasum -a 256 | tr -d "[:space:]-" \
  | xxd -r -p | base64 | cut -c1-11
```

A debug build can be signed with a different debug keystore than `~/.android/debug.keystore`, for example one created by Android Studio on another machine, so the two methods can give different hashes.

For Play App Signing, use the app signing certificate from the Play Console, not your upload key. The retriever needs Google Play services on the device, so a Play Store emulator image or a real device is required to test it. On devices without Google Play services, `onFailure` reports `UNAVAILABLE`; the field still works with autofill, typing and paste.

---

## What this library does not do

By design, it stays a UI component:

- No network calls or code verification
- No resend timer: the resend button shows the seconds you pass in
- No SMS reading in the core library and no SMS permissions. Automatic SMS delivery on Android is opt-in through `a11yotp-sms-retriever`.
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

### Local checks

`./scripts/check.sh` runs all project checks on your machine, the same ones the GitHub validation workflow runs:

```bash
./scripts/check.sh                          # everything: kmp, android, ios
./scripts/check.sh kmp                      # ktlint, host tests, Kotlin/Native compile, Shared framework link
./scripts/check.sh android                  # Android device test compile, Android sample, publication and consumer
./scripts/check.sh ios                      # SwiftLint, iOS sample and UI test builds for the simulator
./scripts/check.sh android --device         # plus Android device tests on a connected, unlocked device or emulator
./scripts/check.sh ios --device <iphone-id> # plus iOS UI tests on a connected, unlocked iPhone
```

Each step runs even if an earlier one failed. At the end the script prints which steps passed, failed, or were skipped, and it exits non-zero if anything failed. Steps that this machine can't run are skipped, never silently passed. The Kotlin/Native tests on the iOS simulator need Apple Silicon, so an Intel Mac skips them. Device tests run only with `--device`.

Run it before opening a pull request. The individual commands are below.

### GitHub validation

`.github/workflows/validation.yml` runs on every pull request and on pushes to `main`. Standard GitHub-hosted runners are free for public repositories.

- **Shared and Android (JVM)**, on Ubuntu: ktlint, host tests, Android device test compile, and the Android sample.
- **iOS (Apple Silicon)**, on macOS: Kotlin/Native tests on the iOS simulator, the Shared framework, SwiftLint, and the iOS UI tests on a simulator. This covers what an Intel Mac can't run.
- **Android device tests**: only when started by hand (Actions → Validation → Run workflow, with the emulator option), because they are slow.

A manual run can also pick a single target: `all`, `kmp`, `android`, or `ios`.

The JVM job also runs `./scripts/verify-publication.sh`: it publishes both modules to Maven Local and builds a clean consumer project (`scripts/consumer-check`) against them, for Android everywhere and for iOS on macOS.

### Releasing

`.github/workflows/publish.yml` releases the `VERSION_NAME` from `gradle.properties` to Maven Central. Nothing is ever published automatically.

1. Raise `VERSION_NAME` in a pull request and merge it.
2. Start Actions → Publish → Run workflow on `main`. Other branches are refused.
3. The full validation runs, including the Android device tests on an emulator.
4. The publish job waits for approval in the protected `release` environment. It refuses a version that already has a tag or is already on Maven Central, verifies the publication with a clean consumer, signs, and uploads.
5. Check the deployment in the [Central Portal](https://central.sonatype.com/publishing/deployments) and publish it there. Uploads are not released automatically.
6. Publish the draft GitHub release `v<version>` that the workflow created.

The `release` environment holds the secrets `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` (a Central Portal user token), and `SIGNING_KEY` and `SIGNING_KEY_PASSWORD` (the ASCII-armored GPG private key). They never go into the repository. Local `publishToMavenLocal` runs are unsigned and need none of them.

### Individual commands

```bash
./gradlew ktlintCheck                              # formatting
./gradlew :a11yotp:testAndroidHostTest             # shared and Android tests on the JVM
./gradlew :a11yotp:compileTestKotlinIosSimulatorArm64  # compile Kotlin/Native tests
./gradlew :androidApp:assembleDebug                # Android sample app
```

Open `iosApp/iosApp.xcodeproj` in Xcode to run the iOS sample. The build phase runs `:sampleShared:embedAndSignAppleFrameworkForXcode`.

> **Intel Macs** cannot run the ARM64 iOS simulator tests (`:a11yotp:iosSimulatorArm64Test`). Run them on Apple Silicon. The deprecated `iosX64` target is intentionally not added.

### iOS UI tests

`iosAppUITests` drives the iOS sample with XCUITest. On a real iPhone it also runs from an Intel Mac. To sign for a device, create the git-ignored `iosApp/Configuration/Local.xcconfig` with your team:

```text
TEAM_ID=<your team id>
```

Don't set the team in Xcode's Signing settings; that writes it into `project.pbxproj`. Then, with the phone unlocked:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations   # find the device id
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'id=<device id>' -allowProvisioningUpdates
```

If `xcodebuild` says the scheme is not configured for testing, delete the auto-generated user scheme in `iosApp/iosApp.xcodeproj/xcuserdata/*/xcschemes/iosApp.xcscheme`; the shared scheme has the tests.

The tests cover typing, the single accessibility element and its value, the error state, one completion, the Verify and Resend buttons, focus on launch, and masked mode. Paste (the system paste prompt), one-time-code autofill from Messages, and what VoiceOver actually says are still manual checks on a device.

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
- [ ] Maven Central publication (configured; first release pending)
- [ ] Optional Compose Multiplatform UI module
- [x] Optional SMS Retriever module for Android
- [ ] Optional SMS User Consent support for Android

---

## Contributing

Issues and pull requests are welcome. For accessibility bugs, please include:
- platform and OS version,
- assistive technology and its version,
- what was announced vs. what you expected.

---

## License

MIT. See [LICENSE](LICENSE).
