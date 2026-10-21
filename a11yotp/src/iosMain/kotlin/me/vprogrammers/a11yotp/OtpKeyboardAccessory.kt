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
import platform.QuartzCore.kCALayerMaxXMinYCorner
import platform.QuartzCore.kCALayerMinXMinYCorner
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
import platform.UIKit.UIVisualEffectView
import platform.UIKit.colorWithDynamicProvider
import platform.UIKit.labelColor
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setIsAccessibilityElement
import platform.darwin.NSObject

/**
 * Done key that extends the number pad, because the number pad has no Return key.
 *
 * On iOS 26 the bar draws a Liquid Glass panel with the keyboard's rounded top corners. The panel
 * is taller than the bar and continues behind the top of the keyboard, so the keyboard panel looks
 * taller and the Done key reads as one more row of it; app content never shows behind the key.
 * Earlier iOS versions use the keyboard-style UIInputView background, which matches their flat
 * keyboard. The key sits above the 3, one number pad column wide, in the key color. Its title is
 * blue like the classic keyboard Done button, but darker in light mode and lighter in dark mode
 * than the system blue, which is below 4.5:1 on the key; it scales with Dynamic Type. Under
 * Increase Contrast the key gets a border. It is at least 44 points in both directions, and the visible title is the accessible
 * name, so Voice Control "Tap Done" works.
 */
internal class OtpKeyboardAccessory(
    target: NSObject,
    action: String,
) : UIInputView(
        frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, KEY_HEIGHT + KEY_TOP),
        inputViewStyle = UIInputViewStyle.UIInputViewStyleKeyboard,
    ) {
    private val panel: UIVisualEffectView? =
        sharedGlassEffect?.let { glass ->
            UIVisualEffectView(effect = glass).also {
                it.userInteractionEnabled = false
                it.setIsAccessibilityElement(false)
                it.clipsToBounds = true
                it.layer.cornerRadius = PANEL_RADIUS
                it.layer.cornerCurve = kCACornerCurveContinuous
                it.layer.maskedCorners = kCALayerMinXMinYCorner or kCALayerMaxXMinYCorner
            }
        }
    private val button = UIButton.buttonWithType(UIButtonTypeSystem)

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        allowsSelfSizing = true
        // The panel extends below the bar, behind the top of the keyboard.
        clipsToBounds = false
        panel?.let {
            backgroundColor = UIColor.clearColor
            addSubview(it)
        }
        button.tintColor = doneTitleColor()
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
        button.layer.borderWidth = if (high) HIGH_CONTRAST_BORDER else 0.0
        button.layer.borderColor = UIColor.labelColor.resolvedColorWithTraitCollection(traits).CGColor
    }

    private fun titleSize(): Pair<Double, Double> =
        button.titleLabel
            ?.intrinsicContentSize()
            ?.useContents { this.width to this.height } ?: (0.0 to 0.0)

    private fun keyHeight(): Double = maxOf(KEY_HEIGHT, titleSize().second + 2 * VERTICAL_PADDING)

    override fun intrinsicContentSize(): CValue<CGSize> = CGSizeMake(UIViewNoIntrinsicMetric, keyHeight() + KEY_TOP)

    override fun layoutSubviews() {
        super.layoutSubviews()
        val (width, height) = bounds.useContents { this.size.width to this.size.height }
        val (left, right) = safeAreaInsets.useContents { this.left to this.right }
        panel?.setFrame(CGRectMake(0.0, 0.0, width, height + PANEL_OVERLAP))
        val available = width - left - right - 2 * KEY_MARGIN
        // One of the three number pad columns, aligned with the keys below.
        val column = (available - 2 * KEY_SPACING) / 3.0
        val titleWidth = titleSize().first + 2 * HORIZONTAL_PADDING
        val keyWidth = minOf(maxOf(column, titleWidth, MIN_SIZE), available)
        button.setFrame(CGRectMake(width - right - KEY_MARGIN - keyWidth, KEY_TOP, keyWidth, keyHeight()))
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        applyBorder(traitCollection)
        invalidateIntrinsicContentSize()
    }
}

/** Done title blue with at least 4.5:1 on [keyColor]; stronger under Increase Contrast. */
private fun doneTitleColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        val dark = traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
        val high = traits?.accessibilityContrast == UIAccessibilityContrastHigh
        when {
            dark && high -> UIColor(red = 0.86, green = 0.93, blue = 1.0, alpha = 1.0)
            dark -> UIColor(red = 0.76, green = 0.86, blue = 1.0, alpha = 1.0)
            high -> UIColor(red = 0.0, green = 0.2, blue = 0.6, alpha = 1.0)
            else -> UIColor(red = 0.0, green = 0.32, blue = 0.8, alpha = 1.0)
        }
    }

/** Number pad key color: white in light mode, gray in dark mode, so label-colored text keeps 4.5:1. */
private fun keyColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        val dark = traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
        UIColor(white = if (dark) 0.36 else 1.0, alpha = 1.0)
    }

private const val INITIAL_WIDTH = 320.0
private const val MIN_SIZE = 44.0
private const val VERTICAL_PADDING = 4.0
private const val HORIZONTAL_PADDING = 16.0
private const val KEY_HEIGHT = 48.0
private const val KEY_RADIUS = 10.0
private const val KEY_MARGIN = 6.0
private const val KEY_SPACING = 6.0
private const val KEY_TOP = 10.0
private const val PANEL_RADIUS = 28.0
private const val PANEL_OVERLAP = 40.0
private const val HIGH_CONTRAST_BORDER = 2.0
