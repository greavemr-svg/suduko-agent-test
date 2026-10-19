package io.github.greavemrsvg.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.sudoku.R
import io.github.greavemrsvg.sudoku.domain.BOX_SIZE
import io.github.greavemrsvg.sudoku.domain.EMPTY
import io.github.greavemrsvg.sudoku.ui.theme.selectedCellTint

private val ThinLine = 1.dp
private val ThickLine = 2.dp
private val SelectedBorder = 3.dp
private val MistakeBarHeight = 3.dp
private val MistakeBarBottomPadding = 4.dp
private const val MISTAKE_BAR_WIDTH_FRACTION = 0.5f

/** Test tag of the cell at [row], [column] (0-based). */
fun cellTag(
    row: Int,
    column: Int,
) = "cell_${row}_$column"

/** Test tag of the underline bar shown under a mistaken digit. */
fun mistakeTag(
    row: Int,
    column: Int,
) = "mistake_${row}_$column"

/**
 * One grid cell. [digitSize] is the digit size, already capped to the cell size by [SudokuGrid].
 * The caller gives it an equal share of the row width and the cell keeps itself square.
 * Cell and box borders are drawn inside the cell bounds, so they never change the cell size.
 */
@Composable
fun SudokuCell(
    cell: CellUi,
    onClick: () -> Unit,
    digitSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val lineColor = colors.outline
    val boxLineColor = colors.onSurface
    val selectedColor = colors.primary
    val background = if (cell.isSelected) colors.selectedCellTint() else Color.Transparent
    val textColor =
        when {
            cell.isMistake -> colors.error
            cell.isGiven -> colors.onSurface
            else -> colors.primary
        }
    val description = cellDescription(cell)
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .testTag(cellTag(cell.row, cell.column))
                .background(background)
                .drawBehind { drawCellBorders(cell, lineColor, boxLineColor, selectedColor) }
                .selectable(selected = cell.isSelected, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (cell.digit != EMPTY) {
            BasicText(
                text = cell.digit.toString(),
                style =
                    TextStyle(
                        color = textColor,
                        fontSize = digitSize,
                        fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    ),
                maxLines = 1,
                softWrap = false,
            )
        }
        if (cell.isMistake) {
            // Non-colour cue for mistakes (AC20): a bar under the digit.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = MistakeBarBottomPadding)
                        .fillMaxWidth(MISTAKE_BAR_WIDTH_FRACTION)
                        .height(MistakeBarHeight)
                        .background(colors.error)
                        .testTag(mistakeTag(cell.row, cell.column)),
            )
        }
    }
}

/** "Row r, column c, <digit|empty>[, given][, mistake]" for screen readers. Rows and columns are 1-based. */
@Composable
private fun cellDescription(cell: CellUi): String {
    val position = stringResource(R.string.cell_position, cell.row + 1, cell.column + 1)
    val content = if (cell.digit == EMPTY) stringResource(R.string.cell_empty) else cell.digit.toString()
    val given = if (cell.isGiven) stringResource(R.string.cell_given) else null
    val mistake = if (cell.isMistake) stringResource(R.string.cell_mistake) else null
    return listOfNotNull(position, content, given, mistake).joinToString(stringResource(R.string.cell_description_separator))
}

private fun DrawScope.drawCellBorders(
    cell: CellUi,
    lineColor: Color,
    boxLineColor: Color,
    selectedColor: Color,
) {
    // Every cell draws its right and bottom edge. The first row and column also draw the outer edge.
    val rightIsBox = (cell.column + 1) % BOX_SIZE == 0
    val bottomIsBox = (cell.row + 1) % BOX_SIZE == 0
    drawRect(
        color = if (rightIsBox) boxLineColor else lineColor,
        topLeft = Offset(size.width - (if (rightIsBox) ThickLine else ThinLine).toPx(), 0f),
        size = Size((if (rightIsBox) ThickLine else ThinLine).toPx(), size.height),
    )
    drawRect(
        color = if (bottomIsBox) boxLineColor else lineColor,
        topLeft = Offset(0f, size.height - (if (bottomIsBox) ThickLine else ThinLine).toPx()),
        size = Size(size.width, (if (bottomIsBox) ThickLine else ThinLine).toPx()),
    )
    if (cell.column == 0) drawRect(boxLineColor, Offset.Zero, Size(ThickLine.toPx(), size.height))
    if (cell.row == 0) drawRect(boxLineColor, Offset.Zero, Size(size.width, ThickLine.toPx()))
    if (cell.isSelected) {
        val width = SelectedBorder.toPx()
        drawRect(
            color = selectedColor,
            topLeft = Offset(width / 2, width / 2),
            size = Size(size.width - width, size.height - width),
            style = Stroke(width = width),
        )
    }
}
