package io.github.greavemrsvg.sudoku.domain

data class Puzzle(
    val givens: Grid,
    val solution: Grid,
) {
    init {
        require(solution.isValidSolution()) { "Solution is not a valid filled grid" }
        for (i in 0 until CELL_COUNT) {
            val given = givens.get(i)
            require(given == EMPTY || given == solution.get(i)) { "Given at $i differs from the solution" }
        }
    }

    fun isGiven(index: Int): Boolean = givens.get(index) != EMPTY
}
