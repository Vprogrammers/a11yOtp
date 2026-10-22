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
