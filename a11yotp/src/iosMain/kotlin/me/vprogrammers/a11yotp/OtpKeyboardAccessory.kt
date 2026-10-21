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
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.labelColor
import platform.UIKit.secondarySystemBackgroundColor
import platform.UIKit.separatorColor
import platform.UIKit.setAccessibilityLabel
import platform.darwin.NSObject

/**
 * Bar above the number pad with a Done button, because the number pad has no Return key.
 *
 * A plain view instead of a UIToolbar: on iOS 26 a toolbar has no background and its Done item
 * becomes a floating blue capsule over the app content. This bar stays attached to the keyboard
 * and looks the same on every iOS version. The title uses the label color, not the blue tint,
 * which is below 4.5:1 on this background, and it scales with Dynamic Type.
 */
internal class OtpKeyboardAccessory(
    target: NSObject,
    action: String,
) : UIView(frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, MIN_HEIGHT)) {
    private val button = UIButton.buttonWithType(UIButtonTypeSystem)
    private val separator = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        backgroundColor = UIColor.secondarySystemBackgroundColor
        separator.backgroundColor = UIColor.separatorColor
        button.tintColor = UIColor.labelColor
        button.titleLabel?.font = UIFont.preferredFontForTextStyle(UIFontTextStyleHeadline)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.addTarget(target, NSSelectorFromString(action), UIControlEventTouchUpInside)
        addSubview(separator)
        addSubview(button)
    }

    fun setTitle(title: String) {
        button.setTitle(title, forState = UIControlStateNormal)
        button.setAccessibilityLabel(title)
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    private fun buttonSize(): Pair<Double, Double> =
        button.intrinsicContentSize().useContents { this.width to this.height }

    override fun intrinsicContentSize(): CValue<CGSize> =
        CGSizeMake(UIViewNoIntrinsicMetric, maxOf(MIN_HEIGHT, buttonSize().second + 2 * PADDING))

    override fun layoutSubviews() {
        super.layoutSubviews()
        val (width, height) = bounds.useContents { this.size.width to this.size.height }
        val right = safeAreaInsets.useContents { this.right }
        val buttonWidth = maxOf(MIN_HEIGHT, buttonSize().first + 2 * HORIZONTAL_PADDING)
        button.setFrame(CGRectMake(width - right - buttonWidth, 0.0, buttonWidth, height))
        separator.setFrame(CGRectMake(0.0, 0.0, width, 1.0 / UIScreen.mainScreen.scale))
    }
}

private const val INITIAL_WIDTH = 320.0
private const val MIN_HEIGHT = 44.0
private const val PADDING = 4.0
private const val HORIZONTAL_PADDING = 16.0
