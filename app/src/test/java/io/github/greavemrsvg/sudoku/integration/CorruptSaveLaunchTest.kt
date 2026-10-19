package io.github.greavemrsvg.sudoku.integration

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.data.SaveFixtures
import io.github.greavemrsvg.sudoku.data.SavedGameCodec
import io.github.greavemrsvg.sudoku.data.SavedGameDto
import io.github.greavemrsvg.sudoku.domain.EMPTY
import io.github.greavemrsvg.sudoku.domain.SavedGameValidator
import io.github.greavemrsvg.sudoku.ui.GRID_TAG
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** AC18: a missing, unreadable or invalid save shows a new puzzle and does not crash. */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class)
class CorruptSaveLaunchTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val driver = GameDriver(composeRule)
    private val puzzle = TestSudokuApplication.firstPuzzle()
    private val saveFile get() = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "saved_game.json")

    private fun assertNewPuzzleShownAndSaved() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.onNodeWithTag(GRID_TAG).assertIsDisplayed()
        val givens = (0 until 81).map { puzzle.givens.get(it).let { g -> if (g == EMPTY) "" else g.toString() } }
        assertEquals(givens, driver.displayedDigits())
        (ApplicationProvider.getApplicationContext<Context>() as TestSudokuApplication).flushSaves()
        // The bad file was replaced by a valid save of the new puzzle.
        assertEquals(puzzle, SavedGameCodec.decode(saveFile.readBytes())?.let { SavedGameValidator.validate(it) }?.puzzle)
        scenario.close()
    }

    private fun invalidSolutionBytes(): ByteArray {
        val dto: SavedGameDto = SaveFixtures.dto.copy(solution = "1".repeat(81))
        return Json.encodeToString(SavedGameDto.serializer(), dto).toByteArray()
    }

    @Test
    fun AC18_missing_save_shows_a_new_puzzle() {
        assertNotNull(puzzle)
        assertNewPuzzleShownAndSaved()
    }

    @Test
    fun AC18_empty_save_shows_a_new_puzzle() {
        saveFile.writeBytes(ByteArray(0))
        assertNewPuzzleShownAndSaved()
    }

    @Test
    fun AC18_garbage_save_shows_a_new_puzzle() {
        saveFile.writeBytes(byteArrayOf(0x00, 0x7F, 0x13, 0x42, 0x55))
        assertNewPuzzleShownAndSaved()
    }

    @Test
    fun AC18_json_that_is_not_a_game_shows_a_new_puzzle() {
        saveFile.writeText("""{"hello":"world"}""")
        assertNewPuzzleShownAndSaved()
    }

    @Test
    fun AC18_semantically_invalid_save_shows_a_new_puzzle() {
        saveFile.writeBytes(invalidSolutionBytes())
        assertNewPuzzleShownAndSaved()
    }

    @Test
    fun AC18_a_directory_at_the_save_path_shows_a_new_puzzle() {
        saveFile.mkdirs()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.onNodeWithTag(GRID_TAG).assertIsDisplayed()
        scenario.close()
    }
}
