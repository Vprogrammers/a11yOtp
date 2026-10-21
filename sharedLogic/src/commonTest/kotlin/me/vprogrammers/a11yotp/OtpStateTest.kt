package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OtpStateTest {
    @Test
    fun emptyStateForEachSupportedLength() {
        for ((length, digitCount) in listOf(OtpLength.FOUR to 4, OtpLength.SIX to 6)) {
            val state = OtpState(length)

            assertEquals(digitCount, state.length.digitCount)
            assertEquals("", state.code)
            assertEquals(0, state.enteredDigitCount)
            assertFalse(state.isComplete)
            assertFalse(state.isError)
        }
    }

    @Test
    fun typingDigitByDigitCompletesOnlyOnLastDigit() {
        var state = OtpState(OtpLength.FOUR)
        val completions = mutableListOf<Boolean>()

        for (text in listOf("1", "12", "123", "1234")) {
            val result = state.withInput(text)
            state = result.state
            completions += result.didComplete
        }

        assertEquals("1234", state.code)
        assertTrue(state.isComplete)
        assertEquals(listOf(false, false, false, true), completions)
    }

    @Test
    fun normalizesPastedInput() {
        val cases =
            listOf(
                "482913" to "482913",
                "482 913" to "482913",
                " 482-913 " to "482913",
                "48.29.13" to "482913",
                "482\u00A0913" to "482913",
                "482\u2013913" to "482913",
                "\u200E482913\u200F" to "482913",
                "\u061C\u0664\u0668\u0662\u061C\u0669\u0661\u0663" to "482913",
                "482\u00AD913" to "482913",
                "\uFF14\uFF18\uFF12\uFF0D\uFF19\uFF11\uFF0E\uFF13" to "482913",
                "\u0664\u0668\u0662\u0669\u0661\u0663" to "482913",
                "\u06F4\u06F8\u06F2\u06F9\u06F1\u06F3" to "482913",
                "\u096A\u096E\u0968\u096F\u0967\u0969" to "482913",
                "\uFF14\uFF18\uFF12\uFF19\uFF11\uFF13" to "482913",
                "48" to "48",
                "" to "",
                " - " to "",
            )

        for ((input, expected) in cases) {
            assertEquals(expected, OtpState(OtpLength.SIX).withInput(input).state.code, "input: $input")
        }
    }

    @Test
    fun rejectsUnsupportedCharactersWithoutChangingState() {
        val state = OtpState(OtpLength.SIX).withInput("12").state.withError()

        for (input in listOf("12a", "123x56", "12+3", "code 123456", "12\u00B2", "\uD835\uDFCF")) {
            val result = state.withInput(input)

            assertSame(state, result.state, "input: $input")
            assertFalse(result.didComplete)
        }
    }

    @Test
    fun overlongInputKeepsFirstDigits() {
        val result = OtpState(OtpLength.FOUR).withInput("12-34-56")

        assertEquals("1234", result.state.code)
        assertTrue(result.didComplete)
    }

    @Test
    fun deletionAndReplacement() {
        val complete = OtpState(OtpLength.FOUR).withInput("1234").state

        val deleted = complete.withInput("124")
        assertEquals("124", deleted.state.code)
        assertFalse(deleted.state.isComplete)
        assertFalse(deleted.didComplete)

        val replaced = complete.withInput("9876")
        assertEquals("9876", replaced.state.code)
        assertFalse(replaced.didComplete)

        assertEquals("", complete.withInput("").state.code)
    }

    @Test
    fun completionFiresOncePerIncompleteToCompleteTransition() {
        var state = OtpState(OtpLength.FOUR)
        val completions = mutableListOf<Boolean>()

        for (text in listOf("1234", "1234", "12345", "5678", "567", "5679", "")) {
            val result = state.withInput(text)
            state = result.state
            completions += result.didComplete
        }

        assertEquals(listOf(true, false, false, false, false, true, false), completions)
    }

    @Test
    fun pastingNewFullCodeOverRejectedCodeDoesNotCompleteUntilCleared() {
        val rejected = OtpState(OtpLength.FOUR).withInput("1234").state.withError()

        val replaced = rejected.withInput("5678")
        assertEquals("5678", replaced.state.code)
        assertFalse(replaced.state.isError)
        assertFalse(replaced.didComplete)

        assertTrue(rejected.cleared().withInput("5678").didComplete)
    }

    @Test
    fun errorIsClearedWhenCodeChanges() {
        val rejected = OtpState(OtpLength.FOUR).withInput("1234").state.withError()
        assertTrue(rejected.isError)

        assertTrue(rejected.withInput("1234").state.isError)
        assertTrue(rejected.withInput("12 34").state.isError)
        assertFalse(rejected.withInput("123").state.isError)
    }

    @Test
    fun errorCanBeSetAndClearedExplicitly() {
        val state = OtpState(OtpLength.SIX).withInput("123456").state

        val rejected = state.withError()
        assertTrue(rejected.isError)
        assertEquals("123456", rejected.code)
        assertSame(rejected, rejected.withError())

        val cleared = rejected.withoutError()
        assertFalse(cleared.isError)
        assertEquals("123456", cleared.code)
        assertSame(state, state.withoutError())
    }

    @Test
    fun clearedResetsCodeAndError() {
        val state = OtpState(OtpLength.SIX).withInput("123456").state.withError()

        val cleared = state.cleared()

        assertEquals(OtpState(OtpLength.SIX), cleared)
        assertTrue(cleared.withInput("123456").didComplete)
    }

    @Test
    fun equalityUsesLengthCodeAndError() {
        val a = OtpState(OtpLength.SIX).withInput("12").state
        val b = OtpState(OtpLength.SIX).withInput("1-2").state

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == a.withError())
        assertFalse(OtpState(OtpLength.FOUR) == OtpState(OtpLength.SIX))
    }

    @Test
    fun toStringDoesNotExposeDigits() {
        val text = OtpState(OtpLength.SIX).withInput("482913").state.toString()

        assertFalse(text.contains("482913"))
        assertTrue(text.contains("enteredDigitCount=6"))
    }
}
