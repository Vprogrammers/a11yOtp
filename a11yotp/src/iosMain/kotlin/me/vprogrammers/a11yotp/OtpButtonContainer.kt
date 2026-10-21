@file:OptIn(ExperimentalForeignApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.QuartzCore.CAShapeLayer
import platform.UIKit.NSLineBreakByWordWrapping
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIBezierPath
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIFontMetrics
import platform.UIKit.UILabel
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIView
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setIsAccessibilityElement

/** Everything the button container needs to draw one frame. */
internal class OtpButtonModel(
    val text: String,
    val isEnabled: Boolean,
    val alignment: OtpAlignment,
    val style: OtpButtonStyle,
)

/** The border width actually drawn: a disabled button keeps at least a 1 point border. */
internal fun otpButtonBorderWidth(
    style: OtpButtonStyle,
    isEnabled: Boolean,
): Double = if (isEnabled) style.borderWidth else maxOf(style.borderWidth, 1.0)

/**
 * The button itself. Its title is a separate label so the layout is fully deterministic; the
 * button carries the accessible name (the same text), the button trait, and the dimmed state.
 */
internal class OtpButtonControl : UIButton(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val titleView = UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
    private val borderLayer = CAShapeLayer()
    private var cornerRadius = 0.0
    private var borderWidth = 0.0
    private var insetX = 0.0
    private var insetY = 0.0

    init {
        titleView.setIsAccessibilityElement(false)
        titleView.userInteractionEnabled = false
        titleView.numberOfLines = 0
        titleView.lineBreakMode = NSLineBreakByWordWrapping
        titleView.textAlignment = NSTextAlignmentCenter
        titleView.adjustsFontForContentSizeCategory = true
        addSubview(titleView)
        borderLayer.fillColor = null
        layer.addSublayer(borderLayer)
    }

    fun apply(
        model: OtpButtonModel,
        traits: UITraitCollection,
    ) {
        val style = model.style
        val colors = style.colors
        val enabled = model.isEnabled
        val glass = enabled && style.glass && isLiquidGlassAvailable()
        setEnabled(enabled)
        configuration =
            if (glass) {
                UIButtonConfiguration.glassButtonConfiguration().apply {
                    cornerStyle = UIButtonConfigurationCornerStyleCapsule
                }
            } else {
                null
            }
        setAccessibilityLabel(model.text)
        titleView.text = model.text
        titleView.font =
            UIFontMetrics(forTextStyle = style.fontTextStyle).scaledFontForFont(
                style.font,
                compatibleWithTraitCollection = traits,
            )
        titleView.textColor =
            (if (enabled) colors.content else colors.disabledContent).resolvedColorWithTraitCollection(traits)
        // With glass the configuration draws the background; the solid fill and border are off.
        layer.backgroundColor =
            if (glass) {
                null
            } else {
                (if (enabled) colors.container else colors.disabledContainer)
                    .resolvedColorWithTraitCollection(traits)
                    .CGColor
            }
        layer.cornerRadius = if (glass) 0.0 else style.cornerRadius
        cornerRadius = style.cornerRadius
        borderLayer.hidden = glass
        borderWidth = otpButtonBorderWidth(style, enabled)
        insetX = style.horizontalPadding
        insetY = style.verticalPadding
        borderLayer.strokeColor =
            (if (enabled) colors.border else colors.disabledBorder).resolvedColorWithTraitCollection(traits).CGColor
        borderLayer.lineWidth = borderWidth
        // Dashed when disabled so the state is not shown by color alone.
        borderLayer.lineDashPattern = if (enabled) null else listOf(NSNumber(double = DASH), NSNumber(double = GAP))
        setNeedsLayout()
    }

    /** Size of the button for at most [maxWidth] points and the given [minHeight]. */
    fun fittingSize(
        maxWidth: Double,
        minHeight: Double,
    ): Pair<Double, Double> {
        val available = (maxWidth - 2 * insetX).coerceAtLeast(1.0)
        val (labelWidth, labelHeight) =
            titleView.sizeThatFits(CGSizeMake(available, UNBOUNDED)).useContents {
                width to
                    height
            }
        val width = minOf(maxOf(labelWidth + 2 * insetX, OtpFieldDefaults.MIN_TARGET_SIZE), maxWidth)
        return width to maxOf(minHeight, labelHeight + 2 * insetY)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // A button configuration adds its own background view; keep the title above it.
        bringSubviewToFront(titleView)
        val (width, height) = bounds.useContents { size.width to size.height }
        titleView.setFrame(
            CGRectMake(
                insetX,
                insetY,
                (width - 2 * insetX).coerceAtLeast(0.0),
                (height - 2 * insetY).coerceAtLeast(0.0),
            ),
        )
        borderLayer.frame = CGRectMake(0.0, 0.0, width, height)
        val half = borderWidth / 2.0
        if (width > borderWidth && height > borderWidth) {
            val rect = CGRectMake(half, half, width - borderWidth, height - borderWidth)
            borderLayer.path =
                UIBezierPath
                    .bezierPathWithRoundedRect(
                        rect,
                        cornerRadius = (cornerRadius - half).coerceAtLeast(0.0),
                    ).CGPath
        }
    }
}

/**
 * Full-width host of one [OtpButtonControl] that positions it by [OtpAlignment]. It has no strong
 * reference to the Kotlin owner; [onTraitsChanged] must capture the owner weakly.
 */
internal class OtpButtonContainer : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val control = OtpButtonControl()
    var onTraitsChanged: (() -> Unit)? = null
    private var model: OtpButtonModel? = null

    init {
        setIsAccessibilityElement(false)
        addSubview(control)
    }

    fun render(model: OtpButtonModel) {
        this.model = model
        control.apply(model, traitCollection)
        setNeedsLayout()
        invalidateIntrinsicContentSize()
    }

    private fun minHeight(model: OtpButtonModel): Double =
        UIFontMetrics(forTextStyle = model.style.fontTextStyle)
            .scaledValueForValue(model.style.minHeight, compatibleWithTraitCollection = traitCollection)

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val model = model ?: return CGSizeMake(0.0, OtpFieldDefaults.MIN_TARGET_SIZE)
        val proposed = size.useContents { width }
        val width = if (proposed.isFinite() && proposed > 0.0 && proposed < UNBOUNDED / 2) proposed else UNBOUNDED_WIDTH
        val (buttonWidth, buttonHeight) = control.fittingSize(width, minHeight(model))
        val finite = if (proposed.isFinite() && proposed > 0.0 && proposed < UNBOUNDED / 2) proposed else buttonWidth
        return CGSizeMake(finite, buttonHeight)
    }

    override fun intrinsicContentSize(): CValue<CGSize> {
        val width = bounds.useContents { size.width }
        val height =
            sizeThatFits(
                CGSizeMake(
                    if (width >
                        0.0
                    ) {
                        width
                    } else {
                        UNBOUNDED_WIDTH
                    },
                    UNBOUNDED,
                ),
            ).useContents { height }
        return CGSizeMake(UIViewNoIntrinsicMetric, height)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val model = model ?: return
        val width = bounds.useContents { size.width }
        val (buttonWidth, buttonHeight) = control.fittingSize(width, minHeight(model))
        val rtl =
            effectiveUserInterfaceLayoutDirection ==
                UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft
        val fraction = (model.alignment.absoluteBias(rtl) + 1.0) / 2.0
        control.setFrame(CGRectMake((width - buttonWidth) * fraction, 0.0, buttonWidth, buttonHeight))
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        onTraitsChanged?.invoke()
    }
}

/** Liquid Glass button configurations exist from iOS 26. */
private fun isLiquidGlassAvailable(): Boolean =
    NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 26 }

private const val DASH = 6.0
private const val GAP = 4.0
private const val UNBOUNDED = 100_000.0
private const val UNBOUNDED_WIDTH = 10_000.0
