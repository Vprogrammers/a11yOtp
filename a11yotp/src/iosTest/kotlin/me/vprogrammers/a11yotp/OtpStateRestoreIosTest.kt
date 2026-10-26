package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun accessibility() =
    OtpFieldAccessibility(
        "Verification code",
        "Incorrect code",
        { "${it.entered} of ${it.total} digits entered" },
        { "${it.receivedDigitCount} digits" },
    )

private fun OtpField.textField() = (view as OtpFieldContainer).textField

/** Restoring from saved names, as a Swift host does with scene storage. */
class OtpStateRestoreIosTest {
    private fun roundTrip(state: OtpState): OtpState? {
        val length =
            OtpLength.fromName(state.length.name) ?: error("name")
        val charset = OtpCharset.fromName(state.charset.name) ?: error("charset")
        return OtpState.restore(length, charset, state.code, state.isError)
    }

    @Test
    fun numericStateRoundTripsThroughNames() {
        val state = OtpState(OtpLength.SIX).withInput("1234").state
        assertEquals(state, roundTrip(state))
    }

    @Test
    fun alphanumericErrorStateRoundTripsThroughNames() {
        val state = OtpState(OtpLength.EIGHT, OtpCharset.ALPHANUMERIC).withInput("AB12CD34").state.withError()
        val restored = assertNotNull(roundTrip(state))
        assertEquals(state, restored)
        assertTrue(restored.isError)
        assertTrue(restored.isComplete)
    }

    @Test
    fun unknownNamesGiveNull() {
        assertNull(OtpLength.fromName("SEVEN"))
        assertNull(OtpCharset.fromName("HEX"))
    }

    @Test
    fun restoredStateIsShownWithoutCallbacks() {
        val restored = assertNotNull(OtpState.restore(OtpLength.SIX, OtpCharset.NUMERIC, "123456", false))
        var calls = 0
        val field = OtpField(OtpState(OtpLength.SIX), accessibility(), doneButtonTitle = "Done")
        field.onComplete = { calls++ }
        field.onStateChange = { calls++ }

        field.state = restored

        assertEquals("123456", field.textField().text)
        assertEquals(0, calls)

        val created = OtpField(restored, accessibility(), doneButtonTitle = "Done")
        assertEquals("123456", created.textField().text)
    }
}
