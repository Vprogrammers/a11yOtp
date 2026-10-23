import Shared
import SwiftUI

/// SwiftUI wrapper around the Kotlin `OtpField`. The coordinator keeps the Kotlin object alive.
struct OtpFieldView: UIViewRepresentable {
    @Binding var state: OtpState
    let accessibility: OtpFieldAccessibility
    var doneButtonTitle = "Done"
    var style = Self.defaultStyle
    var mask: OtpMask = OtpMaskNone.shared
    var alignment: OtpAlignment = .center
    var hapticFeedbackOnError = true
    /// Focuses the field once when it appears, so the keyboard and its code suggestion show without a tap.
    var focusOnAppear = false
    var onComplete: (String) -> Void = { _ in }
    /// Called once per rejected paste or autofill whose digit count differs from the field length.
    var onInputRejected: (OtpInputRejection) -> Void = { _ in }

    /// Created once so SwiftUI updates do not look like style changes.
    static let defaultStyle = OtpFieldDefaults.shared.style()

    final class Coordinator {
        var field: OtpField?
        var setState: (OtpState) -> Void = { _ in }
        var onComplete: (String) -> Void = { _ in }
        var onInputRejected: (OtpInputRejection) -> Void = { _ in }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> UIView {
        let field = OtpField(state: state, accessibility: accessibility, doneButtonTitle: doneButtonTitle)
        let coordinator = context.coordinator
        coordinator.field = field
        field.onStateChange = { [weak coordinator] newState in coordinator?.setState(newState) }
        field.onComplete = { [weak coordinator] code in coordinator?.onComplete(code) }
        field.onInputRejected = { [weak coordinator] rejection in coordinator?.onInputRejected(rejection) }
        configure(field, context: context)
        if focusOnAppear {
            // The view must be in a window before it can become first responder.
            DispatchQueue.main.async { [weak field] in field?.focus() }
        }
        return field.view
    }

    func updateUIView(_: UIView, context: Context) {
        guard let field = context.coordinator.field else { return }
        configure(field, context: context)
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UIView, context _: Context) -> CGSize? {
        let width = proposal.width.flatMap { $0.isFinite ? $0 : nil } ?? 320
        let fitted = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: width, height: fitted.height)
    }

    private func configure(_ field: OtpField, context: Context) {
        context.coordinator.setState = { newState in state = newState }
        context.coordinator.onComplete = onComplete
        context.coordinator.onInputRejected = onInputRejected
        field.doneButtonTitle = doneButtonTitle
        field.accessibility = accessibility
        field.style = style
        field.mask = mask
        field.alignment = alignment
        field.hapticFeedbackOnError = hapticFeedbackOnError
        field.isEnabled = context.environment.isEnabled
        field.state = state
    }
}
