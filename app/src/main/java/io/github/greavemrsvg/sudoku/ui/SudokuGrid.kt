package io.github.greavemrsvg.sudoku.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE

/** Test tag of the whole grid. */
const val GRID_TAG = "grid"

private const val DIGIT_TEXT_SP = 22f
private const val MAX_DIGIT_FRACTION_OF_CELL = 0.6f

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
    BoxWithConstraints(modifier = modifier.fillMaxWidth().testTag(GRID_TAG)) {
        // The digit grows with the system font size, but never beyond a fraction of the cell, so it
        // cannot overflow at the largest font setting. Computed once here for all 81 cells.
        val density = LocalDensity.current
        val digitSize =
            with(density) {
                val cellPx = constraints.maxWidth.toFloat() / GRID_SIZE
                minOf(DIGIT_TEXT_SP.sp.toPx(), cellPx * MAX_DIGIT_FRACTION_OF_CELL).toSp()
            }
        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until GRID_SIZE) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (column in 0 until GRID_SIZE) {
                        val index = row * GRID_SIZE + column
                        SudokuCell(
                            cell = cells[index],
                            onClick = { onCellTapped(index) },
                            digitSize = digitSize,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
