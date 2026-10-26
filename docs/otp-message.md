# The OTP message

How to write the SMS that carries the code, so that iOS and Android offer it above the keyboard and the optional [SMS Retriever](sms-retriever.md) fills it without a tap. The library does not send messages; your backend or SMS provider does.

## Templates

Numeric code, all platforms:

```text
Your Example code is 482913. Do not share it with anyone.
```

Alphanumeric code (`OtpCharset.ALPHANUMERIC`):

```text
Your Example code is K7Q2MX. Do not share it with anyone.
```

With the Android SMS Retriever, the last line is your app's 11-character hash (see [The app hash](sms-retriever.md#the-app-hash)). One message works for iOS, Android autofill and the retriever:

```text
Your Example code is K7Q2MX. Do not share it with anyone.

FA+9qCX9VSu
```

## Rules

| Rule | Why |
|---|---|
| Send the code as one unbroken token: `482913`, `K7Q2MX` | The field accepts `482 913` or `K7Q-2MX` when pasted, but keyboards and the SMS Retriever look for one token. A split code is not suggested or read |
| Make it exactly the field's length (`OtpLength`) | A code of another length is usually not suggested, is rejected when pasted, and the retriever reports `CODE_LENGTH_MISMATCH` |
| Keep it the only code-like token in the message | A second token of the same length makes the message ambiguous: the retriever delivers nothing, and keyboards may suggest the wrong one. Leave out order numbers, amounts, phone numbers and brand names with digits |
| Put a word such as "code" right before it | iOS and Android recognize one-time codes heuristically. A phrase like "Your Example code is" helps; it is not a guarantee |
| Alphanumeric: uppercase `A`–`Z` and digits, at least one digit | The retriever takes only uppercase tokens with a digit, so words like `Please` or `VERIFY` are never read as the code. A letter-only code is not recognized |
| Alphanumeric: consider leaving out `O`, `0`, `I` and `1` | They are easy to confuse when the code is read or typed by hand. The library never merges look-alikes |
| Keep the message within 140 bytes | The SMS Retriever limit. Non-Latin characters take more bytes per character |
| Retriever only: end with the app hash on its own line | Required by the SMS Retriever. The hash is stripped before the code is looked for, so it is never taken for the code |
| Send from a registered SMS provider (application-to-person) | Carriers can block or drop code-like messages from private numbers |

## Platform behavior

- **iOS:** apps cannot read SMS. Messages offers the code above the keyboard while the field is empty; the user taps it, and the field accepts it like a paste (lowercase letters become uppercase). Suggestions need a real device. Whether a code with letters is offered depends on iOS recognizing it; see [Autofill on iOS](input-rules.md#autofill-on-ios).
- **Android without the retriever:** the keyboard (for example Gboard) or the autofill service offers the code. Codes with letters depend on the keyboard; see [Autofill on Android](input-rules.md#autofill-on-android).
- **Android with the retriever:** the code is filled without a tap. Pass the field's `charset` to `SmsRetrieverEffect` for alphanumeric codes; see [SMS Retriever](sms-retriever.md).

Typing and paste always work, whatever the message looks like.

## Testing from a personal phone

Carriers treat a message that looks like a service template (`Your code is 123456`) sent from a private number as abuse and can drop it silently, so it never reaches the device. A shorter text such as `Code is 123456` usually gets through. If a message seems lost on an iPhone, check the Messages filters first (Unknown Senders, Transactions, Junk). Test the real template from your SMS provider before release.
