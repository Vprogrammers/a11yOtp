package me.vprogrammers.a11yotp

import platform.UIKit.UIAccessibilityContrastHigh
import platform.UIKit.UIColor
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.systemBlueColor
import platform.UIKit.systemYellowColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OtpContrastIosTest {
    private val light =
        UITraitCollection.traitCollectionWithUserInterfaceStyle(
            UIUserInterfaceStyle.UIUserInterfaceStyleLight,
        )
    private val dark =
        UITraitCollection.traitCollectionWithUserInterfaceStyle(
            UIUserInterfaceStyle.UIUserInterfaceStyleDark,
        )

    private fun gray(value: Double) = UIColor(red = value, green = value, blue = value, alpha = 1.0)

    @Test
    fun defaultColorsMeetContrastInLightAndDarkMode() {
        assertEquals(emptyList(), OtpFieldDefaults.colors().contrastWarningsIn(light))
        assertEquals(emptyList(), OtpFieldDefaults.colors().contrastWarningsIn(dark))
        assertEquals(emptyList(), OtpButtonDefaults.colors().contrastWarningsIn(light))
        assertEquals(emptyList(), OtpButtonDefaults.colors().contrastWarningsIn(dark))
    }

    @Test
    fun lowContrastTextBorderAndPlaceholderAreReported() {
        val colors =
            OtpFieldDefaults.colors(
                text = gray(0.8),
                cellBackground = gray(1.0),
                cellBorder = gray(0.9),
                placeholder = gray(0.9),
            )

        val warnings = colors.contrastWarningsIn(light)

        assertTrue(warnings.any { "text (" in it && "1.4.3" in it }, warnings.toString())
        assertTrue(warnings.any { "cellBorder (" in it && "1.4.11" in it }, warnings.toString())
        assertTrue(warnings.any { "placeholder (" in it }, warnings.toString())
    }

    @Test
    fun weakFocusIndicatorAgainstTheBorderIsReported() {
        val colors = OtpFieldDefaults.colors(cellBorder = gray(0.4), focusedCellBorder = gray(0.45))

        assertTrue(colors.contrastWarningsIn(light).any { "2.4.13" in it })
    }

    @Test
    fun translucentColorsAreSkipped() {
        val colors = OtpFieldDefaults.colors(text = UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 0.5))

        assertTrue(colors.contrastWarningsIn(light).none { "text (" in it })
    }

    @Test
    fun dynamicColorsAreResolvedPerTraitCollection() {
        // Black text on a dark-mode background fails in dark mode only.
        val colors = OtpFieldDefaults.colors(text = UIColor(red = 0.0, green = 0.0, blue = 0.0, alpha = 1.0))

        assertTrue(colors.contrastWarningsIn(light).none { "text (" in it })
        assertTrue(colors.contrastWarningsIn(dark).any { "text (" in it })
    }

    @Test
    fun lowContrastButtonContentIsReported() {
        val colors = OtpButtonDefaults.colors(content = gray(0.7), container = gray(0.8))

        assertTrue(colors.contrastWarningsIn(light).single().contains("1.4.3"))
    }

    private val high = UITraitCollection.traitCollectionWithAccessibilityContrast(UIAccessibilityContrastHigh)
    private val darkHigh = UITraitCollection.traitCollectionWithTraitsFromCollections(listOf(dark, high))

    private fun fillRatio(
        tint: UIColor,
        traits: UITraitCollection,
    ): Double = OtpContrast.ratio(0xFFFFFF, doneFillColor { tint }.opaqueRgb(traits)!!)

    @Test
    fun doneFillOnSystemBlueMeetsTextContrastInEveryAppearance() {
        val blue = UIColor.systemBlueColor
        assertTrue(fillRatio(blue, light) >= 4.5, "light ${fillRatio(blue, light)}")
        assertTrue(fillRatio(blue, dark) >= 4.5, "dark ${fillRatio(blue, dark)}")
        assertTrue(fillRatio(blue, high) >= 7.0, "high ${fillRatio(blue, high)}")
        assertTrue(fillRatio(blue, darkHigh) >= 7.0, "dark high ${fillRatio(blue, darkHigh)}")
    }

    @Test
    fun doneFillOnLightHostTintsMeetsTextContrast() {
        val lightCyan = UIColor(red = 0.6, green = 0.95, blue = 1.0, alpha = 1.0)
        for (tint in listOf(UIColor.systemYellowColor, lightCyan)) {
            assertTrue(fillRatio(tint, light) >= 4.5, "light ${fillRatio(tint, light)}")
            assertTrue(fillRatio(tint, high) >= 7.0, "high ${fillRatio(tint, high)}")
        }
    }

    @Test
    fun aTranslucentTintStillGivesAnOpaqueFillThatMeetsTextContrast() {
        val translucent = UIColor.systemBlueColor.colorWithAlphaComponent(0.3)

        assertTrue(fillRatio(translucent, light) >= 4.5, "light ${fillRatio(translucent, light)}")
        assertTrue(fillRatio(translucent, darkHigh) >= 7.0, "dark high ${fillRatio(translucent, darkHigh)}")
    }

    @Test
    fun aDarkTintThatAlreadyPassesIsNotDarkened() {
        val navy = UIColor(red = 0.0, green = 0.1, blue = 0.4, alpha = 1.0)

        assertEquals(navy.opaqueRgb(light), doneFillColor { navy }.opaqueRgb(light))
        assertEquals(0x001966, doneFillRgb(0x001966, 4.5))
    }

    @Test
    fun doneFillFollowsTheCurrentTint() {
        var tint = UIColor(red = 0.0, green = 0.1, blue = 0.4, alpha = 1.0)
        val color = doneFillColor { tint }
        val before = color.opaqueRgb(light)

        tint = UIColor(red = 0.0, green = 0.4, blue = 0.1, alpha = 1.0)

        assertTrue(before != color.opaqueRgb(light))
    }
}
