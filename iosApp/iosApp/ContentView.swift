import Shared
import SwiftUI

/// Demo screen. Strings are hard-coded English for the sample only; the library requires host-provided strings.
///
/// Fake verification: only "000000" (or its 4 and 8 digit forms) fails. The options below the main flow
/// exercise code length, masking, round cells, and the disabled state.
struct ContentView: View {
    private static let resendSeconds = 30

    private enum LengthOption: String, CaseIterable, Identifiable {
        case four = "4"
        case six = "6"
        case eight = "8"

        var id: String { rawValue }

        var length: OtpLength {
            switch self {
            case .four: .four
            case .six: .six
            case .eight: .eight
            }
        }
    }

    private enum MaskOption: String, CaseIterable, Identifiable {
        case none = "None"
        case character = "Character"
        case filledCell = "Filled cell"

        var id: String { rawValue }

        var mask: OtpMask {
            switch self {
            case .none: OtpMaskNone.shared
            case .character: OtpMaskCharacter(char: 0x2022)
            case .filledCell: OtpMaskFilledCell.shared
            }
        }
    }

    @State private var otp = SampleOtpKt.sampleInitialOtpState()
    @State private var lengthOption = LengthOption.six
    @State private var maskOption = MaskOption.none
    @State private var isDisabled = false
    @State private var roundCells = false
    @State private var lastVerifiedCode: String?
    @State private var isVerified = false
    @State private var secondsRemaining = ContentView.resendSeconds
    @State private var countdownRun = 0

    /// Groups of 3 give "123 - 456" for 6 digits. The library draws no separator for 4 and 8 digits,
    /// which do not split evenly into groups of 3.
    private static let fieldStyle = fieldStyle(cornerRadius: 8, cellWidth: 48, cellHeight: 56)

    /// Square cells with a radius capped at half their size are drawn as circles.
    private static let roundFieldStyle = fieldStyle(
        cornerRadius: .greatestFiniteMagnitude,
        cellWidth: 56,
        cellHeight: 56
    )

    private static func fieldStyle(cornerRadius: Double, cellWidth: Double, cellHeight: Double) -> OtpFieldStyle {
        OtpFieldDefaults.shared.style(
            font: UIFont.monospacedSystemFont(ofSize: 28, weight: .semibold),
            fontTextStyle: UIFont.TextStyle.title1.rawValue,
            errorTextStyle: UIFont.TextStyle.callout.rawValue,
            cornerRadius: cornerRadius,
            minCellWidth: cellWidth,
            minCellHeight: cellHeight,
            cellSpacing: 8,
            borderWidth: 1,
            focusedBorderWidth: 3,
            colors: OtpFieldDefaults.shared.colors(),
            groupSize: 3,
            placeholder: nil
        )
    }

    /// Dark green in light mode (about 9:1 on white), light green in dark mode (about 12:1 on black).
    /// Both clear 4.5:1 in standard and Increase Contrast appearances.
    private static let successColor = UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0.55, green: 0.85, blue: 0.55, alpha: 1)
            : UIColor(red: 0.11, green: 0.37, blue: 0.13, alpha: 1)
    }

    private static let buttonStyle = OtpButtonDefaults.shared.style(
        font: UIFont.systemFont(ofSize: 18, weight: .bold),
        fontTextStyle: UIFont.TextStyle.headline.rawValue,
        cornerRadius: 24,
        minHeight: 48,
        horizontalPadding: 20,
        verticalPadding: 10,
        borderWidth: 1,
        colors: OtpButtonDefaults.shared.colors(
            content: .white,
            // The dark container keeps white text above 9:1; the border turns light in dark mode so the
            // button edge keeps 3:1 against the background.
            container: UIColor(red: 0.11, green: 0.37, blue: 0.13, alpha: 1),
            border: successColor,
            disabledContent: .secondaryLabel,
            disabledContainer: .systemGray6,
            disabledBorder: .systemGray
        )
    )

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Enter the \(lengthOption.rawValue)-digit code we sent you")
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)

                OtpFieldView(
                    state: $otp,
                    accessibility: OtpFieldAccessibility(
                        label: "Verification code",
                        errorMessage: "Incorrect code. Try again.",
                        progressDescription: { progress in
                            "\(progress.entered) of \(progress.total) digits entered"
                        }
                    ),
                    style: roundCells ? Self.roundFieldStyle : Self.fieldStyle,
                    mask: maskOption.mask,
                    alignment: .center,
                    onComplete: verify
                )
                .disabled(isVerified || isDisabled)

                OtpVerifyButtonView(
                    state: otp,
                    accessibility: OtpVerifyAccessibility(label: "Verify"),
                    style: Self.buttonStyle,
                    onVerify: verify
                )
                .disabled(otp.isError || isVerified)

                if isVerified {
                    Label("Code verified", systemImage: "checkmark.circle.fill")
                        .font(.headline)
                        .foregroundStyle(Color(Self.successColor))
                }

                OtpResendButtonView(
                    secondsRemaining: secondsRemaining,
                    accessibility: OtpResendAccessibility(
                        label: "Resend code",
                        countdownDescription: { countdown in
                            "Resend code in \(countdown.secondsRemaining) s"
                        }
                    ),
                    style: Self.buttonStyle,
                    onResend: resend
                )

                options
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
        .onChange(of: lengthOption) { _, option in
            otp = OtpState(length: option.length)
            lastVerifiedCode = nil
            isVerified = false
        }
        .task(id: countdownRun) { await runCountdown() }
    }

    private var options: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Options").font(.headline)
            Picker("Code length", selection: $lengthOption) {
                ForEach(LengthOption.allCases) { Text($0.rawValue).tag($0) }
            }
            .pickerStyle(.segmented)
            Picker("Masking", selection: $maskOption) {
                ForEach(MaskOption.allCases) { Text($0.rawValue).tag($0) }
            }
            .pickerStyle(.segmented)
            Toggle("Round cells", isOn: $roundCells)
            Toggle("Disable field", isOn: $isDisabled)
        }
    }

    /// Runs once per complete code, whether it comes from the field or the button.
    private func verify(_ code: String) {
        guard lastVerifiedCode != code else { return }
        lastVerifiedCode = code
        if code.allSatisfy({ $0 == "0" }) {
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
