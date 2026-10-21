@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CAShapeLayer
import platform.QuartzCore.kCACornerCurveContinuous
import platform.QuartzCore.kCAFillRuleEvenOdd
import platform.QuartzCore.kCALayerMaxXMinYCorner
import platform.QuartzCore.kCALayerMinXMinYCorner
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIAccessibilityContrastHigh
import platform.UIKit.UIBezierPath
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
import platform.UIKit.systemBackgroundColor
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

/**
 * Done key that extends the number pad, because the number pad has no Return key.
 *
 * On iOS 26 the keyboard is a rounded panel and its UIInputView background is transparent, so the
 * bar draws its own opaque panel in the keyboard color with the keyboard's rounded top corners.
 * The bar and the keyboard live in different windows, so nothing in the bar can reach behind the
 * keyboard. Under the keyboard's rounded top corners the app's own window shows through, which
 * read as a light line between the bar and the keyboard. Two small fills in the panel color are
 * therefore placed in the app window (the window of [anchorView]) under those corners, pinned to
 * the window's keyboardLayoutGuide so they follow the keyboard animation. Being opaque, the panel
 * never lets app content show behind Done, which keeps it perceivable for low-vision users.
 * Earlier iOS versions use the keyboard-style UIInputView background, which matches their flat
 * keyboard.
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

    // A plain band behind the top of the keyboard glass, then the two corner fills on top of it.
    private val cornerFills: List<UIView> =
        if (panel == null) emptyList() else listOf(glassBand(), cornerFill(leading = true), cornerFill(leading = false))
    private val cornerFillTops = mutableListOf<NSLayoutConstraint>()
    private var anchor: WeakReference<UIView>? = null

    /** A view in the app window, used to find the window that the keyboard covers. */
    var anchorView: UIView?
        get() = anchor?.get()
        set(value) {
            anchor = value?.let { WeakReference(it) }
        }

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        allowsSelfSizing = true
        panel?.let { insertSubview(it, atIndex = 0) }
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
        panel?.setFrame(bounds)
        cornerFillTops.forEach { it.constant = height }
        val available = width - left - right - 2 * KEY_MARGIN
        // One of the three number pad columns, aligned with the 3, 6, and 9 keys below.
        val column = (available - 2 * KEY_GAP) / 3.0
        val titleWidth = titleSize().first + 2 * HORIZONTAL_PADDING
        val keyWidth = minOf(maxOf(column, titleWidth, MIN_SIZE), available)
        button.setFrame(CGRectMake(width - right - KEY_MARGIN - keyWidth, 0.0, keyWidth, height))
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        if (window != null) attachCornerFills() else detachCornerFills()
    }

    private fun attachCornerFills() {
        // The root view's keyboard layout guide tracks the keyboard; the window's own guide does not.
        val appWindow = anchorView?.window ?: return
        val root = appWindow.rootViewController?.view ?: return
        if (cornerFills.isEmpty() || cornerFills.first().superview == root) return
        detachCornerFills()
        val guide = root.keyboardLayoutGuide
        val barHeight = bounds.useContents { this.size.height }
        val constraints = mutableListOf<NSLayoutConstraint>()
        cornerFills.forEachIndexed { index, fill ->
            root.addSubview(fill)
            val top = fill.topAnchor.constraintEqualToAnchor(guide.topAnchor, constant = barHeight)
            cornerFillTops += top
            constraints += top
            if (index == 0) {
                constraints += fill.leadingAnchor.constraintEqualToAnchor(root.leadingAnchor)
                constraints += fill.trailingAnchor.constraintEqualToAnchor(root.trailingAnchor)
                constraints += fill.heightAnchor.constraintEqualToConstant(GLASS_BAND)
            } else {
                constraints += fill.widthAnchor.constraintEqualToConstant(CORNER_FILL)
                constraints += fill.heightAnchor.constraintEqualToConstant(CORNER_FILL)
                constraints +=
                    if (index == 1) {
                        fill.leadingAnchor.constraintEqualToAnchor(root.leadingAnchor)
                    } else {
                        fill.trailingAnchor.constraintEqualToAnchor(root.trailingAnchor)
                    }
            }
        }
        NSLayoutConstraint.activateConstraints(constraints)
    }

    private fun detachCornerFills() {
        cornerFills.forEach { it.removeFromSuperview() }
        cornerFillTops.clear()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        applyBorder(traitCollection)
        invalidateIntrinsicContentSize()
    }
}

/**
 * Plain background behind the top of the keyboard glass, so app content does not show blurred
 * through it. Glass over the system background gives the measured panel color (#E2E3E6 in light
 * mode), so the bar and the keyboard read as one surface.
 */
private fun glassBand(): UIView =
    UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).also {
        it.translatesAutoresizingMaskIntoConstraints = false
        it.userInteractionEnabled = false
        it.setIsAccessibilityElement(false)
        it.backgroundColor = UIColor.systemBackgroundColor
    }

/**
 * Fill for the area outside one rounded top corner of the keyboard. Inside the corner arc the
 * keyboard's glass is drawn over it, which would darken the glass, so the mask keeps only the part
 * outside the arc (even-odd: the square minus the keyboard's rounded shape).
 */
private fun cornerFill(leading: Boolean): UIView =
    UIView(frame = CGRectMake(0.0, 0.0, CORNER_FILL, CORNER_FILL)).also {
        it.translatesAutoresizingMaskIntoConstraints = false
        it.userInteractionEnabled = false
        it.setIsAccessibilityElement(false)
        it.backgroundColor = keyboardColor()
        val keyboardX = if (leading) 0.0 else -CORNER_FILL
        val path = UIBezierPath.bezierPathWithRect(CGRectMake(0.0, 0.0, CORNER_FILL, CORNER_FILL))
        path.appendPath(
            UIBezierPath.bezierPathWithRoundedRect(
                CGRectMake(keyboardX, 0.0, 2 * CORNER_FILL, 2 * CORNER_FILL),
                cornerRadius = KEYBOARD_CORNER_RADIUS,
            ),
        )
        it.layer.mask =
            CAShapeLayer().apply {
                this.path = path.CGPath
                fillRule = kCAFillRuleEvenOdd
            }
    }

private fun isDark(traits: UITraitCollection?) =
    traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

/** iOS 26 number pad panel color, measured over the system background: #E2E3E6 light, #171717 dark. */
private fun keyboardColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        if (isDark(traits)) {
            UIColor(red = 0.09, green = 0.09, blue = 0.09, alpha = 1.0)
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

// The iOS 26 keyboard's top corner radius, measured in the simulator (about 80 px at 3x).
private const val KEYBOARD_CORNER_RADIUS = 27.0
private const val CORNER_FILL = 32.0

// Covers the keyboard's empty top band and its first key row.
private const val GLASS_BAND = 80.0
private const val HIGH_CONTRAST_BORDER = 2.0
