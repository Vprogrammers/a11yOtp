package me.vprogrammers.a11yotp

import androidx.compose.runtime.Immutable

/**
 * Required, localized accessibility text for an [OtpField]. There are deliberately no
 * defaults so the component cannot be shown without it.
 *
 * @property label accessible name of the field; should match any visible label.
 * @property errorMessage shown below the cells and exposed as the semantics error while
 * [OtpState.isError] is true.
 * @property progressDescription state description such as "3 of 6 digits entered".
 */
@Immutable
class OtpFieldAccessibility(
    val label: String,
    val errorMessage: String,
    val progressDescription: (entered: Int, total: Int) -> String,
) {
    init {
        require(label.isNotBlank()) { "label must not be blank" }
        require(errorMessage.isNotBlank()) { "errorMessage must not be blank" }
    }
}

/**
 * Required, localized accessibility text for an [OtpResendButton].
 *
 * @property label accessible name and visible text when resending is available.
 * @property countdownDescription visible text (and therefore accessible name) while the
 * countdown runs. It should begin with the label, for example "Resend code in 25 s", so the
 * visible label stays contained in the accessible name (WCAG 2.5.3).
 */
@Immutable
class OtpResendAccessibility(
    val label: String,
    val countdownDescription: (seconds: Int) -> String,
) {
    init {
        require(label.isNotBlank()) { "label must not be blank" }
    }
}
