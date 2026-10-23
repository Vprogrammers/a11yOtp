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
 * The length is saved by name, with the code and the error mark. Saved data that cannot be
 * restored (an unknown length or an invalid code) is dropped, and `rememberSaveable` starts from
 * its initial value. Restoring fires no callback and does not report completion again (see
 * [OtpState.restore]). The platform may write saved state to disk, code included.
 */
val OtpStateSaver: Saver<OtpState, Any> =
    listSaver(
        save = { listOf(it.length.name, it.code, it.isError) },
        restore = { saved ->
            val length = OtpLength.entries.firstOrNull { it.name == saved.getOrNull(0) }
            val code = saved.getOrNull(1) as? String
            val isError = saved.getOrNull(2) as? Boolean
            if (length == null || code == null || isError == null) null else OtpState.restore(length, code, isError)
        },
    )
