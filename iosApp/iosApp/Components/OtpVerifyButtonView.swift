import Shared
import SwiftUI

/// SwiftUI wrapper around the Kotlin `OtpVerifyButton`. It is enabled only while `state` is complete.
struct OtpVerifyButtonView: UIViewRepresentable {
    let state: OtpState
    let accessibility: OtpVerifyAccessibility
    var alignment: OtpAlignment = .center
    var style = Self.defaultStyle
    var onVerify: (String) -> Void

    /// Created once so SwiftUI updates do not look like style changes.
    static let defaultStyle = OtpButtonDefaults.shared.style()

    final class Coordinator {
        var button: OtpVerifyButton?
        var onVerify: (String) -> Void = { _ in }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> UIView {
        let button = OtpVerifyButton(accessibility: accessibility)
        let coordinator = context.coordinator
        coordinator.button = button
        button.onVerify = { [weak coordinator] code in coordinator?.onVerify(code) }
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

    private func configure(_ button: OtpVerifyButton, context: Context) {
        context.coordinator.onVerify = onVerify
        button.accessibility = accessibility
        button.alignment = alignment
        button.style = style
        button.isEnabled = context.environment.isEnabled
        button.state = state
    }
}
