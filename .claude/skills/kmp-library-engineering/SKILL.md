---
name: kmp-library-engineering
description: Design or review A11yOtp module boundaries, shared Kotlin logic, public APIs, Swift interoperability, dependency choices, and library changes. Use before KMP architecture changes, public API evolution, new cross-platform features, structural refactors, or dependency additions.
---

# KMP library engineering

This file is the authoritative source for module boundaries.

## Module boundaries

- `sharedLogic`: platform-neutral OTP state, normalization, validation, completion behavior, and shared/Kotlin platform tests.
- `sharedUI`: Android Jetpack Compose component and Android accessibility behavior.
- `androidApp`: Android sample and integration host.
- `iosApp`: native SwiftUI component, iOS accessibility behavior, and iOS sample.
- Do not introduce Compose Multiplatform UI on iOS.
- Keep Android and iOS accessibility integrations platform-native.

## Workflow

1. Inspect the affected code and existing conventions.
2. State the user-visible behavior and edge cases.
3. Propose the smallest API and implementation that solves the requirement.
4. Keep platform-specific behavior in platform code.
5. Add focused tests and run the relevant commands from `CLAUDE.md`.
6. Report API impact, validation results, and remaining platform risks.

## Public API rules

- Keep shared APIs small, deterministic, and free of Android, Compose, SwiftUI, UIKit, and Foundation types.
- Prefer immutable configuration and explicit state transitions.
- Avoid wrappers, use cases, repositories, DI, and abstractions that add no behavior.
- Do not expose internal focus, animation, or rendering mechanics.
- Make defaults safe for accessibility and privacy.
- Document host responsibilities, including server verification and lifecycle clearing.
- Treat source and binary compatibility as release concerns once an API is published.

## Swift interoperability

Expose APIs that remain understandable from Swift. Avoid deeply nested generics, ambiguous overloads, and Kotlin-only idioms that produce awkward Objective-C headers. Keep SwiftUI state and focus ownership in Swift.

## Dependencies

Reuse the existing stack. Add a dependency only when its value exceeds its API, build, publication, and maintenance cost. Never add Android dependencies to `sharedLogic`.

## Review output

Order findings by impact. Include exact files, the violated boundary, and the smallest correction. Separate required fixes from optional improvements.

## Sources

https://github.com/kazimdsaidul/android-engineering-ai-skill/blob/main/skills/android-engineering/SKILL.md
