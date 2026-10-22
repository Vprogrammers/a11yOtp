package me.vprogrammers.a11yotp

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * One-time-code input that exposes a single logical text field to screen readers while
 * drawing the digits as decorative cells (cells, group separators and placeholders are not
 * in the accessibility tree).
 *
 * The host owns [state] and verification: call [OtpState.withError] or [OtpState.cleared]
 * after a failed verification. [onComplete] fires once per incomplete-to-complete
 * transition. [accessibility] is required and must be localized by the host. With a [mask]
 * other than [OtpMask.None] the field uses a password keyboard and password semantics, and
 * screen readers get the label and progress only (tested: no digits in any semantics, no
 * copy or cut action). [hapticFeedbackOnError] vibrates once when [OtpState.isError] becomes
 * true. [alignment] (default centered) only positions the cells and error text; the digits
 * inside the row always read left to right. The field spans the available width.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpField(
    state: OtpState,
    onStateChange: (OtpState) -> Unit,
    onComplete: (code: String) -> Unit,
    accessibility: OtpFieldAccessibility,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: OtpFieldStyle = OtpFieldDefaults.style(),
    mask: OtpMask = OtpMask.None,
    hapticFeedbackOnError: Boolean = true,
    alignment: OtpAlignment = OtpAlignment.CENTER,
) {
    // Resolve against the surrounding direction before the cells are forced to LTR.
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val horizontal = alignment.absoluteHorizontal(isRtl)
    val textAlign = alignment.absoluteTextAlign(isRtl)
    val total = state.length.digitCount
    val visibleError = if (state.isError) accessibility.errorMessage else null
    val masked = mask != OtpMask.None
    var focused by remember { mutableStateOf(false) }
    // Always show the committed code with the cursor at the end.
    val fieldValue = TextFieldValue(state.code, TextRange(state.code.length))
    val progress = accessibility.progressDescription(OtpProgress(state.enteredDigitCount, total))

    val haptics = LocalHapticFeedback.current
    var wasError by remember { mutableStateOf(state.isError) }
    LaunchedEffect(state.isError) {
        if (state.isError && !wasError && hapticFeedbackOnError) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
        }
        wasError = state.isError
    }

    Column(modifier = modifier) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { new ->
                val result = state.withInput(new.text)
                onStateChange(result.state)
                if (result.didComplete) onComplete(result.state.code)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(
                        minWidth = OtpFieldDefaults.MinTargetSize,
                        minHeight = OtpFieldDefaults.MinTargetSize,
                    ).onFocusChanged { focused = it.isFocused }
                    .semantics {
                        contentDescription = accessibility.label
                        stateDescription = progress
                        contentType = ContentType.SmsOtpCode
                        if (masked) isSensitiveData = true
                        if (visibleError != null) error(visibleError)
                    },
            enabled = enabled,
            textStyle = style.textStyle.copy(color = Color.Transparent),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (masked) KeyboardType.NumberPassword else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
            singleLine = true,
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                // The real input stays in the tree but is invisible; cells are decorative. The gap keeps
                // the TalkBack focus outline around the cells; the top also makes room for the lift, so
                // the lifted focused cell stays centered in the outline.
                val gap = OtpFieldDefaults.FocusOutlineGap
                val top = if (style.liftFocusedCell) gap + FocusLift else gap
                Box(Modifier.fillMaxWidth().padding(start = gap, top = top, end = gap, bottom = gap)) {
                    Box(Modifier.size(1.dp).alpha(0f)) { innerTextField() }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OtpCells(state, focused && enabled, enabled, style, mask, horizontal)
                    }
                }
            },
        )
        if (visibleError != null) {
            BasicText(
                text = visibleError,
                style =
                    style.errorTextStyle.copy(
                        color = style.colors.errorText,
                        textAlign = textAlign,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * Lays out the decorative cells with the shared [OtpCellsPlanner], so Android and iOS wrap the same
 * way: one row with separators, then one row without them, then whole groups per row, then cells
 * per row, and cells shrink only as a last resort.
 */
@Composable
private fun OtpCells(
    state: OtpState,
    focused: Boolean,
    enabled: Boolean,
    style: OtpFieldStyle,
    mask: OtpMask,
    horizontal: Alignment.Horizontal,
) {
    val total = state.length.digitCount
    val activeIndex = state.enteredDigitCount.coerceAtMost(total - 1)
    val groupSize = evenGroupSize(style.groupSize, total) ?: 0
    val separatorCount = if (groupSize > 0) total / groupSize - 1 else 0
    Layout(
        content = {
            for (index in 0 until total) {
                OtpCell(
                    digit = state.code.getOrNull(index),
                    isActive = focused && index == activeIndex,
                    isError = state.isError,
                    enabled = enabled,
                    style = style,
                    mask = mask,
                )
            }
            repeat(separatorCount) {
                BasicText(
                    text = "–",
                    style = style.textStyle.copy(color = if (enabled) style.colors.text else style.colors.disabledText),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
    ) { measurables, constraints ->
        val cells = measurables.take(total)
        val separators = measurables.drop(total)
        val spacing = style.cellSpacing.toPx().toDouble()
        val separatorWidth = separators.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        val planner =
            OtpCellsPlanner(
                count = total,
                groupSize = groupSize,
                cellWidth = cells.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }.toDouble(),
                spacing = spacing,
                separatorWidth = separatorWidth.toDouble(),
            )
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth.toDouble() else null
        val plan = planner.plan(available)
        val cellWidth = floor(plan.cellWidth).toInt().coerceAtLeast(1)
        val cellHeight = cells.maxOf { it.maxIntrinsicHeight(cellWidth) }
        val cellPlaceables = cells.map { it.measure(Constraints.fixed(cellWidth, cellHeight)) }
        val separatorPlaceables = separators.map { it.measure(Constraints()) }
        val rowWidths = plan.rows.map { planner.rowWidth(it, cellWidth.toDouble(), plan.showsSeparators) }
        val width = available?.toInt() ?: rowWidths.maxOf { it }.roundToInt()
        val rowStep = cellHeight + spacing.roundToInt()
        val height = plan.rows.size * rowStep - spacing.roundToInt()
        layout(width, height) {
            plan.rows.forEachIndexed { rowIndex, row ->
                var x = horizontal.align(rowWidths[rowIndex].roundToInt(), width, layoutDirection).toDouble()
                val y = rowIndex * rowStep
                for (cell in row.start until row.endExclusive) {
                    cellPlaceables[cell].place(x.roundToInt(), y)
                    x += cellWidth
                    val endsGroup = groupSize > 0 && (cell + 1) % groupSize == 0
                    if (plan.showsSeparators && endsGroup && cell != row.endExclusive - 1) {
                        val separator = separatorPlaceables[(cell + 1) / groupSize - 1]
                        separator.place(
                            (x + spacing + (separatorWidth - separator.width) / 2.0).roundToInt(),
                            y + (cellHeight - separator.height) / 2,
                        )
                    }
                    x += planner.gapAfter(cell, plan.showsSeparators)
                }
            }
        }
    }
}

@Composable
private fun OtpCell(
    digit: Char?,
    isActive: Boolean,
    isError: Boolean,
    enabled: Boolean,
    style: OtpFieldStyle,
    mask: OtpMask,
) {
    val colors = style.colors
    val borderColor =
        when {
            !enabled -> colors.disabledCellBorder
            isError -> colors.errorCellBorder
            isActive -> colors.focusedCellBorder
            else -> colors.cellBorder
        }
    // Error and focus both use the thicker border so neither relies on color alone.
    val borderWidth = if (enabled && (isError || isActive)) style.focusedBorderWidth else style.borderWidth
    val baseBackground = if (enabled) colors.cellBackground else colors.disabledCellBackground
    val filled = digit != null && mask == OtpMask.FilledCell
    // White on a dark cell background, where a black shadow would not show; black otherwise.
    // A white glow needs more opacity than a black shadow to be as noticeable.
    val lightShadow = baseBackground.alpha == 1f && otpShadowIsLight(baseBackground.toArgb() and 0xFFFFFF)
    val shadowColor = if (lightShadow) Color.White else Color.Black
    val shadowAlpha = if (lightShadow) FOCUS_LIGHT_SHADOW_ALPHA else FOCUS_SHADOW_ALPHA
    // The thicker border shows focus; the lift and shadow are extra. Compose animations follow the
    // system animator scale, so "Remove animations" makes the move instant.
    val lift by animateDpAsState(if (isActive && style.liftFocusedCell) FocusLift else 0.dp, label = "otpCellLift")
    var cellModifier =
        Modifier
            .offset { IntOffset(0, -lift.roundToPx()) }
            .dropShadow(style.cellShape) {
                // Same look as iOS: 35% opacity, 8dp blur, 6dp down. Elevation shadows are too
                // faint on many devices, especially Samsung.
                val progress = lift / FocusLift
                radius = FocusShadowRadius.toPx()
                offset = Offset(0f, FocusShadowOffset.toPx())
                color = shadowColor
                alpha = shadowAlpha * progress
            }.defaultMinSize(minWidth = style.cellWidth, minHeight = style.cellHeight)
            .background(if (filled) borderColor else baseBackground, style.cellShape)
            .border(borderWidth, borderColor, style.cellShape)
    if (filled) {
        // Inset ring in the cell background color keeps the (thicker) focus/error border
        // visible by shape and width even though fill and border share a hue.
        cellModifier = cellModifier.padding(borderWidth).border(FilledRingWidth, colors.cellBackground, style.cellShape)
    }
    Box(
        modifier = cellModifier.padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        val textColor = if (enabled) colors.text else colors.disabledText
        when {
            digit != null && mask is OtpMask.Character -> {
                BasicText(mask.char.toString(), style = style.textStyle.copy(color = textColor))
            }

            digit != null && mask == OtpMask.None -> {
                BasicText(digit.toString(), style = style.textStyle.copy(color = textColor))
            }

            digit == null && style.placeholder != null -> {
                BasicText(
                    style.placeholder.toString(),
                    style = style.textStyle.copy(color = if (enabled) colors.placeholder else colors.disabledText),
                )
            }
        }
    }
}

private val FilledRingWidth = 2.dp
private val FocusLift = 4.dp

private val FocusShadowRadius = 8.dp
private val FocusShadowOffset = 6.dp
private const val FOCUS_SHADOW_ALPHA = 0.35f
private const val FOCUS_LIGHT_SHADOW_ALPHA = 0.6f
