# Getting started

This guide walks through a complete OTP screen on both platforms: the field, the Verify and Resend buttons, and what to do after your server answers. The rules for what the field accepts are in [Input rules](input-rules.md); the look is in [Customization](customization.md).

## How it fits together

```text
your shared module ── api("me.vprogrammers:a11yotp") ──┬── androidApp: Compose components
                     export(...) in the iOS framework ─┴── iosApp:     UIKit components (from Swift)
```

- **You own the state.** `OtpState` is immutable. The field reports every change through `onStateChange`; you store the new state and pass it back. Your own code can also replace it, for example with `withError()` or `cleared()`.
- **You own verification.** `onComplete` hands you the code once it is complete. The library never talks to a server.
- **You supply all text.** `OtpFieldAccessibility`, `OtpVerifyAccessibility` and `OtpResendAccessibility` have no defaults, so nothing ships untranslated. Localize them like any other string.

## 1. Add the dependency

Add `a11yotp` to your KMP shared module as shown in the [README](../README.md#installation). Use `api(...)` so your Android app sees the Compose components, and `export(...)` in the iOS framework so Swift sees the UIKit components. `sample/shared/build.gradle.kts` is a working setup.

## 2. The state

```kotlin
val empty = OtpState(OtpLength.SIX)  // FOUR, SIX or EIGHT; must match your backend
```

| Member | Meaning |
|---|---|
| `length`, `code`, `isError` | The configured length, the entered ASCII digits, the error flag |
| `enteredDigitCount`, `isComplete` | Progress; `isComplete` is true when `code` has `length` digits |
| `withInput(text)` | Applies text as if the user had entered it; returns `OtpInputResult(state, didComplete, rejection)` |
| `withError()` | The same code, marked as rejected by your server |
| `withoutError()` | The same code, without the error |
| `cleared()` | An empty state of the same length, without an error |

The field calls `withInput` for you. Call it yourself only when code comes from somewhere else, such as the [SMS Retriever](sms-retriever.md).

## 3. Android (Compose)

```kotlin
@Composable
fun OtpScreen(viewModel: OtpViewModel) {
    var otp by remember { mutableStateOf(OtpState(OtpLength.SIX)) }
    val context = LocalContext.current

    val accessibility = OtpFieldAccessibility(
        label = stringResource(R.string.otp_label),                 // "Verification code"
        errorMessage = stringResource(R.string.otp_error),          // "Incorrect code. Try again."
        progressDescription = { context.resources.getQuantityString(R.plurals.otp_progress, it.total, it.entered, it.total) },
        inputRejectedMessage = {
            context.resources.getQuantityString(R.plurals.otp_rejected, it.receivedDigitCount, it.receivedDigitCount, it.expectedDigitCount)
        },
    )

    Column {
        OtpField(
            state = otp,
            onStateChange = { otp = it },
            onComplete = { code -> viewModel.verify(code) },
            accessibility = accessibility,
        )
        OtpVerifyButton(
            state = otp,
            onVerify = { code -> viewModel.verify(code) },
            accessibility = OtpVerifyAccessibility(stringResource(R.string.otp_verify)),
        )
        OtpResendButton(
            secondsRemaining = viewModel.secondsRemaining,
            onResend = { viewModel.resend(); otp = otp.cleared() },
            accessibility = OtpResendAccessibility(stringResource(R.string.otp_resend)) {
                context.getString(R.string.otp_resend_in, it.secondsRemaining) // "Resend code in %d s"
            },
        )
    }
}
```

- `OtpField` also takes `modifier`, `enabled`, `style`, `mask`, `hapticFeedbackOnError` (default `true`), `alignment` and `onInputRejected`.
- To show the keyboard when the screen opens, pass `Modifier.focusRequester(requester)` and call `requester.requestFocus()` in a `LaunchedEffect`. Then the keyboard and its code suggestion appear without a tap.
- The keyboard's Done key closes the keyboard.

## 4. iOS (UIKit from Swift)

The iOS components are Kotlin classes that own a UIKit `view`:

```swift
import Shared

let field = OtpField(state: OtpState(length: .six), accessibility: accessibility, doneButtonTitle: "Done")
field.onStateChange = { [weak field] newState in field?.state = newState }
field.onComplete = { code in verify(code) }
field.onInputRejected = { rejection in log(rejection.expectedDigitCount, rejection.receivedDigitCount) }
container.addSubview(field.view)
field.focus()   // after the view is in a window; field.blur() dismisses the keyboard
```

- **Keep the Kotlin object alive** (`OtpField`, `OtpVerifyButton`, `OtpResendButton`) as long as its view is on screen. The view does not keep it alive.
- **Properties you can change at any time:** `state`, `accessibility`, `doneButtonTitle`, `style`, `mask`, `isEnabled`, `alignment` and `hapticFeedbackOnError`.
- **Buttons:**
  - `OtpVerifyButton(accessibility:)`: set `state` to the field's state and handle `onVerify`.
  - `OtpResendButton(accessibility:)`: set `secondsRemaining` and handle `onResend`.
- **Done:** the number pad has no Return key, so the field adds a Done key above it; see [Customization](customization.md#the-ios-done-key).

### SwiftUI

Copy the wrappers in `iosApp/iosApp/Components/` (`OtpFieldView`, `OtpVerifyButtonView`, `OtpResendButtonView`). Their coordinators keep the Kotlin objects alive:

```swift
@State private var otp = OtpState(length: .six)

OtpFieldView(
    state: $otp,
    accessibility: fieldAccessibility,
    focusOnAppear: true,
    onComplete: verify
)
OtpVerifyButtonView(state: otp, accessibility: verifyAccessibility, onVerify: verify)
OtpResendButtonView(secondsRemaining: secondsRemaining, accessibility: resendAccessibility, onResend: resend)
```

Create styles and accessibility objects once (for example as `static let`), not in `body`. A new object on every update looks like a change to the component.

Localize the counts with plural rules. In SwiftUI, automatic grammar agreement works well, for example `AttributedString(localized: "^[\(count) digit](inflect: true)")`; otherwise use `Localizable.stringsdict` entries.

## 5. Verify and handle the result

`onComplete` fires once each time the code becomes complete: typed, pasted or autofilled. `OtpVerifyButton` is enabled only while the code is complete. Use one of the two to start verification, or de-duplicate if you wire both to the same call.

| Server answer | What to do |
|---|---|
| Correct | Leave the screen, or disable the field (`enabled = false` / `isEnabled = false`) and show your success state |
| Wrong code | `otp = otp.withError()`. The field shows `errorMessage` with an icon, announces it once without moving focus, and vibrates once (unless `hapticFeedbackOnError` is false). The next edit clears the error. |
| New code sent (resend) | `otp = otp.cleared()`, then restart your countdown |
| Too many attempts, expired | Show your own message; the field has no rate limiting |

After a wrong code, the next complete code reports completion again, whether the user edits the code or you call `cleared()`.

## 6. Recommended message text

So that autofill offers the right number:

- Keep the code the only number in the message. A brand name with digits, a phone number or an amount can be suggested instead.
- Send it from a registered SMS provider (application-to-person). Carriers can drop code-like messages from personal numbers.
- On Android, the [SMS Retriever](sms-retriever.md) can fill the code without a tap if the message ends with your app's hash.

## Next

- [Input rules](input-rules.md): exactly what is accepted, rejected and normalized.
- [Customization](customization.md): styles, dark mode, masks.
- [Accessibility](accessibility.md): what users hear, and what to check before you ship.
