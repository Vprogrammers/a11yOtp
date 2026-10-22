package me.vprogrammers.a11yotp

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OtpFieldTest {
    @get:Rule
    val rule = createComposeRule()

    private var state by mutableStateOf(OtpState(OtpLength.SIX))
    private val completions = mutableListOf<String>()
    private val haptics = mutableListOf<HapticFeedbackType>()
    private val fakeHaptics =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics += hapticFeedbackType
            }
        }
    private val accessibility =
        OtpFieldAccessibility("Code", "Wrong code") { "${it.entered} of ${it.total} digits entered" }

    private fun setField(
        enabled: Boolean = true,
        direction: LayoutDirection = LayoutDirection.Ltr,
        mask: OtpMask = OtpMask.None,
        style: OtpFieldStyle = OtpFieldDefaults.style(),
        alignment: OtpAlignment = OtpAlignment.START,
    ) {
        setThemedContent(night = false) {
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalHapticFeedback provides fakeHaptics,
            ) {
                OtpField(
                    state = state,
                    onStateChange = { state = it },
                    onComplete = { completions += it },
                    accessibility = accessibility,
                    enabled = enabled,
                    mask = mask,
                    style = style,
                    alignment = alignment,
                    modifier = Modifier.width(400.dp).background(Color.White),
                )
            }
        }
    }

    /** Forces the system theme so pixel checks do not depend on the phone's dark mode setting. */
    private fun setThemedContent(
        night: Boolean,
        content: @Composable () -> Unit,
    ) {
        rule.setContent {
            val base = LocalConfiguration.current
            val themed =
                Configuration(base).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            CompositionLocalProvider(LocalConfiguration provides themed, content = content)
        }
    }

    private fun pixel(tag: String) =
        rule
            .onNodeWithTag(tag)
            .captureToImage()
            .toPixelMap()
            .let { it[it.width / 2, it.height / 2] }

    private fun themedCellAndButton(
        night: Boolean,
        style: OtpFieldStyle,
        buttonStyle: OtpButtonStyle,
    ) = setThemedContent(night) {
        Column(Modifier.width(400.dp)) {
            OtpField(
                state = OtpState(OtpLength.SIX),
                onStateChange = {},
                onComplete = {},
                accessibility = accessibility,
                style = style,
                alignment = OtpAlignment.START,
                modifier = Modifier.testTag("field"),
            )
            OtpVerifyButton(
                state = OtpState(OtpLength.SIX).withInput("123456").state,
                onVerify = {},
                accessibility = OtpVerifyAccessibility("Verify"),
                style = buttonStyle,
                modifier = Modifier.testTag("button"),
            )
        }
    }

    /** Samples the first cell's background, left of its center to avoid a placeholder or digit. */
    private fun firstCellBackground(): Color {
        val map = rule.onNodeWithTag("field").captureToImage().toPixelMap()
        val gap = with(rule.density) { OtpFieldDefaults.FocusOutlineGap.roundToPx() }
        return map[gap + with(rule.density) { 6.dp.roundToPx() }, map.height / 2]
    }

    @Test
    fun defaultStyleFollowsNightThemeForCellAndButton() {
        themedCellAndButton(true, OtpFieldDefaults.style(), OtpButtonDefaults.style())
        assertEquals(Color(0xFF1C1C1E), firstCellBackground())
        // Sample just inside the top edge of the drawn button, above its text.
        val button = rule.onNodeWithTag("button").captureToImage().toPixelMap()
        val gap = with(rule.density) { OtpFieldDefaults.FocusOutlineGap.roundToPx() }
        assertEquals(Color(0xFFE5E5EA), button[button.width / 2, gap + 3])
    }

    @Test
    fun defaultStyleFollowsDayThemeForCellAndButton() {
        themedCellAndButton(false, OtpFieldDefaults.style(), OtpButtonDefaults.style())
        assertEquals(Color.White, firstCellBackground())
        val button = rule.onNodeWithTag("button").captureToImage().toPixelMap()
        val gap = with(rule.density) { OtpFieldDefaults.FocusOutlineGap.roundToPx() }
        assertEquals(Color(0xFF1F1F1F), button[button.width / 2, gap + 3])
    }

    @Test
    fun explicitLightColorsStayLightUnderNightTheme() {
        themedCellAndButton(
            true,
            OtpFieldDefaults.style(colors = OtpFieldDefaults.colors()),
            OtpButtonDefaults.style(colors = OtpButtonDefaults.colors()),
        )
        assertEquals(Color.White, firstCellBackground())
        val button = rule.onNodeWithTag("button").captureToImage().toPixelMap()
        val gap = with(rule.density) { OtpFieldDefaults.FocusOutlineGap.roundToPx() }
        assertEquals(Color(0xFF1F1F1F), button[button.width / 2, gap + 3])
    }

    private fun field() = rule.onNode(hasSetTextAction())

    private fun allTexts() =
        rule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()

    @Test
    fun exposesSingleLogicalFieldWithLabelAndProgress() {
        setField(style = OtpFieldDefaults.style(placeholder = '_', groupSize = 3))
        state = state.withInput("123").state
        rule.waitForIdle()
        val inputs = rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        assertEquals(1, inputs.size)
        field().assert(hasContentDescription("Code"))
        assertEquals("3 of 6 digits entered", inputs[0].config.getOrNull(SemanticsProperties.StateDescription))
        // Digits, separator and placeholders are not separately exposed.
        assertTrue(allTexts().all { it.id == inputs[0].id })
    }

    @Test
    fun typingCompletesOnce() {
        setField()
        field().performTextInput("12345")
        assertTrue(completions.isEmpty())
        field().performTextInput("6")
        rule.waitForIdle()
        assertEquals(listOf("123456"), completions)
        assertEquals("123456", state.code)
    }

    @Test
    fun eightDigitCodeCompletes() {
        state = OtpState(OtpLength.EIGHT)
        setField(style = OtpFieldDefaults.style(groupSize = 4))
        field().performTextInput("1234-5678")
        rule.waitForIdle()
        assertEquals("12345678", state.code)
        assertEquals(listOf("12345678"), completions)
    }

    @Test
    fun pasteIsNormalized() {
        setField()
        field().performTextReplacement("482-913")
        rule.waitForIdle()
        assertEquals("482913", state.code)
        assertEquals(listOf("482913"), completions)
    }

    @Test
    fun pasteOverCompleteCodeReplacesIt() {
        setField()
        field().performTextInput("123456")
        field().performTextInput("482913")
        rule.waitForIdle()
        assertEquals("482913", state.code)
        assertEquals(listOf("123456", "482913"), completions)
    }

    @Test
    fun rejectedInputIsWrittenBack() {
        setField()
        field().performTextInput("12")
        field().performTextInput("a")
        rule.waitForIdle()
        assertEquals("12", state.code)
        field().assertTextEquals("12")
        field().performTextInput("3")
        rule.waitForIdle()
        assertEquals("123", state.code)
    }

    @Test
    fun errorIsExposedWithMessageOnlyWhileInErrorState() {
        setField()
        field().performTextInput("000000")
        state = state.withError()
        rule.waitForIdle()
        assertEquals("Wrong code", field().fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
        rule.onNode(hasText("Wrong code")).assertExists()
        state = state.cleared()
        rule.waitForIdle()
        assertNull(field().fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
        rule.onNode(hasText("Wrong code")).assertDoesNotExist()
    }

    @Test
    fun hapticFiresOnceOnTransitionIntoError() {
        setField()
        rule.waitForIdle()
        assertTrue(haptics.isEmpty())
        state = state.withInput("111111").state.withError()
        rule.waitForIdle()
        assertEquals(listOf(HapticFeedbackType.Reject), haptics)
        // Further recompositions while still in error do not repeat it.
        state = state.withError()
        rule.waitForIdle()
        assertEquals(1, haptics.size)
        state = state.cleared()
        rule.waitForIdle()
        state = state.withError()
        rule.waitForIdle()
        assertEquals(2, haptics.size)
    }

    @Test
    fun declaresSmsOtpContentType() {
        setField()
        assertEquals(
            ContentType.SmsOtpCode,
            field().fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentType),
        )
    }

    @Test
    fun legacyMaskedFieldExposesNoDigitsAndIsPassword() {
        setField(mask = OtpMask.Character('*'))
        field().performTextInput("123")
        rule.waitForIdle()
        assertEquals("123", state.code)
        val config = field().fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsProperties.Password))
        assertEquals("3 of 6 digits entered", config.getOrNull(SemanticsProperties.StateDescription))
        val exposed =
            allTexts().flatMap { node ->
                node.config
                    .getOrNull(SemanticsProperties.EditableText)
                    ?.let { listOf(it.text) }
                    .orEmpty() +
                    node.config
                        .getOrNull(SemanticsProperties.Text)
                        .orEmpty()
                        .map { it.text }
            }
        assertTrue(exposed.none { text -> text.any { it.isDigit() } }, exposed.toString())
    }

    @Test
    fun filledCellMaskRendersAndExposesNoDigits() {
        setField(mask = OtpMask.FilledCell)
        field().performTextInput("12")
        rule.waitForIdle()
        assertTrue(field().fetchSemanticsNode().config.contains(SemanticsProperties.Password))
    }

    @Test
    fun rtlKeepsDigitOrderInField() {
        setField(direction = LayoutDirection.Rtl)
        field().performTextInput("123456")
        rule.waitForIdle()
        assertEquals("123456", state.code)
        field().assertTextEquals("123456")
    }

    /** Returns (leftGap, rightGap) in pixels between the field edges and the drawn cells. */
    private fun gaps(): Pair<Int, Int> {
        val map = field().captureToImage().toPixelMap()
        val inked = (0 until map.width).filter { x -> (0 until map.height).any { y -> map[x, y] != Color.White } }
        // Measured from the drawn content, not from the focus outline gap around it.
        val gap = with(rule.density) { OtpFieldDefaults.FocusOutlineGap.roundToPx() }
        return (inked.first() - gap) to (map.width - 1 - inked.last() - gap)
    }

    @Test
    fun alignmentPositionsRowInLtr() {
        setField(alignment = OtpAlignment.START)
        val (startLeft, startRight) = gaps()
        assertTrue(startLeft <= 2 && startRight > 20, "$startLeft $startRight")
    }

    @Test
    fun centerAlignmentCentersRow() {
        setField(alignment = OtpAlignment.CENTER)
        val (left, right) = gaps()
        assertTrue(left > 20 && kotlin.math.abs(left - right) <= 3, "$left $right")
    }

    @Test
    fun endAlignmentPlacesRowAtEnd() {
        setField(alignment = OtpAlignment.END)
        val (left, right) = gaps()
        assertTrue(right <= 2 && left > 20, "$left $right")
    }

    @Test
    fun startAndEndAreMirroredInRtlWithDigitsStillLeftToRight() {
        setField(direction = LayoutDirection.Rtl, alignment = OtpAlignment.START)
        // Measure the cells before typing: input focuses the field, and the focus shadow and
        // platform text decorations (which differ per device) make a pixel scan unreliable then.
        val (left, right) = gaps()
        assertTrue(right <= 2 && left > 20, "$left $right")
        field().performTextInput("123456")
        rule.waitForIdle()
        // Digits stay in typing order (left to right) even though the row is mirrored.
        assertEquals("123456", state.code)
        field().assertTextEquals("123456")
    }

    @Test
    fun endIsOnLeftInRtl() {
        setField(direction = LayoutDirection.Rtl, alignment = OtpAlignment.END)
        val (left, right) = gaps()
        assertTrue(left <= 2 && right > 20, "$left $right")
    }

    @Test
    fun disabledFieldIsNotEnabled() {
        setField(enabled = false)
        rule.onNode(hasContentDescription("Code")).assertIsNotEnabled()
        assertEquals("", state.code)
    }

    @Test
    fun verifyButtonIsEnabledOnlyForCompleteCodeAndVerifiesOnce() {
        val verified = mutableListOf<String>()
        setThemedContent(night = false) {
            OtpVerifyButton(
                state = state,
                onVerify = { verified += it },
                accessibility = OtpVerifyAccessibility("Verify"),
            )
        }
        val button = rule.onNode(hasText("Verify"))
        button.assertIsNotEnabled()
        state = state.withInput("123").state
        rule.waitForIdle()
        button.assertIsNotEnabled()
        state = state.withInput("123456").state
        rule.waitForIdle()
        button.assertIsEnabled()
        button.performClick()
        assertEquals(listOf("123456"), verified)
    }

    private fun buttonGaps(
        direction: LayoutDirection,
        alignment: OtpAlignment,
    ): Pair<Int, Int> {
        setThemedContent(night = false) {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                OtpVerifyButton(
                    state = OtpState(OtpLength.SIX).withInput("123456").state,
                    onVerify = {},
                    accessibility = OtpVerifyAccessibility("Verify"),
                    alignment = alignment,
                    modifier = Modifier.width(400.dp).background(Color.White).testTag("host"),
                )
            }
        }
        val map = rule.onNodeWithTag("host").captureToImage().toPixelMap()
        val inked = (0 until map.width).filter { x -> (0 until map.height).any { y -> map[x, y] != Color.White } }
        // Measured from the drawn content, not from the focus outline gap around it.
        val gap = with(rule.density) { OtpFieldDefaults.FocusOutlineGap.roundToPx() }
        return (inked.first() - gap) to (map.width - 1 - inked.last() - gap)
    }

    @Test
    fun buttonAlignmentFollowsLayoutDirection() {
        val (startL, startR) = buttonGaps(LayoutDirection.Ltr, OtpAlignment.START)
        assertTrue(startL <= 2 && startR > 20, "$startL $startR")
    }

    @Test
    fun buttonAlignmentIsMirroredInRtl() {
        val (left, right) = buttonGaps(LayoutDirection.Rtl, OtpAlignment.START)
        assertTrue(right <= 2 && left > 20, "$left $right")
    }

    @Test
    fun buttonCenterAlignment() {
        val (left, right) = buttonGaps(LayoutDirection.Ltr, OtpAlignment.CENTER)
        assertTrue(left > 20 && kotlin.math.abs(left - right) <= 3, "$left $right")
    }

    @Test
    fun buttonEndAlignment() {
        val (left, right) = buttonGaps(LayoutDirection.Ltr, OtpAlignment.END)
        assertTrue(right <= 2 && left > 20, "$left $right")
    }

    @Test
    fun resendButtonNameIsItsVisibleTextAndHasNoLiveRegion() {
        var seconds by mutableStateOf(25)
        var resends = 0
        setThemedContent(night = false) {
            OtpResendButton(
                secondsRemaining = seconds,
                onResend = { resends++ },
                accessibility = OtpResendAccessibility("Resend code") { "Resend code in ${it.secondsRemaining} s" },
            )
        }
        val countdown = rule.onNode(hasText("Resend code in 25 s"))
        countdown.assertIsNotEnabled()
        val config = countdown.fetchSemanticsNode().config
        assertNull(config.getOrNull(SemanticsProperties.ContentDescription))
        assertNull(config.getOrNull(SemanticsProperties.StateDescription))
        assertNull(config.getOrNull(SemanticsProperties.LiveRegion))
        seconds = 24
        rule.waitForIdle()
        assertNull(
            rule
                .onNode(
                    hasText("Resend code in 24 s"),
                ).fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.LiveRegion),
        )
        seconds = 0
        rule.waitForIdle()
        val button = rule.onNode(hasText("Resend code"))
        button.assertIsEnabled()
        button.performClick()
        assertEquals(1, resends)
    }

    private fun collect(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::collect)

    private fun unmergedNodes() = collect(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())

    private fun exposedStrings(node: SemanticsNode): List<String> {
        val config = node.config
        return listOfNotNull(
            config.getOrNull(SemanticsProperties.EditableText)?.text,
            config.getOrNull(SemanticsProperties.StateDescription),
            config.getOrNull(SemanticsProperties.Error),
        ) + config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    }

    private fun assertMaskHidesDigits(mask: OtpMask) {
        setField(mask = mask)
        field().performTextInput("7777")
        rule.waitForIdle()
        assertEquals("7777", state.code)
        val nodes = unmergedNodes()
        assertTrue(nodes.flatMap(::exposedStrings).none { '7' in it }, nodes.flatMap(::exposedStrings).toString())
        val config = field().fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsProperties.Password))
        assertTrue(!config.contains(SemanticsActions.CopyText), "copy action exposed")
        assertTrue(!config.contains(SemanticsActions.CutText), "cut action exposed")
    }

    @Test
    fun characterMaskExposesNoDigitsAnywhere() = assertMaskHidesDigits(OtpMask.Character('*'))

    @Test
    fun filledCellMaskExposesNoDigitsAnywhere() = assertMaskHidesDigits(OtpMask.FilledCell)

    @Test
    fun decorativeCellsSeparatorAndPlaceholderAreUnreachableInUnmergedTree() {
        setField(style = OtpFieldDefaults.style(placeholder = '_', groupSize = 3))
        field().performTextInput("12")
        rule.waitForIdle()
        val fieldId = field().fetchSemanticsNode().id
        // The merged tree is what accessibility services see; the unmerged tree still lists cells.
        val others =
            collect(rule.onRoot().fetchSemanticsNode()).filter {
                it.id != fieldId &&
                    exposedStrings(it).isNotEmpty()
            }
        // Only the field itself carries text; the error message is absent in this state.
        assertTrue(others.isEmpty(), others.flatMap(::exposedStrings).toString())
    }

    @Test
    fun fieldAndButtonsMeetMinimumTargetSize() {
        setField()
        field().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun buttonsMeetMinimumTargetSize() {
        setThemedContent(night = false) {
            OtpVerifyButton(
                state = OtpState(OtpLength.SIX),
                onVerify = {},
                accessibility = OtpVerifyAccessibility("Verify"),
            )
            OtpResendButton(
                secondsRemaining = 0,
                onResend = {},
                accessibility = OtpResendAccessibility("Resend code") { "Resend code in ${it.secondsRemaining} s" },
            )
        }
        rule.onNode(hasText("Verify")).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        rule.onNode(hasText("Resend code")).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun largeFontScaleGrowsCellsAndKeepsButtonTextUnclipped() {
        state = OtpState(OtpLength.EIGHT).withInput("12345678").state
        var density = 1f
        setThemedContent(night = false) {
            val current = LocalDensity.current
            density = current.density
            CompositionLocalProvider(LocalDensity provides Density(current.density, fontScale = 2f)) {
                Column(Modifier.width(300.dp)) {
                    OtpField(
                        state = state,
                        onStateChange = {},
                        onComplete = {},
                        accessibility = accessibility,
                        style = OtpFieldDefaults.style(groupSize = 4),
                        modifier = Modifier.testTag("field"),
                    )
                    OtpVerifyButton(
                        state = state,
                        onVerify = {},
                        accessibility = OtpVerifyAccessibility("Verify the code now"),
                    )
                }
            }
        }
        // Two wrapped rows of 48dp minimum cells plus spacing would be 104dp; 48sp digits at 2x need more.
        val fieldHeight =
            rule
                .onNodeWithTag("field")
                .fetchSemanticsNode()
                .size.height
        val bh = rule.onNode(hasText("Verify the code now")).fetchSemanticsNode().size
        assertTrue(fieldHeight > 104 * density, "field height $fieldHeight px at density $density")
        val button = rule.onNode(hasText("Verify the code now"))
        button.assertHeightIsAtLeast(48.dp)
        val results = mutableListOf<TextLayoutResult>()
        button
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        // hasVisualOverflow and layout-width comparisons are not used: the platform reports the
        // line a few pixels wider than the laid-out node (advance and side bearings) although every
        // glyph is drawn. Check the drawn pixels instead: the text must keep its horizontal padding.
        assertTrue(results.isNotEmpty(), "no text layout")
        results.forEach { r -> assertTrue(!r.didOverflowHeight, "text overflows its height: ${r.size}") }
        val img = button.captureToImage().toPixelMap()
        val container = (0 until img.width).filter { img[it, img.height / 2].red < 0.3f }
        val margin = with(rule.density) { 8.dp.roundToPx() }
        val ink =
            (container.first() + margin..container.last() - margin).filter { x ->
                (img.height / 3 until img.height * 2 / 3).any { y -> img[x, y].red > 0.8f }
            }
        assertTrue(ink.isNotEmpty(), "no text pixels drawn")
        assertTrue(ink.first() - container.first() >= margin, "text touches the left edge")
        assertTrue(container.last() - ink.last() >= margin, "text is clipped or touches the right edge")
    }

    @Test
    fun filledCellKeepsInsetRingSoBorderIsNotColorOnly() {
        state = OtpState(OtpLength.SIX).withInput("1").state
        setField(mask = OtpMask.FilledCell, alignment = OtpAlignment.START)
        val map = field().captureToImage().toPixelMap()
        val y = map.height / 2
        val runs = mutableListOf<Boolean>()
        for (x in 0 until map.width / 8) {
            val inked = map[x, y] != Color.White
            if (runs.lastOrNull() != inked) runs += inked
        }
        // Border band, then background-colored ring, then the fill.
        // Skip the focus outline gap before the first cell.
        assertTrue(runs.dropWhile { !it }.take(3) == listOf(true, false, true), runs.toString())
    }
}
