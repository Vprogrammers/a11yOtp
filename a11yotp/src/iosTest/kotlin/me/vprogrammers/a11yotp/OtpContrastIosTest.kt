package me.vprogrammers.a11yotp

import platform.UIKit.UIColor
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
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
}
