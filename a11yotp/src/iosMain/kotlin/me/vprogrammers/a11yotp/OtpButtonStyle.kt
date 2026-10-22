package me.vprogrammers.a11yotp

import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyle
import platform.UIKit.UIFontTextStyleHeadline
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UITraitCollection
import platform.UIKit.currentTraitCollection
import platform.UIKit.labelColor
import platform.UIKit.secondaryLabelColor
import platform.UIKit.secondarySystemBackgroundColor
import platform.UIKit.systemBackgroundColor
import platform.UIKit.systemGray3Color
import platform.UIKit.systemGrayColor

/**
 * Colors of an OTP button, created with [OtpButtonDefaults.colors]. [content] should have at
 * least 4.5:1 contrast against [container] (WCAG 1.4.3); violations are logged once and listed in
 * [contrastWarnings], never thrown, so brand colors remain usable. The disabled pair is exempt.
 */
class OtpButtonColors internal constructor(
    val content: UIColor,
    val container: UIColor,
    val border: UIColor,
    val disabledContent: UIColor,
    val disabledContainer: UIColor,
    val disabledBorder: UIColor,
) {
    init {
        OtpContrastLog.log(contrastWarnings)
    }

    /** Contrast problems for the current trait collection, empty when compliant. */
    val contrastWarnings: List<String> get() = contrastWarningsIn(UITraitCollection.currentTraitCollection)

    /** Contrast problems when the colors are resolved for [traits], for example dark mode or Increase Contrast. */
    fun contrastWarningsIn(traits: UITraitCollection): List<String> =
        listOfNotNull(
            contrastWarning(
                "OtpButtonColors",
                "content",
                content,
                "container",
                container,
                OtpContrastRequirement.TEXT,
                traits,
            ),
        )
}

/**
 * Style of [OtpVerifyButton] and [OtpResendButton], created with [OtpButtonDefaults.style].
 * [minHeight] is in points at the default text size, scales with Dynamic Type, and is raised to
 * 44. A disabled button always draws a dashed border of at least 1 point, so its state is not
 * shown by color alone.
 *
 * [glass] opts in to the iOS 26 Liquid Glass button background (a capsule) while the button is
 * enabled. Earlier iOS versions, and disabled buttons, keep the solid style. Glass is translucent,
 * so the [colors] contrast check cannot cover it; the system makes the glass opaque when Reduce
 * Transparency or Increase Contrast is on. Use [withGlass] to turn it on.
 */
class OtpButtonStyle internal constructor(
    val font: UIFont,
    val fontTextStyle: UIFontTextStyle,
    val cornerRadius: Double,
    minHeight: Double,
    val horizontalPadding: Double,
    val verticalPadding: Double,
    val borderWidth: Double,
    val colors: OtpButtonColors,
    val glass: Boolean = false,
) {
    val minHeight: Double = maxOf(minHeight, OtpFieldDefaults.MIN_TARGET_SIZE)

    init {
        require(borderWidth >= 0.0) { "borderWidth must not be negative" }
        require(horizontalPadding >= 0.0 && verticalPadding >= 0.0) { "padding must not be negative" }
        require(cornerRadius >= 0.0) { "cornerRadius must not be negative" }
    }

    /** Contrast problems of [colors] for the current trait collection. */
    val contrastWarnings: List<String> get() = colors.contrastWarnings

    /** Copy with [glass] on or off; Swift cannot use Kotlin default arguments. */
    fun withGlass(glass: Boolean): OtpButtonStyle =
        OtpButtonStyle(
            font,
            fontTextStyle,
            cornerRadius,
            minHeight,
            horizontalPadding,
            verticalPadding,
            borderWidth,
            colors,
            glass,
        )
}

/**
 * Default styles for [OtpVerifyButton] and [OtpResendButton]. From Swift, call for example
 * `OtpButtonDefaults.shared.style()`.
 */
object OtpButtonDefaults {
    /** Style with all defaults. */
    fun style(): OtpButtonStyle = style(colors = colors())

    fun style(
        font: UIFont = UIFont.systemFontOfSize(17.0, weight = UIFontWeightSemibold),
        fontTextStyle: UIFontTextStyle = UIFontTextStyleHeadline,
        cornerRadius: Double = 12.0,
        minHeight: Double = 48.0,
        horizontalPadding: Double = 20.0,
        verticalPadding: Double = 10.0,
        borderWidth: Double = 1.0,
        colors: OtpButtonColors = colors(),
    ): OtpButtonStyle =
        OtpButtonStyle(
            font,
            fontTextStyle,
            cornerRadius,
            minHeight,
            horizontalPadding,
            verticalPadding,
            borderWidth,
            colors,
        )

    /** Dynamic system colors that adapt to dark mode and Increase Contrast. */
    fun colors(): OtpButtonColors = colors(content = UIColor.labelColor())

    fun colors(
        content: UIColor = UIColor.labelColor(),
        container: UIColor = UIColor.secondarySystemBackgroundColor(),
        border: UIColor = UIColor.systemGrayColor(),
        disabledContent: UIColor = UIColor.secondaryLabelColor(),
        disabledContainer: UIColor = UIColor.systemBackgroundColor(),
        disabledBorder: UIColor = UIColor.systemGray3Color(),
    ): OtpButtonColors = OtpButtonColors(content, container, border, disabledContent, disabledContainer, disabledBorder)
}
