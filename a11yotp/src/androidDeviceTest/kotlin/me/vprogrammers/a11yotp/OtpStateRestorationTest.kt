package me.vprogrammers.a11yotp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A partly entered code survives the saved-instance-state round trip of rotation and process death. */
class OtpStateRestorationTest {
    @get:Rule
    val rule = createComposeRule()

    private val accessibility =
        OtpFieldAccessibility(
            label = "Code",
            errorMessage = "Wrong code",
            progressDescription = { "${it.entered} of ${it.total} digits entered" },
            inputRejectedMessage = { "Got ${it.receivedDigitCount} digits" },
        )

    @Test
    fun aPartlyTypedCodeAndItsErrorSurviveRecreationWithoutCompletingAgain() {
        val tester = StateRestorationTester(rule)
        var current: OtpState? = null
        val completions = mutableListOf<String>()
        tester.setContent {
            var otp by rememberSaveable(stateSaver = OtpStateSaver) { mutableStateOf(OtpState(OtpLength.FOUR)) }
            current = otp
            OtpField(otp, { otp = it }, { completions += it }, accessibility)
        }
        val field = rule.onNode(hasSetTextAction())
        "123".forEach { field.performTextInput(it.toString()) }
        rule.runOnIdle { assertEquals("123", current?.code) }

        tester.emulateSavedInstanceStateRestore()

        rule.runOnIdle { assertEquals("123", current?.code) }
        field.performTextInput("4")
        rule.runOnIdle {
            assertEquals("1234", current?.code)
            assertEquals(listOf("1234"), completions)
        }
    }

    @Test
    fun anErrorMarkSetAfterVerificationSurvivesRecreation() {
        val tester = StateRestorationTester(rule)
        var current: OtpState? = null
        var update: ((OtpState) -> Unit)? = null
        tester.setContent {
            var otp by rememberSaveable(stateSaver = OtpStateSaver) { mutableStateOf(OtpState(OtpLength.FOUR)) }
            current = otp
            update = { otp = it }
            OtpField(otp, { otp = it }, {}, accessibility)
        }
        val field = rule.onNode(hasSetTextAction())
        "1234".forEach { field.performTextInput(it.toString()) }
        rule.runOnIdle { update?.invoke(current!!.withError()) }

        tester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertEquals("1234", current?.code)
            assertTrue(current?.isError == true)
        }
    }
}
