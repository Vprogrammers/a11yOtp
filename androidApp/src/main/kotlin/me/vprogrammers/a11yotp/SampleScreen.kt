package me.vprogrammers.a11yotp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.vprogrammers.a11yotp.sample.SAMPLE_RESEND_SECONDS
import me.vprogrammers.a11yotp.sample.isSampleCodeRejected
import me.vprogrammers.a11yotp.smsretriever.SmsRetrieverEffect
import me.vprogrammers.a11yotp.smsretriever.SmsRetrieverFailure

/** Groups of 3 give "123 - 456"; 4 and 8 digits do not split evenly, so they get no separator. */
private val fieldStyle = OtpFieldDefaults.style(groupSize = 3, cellHeight = 56.dp)

/** Square cells drawn as circles. */
private val roundFieldStyle = OtpFieldDefaults.style(groupSize = 3, cellShape = CircleShape)

/** The rejection text is a plurals resource, quantity = digits received ("1 digit" vs "4 digits"). */
private fun fieldAccessibility(context: Context) =
    OtpFieldAccessibility(
        label = "Verification code",
        errorMessage = "Incorrect code. Try again.",
        progressDescription = { "${it.entered} of ${it.total} digits entered" },
        inputRejectedMessage = {
            context.resources.getQuantityString(
                R.plurals.otp_rejected,
                it.receivedDigitCount,
                it.receivedDigitCount,
                it.expectedDigitCount,
            )
        },
    )

private val verifyAccessibility = OtpVerifyAccessibility("Verify")
private val resendAccessibility = OtpResendAccessibility("Resend code") { "Resend code in ${it.secondsRemaining} s" }

/**
 * Sample screen, the same flow as the iOS sample. Strings are English literals for the sample
 * only; real apps pass localized strings. A code of only zeros is rejected (see sample/shared).
 */
@Composable
fun SampleScreen(initialOptions: SampleOptions = SampleOptions()) {
    var options by remember { mutableStateOf(initialOptions) }
    var state by remember { mutableStateOf(OtpState(options.length)) }
    var verified by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(SAMPLE_RESEND_SECONDS) }
    var resendCount by remember { mutableIntStateOf(0) }
    var smsLengthMismatch by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }

    // Focus the field once so the keyboard and its code suggestion appear without a tap.
    LaunchedEffect(Unit) {
        if (!options.isDisabled) fieldFocus.requestFocus()
    }

    LaunchedEffect(seconds) {
        if (seconds > 0) {
            delay(1000)
            seconds--
        }
    }

    // Called on completion and by the Verify button.
    fun verify(code: String) {
        if (verified) return
        if (isSampleCodeRejected(code)) state = state.withError() else verified = true
    }

    // Optional SMS Retriever: the SMS carries the whole code, so it replaces what was typed and
    // still goes through withInput, which reports completion once.
    SmsRetrieverEffect(
        length = state.length,
        restartKey = resendCount,
        enabled = !verified && !options.isDisabled,
        onFailure = { smsLengthMismatch = it == SmsRetrieverFailure.CODE_LENGTH_MISMATCH },
        onCode = { code ->
            smsLengthMismatch = false
            val result = state.cleared().withInput(code)
            state = result.state
            if (result.didComplete) verify(result.state.code)
        },
    )

    val dark = isSystemInDarkTheme()
    val ink = if (dark) Color.White else Color(0xFF1B1B1F)
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (dark) Color.Black else Color.White)
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = "Enter the ${state.length.digitCount}-digit code we sent you",
            style =
                TextStyle(
                    color = ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            modifier = Modifier.semantics { heading() },
        )
        OtpField(
            state = state,
            onStateChange = {
                state = it
                if (!it.isComplete) verified = false
            },
            onComplete = ::verify,
            accessibility = fieldAccessibility(LocalContext.current),
            modifier = Modifier.focusRequester(fieldFocus),
            enabled = !verified && !options.isDisabled,
            style = if (options.roundCells) roundFieldStyle else fieldStyle,
            mask = options.mask,
        )
        if (smsLengthMismatch) {
            BasicText(
                text = "The text message has a code of a different length than this field.",
                style = TextStyle(color = ink, fontSize = 14.sp, textAlign = TextAlign.Center),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        OtpVerifyButton(
            state = state,
            onVerify = ::verify,
            accessibility = verifyAccessibility,
            enabled = !verified && !state.isError,
        )
        if (verified) {
            // The check mark and text carry the result, so the default text color is enough.
            BasicText(
                text = "✓ Code verified",
                style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        OtpResendButton(
            secondsRemaining = seconds,
            onResend = {
                state = state.cleared()
                verified = false
                seconds = SAMPLE_RESEND_SECONDS
                resendCount++
                smsLengthMismatch = false
            },
            accessibility = resendAccessibility,
        )
        SampleOptionsPanel(
            options = options,
            onChange = { new ->
                if (new.length != options.length) {
                    state = OtpState(new.length)
                    verified = false
                }
                options = new
            },
        )
    }
}
