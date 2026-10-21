package me.vprogrammers.a11yotp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual style of an [OtpField].
 *
 * [cellWidth] and [cellHeight] are minimum sizes: cells grow with the system font scale.
 * Sizes below 48dp are raised to 48dp. [focusedBorderWidth] must be larger than
 * [borderWidth] so focus and error are never communicated by color alone.
 * [groupSize] draws a decorative separator between groups of digits (for example 3 gives
 * "123 - 456") without changing the code. [placeholder] is drawn in empty cells.
 * [errorTextStyle] styles the visible error message (sp, so it follows font scale).
 * Create instances with [OtpFieldDefaults.style].
 */
@Immutable
class OtpFieldStyle internal constructor(
    val textStyle: TextStyle,
    val cellShape: Shape,
    cellWidth: Dp,
    cellHeight: Dp,
    val cellSpacing: Dp,
    val borderWidth: Dp,
    val focusedBorderWidth: Dp,
    val colors: OtpFieldColors,
    val groupSize: Int? = null,
    val placeholder: Char? = null,
    val errorTextStyle: TextStyle = TextStyle(fontSize = 14.sp),
) {
    /** Minimum cell width, never below 48dp. */
    val cellWidth: Dp = cellWidth.coerceAtLeast(OtpFieldDefaults.MinTargetSize)

    /** Minimum cell height, never below 48dp. */
    val cellHeight: Dp = cellHeight.coerceAtLeast(OtpFieldDefaults.MinTargetSize)

    init {
        require(focusedBorderWidth > borderWidth) { "focusedBorderWidth must be larger than borderWidth" }
        require(groupSize == null || groupSize > 0) { "groupSize must be positive" }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is OtpFieldStyle &&
                    textStyle == other.textStyle &&
                    cellShape == other.cellShape &&
                    cellWidth == other.cellWidth &&
                    cellHeight == other.cellHeight &&
                    cellSpacing == other.cellSpacing &&
                    borderWidth == other.borderWidth &&
                    focusedBorderWidth == other.focusedBorderWidth &&
                    colors == other.colors &&
                    groupSize == other.groupSize &&
                    placeholder == other.placeholder &&
                    errorTextStyle == other.errorTextStyle
            )

    override fun hashCode(): Int {
        var result = textStyle.hashCode()
        result = 31 * result + cellShape.hashCode()
        result = 31 * result + cellWidth.hashCode()
        result = 31 * result + cellHeight.hashCode()
        result = 31 * result + cellSpacing.hashCode()
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + focusedBorderWidth.hashCode()
        result = 31 * result + colors.hashCode()
        result = 31 * result + groupSize.hashCode()
        result = 31 * result + placeholder.hashCode()
        result = 31 * result + errorTextStyle.hashCode()
        return result
    }
}

/**
 * Colors of an [OtpField], created with [OtpFieldDefaults.colors]. Checked pairs (opaque colors
 * only; masked [OtpMask.FilledCell] cells use the border colors):
 * - [text] and [errorText] against [cellBackground], 4.5:1 (WCAG 1.4.3);
 * - [cellBorder], [focusedCellBorder], [errorCellBorder] and [placeholder] against
 *   [cellBackground], 3:1 (WCAG 1.4.11);
 * - [focusedCellBorder] against [cellBorder], 3:1 (WCAG 2.4.13).
 *
 * Violations are logged and listed in [contrastWarnings] but never thrown, so brand colors
 * remain usable. Disabled colors are exempt.
 */
@Immutable
class OtpFieldColors internal constructor(
    val text: Color,
    val cellBackground: Color,
    val cellBorder: Color,
    val focusedCellBorder: Color,
    val errorCellBorder: Color,
    val errorText: Color,
    val disabledText: Color,
    val disabledCellBackground: Color,
    val disabledCellBorder: Color,
    val placeholder: Color = Color(0xFF767676),
) {
    /**
     * Contrast problems found at construction, empty when compliant. Contrast is warned
     * (and logged once per instance), not enforced, so brand colors remain usable.
     */
    val contrastWarnings: List<String> =
        reportContrast(
            listOf(
                contrastWarning(OWNER, "text", text, BG, cellBackground, OtpContrastRequirement.TEXT),
                contrastWarning(OWNER, "errorText", errorText, BG, cellBackground, OtpContrastRequirement.TEXT),
                contrastWarning(OWNER, "cellBorder", cellBorder, BG, cellBackground, OtpContrastRequirement.NON_TEXT),
                contrastWarning(
                    OWNER,
                    "focusedCellBorder",
                    focusedCellBorder,
                    BG,
                    cellBackground,
                    OtpContrastRequirement.NON_TEXT,
                ),
                contrastWarning(
                    OWNER,
                    "errorCellBorder",
                    errorCellBorder,
                    BG,
                    cellBackground,
                    OtpContrastRequirement.NON_TEXT,
                ),
                contrastWarning(OWNER, "placeholder", placeholder, BG, cellBackground, OtpContrastRequirement.NON_TEXT),
                contrastWarning(
                    OWNER,
                    "focusedCellBorder",
                    focusedCellBorder,
                    "cellBorder",
                    cellBorder,
                    OtpContrastRequirement.FOCUS_INDICATOR,
                ),
            ),
        )

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is OtpFieldColors &&
                    text == other.text &&
                    cellBackground == other.cellBackground &&
                    cellBorder == other.cellBorder &&
                    focusedCellBorder == other.focusedCellBorder &&
                    errorCellBorder == other.errorCellBorder &&
                    errorText == other.errorText &&
                    disabledText == other.disabledText &&
                    disabledCellBackground == other.disabledCellBackground &&
                    disabledCellBorder == other.disabledCellBorder &&
                    placeholder == other.placeholder
            )

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + cellBackground.hashCode()
        result = 31 * result + cellBorder.hashCode()
        result = 31 * result + focusedCellBorder.hashCode()
        result = 31 * result + errorCellBorder.hashCode()
        result = 31 * result + errorText.hashCode()
        result = 31 * result + disabledText.hashCode()
        result = 31 * result + disabledCellBackground.hashCode()
        result = 31 * result + disabledCellBorder.hashCode()
        result = 31 * result + placeholder.hashCode()
        return result
    }
}

private const val OWNER = "OtpFieldColors"
private const val BG = "cellBackground"

/** Default styles for [OtpField]. */
object OtpFieldDefaults {
    /** Minimum touch target dimension for the whole field. */
    internal val MinTargetSize: Dp = 48.dp

    fun style(
        textStyle: TextStyle =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
            ),
        cellShape: Shape = RoundedCornerShape(8.dp),
        cellWidth: Dp = MinTargetSize,
        cellHeight: Dp = MinTargetSize,
        cellSpacing: Dp = 8.dp,
        borderWidth: Dp = 1.dp,
        focusedBorderWidth: Dp = 3.dp,
        colors: OtpFieldColors = colors(),
        groupSize: Int? = null,
        placeholder: Char? = null,
        errorTextStyle: TextStyle = TextStyle(fontSize = 14.sp),
    ): OtpFieldStyle =
        OtpFieldStyle(
            textStyle = textStyle,
            cellShape = cellShape,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            cellSpacing = cellSpacing,
            borderWidth = borderWidth,
            focusedBorderWidth = focusedBorderWidth,
            colors = colors,
            groupSize = groupSize,
            placeholder = placeholder,
            errorTextStyle = errorTextStyle,
        )

    fun colors(
        text: Color = Color(0xFF1B1B1F),
        cellBackground: Color = Color.White,
        cellBorder: Color = Color(0xFF767676),
        focusedCellBorder: Color = Color(0xFF001F4D),
        errorCellBorder: Color = Color(0xFFB3261E),
        errorText: Color = Color(0xFFB3261E),
        disabledText: Color = Color(0xFF6E6E6E),
        disabledCellBackground: Color = Color(0xFFF0F0F0),
        disabledCellBorder: Color = Color(0xFFBDBDBD),
        placeholder: Color = Color(0xFF767676),
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
