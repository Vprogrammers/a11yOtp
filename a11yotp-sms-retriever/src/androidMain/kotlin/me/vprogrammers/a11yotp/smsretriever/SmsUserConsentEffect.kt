package me.vprogrammers.a11yotp.smsretriever

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStarted
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.Status
import me.vprogrammers.a11yotp.OtpCharset
import me.vprogrammers.a11yotp.OtpLength

/** Why SMS User Consent did not deliver a code. */
enum class SmsUserConsentFailure {
    /** Google waited about five minutes and no message from the sender arrived. */
    TIMEOUT,

    /** The user dismissed the consent prompt or tapped Deny. No message was shared. */
    DENIED,

    /** User Consent could not start or return a prompt, for example because Google Play services is missing or outdated. */
    UNAVAILABLE,

    /**
     * The user allowed a message that has no standalone code of the field's length but has one of another
     * plausible code length (4 to 8 characters). The configured [OtpLength] probably differs from the
     * backend's code length. No code is delivered, and the message is never passed or logged.
     */
    CODE_LENGTH_MISMATCH,
}

/** Value of `Activity.RESULT_OK`, duplicated so the mapping stays testable without the Android framework. */
internal const val RESULT_OK = -1

/** Value of `Activity.RESULT_CANCELED`. */
internal const val RESULT_CANCELED = 0

/** What to do with one SMS User Consent broadcast. */
internal sealed interface ConsentBroadcastOutcome {
    /** Launch the consent prompt. */
    data object Launch : ConsentBroadcastOutcome

    class Failure(
        val reason: SmsUserConsentFailure,
    ) : ConsentBroadcastOutcome
}

/** Maps a User Consent broadcast status. Every non-launch outcome is reported, so the effect never hangs silently. */
internal fun resolveConsentBroadcast(
    statusCode: Int,
    hasIntent: Boolean,
): ConsentBroadcastOutcome =
    when {
        statusCode == STATUS_SUCCESS && hasIntent -> ConsentBroadcastOutcome.Launch
        statusCode == STATUS_TIMEOUT -> ConsentBroadcastOutcome.Failure(SmsUserConsentFailure.TIMEOUT)
        else -> ConsentBroadcastOutcome.Failure(SmsUserConsentFailure.UNAVAILABLE)
    }

/** Outcome of the consent prompt result. */
internal sealed interface ConsentResult {
    class Code(
        val code: String,
    ) : ConsentResult {
        // Omits the code so it does not end up in logs.
        override fun toString(): String = "Code"
    }

    class Failure(
        val reason: SmsUserConsentFailure,
    ) : ConsentResult
}

/** Maps the prompt's [resultCode] and the shared [message] to a result, or null when there is nothing to deliver. */
internal fun resolveConsentResult(
    resultCode: Int,
    message: String?,
    length: OtpLength,
    charset: OtpCharset,
): ConsentResult? =
    when (resultCode) {
        RESULT_OK -> {
            when (val outcome = classifyMessage(message, length, charset)) {
                is MessageOutcome.Code -> ConsentResult.Code(outcome.code)
                MessageOutcome.LengthMismatch -> ConsentResult.Failure(SmsUserConsentFailure.CODE_LENGTH_MISMATCH)
                null -> null
            }
        }

        RESULT_CANCELED -> {
            ConsentResult.Failure(SmsUserConsentFailure.DENIED)
        }

        else -> {
            null
        }
    }

/**
 * Listens once for an SMS and, after the user allows it, delivers its verification code.
 *
 * Uses the Google SMS User Consent API, which needs no SMS permission and no app hash in the message.
 * When a matching message arrives, Google shows a consent prompt (a system dialog); the effect then
 * receives only that one message if the user taps Allow. The effect is one-shot: after a code, a
 * failure, or a message without a matching code, it stops. Change [restartKey] (for example when
 * the user taps resend) to listen again. A message without any plausible code delivers nothing and
 * reports no failure. A message whose only plausible code has a different length than [length]
 * reports [SmsUserConsentFailure.CODE_LENGTH_MISMATCH].
 *
 * The code is a whole code, so replace the current input with it:
 * `state.cleared().withInput(code)`. This effect never logs the message or the code, draws no UI of
 * its own, never moves focus, and makes no accessibility announcement.
 *
 * Accessibility host responsibilities: the consent prompt is a system dialog that takes focus and
 * TalkBack reads it; when it closes, the system returns focus to the activity, so keep the OTP field
 * where it is and do not steal focus. Announce the fill or the verification result in the host UI,
 * and explain [SmsUserConsentFailure.DENIED] and [SmsUserConsentFailure.TIMEOUT] with visible text
 * plus the resend action. If the app is in the background when the message arrives, the prompt is
 * held until the host lifecycle is at least STARTED.
 *
 * Do not enable this together with [SmsRetrieverEffect]; use one of them per screen. The host must
 * be a `ComponentActivity` (any Compose host with an activity result registry).
 *
 * @param length expected code length; only a standalone run of this length is accepted.
 * @param onCode called with the code, in the field's [charset].
 * @param charset characters the code may contain; use the field's [OtpCharset].
 * @param enabled stops listening while false.
 * @param restartKey a change restarts the listener.
 * @param senderPhoneNumber restricts the prompt to messages from this number; null accepts any sender
 * (Google never offers messages from senders in the user's contacts).
 * @param onFailure called with the [SmsUserConsentFailure] reason.
 */
@Composable
fun SmsUserConsentEffect(
    length: OtpLength,
    onCode: (code: String) -> Unit,
    charset: OtpCharset = OtpCharset.NUMERIC,
    enabled: Boolean = true,
    restartKey: Any? = null,
    senderPhoneNumber: String? = null,
    onFailure: (SmsUserConsentFailure) -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnFailure by rememberUpdatedState(onFailure)
    val currentLength by rememberUpdatedState(length)
    val currentCharset by rememberUpdatedState(charset)

    // The consent Intent waits here until the host is at least STARTED, then is launched once.
    var pendingConsent by remember { mutableStateOf<Intent?>(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            when (val outcome = resolveConsentResult(result.resultCode, message, currentLength, currentCharset)) {
                is ConsentResult.Code -> currentOnCode(outcome.code)
                is ConsentResult.Failure -> currentOnFailure(outcome.reason)
                null -> Unit
            }
        }

    LaunchedEffect(pendingConsent) {
        val intent = pendingConsent ?: return@LaunchedEffect
        // Suspends while the host is stopped; cancelled (and so dropped) if the effect is disposed.
        lifecycle.withStarted { }
        pendingConsent = null
        launcher.launch(intent)
    }

    DisposableEffect(enabled, length, charset, restartKey, senderPhoneNumber) {
        if (!enabled) return@DisposableEffect onDispose {}

        var registered = false

        fun unregister(
            context: Context,
            receiver: BroadcastReceiver,
        ) {
            if (!registered) return
            registered = false
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered.
            }
        }

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                    val extras = intent.extras ?: return
                    val status =
                        BundleCompat.getParcelable(extras, SmsRetriever.EXTRA_STATUS, Status::class.java) ?: return
                    val consentIntent =
                        BundleCompat.getParcelable(extras, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                    unregister(appContext, this)
                    when (val outcome = resolveConsentBroadcast(status.statusCode, consentIntent != null)) {
                        ConsentBroadcastOutcome.Launch -> pendingConsent = consentIntent
                        is ConsentBroadcastOutcome.Failure -> currentOnFailure(outcome.reason)
                    }
                }
            }

        // Register before starting so a message that arrives immediately is not missed.
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED,
        )
        registered = true

        try {
            SmsRetriever
                .getClient(appContext)
                .startSmsUserConsent(senderPhoneNumber)
                .addOnFailureListener {
                    if (registered) {
                        unregister(appContext, receiver)
                        currentOnFailure(SmsUserConsentFailure.UNAVAILABLE)
                    }
                }
        } catch (_: RuntimeException) {
            unregister(appContext, receiver)
            currentOnFailure(SmsUserConsentFailure.UNAVAILABLE)
        }

        onDispose {
            unregister(appContext, receiver)
            pendingConsent = null
        }
    }
}
