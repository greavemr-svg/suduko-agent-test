package io.github.greavemrsvg.sudoku.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import io.github.greavemrsvg.sudoku.domain.MAX_GIVENS
import io.github.greavemrsvg.sudoku.domain.MIN_GIVENS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class)
class GameScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val puzzle = TestSudokuApplication.firstPuzzle()
    private val openCells = (0 until CELL_COUNT).filter { !puzzle.isGiven(it) }
    private val givenCells = (0 until CELL_COUNT).filter { puzzle.isGiven(it) }

    private fun cell(index: Int): SemanticsNodeInteraction = composeRule.onNodeWithTag(cellTag(index / GRID_SIZE, index % GRID_SIZE))

    private fun mistakeBars(index: Int) =
        composeRule
            .onAllNodesWithTag(mistakeTag(index / GRID_SIZE, index % GRID_SIZE), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size

    private fun pressDigit(digit: Int) = composeRule.onNodeWithTag("digit_$digit").performClick()

    private fun pressErase() = composeRule.onNodeWithTag("erase").performClick()

    private fun wrongDigitAt(index: Int) = puzzle.solution.get(index) % GRID_SIZE + 1

    @Test
    fun AC1_launch_shows_generated_puzzle_with_givens_and_empty_cells() {
        for (i in 0 until CELL_COUNT) {
            if (puzzle.isGiven(i)) {
                cell(i).assertTextEquals(puzzle.givens.get(i).toString())
            } else {
                cell(i).assertTextEquals()
            }
        }
        assertTrue(givenCells.size in MIN_GIVENS..MAX_GIVENS)
    }

    @Test
    @Config(sdk = [26])
    fun AC1_launch_shows_generated_puzzle_at_api_26() {
        givenCells.forEach { cell(it).assertTextEquals(puzzle.givens.get(it).toString()) }
        openCells.forEach { cell(it).assertTextEquals() }
        assertTrue(givenCells.size in MIN_GIVENS..MAX_GIVENS)
    }

    @Test
    fun AC5_tapping_an_empty_cell_marks_only_that_cell_selected() {
        val (first, second) = openCells
        cell(first).assertIsNotSelected()
        cell(first).performClick()
        cell(first).assertIsSelected()
        cell(second).assertIsNotSelected()
        cell(second).performClick()
        cell(second).assertIsSelected()
        cell(first).assertIsNotSelected()
    }

    @Test
    fun AC6_digit_pad_enters_digit_in_selected_cell() {
        val target = openCells.first()
        cell(target).performClick()
        pressDigit(4)
        cell(target).assertTextEquals("4")
    }

    @Test
    fun AC9_wrong_digit_is_shown_as_mistake_immediately() {
        val target = openCells.first()
        cell(target).performClick()
        assertEquals(0, mistakeBars(target))
        pressDigit(wrongDigitAt(target))
        cell(target).assertTextEquals(wrongDigitAt(target).toString())
        assertEquals(1, mistakeBars(target))
    }

    @Test
    fun AC20_only_mistakes_have_the_underline_bar() {
        val (first, second) = openCells
        cell(first).performClick()
        pressDigit(puzzle.solution.get(first))
        cell(second).performClick()
        pressDigit(wrongDigitAt(second))
        assertEquals(0, mistakeBars(first))
        assertEquals(0, mistakeBars(givenCells.first()))
        assertEquals(1, mistakeBars(second))
    }

    @Test
    fun AC11_erase_empties_a_correct_entry_and_a_mistake() {
        val (first, second) = openCells
        cell(first).performClick()
        pressDigit(puzzle.solution.get(first))
        pressErase()
        cell(first).assertTextEquals()

        cell(second).performClick()
        pressDigit(wrongDigitAt(second))
        assertEquals(1, mistakeBars(second))
        pressErase()
        cell(second).assertTextEquals()
        assertEquals(0, mistakeBars(second))
    }

    @Test
    fun AC8_given_cell_ignores_digit_and_erase() {
        val given = givenCells.first()
        val digit = puzzle.givens.get(given)
        cell(given).performClick()
        pressDigit(digit % GRID_SIZE + 1)
        pressErase()
        cell(given).assertTextEquals(digit.toString())
    }
}
