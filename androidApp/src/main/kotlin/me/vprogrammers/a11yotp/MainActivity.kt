package me.vprogrammers.a11yotp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SampleScreen()
        }
    }
}

private const val RESEND_SECONDS = 30

@Preview
@Composable
fun SampleScreen() {
    var state by remember { mutableStateOf(OtpState(OtpLength.SIX)) }
    var verified by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(RESEND_SECONDS) }

    LaunchedEffect(seconds) {
        if (seconds > 0) {
            delay(1000)
            seconds--
        }
    }

    val buttonStyle =
        OtpButtonDefaults.style(
            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
            cornerRadius = 24.dp,
            colors = OtpButtonDefaults.colors(container = Color(0xFF004D40)),
        )

    // Fake verification: only "000000" fails. Verified codes are not checked again.
    fun verify(code: String) {
        if (verified) return
        if (code == "000000") state = state.withError() else verified = true
    }

    Column(
        modifier = Modifier.safeContentPadding().fillMaxSize().background(Color.White),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = "Enter the 6-digit code we sent you",
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
        OtpField(
            state = state,
            onStateChange = {
                state = it
                verified = false
            },
            // Auto-submit on completion; the Verify button is the manual path to the same function.
            onComplete = ::verify,
            accessibility =
                OtpFieldAccessibility(
                    label = "Verification code",
                    errorMessage = "Incorrect code. Try again.",
                    progressDescription = { "${it.entered} of ${it.total} digits entered" },
                ),
            style = OtpFieldDefaults.style(groupSize = 3),
        )
        OtpVerifyButton(
            state = state,
            onVerify = ::verify,
            accessibility = OtpVerifyAccessibility("Verify"),
            enabled = !verified,
            style = buttonStyle,
        )
        if (verified) {
            BasicText(
                text = "\u2713 Code verified",
                style = TextStyle(fontSize = 18.sp, color = Color(0xFF0B6B2B)),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        OtpResendButton(
            secondsRemaining = seconds,
            onResend = {
                state = state.cleared()
                verified = false
                seconds = RESEND_SECONDS
            },
            accessibility =
                OtpResendAccessibility(
                    label = "Resend code",
                    countdownDescription = { "Resend code in ${it.secondsRemaining} s" },
                ),
            style = buttonStyle,
        )
    }
}
