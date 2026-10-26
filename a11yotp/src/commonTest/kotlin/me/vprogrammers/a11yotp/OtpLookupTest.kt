package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OtpLookupTest {
    @Test
    fun everyLengthIsFoundByItsName() {
        for (length in OtpLength.entries) {
            assertEquals(length, OtpLength.fromName(length.name))
        }
    }

    @Test
    fun unknownLengthsAreNull() {
        for (name in listOf("", "six", "Six", " SIX", "SEVEN", "6")) {
            assertNull(OtpLength.fromName(name), name)
        }
    }

    @Test
    fun everyCharsetIsFoundByItsName() {
        for (charset in OtpCharset.entries) {
            assertEquals(charset, OtpCharset.fromName(charset.name))
        }
    }

    @Test
    fun unknownCharsetsAreNull() {
        for (name in listOf("", "numeric", "Alphanumeric", "ALPHA", " NUMERIC")) {
            assertNull(OtpCharset.fromName(name), name)
        }
    }

    @Test
    fun savedNamesRestoreTheState() {
        val state = OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC).typed("K7Q").withError()

        val restored =
            OtpState.restore(
                OtpLength.fromName(state.length.name)!!,
                OtpCharset.fromName(state.charset.name)!!,
                state.code,
                state.isError,
            )

        assertEquals(state, restored)
    }
}
