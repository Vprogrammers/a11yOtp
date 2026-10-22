package me.vprogrammers.a11yotp.smsretriever

import me.vprogrammers.a11yotp.OtpLength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SmsCodeExtractionTest {
    private val hash = "FA+9qCX9VSu"

    @Test
    fun extractsFourSixAndEightDigitCodes() {
        assertEquals("1234", extractSmsCode("Your code is 1234\n$hash", OtpLength.FOUR))
        assertEquals("123456", extractSmsCode("Your code is 123456\n$hash", OtpLength.SIX))
        assertEquals("12345678", extractSmsCode("Your code is 12345678\n$hash", OtpLength.EIGHT))
    }

    @Test
    fun handlesRetrieverPrefix() {
        assertEquals("123456", extractSmsCode("<#> 123456 is your code.\n$hash", OtpLength.SIX))
    }

    @Test
    fun ignoresAppHashEvenWhenItContainsDigits() {
        assertNull(extractSmsCode("Welcome\nab123456789", OtpLength.SIX))
        assertNull(extractSmsCode("Welcome\n12345678901", OtpLength.SIX))
    }

    @Test
    fun ignoresLongerDigitRuns() {
        assertNull(extractSmsCode("Call 0123456789 now\n$hash", OtpLength.SIX))
        assertNull(extractSmsCode("Call +1234567 now\n$hash", OtpLength.SIX))
        assertEquals("123456", extractSmsCode("Call 0123456789, code 123456\n$hash", OtpLength.SIX))
    }

    @Test
    fun ignoresDigitsAdjacentToLettersOrSlash() {
        assertNull(extractSmsCode("ref A123456 ok\n$hash", OtpLength.SIX))
        assertNull(extractSmsCode("see /123456 ok\n$hash", OtpLength.SIX))
    }

    @Test
    fun returnsNullWithoutDigits() {
        assertNull(extractSmsCode("No code here\n$hash", OtpLength.SIX))
        assertNull(extractSmsCode("", OtpLength.SIX))
    }

    @Test
    fun returnsNullForTwoDifferentCodes() {
        assertNull(extractSmsCode("Codes 123456 and 654321\n$hash", OtpLength.SIX))
    }

    @Test
    fun returnsCodeWhenSameCodeRepeats() {
        assertEquals("123456", extractSmsCode("Code 123456. Again: 123456\n$hash", OtpLength.SIX))
    }

    @Test
    fun resolveMapsSuccessWithCode() {
        val result = resolveSmsResult(STATUS_SUCCESS, "Code 123456\n$hash", OtpLength.SIX)
        assertEquals("123456", assertIs<SmsResult.Code>(result).code)
    }

    @Test
    fun resolveMapsTimeout() {
        val result = resolveSmsResult(STATUS_TIMEOUT, null, OtpLength.SIX)
        assertEquals(SmsRetrieverFailure.TIMEOUT, assertIs<SmsResult.Failure>(result).reason)
    }

    @Test
    fun resolveReturnsNullWithoutCodeOrForOtherStatus() {
        assertNull(resolveSmsResult(STATUS_SUCCESS, "No code\n$hash", OtpLength.SIX))
        assertNull(resolveSmsResult(STATUS_SUCCESS, null, OtpLength.SIX))
        assertNull(resolveSmsResult(8, "Code 123456\n$hash", OtpLength.SIX))
    }
}
