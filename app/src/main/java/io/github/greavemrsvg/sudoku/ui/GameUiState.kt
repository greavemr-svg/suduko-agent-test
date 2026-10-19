package io.github.greavemrsvg.sudoku.ui

/** What the game screen renders. Built from the domain `GameState` by [GameViewModel]. */
sealed interface GameUiState {
    /** The first puzzle is being generated. */
    data object Loading : GameUiState

    data class Ready(
        val cells: List<CellUi>,
        val isSolved: Boolean,
        /** A replacement puzzle is being generated after New Game. Input is ignored meanwhile. */
        val isGenerating: Boolean = false,
    ) : GameUiState
}

/** One grid cell. [row] and [column] are 0-based. [digit] is 0 when the cell is empty. */
data class CellUi(
    val row: Int,
    val column: Int,
    val digit: Int,
    val isGiven: Boolean,
    val isMistake: Boolean,
    val isSelected: Boolean,
)
