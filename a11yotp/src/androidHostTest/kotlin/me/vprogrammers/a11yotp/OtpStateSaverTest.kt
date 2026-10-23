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
