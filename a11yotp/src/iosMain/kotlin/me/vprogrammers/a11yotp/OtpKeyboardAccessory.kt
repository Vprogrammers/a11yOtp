@file:OptIn(ExperimentalForeignApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleHeadline
import platform.UIKit.UIInputView
import platform.UIKit.UIInputViewStyle
import platform.UIKit.UITraitCollection
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.labelColor
import platform.UIKit.setAccessibilityLabel
import platform.darwin.NSObject

/**
 * Done button on top of the number pad, because the number pad has no Return key.
 *
 * The bar is a UIInputView with the keyboard style, so the system draws the keyboard background
 * behind it (opaque under Reduce Transparency) and app content never shows behind the button. Like
 * the system passcode screen's Cancel button, Done is plain text at the trailing edge: a text
 * button needs no boundary, only text contrast, and label-colored text keeps at least 4.5:1 on the
 * keyboard background. The title scales with Dynamic Type, the touch target is at least 44 points
 * in both directions, and the visible title is the accessible name, so Voice Control "Tap Done"
 * works.
 */
internal class OtpKeyboardAccessory(
    target: NSObject,
    action: String,
) : UIInputView(
        frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, MIN_SIZE),
        inputViewStyle = UIInputViewStyle.UIInputViewStyleKeyboard,
    ) {
    private val button = UIButton.buttonWithType(UIButtonTypeSystem)

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        allowsSelfSizing = true
        button.tintColor = UIColor.labelColor
        button.titleLabel?.font = UIFont.preferredFontForTextStyle(UIFontTextStyleHeadline)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.addTarget(target, NSSelectorFromString(action), UIControlEventTouchUpInside)
        addSubview(button)
    }

    fun setTitle(title: String) {
        button.setTitle(title, forState = UIControlStateNormal)
        button.setAccessibilityLabel(title)
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    private fun titleSize(): Pair<Double, Double> =
        button.titleLabel
            ?.intrinsicContentSize()
            ?.useContents { this.width to this.height } ?: (0.0 to 0.0)

    private fun barHeight(): Double = maxOf(MIN_SIZE, titleSize().second + 2 * VERTICAL_PADDING)

    override fun intrinsicContentSize(): CValue<CGSize> = CGSizeMake(UIViewNoIntrinsicMetric, barHeight())

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = bounds.useContents { this.size.width }
        val right = safeAreaInsets.useContents { this.right }
        val buttonWidth = minOf(maxOf(titleSize().first + 2 * HORIZONTAL_PADDING, MIN_SIZE), width - 2 * EDGE_MARGIN)
        button.setFrame(CGRectMake(width - right - EDGE_MARGIN - buttonWidth, 0.0, buttonWidth, barHeight()))
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        invalidateIntrinsicContentSize()
    }
}

private const val INITIAL_WIDTH = 320.0
private const val MIN_SIZE = 44.0
private const val VERTICAL_PADDING = 4.0
private const val HORIZONTAL_PADDING = 16.0
private const val EDGE_MARGIN = 4.0
