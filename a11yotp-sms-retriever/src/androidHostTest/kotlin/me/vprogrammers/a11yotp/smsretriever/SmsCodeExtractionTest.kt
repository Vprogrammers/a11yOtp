package me.vprogrammers.a11yotp.smsretriever

import me.vprogrammers.a11yotp.OtpCharset
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

    private fun mismatch(
        message: String,
        length: OtpLength,
    ) = resolveSmsResult(STATUS_SUCCESS, message, length)

    @Test
    fun reportsLengthMismatchForEveryFieldAndReceivedLength() {
        val codes = mapOf(4 to "1234", 5 to "12345", 6 to "123456", 7 to "1234567", 8 to "12345678")
        for (field in OtpLength.entries) {
            for ((received, code) in codes) {
                val result = mismatch("Your code is $code\n$hash", field)
                if (received == field.digitCount) {
                    assertEquals(code, assertIs<SmsResult.Code>(result).code)
                } else {
                    val failure = assertIs<SmsResult.Failure>(result, "$field $received")
                    assertEquals(SmsRetrieverFailure.CODE_LENGTH_MISMATCH, failure.reason)
                }
            }
        }
    }

    @Test
    fun exactLengthCodeWinsOverOtherLengthRuns() {
        val result = mismatch("Ref 12345678, code 123456\n$hash", OtpLength.SIX)
        assertEquals("123456", assertIs<SmsResult.Code>(result).code)
    }

    @Test
    fun twoDifferentExactLengthCodesAreNotAMismatch() {
        assertNull(mismatch("Codes 123456 and 654321\n$hash", OtpLength.SIX))
        assertNull(mismatch("Codes 123456, 654321 and 1234\n$hash", OtpLength.SIX))
    }

    @Test
    fun longNumbersAndShortRunsAreNotAMismatch() {
        assertNull(mismatch("Call 0123456789 now\n$hash", OtpLength.SIX))
        assertNull(mismatch("Call +1234567 now\n$hash", OtpLength.SIX))
        assertNull(mismatch("Only 123 here\n$hash", OtpLength.SIX))
        assertNull(mismatch("ref A12345678 ok\n$hash", OtpLength.SIX))
        assertNull(mismatch("No code\n$hash", OtpLength.SIX))
    }

    @Test
    fun appHashDigitsAreNotAMismatch() {
        assertNull(mismatch("Welcome\n12345678901", OtpLength.SIX))
    }

    @Test
    fun mismatchOnlyOnSuccessAndNeverExposesDigits() {
        assertNull(resolveSmsResult(8, "Code 1234\n$hash", OtpLength.SIX))
        assertNull(resolveSmsResult(STATUS_SUCCESS, null, OtpLength.SIX))
        val failure = assertIs<SmsResult.Failure>(mismatch("Code 1234\n$hash", OtpLength.SIX))
        assertEquals("CODE_LENGTH_MISMATCH", failure.reason.name)
        assertEquals(false, failure.toString().contains("1234"))
    }

    private val alnum = OtpCharset.ALPHANUMERIC

    @Test
    fun alphanumericFindsCodeAndIgnoresRealHash() {
        assertEquals("K7Q2MX", extractSmsCode("Your code is K7Q2MX\n\nwlhIal9KIdf", OtpLength.SIX, alnum))
    }

    @Test
    fun alphanumericStripsHashEvenWhenItLooksLikeACode() {
        assertNull(extractSmsCode("Welcome\nAB12CD34EF5", OtpLength.SIX, alnum))
        assertEquals("K7Q2MX", extractSmsCode("Code K7Q2MX\nAB12CD34EF5", OtpLength.SIX, alnum))
    }

    @Test
    fun alphanumericIgnoresWordsWithoutDigitsOrWithLowercase() {
        assertEquals("K7Q2MX", extractSmsCode("Please verify: K7Q2MX", OtpLength.SIX, alnum))
        assertNull(extractSmsCode("VERIFY", OtpLength.SIX, alnum))
        assertNull(extractSmsCode("Example code VERIFY now", OtpLength.SIX, alnum))
        assertNull(extractSmsCode("Your code is k7q2mx", OtpLength.SIX, alnum))
        assertNull(extractSmsCode("Your code is K7q2MX", OtpLength.SIX, alnum))
    }

    @Test
    fun alphanumericTwoDifferentCodesAreAmbiguous() {
        assertNull(extractSmsCode("Codes K7Q2MX or B8R3NY", OtpLength.SIX, alnum))
        assertEquals("K7Q2MX", extractSmsCode("Codes K7Q2MX or K7Q2MX", OtpLength.SIX, alnum))
    }

    @Test
    fun alphanumericAcceptsPureDigitCode() {
        assertEquals("482913", extractSmsCode("Code 482913\n$hash", OtpLength.SIX, alnum))
    }

    @Test
    fun alphanumericRespectsBoundaries() {
        assertNull(extractSmsCode("see /K7Q2MX ok", OtpLength.SIX, alnum))
        assertNull(extractSmsCode("ref K7Q2MXZ ok", OtpLength.SIX, alnum))
    }

    @Test
    fun alphanumericLengthMismatch() {
        val failure =
            assertIs<SmsResult.Failure>(resolveSmsResult(STATUS_SUCCESS, "Code K7Q2\n$hash", OtpLength.SIX, alnum))
        assertEquals("CODE_LENGTH_MISMATCH", failure.reason.name)
        assertEquals(false, failure.toString().contains("K7Q2"))
        assertNull(resolveSmsResult(STATUS_SUCCESS, "Please VERIFY\n$hash", OtpLength.SIX, alnum))
        assertNull(resolveSmsResult(STATUS_SUCCESS, "Codes K7Q2MX or B8R3NY", OtpLength.SIX, alnum))
    }

    @Test
    fun numericIsUnchangedByAlphanumericSupport() {
        assertNull(extractSmsCode("Your code is K7Q2MX", OtpLength.SIX))
        assertNull(extractSmsCode("Your code is K7Q2MX", OtpLength.SIX, OtpCharset.NUMERIC))
        assertEquals("482913", extractSmsCode("Code 482913", OtpLength.SIX, OtpCharset.NUMERIC))
        assertNull(resolveSmsResult(STATUS_SUCCESS, "Code K7Q2\n$hash", OtpLength.SIX))
    }

    @Test
    fun hashLessMessagesAreUnchangedByHashStripping() {
        assertEquals("123456", extractSmsCode("Your code is 123456", OtpLength.SIX))
        assertEquals("123456", extractSmsCode("Your code is 123456\n", OtpLength.SIX))
        assertEquals("K7Q2MX", extractSmsCode("Code: K7Q2MX", OtpLength.SIX, alnum))
    }
}
