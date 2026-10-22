package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Cell 48, spacing 8, separator 12: a gap between groups is 2 * 8 + 12 = 28. */
class OtpCellsPlannerGridTest {
    private fun planner(
        count: Int,
        groupSize: Int,
    ) = OtpCellsPlanner(count, groupSize, cellWidth = 48.0, spacing = 8.0, separatorWidth = 12.0)

    private fun rows(
        count: Int,
        groupSize: Int,
        available: Double,
    ) = planner(count, groupSize).plan(available).rows.map { it.count }

    @Test
    fun nonPositiveOrNonFiniteWidthIsTreatedAsUnconstrained() {
        for (available in listOf(null, 0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val plan = planner(6, 3).plan(available)

            assertEquals(listOf(6), plan.rows.map { it.count }, "available: $available")
            assertEquals(48.0, plan.cellWidth)
            assertTrue(plan.showsSeparators)
        }
    }

    @Test
    fun separatorsAreOnlyShownWhenGroupingSplitsTheCode() {
        assertFalse(planner(6, 0).plan(null).showsSeparators)
        assertFalse(planner(6, 6).plan(null).showsSeparators)
        assertFalse(planner(6, 9).plan(null).showsSeparators)
        assertTrue(planner(6, 2).plan(null).showsSeparators)
    }

    @Test
    fun separatorCountForEachLengthAndGroupSize() {
        val cases =
            listOf(
                Triple(4, 0, 0),
                Triple(4, 2, 1),
                Triple(4, 3, 1),
                Triple(4, 4, 0),
                Triple(6, 2, 2),
                Triple(6, 3, 1),
                Triple(6, 4, 1),
                Triple(8, 2, 3),
                Triple(8, 3, 2),
                Triple(8, 4, 1),
            )
        for ((count, groupSize, expected) in cases) {
            assertEquals(expected, planner(count, groupSize).separatorCount, "count=$count group=$groupSize")
        }
    }

    @Test
    fun fourDigitsInPairsFitOrSplitIntoPairs() {
        assertEquals(listOf(4), rows(4, 2, 240.0))
        assertEquals(listOf(2, 2), rows(4, 2, 200.0))
    }

    @Test
    fun eightDigitsInFoursSplitIntoTwoGroups() {
        assertEquals(listOf(8), rows(8, 4, 500.0))
        assertEquals(listOf(4, 4), rows(8, 4, 300.0))
    }

    @Test
    fun eightDigitsInPairsMergeAsManyGroupsAsFit() {
        // Group = 104; two = 236; three = 368; four = 500.
        assertEquals(listOf(8), rows(8, 2, 500.0))
        assertEquals(listOf(6, 2), rows(8, 2, 400.0))
        assertEquals(listOf(4, 4), rows(8, 2, 240.0))
        assertEquals(listOf(2, 2, 2, 2), rows(8, 2, 110.0))
    }

    @Test
    fun unevenGroupsKeepTheShortGroupLast() {
        assertEquals(listOf(4, 2), rows(6, 4, 220.0))
        assertEquals(listOf(3, 3, 2), rows(8, 3, 170.0))
    }

    @Test
    fun groupOfOneKeepsSeparatorsBetweenEveryCell() {
        val narrow = planner(4, 1).plan(100.0)
        assertEquals(listOf(1, 1, 1, 1), narrow.rows.map { it.count })
        assertTrue(narrow.showsSeparators)
        assertEquals(listOf(2, 2), rows(4, 1, 130.0))
    }

    @Test
    fun ungroupedCodeThatDoesNotFitWrapsEvenlyWithoutSeparators() {
        val full = planner(6, 0)
        assertEquals(listOf(6), rows(6, 0, 328.0))

        val plan = full.plan(200.0)

        assertEquals(listOf(3, 3), plan.rows.map { it.count })
        assertFalse(plan.showsSeparators)
        assertEquals(48.0, plan.cellWidth)
    }

    @Test
    fun cellsShrinkToOneColumnWhenNothingElseFits() {
        val plan = planner(8, 4).plan(30.0)

        assertEquals(30.0, plan.cellWidth)
        assertEquals(List(8) { 1 }, plan.rows.map { it.count })
        assertFalse(plan.showsSeparators)
    }

    @Test
    fun everyPlanCoversAllCellsInOrderAndFitsTheWidth() {
        for (count in listOf(4, 6, 8)) {
            for (groupSize in 0..4) {
                val planner = planner(count, groupSize)
                var available = 20.0
                while (available <= 600.0) {
                    val plan = planner.plan(available)
                    var next = 0
                    for (row in plan.rows) {
                        assertEquals(next, row.start, "count=$count group=$groupSize width=$available")
                        assertTrue(row.count > 0)
                        assertTrue(
                            planner.rowWidth(row, plan.cellWidth, plan.showsSeparators) <= available + 1e-9,
                            "count=$count group=$groupSize width=$available row=${row.start}",
                        )
                        next = row.endExclusive
                    }
                    assertEquals(count, next, "count=$count group=$groupSize width=$available")
                    available += 7.0
                }
            }
        }
    }
}
