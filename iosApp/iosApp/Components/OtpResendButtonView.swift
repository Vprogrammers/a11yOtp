import Shared
import SwiftUI

/// SwiftUI wrapper around the Kotlin `OtpResendButton`. The host owns the countdown timer.
struct OtpResendButtonView: UIViewRepresentable {
    let secondsRemaining: Int
    let accessibility: OtpResendAccessibility
    var alignment: OtpAlignment = .center
    var style = Self.defaultStyle
    var onResend: () -> Void

    /// Created once so SwiftUI updates do not look like style changes.
    static let defaultStyle = OtpButtonDefaults.shared.style()

    final class Coordinator {
        var button: OtpResendButton?
        var onResend: () -> Void = {}
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> UIView {
        let button = OtpResendButton(accessibility: accessibility)
        let coordinator = context.coordinator
        coordinator.button = button
        button.onResend = { [weak coordinator] in coordinator?.onResend() }
        configure(button, context: context)
        return button.view
    }

    func updateUIView(_: UIView, context: Context) {
        guard let button = context.coordinator.button else { return }
        configure(button, context: context)
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UIView, context _: Context) -> CGSize? {
        let width = proposal.width.flatMap { $0.isFinite ? $0 : nil } ?? 320
        let fitted = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: width, height: fitted.height)
    }

    private func configure(_ button: OtpResendButton, context: Context) {
        context.coordinator.onResend = onResend
        button.accessibility = accessibility
        button.alignment = alignment
        button.style = style
        button.secondsRemaining = Int32(secondsRemaining)
    }
}
