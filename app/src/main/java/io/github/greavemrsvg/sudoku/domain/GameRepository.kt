package io.github.greavemrsvg.sudoku.domain

interface GameRepository {
    /** The saved game, or null when absent or invalid (invalid data is already logged and deleted). */
    suspend fun load(): GameState?

    /** Saves the snapshot. IO failures are handled inside; throws only on programmer error. */
    suspend fun save(state: GameState)
}
