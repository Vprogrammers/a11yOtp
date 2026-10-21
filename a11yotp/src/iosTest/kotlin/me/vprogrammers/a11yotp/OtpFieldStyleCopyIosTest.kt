package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class OtpFieldStyleCopyIosTest {
    @Test
    fun copiesChangeOnlyTheNamedValues() {
        val base = OtpFieldDefaults.style()
        val round =
            base
                .withGroupSize(3)
                .withCornerRadius(1_000.0)
                .withMinCellSize(width = 50.0, height = 50.0)

        assertEquals(3, round.groupSize)
        assertEquals(1_000.0, round.cornerRadius)
        assertEquals(50.0, round.minCellWidth)
        assertEquals(50.0, round.minCellHeight)
        assertEquals(base.cellSpacing, round.cellSpacing)
        assertEquals(base.focusedBorderWidth, round.focusedBorderWidth)
        assertSame(base.colors, round.colors)
    }

    @Test
    fun copiedCellSizesKeepTheMinimumTouchTarget() {
        val small = OtpFieldDefaults.style().withMinCellSize(width = 10.0, height = 20.0)

        assertEquals(44.0, small.minCellWidth)
        assertEquals(44.0, small.minCellHeight)
    }

    @Test
    fun glassIsOffByDefaultAndCopiesKeepTheRestOfTheButtonStyle() {
        val base = OtpButtonDefaults.style()
        val glass = base.withGlass(true)

        assertEquals(false, base.glass)
        assertEquals(true, glass.glass)
        assertEquals(base.minHeight, glass.minHeight)
        assertEquals(base.cornerRadius, glass.cornerRadius)
        assertSame(base.colors, glass.colors)
        assertEquals(false, glass.withGlass(false).glass)
    }

    @Test
    fun fieldGlassIsOffByDefaultAndKeepsTheCellStyle() {
        val base = OtpFieldDefaults.style().withGroupSize(3)
        val glass = base.withGlass(true)

        assertEquals(false, base.glass)
        assertEquals(true, glass.glass)
        assertEquals(3, glass.groupSize)
        assertSame(base.colors, glass.colors)
    }

    @Test
    fun focusLiftIsOnByDefaultAndCanBeTurnedOff() {
        val base = OtpFieldDefaults.style()

        assertEquals(true, base.liftFocusedCell)
        assertEquals(false, base.withFocusLift(false).liftFocusedCell)
        assertEquals(base.minCellHeight, base.withFocusLift(false).minCellHeight)
    }
}
