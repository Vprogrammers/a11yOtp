package me.vprogrammers.a11yotp.sample

import me.vprogrammers.a11yotp.OtpLength
import me.vprogrammers.a11yotp.OtpState

/** Sample shared logic: the OTP length used by both sample apps. */
fun sampleInitialOtpState(): OtpState = OtpState(OtpLength.SIX)
