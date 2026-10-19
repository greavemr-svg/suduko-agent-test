package io.github.greavemrsvg.sudoku.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.MainActivity
import io.github.greavemrsvg.sudoku.TestSudokuApplication
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import io.github.greavemrsvg.sudoku.ui.theme.SudokuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Accessibility NFR: at the largest system font, grid digits stay inside their cells and controls keep 48 dp. */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class, qualifiers = "w360dp-h640dp-port-xhdpi")
class LargeFontTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun NFR_digits_do_not_overflow_cells_at_largest_font_scale() {
        val cells =
            List(CELL_COUNT) { i ->
                CellUi(i / GRID_SIZE, i % GRID_SIZE, i % GRID_SIZE + 1, isGiven = i % 2 == 0, isMistake = i % 3 == 0, isSelected = i == 4)
            }
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                    SudokuTheme {
                        GameScreen(GameUiState.Ready(cells, isSolved = false), {}, {}, {}, {})
                    }
                }
            }
        }
        composeRule.waitForIdle()
        for (i in 0 until CELL_COUNT) {
            val node = composeRule.onNodeWithTag(cellTag(i / GRID_SIZE, i % GRID_SIZE), useUnmergedTree = true)
            val semantics = node.fetchSemanticsNode()
            val results = mutableListOf<TextLayoutResult>()
            val action = semantics.children.firstNotNullOfOrNull { it.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action }
            assertTrue("cell $i exposes text layout", action != null && action(results) && results.isNotEmpty())
            val layout = results.first()
            // Robolectric text metrics are not real, so check the cap on the font size itself.
            val fontPx =
                with(layout.layoutInput.density) {
                    layout.layoutInput.style.fontSize
                        .toPx()
                }
            assertTrue(
                "cell $i font ${fontPx}px exceeds 60% of cell height ${semantics.size.height}px",
                fontPx <= semantics.size.height * 0.6f + 1f,
            )
            assertTrue("cell $i digit taller than cell", layout.size.height <= semantics.size.height)
            assertTrue("cell $i digit wider than cell", layout.size.width <= semantics.size.width)
            node.assertWidthIsAtLeast(24.dp).assertHeightIsAtLeast(24.dp)
        }
        for (digit in 1..GRID_SIZE) {
            composeRule.onNodeWithTag("digit_$digit").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithTag("erase").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }
}
