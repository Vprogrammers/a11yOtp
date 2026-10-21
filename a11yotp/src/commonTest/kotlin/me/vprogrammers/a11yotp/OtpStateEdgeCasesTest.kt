package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OtpStateEdgeCasesTest {
    @Test
    fun eightDigitFieldReplacesCompleteCodeWithAppendedFullCode() {
        val complete = OtpState(OtpLength.EIGHT).withInput("12345678").state

        val result = complete.withInput("12345678 8765-4321")

        assertEquals("87654321", result.state.code)
        assertTrue(result.didComplete)
    }

    @Test
    fun appendedSameCodeIsNotAReplacement() {
        val complete = OtpState(OtpLength.FOUR).withInput("1234").state

        val result = complete.withInput("12341234")

        assertSame(complete, result.state)
        assertFalse(result.didComplete)
    }

    @Test
    fun appendedShortCodeToCompleteFieldIsNotAReplacement() {
        val complete = OtpState(OtpLength.SIX).withInput("123456").state

        val result = complete.withInput("123456789")

        assertSame(complete, result.state)
        assertFalse(result.didComplete)
    }

    @Test
    fun appendedInputToIncompleteFieldIsPlainOverflow() {
        val partial = OtpState(OtpLength.FOUR).withInput("12").state

        val result = partial.withInput("125678")

        assertEquals("1256", result.state.code)
        assertTrue(result.didComplete)
    }

    @Test
    fun appendedFullCodeWithUnsupportedCharacterIsRejected() {
        val complete = OtpState(OtpLength.FOUR).withInput("1234").state

        val result = complete.withInput("12345x78")

        assertSame(complete, result.state)
        assertFalse(result.didComplete)
    }

    @Test
    fun inputIsRejectedForEveryLengthWithoutChangingPartialState() {
        for (length in OtpLength.entries) {
            val partial = OtpState(length).withInput("12").state

            assertSame(partial, partial.withInput("1a").state, "$length")
        }
    }

    @Test
    fun overlongInputCompletesForEachLength() {
        for (length in OtpLength.entries) {
            val result = OtpState(length).withInput("1234567890123")

            assertEquals("1234567890123".take(length.digitCount), result.state.code)
            assertTrue(result.didComplete)
        }
    }

    @Test
    fun unchangedInputDoesNotClearErrorOrComplete() {
        val partial = OtpState(OtpLength.SIX).withInput("123").state.withError()

        val result = partial.withInput("1 2 3")

        assertSame(partial, result.state)
        assertTrue(result.state.isError)
    }

    @Test
    fun stateEqualityDistinguishesCode() {
        assertNotEquals(OtpState(OtpLength.FOUR).withInput("1").state, OtpState(OtpLength.FOUR).withInput("2").state)
    }

    @Test
    fun lengthsExposeTheirDigitCounts() {
        assertEquals(listOf(4, 6, 8), OtpLength.entries.map { it.digitCount })
    }

    @Test
    fun errorMarkerDoesNotAffectCompletion() {
        val rejected = OtpState(OtpLength.FOUR).withInput("1234").state.withError()

        assertTrue(rejected.isComplete)
        assertEquals(4, rejected.enteredDigitCount)
    }

    @Test
    fun contrastBelowWholeHundredthsIsTruncatedNotRounded() {
        // Ratio is about 4.4797; rounding would print 4.48, truncation prints 4.47.
        val warning = OtpContrast.warning("C", "f", 0x777777, "b", 0xFFFFFF, OtpContrastRequirement.TEXT)

        assertTrue(warning!!.contains("has contrast 4.47:1"), warning)
    }

    @Test
    fun hexInWarningIsUppercaseAndPadded() {
        val warning = OtpContrast.warning("C", "f", 0x00000A, "b", 0x000000, OtpContrastRequirement.TEXT)

        assertTrue(warning!!.contains("f (#00000A) on b (#000000)"), warning)
    }

    @Test
    fun alphaBitsAreIgnoredByTheRatio() {
        assertEquals(OtpContrast.ratio(0x767676, 0xFFFFFF), OtpContrast.ratio(0xFF767676.toInt(), 0xFFFFFF))
    }

    @Test
    fun identicalColorsFailEveryRequirement() {
        for (requirement in OtpContrastRequirement.entries) {
            assertTrue(OtpContrast.warning("C", "f", 0x336699, "b", 0x336699, requirement) != null, "$requirement")
        }
    }

    @Test
    fun blackOnWhitePassesEveryRequirement() {
        for (requirement in OtpContrastRequirement.entries) {
            assertNull(OtpContrast.warning("C", "f", 0, "b", 0xFFFFFF, requirement), "$requirement")
        }
    }

    @Test
    fun requirementThresholdsAreWcagValues() {
        assertEquals(4.5, OtpContrastRequirement.TEXT.minimumRatio)
        assertEquals(3.0, OtpContrastRequirement.NON_TEXT.minimumRatio)
        assertEquals(3.0, OtpContrastRequirement.FOCUS_INDICATOR.minimumRatio)
    }

    @Test
    fun ratioAtExactlyTheThresholdPasses() {
        // Black on a gray whose ratio is just above 3.0 passes; one step darker fails.
        var threshold = -1
        for (gray in 0..255) {
            val rgb = (gray shl 16) or (gray shl 8) or gray
            if (OtpContrast.ratio(rgb, 0xFFFFFF) < 3.0) {
                threshold = gray - 1
                break
            }
        }
        val pass = (threshold shl 16) or (threshold shl 8) or threshold
        val fail = ((threshold + 1) shl 16) or ((threshold + 1) shl 8) or (threshold + 1)

        assertNull(OtpContrast.warning("C", "f", pass, "b", 0xFFFFFF, OtpContrastRequirement.NON_TEXT))
        assertTrue(OtpContrast.warning("C", "f", fail, "b", 0xFFFFFF, OtpContrastRequirement.NON_TEXT) != null)
    }
}
