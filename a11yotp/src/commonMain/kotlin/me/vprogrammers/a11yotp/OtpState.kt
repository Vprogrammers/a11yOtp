package me.vprogrammers.a11yotp

/**
 * Immutable state of one OTP field.
 *
 * [code] contains only ASCII digits `0`-`9` and is never longer than [length].
 * The host app is responsible for verifying the code with its server and for
 * clearing the state when the screen is left or the code expires. After a failed
 * verification, call [cleared] or let the user edit the code, so that the next
 * full code reports completion again.
 */
class OtpState private constructor(
    val length: OtpLength,
    val code: String,
    val isError: Boolean,
) {
    /** Creates an empty state without an error. */
    constructor(length: OtpLength) : this(length, "", false)

    val enteredDigitCount: Int get() = code.length

    val isComplete: Boolean get() = code.length == length.digitCount

    /**
     * Applies the full current text of the input field.
     *
     * Typing, deletion, replacement, and paste all arrive as the new field text.
     * Whitespace, dashes, dots, soft hyphens, and invisible direction marks are ignored,
     * and Unicode decimal digits in the Basic Multilingual Plane are converted to ASCII.
     * Text containing any other character is rejected and the state is unchanged.
     * Digits beyond [length] are dropped, keeping the first ones. A change to [code]
     * clears the error.
     *
     * When the code is complete and [text] is the current code followed by a different full
     * code, as happens when a code is pasted or autofilled at the end of a full field, the
     * new code replaces the old one and reports completion.
     *
     * Always write `result.state.code` back to the field, also after a rejection, so the
     * visible text and the screen-reader value stay the same.
     */
    fun withInput(text: String): OtpInputResult {
        if (isComplete && text.length > code.length && text.startsWith(code)) {
            val replacement = cleared().withInput(text.substring(code.length))
            if (replacement.state.isComplete && replacement.state.code != code) return replacement
        }
        val digits = normalizeOtpDigits(text) ?: return OtpInputResult(this, didComplete = false)
        val newCode = digits.take(length.digitCount)
        if (newCode == code) return OtpInputResult(this, didComplete = false)
        val newState = OtpState(length, newCode, isError = false)
        return OtpInputResult(newState, didComplete = !isComplete && newState.isComplete)
    }

    /** Returns an empty state with the same length and no error. */
    fun cleared(): OtpState = OtpState(length)

    /** Marks the current code as rejected, for example after failed server verification. */
    fun withError(): OtpState = if (isError) this else OtpState(length, code, isError = true)

    /** Removes the error mark and keeps the current code. */
    fun withoutError(): OtpState = if (isError) OtpState(length, code, isError = false) else this

    override fun equals(other: Any?): Boolean =
        other is OtpState && other.length == length && other.code == code && other.isError == isError

    override fun hashCode(): Int = (length.hashCode() * 31 + code.hashCode()) * 31 + isError.hashCode()

    // Omits the digits so the code does not end up in logs or crash reports.
    override fun toString(): String = "OtpState(length=$length, enteredDigitCount=$enteredDigitCount, isError=$isError)"
}

/**
 * Result of [OtpState.withInput].
 *
 * [didComplete] is true only when this input turned an incomplete code into a complete
 * one, so hosts can submit the code exactly once per completion.
 */
class OtpInputResult internal constructor(
    val state: OtpState,
    val didComplete: Boolean,
)

private const val IGNORED_CHARACTERS =
    "-.\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\uFF0D\uFF0E" +
        "\u061C\u200B\u200C\u200D\u200E\u200F\u202A\u202B\u202C\u202D\u202E" +
        "\u2060\u2066\u2067\u2068\u2069\uFEFF"

/** Returns the ASCII digits in [text], or null if [text] contains an unsupported character. */
internal fun normalizeOtpDigits(text: String): String? {
    val digits = StringBuilder(text.length)
    for (char in text) {
        val digit = char.digitToIntOrNull()
        when {
            digit != null -> digits.append('0' + digit)
            char.isWhitespace() || char in IGNORED_CHARACTERS -> Unit
            else -> return null
        }
    }
    return digits.toString()
}
