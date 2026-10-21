import SharedLogic
import SwiftUI
import UIKit

/// One-time-code input. Visually it is a row of cells; for assistive technologies it is one text field.
///
/// A transparent backing text field covers the cell row and is the only accessibility element. Cells,
/// separators and the visible error text are hidden from the accessibility tree. Disable the field with
/// the SwiftUI `.disabled(_:)` modifier.
///
/// Host responsibilities: verify the code on your server, call `state.withError()` on failure, and clear
/// the state when the screen is left or the code expires.
struct OtpField: View {
    @Binding private var state: OtpState
    private let accessibility: OtpFieldAccessibility
    private let style: OtpFieldStyle
    private let mask: OtpMask
    private let alignment: OtpAlignment
    private let hapticFeedbackOnError: Bool
    private let onComplete: (String) -> Void

    @State private var text: String
    @State private var selection: TextSelection?
    /// Set when a masked field gains focus while it holds digits. See `apply(_:)`.
    @State private var didRefocusMasked = false
    @FocusState private var isFocused: Bool
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast
    @Environment(\.self) private var environment
    @ScaledMetric private var scale: CGFloat

    init(
        state: Binding<OtpState>,
        accessibility: OtpFieldAccessibility,
        style: OtpFieldStyle = .default,
        mask: OtpMask = .none,
        alignment: OtpAlignment = .center,
        hapticFeedbackOnError: Bool = true,
        onComplete: @escaping (String) -> Void = { _ in }
    ) {
        _state = state
        _text = State(initialValue: state.wrappedValue.code)
        _scale = ScaledMetric(wrappedValue: 1, relativeTo: style.scalingTextStyle)
        self.accessibility = accessibility
        self.style = style
        self.mask = mask
        self.alignment = alignment
        self.hapticFeedbackOnError = hapticFeedbackOnError
        self.onComplete = onComplete
    }

    private var colors: OtpFieldColors { style.colors }
    private var digitCount: Int { Int(state.length.digitCount) }
    private var code: [Character] { Array(state.code) }
    private var groupSize: Int { min(max(style.groupSize ?? digitCount, 1), digitCount) }
    private var separatorCount: Int { groupSize < digitCount ? (digitCount - 1) / groupSize : 0 }
    private var focusedIndex: Int { min(code.count, digitCount - 1) }

    var body: some View {
        VStack(spacing: 8) {
            cells
                .overlay { input }
                // Taps anywhere on the cell row focus the field, not only on the backing field.
                .background {
                    Color.clear
                        .contentShape(Rectangle())
                        .onTapGesture { isFocused = true }
                        .accessibilityHidden(true)
                }
                .frame(maxWidth: .infinity, alignment: alignment.frameAlignment)
            if state.isError {
                errorLabel
                    .frame(maxWidth: .infinity, alignment: alignment.frameAlignment)
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button(accessibility.doneButtonTitle) { isFocused = false }
            }
        }
        .onChange(of: text) { _, newText in
            apply(newText)
            pinSelectionToEnd()
        }
        .onChange(of: selection) { pinSelectionToEnd() }
        .onChange(of: isFocused) { _, focused in
            // A masked SecureField may clear its text when editing resumes; remember that it held digits.
            didRefocusMasked = focused && mask.isMasked && !state.code.isEmpty
        }
        .onChange(of: state.code) { _, newCode in
            if text != newCode { text = newCode }
        }
        .onChange(of: state.isError) { wasError, isError in
            if !wasError && isError { errorEntered() }
        }
        .onAppear { logContrast() }
        .onChange(of: colorScheme) { logContrast() }
        .onChange(of: colorSchemeContrast) { logContrast() }
    }

    // MARK: Cells

    private var cells: some View {
        OtpCellsLayout(
            count: digitCount,
            groupSize: groupSize,
            cellWidth: style.minCellWidth * scale,
            spacing: style.spacing,
            rowSpacing: style.spacing,
            separatorWidth: 12 * scale,
            fraction: alignment.physicalFraction(layoutDirection: layoutDirection)
        ) {
            ForEach(0..<digitCount, id: \.self) { index in cell(at: index) }
            ForEach(0..<separatorCount, id: \.self) { _ in
                Rectangle()
                    .fill(isEnabled ? colors.cellBorder : colors.disabledCellBorder)
                    .frame(maxWidth: .infinity, maxHeight: max(style.borderWidth, 2))
            }
        }
        // Digits always read left to right, also in right-to-left layouts.
        .environment(\.layoutDirection, .leftToRight)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
        .accessibilityHidden(true)
    }

    private func cell(at index: Int) -> some View {
        let digit: Character? = index < code.count ? code[index] : nil
        let isHighlighted = isEnabled && isFocused && index == focusedIndex
        let borderColor = cellBorderColor(highlighted: isHighlighted)
        let emphasised = isHighlighted || (isEnabled && state.isError)
        let borderWidth = emphasised ? style.focusedBorderWidth : style.borderWidth
        let isFilled = digit != nil && mask == .filledCell
        let background = isEnabled ? colors.cellBackground : colors.disabledCellBackground
        let ringInset: CGFloat = 3

        return ZStack {
            if isFilled {
                style.shape.fill(borderColor)
            } else {
                style.shape.fill(background)
                style.shape.stroke(borderColor, lineWidth: borderWidth * 2).clipShape(style.shape)
            }
            if isFilled && emphasised {
                style.shape
                    .stroke(background, lineWidth: style.focusedBorderWidth)
                    .padding(ringInset + style.focusedBorderWidth / 2)
            }
            glyph(for: digit)
        }
        .frame(maxWidth: .infinity, minHeight: style.minCellHeight * scale)
    }

    @ViewBuilder
    private func glyph(for digit: Character?) -> some View {
        let textColor = isEnabled ? colors.text : colors.disabledText
        if let digit {
            switch mask {
            case .none:
                Text(String(digit)).font(style.font).foregroundStyle(textColor)
            case .character(let character):
                Text(String(character)).font(style.font).foregroundStyle(textColor)
            case .filledCell:
                EmptyView()
            }
        } else if let placeholder = style.placeholder {
            Text(String(placeholder)).font(style.font)
                .foregroundStyle(isEnabled ? colors.placeholder : colors.disabledPlaceholder)
        }
    }

    private func cellBorderColor(highlighted: Bool) -> Color {
        guard isEnabled else { return colors.disabledCellBorder }
        if state.isError { return colors.errorCellBorder }
        return highlighted ? colors.focusedCellBorder : colors.cellBorder
    }

    private var errorLabel: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Image(systemName: "exclamationmark.circle.fill")
                .accessibilityHidden(true)
            Text(accessibility.errorMessage)
                .fixedSize(horizontal: false, vertical: true)
        }
        .font(.callout)
        .foregroundStyle(isEnabled ? colors.errorText : colors.disabledText)
        // The error is part of the field's accessibility value and is announced once on entry.
        .accessibilityHidden(true)
    }

    // MARK: Backing input

    private var input: some View {
        Group {
            if mask.isMasked {
                SecureField("", text: $text)
            } else {
                // iOS 18 selection API: keeps the insertion point at the end (see pinSelectionToEnd()).
                TextField("", text: $text, selection: $selection)
            }
        }
        .focused($isFocused)
        .keyboardType(.numberPad)
        .textContentType(.oneTimeCode)
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
        .foregroundStyle(.clear)
        .tint(.clear)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(Rectangle())
        .accessibilityLabel(accessibility.label)
        .accessibilityValue(accessibilityValueText)
    }

    private var accessibilityValueText: String {
        var parts: [String] = []
        if !mask.isMasked && !state.code.isEmpty {
            parts.append(state.code.map { String($0) }.joined(separator: " "))
        }
        parts.append(accessibility.progressDescription(Int(state.enteredDigitCount), digitCount))
        if state.isError { parts.append(accessibility.errorMessage) }
        return parts.joined(separator: ", ")
    }

    // MARK: Behavior

    /// Keeps the insertion point at the end so mid-string edits cannot reorder digits. The highlighted
    /// cell is the visual caret substitute. `SecureField` has no selection API in iOS 18, so a masked
    /// field cannot be pinned; the masked path relies on `didRefocusMasked` and the write-back instead.
    private func pinSelectionToEnd() {
        guard !mask.isMasked else { return }
        if case .selection(let range)? = selection?.indices, range.isEmpty, range.lowerBound == text.endIndex {
            return
        }
        selection = TextSelection(insertionPoint: text.endIndex)
    }

    /// Handles a change of the backing text.
    ///
    /// Masked-field workaround: UIKit clears a secure text field when editing resumes, so the first
    /// change after refocusing would silently wipe the code. For that first change, the new characters
    /// are treated as appended to the existing code, and an empty text as a backspace. Remaining edge:
    /// if the user selects all and types right after refocus, the new digits are appended too. I could not
    /// observe on a device whether SwiftUI exposes the intermediate empty value; verify on hardware.
    private func apply(_ rawText: String) {
        var newText = rawText
        if didRefocusMasked {
            didRefocusMasked = false
            let existing = state.code
            if mask.isMasked, !existing.isEmpty, !rawText.hasPrefix(existing) {
                newText = rawText.isEmpty ? String(existing.dropLast()) : existing + rawText
            }
        }
        let current = state
        // Paste/autofill over a complete code is handled by the shared withInput(text:).
        let result = current.withInput(text: newText)
        if result.state !== current { state = result.state }
        if text != result.state.code { text = result.state.code }
        if result.didComplete { onComplete(result.state.code) }
    }

    private func errorEntered() {
        // The visible error text is hidden from VoiceOver. Sighted VoiceOver users get the message through
        // the field value and this announcement. High priority keeps it from being dropped by the digit
        // echo or by state changes in the same pass.
        var message = AttributedString(accessibility.errorMessage)
        message.accessibilitySpeechAnnouncementPriority = .high
        AccessibilityNotification.Announcement(message).post()
        if hapticFeedbackOnError {
            UINotificationFeedbackGenerator().notificationOccurred(.error)
        }
    }

    private func logContrast() {
        OtpContrastLog.shared.log(style.contrastWarnings(in: environment))
    }
}
