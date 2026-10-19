package io.github.greavemrsvg.sudoku

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.sudoku.ui.GRID_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Launch smoke test on the real [SudokuApplication] with the production [AppContainer] defaults
 * (real dispatchers, no seeded randomness): the puzzle grid appears.
 */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = SudokuApplication::class)
class LaunchSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_with_production_container_shows_the_grid() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(GRID_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
