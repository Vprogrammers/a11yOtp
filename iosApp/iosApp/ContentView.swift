import Shared
import SwiftUI

/// Sample screen. Strings are English literals for the sample only; real apps pass localized strings.
/// A code of only zeros is rejected (see `isSampleCodeRejected` in sample/shared).
struct ContentView: View {
    /// Groups of 3 give "123 - 456"; 4 and 8 digits do not split evenly, so they get no separator.
    private static let fieldStyle = OtpFieldDefaults.shared.style().withGroupSize(groupSize: 3)
    /// Square cells with a large corner radius are drawn as circles.
    private static let roundFieldStyle = fieldStyle
        .withCornerRadius(cornerRadius: .greatestFiniteMagnitude)
        .withMinCellSize(width: 48, height: 48)

    /// Liquid Glass backdrop behind the cells; the cells stay solid for contrast.
    private static let glassFieldStyle = fieldStyle.withGlass(glass: true)
    private static let glassRoundFieldStyle = roundFieldStyle.withGlass(glass: true)

    private static let buttonStyle = OtpButtonDefaults.shared.style()
    private static let glassButtonStyle = buttonStyle.withGlass(glass: true)

    private static let fieldAccessibility = makeFieldAccessibility(unit: "digit")
    /// Letters count too, so progress and rejection say "characters" rather than "digits".
    private static let alphanumericFieldAccessibility = makeFieldAccessibility(unit: "character")

    private static func makeFieldAccessibility(unit: String) -> OtpFieldAccessibility {
        OtpFieldAccessibility(
            label: "Verification code",
            errorMessage: "Incorrect code. Try again.",
            progressDescription: { "\($0.entered) of \($0.total) \(unit)s entered" },
            inputRejectedMessage: {
                let received = counted(Int($0.receivedDigitCount), unit: unit)
                let expected = counted(Int($0.expectedDigitCount), unit: unit)
                return "The code has \(received), but \(expected) are expected."
            }
        )
    }

    /// "1 digit" or "4 digits" through automatic grammar agreement.
    private static func counted(_ count: Int, unit: String) -> String {
        // Both units are inflected by the same rule; "character" is a regular English noun.
        String(AttributedString(localized: "^[\(count) \(unit)](inflect: true)").characters)
    }

    private static let verifyAccessibility = OtpVerifyAccessibility(label: "Verify")
    private static let resendAccessibility = OtpResendAccessibility(
        label: "Resend code",
        countdownDescription: { "Resend code in \($0.secondsRemaining) s" }
    )

    @State private var otp: OtpState
    @State private var options: SampleOptions
    @State private var isVerified = false
    @State private var secondsRemaining = Self.resendSeconds
    @State private var countdownRun = 0
    @State private var didRestore = false
    // Scene restoration: the Kotlin enum names ("SIX", "ALPHANUMERIC"), not the Swift case names.
    // The code is sensitive and is written to disk here; real apps may choose not to persist it.
    @SceneStorage("otp.length") private var savedLength = ""
    @SceneStorage("otp.charset") private var savedCharset = ""
    @SceneStorage("otp.code") private var savedCode = ""
    @SceneStorage("otp.isError") private var savedIsError = false

    /// UI tests pass `-resendSeconds <n>` to shorten the countdown; it is 30 seconds otherwise.
    private static var resendSeconds: Int {
        let override = UserDefaults.standard.integer(forKey: "resendSeconds")
        return override > 0 ? override : Int(SampleOtpKt.SAMPLE_RESEND_SECONDS)
    }

    /// UI tests pass `-manualVerify YES` so completion does not verify and the Verify button can be tested.
    private static var manualVerify: Bool { UserDefaults.standard.bool(forKey: "manualVerify") }

    /// UI tests pass `-skipRestore YES` so every launch starts empty, whatever the scene saved.
    private static var skipRestore: Bool { UserDefaults.standard.bool(forKey: "skipRestore") }

    init(options: SampleOptions = SampleOptions()) {
        _options = State(initialValue: options)
        _otp = State(initialValue: OtpState(length: options.length, charset: options.charset))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Enter the \(otp.length.digitCount)-\(codeUnit) code we sent you")
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
                OtpFieldView(
                    state: $otp,
                    accessibility: options.alphanumeric ? Self.alphanumericFieldAccessibility : Self.fieldAccessibility,
                    style: fieldStyle,
                    mask: options.mask,
                    focusOnAppear: !options.isDisabled,
                    onComplete: { if !Self.manualVerify { verify($0) } }
                )
                .disabled(isVerified || options.isDisabled)
                OtpVerifyButtonView(
                    state: otp,
                    accessibility: Self.verifyAccessibility,
                    style: buttonStyle,
                    onVerify: verify
                )
                .disabled(otp.isError || isVerified)
                if isVerified {
                    // The icon and text carry the result, so the default text color is enough.
                    Label("Code verified", systemImage: "checkmark.circle.fill").font(.headline)
                }
                OtpResendButtonView(
                    secondsRemaining: secondsRemaining,
                    accessibility: Self.resendAccessibility,
                    style: buttonStyle,
                    onResend: resend
                )
                SampleOptionsView(options: $options)
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
        .onAppear(perform: restoreIfSaved)
        .onChange(of: otp.isComplete) { _, isComplete in
            if !isComplete { isVerified = false }
        }
        .onChange(of: options.length) { _, length in
            // Restoring sets the options to match the restored state; keep that state.
            guard otp.length != length else { return }
            otp = OtpState(length: length, charset: options.charset)
            isVerified = false
        }
        .onChange(of: options.alphanumeric) { _, _ in
            guard otp.charset != options.charset else { return }
            otp = OtpState(length: options.length, charset: options.charset)
            isVerified = false
        }
        .onChange(of: otp.code) { _, _ in save() }
        .onChange(of: otp.isError) { _, _ in save() }
        .onChange(of: otp.length) { _, _ in save() }
        .onChange(of: otp.charset) { _, _ in save() }
        .task(id: countdownRun) {
            while secondsRemaining > 0 {
                try? await Task.sleep(for: .seconds(1))
                if Task.isCancelled { return }
                secondsRemaining -= 1
            }
        }
    }

    private func save() {
        savedLength = otp.length.name
        savedCharset = otp.charset.name
        savedCode = otp.code
        savedIsError = otp.isError
    }

    /// Rebuilds the saved state once; anything missing or invalid keeps the fresh state.
    private func restoreIfSaved() {
        guard !didRestore, !Self.skipRestore else { return }
        didRestore = true
        guard let length = OtpLength.companion.fromName(name: savedLength),
              let charset = OtpCharset.companion.fromName(name: savedCharset),
              let restored = OtpState.companion.restore(
                  length: length, charset: charset, code: savedCode, isError: savedIsError
              )
        else { return }
        options.length = length
        options.alphanumeric = charset == .alphanumeric
        otp = restored
    }

    private var codeUnit: String { options.alphanumeric ? "character" : "digit" }

    private var fieldStyle: OtpFieldStyle {
        switch (options.roundCells, options.glassField) {
        case (false, false): Self.fieldStyle
        case (true, false): Self.roundFieldStyle
        case (false, true): Self.glassFieldStyle
        case (true, true): Self.glassRoundFieldStyle
        }
    }

    private var buttonStyle: OtpButtonStyle {
        options.glassButtons ? Self.glassButtonStyle : Self.buttonStyle
    }

    /// Called on completion and by the Verify button.
    private func verify(_ code: String) {
        guard !isVerified else { return }
        if SampleOtpKt.isSampleCodeRejected(code: code) {
            otp = otp.withInput(text: code).state.withError()
        } else {
            isVerified = true
            // Delayed so it is not cut off by the speech for the last digit typed.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                UIAccessibility.post(notification: .announcement, argument: "Code verified")
            }
        }
    }

    private func resend() {
        otp = otp.cleared()
        isVerified = false
        secondsRemaining = Self.resendSeconds
        countdownRun += 1
    }
}

#Preview("Default") {
    ContentView()
}

#Preview("Round cells, masked") {
    ContentView(options: SampleOptions(maskIndex: 1, roundCells: true))
}

// Accessibility checks: large Dynamic Type wraps the cells, and RTL keeps the digits left to right.
#Preview("Accessibility text size") {
    ContentView().environment(\.dynamicTypeSize, .accessibility3)
}

#Preview("RTL, 8 digits") {
    ContentView(options: SampleOptions(length: .eight))
        .environment(\.layoutDirection, .rightToLeft)
}
