package consumer

import me.vprogrammers.a11yotp.smsretriever.SmsRetrieverFailure
import me.vprogrammers.a11yotp.smsretriever.SmsUserConsentEffect
import me.vprogrammers.a11yotp.smsretriever.SmsUserConsentFailure

fun isTimeout(failure: SmsRetrieverFailure): Boolean = failure == SmsRetrieverFailure.TIMEOUT

fun isConsentDenied(failure: SmsUserConsentFailure): Boolean = failure == SmsUserConsentFailure.DENIED

/** Keeps the published User Consent effect on the consumer's compile classpath. */
internal val consentEffect = ::SmsUserConsentEffect
