package io.github.greavemrsvg.sudoku.ui

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import io.github.greavemrsvg.sudoku.ui.theme.SudokuTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The New Game button reflects [GameUiState.Ready.isGenerating]. */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class)
class GameScreenStateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun show(isGenerating: Boolean) {
        val cells =
            List(CELL_COUNT) { i -> CellUi(i / GRID_SIZE, i % GRID_SIZE, 0, isGiven = false, isMistake = false, isSelected = false) }
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                SudokuTheme { GameScreen(GameUiState.Ready(cells, isSolved = false, isGenerating = isGenerating), {}, {}, {}, {}) }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun AC14_new_game_button_is_disabled_while_a_puzzle_is_generating() {
        show(isGenerating = true)
        composeRule.onNodeWithTag("new_game").assertIsNotEnabled()
    }

    @Test
    fun AC14_new_game_button_is_enabled_when_idle() {
        show(isGenerating = false)
        composeRule.onNodeWithTag("new_game").assertIsEnabled()
    }
}
