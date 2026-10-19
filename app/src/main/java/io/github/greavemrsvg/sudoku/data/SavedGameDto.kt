package io.github.greavemrsvg.sudoku.data

import kotlinx.serialization.Serializable

/** Wire form of the save file (ADR 0002, schema version 1). All fields are required. */
@Serializable
data class SavedGameDto(
    val version: Int,
    val givens: String,
    val solution: String,
    val entries: String,
    val solved: Boolean,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
