package io.github.greavemrsvg.sudoku.integration

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import io.github.greavemrsvg.sudoku.ui.SOLVED_BANNER_TAG
import io.github.greavemrsvg.sudoku.ui.cellTag
import io.github.greavemrsvg.sudoku.ui.mistakeTag

/** Reads and drives the game screen in the integration tests through semantics tags. */
class GameDriver(
    private val rule: ComposeTestRule,
) {
    fun cell(index: Int): SemanticsNodeInteraction = rule.onNodeWithTag(cellTag(index / GRID_SIZE, index % GRID_SIZE))

    fun enter(
        index: Int,
        digit: Int,
    ) {
        cell(index).performClick()
        rule.onNodeWithTag("digit_$digit").performClick()
    }

    fun erase() = rule.onNodeWithTag("erase").performClick()

    fun newGame() = rule.onNodeWithTag("new_game").performClick()

    /** The digit text of every cell, empty string for empty cells. */
    fun displayedDigits(): List<String> =
        (0 until CELL_COUNT).map { index ->
            cell(index)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Text)
                ?.joinToString("") { it.text }
                .orEmpty()
        }

    fun mistakeCount(): Int =
        (0 until CELL_COUNT).count { index ->
            rule
                .onAllNodesWithTag(mistakeTag(index / GRID_SIZE, index % GRID_SIZE), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

    fun isSolvedMessageShown(): Boolean =
        rule
            .onAllNodesWithTag(SOLVED_BANNER_TAG)
            .fetchSemanticsNodes()
            .any { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.isNotEmpty() } == true }
}
