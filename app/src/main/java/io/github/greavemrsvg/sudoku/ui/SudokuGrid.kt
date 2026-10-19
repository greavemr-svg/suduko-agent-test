package io.github.greavemrsvg.sudoku.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE

/** Test tag of the whole grid. */
const val GRID_TAG = "grid"

/**
 * The 9x9 grid. Each row gives its nine cells an equal share of the width (spec A10) and every cell is
 * square, so the grid never scrolls and its height equals its width.
 */
@Composable
fun SudokuGrid(
    cells: List<CellUi>,
    onCellTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag(GRID_TAG)) {
        for (row in 0 until GRID_SIZE) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until GRID_SIZE) {
                    val index = row * GRID_SIZE + column
                    SudokuCell(
                        cell = cells[index],
                        onClick = { onCellTapped(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
