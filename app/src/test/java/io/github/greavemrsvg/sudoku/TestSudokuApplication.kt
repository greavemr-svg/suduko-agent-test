package io.github.greavemrsvg.sudoku

import io.github.greavemrsvg.sudoku.domain.Puzzle
import io.github.greavemrsvg.sudoku.domain.PuzzleGenerator
import io.github.greavemrsvg.sudoku.domain.SudokuSolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.random.Random

/**
 * Application for Robolectric integration tests. Randomness is seeded and puzzle generation runs on an
 * unconfined test dispatcher, so the first puzzle is deterministic and ready without waiting, as do file loads
 * and writes. The save queue (the application scope) runs on a [TestCoroutineScheduler] that [flushSaves]
 * drains, so tests decide exactly when pending saves reach the file. The container saves under [filesDir].
 * The earlier "later saves not written" symptom was the Windows AtomicFile rename quirk (see task 6), not a
 * dispatcher effect; on Windows these integration tests pass because conflation turns each launch into a
 * single write.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestSudokuApplication : SudokuApplication() {
    private val saveScheduler = TestCoroutineScheduler()

    override fun createContainer(): AppContainer =
        AppContainer(
            saveDirectory = filesDir,
            random = Random(SEED),
            defaultDispatcher = UnconfinedTestDispatcher(),
            ioDispatcher = UnconfinedTestDispatcher(),
            applicationScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(saveScheduler)),
        )

    /** Runs all queued saves to completion. */
    fun flushSaves() = saveScheduler.advanceUntilIdle()

    /**
     * Simulates process death after the player's last action has been saved: pending saves are flushed, then
     * the container (and its scope) is discarded and rebuilt with a fresh seeded random over the same
     * [filesDir]. Only the save file survives, as in a real relaunch.
     */
    fun rebuildContainer() {
        flushSaves()
        container.applicationScope.cancel()
        container = createContainer()
    }

    companion object {
        const val SEED = 20261019

        /** The puzzle a freshly launched app generates with the seeded random. */
        fun firstPuzzle(): Puzzle = PuzzleGenerator(Random(SEED), SudokuSolver()).generate()
    }
}
