@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.domain.GameRepository
import io.github.greavemrsvg.sudoku.domain.GameState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GameSaverTest {
    private class RecordingRepository(
        private val failFirst: Int = 0,
    ) : GameRepository {
        val saved = mutableListOf<GameState>()
        var attempts = 0

        override suspend fun load(): GameState? = null

        override suspend fun save(state: GameState) {
            attempts++
            if (attempts <= failFirst) throw IllegalStateException("simulated failure")
            saved += state
        }
    }

    @Test
    fun only_the_latest_snapshot_is_written() =
        runTest {
            val repo = RecordingRepository()
            val saver = GameSaver(backgroundScope, repo)
            saver.save(SaveFixtures.inProgress)
            saver.save(SaveFixtures.solved)
            saver.save(SaveFixtures.inProgress)
            runCurrent()
            assertEquals(listOf(SaveFixtures.inProgress), repo.saved)
        }

    @Test
    fun later_snapshots_are_written_after_earlier_ones_finish() =
        runTest {
            val repo = RecordingRepository()
            val saver = GameSaver(backgroundScope, repo)
            saver.save(SaveFixtures.inProgress)
            runCurrent()
            saver.save(SaveFixtures.solved)
            runCurrent()
            assertEquals(listOf(SaveFixtures.inProgress, SaveFixtures.solved), repo.saved)
        }

    @Test
    fun continues_after_a_failed_write() =
        runTest {
            val repo = RecordingRepository(failFirst = 1)
            val saver = GameSaver(backgroundScope, repo)
            saver.save(SaveFixtures.inProgress)
            runCurrent()
            assertEquals(emptyList<GameState>(), repo.saved)

            saver.save(SaveFixtures.solved)
            runCurrent()
            assertEquals(listOf(SaveFixtures.solved), repo.saved)
            assertEquals(2, repo.attempts)
        }
}
