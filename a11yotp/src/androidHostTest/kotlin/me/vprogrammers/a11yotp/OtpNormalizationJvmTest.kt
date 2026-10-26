package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals

class OtpNormalizationJvmTest {
    @Test
    fun everyJvmDecimalDigitNormalizesToAsciiDigit() {
        for (codePoint in 0..0xFFFF) {
            val char = codePoint.toChar()
            if (!Character.isDigit(char)) continue

            assertEquals(
                Character.digit(char, 10).toString(),
                normalizeOtpDigits(char.toString()),
                "U+${codePoint.toString(16).uppercase()}",
            )
        }
    }

    @Test
    fun jvmLettersAndSymbolsAreRejected() {
        for (codePoint in 0..0xFFFF) {
            val char = codePoint.toChar()
            if (Character.isLetter(char) || Character.getType(char) == Character.OTHER_NUMBER.toInt()) {
                assertEquals(null, normalizeOtpDigits(char.toString()), "U+${codePoint.toString(16).uppercase()}")
            }
        }
    }
}

class OtpAlphanumericNormalizationJvmTest {
    private fun hex(char: Char) = "U+${char.code.toString(16).uppercase()}"

    @Test
    fun onlyAsciiAndFullwidthLettersAndDecimalDigitsAreAccepted() {
        for (codePoint in 0..0xFFFF) {
            val char = codePoint.toChar()
            val result = normalizeOtpInput(char.toString(), OtpCharset.ALPHANUMERIC)
            val expected: String? =
                when {
                    char in 'a'..'z' || char in 'A'..'Z' -> char.uppercaseChar().toString()
                    char in '\uFF21'..'\uFF3A' -> ('A' + (char - '\uFF21')).toString()
                    char in '\uFF41'..'\uFF5A' -> ('A' + (char - '\uFF41')).toString()
                    Character.isDigit(char) -> Character.digit(char, 10).toString()
                    Character.isLetter(char) -> null
                    else -> continue
                }
            assertEquals(expected, result, hex(char))
        }
    }

    @Test
    fun numericNormalizationMatchesTheDigitOnlyRules() {
        for (codePoint in 0..0xFFFF) {
            val char = codePoint.toChar()
            assertEquals(
                normalizeOtpDigits(char.toString()),
                normalizeOtpInput(char.toString(), OtpCharset.NUMERIC),
                hex(char),
            )
        }
    }

    @Test
    fun noLetterMapsToAnotherLetterThanItsOwn() {
        // Locale-independent: dotless i, long s and Kelvin must not become I, S, K.
        assertEquals(null, normalizeOtpInput("ıſK", OtpCharset.ALPHANUMERIC))
    }
}
