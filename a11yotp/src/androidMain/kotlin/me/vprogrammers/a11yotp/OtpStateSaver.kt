package me.vprogrammers.a11yotp

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Saves an [OtpState] for `rememberSaveable`, so a partly entered code survives configuration
 * changes and process death:
 *
 * ```
 * var otp by rememberSaveable(stateSaver = OtpStateSaver) { mutableStateOf(OtpState(OtpLength.SIX)) }
 * ```
 *
 * The length and charset are saved by name, with the code and the error mark. A numeric state is
 * saved in the 0.1.0 format, and data saved without a charset restores as numeric. Saved data that cannot be
 * restored (an unknown length or an invalid code) is dropped, and `rememberSaveable` starts from
 * its initial value. Restoring fires no callback and does not report completion again (see
 * [OtpState.restore]). The platform may write saved state to disk, code included.
 */
val OtpStateSaver: Saver<OtpState, Any> =
    listSaver(
        save = {
            val saved = listOf(it.length.name, it.code, it.isError)
            if (it.charset == OtpCharset.NUMERIC) saved else saved + it.charset.name
        },
        restore = { saved ->
            val length = OtpLength.entries.firstOrNull { it.name == saved.getOrNull(0) }
            val code = saved.getOrNull(1) as? String
            val isError = saved.getOrNull(2) as? Boolean
            val charset =
                if (saved.size <= 3) OtpCharset.NUMERIC else OtpCharset.entries.firstOrNull { it.name == saved[3] }
            if (length == null || charset == null || code == null || isError == null) {
                null
            } else {
                OtpState.restore(length, charset, code, isError)
            }
        },
    )
