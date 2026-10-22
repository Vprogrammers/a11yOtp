package me.vprogrammers.a11yotp

/** Cells `start` until `endExclusive` of one visual row. */
internal class OtpRow(
    val start: Int,
    val endExclusive: Int,
) {
    val count: Int get() = endExclusive - start
}

internal class OtpRowPlan(
    val rows: List<OtpRow>,
    val cellWidth: Double,
    val showsSeparators: Boolean,
)

/**
 * Wraps [count] cells and the separators between groups of [groupSize] into rows. When one row with
 * separators does not fit but one row without them does, the separators are dropped: keeping the
 * code on one line matters more than the decorative grouping. Otherwise rows wrap by group, then by
 * cell without separators, and cells shrink only as a last resort. [groupSize] is clamped to
 * 1..[count].
 */
internal class OtpCellsPlanner(
    private val count: Int,
    groupSize: Int,
    private val cellWidth: Double,
    private val spacing: Double,
    private val separatorWidth: Double,
) {
    val groupSize: Int = if (groupSize <= 0) count else groupSize.coerceIn(1, count)

    val separatorCount: Int get() = if (groupSize < count) (count - 1) / groupSize else 0

    private val hasSeparators: Boolean get() = groupSize < count

    /** Gap after cell [cell] (0-based) when it is not the last of its row. */
    fun gapAfter(
        cell: Int,
        separators: Boolean,
    ): Double = if (separators && (cell + 1) % groupSize == 0) 2 * spacing + separatorWidth else spacing

    fun rowWidth(
        row: OtpRow,
        cellWidth: Double,
        separators: Boolean,
    ): Double {
        var width = row.count * cellWidth
        for (cell in row.start until row.endExclusive - 1) width += gapAfter(cell, separators)
        return width
    }

    /** [available] is the width offered to the cells, or null/non-finite when unconstrained. */
    fun plan(available: Double?): OtpRowPlan {
        if (available == null || !available.isFinite() || available <= 0.0) {
            return OtpRowPlan(listOf(OtpRow(0, count)), cellWidth, hasSeparators)
        }
        val cell = minOf(cellWidth, available).coerceAtLeast(1.0)
        val all = OtpRow(0, count)
        if (rowWidth(all, cell, hasSeparators) <= available) return OtpRowPlan(listOf(all), cell, hasSeparators)
        if (rowWidth(all, cell, false) <= available) return OtpRowPlan(listOf(all), cell, false)
        val groups = (0 until count step groupSize).map { OtpRow(it, minOf(it + groupSize, count)) }
        if (groups.all { rowWidth(it, cell, true) <= available }) {
            val rows = mutableListOf<OtpRow>()
            var current = groups.first()
            for (group in groups.drop(1)) {
                val merged = OtpRow(current.start, group.endExclusive)
                if (rowWidth(merged, cell, true) <= available) {
                    current = merged
                } else {
                    rows += current
                    current = group
                }
            }
            rows += current
            return OtpRowPlan(rows, cell, hasSeparators)
        }
        val perRowMax = ((available + spacing) / (cell + spacing)).toInt().coerceAtLeast(1)
        val rowCount = (count + perRowMax - 1) / perRowMax
        val perRow = (count + rowCount - 1) / rowCount
        val rows = (0 until count step perRow).map { OtpRow(it, minOf(it + perRow, count)) }
        return OtpRowPlan(rows, cell, false)
    }
}
