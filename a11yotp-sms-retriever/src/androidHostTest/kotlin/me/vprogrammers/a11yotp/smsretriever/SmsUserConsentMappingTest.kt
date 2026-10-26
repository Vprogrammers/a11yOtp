package me.vprogrammers.a11yotp.smsretriever

import me.vprogrammers.a11yotp.OtpCharset
import me.vprogrammers.a11yotp.OtpLength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class SmsUserConsentMappingTest {
    private val six = OtpLength.SIX
    private val numeric = OtpCharset.NUMERIC
    private val alnum = OtpCharset.ALPHANUMERIC

    private fun failure(outcome: ConsentBroadcastOutcome) = assertIs<ConsentBroadcastOutcome.Failure>(outcome).reason

    @Test
    fun successWithIntentLaunches() {
        assertEquals(ConsentBroadcastOutcome.Launch, resolveConsentBroadcast(STATUS_SUCCESS, hasIntent = true))
    }

    @Test
    fun successWithoutIntentIsUnavailable() {
        assertEquals(SmsUserConsentFailure.UNAVAILABLE, failure(resolveConsentBroadcast(STATUS_SUCCESS, false)))
    }

    @Test
    fun timeoutIsReported() {
        assertEquals(SmsUserConsentFailure.TIMEOUT, failure(resolveConsentBroadcast(STATUS_TIMEOUT, false)))
        assertEquals(SmsUserConsentFailure.TIMEOUT, failure(resolveConsentBroadcast(STATUS_TIMEOUT, true)))
    }

    @Test
    fun unknownStatusIsUnavailable() {
        assertEquals(SmsUserConsentFailure.UNAVAILABLE, failure(resolveConsentBroadcast(8, true)))
        assertEquals(SmsUserConsentFailure.UNAVAILABLE, failure(resolveConsentBroadcast(-1, false)))
    }

    @Test
    fun allowedMessageWithoutHashDeliversNumericCode() {
        val result = assertIs<ConsentResult.Code>(resolveConsentResult(RESULT_OK, "Your code is 482913", six, numeric))
        assertEquals("482913", result.code)
    }

    @Test
    fun allowedMessageDeliversAlphanumericCode() {
        val result = assertIs<ConsentResult.Code>(resolveConsentResult(RESULT_OK, "Code: K7Q2MX", six, alnum))
        assertEquals("K7Q2MX", result.code)
    }

    @Test
    fun allowedMessageWithHashStillWorks() {
        val message = "<#> 482913\nFA+9qCX9VSu"
        val result = assertIs<ConsentResult.Code>(resolveConsentResult(RESULT_OK, message, six, numeric))
        assertEquals("482913", result.code)
    }

    @Test
    fun lengthMismatchIsReported() {
        val result = assertIs<ConsentResult.Failure>(resolveConsentResult(RESULT_OK, "Your code is 1234", six, numeric))
        assertEquals(SmsUserConsentFailure.CODE_LENGTH_MISMATCH, result.reason)
    }

    @Test
    fun ambiguousOrCodelessOrNullMessageDeliversNothing() {
        assertNull(resolveConsentResult(RESULT_OK, "Codes 123456 or 654321", six, numeric))
        assertNull(resolveConsentResult(RESULT_OK, "No code here", six, numeric))
        assertNull(resolveConsentResult(RESULT_OK, null, six, numeric))
    }

    @Test
    fun cancelledIsDeniedEvenWithMessage() {
        val result = assertIs<ConsentResult.Failure>(resolveConsentResult(RESULT_CANCELED, null, six, numeric))
        assertEquals(SmsUserConsentFailure.DENIED, result.reason)
        assertIs<ConsentResult.Failure>(resolveConsentResult(RESULT_CANCELED, "Code 123456", six, numeric))
    }

    @Test
    fun otherResultCodesDeliverNothing() {
        assertNull(resolveConsentResult(1, "Code 123456", six, numeric))
    }

    @Test
    fun codeNeverAppearsInToString() {
        val result = resolveConsentResult(RESULT_OK, "Your code is 482913", six, numeric)
        assertFalse(result.toString().contains("482913"))
        assertEquals("Code", result.toString())
        assertEquals("Code", classifyMessage("Code 482913", six, numeric).toString())
    }
}
