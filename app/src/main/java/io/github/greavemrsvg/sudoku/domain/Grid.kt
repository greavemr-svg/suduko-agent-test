package io.github.greavemrsvg.sudoku.domain

/** Immutable 9x9 grid in row-major order: index = row * 9 + column. 0 is empty, 1..9 is a digit. */
class Grid(
    cells: IntArray,
) {
    private val cells: IntArray = cells.copyOf()

    init {
        require(cells.size == CELL_COUNT) { "A grid has $CELL_COUNT cells, got ${cells.size}" }
        require(cells.all { it in EMPTY..GRID_SIZE }) { "Cell values must be in $EMPTY..$GRID_SIZE" }
    }

    val filledCount: Int get() = cells.count { it != EMPTY }

    fun get(index: Int): Int {
        requireIndex(index)
        return cells[index]
    }

    fun with(
        index: Int,
        digit: Int,
    ): Grid {
        requireIndex(index)
        require(digit in EMPTY..GRID_SIZE) { "Digit must be in $EMPTY..$GRID_SIZE, got $digit" }
        val copy = cells.copyOf()
        copy[index] = digit
        return Grid(copy)
    }

    /** True when every row, column and box contains each of 1..9 exactly once. */
    fun isValidSolution(): Boolean {
        if (cells.any { it == EMPTY }) return false
        val rows = IntArray(GRID_SIZE)
        val cols = IntArray(GRID_SIZE)
        val boxes = IntArray(GRID_SIZE)
        for (i in 0 until CELL_COUNT) {
            val row = i / GRID_SIZE
            val col = i % GRID_SIZE
            val box = (row / BOX_SIZE) * BOX_SIZE + col / BOX_SIZE
            val bit = 1 shl cells[i]
            if ((rows[row] and bit) != 0 || (cols[col] and bit) != 0 || (boxes[box] and bit) != 0) return false
            rows[row] = rows[row] or bit
            cols[col] = cols[col] or bit
            boxes[box] = boxes[box] or bit
        }
        return true
    }

    override fun equals(other: Any?): Boolean = other is Grid && cells.contentEquals(other.cells)

    override fun hashCode(): Int = cells.contentHashCode()

    override fun toString(): String = cells.joinToString("")

    private fun requireIndex(index: Int) {
        require(index in 0 until CELL_COUNT) { "Cell index must be in 0 until $CELL_COUNT, got $index" }
    }
}
