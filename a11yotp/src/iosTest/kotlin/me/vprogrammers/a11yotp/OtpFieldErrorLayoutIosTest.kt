package me.vprogrammers.a11yotp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSizeMake
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class OtpFieldErrorLayoutIosTest {
    // Regression: the error row used the whole field width as the icon width, which left the
    // message 1pt wide, wrapped one character per line, and made the field hundreds of points tall.
    @Test
    fun errorMessageAddsOneShortRowBelowTheCells() {
        val field =
            OtpField(
                OtpState(OtpLength.FOUR),
                OtpFieldAccessibility(
                    label = "Verification code",
                    errorMessage = "Incorrect code. Try again.",
                    progressDescription = { "${it.entered} of ${it.total} digits entered" },
                ),
                doneButtonTitle = "Done",
            ).also { it.hapticFeedbackOnError = false }
        val withoutError = fittedHeight(field)

        field.state =
            field.state
                .withInput("0000")
                .state
                .withError()
        val withError = fittedHeight(field)

        val errorRow = withError - withoutError
        assertTrue(errorRow > 0.0, "error row is laid out, got $errorRow")
        assertTrue(errorRow < 100.0, "error row fits on about one line at 360pt, got $errorRow")
    }

    private fun fittedHeight(field: OtpField): Double {
        val size = field.view.sizeThatFits(CGSizeMake(360.0, 10_000.0))
        return size.useContents { this.height }
    }
}
