package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class OtpConfigurationTest {
    @Test
    fun fieldAccessibilityTextMustNotBeBlank() {
        assertFailsWith<IllegalArgumentException> { OtpFieldAccessibility(" ", "Error", { "x" }, { "x" }) }
        assertFailsWith<IllegalArgumentException> { OtpFieldAccessibility("Code", "", { "x" }, { "x" }) }
    }

    @Test
    fun verifyAndResendAccessibilityTextMustNotBeBlank() {
        assertFailsWith<IllegalArgumentException> { OtpVerifyAccessibility("") }
        assertFailsWith<IllegalArgumentException> { OtpResendAccessibility(" ") { "x" } }
    }

    @Test
    fun descriptionsReceiveValueObjects() {
        val field =
            OtpFieldAccessibility("Code", "Wrong", {
                "${it.entered} of ${it.total}"
            }, { "${it.receivedDigitCount}/${it.expectedDigitCount}" })
        assertEquals("3 of 6", field.progressDescription(OtpProgress(3, 6)))
        assertEquals("4/6", field.inputRejectedMessage(OtpInputRejection(6, 4)))
        val resend = OtpResendAccessibility("Resend") { "Resend in ${it.secondsRemaining} s" }
        assertEquals("Resend in 25 s", resend.countdownDescription(OtpCountdown(25)))
    }

    @Test
    fun valueObjectsHaveValueSemantics() {
        assertEquals(OtpProgress(1, 4), OtpProgress(1, 4))
        assertEquals(OtpProgress(1, 4).hashCode(), OtpProgress(1, 4).hashCode())
        assertNotEquals(OtpProgress(1, 4), OtpProgress(2, 4))
        assertEquals("OtpProgress(entered=1, total=4)", OtpProgress(1, 4).toString())
        assertEquals(OtpCountdown(5), OtpCountdown(5))
        assertNotEquals(OtpCountdown(5), OtpCountdown(6))
        assertEquals("OtpCountdown(secondsRemaining=5)", OtpCountdown(5).toString())
    }

    @Test
    fun absoluteBiasTruthTable() {
        assertEquals(-1f, OtpAlignment.START.absoluteBias(isRtl = false))
        assertEquals(1f, OtpAlignment.START.absoluteBias(isRtl = true))
        assertEquals(0f, OtpAlignment.CENTER.absoluteBias(isRtl = false))
        assertEquals(0f, OtpAlignment.CENTER.absoluteBias(isRtl = true))
        assertEquals(1f, OtpAlignment.END.absoluteBias(isRtl = false))
        assertEquals(-1f, OtpAlignment.END.absoluteBias(isRtl = true))
    }

    @Test
    fun maskEquality() {
        assertEquals(OtpMask.Character('*'), OtpMask.Character('*'))
        assertEquals(OtpMask.Character('*').hashCode(), OtpMask.Character('*').hashCode())
        assertNotEquals<OtpMask>(OtpMask.Character('*'), OtpMask.Character('#'))
        assertNotEquals<OtpMask>(OtpMask.None, OtpMask.FilledCell)
        assertEquals(OtpMask.Character(), OtpMask.Character('*'))
        assertEquals("Character(*)", OtpMask.Character('*').toString())
    }
}
