@file:OptIn(ExperimentalForeignApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.kCACornerCurveContinuous
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleHeadline
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.colorWithDynamicProvider
import platform.UIKit.labelColor
import platform.UIKit.secondarySystemBackgroundColor
import platform.UIKit.separatorColor
import platform.UIKit.setAccessibilityLabel
import platform.darwin.NSObject

/**
 * Done button above the number pad, because the number pad has no Return key.
 *
 * On iOS 26 the keyboard is a rounded panel of keys, and apps cannot draw inside it. The button
 * therefore looks like one more key: the width of one number pad column, the key color and corner
 * shape, at the trailing edge with the keys' spacing, directly on top of the keyboard. Earlier
 * iOS versions get the classic bar with a background and separator. The title uses the label
 * color and scales with Dynamic Type; the button is at least 44 points in both directions.
 */
internal class OtpKeyboardAccessory(
    target: NSObject,
    action: String,
) : UIView(frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, MIN_SIZE)) {
    private val keyStyle = isLiquidGlassAvailable()
    private val button = UIButton.buttonWithType(UIButtonTypeSystem)
    private val separator = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        button.tintColor = UIColor.labelColor
        button.titleLabel?.font = UIFont.preferredFontForTextStyle(UIFontTextStyleHeadline)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.addTarget(target, NSSelectorFromString(action), UIControlEventTouchUpInside)
        if (keyStyle) {
            backgroundColor = UIColor.clearColor
            separator.hidden = true
            button.backgroundColor = keyColor()
            button.layer.cornerRadius = KEY_RADIUS
            button.layer.cornerCurve = kCACornerCurveContinuous
        } else {
            backgroundColor = UIColor.secondarySystemBackgroundColor
            separator.backgroundColor = UIColor.separatorColor
        }
        addSubview(separator)
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

    private fun buttonHeight(): Double =
        maxOf(if (keyStyle) KEY_HEIGHT else MIN_SIZE, titleSize().second + 2 * VERTICAL_PADDING)

    override fun intrinsicContentSize(): CValue<CGSize> =
        CGSizeMake(UIViewNoIntrinsicMetric, buttonHeight() + if (keyStyle) KEY_SPACING else 0.0)

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = bounds.useContents { this.size.width }
        val (left, right) = safeAreaInsets.useContents { this.left to this.right }
        val titleWidth = titleSize().first + 2 * HORIZONTAL_PADDING
        val height = buttonHeight()
        if (keyStyle) {
            // One of the three number pad columns, aligned with the keys below.
            val column = (width - left - right - 2 * KEY_MARGIN - 2 * KEY_SPACING) / 3.0
            val buttonWidth = minOf(maxOf(column, titleWidth, MIN_SIZE), width - left - right - 2 * KEY_MARGIN)
            button.setFrame(CGRectMake(width - right - KEY_MARGIN - buttonWidth, 0.0, buttonWidth, height))
        } else {
            val buttonWidth = maxOf(titleWidth, MIN_SIZE)
            button.setFrame(CGRectMake(width - right - buttonWidth, 0.0, buttonWidth, height))
            separator.setFrame(CGRectMake(0.0, 0.0, width, 1.0 / UIScreen.mainScreen.scale))
        }
    }
}

/** Number pad key color: white in light mode, gray in dark mode, so label-colored text keeps 4.5:1. */
private fun keyColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        if (traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark) {
            UIColor(white = 0.36, alpha = 1.0)
        } else {
            UIColor(white = 1.0, alpha = 1.0)
        }
    }

private const val INITIAL_WIDTH = 320.0
private const val MIN_SIZE = 44.0
private const val VERTICAL_PADDING = 4.0
private const val HORIZONTAL_PADDING = 16.0
private const val KEY_HEIGHT = 48.0
private const val KEY_RADIUS = 10.0
private const val KEY_MARGIN = 6.0
private const val KEY_SPACING = 6.0
