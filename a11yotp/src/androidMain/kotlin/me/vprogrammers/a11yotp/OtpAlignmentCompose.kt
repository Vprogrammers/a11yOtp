package me.vprogrammers.a11yotp

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.text.style.TextAlign

internal fun OtpAlignment.absoluteHorizontal(isRtl: Boolean): Alignment.Horizontal =
    BiasAbsoluteAlignment.Horizontal(absoluteBias(isRtl))

internal fun OtpAlignment.absoluteBox(isRtl: Boolean): Alignment = BiasAbsoluteAlignment(absoluteBias(isRtl), -1f)

internal fun OtpAlignment.absoluteTextAlign(isRtl: Boolean): TextAlign =
    when (absoluteBias(isRtl)) {
        0f -> TextAlign.Center
        1f -> TextAlign.Right
        else -> TextAlign.Left
    }
