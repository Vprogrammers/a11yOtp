package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.create
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OtpStateIosTest {
    @OptIn(BetaInteropApi::class)
    @Test
    fun foundationBridgedPasteIsNormalized() {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val pasted = NSString.create(string = "\u200F\u0664\u0668\u0662 \u0669\u0661\u0663") as String

        val result = OtpState(OtpLength.SIX).withInput(pasted)

        assertEquals("482913", result.state.code)
        assertTrue(result.didComplete)
    }

    @Test
    fun enumEntriesKeepDigitCounts() {
        assertEquals(listOf(4, 6), OtpLength.entries.map { it.digitCount })
    }
}
