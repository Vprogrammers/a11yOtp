package me.vprogrammers.a11yotp.smsretriever

import me.vprogrammers.a11yotp.OtpLength

/** Value of `CommonStatusCodes.SUCCESS`, duplicated so the mapping stays testable without Play Services. */
internal const val STATUS_SUCCESS = 0

/** Value of `CommonStatusCodes.TIMEOUT`. */
internal const val STATUS_TIMEOUT = 15

/** Outcome of one SMS Retriever broadcast. */
internal sealed interface SmsResult {
    class Code(
        val code: String,
    ) : SmsResult {
        // Omits the code so it does not end up in logs.
        override fun toString(): String = "Code"
    }

    class Failure(
        val reason: SmsRetrieverFailure,
    ) : SmsResult
}

private val appHashToken = Regex("[A-Za-z0-9+/=]{11}")

/**
 * Extracts the verification code from an SMS Retriever [message].
 *
 * Drops the trailing 11-character app hash, then looks for a standalone run of exactly
 * [OtpLength.digitCount] digits. A run counts as standalone when it is not adjacent to
 * letters, digits, `+` or `/`, so longer numbers such as phone numbers are ignored.
 * Returns null when there is no match or when two different codes match.
 */
internal fun extractSmsCode(
    message: String,
    length: OtpLength,
): String? {
    val body = stripAppHash(message)
    val count = length.digitCount
    val pattern = Regex("(?<![A-Za-z0-9+/])[0-9]{$count}(?![A-Za-z0-9+/])")
    val matches = pattern.findAll(body).map { it.value }.toSet()
    return matches.singleOrNull()
}

private fun stripAppHash(message: String): String {
    val trimmed = message.trimEnd()
    val start = trimmed.lastIndexOfAny(charArrayOf(' ', '\n', '\r', '\t')) + 1
    val last = trimmed.substring(start)
    return if (appHashToken.matches(last)) trimmed.substring(0, start) else trimmed
}

/** Maps a retriever [statusCode] and [message] to a result, or null when there is nothing to deliver. */
internal fun resolveSmsResult(
    statusCode: Int,
    message: String?,
    length: OtpLength,
): SmsResult? =
    when (statusCode) {
        STATUS_SUCCESS -> message?.let { extractSmsCode(it, length) }?.let { SmsResult.Code(it) }
        STATUS_TIMEOUT -> SmsResult.Failure(SmsRetrieverFailure.TIMEOUT)
        else -> null
    }
