@file:OptIn(ExperimentalForeignApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSArray
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIKeyCommand
import platform.UIKit.UIKeyInputEscape
import platform.UIKit.UIKeyboardTypeASCIICapable
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentTypeOneTimeCode
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
        "123".forEach { field.handleReplacement(it.toString()) }

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
    fun partialPasteIsRejectedNotAppended() {
        val field = newField()
        "12".forEach { field.handleReplacement(it.toString()) }

        field.handleReplacement("34")

        assertEquals("12", field.state.code)
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
        "123".forEach { field.handleReplacement(it.toString()) }

        assertTrue(textFieldOf(field).secureTextEntry)
        assertEquals("3 of 6 digits entered", valueOf(field))

        field.mask = OtpMask.None
        assertFalse(textFieldOf(field).secureTextEntry)
        assertEquals("1 2 3, 3 of 6 digits entered", valueOf(field))
    }

    @Test
    fun numericFieldUsesNumberPadWithDoneAccessory() {
        val textField = textFieldOf(newField())

        assertEquals(UIKeyboardTypeNumberPad, textField.keyboardType)
        assertEquals(UITextAutocapitalizationType.UITextAutocapitalizationTypeNone, textField.autocapitalizationType)
        assertNotNull(textField.inputAccessoryView)
    }

    @Test
    fun alphanumericFieldUsesAsciiKeyboardWithAllCapsAndNoAccessory() {
        val field = OtpField(OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC), fieldAccessibility(), "Done")
        val textField = textFieldOf(field)

        assertEquals(UIKeyboardTypeASCIICapable, textField.keyboardType)
        assertEquals(
            UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters,
            textField.autocapitalizationType,
        )
        assertEquals(UIReturnKeyType.UIReturnKeyDone, textField.returnKeyType)
        assertNull(textField.inputAccessoryView)
        assertEquals(UITextAutocorrectionType.UITextAutocorrectionTypeNo, textField.autocorrectionType)
        assertEquals(UITextContentTypeOneTimeCode, textField.textContentType)
    }

    @Test
    fun changingTheCharsetAtRuntimeReappliesTheKeyboardAndKeepsTheAccessory() {
        val field = newField()
        val textField = textFieldOf(field)
        val accessory = textField.inputAccessoryView

        field.state = OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC)
        assertEquals(UIKeyboardTypeASCIICapable, textField.keyboardType)
        assertNull(textField.inputAccessoryView)

        field.state = OtpState(OtpLength.SIX, OtpCharset.NUMERIC)
        assertEquals(UIKeyboardTypeNumberPad, textField.keyboardType)
        assertTrue(textField.inputAccessoryView === accessory)
    }

    @Test
    fun alphanumericValueSpellsLettersAndMaskedValueHasProgressOnly() {
        val field = OtpField(OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC), fieldAccessibility(), "Done")
        "a7b".forEach { field.handleReplacement(it.toString()) }

        assertEquals("A 7 B, 3 of 6 digits entered", valueOf(field))

        field.mask = OtpMask.FilledCell
        assertEquals("3 of 6 digits entered", valueOf(field))
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

    private fun accessory(field: OtpField): OtpDoneAccessory {
        val view = (field.view as OtpFieldContainer).textField.inputAccessoryView
        assertTrue(view is OtpDoneAccessory, "inputAccessoryView was $view")
        return view
    }

    @Test
    fun keyboardAccessoryHasOneDoneButtonWithTheTitle() {
        val accessory = accessory(newField())

        assertEquals(listOf(accessory.button), accessory.subviews)
        assertEquals("Done", accessory.title)
        assertEquals("Done", accessory.button.accessibilityLabel)
        assertEquals(
            9.0,
            accessory.button.configuration
                ?.background
                ?.cornerRadius,
        )
    }

    @Test
    fun changingTheDoneTitleUpdatesTheButton() {
        val field = newField()

        field.doneButtonTitle = "Fertig"

        assertEquals("Fertig", accessory(field).title)
        assertEquals("Fertig", accessory(field).button.accessibilityLabel)
    }

    @Test
    fun doneButtonSitsAboveTheThreeKey() {
        val accessory = accessory(newField())
        accessory.setFrame(CGRectMake(0.0, 0.0, 390.0, 60.0))
        accessory.layoutIfNeeded()

        accessory.button.frame.useContents {
            assertEquals(262.0, origin.x)
            assertEquals(6.0, origin.y)
            assertEquals(122.0, size.width)
            assertEquals(48.0, size.height)
        }
    }

    @Test
    fun doneKeyFrameKeepsPortraitAndRespectsTheSafeArea() {
        assertEquals(OtpKeyFrame(262.0, 6.0, 122.0, 48.0), doneKeyFrame(390.0, 0.0, 0.0, false, 48.0))
        // Landscape iPhone with a notch: 852 pt wide, 62 pt inset left and right.
        val landscape = doneKeyFrame(852.0, 62.0, 62.0, false, 48.0)
        assertEquals((852.0 - 124.0 - 24.0) / 3.0, landscape.width)
        assertEquals(852.0 - 62.0 - 6.0 - landscape.width, landscape.x)
    }

    @Test
    fun doneKeyOnIpadKeepsTheIphoneKeyWidthRightAlignedInTheSafeArea() {
        val wide = doneKeyFrame(1024.0, 0.0, 20.0, true, 48.0)
        assertEquals(122.0, wide.width)
        assertEquals(1024.0 - 20.0 - 6.0 - 122.0, wide.x)
        // Narrower than three iPhone columns: clamped to the column formula.
        assertEquals(((300.0 - 24.0) / 3.0), doneKeyFrame(300.0, 0.0, 0.0, true, 48.0).width)
    }

    @Test
    fun theTextFieldOffersAnEscapeKeyCommandWithPriority() {
        val textField = (newField().view as OtpFieldContainer).textField as OtpTextField

        val commands = textField.performSelector(NSSelectorFromString("keyCommands")) as? NSArray
        val command = commands?.objectAtIndex(0uL) as? UIKeyCommand

        assertEquals(1uL, commands?.count)
        assertEquals(UIKeyInputEscape, command?.input)
        assertEquals(NSSelectorFromString("escapeKeyPressed"), command?.action)
        assertTrue(command?.wantsPriorityOverSystemBehavior == true)
        assertTrue(textField.respondsToSelector(NSSelectorFromString("escapeKeyPressed")))
    }

    @Test
    fun aTitleTooLongForOneLineMakesTheKeyTallerNotClipped() {
        val field = newField()
        field.doneButtonTitle = "Confirm the verification code now"
        val accessory = accessory(field)
        accessory.setFrame(CGRectMake(0.0, 0.0, 390.0, 60.0))
        accessory.layoutIfNeeded()

        accessory.button.frame.useContents {
            assertEquals(122.0, size.width)
            assertTrue(size.height > 48.0, "height ${size.height}")
        }
    }

    @Test
    fun escapeGestureReturnsFalseWhenTheFieldIsNotFocused() {
        val textField = (newField().view as OtpFieldContainer).textField as OtpTextField

        assertFalse(textField.performEscape())
    }
}
