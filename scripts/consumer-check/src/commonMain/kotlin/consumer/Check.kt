package consumer

import me.vprogrammers.a11yotp.OtpLength
import me.vprogrammers.a11yotp.OtpState

fun completeCode(code: String): Boolean = OtpState(OtpLength.SIX).withInput(code).state.isComplete
