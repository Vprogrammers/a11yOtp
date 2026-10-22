package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OtpInputRejectionTest {
    private fun digits(
        count: Int,
        from: Char = '1',
    ): String = CharArray(count) { '0' + ((from - '0' + it) % 10) }.concatToString()

    private enum class Fill { EMPTY, PARTIAL, FULL }

    /** The state before the input: empty, 2 typed digits, or a complete code. */
    private fun start(
        length: OtpLength,
        fill: Fill,
    ): OtpState =
        when (fill) {
            Fill.EMPTY -> OtpState(length)
            Fill.PARTIAL -> OtpState(length).typed("90")
            Fill.FULL -> OtpState(length).typed(digits(length.digitCount, '9'))
        }

    @Test
    fun multiDigitInsertionMatrix() {
        for (length in OtpLength.entries) {
            val n = length.digitCount
            for (fill in Fill.entries) {
                val before = start(length, fill)
                // Appended at the end, where the cursor is. Digits start at 1 so they differ from the start.
                for (k in listOf(2, n - 1, n, n + 1, n + 2)) {
                    val received = digits(k)
                    val label = "$length $fill k=$k"
                    val result = before.withInput(before.code + received)
                    when {
                        k == n -> {
                            assertEquals(received, result.state.code, label)
                            assertTrue(result.didComplete, label)
                            assertNull(result.rejection, label)
                        }

                        else -> {
                            assertSame(before, result.state, label)
                            assertFalse(result.didComplete, label)
                            assertEquals(OtpInputRejection(n, k), result.rejection, label)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun pasteWithoutAppendingToTheCurrentCodeMatchesToo() {
        // The field text is replaced entirely, for example by select-all and paste.
        for (length in OtpLength.entries) {
            val n = length.digitCount
            for (fill in Fill.entries) {
                val before = start(length, fill)
                for (k in listOf(n - 1, n + 1)) {
                    // Skip the case where the pasted text equals the current code or extends it by one digit.
                    val result = before.withInput(digits(k))
                    if (before.code.isNotEmpty() && digits(k).startsWith(before.code)) continue
                    assertSame(before, result.state, "$length $fill k=$k")
                    assertEquals(OtpInputRejection(n, k), result.rejection, "$length $fill k=$k")
                }
                val exact = before.withInput(digits(n))
                assertEquals(digits(n), exact.state.code, "$length $fill")
                assertTrue(exact.didComplete, "$length $fill")
            }
        }
    }

    @Test
    fun singleExtraDigitOnFullFieldIsIgnoredWithoutRejection() {
        for (length in OtpLength.entries) {
            val full = start(length, Fill.FULL)

            val result = full.withInput(full.code + "7")

            assertSame(full, result.state)
            assertFalse(result.didComplete)
            assertNull(result.rejection)
        }
    }

    @Test
    fun deletionNeverRejects() {
        for (length in OtpLength.entries) {
            val full = start(length, Fill.FULL)

            for (text in listOf(full.code.dropLast(1), full.code.drop(1), "", full.code.removeRange(1, 3))) {
                val result = full.withInput(text)

                assertNull(result.rejection, text)
                assertEquals(text, result.state.code, text)
                assertFalse(result.didComplete, text)
            }
        }
    }

    @Test
    fun pastingTheCurrentFullCodeAgainChangesNothing() {
        for (length in OtpLength.entries) {
            val full = start(length, Fill.FULL).withError()

            for (text in listOf(full.code, full.code + full.code, full.code.chunked(2).joinToString(" "))) {
                val result = full.withInput(text)

                assertSame(full, result.state, text)
                assertTrue(result.state.isError)
                assertFalse(result.didComplete)
                assertNull(result.rejection)
            }
        }
    }

    @Test
    fun separatorsAreNotCountedAsDigits() {
        val empty = OtpState(OtpLength.SIX)

        val accepted = empty.withInput("123 456")
        assertEquals("123456", accepted.state.code)
        assertTrue(accepted.didComplete)

        val rejected = empty.withInput("123 45")
        assertSame(empty, rejected.state)
        assertEquals(OtpInputRejection(6, 5), rejected.rejection)

        assertEquals(OtpInputRejection(6, 7), empty.withInput("123-456-7").rejection)
    }

    @Test
    fun unsupportedCharactersAreIgnoredWithoutRejection() {
        val empty = OtpState(OtpLength.SIX)

        for (text in listOf("123a456", "12345x", "code 1234567")) {
            val result = empty.withInput(text)

            assertSame(empty, result.state, text)
            assertNull(result.rejection, text)
            assertFalse(result.didComplete, text)
        }
    }

    @Test
    fun rejectionKeepsAnExistingError() {
        val errored = OtpState(OtpLength.SIX).typed("123456").withError()

        val result = errored.withInput("123456" + "1234")

        assertSame(errored, result.state)
        assertTrue(result.state.isError)
        assertEquals(OtpInputRejection(6, 4), result.rejection)
        assertFalse(result.didComplete)
    }

    @Test
    fun exactReplacementClearsErrorAndCompletes() {
        val errored = OtpState(OtpLength.SIX).typed("123456").withError()

        val result = errored.withInput("123456" + "654321")

        assertEquals("654321", result.state.code)
        assertFalse(result.state.isError)
        assertTrue(result.didComplete)
        assertNull(result.rejection)
    }

    @Test
    fun rejectionHasValueSemanticsAndNoDigits() {
        val rejection = OtpState(OtpLength.SIX).withInput("48291").rejection

        assertEquals(OtpInputRejection(6, 5), rejection)
        assertEquals(OtpInputRejection(6, 5).hashCode(), rejection.hashCode())
        assertNotEquals(OtpInputRejection(6, 4), rejection)
        assertNotEquals(OtpInputRejection(8, 5), rejection)
        val text = rejection.toString()
        assertEquals("OtpInputRejection(expectedDigitCount=6, receivedDigitCount=5)", text)
        assertFalse(text.contains("48291"))
        assertEquals(6, rejection?.expectedDigitCount)
        assertEquals(5, rejection?.receivedDigitCount)
    }
}
