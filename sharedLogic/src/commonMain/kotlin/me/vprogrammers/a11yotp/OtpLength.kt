package me.vprogrammers.a11yotp

/**
 * Supported numeric one-time code lengths.
 *
 * More lengths may be added, so `when` expressions in Kotlin and `switch` statements
 * in Swift should handle unknown entries.
 */
enum class OtpLength(
    val digitCount: Int,
) {
    FOUR(4),
    SIX(6),
}
