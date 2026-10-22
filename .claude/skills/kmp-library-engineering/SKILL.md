---
name: kmp-library-engineering
description: Design or review A11yOtp module boundaries, shared Kotlin logic, public APIs, Swift interoperability, dependency choices, and library changes. Use before KMP architecture changes, public API evolution, new cross-platform features, structural refactors, distribution decisions, or dependency additions.
---

# KMP library engineering

This file is the authoritative source for module boundaries.

## Module boundaries

The primary consumer is a KMP app with native Android UI and native iOS UI. Consumers add the library only through their shared module's `build.gradle.kts` (`api(...)` plus `export(...)` in their iOS framework). There is no Swift package: Kotlin dependencies cannot carry Swift source.

- `a11yotp`: the main library module and the only iOS-facing one (targets `android`, `iosArm64`, `iosSimulatorArm64`).
  - `commonMain`: OTP state, normalization, validation, completion, WCAG contrast checks, and platform-neutral configuration (`OtpMask`, `OtpAlignment`, accessibility text types). No Android, Compose, UIKit, or Foundation types.
  - `androidMain`: Jetpack Compose components on AndroidX Compose artifacts, plus Android styles (Compose `Color`, `TextStyle`, `Shape`).
  - `iosMain`: UIKit components written in Kotlin/Native, plus iOS styles (`UIColor`, `UIFont`).
- `a11yotp-sms-retriever`: optional Android-only module (target `android`) with the Google SMS Retriever integration (`SmsRetrieverEffect`). It depends on `a11yotp` with `api` and is the only module allowed to depend on Play Services, so consumers who don't opt in never get it on their classpath. Consumers add it from their shared module's `androidMain` dependencies. No gms types in its public API.
- `sample/shared` (`:sampleShared`): consumer-like shared module that exports `a11yotp` in the `Shared` framework. Both sample apps use the library through it.
- `androidApp`: Android sample and integration host. It depends on `a11yotp-sms-retriever` directly, because `sampleShared` is exported to the iOS framework.
- `iosApp`: SwiftUI sample that embeds the Kotlin UIKit components through `UIViewRepresentable` wrappers. It contains no library code.
- Colors, fonts, and shapes stay platform-native so dark mode, Increase Contrast, and dynamic colors keep working.
- Do not add the Compose Multiplatform plugin or Compose runtime to iOS. A future shared-Compose-UI module may depend on `a11yotp` without changing its APIs.
- Do not add lifecycle ViewModel or alpha Material3 dependencies unless the component needs them.

## Workflow

1. Inspect affected code and conventions.
2. State user-visible behavior and edge cases.
3. Propose the smallest API and implementation.
4. Keep platform-specific behavior in platform code.
5. Add focused tests and run commands from `CLAUDE.md`.
6. Report API impact, validation results, and platform risks.

## Public API rules

- Keep `commonMain` APIs small, deterministic, and free of Android, Compose, UIKit, and Foundation types. Platform types may appear only in `androidMain` and `iosMain` APIs.
- Prefer immutable configuration and explicit state transitions.
- Avoid wrappers, use cases, repositories, DI, and abstractions that add no behavior.
- Do not expose internal focus, animation, or rendering mechanics.
- Make defaults safe for accessibility and privacy.
- Document host responsibilities, including server verification and lifecycle clearing.
- Once an API is published, add Kotlin binary-compatibility validation before changing it.

## Swift interoperability

Swift sees the library through the consumer's Objective-C-exported framework. Expose APIs that remain understandable from Swift:

- Avoid deeply nested generics, ambiguous overloads, and Kotlin-only idioms that produce awkward Objective-C headers.
- Do not expose lambdas with primitive parameters; they import as `KotlinInt`. Pass a small value object instead (for example `(OtpProgress) -> String`).
- Kotlin default arguments are not visible from Swift. Give iOS types usable defaults through properties or factory functions.
- Swift cannot use a Kotlin subclass of a UIKit class. iOS components are Kotlin classes that own a `view: UIView`.
- Any reference from a UIKit object back to Kotlin must be weak; cycles across the boundary are not collected.
- The host owns the OTP state and focus; components expose callbacks and `focus()`/`blur()`.
- Kotlin/Native does not weak-link. Never reference an Objective-C class newer than the iOS deployment target (for example `UIGlassEffect`, iOS 26) directly: it becomes a strong `_OBJC_CLASS_$_` symbol and the app fails to launch on older iOS. Look the class up with `NSClassFromString` behind a version check, and confirm with `nm -u` on the framework. New methods on older classes are selector calls and only need the version check.

## Distribution status

Publishing is configured but nothing is released yet. Decided: Maven Central, coordinates `me.vprogrammers:a11yotp` and the optional Android-only `me.vprogrammers:a11yotp-sms-retriever`, both consumed from the KMP shared module, first version `0.1.0`, GPG-signed releases from the manual `publish.yml` workflow, no Swift Package Manager or CocoaPods distribution. Consumers need Kotlin 2.4.21 or newer. Not decided: the compatibility policy for 0.x and 1.0.

Treat these as explicit design decisions. Do not claim that consumers can install the library until publication is implemented and verified.

## Dependencies

Reuse the existing stack. Add a dependency only when its value exceeds its API, build, publication, and maintenance cost. Never add Android or Compose dependencies to `commonMain` or `iosMain`. Optional integrations that bring heavy or policy-sensitive dependencies (such as Play Services) get their own module instead of going into `a11yotp`.

## Review output

Order findings by impact. Include exact files, the violated boundary, and the smallest correction. Separate required fixes from optional improvements.
