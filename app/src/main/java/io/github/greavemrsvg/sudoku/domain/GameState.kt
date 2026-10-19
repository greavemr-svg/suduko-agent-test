package io.github.greavemrsvg.sudoku.domain

data class GameState(
    val puzzle: Puzzle,
    val entries: Grid,
    val selected: Int? = null,
) {
    init {
        for (i in 0 until CELL_COUNT) {
            require(!puzzle.isGiven(i) || entries.get(i) == EMPTY) { "Entry stored on given cell $i" }
        }
        require(selected == null || selected in 0 until CELL_COUNT) { "Selected index out of range: $selected" }
    }

    /** The given at the index, or else the player's entry. */
    fun digitAt(index: Int): Int = if (puzzle.isGiven(index)) puzzle.givens.get(index) else entries.get(index)

    fun isMistake(index: Int): Boolean {
        val entry = entries.get(index)
        return entry != EMPTY && entry != puzzle.solution.get(index)
    }

    val isSolved: Boolean
        get() = (0 until CELL_COUNT).all { puzzle.isGiven(it) || entries.get(it) == puzzle.solution.get(it) }

    val isFull: Boolean
        get() = (0 until CELL_COUNT).all { digitAt(it) != EMPTY }
}
