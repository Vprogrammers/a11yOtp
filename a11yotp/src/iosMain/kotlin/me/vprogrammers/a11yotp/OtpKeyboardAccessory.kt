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
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
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
 * Done button above the number pad, because the number pad has no Return key.
 *
 * On iOS 26 it follows the system keyboard design: no bar, just a neutral Liquid Glass capsule
 * at the trailing edge, directly above the keyboard. A UIToolbar would also float its Done item
 * there, but as a blue prominent capsule. Earlier iOS versions get a classic bar with a background
 * and separator. The title uses the label color, not the blue tint (below 4.5:1 on these
 * backgrounds), and scales with Dynamic Type.
 */
internal class OtpKeyboardAccessory(
    target: NSObject,
    action: String,
) : UIView(frame = CGRectMake(0.0, 0.0, INITIAL_WIDTH, MIN_HEIGHT)) {
    private val glass = isLiquidGlassAvailable()
    private val button =
        if (glass) {
            UIButton.buttonWithConfiguration(
                UIButtonConfiguration.glassButtonConfiguration().apply {
                    cornerStyle = UIButtonConfigurationCornerStyleCapsule
                    baseForegroundColor = UIColor.labelColor
                },
                primaryAction = null,
            )
        } else {
            UIButton.buttonWithType(UIButtonTypeSystem)
        }
    private val separator = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))

    init {
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        if (glass) {
            backgroundColor = UIColor.clearColor
            separator.hidden = true
        } else {
            backgroundColor = UIColor.secondarySystemBackgroundColor
            separator.backgroundColor = UIColor.separatorColor
            button.tintColor = UIColor.labelColor
            button.titleLabel?.font = UIFont.preferredFontForTextStyle(UIFontTextStyleHeadline)
            button.titleLabel?.adjustsFontForContentSizeCategory = true
        }
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

    private fun buttonHeight(): Double = maxOf(MIN_HEIGHT, buttonSize().second + if (glass) 0.0 else 2 * PADDING)

    // With glass, a gap below the capsule separates it from the keyboard like the system items.
    override fun intrinsicContentSize(): CValue<CGSize> =
        CGSizeMake(UIViewNoIntrinsicMetric, buttonHeight() + if (glass) GLASS_GAP else 0.0)

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = bounds.useContents { this.size.width }
        val right = safeAreaInsets.useContents { this.right }
        val padding = if (glass) GLASS_PADDING else HORIZONTAL_PADDING
        val buttonWidth = maxOf(MIN_HEIGHT, buttonSize().first + 2 * padding)
        val margin = if (glass) GLASS_MARGIN else 0.0
        button.setFrame(CGRectMake(width - right - margin - buttonWidth, 0.0, buttonWidth, buttonHeight()))
        separator.setFrame(CGRectMake(0.0, 0.0, width, 1.0 / UIScreen.mainScreen.scale))
    }
}

private const val INITIAL_WIDTH = 320.0
private const val MIN_HEIGHT = 44.0
private const val PADDING = 4.0
private const val HORIZONTAL_PADDING = 16.0
private const val GLASS_PADDING = 4.0
private const val GLASS_MARGIN = 8.0
private const val GLASS_GAP = 8.0
