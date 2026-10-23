package me.vprogrammers.a11yotp

/**
 * Progress of the entered code, passed to [OtpFieldAccessibility.progressDescription] so hosts
 * can localize with plural rules. A value object rather than primitive lambda parameters so the
 * callback stays idiomatic when called from Swift.
 */
class OtpProgress internal constructor(
    val entered: Int,
    val total: Int,
) {
    override fun equals(other: Any?): Boolean = other is OtpProgress && other.entered == entered && other.total == total

    override fun hashCode(): Int = 31 * entered + total

    override fun toString(): String = "OtpProgress(entered=$entered, total=$total)"
}

/**
 * Remaining resend time, passed to [OtpResendAccessibility.countdownDescription] so hosts can
 * localize with plural rules.
 */
class OtpCountdown internal constructor(
    val secondsRemaining: Int,
) {
    override fun equals(other: Any?): Boolean = other is OtpCountdown && other.secondsRemaining == secondsRemaining

    override fun hashCode(): Int = secondsRemaining

    override fun toString(): String = "OtpCountdown(secondsRemaining=$secondsRemaining)"
}

/**
 * Required, localized accessibility text for an OTP field. There are deliberately no
 * defaults so the component cannot be shown without it.
 *
 * @property label accessible name of the field; should match any visible label.
 * @property errorMessage shown below the cells and exposed as the semantics error while
 * [OtpState.isError] is true.
 * @property progressDescription state description such as "3 of 6 digits entered".
 * @property inputRejectedMessage text for a rejected multi-digit input (see [OtpInputRejection]),
 * for example "Code has 4 digits, but 6 are expected". Localize it with plural rules from the
 * counts. It is shown below the field and announced once, like the error message, but it does not
 * put the field into an error state. It must not contain the digits.
 */
class OtpFieldAccessibility(
    val label: String,
    val errorMessage: String,
    val progressDescription: (OtpProgress) -> String,
    val inputRejectedMessage: (OtpInputRejection) -> String,
) {
    init {
        require(label.isNotBlank()) { "label must not be blank" }
        require(errorMessage.isNotBlank()) { "errorMessage must not be blank" }
    }
}

/**
 * The host's [OtpFieldAccessibility.inputRejectedMessage] for [rejection], or null with a logged
 * warning when it is blank, because a blank message would be shown and announced as nothing.
 */
internal fun OtpFieldAccessibility.rejectionMessageOrNull(rejection: OtpInputRejection): String? {
    val message = inputRejectedMessage(rejection)
    if (message.isNotBlank()) return message
    logA11yWarning("OtpFieldAccessibility.inputRejectedMessage returned blank text; the rejection is not shown")
    return null
}

/** Required, localized accessibility text for an OTP verify button; [label] is also the visible text. */
class OtpVerifyAccessibility(
    val label: String,
) {
    init {
        require(label.isNotBlank()) { "label must not be blank" }
    }
}

/**
 * Required, localized accessibility text for an OTP resend button.
 *
 * @property label accessible name and visible text when resending is available.
 * @property countdownDescription visible text (and therefore accessible name) while the
 * countdown runs. It should begin with the label, for example "Resend code in 25 s", so the
 * visible label stays contained in the accessible name (WCAG 2.5.3).
 */
class OtpResendAccessibility(
    val label: String,
    val countdownDescription: (OtpCountdown) -> String,
) {
    init {
        require(label.isNotBlank()) { "label must not be blank" }
    }
}
