@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.AppContainer
import io.github.greavemrsvg.sudoku.domain.GameRepository
import io.github.greavemrsvg.sudoku.domain.GameState
import io.github.greavemrsvg.sudoku.domain.Grid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Collections

/**
 * Guards the saver risk: with the production wiring (the real Dispatchers.Default scope from [AppContainer] and
 * saves switching to Dispatchers.IO, no test schedulers) every later save must reach the repository, not only the
 * first. The dispatcher behaviour is asserted on every host with a recording repository. The real file round
 * trip is host-aware, because Robolectric's AtomicFile cannot rename over an existing file on a Windows host
 * (a host quirk; Android renames over the old file): there the first write survives intact, elsewhere the last
 * write wins.
 */
@RunWith(RobolectricTestRunner::class)
class GameSaverProductionWiringTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val isWindows: Boolean get() = System.getProperty("os.name").orEmpty().startsWith("Windows")

    private class RecordingRepository : GameRepository {
        val saved: MutableList<GameState> = Collections.synchronizedList(mutableListOf())

        override suspend fun load(): GameState? = saved.lastOrNull()

        override suspend fun save(state: GameState) {
            withContext(Dispatchers.IO) { saved.add(state) }
        }
    }

    private fun stateAfter(entries: Int): GameState {
        var grid: Grid = SaveFixtures.inProgress.entries
        for (i in 2 until 2 + entries) {
            val cell = SaveFixtures.openCells[i]
            grid = grid.with(cell, SaveFixtures.puzzle.solution.get(cell))
        }
        return SaveFixtures.inProgress.copy(entries = grid)
    }

    private suspend fun awaitRecorded(
        repository: RecordingRepository,
        count: Int,
    ) = withTimeout(10_000) { while (repository.saved.size < count) delay(10) }

    private suspend fun awaitLast(
        repository: RecordingRepository,
        expected: GameState,
    ) = withTimeout(10_000) { while (repository.saved.lastOrNull() != expected) delay(10) }

    @Test
    fun AC15_every_successive_save_is_recorded_in_order_with_production_dispatchers() =
        runBlocking {
            val repository = RecordingRepository()
            val saver = GameSaver(AppContainer(temp.root).applicationScope, repository)
            for (entries in 0..5) {
                saver.save(stateAfter(entries))
                awaitRecorded(repository, entries + 1)
            }
            assertEquals((0..5).map { stateAfter(it) }, repository.saved.toList())
        }

    @Test
    fun AC15_a_burst_of_saves_ends_with_the_last_one_recorded() =
        runBlocking {
            val repository = RecordingRepository()
            val saver = GameSaver(AppContainer(temp.root).applicationScope, repository)
            for (entries in 0..5) saver.save(stateAfter(entries))
            awaitLast(repository, stateAfter(5))
            val recorded = repository.saved.toList()
            assertEquals(stateAfter(5), recorded.last())
            // Conflation may drop intermediate states but never reorders them.
            val order = recorded.map { state -> (0..5).first { stateAfter(it) == state } }
            assertEquals(order.sorted(), order)
        }

    @Test
    fun AC15_successive_saves_through_the_real_file_repository_are_host_aware() =
        runBlocking {
            val container = AppContainer(temp.root)
            for (entries in 0..5) {
                container.gameSaver.save(stateAfter(entries))
                // Wait until the saver has drained this snapshot; the window is generous for a tiny write.
                delay(150)
            }
            val loaded = container.gameRepository.load()
            if (isWindows) {
                // The first write survives intact; later renames fail on a Windows host.
                assertEquals(stateAfter(0), loaded)
            } else {
                withTimeout(10_000) {
                    while (container.gameRepository.load() != stateAfter(5)) delay(25)
                }
                assertEquals(stateAfter(5), container.gameRepository.load())
            }
        }
}
