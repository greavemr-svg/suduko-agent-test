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
            if (!file.baseFile.exists()) return@withContext null
            val state = readBytes()?.let { SavedGameCodec.decode(it) }?.let { SavedGameValidator.validate(it) }
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

    /** The file content; null when unreadable. A file over the size cap yields bytes the codec rejects. */
    private fun readBytes(): ByteArray? =
        try {
            if (file.baseFile.length() > SavedGameCodec.MAX_BYTES) {
                ByteArray(SavedGameCodec.MAX_BYTES + 1)
            } else {
                file.readFully()
            }
        } catch (_: FileNotFoundException) {
            null
        } catch (_: IOException) {
            null
        }

    private companion object {
        const val FILE_NAME = "saved_game.json"
        const val TAG = "GameRepository"
    }
}
