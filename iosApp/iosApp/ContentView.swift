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

    private static let fieldAccessibility = OtpFieldAccessibility(
        label: "Verification code",
        errorMessage: "Incorrect code. Try again.",
        progressDescription: { "\($0.entered) of \($0.total) digits entered" }
    )
    private static let verifyAccessibility = OtpVerifyAccessibility(label: "Verify")
    private static let resendAccessibility = OtpResendAccessibility(
        label: "Resend code",
        countdownDescription: { "Resend code in \($0.secondsRemaining) s" }
    )

    @State private var otp: OtpState
    @State private var options: SampleOptions
    @State private var isVerified = false
    @State private var secondsRemaining = Int(SampleOtpKt.SAMPLE_RESEND_SECONDS)
    @State private var countdownRun = 0

    init(options: SampleOptions = SampleOptions()) {
        _options = State(initialValue: options)
        _otp = State(initialValue: OtpState(length: options.length))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Enter the \(otp.length.digitCount)-digit code we sent you")
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
                OtpFieldView(
                    state: $otp,
                    accessibility: Self.fieldAccessibility,
                    style: fieldStyle,
                    mask: options.mask,
                    onComplete: verify
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
        .onChange(of: otp.isComplete) { _, isComplete in
            if !isComplete { isVerified = false }
        }
        .onChange(of: options.length) { _, length in
            otp = OtpState(length: length)
            isVerified = false
        }
        .task(id: countdownRun) {
            while secondsRemaining > 0 {
                try? await Task.sleep(for: .seconds(1))
                if Task.isCancelled { return }
                secondsRemaining -= 1
            }
        }
    }

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
        secondsRemaining = Int(SampleOtpKt.SAMPLE_RESEND_SECONDS)
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
