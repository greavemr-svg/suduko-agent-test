package io.github.greavemrsvg.sudoku

import io.github.greavemrsvg.sudoku.domain.PuzzleGenerator
import io.github.greavemrsvg.sudoku.domain.SudokuSolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.random.Random

/**
 * Manual dependency container created by [SudokuApplication]. It owns the
 * application-scoped coroutine scope and holds no game state. Randomness and the
 * dispatcher for puzzle generation are injected so tests can make them deterministic.
 */
class AppContainer(
    private val random: Random = Random.Default,
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun createPuzzleGenerator(): PuzzleGenerator = PuzzleGenerator(random, SudokuSolver())
}
