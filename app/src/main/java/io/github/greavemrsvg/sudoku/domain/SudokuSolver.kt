package io.github.greavemrsvg.sudoku.domain

/**
 * Bitmask backtracking solver. Counts solutions up to a limit, always branching on the
 * empty cell with the fewest candidates.
 */
class SudokuSolver {
    /** Search nodes visited by the most recent [countSolutions] call. */
    var lastSearchNodes: Long = 0
        private set

    /** Search nodes visited by every [countSolutions] call on this instance. */
    var totalSearchNodes: Long = 0
        private set

    /** Returns the number of solutions of [grid], stopping as soon as [limit] is reached. */
    fun countSolutions(
        grid: Grid,
        limit: Int = 2,
    ): Int {
        require(limit >= 1) { "Limit must be at least 1, got $limit" }
        val search = Search(limit)
        val found = if (search.load(grid)) search.run() else 0
        lastSearchNodes = search.nodes
        totalSearchNodes += search.nodes
        return found
    }

    private class Search(
        private val limit: Int,
    ) {
        private val cells = IntArray(CELL_COUNT)
        private val rows = IntArray(GRID_SIZE)
        private val cols = IntArray(GRID_SIZE)
        private val boxes = IntArray(GRID_SIZE)
        private var found = 0
        var nodes = 0L
            private set

        /** Loads the givens; false when two of them conflict. */
        fun load(grid: Grid): Boolean {
            for (i in 0 until CELL_COUNT) {
                val digit = grid.get(i)
                if (digit == EMPTY) continue
                val bit = 1 shl digit
                if ((used(i) and bit) != 0) return false
                place(i, digit)
            }
            return true
        }

        fun run(): Int {
            solve()
            return found
        }

        private fun box(index: Int) = (index / GRID_SIZE / BOX_SIZE) * BOX_SIZE + (index % GRID_SIZE) / BOX_SIZE

        private fun used(index: Int) = rows[index / GRID_SIZE] or cols[index % GRID_SIZE] or boxes[box(index)]

        private fun place(
            index: Int,
            digit: Int,
        ) {
            val bit = 1 shl digit
            cells[index] = digit
            rows[index / GRID_SIZE] = rows[index / GRID_SIZE] or bit
            cols[index % GRID_SIZE] = cols[index % GRID_SIZE] or bit
            boxes[box(index)] = boxes[box(index)] or bit
        }

        private fun unplace(index: Int) {
            val mask = (1 shl cells[index]).inv()
            cells[index] = EMPTY
            rows[index / GRID_SIZE] = rows[index / GRID_SIZE] and mask
            cols[index % GRID_SIZE] = cols[index % GRID_SIZE] and mask
            boxes[box(index)] = boxes[box(index)] and mask
        }

        private fun solve() {
            nodes++
            var best = -1
            var bestCandidates = 0
            var bestCount = GRID_SIZE + 1
            for (i in 0 until CELL_COUNT) {
                if (cells[i] != EMPTY) continue
                val candidates = ALL_DIGITS and used(i).inv()
                val count = Integer.bitCount(candidates)
                if (count < bestCount) {
                    best = i
                    bestCandidates = candidates
                    bestCount = count
                    if (count == 0) return
                }
            }
            if (best == -1) {
                found++
                return
            }
            var remaining = bestCandidates
            while (remaining != 0 && found < limit) {
                val bit = remaining and -remaining
                remaining = remaining xor bit
                place(best, Integer.numberOfTrailingZeros(bit))
                solve()
                unplace(best)
            }
        }
    }

    private companion object {
        /** Bits 1..9 set. */
        const val ALL_DIGITS = 0b11_1111_1110
    }
}
