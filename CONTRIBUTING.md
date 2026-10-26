# Contributing

Issues and pull requests are welcome. For accessibility bugs, please include the platform and OS version, the assistive technology and its version, and what was announced compared with what you expected.

## Project layout

| Module | Contents |
|---|---|
| `a11yotp` | The library. `commonMain`: OTP state, input rules, contrast checks, masks, alignment and accessibility text. `androidMain`: Jetpack Compose components. `iosMain`: UIKit components written in Kotlin/Native. |
| `a11yotp-sms-retriever` | Optional Android-only SMS Retriever integration; the only module that depends on Google Play services |
| `sample/shared` | A consumer-style KMP shared module that adds `a11yotp` once and exports it to iOS |
| `androidApp` | Android sample app |
| `iosApp` | SwiftUI sample app that embeds the UIKit components |

Both platforms use native UI and native accessibility APIs. Compose is not used on iOS, so iOS apps do not take on the Compose runtime.

iOS uses UIKit rather than SwiftUI because the library is delivered through Gradle, and a Kotlin dependency cannot carry Swift source. Kotlin/Native can build UIKit views, and SwiftUI apps embed them with a short `UIViewRepresentable` wrapper.

## Requirements

JDK 21, the Android SDK, and Xcode for iOS.

## Local checks

`./scripts/check.sh` runs all project checks on your machine, the same ones the GitHub validation workflow runs:

```bash
./scripts/check.sh                          # everything: kmp, android, ios
./scripts/check.sh kmp                      # ktlint, host tests, public API check, Kotlin/Native compile, Shared framework link
./scripts/check.sh android                  # Android device test compile, Android sample, publication and consumer
./scripts/check.sh ios                      # SwiftLint, iOS sample and UI test builds for the simulator
./scripts/check.sh android --device         # plus Android device tests on a connected, unlocked device or emulator
./scripts/check.sh ios --device <iphone-id> # plus iOS UI tests on a connected, unlocked iPhone
```

- **Failures:** each step runs even if an earlier one failed. At the end the script prints which steps passed, failed or were skipped, and it exits non-zero if anything failed.
- **Skipped steps:** steps this machine can't run are skipped, never silently passed. The Kotlin/Native tests on the iOS simulator need Apple Silicon, so an Intel Mac skips them. Device tests run only with `--device`.

Run it before opening a pull request.

### Individual commands

```bash
./gradlew ktlintCheck                                  # formatting
./gradlew checkKotlinAbi                               # public API against the api/ dumps (macOS)
./gradlew updateKotlinAbi                              # after an intended public API change; commit api/
./gradlew :a11yotp:testAndroidHostTest                 # shared and Android tests on the JVM
./gradlew :a11yotp-sms-retriever:testAndroidHostTest   # SMS Retriever tests on the JVM
./gradlew :a11yotp:compileTestKotlinIosSimulatorArm64  # compile Kotlin/Native tests
./gradlew :a11yotp:iosSimulatorArm64Test               # Kotlin/Native tests (Apple Silicon)
./gradlew :a11yotp:connectedAndroidDeviceTest          # Android device tests, including the Accessibility Test Framework
./gradlew :androidApp:assembleDebug                    # Android sample app
./scripts/verify-publication.sh                        # publish to Maven Local and build a clean consumer
./scripts/swiftlint.sh                                 # SwiftLint (pinned version)
```

Open `iosApp/iosApp.xcodeproj` in Xcode to run the iOS sample. The build phase runs `:sampleShared:embedAndSignAppleFrameworkForXcode`.

> **Intel Macs** cannot run the ARM64 iOS simulator tests (`:a11yotp:iosSimulatorArm64Test`). Run them on Apple Silicon or in CI. The deprecated `iosX64` target is intentionally not added.

## Public API

Both published modules use Kotlin ABI validation. The reference dumps in `a11yotp/api/` and `a11yotp-sms-retriever/api/` record the public API, and `checkKotlinAbi` fails when the code no longer matches them.

- **An intended API change:** run `./gradlew updateKotlinAbi` and commit the dump. Reviewers read the dump diff as the API change.
- **On macOS only:** the check needs macOS, because the klib dump includes the iOS targets.
- **Compatibility:** keep changes source- and binary-compatible unless the version says otherwise.

Keep `commonMain` free of Android, Compose, UIKit and Foundation types. Platform types appear only in `androidMain` and `iosMain`.

## GitHub validation

`.github/workflows/validation.yml` runs on every pull request and on pushes to `main`:

- **Shared and Android (JVM)**, on Ubuntu: ktlint, host tests, the Android device test compile, the Android sample, and `./scripts/verify-publication.sh`.
  The verification publishes both modules to Maven Local and builds the clean consumer in `scripts/consumer-check`.
- **iOS (Apple Silicon)**, on macOS: the public API check, the Kotlin/Native tests on the iOS simulator, the Shared framework, SwiftLint, and the iOS UI tests (including Apple's accessibility audit) on a simulator.
- **Android device tests**, on an emulator (API 35, a narrow screen of about 320 dp): on every pull request, when started by hand (Actions → Validation → Run workflow, with the emulator option), and in the release workflow. Pushes to `main` skip them, because the pull request already ran them. Device tests must not assume a wide screen.

A manual run can also pick a single target: `all`, `kmp`, `android` or `ios`.

## iOS UI tests

`iosAppUITests` drives the iOS sample with XCUITest. The tests cover:

- typing, deleting and quick typing;
- the single accessibility element and its value;
- the error state, one completion, and masked mode;
- the Verify and Resend buttons, focus on launch, and the Done key;
- Apple's accessibility audit, by default, in dark mode and at the largest accessibility text size.

Paste (the system paste prompt), one-time-code autofill from Messages, a hardware Escape key, and what VoiceOver actually says remain manual checks on a device.

On a real iPhone the tests also run from an Intel Mac. To sign for a device, create the git-ignored `iosApp/Configuration/Local.xcconfig` with your team:

```text
TEAM_ID=<your team id>
```

Don't set the team in Xcode's Signing settings; that writes it into `project.pbxproj`. Then, with the phone unlocked:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations   # find the device id
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'id=<device id>' -allowProvisioningUpdates
```

- **Not configured for testing:** if `xcodebuild` says the scheme is not configured for testing, delete the auto-generated user scheme in `iosApp/iosApp.xcodeproj/xcuserdata/*/xcschemes/iosApp.xcscheme`. The shared scheme has the tests.
- **Automation timeout:** if the run stops with "Timed out while enabling automation mode" or "has not loaded accessibility", restart the iPhone. Then keep it unlocked and confirm the UI automation prompt on the device.

## SwiftLint

Swift sources in the sample app are checked with the version pinned in `Mintfile`, so local and CI checks use the same rules. Install [Mint](https://github.com/yonaskolb/Mint) once (`brew install mint`) and run `./scripts/swiftlint.sh` from the repository root. The script installs the pinned version through Mint when needed. If Mint is unavailable, it accepts an existing `swiftlint` only when its version matches the pin.

To show violations in Xcode, add a Run Script build phase:

```bash
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
"$SRCROOT/../scripts/swiftlint.sh"
```

and set `ENABLE_USER_SCRIPT_SANDBOXING = NO`, or Xcode can prevent the script from reading project files. This is optional; CI does not depend on it and never modifies Swift sources.

## Releasing

`.github/workflows/publish.yml` releases the `VERSION_NAME` from `gradle.properties` to Maven Central. Nothing is ever published automatically.

1. Between releases `main` carries the next `-SNAPSHOT` version (the publish workflow refuses it). For a release, set `VERSION_NAME` to the release version, move the `[Unreleased]` entries in [CHANGELOG.md](CHANGELOG.md) under it, and merge the pull request. After the release, raise `VERSION_NAME` to the next `-SNAPSHOT`.
2. Start Actions → Publish → Run workflow on `main`. Other branches are refused.
3. The full validation runs, including the Android device tests on an emulator.
4. The publish job waits for approval in the protected `release` environment. Then it:
   - refuses a version that already has a tag or is already on Maven Central;
   - verifies the publication with a clean consumer;
   - signs and uploads.
5. Check the deployment in the [Central Portal](https://central.sonatype.com/publishing/deployments) and publish it there. Uploads are not released automatically.
6. Publish the draft GitHub release `v<version>` that the workflow created. Publishing it starts `.github/workflows/api-reference.yml`, which deploys the API reference of that release to GitHub Pages.

## API reference

Dokka generates the API reference for `a11yotp` and `a11yotp-sms-retriever`: `./gradlew :dokkaGenerate` writes `build/dokka/html`. Run it with `-PdokkaStrict=true` (as CI does on macOS) to fail on any warning, such as a KDoc link that does not resolve. Write links that Dokka can resolve: on Android `OtpField` is a function, so link `[OtpField]` and name the parameter in backticks. The javadoc jars of both artifacts contain the same HTML. A cold run takes about 1.5 minutes; it is cached afterwards.

GitHub Pages must use **GitHub Actions** as its source (Settings → Pages). `api-reference.yml` can also be started by hand; it then publishes the current `main`.

The `release` environment holds the secrets. They never go into the repository:

- `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`: a Central Portal user token;
- `SIGNING_KEY` and `SIGNING_KEY_PASSWORD`: the ASCII-armored GPG private key.

Local `publishToMavenLocal` runs are unsigned and need none of them.
