package me.vprogrammers.a11yotp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SampleScreen()
        }
    }
}

@Preview(name = "Default", showBackground = true, heightDp = 1000)
@Composable
private fun SampleScreenPreview() {
    SampleScreen()
}

@Preview(name = "Round cells, masked", showBackground = true, heightDp = 1000)
@Composable
private fun RoundMaskedPreview() {
    SampleScreen(SampleOptions(roundCells = true, mask = OtpMask.Character('\u2022')))
}

// Accessibility checks: largest font scale wraps the cells, and RTL keeps the digits left to right.
@Preview(name = "Font scale 2x", showBackground = true, heightDp = 1400, fontScale = 2f)
@Composable
private fun LargeFontPreview() {
    SampleScreen()
}

@Preview(name = "RTL, 8 digits", showBackground = true, heightDp = 1000, locale = "ar")
@Composable
private fun RtlPreview() {
    SampleScreen(SampleOptions(length = OtpLength.EIGHT))
}
