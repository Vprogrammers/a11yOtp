import SwiftUI

/// Where the code row sits inside the available width. `start` and `end` follow the surrounding
/// layout direction (start is on the right in right-to-left layouts). The digits themselves are
/// always laid out left to right.
enum OtpAlignment: Hashable {
    case start
    case center
    case end

    /// Alignment for a frame that lives in the surrounding layout direction.
    var frameAlignment: Alignment {
        switch self {
        case .start: .leading
        case .center: .center
        case .end: .trailing
        }
    }

    /// 0 = physical left, 1 = physical right.
    func physicalFraction(layoutDirection: LayoutDirection) -> CGFloat {
        switch (self, layoutDirection) {
        case (.center, _): 0.5
        case (.start, .leftToRight), (.end, .rightToLeft): 0
        default: 1
        }
    }
}

/// How entered digits are shown. Masking applies to the visual cells and to the accessibility value.
enum OtpMask: Hashable {
    /// Digits are visible and readable by VoiceOver.
    case none
    /// Each entered digit is replaced by a character. Uses a secure text field internally.
    case character(Character = "*")
    /// Entered cells are filled with their current border color, with no glyph. Focus and error
    /// stay visible through border width and a contrasting inset ring, not only hue.
    case filledCell

    var isMasked: Bool { self != .none }
}

struct OtpFieldColors: Hashable {
    var text: Color
    var cellBackground: Color
    var cellBorder: Color
    var focusedCellBorder: Color
    var errorCellBorder: Color
    var errorText: Color
    var placeholder: Color
    var disabledText: Color
    var disabledCellBackground: Color
    var disabledCellBorder: Color
    var disabledPlaceholder: Color

    static let `default` = OtpFieldColors(
        text: .primary,
        cellBackground: Color(.systemBackground),
        cellBorder: Color(.systemGray),
        focusedCellBorder: .primary,
        errorCellBorder: otpErrorColor,
        errorText: otpErrorColor,
        placeholder: Color(.systemGray),
        disabledText: Color(.tertiaryLabel),
        disabledCellBackground: Color(.systemGray6),
        disabledCellBorder: Color(.systemGray3),
        disabledPlaceholder: Color(.systemGray3)
    )

    /// Contrast warnings for these colors, or an empty array. Disabled colors are exempt (WCAG 1.4.3 and
    /// 1.4.11 do not apply to inactive components). Pass the environment to evaluate dynamic colors in
    /// dark mode or increased contrast.
    func contrastWarnings(in environment: EnvironmentValues = EnvironmentValues()) -> [String] {
        let background = "cellBackground"
        let checks: [OtpContrastAdapter.Check] = [
            .init(foregroundName: "text", foreground: text, backgroundName: background,
                  background: cellBackground, requirement: .text),
            .init(foregroundName: "errorText", foreground: errorText, backgroundName: background,
                  background: cellBackground, requirement: .text),
            .init(foregroundName: "cellBorder", foreground: cellBorder, backgroundName: background,
                  background: cellBackground, requirement: .nonText),
            .init(foregroundName: "errorCellBorder", foreground: errorCellBorder, backgroundName: background,
                  background: cellBackground, requirement: .nonText),
            .init(foregroundName: "placeholder", foreground: placeholder, backgroundName: background,
                  background: cellBackground, requirement: .nonText),
            .init(foregroundName: "focusedCellBorder", foreground: focusedCellBorder,
                  backgroundName: background, background: cellBackground, requirement: .nonText),
            .init(foregroundName: "focusedCellBorder", foreground: focusedCellBorder,
                  backgroundName: "cellBorder", background: cellBorder, requirement: .focusIndicator)
        ]
        return OtpContrastAdapter.warnings(owner: "OtpFieldColors", checks: checks, in: environment)
    }
}

private let otpErrorColor = Color(UIColor { traits in
    let isDark = traits.userInterfaceStyle == .dark
    if traits.accessibilityContrast == .high {
        return isDark
            ? UIColor(red: 1.0, green: 0.72, blue: 0.68, alpha: 1)
            : UIColor(red: 0.55, green: 0, blue: 0.08, alpha: 1)
    }
    return isDark
        ? UIColor(red: 1.0, green: 0.54, blue: 0.5, alpha: 1)
        : UIColor(red: 0.69, green: 0, blue: 0.125, alpha: 1)
})

struct OtpFieldStyle {
    /// Digit font. Use a text style or `relativeTo:` so it scales with Dynamic Type.
    var font: Font
    /// Shape of every cell. Any `Shape` works, wrapped in `AnyShape`.
    var shape: AnyShape
    /// Minimum cell width and height in points at the default text size, both coerced to at least 44.
    /// They scale with Dynamic Type.
    var minCellWidth: CGFloat
    var minCellHeight: CGFloat
    var spacing: CGFloat
    var borderWidth: CGFloat
    var focusedBorderWidth: CGFloat
    /// Number of digits per decorative group, or nil for one group.
    var groupSize: Int?
    /// Decorative glyph shown in empty cells, or nil for none. It is hidden from assistive technologies.
    /// Keep at least 3:1 against `cellBackground` (WCAG 1.4.11), which the contrast check enforces by warning.
    var placeholder: Character?
    var colors: OtpFieldColors
    /// Text style whose Dynamic Type scale is applied to the cell size. Pick the style closest to `font`
    /// (default `.title`, matching the default font) so a larger custom font cannot clip in its cell.
    var scalingTextStyle: Font.TextStyle

    static let minimumTarget: CGFloat = 44

    init(
        font: Font = .system(.title, design: .monospaced, weight: .semibold),
        shape: AnyShape = AnyShape(RoundedRectangle(cornerRadius: 8)),
        minCellWidth: CGFloat = 48,
        minCellHeight: CGFloat = 56,
        spacing: CGFloat = 8,
        borderWidth: CGFloat = 1,
        focusedBorderWidth: CGFloat = 3,
        groupSize: Int? = nil,
        placeholder: Character? = nil,
        colors: OtpFieldColors = .default,
        scalingTextStyle: Font.TextStyle = .title
    ) {
        precondition(focusedBorderWidth > borderWidth, "focusedBorderWidth must be greater than borderWidth")
        precondition(borderWidth >= 0, "borderWidth must not be negative")
        precondition(groupSize == nil || groupSize ?? 0 > 0, "groupSize must be positive")
        self.font = font
        self.shape = shape
        self.minCellWidth = max(minCellWidth, Self.minimumTarget)
        self.minCellHeight = max(minCellHeight, Self.minimumTarget)
        self.spacing = max(spacing, 0)
        self.borderWidth = borderWidth
        self.focusedBorderWidth = focusedBorderWidth
        self.groupSize = groupSize
        self.placeholder = placeholder
        self.colors = colors
        self.scalingTextStyle = scalingTextStyle
    }

    static let `default` = OtpFieldStyle()

    func contrastWarnings(in environment: EnvironmentValues = EnvironmentValues()) -> [String] {
        colors.contrastWarnings(in: environment)
    }
}
