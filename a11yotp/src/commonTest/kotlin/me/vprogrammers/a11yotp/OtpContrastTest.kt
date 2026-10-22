package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OtpContrastTest {
    @Test
    fun ratioMatchesWcagReferenceValues() {
        val cases =
            listOf(
                Triple(0x000000, 0xFFFFFF, 21.0),
                Triple(0xFFFFFF, 0xFFFFFF, 1.0),
                Triple(0x777777, 0xFFFFFF, 4.48),
                Triple(0x767676, 0xFFFFFF, 4.54),
                Triple(0x949494, 0xFFFFFF, 3.03),
            )

        for ((foreground, background, expected) in cases) {
            val ratio = OtpContrast.ratio(foreground, background)
            assertTrue(kotlin.math.abs(ratio - expected) < 0.01, "$foreground on $background: $ratio")
            assertEquals(ratio, OtpContrast.ratio(background, foreground))
        }
    }

    @Test
    fun passingPairsHaveNoWarning() {
        assertNull(OtpContrast.warning("Colors", "text", 0x767676, "bg", 0xFFFFFF, OtpContrastRequirement.TEXT))
        assertNull(OtpContrast.warning("Colors", "border", 0x949494, "bg", 0xFFFFFF, OtpContrastRequirement.NON_TEXT))
    }

    @Test
    fun warningExplainsPairRatioAndCriterion() {
        val warning =
            OtpContrast.warning(
                "OtpFieldColors",
                "text",
                0x777777,
                "cellBackground",
                0xFFFFFF,
                OtpContrastRequirement.TEXT,
            )

        assertEquals(
            "A11yOtp: OtpFieldColors text (#777777) on cellBackground (#FFFFFF) has contrast 4.47:1; " +
                "WCAG 1.4.3 Contrast (Minimum) requires at least 4.5:1 for text. Users with low vision " +
                "may not be able to read this text. Using the colors as provided.",
            warning,
        )
    }

    @Test
    fun ratioJustBelowThresholdIsNotRoundedUp() {
        val warning = OtpContrast.warning("C", "f", 0x777777, "b", 0xFFFFFF, OtpContrastRequirement.TEXT)

        assertTrue(warning!!.contains("has contrast 4.4"))
    }

    @Test
    fun focusRequirementUsesFocusCriterion() {
        val warning =
            OtpContrast.warning(
                "C",
                "focusedCellBorder",
                0x777777,
                "cellBorder",
                0x888888,
                OtpContrastRequirement.FOCUS_INDICATOR,
            )

        assertTrue(warning!!.contains("WCAG 2.4.13 Focus Appearance requires at least 3.0:1"))
    }

    @Test
    fun shadowIsLightOnlyOnDarkBackgrounds() {
        assertTrue(otpShadowIsLight(0x000000))
        assertTrue(otpShadowIsLight(0x1C1C1E))
        assertTrue(!otpShadowIsLight(0xFFFFFF))
        assertTrue(!otpShadowIsLight(0xE8E8E8))
        // Mid gray: black contrasts more, so the shadow stays black.
        assertTrue(!otpShadowIsLight(0x9E9E9E))
    }
}
