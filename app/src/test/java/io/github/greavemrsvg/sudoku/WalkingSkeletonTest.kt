package io.github.greavemrsvg.sudoku

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test for the walking skeleton: the app starts and the themed placeholder screen renders. */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
class WalkingSkeletonTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun skeleton_application_creates_container() {
        val app = ApplicationProvider.getApplicationContext<Context>() as SudokuApplication
        assertNotNull(app.container)
        assertTrue(
            app.container.applicationScope.coroutineContext[kotlinx.coroutines.Job]!!
                .isActive,
        )
    }

    @Test
    fun skeleton_main_activity_shows_themed_placeholder_with_app_name() {
        val name = composeRule.activity.getString(R.string.app_name)
        composeRule.onNodeWithText(name).assertIsDisplayed()
    }
}
