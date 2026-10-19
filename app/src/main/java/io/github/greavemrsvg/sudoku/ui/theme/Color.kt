package io.github.greavemrsvg.sudoku.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

private const val SELECTED_TINT_ALPHA = 0.16f

/**
 * Background of the selected cell: the primary colour at low opacity over the surface. It stays light
 * (or dark) enough that digits in on-surface, primary and error colours keep at least 4.5:1 contrast.
 */
fun ColorScheme.selectedCellTint(): Color = primary.copy(alpha = SELECTED_TINT_ALPHA).compositeOver(surface)
