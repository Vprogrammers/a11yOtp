---
name: gradle-ci-quality
description: Configure or review A11yOtp Gradle builds and GitHub Actions quality gates. Use for version catalogs, KMP and Android build configuration, ktlint, tests, caching, dependency verification, CI workflows, artifacts, publishing preparation, or toolchain upgrades.
---

# Gradle and CI quality

Keep build changes compatible with the existing KMP project and avoid unrelated upgrades.

## Build rules

- Use `gradle/libs.versions.toml` as the version source.
- Use the committed Gradle wrapper and JDK 21 where required by the current toolchain.
- Keep generated `build/` sources out of ktlint and source control.
- Preserve configuration-cache compatibility.
- Never use `clean` as a routine verification step.
- Do not apply Android-only migration guidance blindly to KMP modules.
- Do not change minimum SDKs, Apple targets, publishing coordinates, or public API compatibility without explaining the impact.
- Do not reintroduce deprecated `iosX64` solely to suppress Intel-host warnings.

## CI gates

Prefer separate attributable jobs or steps for:

1. `ktlintCheck`;
2. shared logic tests;
3. Android build and Compose tests;
4. iOS framework and Swift tests on a compatible macOS runner;
5. publication validation when publishing is introduced.

Cache Gradle dependencies and supported build/configuration-cache state. Never skip tests or verification with convenience flags in CI.

## Platform handling

An Intel Mac cannot execute ARM64 iOS simulator tests, and Kotlin/Native support for Intel macOS is deprecated. Report that limitation; do not treat the disabled target as a product defect. Run ARM64 simulator validation on Apple Silicon CI.

## Supply chain and artifacts

- Never commit signing keys, credentials, tokens, `.env`, or `local.properties`.
- Use CI secret storage for future signing and publishing.
- Add dependency verification and automated dependency updates deliberately, not as incidental feature work.
- Apply restricted retention to signed or publication artifacts.

## Verification

Run the narrowest relevant task first. Before a PR, run all applicable gates and report actual output. Never claim coverage, compatibility, or publication success without executing the corresponding verification.

## Sources

Guidance informed by:
https://github.com/kazimdsaidul/android-engineering-ai-skill/blob/main/skills/android-engineering/references/gradle-testing-ci.md
