package me.vprogrammers.a11yotp.sample

/** Seconds before the sample apps allow resending a code. */
const val SAMPLE_RESEND_SECONDS = 30

/**
 * Fake server check shared by both sample apps: a code of only zeros (0000, 000000, 00000000) is
 * rejected, so the error state is easy to try. Real apps verify the code with their server.
 */
fun isSampleCodeRejected(code: String): Boolean = code.all { it == '0' }
