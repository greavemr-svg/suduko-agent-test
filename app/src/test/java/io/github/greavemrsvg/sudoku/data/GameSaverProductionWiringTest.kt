@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.AppContainer
import io.github.greavemrsvg.sudoku.domain.GameState
import io.github.greavemrsvg.sudoku.domain.Grid
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Guards the saver risk: with the production wiring (the real Dispatchers.Default scope and Dispatchers.IO file
 * writes, no test schedulers) every later save must reach the file, not only the first. The file is polled with
 * a plain read, because reading through AtomicFile while a write is in flight would interfere with that write.
 */
@RunWith(RobolectricTestRunner::class)
class GameSaverProductionWiringTest {
    /**
     * Robolectric runs the real AtomicFile, whose rename does not replace an existing file on a Windows host, so
     * only the first write of a run lands there. That is a host quirk, not app behaviour (Android renames over
     * the old file). These tests therefore run on the Linux CI and on Linux/macOS hosts.
     */
    private fun assumeRenameReplacesFiles() = assumeFalse(System.getProperty("os.name").orEmpty().startsWith("Windows"))

    @get:Rule
    val temp = TemporaryFolder()

    private val saveFile: File get() = File(temp.root, "saved_game.json")

    private fun stateAfter(entries: Int): GameState {
        var grid: Grid = SaveFixtures.inProgress.entries
        for (i in 2 until 2 + entries) {
            val cell = SaveFixtures.openCells[i]
            grid = grid.with(cell, SaveFixtures.puzzle.solution.get(cell))
        }
        return SaveFixtures.inProgress.copy(entries = grid)
    }

    private suspend fun awaitSaved(expected: GameState) {
        val expectedBytes = SavedGameCodec.encode(expected).toList()
        withTimeout(10_000) {
            while (!saveFile.exists() || saveFile.readBytes().toList() != expectedBytes) delay(25)
        }
    }

    @Test
    fun AC15_every_successive_save_reaches_the_file_with_production_dispatchers() =
        runBlocking {
            assumeRenameReplacesFiles()
            val container = AppContainer(temp.root)
            for (entries in 0..5) {
                val state = stateAfter(entries)
                container.gameSaver.save(state)
                awaitSaved(state)
            }
            assertEquals(stateAfter(5), container.gameRepository.load())
        }

    @Test
    fun AC15_a_burst_of_saves_ends_with_the_last_one_on_disk() =
        runBlocking {
            assumeRenameReplacesFiles()
            val container = AppContainer(temp.root)
            for (entries in 0..5) container.gameSaver.save(stateAfter(entries))
            awaitSaved(stateAfter(5))
            assertEquals(stateAfter(5), container.gameRepository.load())
        }
}
