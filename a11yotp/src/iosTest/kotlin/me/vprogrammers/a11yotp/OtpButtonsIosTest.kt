package me.vprogrammers.a11yotp

import platform.UIKit.UIControl
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.accessibilityLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun resendAccessibility() =
    OtpResendAccessibility(
        label = "Resend code",
        countdownDescription = { "Resend code in ${it.secondsRemaining} s" },
    )

class OtpButtonsIosTest {
    @Test
    fun resendIsDisabledWhileCountingDownAndNamedByTheVisibleText() {
        val button = OtpResendButton(resendAccessibility())

        button.secondsRemaining = 25

        assertEquals("Resend code in 25 s", button.text)
        assertFalse(button.isActionEnabled)
        val control = button.control()
        assertEquals("Resend code in 25 s", control.accessibilityLabel)
        assertFalse(control.enabled)
        assertTrue(button.text.startsWith("Resend code"))
    }

    @Test
    fun resendIsEnabledWithItsPlainLabelAtZero() {
        val button = OtpResendButton(resendAccessibility())
        button.secondsRemaining = 3

        button.secondsRemaining = 0

        assertEquals("Resend code", button.text)
        assertTrue(button.isActionEnabled)
        assertTrue(button.control().enabled)
    }

    @Test
    fun resendTapInvokesTheCallback() {
        val button = OtpResendButton(resendAccessibility())
        var taps = 0
        button.onResend = { taps++ }

        button.control().tap()

        assertEquals(1, taps)
    }

    @Test
    fun verifyIsEnabledOnlyForACompleteCodeWhileEnabled() {
        val button = OtpVerifyButton(OtpVerifyAccessibility("Verify"))
        val partial = OtpState(OtpLength.SIX).withInput("123").state
        val complete = OtpState(OtpLength.SIX).withInput("123456").state

        button.state = partial
        assertFalse(button.isActionEnabled)

        button.state = complete
        assertTrue(button.isActionEnabled)
        assertTrue(button.control().enabled)

        button.isEnabled = false
        assertFalse(button.isActionEnabled)
        assertFalse(button.control().enabled)
    }

    @Test
    fun verifyTapReportsTheCode() {
        val button = OtpVerifyButton(OtpVerifyAccessibility("Verify"))
        button.state = OtpState(OtpLength.FOUR).withInput("4821").state
        val codes = mutableListOf<String>()
        button.onVerify = { codes += it }

        button.control().tap()

        assertEquals(listOf("4821"), codes)
        assertEquals("Verify", button.control().accessibilityLabel)
    }

    @Test
    fun disabledButtonsKeepABorder() {
        val style = OtpButtonDefaults.style(borderWidth = 0.0)

        assertEquals(0.0, otpButtonBorderWidth(style, isEnabled = true))
        assertEquals(1.0, otpButtonBorderWidth(style, isEnabled = false))
    }
}

private fun OtpResendButton.control() = (view as OtpButtonContainer).control

private fun OtpVerifyButton.control() = (view as OtpButtonContainer).control

/**
 * Invokes the touch-up-inside actions registered on the control. `sendActionsForControlEvents`
 * dispatches through UIApplication, which does not exist in the Kotlin/Native test binary.
 * `performSelector` is not used: it reads an object result from a void action and crashes.
 */
private fun UIControl.tap() {
    var invoked = false
    for (target in allTargets) {
        val handler = target as? OtpTapHandler ?: continue
        if ("tapped" in actionsForTarget(handler, forControlEvent = UIControlEventTouchUpInside).orEmpty()) {
            handler.tapped()
            invoked = true
        }
    }
    check(invoked) { "No touch-up-inside action registered" }
}
