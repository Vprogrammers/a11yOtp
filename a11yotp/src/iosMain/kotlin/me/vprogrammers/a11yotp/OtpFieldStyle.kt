package me.vprogrammers.a11yotp

import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyle
import platform.UIKit.UIFontTextStyleCallout
import platform.UIKit.UIFontTextStyleTitle1
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UITraitCollection
import platform.UIKit.currentTraitCollection
import platform.UIKit.labelColor
import platform.UIKit.systemBackgroundColor
import platform.UIKit.systemGray3Color
import platform.UIKit.systemGray6Color
import platform.UIKit.systemGrayColor
import platform.UIKit.tertiaryLabelColor

/**
 * Visual style of an [OtpField].
 *
 * Sizes are in points at the default text size and grow with Dynamic Type (scaled with
 * [fontTextStyle]). [minCellWidth] and [minCellHeight] below 44 are raised to 44.
 * [focusedBorderWidth] must be larger than [borderWidth] so focus and error are never
 * communicated by color alone. [groupSize] draws a decorative separator between groups of
 * digits (3 gives "123 - 456") without changing the code; 0 means no grouping. Separators are
 * drawn only when the code length splits into equal groups, so 3 groups a 6-digit code but
 * leaves 4- and 8-digit codes ungrouped. [placeholder]
 * is drawn in empty cells and is hidden from VoiceOver; null or empty means none.
 * [errorTextStyle] is the text style of the visible error message. [cornerRadius] is capped at
 * half the cell size, so a large value (for example `.greatestFiniteMagnitude`) with equal
 * [minCellWidth] and [minCellHeight] gives round cells.
 * [glass] opts in to an iOS 26 Liquid Glass backdrop behind the cell row; the cells stay solid,
 * so the contrast of digits, borders, focus, and errors is unchanged. Earlier iOS versions draw no
 * backdrop. Use [withGlass] to turn it on. [liftFocusedCell] raises the focused cell slightly with a
 * shadow, in addition to its thicker border; the movement is skipped under Reduce Motion. Use
 * [withFocusLift] to turn it off.
 * Create instances with [OtpFieldDefaults.style].
 */
class OtpFieldStyle internal constructor(
    val font: UIFont,
    val fontTextStyle: UIFontTextStyle,
    val errorTextStyle: UIFontTextStyle,
    val cornerRadius: Double,
    minCellWidth: Double,
    minCellHeight: Double,
    val cellSpacing: Double,
    val borderWidth: Double,
    val focusedBorderWidth: Double,
    val colors: OtpFieldColors,
    val groupSize: Int,
    placeholder: String?,
    val glass: Boolean = false,
    val liftFocusedCell: Boolean = true,
) {
    /** Minimum cell width in points at the default text size, never below 44. */
    val minCellWidth: Double = maxOf(minCellWidth, OtpFieldDefaults.MIN_TARGET_SIZE)

    /** Minimum cell height in points at the default text size, never below 44. */
    val minCellHeight: Double = maxOf(minCellHeight, OtpFieldDefaults.MIN_TARGET_SIZE)

    /** Glyph drawn in empty cells, or null. */
    val placeholder: String? = placeholder?.takeIf { it.isNotEmpty() }

    init {
        require(borderWidth >= 0.0) { "borderWidth must not be negative" }
        require(focusedBorderWidth > borderWidth) { "focusedBorderWidth must be larger than borderWidth" }
        require(groupSize >= 0) { "groupSize must not be negative" }
        require(cellSpacing >= 0.0) { "cellSpacing must not be negative" }
        require(cornerRadius >= 0.0) { "cornerRadius must not be negative" }
    }

    /** Contrast problems of [colors] for the current trait collection; empty when compliant. */
    val contrastWarnings: List<String> get() = colors.contrastWarnings

    // Swift cannot use Kotlin default arguments, so these copies change one aspect of a style
    // without restating every parameter of OtpFieldDefaults.style.

    /** Copy with [groupSize]; see [OtpFieldStyle] for when separators are drawn. */
    fun withGroupSize(groupSize: Int): OtpFieldStyle = copy(groupSize = groupSize)

    /** Copy with [cornerRadius]; a value of at least half the cell size gives round cells. */
    fun withCornerRadius(cornerRadius: Double): OtpFieldStyle = copy(cornerRadius = cornerRadius)

    /** Copy with minimum cell [width] and [height] in points; values below 44 are raised to 44. */
    fun withMinCellSize(
        width: Double,
        height: Double,
    ): OtpFieldStyle = copy(minCellWidth = width, minCellHeight = height)

    /** Copy with the Liquid Glass backdrop on or off. */
    fun withGlass(glass: Boolean): OtpFieldStyle = copy(glass = glass)

    /** Copy with the focused cell lift on or off. */
    fun withFocusLift(enabled: Boolean): OtpFieldStyle = copy(liftFocusedCell = enabled)

    private fun copy(
        cornerRadius: Double = this.cornerRadius,
        minCellWidth: Double = this.minCellWidth,
        minCellHeight: Double = this.minCellHeight,
        groupSize: Int = this.groupSize,
        glass: Boolean = this.glass,
        liftFocusedCell: Boolean = this.liftFocusedCell,
    ) = OtpFieldStyle(
        font = font,
        fontTextStyle = fontTextStyle,
        errorTextStyle = errorTextStyle,
        cornerRadius = cornerRadius,
        minCellWidth = minCellWidth,
        minCellHeight = minCellHeight,
        cellSpacing = cellSpacing,
        borderWidth = borderWidth,
        focusedBorderWidth = focusedBorderWidth,
        colors = colors,
        groupSize = groupSize,
        placeholder = placeholder,
        glass = glass,
        liftFocusedCell = liftFocusedCell,
    )
}

/**
 * Colors of an [OtpField], created with [OtpFieldDefaults.colors]. Use dynamic colors (the
 * defaults are) so dark mode and Increase Contrast keep working. Checked pairs, resolved for the
 * trait collection in use (opaque colors only; masked [OtpMask.FilledCell] cells use the border colors):
 * - [text] and [errorText] against [cellBackground], 4.5:1 (WCAG 1.4.3);
 * - [cellBorder], [focusedCellBorder], [errorCellBorder] and [placeholder] against
 *   [cellBackground], 3:1 (WCAG 1.4.11);
 * - [focusedCellBorder] against [cellBorder], 3:1 (WCAG 2.4.13).
 *
 * Violations are logged once and listed in [contrastWarnings] but never thrown, so brand colors
 * remain usable. Disabled colors are exempt.
 */
class OtpFieldColors internal constructor(
    val text: UIColor,
    val cellBackground: UIColor,
    val cellBorder: UIColor,
    val focusedCellBorder: UIColor,
    val errorCellBorder: UIColor,
    val errorText: UIColor,
    val disabledText: UIColor,
    val disabledCellBackground: UIColor,
    val disabledCellBorder: UIColor,
    val placeholder: UIColor,
) {
    init {
        OtpContrastLog.log(contrastWarnings)
    }

    /** Contrast problems for the current trait collection, empty when compliant. */
    val contrastWarnings: List<String> get() = contrastWarningsIn(UITraitCollection.currentTraitCollection)

    /** Contrast problems when the colors are resolved for [traits], for example dark mode or Increase Contrast. */
    fun contrastWarningsIn(traits: UITraitCollection): List<String> {
        fun check(
            foregroundName: String,
            foreground: UIColor,
            backgroundName: String,
            background: UIColor,
            requirement: OtpContrastRequirement,
        ) = contrastWarning(OWNER, foregroundName, foreground, backgroundName, background, requirement, traits)
        return listOfNotNull(
            check("text", text, BG, cellBackground, OtpContrastRequirement.TEXT),
            check("errorText", errorText, BG, cellBackground, OtpContrastRequirement.TEXT),
            check("cellBorder", cellBorder, BG, cellBackground, OtpContrastRequirement.NON_TEXT),
            check("focusedCellBorder", focusedCellBorder, BG, cellBackground, OtpContrastRequirement.NON_TEXT),
            check("errorCellBorder", errorCellBorder, BG, cellBackground, OtpContrastRequirement.NON_TEXT),
            check("placeholder", placeholder, BG, cellBackground, OtpContrastRequirement.NON_TEXT),
            check(
                "focusedCellBorder",
                focusedCellBorder,
                "cellBorder",
                cellBorder,
                OtpContrastRequirement.FOCUS_INDICATOR,
            ),
        )
    }

    private companion object {
        const val OWNER = "OtpFieldColors"
        const val BG = "cellBackground"
    }
}

/**
 * Default styles for [OtpField]. From Swift, call for example `OtpFieldDefaults.shared.style()`;
 * Kotlin default arguments are not visible to Swift, so the zero-argument functions return the defaults.
 */
object OtpFieldDefaults {
    /** Smallest touch target dimension on iOS, in points. */
    internal const val MIN_TARGET_SIZE = 44.0

    /** Style with all defaults. */
    fun style(): OtpFieldStyle = style(colors = colors())

    fun style(
        font: UIFont = UIFont.monospacedSystemFontOfSize(28.0, weight = UIFontWeightSemibold),
        fontTextStyle: UIFontTextStyle = UIFontTextStyleTitle1,
        errorTextStyle: UIFontTextStyle = UIFontTextStyleCallout,
        cornerRadius: Double = 8.0,
        minCellWidth: Double = 48.0,
        minCellHeight: Double = 56.0,
        cellSpacing: Double = 8.0,
        borderWidth: Double = 1.0,
        focusedBorderWidth: Double = 2.0,
        colors: OtpFieldColors = colors(),
        groupSize: Int = 0,
        placeholder: String? = null,
    ): OtpFieldStyle =
        OtpFieldStyle(
            font = font,
            fontTextStyle = fontTextStyle,
            errorTextStyle = errorTextStyle,
            cornerRadius = cornerRadius,
            minCellWidth = minCellWidth,
            minCellHeight = minCellHeight,
            cellSpacing = cellSpacing,
            borderWidth = borderWidth,
            focusedBorderWidth = focusedBorderWidth,
            colors = colors,
            groupSize = groupSize,
            placeholder = placeholder,
        )

    /** Dynamic system colors that adapt to dark mode and Increase Contrast. */
    fun colors(): OtpFieldColors = colors(text = UIColor.labelColor())

    fun colors(
        text: UIColor = UIColor.labelColor(),
        cellBackground: UIColor = UIColor.systemBackgroundColor(),
        cellBorder: UIColor = UIColor.systemGrayColor(),
        focusedCellBorder: UIColor = UIColor.labelColor(),
        errorCellBorder: UIColor = errorColor(),
        errorText: UIColor = errorColor(),
        disabledText: UIColor = UIColor.tertiaryLabelColor(),
        disabledCellBackground: UIColor = UIColor.systemGray6Color(),
        disabledCellBorder: UIColor = UIColor.systemGray3Color(),
        placeholder: UIColor = UIColor.systemGrayColor(),
    ): OtpFieldColors =
        OtpFieldColors(
            text = text,
            cellBackground = cellBackground,
            cellBorder = cellBorder,
            focusedCellBorder = focusedCellBorder,
            errorCellBorder = errorCellBorder,
            errorText = errorText,
            disabledText = disabledText,
            disabledCellBackground = disabledCellBackground,
            disabledCellBorder = disabledCellBorder,
            placeholder = placeholder,
        )
}
