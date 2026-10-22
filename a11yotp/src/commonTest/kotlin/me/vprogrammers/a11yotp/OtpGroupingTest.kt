package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OtpGroupingTest {
    @Test
    fun equalSplitsKeepTheGroupSize() {
        assertEquals(3, evenGroupSize(3, 6))
        assertEquals(2, evenGroupSize(2, 4))
        assertEquals(4, evenGroupSize(4, 8))
        assertEquals(2, evenGroupSize(2, 8))
    }

    @Test
    fun unevenSplitsDrawNoSeparator() {
        assertNull(evenGroupSize(3, 4))
        assertNull(evenGroupSize(3, 8))
        assertNull(evenGroupSize(4, 6))
    }

    @Test
    fun noGroupingOrSingleGroupDrawsNoSeparator() {
        assertNull(evenGroupSize(null, 6))
        assertNull(evenGroupSize(0, 6))
        assertNull(evenGroupSize(6, 6))
        assertNull(evenGroupSize(8, 6))
    }
}
