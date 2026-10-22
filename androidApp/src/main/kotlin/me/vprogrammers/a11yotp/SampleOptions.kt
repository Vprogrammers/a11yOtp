package me.vprogrammers.a11yotp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Demo options below the sample flow, the same as in the iOS sample. */
data class SampleOptions(
    val length: OtpLength = OtpLength.SIX,
    val mask: OtpMask = OtpMask.None,
    val roundCells: Boolean = false,
    val isDisabled: Boolean = false,
)

private val masks =
    listOf(
        "None" to OtpMask.None,
        "Character" to OtpMask.Character('•'),
        "Filled cell" to OtpMask.FilledCell,
    )
private val textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF1B1B1F))
private val outline = Color(0xFF767676)

@Composable
fun SampleOptionsPanel(
    options: SampleOptions,
    onChange: (SampleOptions) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            "Options",
            style = textStyle.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        Segmented("Code length", OtpLength.entries.map { "${it.digitCount}" to it }, options.length) {
            onChange(options.copy(length = it))
        }
        Segmented("Masking", masks, options.mask) { onChange(options.copy(mask = it)) }
        Checkbox("Round cells", options.roundCells) { onChange(options.copy(roundCells = it)) }
        Checkbox("Disable field", options.isDisabled) { onChange(options.copy(isDisabled = it)) }
    }
}

/** Foundation-only segmented control. The selected segment is filled and bold, not only colored. */
@Composable
private fun <T> Segmented(
    label: String,
    items: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(label, style = textStyle)
        Row(Modifier.fillMaxWidth().border(1.dp, outline, shape).selectableGroup()) {
            for ((text, value) in items) {
                val isSelected = value == selected
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .background(if (isSelected) Color(0xFF1B1B1F) else Color.Transparent, shape)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) }),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text,
                        style =
                            textStyle.copy(
                                color = if (isSelected) Color.White else textStyle.color,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                    )
                }
            }
        }
    }
}

/** Foundation-only checkbox: one 48dp touch target, checked state shown by a check mark, not color. */
@Composable
private fun Checkbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(24.dp).border(2.dp, Color(0xFF1B1B1F), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) BasicText("✓", style = textStyle.copy(fontWeight = FontWeight.Bold))
        }
        BasicText(label, style = textStyle)
    }
}
