package me.vprogrammers.a11yotp

/** Applies [text] one character at a time, the way a user types, so partial codes can be built. */
internal fun OtpState.typed(text: String): OtpState =
    text.fold(this) { state, char -> state.withInput(state.code + char).state }
