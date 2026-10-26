package me.vprogrammers.a11yotp

/**
 * Supported one-time code lengths, in characters.
 *
 * More lengths may be added, so `when` expressions in Kotlin and `switch` statements
 * in Swift should handle unknown entries. Saved state stores entries by [name], so entries are
 * never renamed or removed.
 */
enum class OtpLength(
    val digitCount: Int,
) {
    FOUR(4),
    SIX(6),
    EIGHT(8),
}
