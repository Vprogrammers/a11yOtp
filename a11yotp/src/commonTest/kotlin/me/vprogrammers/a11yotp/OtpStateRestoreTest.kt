package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OtpStateRestoreTest {
    private fun roundTrip(state: OtpState): OtpState? = OtpState.restore(state.length, state.code, state.isError)

    @Test
    fun everyReachableStateRoundTrips() {
        for (length in OtpLength.entries) {
            val empty = OtpState(length)
            val partial = empty.typed("12")
            val complete = empty.typed("1234567890".take(length.digitCount))
            for (state in listOf(
                empty,
                partial,
                complete,
                partial.withError(),
                complete.withError(),
                empty.withError(),
            )) {
                assertEquals(state, roundTrip(state), "$state")
            }
        }
    }

    @Test
    fun aRestoredPartialCodeContinuesLikeTyping() {
        val restored = OtpState.restore(OtpLength.SIX, "123", isError = false)!!

        val result = restored.withInput("1234")

        assertEquals("1234", result.state.code)
        assertNull(result.rejection)
    }

    @Test
    fun aRestoredCompleteCodeDoesNotCompleteAgainOnAnIgnoredDigit() {
        val restored = OtpState.restore(OtpLength.FOUR, "1234", isError = false)!!

        assertTrue(restored.isComplete)
        assertFalse(restored.withInput("12345").didComplete)
    }

    @Test
    fun theErrorMarkIsRestoredAndClearedByTheNextEdit() {
        val restored = OtpState.restore(OtpLength.SIX, "123456", isError = true)!!

        assertTrue(restored.isError)
        assertFalse(restored.withInput("12345").state.isError)
    }

    @Test
    fun invalidCodesAreNotRestored() {
        for (code in listOf("1234567", "12a", "12 34", "12-34", "٤٨", "１２")) {
            assertNull(OtpState.restore(OtpLength.SIX, code, isError = false), code)
        }
    }
}
