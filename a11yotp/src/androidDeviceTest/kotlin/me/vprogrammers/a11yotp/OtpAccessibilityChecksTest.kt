package me.vprogrammers.a11yotp

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Runs the Accessibility Test Framework (contrast, touch target size, labels, duplicate
 * descriptions, and more) over each visible state of the field and the buttons, in the light and dark
 * theme and at 200% font scale. Any ATF error fails the test (warnings do not).
 */
class OtpAccessibilityChecksTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val fieldAccessibility =
        OtpFieldAccessibility(
            label = "Verification code",
            errorMessage = "Wrong code",
            progressDescription = { "${it.entered} of ${it.total} digits entered" },
            inputRejectedMessage = { "Got ${it.receivedDigitCount} digits, expected ${it.expectedDigitCount}" },
        )

    @Before
    fun enableChecks() {
        rule.enableAccessibilityChecks()
    }

    private fun check(
        dark: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        rule.setContent {
            val configuration =
                Configuration(LocalConfiguration.current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            val density = Density(LocalDensity.current.density, fontScale)
            CompositionLocalProvider(LocalConfiguration provides configuration, LocalDensity provides density) {
                Column { content() }
            }
        }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    private fun allStates(
        dark: Boolean = false,
        fontScale: Float = 1f,
    ) = check(dark, fontScale) {
        val resend = OtpResendAccessibility("Resend code") { "Resend code in ${it.secondsRemaining} seconds" }
        OtpField(OtpState(OtpLength.SIX).typed("123"), {}, {}, fieldAccessibility)
        OtpField(OtpState(OtpLength.SIX).typed("123456").withError(), {}, {}, fieldAccessibility)
        OtpField(OtpState(OtpLength.SIX).typed("12"), {}, {}, fieldAccessibility, enabled = false)
        OtpVerifyButton(OtpState(OtpLength.FOUR).typed("1234"), {}, OtpVerifyAccessibility("Verify"))
        OtpVerifyButton(OtpState(OtpLength.FOUR).typed("12"), {}, OtpVerifyAccessibility("Verify later"))
        OtpResendButton(secondsRemaining = 20, onResend = {}, accessibility = resend)
    }

    private fun field(
        state: OtpState,
        enabled: Boolean = true,
        mask: OtpMask = OtpMask.None,
    ) {
        check {
            OtpField(
                state = state,
                onStateChange = {},
                onComplete = {},
                accessibility = fieldAccessibility,
                enabled = enabled,
                mask = mask,
            )
        }
    }

    @Test
    fun emptyField() = field(OtpState(OtpLength.SIX))

    @Test
    fun partialField() = field(OtpState(OtpLength.SIX).typed("123"))

    @Test
    fun completeField() = field(OtpState(OtpLength.SIX).typed("123456"))

    @Test
    fun fieldInError() = field(OtpState(OtpLength.SIX).typed("123456").withError())

    @Test
    fun maskedField() = field(OtpState(OtpLength.SIX).typed("123"), mask = OtpMask.FilledCell)

    @Test
    fun disabledField() = field(OtpState(OtpLength.SIX).typed("12"), enabled = false)

    @Test
    fun everyStateInTheDarkTheme() = allStates(dark = true)

    @Test
    fun everyStateAtTwiceTheFontScale() = allStates(fontScale = 2f)

    @Test
    fun everyStateDarkAtTwiceTheFontScale() = allStates(dark = true, fontScale = 2f)

    @Test
    fun focusedFieldShowingARejection() {
        var state by mutableStateOf(OtpState(OtpLength.SIX))
        rule.setContent {
            OtpField(state, { state = it }, {}, fieldAccessibility)
        }
        val field = rule.onNode(hasSetTextAction())
        field.requestFocus()
        // A partial paste is rejected and shows the rejection message.
        field.performTextInput("123")
        rule.onNode(hasText("Got 3 digits, expected 6")).assertExists()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun verifyButtonEnabledAndDisabled() =
        check {
            OtpVerifyButton(OtpState(OtpLength.FOUR).typed("1234"), {}, OtpVerifyAccessibility("Verify"))
            OtpVerifyButton(OtpState(OtpLength.FOUR).typed("12"), {}, OtpVerifyAccessibility("Verify later"))
        }

    @Test
    fun resendButtonCountingDownAndReady() =
        check {
            val resend = OtpResendAccessibility("Resend code") { "Resend code in ${it.secondsRemaining} seconds" }
            OtpResendButton(secondsRemaining = 20, onResend = {}, accessibility = resend)
            OtpResendButton(secondsRemaining = 0, onResend = {}, accessibility = resend)
        }
}
