package me.vprogrammers.a11yotp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * "Resend code" button with a countdown. The host owns the timer and passes
 * [secondsRemaining]; the button is disabled while it is above zero. The remaining time is
 * the button text, which is also its accessible name (WCAG 2.5.3 Label in Name). There is
 * no live region and no per-second announcement; verify the screen-reader behaviour on a device.
 *
 * Hosts should not make resend permanently unavailable and should let users request more
 * time or another code (WCAG 2.2.1 Timing Adjustable). Localize [accessibility] text. Pass the same [alignment] as [OtpField] to keep them aligned.
 */
@Composable
fun OtpResendButton(
    secondsRemaining: Int,
    onResend: () -> Unit,
    accessibility: OtpResendAccessibility,
    modifier: Modifier = Modifier,
    alignment: OtpAlignment = OtpAlignment.CENTER,
    style: OtpButtonStyle = OtpButtonDefaults.style(),
) {
    val enabled = secondsRemaining <= 0
    val countdown = accessibility.countdownDescription(secondsRemaining)
    OtpButtonSurface(
        text = if (enabled) accessibility.label else countdown,
        enabled = enabled,
        onClick = onResend,
        alignment = alignment,
        style = style,
        modifier = modifier,
    )
}
