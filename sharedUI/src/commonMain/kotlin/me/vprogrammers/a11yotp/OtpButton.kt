package me.vprogrammers.a11yotp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Colors of an OTP button. [content] should have at least 4.5:1 contrast against [container]
 * (WCAG 1.4.3); violations are logged and listed in [contrastWarnings], never thrown, so brand
 * colors remain usable. The disabled pair is exempt from WCAG contrast requirements.
 */
@Immutable
class OtpButtonColors internal constructor(
    val content: Color,
    val container: Color,
    val border: Color,
    val disabledContent: Color,
    val disabledContainer: Color,
    val disabledBorder: Color,
) {
    val contrastWarnings: List<String> =
        reportContrast(
            listOf(
                contrastWarning(
                    "OtpButtonColors",
                    "content",
                    content,
                    "container",
                    container,
                    OtpContrastRequirement.TEXT,
                ),
            ),
        )

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is OtpButtonColors &&
                    content == other.content &&
                    container == other.container &&
                    border == other.border &&
                    disabledContent == other.disabledContent &&
                    disabledContainer == other.disabledContainer &&
                    disabledBorder == other.disabledBorder
            )

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + container.hashCode()
        result = 31 * result + border.hashCode()
        result = 31 * result + disabledContent.hashCode()
        result = 31 * result + disabledContainer.hashCode()
        result = 31 * result + disabledBorder.hashCode()
        return result
    }
}

/**
 * Style of [OtpVerifyButton] and [OtpResendButton], created with [OtpButtonDefaults.style]. [minHeight] is raised to 48dp. A disabled
 * button always draws at least a 1dp border so its state is not shown by color alone.
 */
@Immutable
class OtpButtonStyle internal constructor(
    val textStyle: TextStyle,
    val shape: Shape,
    minHeight: Dp,
    val contentPadding: PaddingValues,
    val borderWidth: Dp,
    val colors: OtpButtonColors,
) {
    val minHeight: Dp = minHeight.coerceAtLeast(OtpFieldDefaults.MinTargetSize)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is OtpButtonStyle &&
                    textStyle == other.textStyle &&
                    shape == other.shape &&
                    minHeight == other.minHeight &&
                    contentPadding == other.contentPadding &&
                    borderWidth == other.borderWidth &&
                    colors == other.colors
            )

    override fun hashCode(): Int {
        var result = textStyle.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + minHeight.hashCode()
        result = 31 * result + contentPadding.hashCode()
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + colors.hashCode()
        return result
    }
}

/** Required, localized accessibility text for an [OtpVerifyButton]; [label] is also the visible text. */
@Immutable
class OtpVerifyAccessibility(
    val label: String,
) {
    init {
        require(label.isNotBlank()) { "label must not be blank" }
    }
}

/**
 * Button that verifies a complete code. It is enabled only while [state] is complete and
 * [enabled] is true. The host owns verification: [onVerify] receives the code and should not
 * also be triggered by [OtpField.onComplete] for the same code unless it de-duplicates.
 * Pass the same [alignment] as [OtpField] to keep them aligned.
 */
@Composable
fun OtpVerifyButton(
    state: OtpState,
    onVerify: (code: String) -> Unit,
    accessibility: OtpVerifyAccessibility,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alignment: OtpAlignment = OtpAlignment.CENTER,
    style: OtpButtonStyle = OtpButtonDefaults.style(),
) {
    OtpButtonSurface(
        text = accessibility.label,
        enabled = enabled && state.isComplete,
        onClick = { onVerify(state.code) },
        alignment = alignment,
        style = style,
        modifier = modifier,
    )
}

@Composable
internal fun OtpButtonSurface(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    alignment: OtpAlignment,
    style: OtpButtonStyle,
    modifier: Modifier,
) {
    val colors = style.colors
    val borderWidth = if (enabled) style.borderWidth else maxOf(style.borderWidth, 1.dp)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment.absoluteBox(isRtl)) {
        Box(
            modifier =
                Modifier
                    .defaultMinSize(minWidth = OtpFieldDefaults.MinTargetSize, minHeight = style.minHeight)
                    .background(if (enabled) colors.container else colors.disabledContainer, style.shape)
                    .border(borderWidth, if (enabled) colors.border else colors.disabledBorder, style.shape)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                    .padding(style.contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = text,
                style = style.textStyle.copy(color = if (enabled) colors.content else colors.disabledContent),
            )
        }
    }
}

/** Default styles for [OtpVerifyButton] and [OtpResendButton]. */
object OtpButtonDefaults {
    fun style(
        textStyle: TextStyle =
            TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
        cornerRadius: Dp = 8.dp,
        shape: Shape = RoundedCornerShape(cornerRadius),
        minHeight: Dp = OtpFieldDefaults.MinTargetSize,
        contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        borderWidth: Dp = 0.dp,
        colors: OtpButtonColors = colors(),
    ): OtpButtonStyle = OtpButtonStyle(textStyle, shape, minHeight, contentPadding, borderWidth, colors)

    fun colors(
        content: Color = Color.White,
        container: Color = Color(0xFF0B57D0),
        border: Color = container,
        disabledContent: Color = Color(0xFF595959),
        disabledContainer: Color = Color(0xFFE8E8E8),
        disabledBorder: Color = Color(0xFF757575),
    ): OtpButtonColors = OtpButtonColors(content, container, border, disabledContent, disabledContainer, disabledBorder)
}
