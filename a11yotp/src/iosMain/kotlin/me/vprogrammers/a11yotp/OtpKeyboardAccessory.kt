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
import platform.UIKit.UIAccessibilityContrastHigh
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
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.colorWithDynamicProvider
import platform.UIKit.labelColor
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.setAccessibilityLabel
import platform.darwin.NSObject

/**
 * Done key on top of the number pad, because the number pad has no Return key.
 *
 * The bar is a UIInputView with the keyboard style, so the system draws the keyboard background
 * behind it (and makes it opaque under Reduce Transparency). App content never shows behind the
 * key, which keeps it perceivable for low-vision users. The bar sits directly on the keyboard
 * with no gap. The key is one number pad column wide at the trailing edge, in the key color, with
 * a border of at least 3:1 against the bar (WCAG 1.4.11), 2 points under Increase Contrast. The
 * title uses the label color (at least 4.5:1 on the key) and scales with Dynamic Type; the key is
 * at least 44 points in both directions. The visible title is also the accessible name, so Voice
 * Control "Tap Done" works.
 */
internal class OtpKeyboardAccessory(
    target: NSObject,
    action: String,
) : UIInputView(
        frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, KEY_HEIGHT + 2 * KEY_MARGIN),
        inputViewStyle = UIInputViewStyle.UIInputViewStyleKeyboard,
    ) {
    private val button = UIButton.buttonWithType(UIButtonTypeSystem)

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        allowsSelfSizing = true
        button.tintColor = UIColor.labelColor
        button.backgroundColor = keyColor()
        button.titleLabel?.font = UIFont.preferredFontForTextStyle(UIFontTextStyleHeadline)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.layer.cornerRadius = KEY_RADIUS
        button.layer.cornerCurve = kCACornerCurveContinuous
        button.addTarget(target, NSSelectorFromString(action), UIControlEventTouchUpInside)
        addSubview(button)
        applyBorder(traitCollection)
    }

    fun setTitle(title: String) {
        button.setTitle(title, forState = UIControlStateNormal)
        button.setAccessibilityLabel(title)
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    // Layer colors are not dynamic, so the border is resolved again when the traits change.
    private fun applyBorder(traits: UITraitCollection) {
        val high = traits.accessibilityContrast == UIAccessibilityContrastHigh
        button.layer.borderWidth = if (high) HIGH_CONTRAST_BORDER else BORDER
        button.layer.borderColor = keyBorderColor().resolvedColorWithTraitCollection(traits).CGColor
    }

    private fun titleSize(): Pair<Double, Double> =
        button.titleLabel
            ?.intrinsicContentSize()
            ?.useContents { this.width to this.height } ?: (0.0 to 0.0)

    private fun keyHeight(): Double = maxOf(KEY_HEIGHT, titleSize().second + 2 * VERTICAL_PADDING)

    override fun intrinsicContentSize(): CValue<CGSize> =
        CGSizeMake(UIViewNoIntrinsicMetric, keyHeight() + 2 * KEY_MARGIN)

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = bounds.useContents { this.size.width }
        val (left, right) = safeAreaInsets.useContents { this.left to this.right }
        val available = width - left - right - 2 * KEY_MARGIN
        // One of the three number pad columns, aligned with the keys below.
        val column = (available - 2 * KEY_MARGIN) / 3.0
        val titleWidth = titleSize().first + 2 * HORIZONTAL_PADDING
        val keyWidth = minOf(maxOf(column, titleWidth, MIN_SIZE), available)
        button.setFrame(CGRectMake(width - right - KEY_MARGIN - keyWidth, KEY_MARGIN, keyWidth, keyHeight()))
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        applyBorder(traitCollection)
        invalidateIntrinsicContentSize()
    }
}

private fun isDark(traits: UITraitCollection?) =
    traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

/** Number pad key color: white in light mode, gray in dark mode, so label-colored text keeps 4.5:1. */
private fun keyColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        UIColor(white = if (isDark(traits)) 0.36 else 1.0, alpha = 1.0)
    }

/** Opaque key border that stays at least 3:1 against the keyboard background; stronger under Increase Contrast. */
private fun keyBorderColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        val high = traits?.accessibilityContrast == UIAccessibilityContrastHigh
        val white =
            when {
                isDark(traits) && high -> 0.85
                isDark(traits) -> 0.70
                high -> 0.25
                else -> 0.40
            }
        UIColor(white = white, alpha = 1.0)
    }

private const val INITIAL_WIDTH = 320.0
private const val MIN_SIZE = 44.0
private const val VERTICAL_PADDING = 4.0
private const val HORIZONTAL_PADDING = 16.0
private const val KEY_HEIGHT = 48.0
private const val KEY_RADIUS = 10.0
private const val KEY_MARGIN = 6.0
private const val BORDER = 1.0
private const val HIGH_CONTRAST_BORDER = 2.0
