---
name: kmp-library-engineering
description: Design or review A11yOtp module boundaries, shared Kotlin logic, public APIs, Swift interoperability, dependency choices, and library changes. Use for KMP architecture, API evolution, new features, refactors, or cross-platform implementation decisions.
---

# KMP library engineering

Preserve the established platform boundary:

- `sharedLogic`: platform-neutral OTP state, normalization, validation, and completion behavior.
- `sharedUI`: Android Jetpack Compose component and Android accessibility integration.
- `iosApp`: native SwiftUI component and iOS accessibility integration.
- Do not introduce Compose Multiplatform UI on iOS.

## Workflow

1. Inspect the affected code and existing conventions.
2. State the user-visible behavior and edge cases.
3. Propose the smallest API and implementation that solves the requirement.
4. Keep platform-specific behavior in platform code.
5. Add focused tests and run the relevant checks.
6. Report API impact, validation results, and remaining platform risks.

## Public API rules

- Keep the shared API small, deterministic, and free of Android, Compose, SwiftUI, UIKit, and Foundation types.
- Prefer immutable input/configuration and explicit state transitions.
- Avoid wrappers, use cases, repositories, DI, and abstractions that add no behavior.
- Do not expose internal focus, animation, or rendering mechanics.
- Make defaults safe for accessibility and privacy.
- Document behavior that host applications must control, including code verification and lifecycle clearing.
- Treat source and binary compatibility as deliberate release concerns once an API is published.

## Swift interoperability

Expose Kotlin APIs that remain understandable from Swift. Avoid deeply nested generics, overloaded APIs that bridge ambiguously, and Kotlin-only idioms that produce awkward Objective-C headers. Keep SwiftUI state and focus ownership in Swift.

## Dependency rules

Reuse the existing stack. Introduce a dependency only when its value exceeds its API, build, publishing, and maintenance cost. Do not add an Android dependency to `sharedLogic`.

## Review output

Order findings by impact. Include exact files, the violated boundary, and the smallest viable correction. Distinguish required fixes from optional improvements.

## Sources

Workflow informed by:
https://github.com/kazimdsaidul/android-engineering-ai-skill/blob/main/skills/android-engineering/SKILL.md
