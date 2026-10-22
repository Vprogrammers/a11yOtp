package me.vprogrammers.a11yotp

import platform.Foundation.NSLog

internal actual fun logA11yWarning(message: String) {
    NSLog("%@", message)
}
