package me.vprogrammers.a11yotp

/**
 * Visual masking of entered characters. Masking also switches the Android keyboard to a password
 * keyboard, marks the field as a password for screen readers, and blocks copy,
 * so visual and assistive-technology privacy stay consistent.
 * Entries may be added in future versions; handle unknown values with an `else` branch.
 */
sealed interface OtpMask {
    /** Digits are shown. */
    data object None : OtpMask

    /** Each entered digit is drawn as [char]. */
    class Character(
        val char: Char = '*',
    ) : OtpMask {
        override fun equals(other: Any?): Boolean = other is Character && other.char == char

        override fun hashCode(): Int = char.hashCode()

        override fun toString(): String = "Character($char)"
    }

    /** Each entered cell is filled with its current border color and shows no glyph. */
    data object FilledCell : OtpMask
}
