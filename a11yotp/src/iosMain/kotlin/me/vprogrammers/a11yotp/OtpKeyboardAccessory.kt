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
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.colorWithDynamicProvider
import platform.UIKit.labelColor
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setIsAccessibilityElement
import platform.darwin.NSObject

/**
 * Done key that extends the number pad, because the number pad has no Return key.
 *
 * On iOS 26 the keyboard is a rounded panel and its UIInputView background is transparent, so the
 * bar draws its own opaque panel in the keyboard color with the keyboard's rounded top corners.
 * The system clips this bar to its bounds, so the panel is placed in the nearest ancestor that
 * also contains the keyboard, behind it, and reaches down behind the keyboard's top edge and
 * rounded corners. The keyboard then looks taller with no line where the two meet, and the panel
 * moves with the keyboard's show and hide animation. Being opaque, it never lets app content show
 * behind Done, which keeps it perceivable for low-vision users. Earlier iOS versions use the
 * keyboard-style UIInputView background, which matches their flat keyboard.
 *
 * Done is plain blue text above the 3, darker in light mode and lighter in dark mode than the
 * system blue, so it keeps at least 4.5:1 on the panel (5.3:1 light, 10:1 dark). The bar is only
 * as tall as the 44 point touch target (more with large Dynamic Type), and the target is one
 * number pad column wide. Increase Contrast draws a border around the target, and the visible
 * title is the accessible name, so Voice Control "Tap Done" works.
 */
internal class OtpKeyboardAccessory(
    target: NSObject,
    action: String,
) : UIInputView(
        frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, MIN_SIZE),
        inputViewStyle = UIInputViewStyle.UIInputViewStyleKeyboard,
    ) {
    private val panel: UIView? =
        if (isLiquidGlassAvailable()) {
            UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).also {
                it.userInteractionEnabled = false
                it.setIsAccessibilityElement(false)
                it.backgroundColor = keyboardColor()
                it.layer.cornerRadius = PANEL_RADIUS
                it.layer.cornerCurve = kCACornerCurveContinuous
                it.layer.maskedCorners = kCALayerMinXMinYCorner or kCALayerMaxXMinYCorner
            }
        } else {
            null
        }
    private val button = UIButton.buttonWithType(UIButtonTypeSystem)

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        allowsSelfSizing = true
        button.tintColor = doneTitleColor()
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

    private fun barHeight(): Double = maxOf(MIN_SIZE, titleSize().second + 2 * VERTICAL_PADDING)

    override fun intrinsicContentSize(): CValue<CGSize> = CGSizeMake(UIViewNoIntrinsicMetric, barHeight())

    override fun layoutSubviews() {
        super.layoutSubviews()
        val (width, height) = bounds.useContents { this.size.width to this.size.height }
        val (left, right) = safeAreaInsets.useContents { this.left to this.right }
        updatePanelFrame()
        val available = width - left - right - 2 * KEY_MARGIN
        // One of the three number pad columns, aligned with the 3, 6, and 9 keys below.
        val column = (available - 2 * KEY_GAP) / 3.0
        val titleWidth = titleSize().first + 2 * HORIZONTAL_PADDING
        val keyWidth = minOf(maxOf(column, titleWidth, MIN_SIZE), available)
        button.setFrame(CGRectMake(width - right - KEY_MARGIN - keyWidth, 0.0, keyWidth, height))
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        attachPanel()
    }

    /** Moves the panel behind the keyboard, into the nearest ancestor that also contains it. */
    private fun attachPanel() {
        val panel = panel ?: return
        if (window == null) {
            panel.removeFromSuperview()
            return
        }
        val ownHeight = bounds.useContents { this.size.height }
        var host = superview
        while (host != null && host.bounds.useContents { this.size.height } < ownHeight + MIN_KEYBOARD_HEIGHT) {
            host = host.superview
        }
        val target = host ?: this
        if (panel.superview != target) {
            panel.removeFromSuperview()
            target.insertSubview(panel, atIndex = 0)
        }
        updatePanelFrame()
    }

    private fun updatePanelFrame() {
        val panel = panel ?: return
        val host = panel.superview ?: return
        convertRect(bounds, toView = host).useContents {
            panel.setFrame(CGRectMake(origin.x, origin.y, size.width, size.height + PANEL_OVERLAP))
        }
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        applyBorder(traitCollection)
        invalidateIntrinsicContentSize()
    }
}

private fun isDark(traits: UITraitCollection?) =
    traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

/** iOS 26 number pad panel color, measured on device in light mode (#E2E3E6). */
private fun keyboardColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        if (isDark(traits)) {
            UIColor(red = 0.17, green = 0.17, blue = 0.18, alpha = 1.0)
        } else {
            UIColor(red = 0.886, green = 0.89, blue = 0.902, alpha = 1.0)
        }
    }

/** Done title blue with at least 4.5:1 on [keyboardColor]; stronger under Increase Contrast. */
private fun doneTitleColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        val high = traits?.accessibilityContrast == UIAccessibilityContrastHigh
        when {
            isDark(traits) && high -> UIColor(red = 0.86, green = 0.93, blue = 1.0, alpha = 1.0)
            isDark(traits) -> UIColor(red = 0.76, green = 0.86, blue = 1.0, alpha = 1.0)
            high -> UIColor(red = 0.0, green = 0.2, blue = 0.6, alpha = 1.0)
            else -> UIColor(red = 0.0, green = 0.32, blue = 0.8, alpha = 1.0)
        }
    }

private const val INITIAL_WIDTH = 320.0
private const val MIN_SIZE = 44.0
private const val VERTICAL_PADDING = 4.0
private const val HORIZONTAL_PADDING = 16.0
private const val KEY_RADIUS = 9.0
private const val KEY_MARGIN = 7.5
private const val KEY_GAP = 11.0
private const val PANEL_RADIUS = 28.0

// How far the panel reaches behind the keyboard: past its rounded top corners.
private const val PANEL_OVERLAP = 60.0

// An ancestor at least this much taller than the bar also contains the keyboard.
private const val MIN_KEYBOARD_HEIGHT = 150.0
private const val HIGH_CONTRAST_BORDER = 2.0
