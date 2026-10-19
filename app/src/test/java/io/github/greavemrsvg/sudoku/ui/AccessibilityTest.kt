package io.github.greavemrsvg.sudoku.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Automated support for AC21. The TalkBack check itself is manual. */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class)
class AccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val puzzle = TestSudokuApplication.firstPuzzle()
    private val open = (0 until CELL_COUNT).first { !puzzle.isGiven(it) }
    private val given = (0 until CELL_COUNT).first { puzzle.isGiven(it) }

    private fun cell(index: Int): SemanticsNodeInteraction = composeRule.onNodeWithTag(cellTag(index / GRID_SIZE, index % GRID_SIZE))

    private fun position(index: Int) = "Row ${index / GRID_SIZE + 1}, column ${index % GRID_SIZE + 1}"

    @Test
    fun AC21_given_cell_is_announced_with_position_digit_and_given() {
        cell(given).assertContentDescriptionEquals("${position(given)}, ${puzzle.givens.get(given)}, given")
    }

    @Test
    fun AC21_empty_cell_is_announced_as_empty_and_selection_is_exposed() {
        cell(open).assertContentDescriptionEquals("${position(open)}, empty")
        cell(open).performClick()
        cell(open).assertIsSelected()
    }

    @Test
    fun AC21_entered_digit_and_mistake_are_announced() {
        cell(open).performClick()
        val right = puzzle.solution.get(open)
        composeRule.onNodeWithTag("digit_$right").performClick()
        cell(open).assertContentDescriptionEquals("${position(open)}, $right")
        composeRule.onNodeWithTag("digit_${right % GRID_SIZE + 1}").performClick()
        cell(open).assertContentDescriptionEquals("${position(open)}, ${right % GRID_SIZE + 1}, mistake")
    }

    @Test
    fun AC21_erased_cell_is_announced_as_empty_again() {
        cell(open).performClick()
        composeRule.onNodeWithTag("digit_${puzzle.solution.get(open)}").performClick()
        composeRule.onNodeWithTag("erase").performClick()
        cell(open).assertContentDescriptionEquals("${position(open)}, empty")
    }

    @Test
    fun AC21_digit_buttons_erase_and_new_game_are_announced_by_name() {
        for (digit in 1..GRID_SIZE) {
            composeRule.onNodeWithContentDescription("Digit $digit").assertExists()
        }
        composeRule.onNodeWithText("Erase").assertExists()
        composeRule.onNodeWithText("New game").assertExists()
        composeRule.onNodeWithTag("new_game").assertTextEquals("New game")
    }

    @Test
    fun AC21_solved_message_is_a_live_region() {
        // The live-region node exists from the start, empty, so TalkBack sees its text change when solved.
        val before = composeRule.onNodeWithTag(SOLVED_BANNER_TAG).fetchSemanticsNode().config
        assertTrue(before.contains(SemanticsProperties.LiveRegion))
        composeRule.onNodeWithTag(SOLVED_BANNER_TAG).assertTextEquals("")
        for (i in (0 until CELL_COUNT).filter { !puzzle.isGiven(it) }) {
            cell(i).performClick()
            composeRule.onNodeWithTag("digit_${puzzle.solution.get(i)}").performClick()
        }
        val config = composeRule.onNodeWithTag(SOLVED_BANNER_TAG).fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsProperties.LiveRegion))
        composeRule.onNodeWithTag(SOLVED_BANNER_TAG).assertTextEquals("Solved! Well done.")
    }
}
