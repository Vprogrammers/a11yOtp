---
name: kmp-library-engineering
description: Design or review A11yOtp module boundaries, shared Kotlin logic, public APIs, Swift interoperability, dependency choices, and library changes. Use before KMP architecture changes, public API evolution, new cross-platform features, structural refactors, distribution decisions, or dependency additions.
---

# KMP library engineering

This file is the authoritative source for module boundaries.

## Module boundaries

- `sharedLogic`: platform-neutral OTP state, normalization, validation, completion behavior, and shared/Kotlin platform tests.
- `sharedUI`: technically a Compose Multiplatform module using `org.jetbrains.compose` and Compose code in `commonMain`, but its product scope is Android UI only. Do not consume it from iOS.
- `androidApp`: Android sample and integration host.
- `iosApp`: native SwiftUI component, iOS accessibility behavior, and iOS sample.
- Do not introduce Compose Multiplatform UI on iOS.
- Keep Android and iOS accessibility integrations platform-native.

Before expanding `sharedUI`, decide explicitly whether Android-facing APIs use AndroidX Compose artifacts and whether Android-only implementations belong in `androidMain` instead of `commonMain`. Do not add lifecycle ViewModel or alpha Material3 dependencies unless the component needs them.

## Workflow

1. Inspect affected code and conventions.
2. State user-visible behavior and edge cases.
3. Propose the smallest API and implementation.
4. Keep platform-specific behavior in platform code.
5. Add focused tests and run commands from `CLAUDE.md`.
6. Report API impact, validation results, and platform risks.

## Public API rules

- Keep shared APIs small, deterministic, and free of Android, Compose, SwiftUI, UIKit, and Foundation types.
- Prefer immutable configuration and explicit state transitions.
- Avoid wrappers, use cases, repositories, DI, and abstractions that add no behavior.
- Do not expose internal focus, animation, or rendering mechanics.
- Make defaults safe for accessibility and privacy.
- Document host responsibilities, including server verification and lifecycle clearing.
- Once an API is published, add Kotlin binary-compatibility validation before changing it.

## Swift interoperability

Expose APIs that remain understandable from Swift. Avoid deeply nested generics, ambiguous overloads, and Kotlin-only idioms that produce awkward Objective-C headers. Keep SwiftUI state and focus ownership in Swift.

## Distribution status

No publication configuration exists yet. The project has not decided:

- whether Android artifacts are published separately for `sharedLogic` and `sharedUI`;
- whether iOS distribution uses Swift Package Manager, an XCFramework, or both;
- artifact coordinates, versioning policy, repository, signing, or release automation.

Treat these as explicit design decisions. Do not claim that consumers can install the library until publication is implemented and verified.

## Dependencies

Reuse the existing stack. Add a dependency only when its value exceeds its API, build, publication, and maintenance cost. Never add Android dependencies to `sharedLogic`.

## Review output

Order findings by impact. Include exact files, the violated boundary, and the smallest correction. Separate required fixes from optional improvements.
