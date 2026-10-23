@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSAttributedString
import platform.Foundation.NSSelectorFromString
import platform.Foundation.create
import platform.UIKit.NSFontAttributeName
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIButtonConfigurationCornerStyleFixed
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIEvent
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleHeadline
import platform.UIKit.UITraitCollection
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.setAccessibilityLabel
import platform.darwin.NSObject

/**
 * Keyboard accessory that adds a Done key above the number pad (which has no Return key).
 *
 * The accessory is transparent and only the button receives touches, so the area around it does not
 * block the one-time-code suggestion or the app content behind it. The button is shaped and placed like
 * a number pad key, right-aligned over the "3" key, with the same 6 pt spacing as the key rows. It
 * uses the system prominent (tint filled) button look, so dark mode, Increase Contrast, and the
 * system tint come from the system. Metrics are measured from the iOS 26 iPhone number pad in
 * portrait and are an approximation; landscape and iPad use the same formula.
 */
internal class OtpDoneAccessory(
    target: NSObject,
    action: String,
    title: String,
) : UIView(frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, MIN_KEY_HEIGHT + 2 * KEY_SPACING)) {
    val button: UIButton
    private var currentTitle: String = title

    /** Visible title and accessibility label of the button. */
    var title: String
        get() = currentTitle
        set(value) {
            currentTitle = value
            applyTitle()
        }

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth
        backgroundColor = null
        button = UIButton.buttonWithConfiguration(buildConfiguration(), primaryAction = null)
        button.addTarget(target, NSSelectorFromString(action), UIControlEventTouchUpInside)
        addSubview(button)
        applyTitle()
    }

    private fun buildConfiguration(): UIButtonConfiguration =
        UIButtonConfiguration.filledButtonConfiguration().also {
            it.cornerStyle = UIButtonConfigurationCornerStyleFixed
            it.background.cornerRadius = KEY_CORNER_RADIUS
        }

    // The attributed title carries the Dynamic Type font, so it is applied again on trait changes.
    private fun applyTitle() {
        val font = UIFont.preferredFontForTextStyle(UIFontTextStyleHeadline)
        val config = button.configuration ?: buildConfiguration()
        config.attributedTitle =
            NSAttributedString.create(
                string = currentTitle,
                attributes = mapOf<Any?, Any?>(NSFontAttributeName to font),
            )
        button.configuration = config
        button.setAccessibilityLabel(currentTitle)
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    private fun keyHeight(): Double {
        val needed = button.sizeThatFits(CGSizeMake(Double.MAX_VALUE / 4, Double.MAX_VALUE / 4)).useContents { height }
        return maxOf(MIN_KEY_HEIGHT, needed)
    }

    private fun accessoryHeight(): Double = keyHeight() + 2 * KEY_SPACING

    override fun intrinsicContentSize(): CValue<CGSize> = CGSizeMake(UIViewNoIntrinsicMetric, accessoryHeight())

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = bounds.useContents { size.width }
        val height = keyHeight()
        val keyWidth = keyWidth(width)
        button.setFrame(CGRectMake(width - KEY_SPACING - keyWidth, KEY_SPACING, keyWidth, height))
    }

    // Only the button takes touches; the transparent rest passes through to what is behind it.
    override fun hitTest(
        point: CValue<CGPoint>,
        withEvent: UIEvent?,
    ): UIView? {
        val hit = super.hitTest(point, withEvent)
        return if (hit === this) null else hit
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        applyTitle()
        val width = bounds.useContents { size.width }
        setFrame(CGRectMake(0.0, 0.0, width, accessoryHeight()))
    }
}

/** Width of one of the three number pad columns for a keyboard [width]. */
internal fun keyWidth(width: Double): Double = (width - 4 * KEY_SPACING) / 3.0

// Measured from the iOS 26 iPhone number pad in portrait (390 pt wide: keys 121.67 x 48, corner radius
// 9, side margin and gaps about 6). An approximation, not a system value.
private const val INITIAL_WIDTH = 390.0
private const val MIN_KEY_HEIGHT = 48.0
private const val KEY_CORNER_RADIUS = 9.0
private const val KEY_SPACING = 6.0
