package io.github.greavemrsvg.sudoku.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.EMPTY
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class)
class SolvedAndNewGameTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val puzzle = TestSudokuApplication.firstPuzzle()
    private val openCells = (0 until CELL_COUNT).filter { !puzzle.isGiven(it) }

    private fun cell(index: Int): SemanticsNodeInteraction = composeRule.onNodeWithTag(cellTag(index / GRID_SIZE, index % GRID_SIZE))

    private fun digit(d: Int) = composeRule.onNodeWithTag("digit_$d").performClick()

    private fun solve(skip: Int? = null) {
        for (i in openCells) {
            if (i == skip) continue
            cell(i).performClick()
            digit(puzzle.solution.get(i))
        }
    }

    private fun bannerCount() = composeRule.onAllNodesWithTag(SOLVED_BANNER_TAG).fetchSemanticsNodes().size

    private fun displayedDigits() = (0 until CELL_COUNT).map { cellText(it) }

    private fun description(index: Int) =
        cell(index)
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .joinToString(", ")

    private fun cellText(index: Int) =
        cell(index)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }
            .orEmpty()

    @Test
    fun AC12_last_correct_digit_shows_solved_message_and_locks_the_grid() {
        val last = openCells.last()
        solve(skip = last)
        assertEquals(0, bannerCount())
        cell(last).performClick()
        digit(puzzle.solution.get(last))
        composeRule.onNodeWithTag(SOLVED_BANNER_TAG).assertIsDisplayed()

        // A7: read-only once solved. Digits and Erase change nothing.
        val before = displayedDigits()
        val target = openCells.first()
        cell(target).performClick()
        digit(puzzle.solution.get(target) % GRID_SIZE + 1)
        composeRule.onNodeWithTag("erase").performClick()
        assertEquals(before, displayedDigits())
        assertEquals(1, bannerCount())
    }

    @Test
    fun AC12_no_solved_message_while_a_mistake_remains() {
        val wrong = openCells.first()
        solve(skip = wrong)
        cell(wrong).performClick()
        digit(puzzle.solution.get(wrong) % GRID_SIZE + 1)
        assertEquals(0, bannerCount())
    }

    @Test
    fun AC14_new_game_in_progress_replaces_puzzle_and_clears_entries_selection_and_mistakes() {
        val (first, second) = openCells
        cell(first).performClick()
        digit(puzzle.solution.get(first))
        cell(second).performClick()
        digit(puzzle.solution.get(second) % GRID_SIZE + 1)

        composeRule.onNodeWithTag("new_game").performClick()

        assertNewPuzzleShown()
    }

    @Test
    fun AC14_new_game_after_solving_removes_message_and_accepts_input_again() {
        solve()
        composeRule.onNodeWithTag(SOLVED_BANNER_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag("new_game").performClick()

        assertNewPuzzleShown()
        assertEquals(0, bannerCount())
        // The new puzzle is playable.
        val open = (0 until CELL_COUNT).first { cellText(it).isEmpty() }
        cell(open).performClick()
        digit(5)
        cell(open).assertTextEquals("5")
    }

    private fun assertNewPuzzleShown() {
        composeRule.waitForIdle()
        val shown = displayedDigits()
        val oldGivens = (0 until CELL_COUNT).map { puzzle.givens.get(it).let { g -> if (g == EMPTY) "" else g.toString() } }
        assertNotEquals("New Game must show a different puzzle", oldGivens, shown)
        // Only givens are shown: no entry from the old game survives.
        for (i in 0 until CELL_COUNT) {
            if (shown[i].isNotEmpty()) assertEquals(true, description(i).endsWith("given"))
        }
        for (i in 0 until CELL_COUNT) {
            cell(i).assertIsNotSelected()
            assertEquals(
                0,
                composeRule.onAllNodesWithTag(mistakeTag(i / GRID_SIZE, i % GRID_SIZE), useUnmergedTree = true).fetchSemanticsNodes().size,
            )
        }
    }
}
