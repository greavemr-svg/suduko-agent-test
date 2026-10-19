package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.GameState
import io.github.greavemrsvg.sudoku.domain.Grid
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Converts between [GameState] and the JSON bytes of the save file (ADR 0002). */
object SavedGameCodec {
    /** Largest file accepted before decoding. */
    const val MAX_BYTES = 4 * 1024

    // Strict settings, written out explicitly. Unknown keys and missing keys are rejected. Note that
    // kotlinx.serialization still accepts a quoted number for an Int field; the validator checks content.
    private val json =
        Json {
            isLenient = false
            ignoreUnknownKeys = false
            coerceInputValues = false
            explicitNulls = true
        }

    fun toDto(state: GameState): SavedGameDto =
        SavedGameDto(
            version = SavedGameDto.CURRENT_VERSION,
            givens = encodeGrid(state.puzzle.givens),
            solution = encodeGrid(state.puzzle.solution),
            entries = encodeGrid(state.entries),
            solved = state.isSolved,
        )

    fun encode(state: GameState): ByteArray = json.encodeToString(SavedGameDto.serializer(), toDto(state)).toByteArray(Charsets.UTF_8)

    /** The DTO, or null when [bytes] are too large or are not strictly valid JSON for it. Never throws. */
    fun decode(bytes: ByteArray): SavedGameDto? {
        if (bytes.size > MAX_BYTES) return null
        return try {
            json.decodeFromString(SavedGameDto.serializer(), String(bytes, Charsets.UTF_8))
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** 81 characters, row-major, one decimal digit per cell (zero for empty). */
    fun encodeGrid(grid: Grid): String =
        buildString(CELL_COUNT) {
            for (i in 0 until CELL_COUNT) append(('0'.code + grid.get(i)).toChar())
        }
}
