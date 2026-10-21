package me.vprogrammers.a11yotp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtpCellsPlannerTest {
    private fun planner(
        count: Int = 6,
        groupSize: Int = 0,
    ) = OtpCellsPlanner(count, groupSize, cellWidth = 48.0, spacing = 8.0, separatorWidth = 12.0)

    @Test
    fun unconstrainedWidthUsesOneRow() {
        val plan = planner(groupSize = 3).plan(null)

        assertEquals(1, plan.rows.size)
        assertTrue(plan.showsSeparators)
    }

    @Test
    fun groupSizeIsClampedAndZeroMeansNoGrouping() {
        assertEquals(6, planner(groupSize = 0).groupSize)
        assertEquals(6, planner(groupSize = 9).groupSize)
        assertEquals(0, planner(groupSize = 0).separatorCount)
        assertEquals(1, planner(groupSize = 3).separatorCount)
        assertEquals(3, planner(count = 8, groupSize = 2).separatorCount)
    }

    @Test
    fun wideSpaceKeepsAllCellsAndSeparatorsInOneRow() {
        val plan = planner(groupSize = 3).plan(400.0)

        assertEquals(listOf(6), plan.rows.map { it.count })
        assertTrue(plan.showsSeparators)
        assertEquals(48.0, plan.cellWidth)
    }

    @Test
    fun separatorsAreDroppedWhenThatKeepsTheCodeOnOneRow() {
        // With separators the row needs 6 * 48 + 4 * 8 + 28 = 348; without them 6 * 48 + 5 * 8 = 328.
        val plan = planner(groupSize = 3).plan(340.0)

        assertEquals(listOf(6), plan.rows.map { it.count })
        assertFalse(plan.showsSeparators)
        assertEquals(48.0, plan.cellWidth)
    }

    @Test
    fun narrowSpaceWrapsWholeGroupsFirst() {
        // One group is 3 * 48 + 2 * 8 = 160; both groups plus a separator gap need 160 * 2 + 28 = 348.
        val plan = planner(groupSize = 3).plan(200.0)

        assertEquals(listOf(3, 3), plan.rows.map { it.count })
        assertTrue(plan.showsSeparators)
    }

    @Test
    fun veryNarrowSpaceWrapsCellsWithoutSeparators() {
        val plan = planner(groupSize = 3).plan(120.0)

        assertEquals(6, plan.rows.sumOf { it.count })
        assertTrue(plan.rows.size > 2)
        assertFalse(plan.showsSeparators)
        assertTrue(plan.rows.all { planner(groupSize = 3).rowWidth(it, plan.cellWidth, false) <= 120.0 })
    }

    @Test
    fun cellsShrinkOnlyWhenEvenOneCellDoesNotFit() {
        val plan = planner().plan(30.0)

        assertEquals(30.0, plan.cellWidth)
    }
}
