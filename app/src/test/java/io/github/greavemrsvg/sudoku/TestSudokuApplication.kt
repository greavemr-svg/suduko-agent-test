package io.github.greavemrsvg.sudoku

import io.github.greavemrsvg.sudoku.domain.Puzzle
import io.github.greavemrsvg.sudoku.domain.PuzzleGenerator
import io.github.greavemrsvg.sudoku.domain.SudokuSolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.random.Random

/**
 * Application for Robolectric integration tests. Randomness is seeded and generation runs on an
 * unconfined test dispatcher, so the first puzzle is deterministic and ready without waiting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestSudokuApplication : SudokuApplication() {
    override fun createContainer(): AppContainer = AppContainer(Random(SEED), UnconfinedTestDispatcher())

    companion object {
        const val SEED = 20261019

        /** The puzzle a freshly launched app generates with the seeded random. */
        fun firstPuzzle(): Puzzle = PuzzleGenerator(Random(SEED), SudokuSolver()).generate()
    }
}
