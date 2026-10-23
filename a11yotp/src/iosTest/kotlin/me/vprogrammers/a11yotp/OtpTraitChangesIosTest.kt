@file:OptIn(ExperimentalForeignApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIUserInterfaceSizeClassCompact
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The views register for trait changes (iOS 17+) instead of overriding `traitCollectionDidChange`.
 * Trait overrides on a window stand in for the system changing Dynamic Type or dark mode; the change
 * is delivered when the window lays out. Checking that the whole chain fires on a real device still
 * needs the manual Dynamic Type and Increase Contrast steps.
 */
class OtpTraitChangesIosTest {
    private fun window(content: UIView): UIWindow =
        UIWindow(frame = CGRectMake(0.0, 0.0, 390.0, 800.0)).also {
            content.setFrame(CGRectMake(0.0, 0.0, 390.0, 200.0))
            it.addSubview(content)
            it.layoutIfNeeded()
        }

    private fun UIWindow.settle() {
        setNeedsLayout()
        layoutIfNeeded()
    }

    @Test
    fun fieldContainerRendersAgainWhenTheAppearanceChanges() {
        val container = OtpFieldContainer()
        var changes = 0
        container.onTraitsChanged = { changes++ }
        val window = window(container)

        window.traitOverrides.setUserInterfaceStyle(UIUserInterfaceStyle.UIUserInterfaceStyleDark)
        window.settle()

        assertTrue(changes >= 1, "changes=$changes")
    }

    @Test
    fun fieldContainerIgnoresTraitsItDoesNotDependOn() {
        val container = OtpFieldContainer()
        var changes = 0
        container.onTraitsChanged = { changes++ }
        val window = window(container)

        window.traitOverrides.setHorizontalSizeClass(UIUserInterfaceSizeClassCompact)
        window.settle()

        assertEquals(0, changes)
    }

    @Test
    fun buttonContainerRendersAgainWhenTheAppearanceChanges() {
        val container = OtpButtonContainer()
        var changes = 0
        container.onTraitsChanged = { changes++ }
        val window = window(container)

        window.traitOverrides.setUserInterfaceStyle(UIUserInterfaceStyle.UIUserInterfaceStyleDark)
        window.settle()

        assertTrue(changes >= 1, "changes=$changes")
    }

    @Test
    fun doneAccessoryRecomputesItsHeightWhenTheAppearanceChanges() {
        val accessory = OtpDoneAccessory(target = platform.darwin.NSObject(), action = "description", title = "Done")
        val window = window(accessory)
        accessory.setFrame(CGRectMake(0.0, 0.0, 390.0, 1.0))

        window.traitOverrides.setUserInterfaceStyle(UIUserInterfaceStyle.UIUserInterfaceStyleDark)
        window.settle()

        val height = accessory.frame.useContents { size.height }
        assertTrue(height >= 48.0, "height=$height")
    }
}
