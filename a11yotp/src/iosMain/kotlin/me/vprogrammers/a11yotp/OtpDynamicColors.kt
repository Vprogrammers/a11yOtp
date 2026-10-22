package me.vprogrammers.a11yotp

import platform.UIKit.UIAccessibilityContrastHigh
import platform.UIKit.UIColor
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.colorWithDynamicProvider

/**
 * Error red that stays at least 4.5:1 on the default cell background in light, dark, and
 * Increase Contrast variants. It is a dynamic color, so the system resolves it per trait collection.
 */
internal fun errorColor(): UIColor =
    UIColor.colorWithDynamicProvider { traits ->
        val dark = traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
        val high = traits?.accessibilityContrast == UIAccessibilityContrastHigh
        when {
            high && dark -> UIColor(red = 1.0, green = 0.72, blue = 0.68, alpha = 1.0)
            high -> UIColor(red = 0.55, green = 0.0, blue = 0.08, alpha = 1.0)
            dark -> UIColor(red = 1.0, green = 0.54, blue = 0.5, alpha = 1.0)
            else -> UIColor(red = 0.69, green = 0.0, blue = 0.125, alpha = 1.0)
        }
    }
