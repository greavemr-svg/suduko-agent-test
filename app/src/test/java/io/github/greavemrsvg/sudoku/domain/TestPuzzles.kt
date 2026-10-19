package io.github.greavemrsvg.sudoku.domain

object TestPuzzles {
    /** A valid filled grid built from the standard shifting pattern. */
    val solution: Grid =
        Grid(IntArray(CELL_COUNT) { i -> ((i / 9) * 3 + (i / 9) / 3 + i % 9) % 9 + 1 })

    /** Cells with an even index are givens; the rest are empty. */
    val puzzle: Puzzle =
        Puzzle(Grid(IntArray(CELL_COUNT) { i -> if (i % 2 == 0) solution.get(i) else EMPTY }), solution)

    const val GIVEN = 0
    const val OPEN = 1

    fun wrong(index: Int): Int = solution.get(index) % 9 + 1
}
