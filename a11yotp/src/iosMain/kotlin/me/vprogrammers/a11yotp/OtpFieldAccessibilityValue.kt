package me.vprogrammers.a11yotp

/**
 * VoiceOver value of the logical field. Unmasked, it spells the entered digits so users can review
 * them, followed by the progress. Masked, it contains only the progress, never the digits. While
 * [OtpState.isError] is true the error message is appended, so the error is part of the value and
 * not only announced once.
 */
internal fun otpAccessibilityValue(
    state: OtpState,
    accessibility: OtpFieldAccessibility,
    masked: Boolean,
): String {
    val parts = mutableListOf<String>()
    if (!masked && state.code.isNotEmpty()) parts += state.code.toList().joinToString(" ")
    parts += accessibility.progressDescription(OtpProgress(state.enteredDigitCount, state.length.digitCount))
    if (state.isError) parts += accessibility.errorMessage
    return parts.joinToString(", ")
}
