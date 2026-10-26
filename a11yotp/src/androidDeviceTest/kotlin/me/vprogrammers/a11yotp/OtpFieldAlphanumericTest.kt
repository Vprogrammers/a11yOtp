package me.vprogrammers.a11yotp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtpFieldAlphanumericTest {
    @get:Rule
    val rule = createComposeRule()

    private var state by mutableStateOf(OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC))
    private val completions = mutableListOf<String>()

    private fun setField(
        mask: OtpMask = OtpMask.None,
        style: OtpFieldStyle = OtpFieldDefaults.style(),
    ) {
        rule.setContent {
            OtpField(
                state = state,
                onStateChange = { state = it },
                onComplete = { completions += it },
                accessibility =
                    OtpFieldAccessibility(
                        label = "Code",
                        errorMessage = "Wrong code",
                        progressDescription = { "${it.entered} of ${it.total} characters entered" },
                        inputRejectedMessage = { "Got ${it.receivedDigitCount}" },
                    ),
                mask = mask,
                style = style,
                alignment = OtpAlignment.START,
                modifier = Modifier.width(400.dp).background(Color.White),
            )
        }
    }

    private fun field() = rule.onNode(hasSetTextAction())

    /** Types one character at a time, the way a keyboard does. */
    private fun type(text: String) = text.forEach { field().performTextInput(it.toString()) }

    @Test
    fun lowercaseTypingIsShownAndStoredInUppercase() {
        setField()
        type("ab3dxz")
        rule.waitForIdle()
        assertEquals("AB3DXZ", state.code)
        field().assertTextEquals("AB3DXZ")
        assertEquals(listOf("AB3DXZ"), completions)
    }

    @Test
    fun pasteIsNormalizedAndUppercased() {
        setField()
        field().performTextReplacement("k7-m2 qp")
        rule.waitForIdle()
        assertEquals("K7M2QP", state.code)
    }

    @Test
    fun aTypedUnsupportedCharacterIsIgnored() {
        setField()
        type("a!b")
        rule.waitForIdle()
        assertEquals("AB", state.code)
    }

    @Test
    fun progressUsesTheHostCharacterWording() {
        setField()
        type("ab")
        rule.waitForIdle()
        val node = field().fetchSemanticsNode()
        assertEquals("2 of 6 characters entered", node.config.getOrNull(SemanticsProperties.StateDescription))
        assertFalse(node.config.contains(SemanticsProperties.Password))
    }

    @Test
    fun maskedAlphanumericExposesNoCharacters() {
        setField(mask = OtpMask.Character('*'))
        type("ab7")
        rule.waitForIdle()
        val node = field().fetchSemanticsNode()
        assertTrue(node.config.contains(SemanticsProperties.Password))
        val editable =
            node.config
                .getOrNull(SemanticsProperties.EditableText)
                ?.text
                .orEmpty()
        assertTrue(editable.none { it.isLetterOrDigit() }, editable)
    }

    @Test
    fun keyboardOptionsFollowCharsetAndMask() {
        otpKeyboardOptions(OtpCharset.NUMERIC, masked = false).let {
            assertEquals(KeyboardType.Number, it.keyboardType)
            assertEquals(ImeAction.Done, it.imeAction)
        }
        assertEquals(KeyboardType.NumberPassword, otpKeyboardOptions(OtpCharset.NUMERIC, masked = true).keyboardType)
        otpKeyboardOptions(OtpCharset.ALPHANUMERIC, masked = false).let {
            // Not Password: it would suppress TalkBack key echo.
            assertEquals(KeyboardType.Ascii, it.keyboardType)
            assertEquals(KeyboardCapitalization.Characters, it.capitalization)
            assertEquals(false, it.autoCorrectEnabled)
            assertEquals(ImeAction.Done, it.imeAction)
        }
        otpKeyboardOptions(OtpCharset.ALPHANUMERIC, masked = true).let {
            assertEquals(KeyboardType.Password, it.keyboardType)
            assertEquals(KeyboardCapitalization.Characters, it.capitalization)
        }
    }

    /** Right edge of everything drawn, in pixels. */
    private fun inkedRight(): Int {
        val map = field().captureToImage().toPixelMap()
        return (0 until map.width).last { x -> (0 until map.height).any { y -> map[x, y] != Color.White } }
    }

    @Test
    fun cellRowWidthDoesNotDependOnTheLetters() {
        // A large proportional font, so "W" (but not "I") is wider than the 48dp minimum cell.
        setField(
            style =
                OtpFieldDefaults.style(
                    textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 48.sp),
                ),
        )
        val empty = inkedRight()
        state = state.withInput("IIIIII").state
        rule.waitForIdle()
        val narrow = inkedRight()
        state = state.cleared().withInput("WWWWWW").state
        rule.waitForIdle()
        val wide = inkedRight()
        assertEquals(empty, narrow)
        assertEquals(empty, wide)
    }

    @Test
    fun theInvisibleWidthGlyphNeverReachesTheSemanticsTree() {
        setField()
        type("ab")
        rule.waitForIdle()
        rule.onAllNodes(hasText("W", substring = true), useUnmergedTree = false).assertCountEquals(0)
        val config = field().fetchSemanticsNode().config
        assertFalse(
            config
                .getOrNull(SemanticsProperties.EditableText)
                ?.text
                .orEmpty()
                .contains('W'),
        )
        assertFalse(config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text.contains('W') })
        assertFalse(config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { it.contains('W') })
        assertFalse(config.getOrNull(SemanticsProperties.StateDescription).orEmpty().contains('W'))
    }
}
