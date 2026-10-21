import SharedLogic
import SwiftUI

struct OtpButtonColors: Hashable {
    var content: Color
    var background: Color
    var border: Color
    var disabledContent: Color
    var disabledBackground: Color
    var disabledBorder: Color

    static let `default` = OtpButtonColors(
        content: .primary,
        background: Color(.secondarySystemBackground),
        border: Color(.systemGray),
        disabledContent: Color(.secondaryLabel),
        disabledBackground: Color(.systemBackground),
        disabledBorder: Color(.systemGray3)
    )
}

/// Appearance of `OtpVerifyButton` and `OtpResendButton`. A disabled button also gets a dashed border
/// so the state is not communicated by color alone.
struct OtpButtonStyle {
    var font: Font
    var shape: AnyShape
    var colors: OtpButtonColors
    var borderWidth: CGFloat
    /// Minimum height in points at the default text size, coerced to at least 44. Scales with Dynamic Type.
    var minHeight: CGFloat
    var horizontalPadding: CGFloat
    var verticalPadding: CGFloat

    init(
        font: Font = .headline,
        shape: AnyShape = AnyShape(RoundedRectangle(cornerRadius: 12)),
        colors: OtpButtonColors = .default,
        borderWidth: CGFloat = 1,
        minHeight: CGFloat = 48,
        horizontalPadding: CGFloat = 20,
        verticalPadding: CGFloat = 10
    ) {
        self.font = font
        self.shape = shape
        self.colors = colors
        self.borderWidth = borderWidth
        self.minHeight = max(minHeight, OtpFieldStyle.minimumTarget)
        self.horizontalPadding = max(horizontalPadding, 0)
        self.verticalPadding = max(verticalPadding, 0)
    }

    static let `default` = OtpButtonStyle()

    func contrastWarnings(in environment: EnvironmentValues = EnvironmentValues()) -> [String] {
        OtpContrastAdapter.warnings(
            owner: "OtpButtonColors",
            checks: [.init(foregroundName: "content", foreground: colors.content,
                           backgroundName: "container", background: colors.background, requirement: .text)],
            in: environment
        )
    }
}

struct OtpVerifyAccessibility {
    /// Visible text and accessible name of the button.
    let label: String

    init(label: String) {
        precondition(!label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, "label must not be blank")
        self.label = label
    }
}

struct OtpResendAccessibility {
    /// Visible text and accessible name when a resend is possible.
    let label: String
    /// Visible text and accessible name during the countdown. It should begin with `label` so the spoken
    /// name contains the visible text (WCAG 2.5.3).
    let countdownDescription: (Int) -> String

    init(label: String, countdownDescription: @escaping (Int) -> String) {
        precondition(!label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, "label must not be blank")
        self.label = label
        self.countdownDescription = countdownDescription
    }
}

/// Enabled only while the code is complete.
struct OtpVerifyButton: View {
    let state: OtpState
    let accessibility: OtpVerifyAccessibility
    var alignment: OtpAlignment = .center
    var style: OtpButtonStyle = .default
    let action: (String) -> Void

    var body: some View {
        OtpButtonBody(
            title: accessibility.label,
            isActive: state.isComplete,
            alignment: alignment,
            style: style
        ) { action(state.code) }
    }
}

/// Disabled while `secondsRemaining` is above zero; the visible text and accessible name then show the countdown.
struct OtpResendButton: View {
    let secondsRemaining: Int
    let accessibility: OtpResendAccessibility
    var alignment: OtpAlignment = .center
    var style: OtpButtonStyle = .default
    let action: () -> Void

    var body: some View {
        OtpButtonBody(
            title: secondsRemaining > 0
                ? accessibility.countdownDescription(secondsRemaining)
                : accessibility.label,
            isActive: secondsRemaining <= 0,
            alignment: alignment,
            style: style,
            action: action
        )
    }
}

private struct OtpButtonBody: View {
    let title: String
    let isActive: Bool
    let alignment: OtpAlignment
    let style: OtpButtonStyle
    let action: () -> Void

    @Environment(\.isEnabled) private var isEnvironmentEnabled
    @Environment(\.self) private var environment
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1

    private var isEnabled: Bool { isActive && isEnvironmentEnabled }
    private var colors: OtpButtonColors { style.colors }

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(style.font)
                .multilineTextAlignment(.center)
                .foregroundStyle(isEnabled ? colors.content : colors.disabledContent)
                .padding(.horizontal, style.horizontalPadding)
                .padding(.vertical, style.verticalPadding)
                .frame(minWidth: OtpFieldStyle.minimumTarget, minHeight: style.minHeight * scale)
                .background(style.shape.fill(isEnabled ? colors.background : colors.disabledBackground))
                .overlay {
                    style.shape
                        .stroke(
                            isEnabled ? colors.border : colors.disabledBorder,
                            style: StrokeStyle(lineWidth: style.borderWidth * 2, dash: isEnabled ? [] : [6, 4])
                        )
                        .clipShape(style.shape)
                }
                .contentShape(style.shape)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .frame(maxWidth: .infinity, alignment: alignment.frameAlignment)
        .onAppear { OtpContrastLog.shared.log(style.contrastWarnings(in: environment)) }
    }
}
