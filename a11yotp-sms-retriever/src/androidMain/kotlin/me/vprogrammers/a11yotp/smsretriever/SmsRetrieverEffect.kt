package me.vprogrammers.a11yotp.smsretriever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.Status
import me.vprogrammers.a11yotp.OtpLength

/** Why the SMS Retriever did not deliver a code. */
enum class SmsRetrieverFailure {
    /** The retriever waited about five minutes and no matching message arrived. */
    TIMEOUT,

    /** The retriever could not start, for example because Google Play services is missing or outdated. */
    UNAVAILABLE,
}

/**
 * Listens once for an SMS that carries the app's hash and delivers its verification code.
 *
 * Uses the Google SMS Retriever API, which needs no SMS permission. The message must end with
 * the 11-character app hash. The retriever is one-shot: after a code, a timeout, a failure, or
 * a message without a matching code, it stops. Change [restartKey] (for example when the user
 * taps resend) to listen again. A message without a code delivers nothing and reports no failure.
 *
 * The code is a whole code, so replace the current input with it:
 * `state.cleared().withInput(code)`. That clears any error and reports completion even when the
 * user had already typed a full code. This effect never logs the message or the code and makes no
 * accessibility announcement; announce the fill or the verification result in the host UI.
 *
 * @param length expected code length; only a standalone digit run of this length is accepted.
 * @param onCode called with the digits of the code.
 * @param enabled stops listening while false.
 * @param restartKey a change restarts the listener.
 * @param onFailure called on [SmsRetrieverFailure.TIMEOUT] or [SmsRetrieverFailure.UNAVAILABLE].
 */
@Composable
fun SmsRetrieverEffect(
    length: OtpLength,
    onCode: (code: String) -> Unit,
    enabled: Boolean = true,
    restartKey: Any? = null,
    onFailure: (SmsRetrieverFailure) -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnFailure by rememberUpdatedState(onFailure)

    DisposableEffect(enabled, length, restartKey) {
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
                    val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                    unregister(appContext, this)
                    when (val result = resolveSmsResult(status.statusCode, message, length)) {
                        is SmsResult.Code -> currentOnCode(result.code)
                        is SmsResult.Failure -> currentOnFailure(result.reason)
                        null -> Unit
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
                .startSmsRetriever()
                .addOnFailureListener {
                    if (registered) {
                        unregister(appContext, receiver)
                        currentOnFailure(SmsRetrieverFailure.UNAVAILABLE)
                    }
                }
        } catch (_: RuntimeException) {
            unregister(appContext, receiver)
            currentOnFailure(SmsRetrieverFailure.UNAVAILABLE)
        }

        onDispose { unregister(appContext, receiver) }
    }
}
