package me.vprogrammers.a11yotp

import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityValue
import platform.UIKit.isAccessibilityElement
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun fieldAccessibility() =
    OtpFieldAccessibility(
        label = "Verification code",
        errorMessage = "Incorrect code",
        progressDescription = { "${it.entered} of ${it.total} digits entered" },
        inputRejectedMessage = { "${it.receivedDigitCount} digits" },
    )

private fun newField(length: OtpLength = OtpLength.SIX): OtpField =
    OtpField(OtpState(length), fieldAccessibility(), doneButtonTitle = "Done").also {
        it.hapticFeedbackOnError = false
    }

class OtpFieldIosTest {
    @Test
    fun valueSpellsDigitsAndProgressWhenUnmasked() {
        val state = OtpState(OtpLength.SIX).typed("123")

        assertEquals("1 2 3, 3 of 6 digits entered", otpAccessibilityValue(state, fieldAccessibility(), masked = false))
    }

    @Test
    fun valueNeverContainsDigitsWhenMasked() {
        val state = OtpState(OtpLength.SIX).typed("123")

        assertEquals("3 of 6 digits entered", otpAccessibilityValue(state, fieldAccessibility(), masked = true))
    }

    @Test
    fun valueIncludesErrorMessageOnlyInErrorState() {
        val state = OtpState(OtpLength.FOUR).withInput("1234").state

        assertEquals(
            "1 2 3 4, 4 of 4 digits entered, Incorrect code",
            otpAccessibilityValue(state.withError(), fieldAccessibility(), masked = false),
        )
        assertEquals(
            "4 of 4 digits entered, Incorrect code",
            otpAccessibilityValue(state.withError(), fieldAccessibility(), masked = true),
        )
        assertFalse("Incorrect" in otpAccessibilityValue(state, fieldAccessibility(), masked = false))
    }

    @Test
    fun theTextFieldIsTheOnlyLabelledElementAndShowsLabelAndValue() {
        val field = newField()
        val textField = field.view.let { (it as OtpFieldContainer).textField }

        assertEquals("Verification code", textField.accessibilityLabel)
        assertEquals("0 of 6 digits entered", textField.accessibilityValue)
        assertFalse(field.view.isAccessibilityElement())
        assertTrue(textField.isAccessibilityElement())
    }

    @Test
    fun typedDigitsReportStateChangeAndCompleteExactlyOnce() {
        val field = newField(OtpLength.FOUR)
        val changes = mutableListOf<OtpState>()
        val completed = mutableListOf<String>()
        field.onStateChange = { changes += it }
        field.onComplete = { completed += it }

        "1234".forEach { field.handleReplacement(it.toString()) }

        assertEquals(listOf("1", "12", "123", "1234"), changes.map { it.code })
        assertEquals(listOf("1234"), completed)
        assertEquals("1234", field.state.code)
        assertEquals("1234", textOf(field))
    }

    @Test
    fun extraDigitsOnACompleteCodeDoNotCompleteAgain() {
        val field = newField(OtpLength.FOUR)
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }

        "12345".forEach { field.handleReplacement(it.toString()) }

        assertEquals(listOf("1234"), completed)
        assertEquals("1234", field.state.code)
    }

    @Test
    fun rejectedInputKeepsStateWritesCodeBackAndFiresNothing() {
        val field = newField()
        var changes = 0
        var completions = 0
        field.onStateChange = { changes++ }
        field.onComplete = { completions++ }
        field.handleReplacement("1")
        changes = 0

        field.handleReplacement("x")

        assertEquals("1", field.state.code)
        assertEquals("1", textOf(field))
        assertEquals(0, changes)
        assertEquals(0, completions)
    }

    @Test
    fun deletionRemovesTheLastDigit() {
        val field = newField()
        field.handleReplacement("123")

        field.handleReplacement("")

        assertEquals("12", field.state.code)
        assertEquals("2 of 6 digits entered", valueSuffix(field))
    }

    @Test
    fun pastedCompleteCodeReplacesPartialInputAndCompletes() {
        val field = newField()
        val completed = mutableListOf<String>()
        field.onComplete = { completed += it }
        field.handleReplacement("12")

        field.handleReplacement("482 913")

        assertEquals("482913", field.state.code)
        assertEquals(listOf("482913"), completed)
    }

    @Test
    fun partialPasteIsAppended() {
        val field = newField()
        field.handleReplacement("12")

        field.handleReplacement("34")

        assertEquals("1234", field.state.code)
    }

    @Test
    fun hostStateIsRenderedWithoutCallbacks() {
        val field = newField()
        var changes = 0
        field.onStateChange = { changes++ }

        field.state = OtpState(OtpLength.SIX).typed("9").withError()

        assertEquals("9", textOf(field))
        assertContains(assertNotNull(valueOf(field)), "Incorrect code")
        assertEquals(0, changes)
    }

    @Test
    fun maskedFieldIsSecureAndValueHasNoDigits() {
        val field = newField()
        field.mask = OtpMask.FilledCell
        field.handleReplacement("123")

        assertTrue(textFieldOf(field).secureTextEntry)
        assertEquals("3 of 6 digits entered", valueOf(field))

        field.mask = OtpMask.None
        assertFalse(textFieldOf(field).secureTextEntry)
        assertEquals("1 2 3, 3 of 6 digits entered", valueOf(field))
    }

    @Test
    fun disablingTheFieldDisablesTheTextField() {
        val field = newField()

        field.isEnabled = false

        assertFalse(textFieldOf(field).enabled)
    }

    @Test
    fun cellBordersAreThickerForFocusAndErrorAndNeverForDisabled() {
        val style = OtpFieldDefaults.style()

        assertEquals(style.borderWidth, otpCellBorderWidth(style, isEnabled = true, isError = false, isActive = false))
        assertEquals(
            style.focusedBorderWidth,
            otpCellBorderWidth(style, isEnabled = true, isError = true, isActive = false),
        )
        assertEquals(
            style.focusedBorderWidth,
            otpCellBorderWidth(style, isEnabled = true, isError = false, isActive = true),
        )
        assertEquals(style.borderWidth, otpCellBorderWidth(style, isEnabled = false, isError = true, isActive = true))
    }

    private fun textFieldOf(field: OtpField) = (field.view as OtpFieldContainer).textField

    private fun textOf(field: OtpField): String? = textFieldOf(field).text

    private fun valueOf(field: OtpField): String? = textFieldOf(field).accessibilityValue

    private fun valueSuffix(field: OtpField): String = assertNotNull(valueOf(field)).substringAfter(", ")
}

class OtpStyleIosTest {
    @Test
    fun sizesBelowTheTouchTargetAreRaised() {
        val style = OtpFieldDefaults.style(minCellWidth = 10.0, minCellHeight = 20.0)

        assertEquals(44.0, style.minCellWidth)
        assertEquals(44.0, style.minCellHeight)
        assertEquals(44.0, OtpButtonDefaults.style(minHeight = 10.0).minHeight)
    }

    @Test
    fun defaultsAreAtLeastTheAndroidTargetSize() {
        assertTrue(OtpFieldDefaults.style().minCellWidth >= 48.0)
        assertTrue(OtpButtonDefaults.style().minHeight >= 48.0)
    }

    @Test
    fun focusedBorderMustBeThickerThanTheBorder() {
        assertFailsWith<IllegalArgumentException> {
            OtpFieldDefaults.style(borderWidth = 2.0, focusedBorderWidth = 2.0)
        }
    }

    @Test
    fun negativeGroupSizeIsRejectedAndEmptyPlaceholderMeansNone() {
        assertFailsWith<IllegalArgumentException> { OtpFieldDefaults.style(groupSize = -1) }
        assertNull(OtpFieldDefaults.style(placeholder = "").placeholder)
        assertEquals("•", OtpFieldDefaults.style(placeholder = "•").placeholder)
    }

    @Test
    fun blankDoneTitleIsRejected() {
        assertFailsWith<IllegalArgumentException> { OtpField(OtpState(OtpLength.SIX), fieldAccessibility(), " ") }
    }
}
