package me.vprogrammers.a11yotp

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OtpStateSaverTest {
    private val scope = SaverScope { true }

    private fun save(state: OtpState): Any = with(OtpStateSaver) { scope.save(state) }!!

    private fun restore(saved: Any): OtpState? = OtpStateSaver.restore(saved)

    @Test
    fun partialCompleteAndErrorStatesRoundTrip() {
        val partial = OtpState.restore(OtpLength.SIX, "123", isError = false)!!
        val complete = OtpState.restore(OtpLength.EIGHT, "12345678", isError = false)!!
        val error = OtpState.restore(OtpLength.FOUR, "1234", isError = true)!!

        for (state in listOf(OtpState(OtpLength.SIX), partial, complete, error)) {
            assertEquals(state, restore(save(state)))
        }
    }

    @Test
    fun theSavedFormHoldsOnlyBundleSafeValues() {
        val saved = save(OtpState.restore(OtpLength.SIX, "12", isError = true)!!)

        assertEquals(listOf("SIX", "12", true), saved)
    }

    @Test
    fun unrestorableDataIsDroppedSoTheInitialValueIsUsed() {
        assertNull(restore(listOf("SEVEN", "12", false)))
        assertNull(restore(listOf("SIX", "12a", false)))
        assertNull(restore(listOf("SIX", "1234567", false)))
        assertNull(restore(listOf("SIX", 12, false)))
        assertNull(restore(listOf("SIX")))
    }
}

class OtpStateSaverAlphanumericTest {
    private val scope = SaverScope { true }

    private fun save(state: OtpState): Any = with(OtpStateSaver) { scope.save(state) }!!

    private fun restore(saved: Any): OtpState? = OtpStateSaver.restore(saved)

    @Test
    fun numericStatesAreSavedInTheUnchanged0_1_0Format() {
        assertEquals(listOf("SIX", "12", true), save(OtpState.restore(OtpLength.SIX, "12", isError = true)!!))
        assertEquals(listOf("FOUR", "", false), save(OtpState(OtpLength.FOUR, OtpCharset.NUMERIC)))
    }

    @Test
    fun alphanumericStatesSaveTheCharsetAsAFourthEntry() {
        val state = OtpState.restore(OtpLength.SIX, OtpCharset.ALPHANUMERIC, "K7Q", isError = false)!!
        assertEquals(listOf("SIX", "K7Q", false, "ALPHANUMERIC"), save(state))
    }

    @Test
    fun alphanumericStatesRoundTrip() {
        val partial = OtpState(OtpLength.EIGHT, OtpCharset.ALPHANUMERIC).withInput("k7q").state
        val complete = OtpState(OtpLength.SIX, OtpCharset.ALPHANUMERIC).withInput("K7Q2MX").state
        val empty = OtpState(OtpLength.FOUR, OtpCharset.ALPHANUMERIC)
        for (state in listOf(empty, partial, complete, complete.withError(), empty.withError())) {
            assertEquals(state, restore(save(state)))
        }
    }

    @Test
    fun aLegacyThreeEntryListRestoresAsNumeric() {
        val restored = restore(listOf("SIX", "123", false))!!
        assertEquals(OtpCharset.NUMERIC, restored.charset)
        assertEquals("123", restored.code)
    }

    @Test
    fun invalidCharsetDataIsDropped() {
        assertNull(restore(listOf("SIX", "K7", false, "HEX")))
        assertNull(restore(listOf("SIX", "K7", false, "alphanumeric")))
        assertNull(restore(listOf("SIX", "K7", false, 4)))
        assertNull(restore(listOf("SIX", "K7", false, null)))
    }

    @Test
    fun invalidCodesAreDropped() {
        assertNull(restore(listOf("SIX", "k7q", false, "ALPHANUMERIC")))
        assertNull(restore(listOf("SIX", "K7Q2MXZ", false, "ALPHANUMERIC")))
        assertNull(restore(listOf("SIX", "K7-", false, "ALPHANUMERIC")))
        assertNull(restore(listOf("SIX", "K7Q", false)))
        assertNull(restore(listOf("SIX", "K7Q", false, "NUMERIC")))
    }

    @Test
    fun aNumericCodeUnderTheAlphanumericCharsetRestores() {
        assertEquals("123", restore(listOf("SIX", "123", false, "ALPHANUMERIC"))!!.code)
    }
}
