package me.vprogrammers.a11yotp

/**
 * Immutable state of one OTP field.
 *
 * [code] contains only characters of [charset] (ASCII digits `0`-`9` by default) and is never
 * longer than [length]. Counts such as [enteredDigitCount] and [OtpLength.digitCount] count
 * characters, letters included.
 * The host app is responsible for verifying the code with its server and for
 * clearing the state when the screen is left or the code expires. After a failed
 * verification, call [cleared] or let the user edit the code, so that the next
 * full code reports completion again.
 */
class OtpState private constructor(
    val length: OtpLength,
    /** Which characters [code] may contain. */
    val charset: OtpCharset,
    val code: String,
    val isError: Boolean,
) {
    /** Creates an empty numeric state without an error. */
    constructor(length: OtpLength) : this(length, OtpCharset.NUMERIC, "", false)

    /** Creates an empty state for [charset] without an error. */
    constructor(length: OtpLength, charset: OtpCharset) : this(length, charset, "", false)

    val enteredDigitCount: Int get() = code.length

    val isComplete: Boolean get() = code.length == length.digitCount

    /**
     * Applies the full current text of the input field.
     *
     * Typing, deletion, replacement, and paste all arrive as the new field text.
     * Whitespace, dashes, dots, soft hyphens, and invisible direction marks are ignored,
     * and Unicode decimal digits in the Basic Multilingual Plane are converted to ASCII. With
     * [OtpCharset.ALPHANUMERIC], ASCII and fullwidth Latin letters are converted to uppercase ASCII.
     * Text containing any other character is ignored and the state is unchanged. A change to
     * [code] clears the error.
     *
     * The digits that this input inserts are found by comparing [text] with the current code
     * (the common start and end are not new). Then:
     * - One digit or fewer inserted (typing or deletion) is applied. A digit typed into a full
     *   field is ignored without a rejection.
     * - Otherwise, if [text] holds exactly [length] digits in total, they replace the current
     *   code (autofill or select-all and paste, also when the new code shares its start or end
     *   with the old one).
     * - Otherwise, if the inserted digits number exactly [length] (paste appended to the current
     *   code, as on iOS), the inserted digits replace the current code.
     * - An accepted replacement reports completion, also over a different complete code. Pasting
     *   the current code again changes nothing.
     * - Any other number of digits inserted at once is rejected: the state is unchanged, [OtpInputResult.didComplete]
     *   is false, and [OtpInputResult.rejection] carries the expected and received digit counts.
     *   A code is never truncated or filled in partially. A rejection means the host's configured
     *   length probably differs from what its backend sends.
     *
     * Always write `result.state.code` back to the field, also after a rejection, so the
     * visible text and the screen-reader value stay the same.
     */
    fun withInput(text: String): OtpInputResult {
        val digits = normalizeOtpInput(text, charset) ?: return OtpInputResult(this, didComplete = false)
        val expected = length.digitCount
        val inserted = insertedDigits(code, digits)
        val newCode =
            when {
                inserted.length <= 1 -> digits.take(expected)

                digits.length == expected -> digits

                inserted.length == expected -> inserted

                else -> return OtpInputResult(
                    this,
                    didComplete = false,
                    rejection = OtpInputRejection(expected, inserted.length),
                )
            }
        if (newCode == code) return OtpInputResult(this, didComplete = false)
        val newState = OtpState(length, charset, newCode, isError = false)
        // A whole code that replaces a different complete code is a new completion.
        val replacedWholeCode = inserted.length > 1
        return OtpInputResult(newState, didComplete = newState.isComplete && (!isComplete || replacedWholeCode))
    }

    /** Returns an empty state with the same length and charset and no error. */
    fun cleared(): OtpState = OtpState(length, charset)

    /** Marks the current code as rejected, for example after failed server verification. */
    fun withError(): OtpState = if (isError) this else OtpState(length, charset, code, isError = true)

    /** Removes the error mark and keeps the current code. */
    fun withoutError(): OtpState = if (isError) OtpState(length, charset, code, isError = false) else this

    override fun equals(other: Any?): Boolean =
        other is OtpState &&
            other.length == length &&
            other.charset == charset &&
            other.code == code &&
            other.isError == isError

    override fun hashCode(): Int =
        ((length.hashCode() * 31 + charset.hashCode()) * 31 + code.hashCode()) * 31 + isError.hashCode()

    // Omits the digits so the code does not end up in logs or crash reports.
    override fun toString(): String =
        "OtpState(length=$length, enteredDigitCount=$enteredDigitCount, charset=$charset, isError=$isError)"

    companion object {
        /**
         * Rebuilds a numeric state from values saved earlier from [length], [code] and [isError], for example
         * across Android configuration changes or process death, or iOS scene restoration.
         *
         * Returns null unless [code] holds only the ASCII digits `0`-`9` and is no longer than
         * [length]: the values a state can hold. Nothing is normalized, because saved values come from
         * a state, not from user input. Restoring is not input: no callback fires and completion is
         * not reported again, so a restored complete code is not submitted twice.
         *
         * Saved state can be written to disk by the platform. If the code must not outlive the
         * screen, do not save it, or clear it when the screen is left.
         */
        fun restore(
            length: OtpLength,
            code: String,
            isError: Boolean,
        ): OtpState? = restore(length, OtpCharset.NUMERIC, code, isError)

        /**
         * Rebuilds a state from values saved earlier from [length], [charset], [code] and [isError], like
         * the numeric overload. Returns null unless [code] holds only characters of [charset] (uppercase
         * letters for [OtpCharset.ALPHANUMERIC]) and is no longer than [length].
         */
        fun restore(
            length: OtpLength,
            charset: OtpCharset,
            code: String,
            isError: Boolean,
        ): OtpState? {
            if (code.length > length.digitCount || !code.all { it.isOtpChar(charset) }) return null
            return OtpState(length, charset, code, isError)
        }
    }
}

/**
 * Result of [OtpState.withInput].
 *
 * [didComplete] is true only when this input turned an incomplete code into a complete
 * one, so hosts can submit the code exactly once per completion.
 *
 * [rejection] is non-null when the input inserted several digits at once and their count
 * differs from the field length. The state is then unchanged and [didComplete] is false.
 */
class OtpInputResult internal constructor(
    val state: OtpState,
    val didComplete: Boolean,
    val rejection: OtpInputRejection? = null,
)

/**
 * A multi-digit input (paste, autofill, keyboard suggestion) that was rejected because it did
 * not contain exactly [expectedDigitCount] digits. Only the counts are available, never the
 * digits, so they cannot end up in logs. A rejection usually means that the configured
 * [OtpLength] differs from the code length of the host's backend.
 */
class OtpInputRejection internal constructor(
    val expectedDigitCount: Int,
    val receivedDigitCount: Int,
) {
    override fun equals(other: Any?): Boolean =
        other is OtpInputRejection &&
            other.expectedDigitCount == expectedDigitCount &&
            other.receivedDigitCount == receivedDigitCount

    override fun hashCode(): Int = 31 * expectedDigitCount + receivedDigitCount

    override fun toString(): String =
        "OtpInputRejection(expectedDigitCount=$expectedDigitCount, receivedDigitCount=$receivedDigitCount)"
}

/**
 * The part of [new] that was inserted into [old]: [new] without the longest common start and then
 * the longest common end of what remains (the two never overlap).
 *
 * The field always shows the cursor at the end, so a selection that replaces only part of the code
 * cannot be told apart from an insertion here. Selecting all and pasting fewer digits than the field
 * length is rejected like any other partial paste. The caller must not use the result to count a
 * whole-code replacement: shared digits are stripped, so a full code that overlaps the old one
 * yields fewer digits than its length.
 */
private fun insertedDigits(
    old: String,
    new: String,
): String {
    val prefix = old.commonPrefixWith(new).length
    val oldRest = old.substring(prefix)
    val newRest = new.substring(prefix)
    val suffix = oldRest.commonSuffixWith(newRest).length
    return newRest.substring(0, newRest.length - suffix)
}

private const val IGNORED_CHARACTERS =
    "-.\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\uFF0D\uFF0E" +
        "\u061C\u200B\u200C\u200D\u200E\u200F\u202A\u202B\u202C\u202D\u202E" +
        "\u2060\u2066\u2067\u2068\u2069\uFEFF"

/** Returns the ASCII digits in [text], or null if [text] contains an unsupported character. */
internal fun normalizeOtpDigits(text: String): String? = normalizeOtpInput(text, OtpCharset.NUMERIC)

/**
 * Returns the characters of [charset] in [text], with letters in uppercase ASCII, or null if [text]
 * contains an unsupported character. Letters are matched by explicit ranges, not by Unicode case
 * mapping, which would turn characters such as `ı` or `ſ` into ASCII letters.
 */
internal fun normalizeOtpInput(
    text: String,
    charset: OtpCharset,
): String? {
    val letters = charset == OtpCharset.ALPHANUMERIC
    val result = StringBuilder(text.length)
    for (char in text) {
        val digit = char.digitToIntOrNull()
        when {
            digit != null -> result.append('0' + digit)
            letters && char in 'A'..'Z' -> result.append(char)
            letters && char in 'a'..'z' -> result.append('A' + (char - 'a'))
            letters && char in '\uFF21'..'\uFF3A' -> result.append('A' + (char - '\uFF21'))
            letters && char in '\uFF41'..'\uFF5A' -> result.append('A' + (char - '\uFF41'))
            char.isWhitespace() || char in IGNORED_CHARACTERS -> Unit
            else -> return null
        }
    }
    return result.toString()
}

private fun Char.isOtpChar(charset: OtpCharset): Boolean =
    this in '0'..'9' || (charset == OtpCharset.ALPHANUMERIC && this in 'A'..'Z')
