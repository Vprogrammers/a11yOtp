package me.vprogrammers.a11yotp

/**
 * Characters an [OtpState.code] may contain.
 *
 * More charsets may be added, so `when` expressions in Kotlin and `switch` statements
 * in Swift should handle unknown entries. Saved state stores entries by [name], so entries are
 * never renamed or removed.
 */
enum class OtpCharset {
    /** ASCII digits `0`-`9`. */
    NUMERIC,

    /**
     * ASCII letters `A`-`Z` and digits `0`-`9`. Letters are case-insensitive and stored in
     * uppercase. Look-alike characters such as `O` and `0`, or `I` and `1`, are never merged.
     */
    ALPHANUMERIC,
}
