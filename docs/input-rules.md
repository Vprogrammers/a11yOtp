# Input rules

What the field accepts, what it rejects, and when it reports completion. The same rules apply on Android and iOS: they live in the shared `OtpState.withInput`. The platform components only differ in how they show a rejection (see [the end of this page](#how-a-rejection-is-shown)).

## Summary

| Input | Result |
|---|---|
| One digit typed | Appended. Ignored if the field is already full (no message). |
| Backspace | Deletes the last digit |
| Paste or autofill with exactly the field's digit count | Replaces the whole code; `onComplete` fires |
| Paste or autofill of several digits with any other count | Rejected: nothing changes, `onInputRejected` fires, a message is shown and announced |
| Paste of a single digit | Applied like a typed digit; it cannot be told apart from typing |
| Text with letters or other symbols (`Code: 482913`) | Ignored: nothing changes, no message (letters are accepted in [alphanumeric fields](#alphanumeric-codes)) |
| Text with spaces, dashes or dots (`482 913`, `482-913`) | The separators are dropped, then the rules above apply |

## Characters

The field stores only the ASCII digits `0`–`9` (`OtpState.code`), or with `OtpCharset.ALPHANUMERIC` also the uppercase ASCII letters `A`–`Z` (see [Alphanumeric codes](#alphanumeric-codes)).

- **Digits** in other scripts (Unicode decimal digits in the Basic Multilingual Plane, such as Arabic-Indic `٤٨٢٩١٣`) are converted to ASCII.
- **Ignored separators:** whitespace, the hyphen and the other dashes (including the minus sign, the soft hyphen and the fullwidth hyphen), dots (including the fullwidth dot), and invisible formatting characters (zero-width spaces and joiners, direction marks, the byte order mark).
- **Any other character** makes the whole input invalid: the state stays as it was and there is no message. A paste like `Your code is 482913` therefore does nothing. Platform autofill and the SMS Retriever pass only the code, so they are not affected.

## Lengths

`OtpLength.FOUR`, `OtpLength.SIX` or `OtpLength.EIGHT`, counted in characters. In an alphanumeric field, letters count like digits, and so do the counts in `OtpProgress` and `OtpInputRejection`.

**The field length must match your backend.** A code of a different length cannot be entered: typing stops at the field length, and paste or autofill of a different length is rejected. A rejection therefore almost always means the configured length is wrong. It is a configuration error for you to fix, not something users can work around. Log `onInputRejected` to find out (it gives the counts, never the digits).

## Alphanumeric codes

Create the state with `OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC)` for codes such as `K7Q-2MX`. All the other rules on this page stay the same.

- **Letters** `a`–`z` and `A`–`Z`, also fullwidth (`Ｋ`, `ｋ`), are stored in uppercase ASCII: typing `k7q2mx` gives `K7Q2MX`. Compare codes on your server case-insensitively, or send uppercase codes.
- **Look-alikes are kept apart:** `O` and `0`, or `I` and `1`, are different characters. Letters outside `A`–`Z`, such as `é` or the Cyrillic `А`, make the whole input invalid, like any other symbol.
- **Text around a code is no longer filtered out by its letters.** `Your code is K7Q2MX` pasted into a 6-character field is read as `YOURCODEISK7Q2MX`, 16 characters, so it is rejected with a message instead of being ignored. Symbols such as `:` still make the input invalid.
- **Keyboard:** a text keyboard with capital letters instead of the number pad. On iOS it has its own Return key, so there is no Done key above it. Android uses a password keyboard only while the field is masked, because TalkBack does not read back keys typed on it.
- **Accessibility text:** say "characters" rather than "digits" in `progressDescription` and `inputRejectedMessage`.
- **Autofill:** whether the keyboard or the system suggests a code with letters depends on the platform, the keyboard and the message format, and is not verified yet. Typing and paste always work.
- **Dictation and Voice Control:** a dictated chunk such as `K7Q` is several characters at once, so it is rejected unless it is the whole code. Dictate the whole code in one go, or one character at a time (for example with Voice Control's spelling mode).
- **SMS Retriever:** the `a11yotp-sms-retriever` module reads numeric codes only.

## Typing and deleting

- The cursor is always at the end. Each typed digit is appended, and Backspace deletes the last digit.
- A digit typed into a full field is ignored, without a message.
- Any change to the code clears an error set with `withError()`.

## Paste, autofill and keyboard suggestions

A single digit, also a pasted one, is applied like a typed digit. Several digits that arrive in one input are accepted only if the result is a whole code:

1. **The input holds exactly the field's digit count.** It replaces what was typed. For example, `482913` pasted over `12` gives `482913`, also when the new code shares digits with the old one.
2. **The digits added to what was typed make exactly the field's digit count.** For example, typing `12` and then pasting `3456` gives `123456`.
3. **Anything else is rejected**, never truncated or partly filled: `123` into a 6-digit field, `12345678` into a 6-digit field, or `12` followed by a paste of `34`.

Pasting the code that is already in the field changes nothing.

### Autofill on iOS

- iOS offers the code from Messages above the keyboard **only while the field is empty**. If the user has started typing before the message arrives, no suggestion appears.
- If the suggestion is showing and the user then types a few digits and taps it, the code from the message replaces the typed digits.
- iOS inserts an autofilled code one character at a time. The field recognizes this and applies the code as one input, so the length rule above still applies. Typing, including fast typing on a hardware keyboard, is never merged.

Autofill suggestions need a real device; they do not appear in the iOS simulator.

### Autofill on Android

The keyboard (for example Gboard) or the autofill service offers the code. It offers only a code that fits the field, so a code of the wrong length is usually not suggested at all; the user can still paste it, and it is then rejected. To fill the code without a tap, use the optional [SMS Retriever](sms-retriever.md).

## Completion

`onComplete(code)` fires once each time the code becomes complete:

- the last digit is typed;
- a paste or autofill fills the whole code;
- a paste or autofill replaces a complete code with a **different** complete code.

It does not fire again for a re-pasted identical code, or for a digit typed into a full field. After a wrong code, the next complete code fires again, whether the user edits the code or you call `cleared()`.

## Errors

Your server decides that a code is wrong; the field cannot know. Set `withError()` after a failed verification. The field then shows `errorMessage` and announces it once. The error clears when the code changes, or when you set `withoutError()` or `cleared()`. A rejection is not an error: it does not mark the field as being in error.

## How a rejection is shown

| | Android | iOS |
|---|---|---|
| Callback | `onInputRejected(OtpInputRejection)` once | `onInputRejected` once |
| Visible text | `inputRejectedMessage(rejection)` below the field | The same text in the error-text slot, neutral (no error icon) |
| Screen reader | Announced through a polite live region, on every rejection (also an identical repeat), without moving focus | One VoiceOver announcement 300 ms later, without moving focus. The text is also added to the field's value, so it can be read again. |
| While the field is in error | The rejection text replaces the error text; the error styling stays; the error text returns afterwards | The same |
| When it disappears | On the next edit, or when `state` changes | On any following input (also an ignored digit or an invalid character), or when `state` changes |

- The message never contains the digits, also when unmasked.
- `onStateChange` is not called for a rejection, because the state is unchanged.
- `inputRejectedMessage` is required. If it returns blank text, nothing is shown or announced and a warning is logged, but `onInputRejected` still fires.

Localize the message with plural rules. On Android the plural quantity is `receivedDigitCount`:

```xml
<plurals name="otp_rejected">
    <item quantity="one">Code not entered: it has %1$d digit, but %2$d are expected.</item>
    <item quantity="other">Code not entered: it has %1$d digits, but %2$d are expected.</item>
</plurals>
```

## Using the rules outside the field

`OtpState.withInput(text)` is the same logic, available to your own code, for example for a code that arrives from your own SMS handling or a deep link:

```kotlin
val result = otp.cleared().withInput(code) // cleared(): replace, and report completion even over a full code
otp = result.state
when {
    result.rejection != null -> log(result.rejection.expectedDigitCount, result.rejection.receivedDigitCount)
    result.didComplete -> verify(result.state.code)
}
```

Always store `result.state`, also after a rejection (it is the unchanged state).
