package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OtpAlphanumericTest {
    private val six = OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC)

    @Test
    fun theDefaultConstructorIsNumeric() {
        assertEquals(OtpCharset.NUMERIC, OtpState(OtpLength.SIX).charset)
        assertEquals(OtpState(OtpLength.SIX), OtpState(OtpLength.SIX, OtpCharset.NUMERIC))
    }

    @Test
    fun charsetTakesPartInEqualityAndHashCode() {
        val numeric = OtpState(OtpLength.SIX)
        assertNotEquals(numeric, six)
        assertNotEquals(numeric.hashCode(), six.hashCode())
        assertEquals(six, OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC))
        assertEquals(six.hashCode(), OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC).hashCode())
    }

    @Test
    fun toStringShowsTheCharsetButNeverTheCode() {
        val text = six.typed("K7Q2").toString()
        assertTrue("ALPHANUMERIC" in text, text)
        assertFalse("K7Q2" in text, text)
        assertFalse("K7" in text, text)
    }

    @Test
    fun clearedAndErrorTransitionsKeepTheCharset() {
        val state = six.typed("K7Q")
        assertEquals(OtpCharset.ALPHANUMERIC, state.cleared().charset)
        assertEquals(OtpCharset.ALPHANUMERIC, state.withError().charset)
        assertEquals(OtpCharset.ALPHANUMERIC, state.withError().withoutError().charset)
        assertEquals(state, state.withError().withoutError())
        assertEquals("", state.cleared().code)
    }

    @Test
    fun typingStoresUppercaseAndCompletesOnce() {
        var state = six
        var completions = 0
        for (char in "k7q2mx") {
            val result = state.withInput(state.code + char)
            if (result.didComplete) completions++
            state = result.state
        }
        assertEquals("K7Q2MX", state.code)
        assertEquals(1, completions)
        assertTrue(state.isComplete)
    }

    @Test
    fun retypingTheStoredCodeInLowercaseIsANoOp() {
        val state = six.typed("K7Q2MX")
        val result = state.withInput("k7q2mx")
        assertEquals(state, result.state)
        assertFalse(result.didComplete)
        assertNull(result.rejection)
    }

    @Test
    fun deletionKeepsLetters() {
        val state = six.typed("K7Q2")
        assertEquals("K7Q", state.withInput("k7q").state.code)
    }

    @Test
    fun pasteOfExactLengthReplacesAndCompletes() {
        val result = six.typed("AB").withInput("k7-q2 mx")
        assertEquals("K7Q2MX", result.state.code)
        assertTrue(result.didComplete)
        assertNull(result.rejection)
    }

    @Test
    fun pasteAppendedToTheCurrentCodeReplacesIt() {
        val result = six.typed("AB").withInput("ABk7q2mx")
        assertEquals("K7Q2MX", result.state.code)
        assertTrue(result.didComplete)
    }

    @Test
    fun aSentenceIsRejectedAsAWholeWithCounts() {
        val result = six.withInput("Your code is K7Q2MX")
        assertEquals(six, result.state)
        assertFalse(result.didComplete)
        assertEquals(OtpInputRejection(6, 16), result.rejection)
    }

    @Test
    fun partialAndOverlongPasteAreRejectedWithoutChangingTheCode() {
        val state = six.typed("K7")
        assertEquals(OtpInputRejection(6, 3), state.withInput("K7ABC").rejection)
        val long = six.withInput("K7Q2MXZ")
        assertEquals(OtpInputRejection(6, 7), long.rejection)
        assertEquals("", long.state.code)
    }

    @Test
    fun aTypedUnsupportedCharacterIsIgnored() {
        val state = six.typed("K7")
        val result = state.withInput("K7!")
        assertEquals(state, result.state)
        assertNull(result.rejection)
        assertFalse(result.didComplete)
    }

    @Test
    fun editingClearsTheError() {
        val state = six.typed("K7Q2MX").withError()
        assertFalse(state.withInput("k7q2m").state.isError)
    }

    @Test
    fun numericStateStillRejectsLetters() {
        val numeric = OtpState(OtpLength.SIX).typed("12")
        val result = numeric.withInput("12a")
        assertEquals(numeric, result.state)
        assertNull(result.rejection)
        assertEquals("12", numeric.typed("a3").code.take(2))
    }

    @Test
    fun anAlphanumericStateKeepsItsCharsetThroughInput() {
        assertEquals(OtpCharset.ALPHANUMERIC, six.withInput("K").state.charset)
    }

    @Test
    fun lookAlikeCharactersAreNeverMerged() {
        assertEquals("O0I1", six.typed("o0i1").code)
    }

    @Test
    fun restoreAcceptsDigitsAndUppercaseOnly() {
        val restored = OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "K7Q", isError = true)!!
        assertEquals("K7Q", restored.code)
        assertTrue(restored.isError)
        assertEquals(OtpCharset.ALPHANUMERIC, restored.charset)
        assertNull(OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "k7q", false))
        assertNull(OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "K7-", false))
        assertNull(OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "K7Q2MXZ", false))
        assertNull(OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "Ａ", false))
        assertNull(OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "٣", false))
        assertEquals("", OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "", false)!!.code)
    }

    @Test
    fun theOldRestoreOverloadIsNumeric() {
        assertNull(OtpState.restore(OtpLength.SIX, "K7Q", false))
        assertEquals(OtpCharset.NUMERIC, OtpState.restore(OtpLength.SIX, "123", false)!!.charset)
    }

    @Test
    fun numericCharsetRestoreRejectsLetters() {
        assertNull(OtpState.restore(OtpLength.SIX, OtpCharset.NUMERIC, "K7Q", false))
    }

    @Test
    fun everyReachableAlphanumericStateRoundTrips() {
        for (length in OtpLength.entries) {
            val empty = OtpState(length, OtpCharset.ALPHANUMERIC)
            val partial = empty.typed("K7")
            val complete = empty.typed("K7Q2MXZ9AB".take(length.digitCount))
            for (state in listOf(empty, partial, complete, partial.withError(), complete.withError())) {
                assertEquals(state, OtpState.restore(state.length, state.charset, state.code, state.isError), "$state")
            }
        }
    }

    @Test
    fun normalizationMapsLettersDigitsAndFullwidthForms() {
        val alnum = OtpCharset.ALPHANUMERIC
        assertEquals("AZ09", normalizeOtpInput("aZ09", alnum))
        assertEquals("ABZ", normalizeOtpInput("ＡｂＺ", alnum))
        assertEquals("AZ", normalizeOtpInput("ａｚ", alnum))
        assertEquals("12", normalizeOtpInput("１٢", alnum))
        assertEquals("K7Q2", normalizeOtpInput(" k7-q‏2.", alnum))
        assertEquals("", normalizeOtpInput("", alnum))
    }

    @Test
    fun normalizationRejectsLookalikesAndSymbols() {
        val rejected =
            listOf(
                "ı", // dotless i
                "ſ", // long s
                "K", // Kelvin sign
                "é",
                "А", // Cyrillic A
                "Ο", // Greek Omicron
                "́", // combining acute
                "𝐀", // math bold A (surrogates)
                ":",
                "_",
                "［", // just after fullwidth Z
                "｛", // just after fullwidth z
                "＠", // just before fullwidth A
                "@",
                "[",
                "`",
                "{",
            )
        for (text in rejected) {
            assertNull(normalizeOtpInput("K$text", OtpCharset.ALPHANUMERIC), text.map { it.code }.toString())
        }
    }

    @Test
    fun numericNormalizationRejectsEveryLetterForm() {
        for (text in listOf("a", "A", "Ａ", "ａ", "1a")) {
            assertNull(normalizeOtpInput(text, OtpCharset.NUMERIC), text)
        }
        assertEquals("12", normalizeOtpInput("1 2", OtpCharset.NUMERIC))
    }
}
