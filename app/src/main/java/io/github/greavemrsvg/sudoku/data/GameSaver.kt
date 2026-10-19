package io.github.greavemrsvg.sudoku.data

import android.util.Log
import io.github.greavemrsvg.sudoku.domain.GameRepository
import io.github.greavemrsvg.sudoku.domain.GameState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Write queue for the save slot. Holds only the newest snapshot (conflated), written by one
 * coroutine in [scope], which keeps running after a failed write.
 */
class GameSaver(
    scope: CoroutineScope,
    private val repository: GameRepository,
) {
    private val snapshots = Channel<GameState>(Channel.CONFLATED)

    init {
        scope.launch {
            for (state in snapshots) {
                try {
                    repository.save(state)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    Log.w(TAG, "Saving the game failed")
                }
            }
        }
    }

    /** Queues [state], replacing any snapshot not yet written. Never blocks or throws. */
    fun save(state: GameState) {
        snapshots.trySend(state)
    }

    private companion object {
        const val TAG = "GameSaver"
    }
}
