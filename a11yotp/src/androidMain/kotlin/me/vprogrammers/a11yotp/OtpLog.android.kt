package me.vprogrammers.a11yotp

import android.util.Log

internal actual fun logA11yWarning(message: String) {
    // Plain JVM host tests have an unmocked android.util.Log; logging must never break construction.
    runCatching { Log.w("A11yOtp", message) }
}
