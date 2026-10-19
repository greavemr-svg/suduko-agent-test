package io.github.greavemrsvg.sudoku.domain

import io.github.greavemrsvg.sudoku.data.SavedGameDto

/** Applies every ADR 0002 load rule to untrusted data. Returns null for any violation; never throws. */
object SavedGameValidator {
    private const val GRID_CHARS = "0123456789"

    fun validate(dto: SavedGameDto): GameState? {
        if (dto.version != SavedGameDto.CURRENT_VERSION) return null
        val givens = parseGrid(dto.givens) ?: return null
        val solution = parseGrid(dto.solution) ?: return null
        val entries = parseGrid(dto.entries) ?: return null
        if (!solution.isValidSolution()) return null
        if (!givensMatchSolution(givens, solution)) return null
        if (givens.filledCount !in MIN_GIVENS..MAX_GIVENS) return null
        if (SudokuSolver().countSolutions(givens, 2) != 1) return null
        if ((0 until CELL_COUNT).any { givens.get(it) != EMPTY && entries.get(it) != EMPTY }) return null
        val state = GameState(Puzzle(givens, solution), entries)
        return if (state.isSolved == dto.solved) state else null
    }

    private fun givensMatchSolution(
        givens: Grid,
        solution: Grid,
    ): Boolean = (0 until CELL_COUNT).all { givens.get(it) == EMPTY || givens.get(it) == solution.get(it) }

    private fun parseGrid(text: String): Grid? {
        if (text.length != CELL_COUNT || text.any { it !in GRID_CHARS }) return null
        return Grid(IntArray(CELL_COUNT) { text[it] - '0' })
    }
}
