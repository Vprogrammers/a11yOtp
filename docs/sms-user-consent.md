# Android SMS User Consent (optional)

Use Google's [SMS User Consent API](https://developers.google.com/identity/sms-retriever/user-consent/overview) when you cannot add your app's hash to the SMS, for example because the message comes from a third-party service. When the message arrives, Android shows a system prompt with its text. If the user taps **Allow**, the app receives that one message and the code is filled. The app needs no SMS permissions, and the message needs no special format.

| | [SMS Retriever](sms-retriever.md) | SMS User Consent |
|---|---|---|
| Message | Must end with your app's hash | Any text |
| User action | None: the code is filled automatically | Taps **Allow** in a system prompt |
| Best for | Messages your backend controls | Messages you cannot change |

Both are in the same optional module, and both need Google Play services. Use one of them per screen; if both listen, each receives the same message.

## Add the module

```kotlin
// shared/build.gradle.kts
sourceSets.androidMain.dependencies {
    implementation("me.vprogrammers:a11yotp-sms-retriever:0.3.0")
}
```

The module also brings `androidx.activity:activity-compose`, which launches the prompt.

## Listen for the code

```kotlin
SmsUserConsentEffect(
    length = otp.length,
    charset = otp.charset,
    restartKey = resendCount, // listen again after each resend
    onCode = { code ->
        val result = otp.cleared().withInput(code) // reports completion even over a full typed code
        otp = result.state
        if (result.didComplete) viewModel.verify(result.state.code)
    },
    onFailure = { failure -> /* see below */ },
)
```

- **Listening:** the effect listens for one message for up to 5 minutes. Change `restartKey` to listen again, for example when the user requests a new code.
- **Sender:** pass `senderPhoneNumber` to accept only messages from that number. With `null` (the default) any sender is accepted. Google never offers messages from the user's contacts.
- **The code** is read with the same rules as the SMS Retriever: a standalone token of exactly the field's length, digits, or with `OtpCharset.ALPHANUMERIC` uppercase letters and digits with at least one digit. A message with two different codes is ignored. See [The OTP message](otp-message.md).
- **Background:** if the message arrives while your app is in the background, the prompt waits until the screen is visible again. If the screen is closed first, the prompt is dropped.

| `onFailure` | Meaning | What to do |
|---|---|---|
| `DENIED` | The user tapped **Deny** or dismissed the prompt | Show no error; the field still works with typing, paste and autofill |
| `TIMEOUT` | No message within 5 minutes | Offer a resend; change `restartKey` to listen again |
| `UNAVAILABLE` | No Google Play services, or listening could not start | The field still works with typing, paste and autofill |
| `CODE_LENGTH_MISMATCH` | The allowed message has a code of another plausible length (4 to 8 characters) | Your field length differs from your backend's; no code is delivered or logged |

If the user allows a message that has no code, nothing is delivered and no failure is reported.

## Accessibility

- **The prompt is system UI.** TalkBack announces it and moves focus into it; the library shows no UI of its own and moves no focus.
- **Prepare users.** A prompt that appears when the message arrives can interrupt a screen-reader user. Say beforehand that Android may ask to read the message, for example in your "We sent you a code" text.
- **Announce the result.** As with the SMS Retriever, the library makes no announcement. After **Allow**, announce the fill or the verification result in your UI with a polite live region (*"Code filled, verifying"*), because TalkBack may not speak a value change on an unfocused field.
- **On `DENIED`,** do not show an error or move focus. The user can still type, paste or use the keyboard's suggestion.
- **Privacy:** the user sees the whole message in the prompt, and your app receives only that one message, only after **Allow**. The library never logs the message or the code.

## Testing

User Consent needs Google Play services, so test on a real device or a Play Store emulator image; on an emulator, send the SMS from the extended controls. Check **Allow**, **Deny**, a message that arrives while the app is in the background, and rotation while the prompt is open. With TalkBack, check that the prompt is announced and that focus returns to your screen after it closes.

The Android sample has an **SMS** option to switch between Retriever, User Consent and Off.
