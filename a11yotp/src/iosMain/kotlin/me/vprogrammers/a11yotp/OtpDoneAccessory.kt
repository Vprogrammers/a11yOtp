@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalNativeApi::class)

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
import platform.UIKit.UIAccessibilityContrastHigh
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIButtonConfigurationCornerStyleFixed
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIEvent
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleHeadline
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.colorWithDynamicProvider
import platform.UIKit.currentTraitCollection
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.systemBlueColor
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

/**
 * Keyboard accessory that adds a Done key above the number pad (which has no Return key).
 *
 * The accessory is transparent and only the button receives touches, so the area around it does not
 * block the one-time-code suggestion or the app content behind it. The button is shaped and placed like
 * a number pad key, right-aligned over the "3" key, with the same 6 pt spacing as the key rows (see
 * [doneKeyFrame]: it respects the left and right safe area, and on iPad it keeps the iPhone key width
 * instead of stretching). Metrics are measured from the iOS 26 iPhone number pad in portrait and are an
 * approximation.
 *
 * Height: the accessory is one key row plus 6 pt above and below, so it adds key height + 12 pt
 * (about 60 pt at the default size, more at large Dynamic Type sizes) to the keyboard height that host
 * layouts avoid. The space matches the spacing between the number pad key rows, and because the view
 * is transparent and passes touches through, the one-time-code suggestion above it stays tappable.
 * The height is not reduced, so the key keeps a target of at least 44 x 44 pt with room around it.
 *
 * Contrast: the fill is the host tint (default system blue), darkened with [doneFillColor] until the
 * white title reaches 4.5:1 (7:1 with Increase Contrast), per trait collection.
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
            it.baseForegroundColor = UIColor.whiteColor
            it.baseBackgroundColor = fillColor()
        }

    // The provider reads the tint this view inherits from the host (the button inherits the same one).
    // It is built during init, before [button] exists, so it must not capture the button; this view is
    // held weakly so the color does not keep it alive.
    private fun fillColor(): UIColor {
        val weakSelf = WeakReference(this)
        return doneFillColor { weakSelf.get()?.tintColor }
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

    // Measured at the key's own width, so a long title or a large Dynamic Type size wraps and makes the
    // key taller instead of being clipped.
    private fun keyHeight(width: Double): Double {
        val needed =
            button
                .sizeThatFits(
                    CGSizeMake(keyFrame(width, 0.0).width, Double.MAX_VALUE / 4),
                ).useContents { height }
        return maxOf(MIN_KEY_HEIGHT, needed)
    }

    private fun accessoryHeight(width: Double): Double = keyHeight(width) + 2 * KEY_SPACING

    private fun currentWidth(): Double = bounds.useContents { size.width }

    private fun isPad(): Boolean = traitCollection.userInterfaceIdiom == UIUserInterfaceIdiomPad

    private fun keyFrame(
        width: Double,
        keyHeight: Double,
    ): OtpKeyFrame {
        val (left, right) = safeAreaInsets.useContents { left to right }
        return doneKeyFrame(width, left, right, isPad(), keyHeight)
    }

    override fun intrinsicContentSize(): CValue<CGSize> =
        CGSizeMake(UIViewNoIntrinsicMetric, accessoryHeight(currentWidth()))

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = currentWidth()
        val height = keyHeight(width)
        val frame = keyFrame(width, height)
        button.setFrame(CGRectMake(frame.x, frame.y, frame.width, frame.height))
    }

    override fun safeAreaInsetsDidChange() {
        super.safeAreaInsetsDidChange()
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    // A host tint change does not re-resolve the dynamic fill, so the color is created again.
    override fun tintColorDidChange() {
        super.tintColorDidChange()
        button.configuration?.let {
            it.baseBackgroundColor = fillColor()
            button.configuration = it
        }
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
        val width = currentWidth()
        setFrame(CGRectMake(0.0, 0.0, width, accessoryHeight(width)))
    }
}

/** Frame of the Done key inside the accessory, in the accessory's coordinates. */
internal data class OtpKeyFrame(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

/**
 * Frame of the Done key for an accessory [width] with the given safe-area insets. The number pad is
 * laid out inside the safe area (iPhone landscape with a notch), as three columns with 6 pt gaps and
 * margins; the key is the right column. On iPad the pad is not three full-width columns, so the key
 * keeps the iPhone width (122 pt), or the column width when that is narrower, right-aligned in the
 * safe area.
 */
internal fun doneKeyFrame(
    width: Double,
    safeLeft: Double,
    safeRight: Double,
    isPad: Boolean,
    keyHeight: Double,
): OtpKeyFrame {
    val usable = width - safeLeft - safeRight
    val column = ((usable - 4 * KEY_SPACING) / 3.0).coerceAtLeast(0.0)
    val keyWidth = if (isPad) minOf(PAD_KEY_WIDTH, column) else column
    return OtpKeyFrame(width - safeRight - KEY_SPACING - keyWidth, KEY_SPACING, keyWidth, keyHeight)
}

/**
 * Fill of the Done key: the tint from [tint] (system blue when null) for the current traits, darkened
 * by scaling toward black (hue kept) only as far as needed for the white title to reach 4.5:1, or 7:1
 * with Increase Contrast. A tint that already passes is unchanged. A translucent tint is used at full
 * opacity, and a color without RGB components (such as a pattern) falls back to system blue, so the
 * fill is always opaque and the contrast always holds. Resolved per trait collection, so dark mode and
 * Increase Contrast work.
 */
internal fun doneFillColor(tint: () -> UIColor?): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        val resolvedTraits = traits ?: UITraitCollection.currentTraitCollection
        val source = tint() ?: UIColor.systemBlueColor
        val rgb =
            source.colorWithAlphaComponent(1.0).opaqueRgb(resolvedTraits)
                ?: UIColor.systemBlueColor.opaqueRgb(resolvedTraits)
                ?: SYSTEM_BLUE_RGB
        val high = resolvedTraits.accessibilityContrast == UIAccessibilityContrastHigh
        val filled = doneFillRgb(rgb, if (high) HIGH_CONTRAST_RATIO else TEXT_CONTRAST_RATIO)
        UIColor(
            red = ((filled shr 16) and 0xFF) / 255.0,
            green = ((filled shr 8) and 0xFF) / 255.0,
            blue = (filled and 0xFF) / 255.0,
            alpha = 1.0,
        )
    }

/** Darkens [tintRgb] toward black until white reaches [target]; returns it unchanged when it already does. */
internal fun doneFillRgb(
    tintRgb: Int,
    target: Double,
): Int {
    if (OtpContrast.ratio(WHITE_RGB, tintRgb) >= target) return tintRgb
    for (step in 1..DARKEN_STEPS) {
        val factor = 1.0 - step.toDouble() / DARKEN_STEPS
        val candidate =
            (scaleChannel(tintRgb shr 16, factor) shl 16) or
                (scaleChannel(tintRgb shr 8, factor) shl 8) or
                scaleChannel(tintRgb, factor)
        if (OtpContrast.ratio(WHITE_RGB, candidate) >= target) return candidate
    }
    return 0x000000
}

private fun scaleChannel(
    value: Int,
    factor: Double,
): Int = ((value and 0xFF) * factor + 0.5).toInt()

// Measured from the iOS 26 iPhone number pad in portrait (390 pt wide: keys 121.67 x 48, corner radius
// 9, side margin and gaps about 6). An approximation, not a system value.
private const val INITIAL_WIDTH = 390.0
private const val MIN_KEY_HEIGHT = 48.0
private const val KEY_CORNER_RADIUS = 9.0
private const val KEY_SPACING = 6.0
private const val PAD_KEY_WIDTH = 122.0

// Light system blue; only used if UIKit cannot report components for it.
private const val SYSTEM_BLUE_RGB = 0x007AFF
private const val WHITE_RGB = 0xFFFFFF
private const val TEXT_CONTRAST_RATIO = 4.5
private const val HIGH_CONTRAST_RATIO = 7.0
private const val DARKEN_STEPS = 50
