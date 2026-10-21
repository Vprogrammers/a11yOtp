import SwiftUI

/// Lays out `count` cell subviews followed by one separator subview per group boundary.
/// Wraps by group when a row does not fit, then by cell, and shrinks cells only as a last resort.
struct OtpCellsLayout: Layout {
    let count: Int
    let groupSize: Int
    let cellWidth: CGFloat
    let spacing: CGFloat
    let rowSpacing: CGFloat
    let separatorWidth: CGFloat
    /// 0 aligns rows to the physical left of the layout, 1 to the right.
    let fraction: CGFloat

    private struct Plan {
        var rows: [Range<Int>]
        var cellWidth: CGFloat
        var showsSeparators: Bool
    }

    private var hasSeparators: Bool { groupSize < count }
    private var groupGap: CGFloat { 2 * spacing + separatorWidth }

    private func gap(after cell: Int, separators: Bool) -> CGFloat {
        separators && (cell + 1) % groupSize == 0 ? groupGap : spacing
    }

    private func rowWidth(_ row: Range<Int>, cellWidth: CGFloat, separators: Bool) -> CGFloat {
        var width = CGFloat(row.count) * cellWidth
        for cell in row.dropLast() {
            width += gap(after: cell, separators: separators)
        }
        return width
    }

    private func plan(width proposed: CGFloat?) -> Plan {
        guard let available = proposed, available.isFinite else {
            return Plan(rows: [0..<count], cellWidth: cellWidth, showsSeparators: hasSeparators)
        }
        let cell = max(min(cellWidth, available), 1)
        let groups = stride(from: 0, to: count, by: groupSize).map { $0..<min($0 + groupSize, count) }
        if groups.allSatisfy({ rowWidth($0, cellWidth: cell, separators: true) <= available }) {
            var rows: [Range<Int>] = []
            var current = groups[0]
            for group in groups.dropFirst() {
                let merged = current.lowerBound..<group.upperBound
                if rowWidth(merged, cellWidth: cell, separators: true) <= available {
                    current = merged
                } else {
                    rows.append(current)
                    current = group
                }
            }
            rows.append(current)
            return Plan(rows: rows, cellWidth: cell, showsSeparators: hasSeparators)
        }
        let perRowMax = max(Int((available + spacing) / (cell + spacing)), 1)
        let rowCount = (count + perRowMax - 1) / perRowMax
        let perRow = (count + rowCount - 1) / rowCount
        let rows = stride(from: 0, to: count, by: perRow).map { $0..<min($0 + perRow, count) }
        return Plan(rows: rows, cellWidth: cell, showsSeparators: false)
    }

    private func cellHeight(_ subviews: Subviews, cellWidth: CGFloat) -> CGFloat {
        subviews.prefix(count)
            .map { $0.sizeThatFits(ProposedViewSize(width: cellWidth, height: nil)).height }
            .max() ?? 0
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache _: inout ()) -> CGSize {
        let plan = plan(width: proposal.width)
        let width = plan.rows
            .map { rowWidth($0, cellWidth: plan.cellWidth, separators: plan.showsSeparators) }
            .max() ?? 0
        let height = cellHeight(subviews, cellWidth: plan.cellWidth)
        return CGSize(width: width, height: CGFloat(plan.rows.count) * height
            + CGFloat(max(plan.rows.count - 1, 0)) * rowSpacing)
    }

    func placeSubviews(in bounds: CGRect, proposal _: ProposedViewSize, subviews: Subviews, cache _: inout ()) {
        let plan = plan(width: bounds.width)
        let height = cellHeight(subviews, cellWidth: plan.cellWidth)
        let separatorCount = max(subviews.count - count, 0)
        var shown = Set<Int>()
        for (rowIndex, row) in plan.rows.enumerated() {
            let width = rowWidth(row, cellWidth: plan.cellWidth, separators: plan.showsSeparators)
            var originX = bounds.minX + (bounds.width - width) * fraction
            let originY = bounds.minY + CGFloat(rowIndex) * (height + rowSpacing)
            for cell in row {
                subviews[cell].place(
                    at: CGPoint(x: originX, y: originY),
                    anchor: .topLeading,
                    proposal: ProposedViewSize(width: plan.cellWidth, height: height)
                )
                originX += plan.cellWidth
                if plan.showsSeparators, cell != row.upperBound - 1, (cell + 1) % groupSize == 0 {
                    let index = (cell + 1) / groupSize - 1
                    if index < separatorCount {
                        shown.insert(index)
                        subviews[count + index].place(
                            at: CGPoint(x: originX + spacing + separatorWidth / 2, y: originY + height / 2),
                            anchor: .center,
                            proposal: ProposedViewSize(width: separatorWidth, height: height)
                        )
                    }
                }
                originX += gap(after: cell, separators: plan.showsSeparators)
            }
        }
        for index in 0..<separatorCount where !shown.contains(index) {
            subviews[count + index].place(at: bounds.origin, anchor: .topLeading, proposal: .zero)
        }
    }
}
