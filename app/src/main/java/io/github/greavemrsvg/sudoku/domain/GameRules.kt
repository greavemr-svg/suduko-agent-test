package io.github.greavemrsvg.sudoku.domain

object GameRules {
    fun newGame(puzzle: Puzzle): GameState = GameState(puzzle, Grid(IntArray(CELL_COUNT)), null)

    fun select(
        state: GameState,
        index: Int,
    ): GameState {
        require(index in 0 until CELL_COUNT) { "Cell index must be in 0 until $CELL_COUNT, got $index" }
        return state.copy(selected = index)
    }

    fun enterDigit(
        state: GameState,
        digit: Int,
    ): GameState {
        require(digit in 1..GRID_SIZE) { "Digit must be in 1..$GRID_SIZE, got $digit" }
        return setEntry(state, digit)
    }

    fun erase(state: GameState): GameState = setEntry(state, EMPTY)

    private fun setEntry(
        state: GameState,
        digit: Int,
    ): GameState {
        val index = state.selected ?: return state
        if (state.puzzle.isGiven(index) || state.isSolved) return state
        return state.copy(entries = state.entries.with(index, digit))
    }
}
