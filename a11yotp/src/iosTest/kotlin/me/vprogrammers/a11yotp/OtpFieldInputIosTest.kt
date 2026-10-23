package me.vprogrammers.a11yotp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.accessibilityValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun accessibility() =
    OtpFieldAccessibility(
        "Verification code",
        "Incorrect code",
        { "${it.entered} of ${it.total} digits entered" },
        { "${it.receivedDigitCount} digits" },
    )

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
    fun overlongPasteIsRejectedAndNeverTruncated() {
        val field = field(OtpLength.FOUR)
        val completed = mutableListOf<String>()
        val rejections = mutableListOf<OtpInputRejection>()
        field.onComplete = { completed += it }
        field.onInputRejected = { rejections += it }

        field.handleReplacement("12-34-56")

        assertEquals("", field.state.code)
        assertTrue(completed.isEmpty())
        assertEquals(listOf(OtpInputRejection(4, 6)), rejections)
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
        "12".forEach { field.handleReplacement(it.toString()) }
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

class OtpFieldRejectionIosTest {
    private fun messageOf(field: OtpField): String? {
        val container = field.view as OtpFieldContainer
        return container.subviews
            .filterIsInstance<UILabel>()
            .singleOrNull { !it.hidden }
            ?.text
    }

    private fun rejectionCase(
        length: OtpLength,
        typed: String,
        pasted: String,
        expected: Int,
        received: Int,
    ) {
        val field = field(length)
        typed.forEach { field.handleReplacement(it.toString()) }
        val before = field.state
        val completed = mutableListOf<String>()
        val changes = mutableListOf<OtpState>()
        val rejections = mutableListOf<OtpInputRejection>()
        field.onComplete = { completed += it }
        field.onStateChange = { changes += it }
        field.onInputRejected = { rejections += it }

        field.handleReplacement(pasted)

        assertEquals(before, field.state)
        assertEquals(before.code, field.textField().text)
        assertTrue(completed.isEmpty())
        assertTrue(changes.isEmpty())
        assertEquals(listOf(OtpInputRejection(expected, received)), rejections)
        assertEquals("$received digits", messageOf(field))
    }

    @Test
    fun partialAndOverlongPastesAreRejectedFromEmptyPartialAndCompleteFields() {
        rejectionCase(OtpLength.SIX, "", "123", 6, 3)
        rejectionCase(OtpLength.SIX, "", "12345678", 6, 8)
        rejectionCase(OtpLength.SIX, "12", "34", 6, 2)
        rejectionCase(OtpLength.EIGHT, "123", "456789", 8, 6)
        rejectionCase(OtpLength.FOUR, "1234", "123456", 4, 6)
        rejectionCase(OtpLength.EIGHT, "12345678", "12", 8, 2)
    }

    @Test
    fun exactLengthPasteIsAcceptedAndShowsNoMessage() {
        val field = field()
        val rejections = mutableListOf<OtpInputRejection>()
        field.onInputRejected = { rejections += it }

        field.handleReplacement("482913")

        assertEquals("482913", field.state.code)
        assertTrue(rejections.isEmpty())
        assertNull(messageOf(field))
    }

    @Test
    fun theMessageIsNotAnErrorButIsAppendedToTheValueUntilCleared() {
        val field = field()

        field.handleReplacement("123")

        assertFalse(field.state.isError)
        assertEquals("0 of 6 digits entered, 3 digits", field.textField().accessibilityValue)

        field.handleReplacement("4")

        assertEquals("4, 1 of 6 digits entered", field.textField().accessibilityValue)
    }

    @Test
    fun theValueNeverContainsDigitsFromARejectedPasteWhenMasked() {
        val field = field()
        field.mask = OtpMask.FilledCell
        field.handleReplacement("1")

        field.handleReplacement("9876")

        assertEquals("1 of 6 digits entered, 4 digits", field.textField().accessibilityValue)
        assertFalse(field.textField().accessibilityValue!!.contains("9"))
    }

    @Test
    fun aBlankHostMessageIsNeitherShownNorInTheValueButTheCallbackFires() {
        val blank = OtpFieldAccessibility("Verification code", "Incorrect code", { "p" }, { "  " })
        val field = OtpField(OtpState(OtpLength.SIX), blank, doneButtonTitle = "Done")
        val rejections = mutableListOf<OtpInputRejection>()
        field.onInputRejected = { rejections += it }

        field.handleReplacement("123")

        assertEquals(1, rejections.size)
        assertNull(messageOf(field))
        assertEquals("p", field.textField().accessibilityValue)
    }

    @Test
    fun theNextAcceptedEditClearsTheMessage() {
        val field = field()
        field.handleReplacement("123")
        assertEquals("3 digits", messageOf(field))

        field.handleReplacement("4")

        assertNull(messageOf(field))
        assertEquals("4", field.state.code)
    }

    @Test
    fun deletionClearsTheMessage() {
        val field = field()
        field.handleReplacement("1")
        field.handleReplacement("123")
        assertEquals("3 digits", messageOf(field))

        field.handleReplacement("")

        assertNull(messageOf(field))
    }

    @Test
    fun anUnsupportedCharacterClearsTheMessage() {
        val field = field()
        field.handleReplacement("123")

        field.handleReplacement("x")

        assertNull(messageOf(field))
        assertEquals("0 of 6 digits entered", field.textField().accessibilityValue)
    }

    @Test
    fun anIgnoredExtraDigitOnAFullFieldClearsTheMessage() {
        val field = field(OtpLength.FOUR)
        field.handleReplacement("1234")
        field.handleReplacement("12")
        assertEquals("2 digits", messageOf(field))

        field.handleReplacement("5")

        assertEquals("1234", field.state.code)
        assertNull(messageOf(field))
        assertFalse(field.textField().accessibilityValue!!.contains("2 digits"))
    }

    @Test
    fun aDifferentHostStateClearsTheMessage() {
        val field = field()
        field.handleReplacement("123")

        field.state = OtpState(OtpLength.SIX).typed("9")

        assertNull(messageOf(field))
    }

    @Test
    fun theNewestMessageReplacesTheErrorTextAndReturnsWhenCleared() {
        val field = field()
        field.state = field.state.withError()
        assertEquals("Incorrect code", messageOf(field))

        field.handleReplacement("123")

        assertTrue(field.state.isError)
        assertEquals("3 digits", messageOf(field))
        assertEquals("0 of 6 digits entered, 3 digits", field.textField().accessibilityValue)

        field.handleReplacement("x")

        assertEquals("Incorrect code", messageOf(field))
        assertEquals("0 of 6 digits entered, Incorrect code", field.textField().accessibilityValue)
    }

    @Test
    fun aRepeatedRejectionIsReportedEachTime() {
        val field = field()
        var count = 0
        field.onInputRejected = { count++ }

        field.handleReplacement("123")
        field.handleReplacement("123")

        assertEquals(2, count)
    }

    @Test
    fun theRejectionMessageAddsAWrappingRowBelowTheCells() {
        val field = field()
        val without = fitted(field)

        field.handleReplacement("123")

        val row = fitted(field) - without
        assertTrue(row > 0.0, "message row is laid out, got $row")
        assertTrue(row < 100.0, "message fits on a few lines at 360pt, got $row")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun fitted(field: OtpField): Double =
        field.view.sizeThatFits(CGSizeMake(360.0, 10_000.0)).useContents { this.height }
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
            OtpFieldAccessibility("Code", "Wrong", {
                seen += it
                "p"
            }, { "r" })

        otpAccessibilityValue(OtpState(OtpLength.EIGHT).typed("123"), custom, masked = false)

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
