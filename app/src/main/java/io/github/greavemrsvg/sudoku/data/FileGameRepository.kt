package io.github.greavemrsvg.sudoku.data

import android.util.AtomicFile
import android.util.Log
import io.github.greavemrsvg.sudoku.domain.GameRepository
import io.github.greavemrsvg.sudoku.domain.GameState
import io.github.greavemrsvg.sudoku.domain.SavedGameValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Single-slot save in `<directory>/saved_game.json`, written atomically (ADR 0002).
 * Re-reads the file on every [load] and caches nothing. Log messages are fixed text,
 * never game content.
 */
class FileGameRepository(
    directory: File,
    private val ioDispatcher: CoroutineDispatcher,
) : GameRepository {
    private val file = AtomicFile(File(directory, FILE_NAME))

    override suspend fun load(): GameState? =
        withContext(ioDispatcher) {
            val bytes =
                try {
                    readBounded()
                } catch (_: FileNotFoundException) {
                    // Also thrown for a directory or a permission-denied file at the path: those are invalid saves.
                    if (!file.baseFile.exists()) return@withContext null
                    null
                } catch (_: IOException) {
                    null
                }
            val state = bytes?.let { SavedGameCodec.decode(it) }?.let { SavedGameValidator.validate(it) }
            if (state == null) {
                Log.w(TAG, "Saved game is unreadable or invalid and was discarded")
                file.delete()
            }
            state
        }

    override suspend fun save(state: GameState) {
        val bytes = SavedGameCodec.encode(state)
        withContext(ioDispatcher) {
            try {
                val stream = file.startWrite()
                try {
                    stream.write(bytes)
                    file.finishWrite(stream)
                } catch (e: IOException) {
                    file.failWrite(stream)
                    throw e
                }
            } catch (_: IOException) {
                Log.w(TAG, "Saving the game failed")
            }
        }
    }

    /**
     * Reads at most [SavedGameCodec.MAX_BYTES] + 1 bytes through [AtomicFile.openRead], which on API 26-29
     * first restores a leftover backup. A file over the cap yields oversize bytes the codec rejects.
     * Throws [FileNotFoundException] when there is no save, other [IOException]s when unreadable.
     */
    private fun readBounded(): ByteArray {
        val limit = SavedGameCodec.MAX_BYTES + 1
        file.openRead().use { input ->
            val buffer = ByteArray(limit)
            var total = 0
            while (total < limit) {
                val n = input.read(buffer, total, limit - total)
                if (n < 0) break
                total += n
            }
            return buffer.copyOf(total)
        }
    }

    private companion object {
        const val FILE_NAME = "saved_game.json"
        const val TAG = "GameRepository"
    }
}
