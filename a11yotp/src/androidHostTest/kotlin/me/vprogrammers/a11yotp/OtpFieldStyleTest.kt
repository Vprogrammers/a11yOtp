package me.vprogrammers.a11yotp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OtpFieldStyleTest {
    @Test
    fun defaultColorsHaveNoWarnings() {
        assertTrue(OtpFieldDefaults.colors().contrastWarnings.isEmpty())
        assertTrue(OtpButtonDefaults.colors().contrastWarnings.isEmpty())
    }

    @Test
    fun lowContrastColorsConstructAndReportWarnings() {
        val colors = OtpFieldDefaults.colors(text = Color(0xFF999999), cellBorder = Color(0xFFDDDDDD))
        assertEquals(2, colors.contrastWarnings.size)
        val text = colors.contrastWarnings.first()
        assertTrue("text (#999999) on cellBackground (#FFFFFF)" in text, text)
        assertTrue("2.84:1" in text, text)
        assertTrue("1.4.3" in text && "4.5:1" in text && "low vision" in text && "read this text" in text, text)
        assertTrue("1.4.11" in colors.contrastWarnings[1])
    }

    @Test
    fun errorTextPlaceholderAndFocusPairsAreChecked() {
        val colors =
            OtpFieldDefaults.colors(
                errorText = Color(0xFFFF8888),
                placeholder = Color(0xFFDDDDDD),
                focusedCellBorder = Color(0xFF777777),
            )
        val all = colors.contrastWarnings.joinToString("\n")
        assertTrue("errorText" in all && "1.4.3" in all, all)
        assertTrue("placeholder" in all && "1.4.11" in all, all)
        assertTrue("focusedCellBorder" in all && "on cellBorder" in all && "2.4.13" in all, all)
    }

    @Test
    fun translucentColorsAreNotEvaluated() {
        val colors = OtpFieldDefaults.colors(text = Color(0x33000000))
        assertTrue(colors.contrastWarnings.isEmpty())
    }

    @Test
    fun resendColorsWarnOnLowContrast() {
        assertEquals(1, OtpButtonDefaults.colors(content = Color(0xFFAAAAAA)).contrastWarnings.size)
    }

    @Test
    fun buttonMinHeightIsCoerced() {
        assertEquals(48.dp, OtpButtonDefaults.style(minHeight = 20.dp).minHeight)
        assertEquals(60.dp, OtpButtonDefaults.style(minHeight = 60.dp).minHeight)
    }

    @Test
    fun disabledButtonColorsAreExemptFromContrast() {
        val colors = OtpButtonDefaults.colors(disabledContent = Color(0xFFDDDDDD), disabledContainer = Color.White)
        assertTrue(colors.contrastWarnings.isEmpty())
    }

    @Test
    fun minimumCellSizeIsCoerced() {
        val style = OtpFieldDefaults.style(cellWidth = 20.dp, cellHeight = 30.dp)
        assertEquals(48.dp, style.cellWidth)
        assertEquals(48.dp, style.cellHeight)
    }

    @Test
    fun focusedBorderMustBeThicker() {
        assertFailsWith<IllegalArgumentException> {
            OtpFieldDefaults.style(borderWidth = 2.dp, focusedBorderWidth = 2.dp)
        }
    }
}
