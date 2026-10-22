package consumer

import me.vprogrammers.a11yotp.smsretriever.SmsRetrieverFailure

fun isTimeout(failure: SmsRetrieverFailure): Boolean = failure == SmsRetrieverFailure.TIMEOUT
