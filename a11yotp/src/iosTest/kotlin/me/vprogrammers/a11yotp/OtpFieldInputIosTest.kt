package me.vprogrammers.a11yotp

import platform.UIKit.UIColor
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.accessibilityValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun accessibility() =
    OtpFieldAccessibility("Verification code", "Incorrect code") { "${it.entered} of ${it.total} digits entered" }

private fun field(length: OtpLength = OtpLength.SIX) =
    OtpField(OtpState(length), accessibility(), doneButtonTitle = "Done").also { it.hapticFeedbackOnError = false }

private fun OtpField.textField() = (view as OtpFieldContainer).textField

class OtpFieldInputIosTest {
    @Test
    fun pasteWithSeparatorsIsNormalizedAndCompletesOnce() {
        val field = field()
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }

        field.handleReplacement(" 482-913 ")

        assertEquals("482913", field.state.code)
        assertEquals(listOf("482913"), completed)
        assertEquals("482913", field.textField().text)
    }

    @Test
    fun pasteWithArabicIndicDigitsIsConverted() {
        val field = field()

        field.handleReplacement("\u0664\u0668\u0662\u0669\u0661\u0663")

        assertEquals("482913", field.state.code)
    }

    @Test
    fun overlongPasteKeepsTheFirstDigits() {
        val field = field(OtpLength.FOUR)
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }

        field.handleReplacement("12-34-56")

        assertEquals("1234", field.state.code)
        assertEquals(listOf("1234"), completed)
    }

    @Test
    fun eightDigitFieldCompletesOnlyOnTheEighthDigit() {
        val field = field(OtpLength.EIGHT)
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }

        "1234567".forEach { field.handleReplacement(it.toString()) }
        assertTrue(completed.isEmpty())
        field.handleReplacement("8")

        assertEquals(listOf("12345678"), completed)
    }

    @Test
    fun rejectedPasteWritesTheCommittedCodeBackAndKeepsError() {
        val field = field()
        field.handleReplacement("12")
        field.state = field.state.withError()
        var changes = 0
        field.onStateChange = { changes++ }

        field.handleReplacement("12a456")

        assertEquals("12", field.state.code)
        assertTrue(field.state.isError)
        assertEquals("12", field.textField().text)
        assertEquals(0, changes)
    }

    @Test
    fun noOpInputDoesNotReportAStateChange() {
        val field = field()
        var changes = 0
        field.onStateChange = { changes++ }

        field.handleReplacement("")
        field.handleReplacement(" ")
        field.handleReplacement("-")

        assertEquals(0, changes)
        assertEquals("", field.state.code)
    }

    @Test
    fun typingClearsTheErrorAndReportsTheChange() {
        val field = field(OtpLength.FOUR)
        field.handleReplacement("123")
        field.state = field.state.withError()
        val changes = mutableListOf<OtpState>()
        field.onStateChange = { changes += it }

        field.handleReplacement("4")

        assertEquals(1, changes.size)
        assertFalse(changes.single().isError)
        assertFalse(field.state.isError)
    }

    @Test
    fun deletingOnAnEmptyFieldIsANoOp() {
        val field = field()
        var changes = 0
        field.onStateChange = { changes++ }

        field.handleReplacement("")

        assertEquals(0, changes)
        assertEquals("0 of 6 digits entered", field.textField().accessibilityValue)
    }

    @Test
    fun autofillReplacesPartiallyTypedInputWithoutKeepingOldDigits() {
        val field = field()
        field.handleReplacement("4")
        field.handleReplacement("8")
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }

        field.handleReplacement("739201")

        assertEquals("739201", field.state.code)
        assertEquals(listOf("739201"), completed)
    }

    @Test
    fun autofillOverACompleteCodeReplacesItAndReportsTheChange() {
        val field = field(OtpLength.FOUR)
        field.handleReplacement("1234")
        val changes = mutableListOf<String>()
        field.onStateChange = { changes += it.code }
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }

        field.handleReplacement("5678")

        assertEquals("5678", field.state.code)
        assertEquals(listOf("5678"), changes)
        assertEquals(listOf("5678"), completed)
    }

    @Test
    fun completionFiresAgainAfterDeletingAndRetyping() {
        val field = field(OtpLength.FOUR)
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }
        field.handleReplacement("1234")

        field.handleReplacement("")
        field.handleReplacement("9")

        assertEquals(listOf("1234", "1239"), completed)
    }

    @Test
    fun maskedFieldNeverPutsDigitsInTheAccessibilityValueAfterPaste() {
        val field = field()
        field.mask = OtpMask.Character('*')

        field.handleReplacement("482913")

        val value = field.textField().accessibilityValue.orEmpty()
        assertEquals("6 of 6 digits entered", value)
        assertFalse(value.contains("4"))
    }
}

class OtpAccessibilityValueIosTest {
    @Test
    fun emptyUnmaskedValueIsOnlyTheProgress() {
        assertEquals("0 of 4 digits entered", otpAccessibilityValue(OtpState(OtpLength.FOUR), accessibility(), false))
    }

    @Test
    fun eightDigitsAreSpelledSeparately() {
        val state = OtpState(OtpLength.EIGHT).withInput("12345678").state

        assertEquals(
            "1 2 3 4 5 6 7 8, 8 of 8 digits entered",
            otpAccessibilityValue(state, accessibility(), masked = false),
        )
    }

    @Test
    fun errorOnEmptyFieldStillAppendsTheMessage() {
        val state = OtpState(OtpLength.SIX).withError()

        assertEquals(
            "0 of 6 digits entered, Incorrect code",
            otpAccessibilityValue(state, accessibility(), masked = true),
        )
    }

    @Test
    fun progressCallbackReceivesEnteredAndTotal() {
        val seen = mutableListOf<OtpProgress>()
        val custom =
            OtpFieldAccessibility("Code", "Wrong") {
                seen += it
                "p"
            }

        otpAccessibilityValue(OtpState(OtpLength.EIGHT).withInput("123").state, custom, masked = false)

        assertEquals(listOf(OtpProgress(3, 8)), seen)
    }
}

class OtpButtonRulesIosTest {
    private val resend = OtpResendAccessibility("Resend code") { "Resend code in ${it.secondsRemaining} s" }

    @Test
    fun negativeCountdownBehavesLikeZero() {
        val button = OtpResendButton(resend)

        button.secondsRemaining = -3

        assertEquals("Resend code", button.text)
        assertTrue(button.isActionEnabled)
    }

    @Test
    fun oneSecondRemainingStillDisablesResend() {
        val button = OtpResendButton(resend)

        button.secondsRemaining = 1

        assertEquals("Resend code in 1 s", button.text)
        assertFalse(button.isActionEnabled)
    }

    @Test
    fun verifyStaysEnabledForACompleteCodeMarkedAsError() {
        val button = OtpVerifyButton(OtpVerifyAccessibility("Verify"))

        button.state = OtpState(OtpLength.EIGHT).withInput("12345678").state.withError()

        assertTrue(button.isActionEnabled)
    }

    @Test
    fun verifyIsDisabledForAnEmptyCodeAndAfterClearing() {
        val button = OtpVerifyButton(OtpVerifyAccessibility("Verify"))
        assertFalse(button.isActionEnabled)

        button.state = OtpState(OtpLength.FOUR).withInput("1234").state
        assertTrue(button.isActionEnabled)
        button.state = button.state.cleared()

        assertFalse(button.isActionEnabled)
    }
}

class OtpOpaqueRgbIosTest {
    private val light =
        UITraitCollection.traitCollectionWithUserInterfaceStyle(
            UIUserInterfaceStyle.UIUserInterfaceStyleLight,
        )

    @Test
    fun opaqueColorsConvertToRgb() {
        assertEquals(0xFFFFFF, UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 1.0).opaqueRgb(light))
        assertEquals(0x000000, UIColor(red = 0.0, green = 0.0, blue = 0.0, alpha = 1.0).opaqueRgb(light))
        assertEquals(0xFF0080, UIColor(red = 1.0, green = 0.0, blue = 128.0 / 255.0, alpha = 1.0).opaqueRgb(light))
    }

    @Test
    fun translucentColorsHaveNoRgb() {
        assertNull(UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 0.5).opaqueRgb(light))
    }

    @Test
    fun contrastWarningIsSkippedForTranslucentColors() {
        val clear = UIColor(red = 0.0, green = 0.0, blue = 0.0, alpha = 0.1)
        val white = UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 1.0)

        assertNull(contrastWarning("C", "fg", clear, "bg", white, OtpContrastRequirement.TEXT, light))
    }

    @Test
    fun contrastWarningNamesTheFailingPair() {
        val gray = UIColor(red = 0x77 / 255.0, green = 0x77 / 255.0, blue = 0x77 / 255.0, alpha = 1.0)
        val white = UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 1.0)

        val warning = contrastWarning("C", "fg", gray, "bg", white, OtpContrastRequirement.TEXT, light)

        assertTrue(warning!!.contains("fg (#777777) on bg (#FFFFFF)"), warning)
    }
}
