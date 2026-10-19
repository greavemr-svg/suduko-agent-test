package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.EMPTY
import io.github.greavemrsvg.sudoku.domain.GameState
import io.github.greavemrsvg.sudoku.domain.Grid
import io.github.greavemrsvg.sudoku.domain.Puzzle
import io.github.greavemrsvg.sudoku.domain.PuzzleGenerator
import io.github.greavemrsvg.sudoku.domain.SudokuSolver
import kotlin.random.Random

/** Shared, deterministic save-format fixtures. */
object SaveFixtures {
    val puzzle: Puzzle = PuzzleGenerator(Random(42), SudokuSolver()).generate()

    val openCells: List<Int> = (0 until CELL_COUNT).filter { !puzzle.isGiven(it) }

    /** In progress: one correct entry and one mistake. */
    val inProgress: GameState =
        GameState(
            puzzle,
            Grid(IntArray(CELL_COUNT)).with(openCells[0], puzzle.solution.get(openCells[0])).with(
                openCells[1],
                puzzle.solution.get(openCells[1]) % 9 + 1,
            ),
        )

    val solved: GameState =
        GameState(
            puzzle,
            Grid(IntArray(CELL_COUNT) { if (puzzle.isGiven(it)) EMPTY else puzzle.solution.get(it) }),
        )

    /** A valid DTO for [inProgress]. */
    val dto: SavedGameDto = SavedGameCodec.toDto(inProgress)

    fun String.withChar(
        index: Int,
        char: Char,
    ): String = substring(0, index) + char + substring(index + 1)
}
