package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.Foundation.create

@OptIn(BetaInteropApi::class)
internal actual fun logA11yWarning(message: String) {
    // A Kotlin String is not bridged to an Objective-C object in a variadic %@ argument.
    NSLog("%@", NSString.create(string = message))
}
