# Android SMS Retriever (optional)

Platform autofill needs no setup: the keyboard or the autofill service offers the code, and the user taps it (see [Input rules](input-rules.md#autofill-on-android)). If your backend can change the SMS text, Android can also deliver the code automatically, without a tap and without SMS permissions, through Google's [SMS Retriever API](https://developers.google.com/identity/sms-retriever/overview).

## Add the module

It is a separate, Android-only module, so apps that do not use it never get Google Play services:

```kotlin
// shared/build.gradle.kts
sourceSets.androidMain.dependencies {
    implementation("me.vprogrammers:a11yotp-sms-retriever:0.1.0")
}
```

## Listen for the code

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

- **Listening:** the retriever listens for one message for up to 5 minutes. Change `restartKey` to listen again, for example when the user requests a new code.
- **Ignored messages:** a message with no code, or with two different codes of the field's length, is ignored.
- **Wrong length:** the message may have no standalone run of the field's digit count, but have one of another plausible length (4 to 8 digits). Then `onFailure` reports `CODE_LENGTH_MISMATCH` right away, instead of waiting for `TIMEOUT`. No code is delivered, and the digits are never passed or logged. It means the field length differs from your backend's.
- **Without Google Play services**, `onFailure` reports `UNAVAILABLE`. The field still works with autofill, typing and paste.

## Accessibility

The field's value and progress update as they do after typing, but the library makes no announcement. TalkBack usually speaks a state change only on the focused element, so a user whose focus is elsewhere hears nothing when the code arrives. Announce the fill or the verification result in your UI, for example with a polite live region (*"Code filled automatically, verifying"*). The library never logs the message or the code.

## Message format

The SMS must contain the code and end with your app's 11-character hash, and must be no longer than 140 bytes:

```text
Your Example code is 123456

FA+9qCX9VSu
```

Keep the code the only number in the message; the hash is never mistaken for the code.

## The app hash

The hash depends on the package name and the signing key, so debug, release and Play App Signing builds each have their own hash. Compute it once per signing key and configure it on your backend:

```bash
keytool -exportcert -alias <alias> -keystore <keystore> \
  | xxd -p | tr -d "[:space:]" \
  | echo -n "<applicationId> $(cat)" \
  | shasum -a 256 | tr -d "[:space:]-" \
  | xxd -r -p | base64 | cut -c1-11
```

If the hash from the keystore does not work, compute it from the app that is actually installed. The retriever checks that app's certificate, so this hash is always the right one. It needs `apksigner` from the Android SDK build tools:

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

A debug build can be signed with a different debug keystore than `~/.android/debug.keystore`, for example one created by Android Studio on another machine. That is why the two methods can give different hashes.

For Play App Signing, use the app signing certificate from the Play Console, not your upload key.

## Testing

The retriever needs Google Play services, so test on a real device or a Play Store emulator image.

Send the message from a registered SMS provider (application-to-person, for example Twilio or Vonage). Carriers may block or silently drop code-like messages from a private number, so a test SMS from a personal phone can fail to arrive even when the sender is a saved contact. For manual tests, a short text such as `Code is 482915` followed by the hash usually gets through.

This module never uses `READ_SMS` or `RECEIVE_SMS`. Those permissions are heavily restricted on Google Play.
