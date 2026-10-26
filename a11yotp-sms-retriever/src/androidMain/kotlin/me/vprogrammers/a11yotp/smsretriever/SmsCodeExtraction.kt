package me.vprogrammers.a11yotp.smsretriever

import me.vprogrammers.a11yotp.OtpCharset
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

/** Shortest and longest digit runs that are plausible OTP codes, matching the supported field lengths. */
private const val MIN_PLAUSIBLE_DIGITS = 4
private const val MAX_PLAUSIBLE_DIGITS = 8

/** A run of digits not adjacent to letters, digits, `+` or `/`, so longer numbers such as phone numbers never match. */
private fun standaloneDigits(quantifier: String) = Regex("(?<![A-Za-z0-9+/])[0-9]$quantifier(?![A-Za-z0-9+/])")

/**
 * A standalone token of uppercase letters and digits, not adjacent to letters, digits, `+` or `/`.
 * Callers must additionally require a digit, so words such as `VERIFY` are never codes.
 */
private fun standaloneAlphanumeric(quantifier: String) = Regex("(?<![A-Za-z0-9+/])[A-Z0-9]$quantifier(?![A-Za-z0-9+/])")

private fun hasDigit(token: String): Boolean = token.any { it in '0'..'9' }

private fun candidates(
    body: String,
    quantifier: String,
    charset: OtpCharset,
): Sequence<String> =
    when (charset) {
        OtpCharset.NUMERIC -> {
            standaloneDigits(quantifier).findAll(body).map { it.value }
        }

        OtpCharset.ALPHANUMERIC -> {
            standaloneAlphanumeric(quantifier).findAll(body).map { it.value }.filter(::hasDigit)
        }
    }

/**
 * Extracts the verification code from an SMS Retriever [message].
 *
 * Drops the trailing 11-character app hash, then looks for a standalone run of exactly
 * [OtpLength.digitCount] digits. A run counts as standalone when it is not adjacent to
 * letters, digits, `+` or `/`, so longer numbers such as phone numbers are ignored.
 * Returns null when there is no match or when two different codes match.
 *
 * With [OtpCharset.ALPHANUMERIC] a candidate is a standalone token of exactly that many uppercase
 * letters and digits with at least one digit. Lowercase or mixed-case words never match, and
 * letter-only codes are not recognised.
 */
internal fun extractSmsCode(
    message: String,
    length: OtpLength,
    charset: OtpCharset = OtpCharset.NUMERIC,
): String? {
    val body = stripAppHash(message)
    val matches = candidates(body, "{${length.digitCount}}", charset).toSet()
    return matches.singleOrNull()
}

/** True when [message] has a standalone candidate of 4 to 8 characters, whatever the field length. */
private fun hasPlausibleCodeOfAnyLength(
    message: String,
    charset: OtpCharset,
): Boolean = candidates(stripAppHash(message), "{$MIN_PLAUSIBLE_DIGITS,$MAX_PLAUSIBLE_DIGITS}", charset).any()

private fun stripAppHash(message: String): String {
    val trimmed = message.trimEnd()
    val start = trimmed.lastIndexOfAny(charArrayOf(' ', '\n', '\r', '\t')) + 1
    val last = trimmed.substring(start)
    return if (appHashToken.matches(last)) trimmed.substring(0, start) else trimmed
}

private fun hasExactLengthRun(
    message: String,
    length: OtpLength,
    charset: OtpCharset,
): Boolean = candidates(stripAppHash(message), "{${length.digitCount}}", charset).any()

/** Maps a retriever [statusCode] and [message] to a result, or null when there is nothing to deliver. */
internal fun resolveSmsResult(
    statusCode: Int,
    message: String?,
    length: OtpLength,
    charset: OtpCharset = OtpCharset.NUMERIC,
): SmsResult? =
    when (statusCode) {
        STATUS_SUCCESS -> {
            val code = message?.let { extractSmsCode(it, length, charset) }
            when {
                code != null -> {
                    SmsResult.Code(code)
                }

                // Two different codes of the right length are ambiguous, not a length mismatch.
                message != null && !hasExactLengthRun(message, length, charset) &&
                    hasPlausibleCodeOfAnyLength(message, charset) -> {
                    SmsResult.Failure(SmsRetrieverFailure.CODE_LENGTH_MISMATCH)
                }

                else -> {
                    null
                }
            }
        }

        STATUS_TIMEOUT -> {
            SmsResult.Failure(SmsRetrieverFailure.TIMEOUT)
        }

        else -> {
            null
        }
    }
