import SharedLogic
import SwiftUI

/// Demo screen. Strings are hard-coded English for the sample only; the library requires host-provided strings.
struct ContentView: View {
    private static let resendSeconds = 30
    private static let wrongCode = "000000"

    @State private var otp = OtpState(length: .six)
    @State private var lastVerifiedCode: String?
    @State private var isVerified = false
    @State private var secondsRemaining = ContentView.resendSeconds
    @State private var countdownRun = 0

    private let buttonStyle = OtpButtonStyle(
        font: .headline.bold(),
        shape: AnyShape(RoundedRectangle(cornerRadius: 24)),
        colors: OtpButtonColors(
            content: .white,
            background: Color(red: 0.11, green: 0.37, blue: 0.13),
            border: Color(red: 0.11, green: 0.37, blue: 0.13),
            disabledContent: Color(.secondaryLabel),
            disabledBackground: Color(.systemGray6),
            disabledBorder: Color(.systemGray)
        )
    )

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Enter the 6-digit code we sent you")
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)

                OtpField(
                    state: $otp,
                    accessibility: OtpFieldAccessibility(
                        label: "Verification code",
                        errorMessage: "Incorrect code. Try again.",
                        progressDescription: { entered, total in "\(entered) of \(total) digits entered" },
                        doneButtonTitle: "Done"
                    ),
                    style: OtpFieldStyle(groupSize: 3),
                    alignment: .center,
                    onComplete: verify
                )
                .disabled(isVerified)

                OtpVerifyButton(
                    state: otp,
                    accessibility: OtpVerifyAccessibility(label: "Verify"),
                    style: buttonStyle,
                    action: verify
                )
                .disabled(otp.isError || isVerified)

                if isVerified {
                    Label("Code verified", systemImage: "checkmark.circle.fill")
                        .font(.headline)
                        .foregroundStyle(Color(red: 0.11, green: 0.37, blue: 0.13))
                }

                OtpResendButton(
                    secondsRemaining: secondsRemaining,
                    accessibility: OtpResendAccessibility(
                        label: "Resend code",
                        countdownDescription: { "Resend code in \($0) \($0 == 1 ? "second" : "seconds")" }
                    ),
                    action: resend
                )
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
        .onChange(of: otp.isComplete) { _, isComplete in
            if !isComplete {
                lastVerifiedCode = nil
                isVerified = false
            }
        }
        .task(id: countdownRun) { await runCountdown() }
    }

    /// Runs once per complete code, whether it comes from the field or the button.
    private func verify(_ code: String) {
        guard lastVerifiedCode != code else { return }
        lastVerifiedCode = code
        if code == Self.wrongCode {
            // Build from the passed code instead of reading `otp` right after the field's binding write.
            otp = otp.withInput(text: code).state.withError()
        } else {
            isVerified = true
            var message = AttributedString("Code verified")
            message.accessibilitySpeechAnnouncementPriority = .high
            AccessibilityNotification.Announcement(message).post()
        }
    }

    private func resend() {
        otp = otp.cleared()
        lastVerifiedCode = nil
        isVerified = false
        secondsRemaining = Self.resendSeconds
        countdownRun += 1
    }

    private func runCountdown() async {
        secondsRemaining = max(secondsRemaining, 0)
        while secondsRemaining > 0 {
            try? await Task.sleep(for: .seconds(1))
            if Task.isCancelled { return }
            secondsRemaining -= 1
        }
    }
}

#Preview {
    ContentView()
}
