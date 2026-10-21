package me.vprogrammers.a11yotp

/**
 * Horizontal position of an OTP component. [START] and [END] follow the surrounding layout
 * direction, so they mirror in RTL. Entries may be added in future versions; handle unknown
 * values with an `else` branch when matching exhaustively.
 */
enum class OtpAlignment { START, CENTER, END }

/** Absolute (non-mirroring) horizontal bias, -1 = left, 0 = center, 1 = right, for the given surrounding direction. */
internal fun OtpAlignment.absoluteBias(isRtl: Boolean): Float =
    when (this) {
        OtpAlignment.CENTER -> 0f
        OtpAlignment.START -> if (isRtl) 1f else -1f
        OtpAlignment.END -> if (isRtl) -1f else 1f
    }
