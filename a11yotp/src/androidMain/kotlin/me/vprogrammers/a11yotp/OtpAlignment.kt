package me.vprogrammers.a11yotp

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.text.style.TextAlign

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

internal fun OtpAlignment.absoluteHorizontal(isRtl: Boolean): Alignment.Horizontal =
    BiasAbsoluteAlignment.Horizontal(absoluteBias(isRtl))

internal fun OtpAlignment.absoluteBox(isRtl: Boolean): Alignment = BiasAbsoluteAlignment(absoluteBias(isRtl), -1f)

internal fun OtpAlignment.absoluteTextAlign(isRtl: Boolean): TextAlign =
    when (absoluteBias(isRtl)) {
        0f -> TextAlign.Center
        1f -> TextAlign.Right
        else -> TextAlign.Left
    }
