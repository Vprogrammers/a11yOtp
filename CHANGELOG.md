# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [semantic versioning](https://semver.org/). While the version is 0.x, minor versions may change the public API.

## [Unreleased]

### Added

- **Restoring from saved values** ([#37](https://github.com/Vprogrammers/a11yOtp/issues/37)): `OtpLength.fromName` and `OtpCharset.fromName`, also usable from Swift. The iOS sample restores the field with `@SceneStorage`.

## [0.2.0]

### Added

- **Alphanumeric codes** ([#41](https://github.com/Vprogrammers/a11yOtp/issues/41)): `OtpCharset` (`NUMERIC`, `ALPHANUMERIC`), `OtpState(length, charset)`, `OtpState.charset` and `OtpState.restore(length, charset, code, isError)`.
  - Letters `A`–`Z` are case-insensitive and stored in uppercase; fullwidth letters are converted.
  - A capital-letter text keyboard on both platforms; the iOS Done key is shown only above the number pad.
  - `OtpStateSaver` saves the charset; numeric states keep the 0.1.0 format.
  - `a11yotp-sms-retriever`: `SmsRetrieverEffect(length, charset, …)` reads codes of uppercase letters and digits with at least one digit.
  - Existing APIs are unchanged and default to numeric.
- **[The OTP message](docs/otp-message.md):** how to write the SMS so iOS and Android offer the code.

### Changed

- **iOS:** the components register for the traits they read (`registerForTraitChanges`) instead of the deprecated `traitCollectionDidChange`, and re-render on every trait that affects color appearance, including the interface level ([#38](https://github.com/Vprogrammers/a11yOtp/issues/38)).

## [0.1.0]

First release.

### Added

- **`OtpState`** (shared): immutable state for a 4-, 6- or 8-digit numeric code.
  - Input normalization: separators are dropped and non-ASCII digits are converted.
  - The length rule: a paste or autofill of the wrong length is rejected (`OtpInputRejection`), never truncated or partly filled.
  - Completion reporting, and `withError()`, `withoutError()` and `cleared()`.
- **Android:** Jetpack Compose `OtpField`, `OtpVerifyButton` and `OtpResendButton`.
- **iOS:** UIKit `OtpField`, `OtpVerifyButton` and `OtpResendButton`, written in Kotlin/Native and embeddable in SwiftUI.
  - A Done key above the number pad.
  - Dismissal with Return, Escape or VoiceOver's two-finger scrub.
  - Autofilled codes applied as one input.
- **Both platforms:**
  - One accessible text input; the cells are decorative and hidden.
  - Label, progress and error announced.
  - The rejection message shown and announced.
  - Masks (`None`, `Character`, `FilledCell`) that hide the digits from screen readers too.
  - Default colors that follow the system theme and meet WCAG contrast; your own colors are checked.
  - Dynamic Type and font scale, Reduce Motion, right-to-left layouts.
- **Saving the state:** `OtpState.restore(length, code, isError)` (shared) and `OtpStateSaver` for Compose `rememberSaveable`, so a partly entered code survives Android configuration changes and process death.
- **`a11yotp-sms-retriever`**: optional Android module for Google's SMS Retriever, with `CODE_LENGTH_MISMATCH` reporting.

[Unreleased]: https://github.com/Vprogrammers/a11yOtp/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/Vprogrammers/a11yOtp/releases/tag/v0.2.0
[0.1.0]: https://github.com/Vprogrammers/a11yOtp/releases/tag/v0.1.0
