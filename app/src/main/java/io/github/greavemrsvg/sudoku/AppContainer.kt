package io.github.greavemrsvg.sudoku

import io.github.greavemrsvg.sudoku.data.FileGameRepository
import io.github.greavemrsvg.sudoku.data.GameSaver
import io.github.greavemrsvg.sudoku.domain.GameRepository
import io.github.greavemrsvg.sudoku.domain.PuzzleGenerator
import io.github.greavemrsvg.sudoku.domain.SudokuSolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.random.Random

/**
 * Manual dependency container created by [SudokuApplication]. It owns the application-scoped coroutine
 * scope, the save repository (a file in [saveDirectory]) and the [GameSaver], and holds no game state.
 * Randomness, dispatchers and the scope are injected so tests can make them deterministic.
 */
class AppContainer(
    saveDirectory: File,
    private val random: Random = Random.Default,
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    val gameRepository: GameRepository = FileGameRepository(saveDirectory, ioDispatcher)
    val gameSaver: GameSaver = GameSaver(applicationScope, gameRepository)

    fun createPuzzleGenerator(): PuzzleGenerator = PuzzleGenerator(random, SudokuSolver())
}
