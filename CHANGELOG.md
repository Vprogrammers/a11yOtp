# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [semantic versioning](https://semver.org/). While the version is 0.x, minor versions may change the public API.

## [Unreleased]

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

[Unreleased]: https://github.com/Vprogrammers/a11yOtp/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Vprogrammers/a11yOtp/releases/tag/v0.1.0
