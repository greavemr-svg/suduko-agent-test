package io.github.greavemrsvg.sudoku.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.abs

/** Touch-target minimums from the Accessibility NFR and spec A10, on a 360 dp wide portrait screen. */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class, qualifiers = "w360dp-h640dp-port-xhdpi")
class TouchTargetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun size(tag: String) = composeRule.onNodeWithTag(tag).fetchSemanticsNode().size

    @Test
    fun NFR_grid_cells_share_width_equally_and_are_at_least_24dp_at_360dp_width() {
        val reference = size(cellTag(0, 0))
        val gridWidth = size(GRID_TAG).width
        for (row in 0 until GRID_SIZE) {
            var rowWidth = 0
            for (column in 0 until GRID_SIZE) {
                val tag = cellTag(row, column)
                composeRule.onNodeWithTag(tag).assertWidthIsAtLeast(24.dp).assertHeightIsAtLeast(24.dp)
                val cell = size(tag)
                assertTrue("$tag width differs", abs(cell.width - reference.width) <= 1)
                assertTrue("$tag height differs", abs(cell.height - reference.height) <= 1)
                rowWidth += cell.width
            }
            // The nine cells fill the grid width: no scrolling and no clipped cell.
            assertTrue("row $row width $rowWidth vs grid $gridWidth", abs(rowWidth - gridWidth) <= 1)
        }
        assertEquals(size(cellTag(0, 0)).width, size(cellTag(GRID_SIZE - 1, GRID_SIZE - 1)).width)
    }

    @Test
    fun NFR_digit_pad_and_erase_are_at_least_48dp() {
        for (digit in 1..GRID_SIZE) {
            composeRule.onNodeWithTag("digit_$digit").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithTag("erase").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun NFR_new_game_button_is_at_least_48dp() {
        composeRule.onNodeWithTag("new_game").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }
}
