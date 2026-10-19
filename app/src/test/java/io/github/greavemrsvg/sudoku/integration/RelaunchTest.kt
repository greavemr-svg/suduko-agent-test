package io.github.greavemrsvg.sudoku.integration

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.EMPTY
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Close and relaunch: the activity is closed and the container rebuilt, so only the save file survives. */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class)
class RelaunchTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val driver = GameDriver(composeRule)
    private val puzzle = TestSudokuApplication.firstPuzzle()
    private val openCells = (0 until CELL_COUNT).filter { !puzzle.isGiven(it) }

    private fun relaunch(scenario: ActivityScenario<MainActivity>): ActivityScenario<MainActivity> {
        scenario.close()
        (ApplicationProvider.getApplicationContext<Context>() as TestSudokuApplication).rebuildContainer()
        return ActivityScenario.launch(MainActivity::class.java)
    }

    private fun wrong(index: Int) = puzzle.solution.get(index) % GRID_SIZE + 1

    private fun givensAsShown() = (0 until CELL_COUNT).map { puzzle.givens.get(it).let { g -> if (g == EMPTY) "" else g.toString() } }

    private fun assertEntriesAndMistake(
        correct: Int,
        mistake: Int,
    ) {
        assertEquals(puzzle.solution.get(correct).toString(), driver.displayedDigits()[correct])
        assertEquals(wrong(mistake).toString(), driver.displayedDigits()[mistake])
        assertEquals(1, driver.mistakeCount())
    }

    private fun playTwoMoves(): Pair<Int, Int> {
        val (correct, mistake) = openCells
        driver.enter(correct, puzzle.solution.get(correct))
        driver.enter(mistake, wrong(mistake))
        return correct to mistake
    }

    @Test
    fun AC15_same_puzzle_entries_and_mistakes_come_back_after_relaunch() {
        val first = ActivityScenario.launch(MainActivity::class.java)
        val (correct, mistake) = playTwoMoves()
        val before = driver.displayedDigits()

        val second = relaunch(first)

        assertEquals(before, driver.displayedDigits())
        assertEntriesAndMistake(correct, mistake)
        second.close()
    }

    @Test
    @Config(sdk = [26])
    fun AC15_same_puzzle_entries_and_mistakes_come_back_after_relaunch_on_api_26() {
        val first = ActivityScenario.launch(MainActivity::class.java)
        val (correct, mistake) = playTwoMoves()

        val second = relaunch(first)

        assertEntriesAndMistake(correct, mistake)
        second.close()
    }

    @Test
    fun AC15_an_erased_entry_stays_erased_after_relaunch() {
        val first = ActivityScenario.launch(MainActivity::class.java)
        val (correct, mistake) = playTwoMoves()
        driver.cell(mistake).performClick()
        driver.erase()

        val second = relaunch(first)

        assertEquals("", driver.displayedDigits()[mistake])
        assertEquals(puzzle.solution.get(correct).toString(), driver.displayedDigits()[correct])
        assertEquals(0, driver.mistakeCount())
        second.close()
    }

    @Test
    fun AC16_solved_game_is_restored_solved_and_read_only() {
        val first = ActivityScenario.launch(MainActivity::class.java)
        for (i in openCells) driver.enter(i, puzzle.solution.get(i))
        assertTrue(driver.isSolvedMessageShown())
        val solvedDigits = driver.displayedDigits()

        val second = relaunch(first)

        assertTrue(driver.isSolvedMessageShown())
        assertEquals(solvedDigits, driver.displayedDigits())
        driver.enter(openCells.first(), wrong(openCells.first()))
        composeRule.waitForIdle()
        assertEquals(solvedDigits, driver.displayedDigits())
        assertTrue(driver.isSolvedMessageShown())
        second.close()
    }

    @Test
    fun AC17_after_new_game_the_newer_game_is_restored_and_the_old_one_is_gone() {
        val first = ActivityScenario.launch(MainActivity::class.java)
        playTwoMoves()
        driver.newGame()
        composeRule.waitForIdle()
        val newer = driver.displayedDigits()
        assertNotEquals(givensAsShown(), newer)

        val second = relaunch(first)
        assertEquals(newer, driver.displayedDigits())
        assertEquals(0, driver.mistakeCount())

        // Single slot: a second relaunch still shows the same newer game, not the old one.
        val third = relaunch(second)
        assertEquals(newer, driver.displayedDigits())
        assertNotEquals(givensAsShown(), driver.displayedDigits())
        third.close()
    }
}
