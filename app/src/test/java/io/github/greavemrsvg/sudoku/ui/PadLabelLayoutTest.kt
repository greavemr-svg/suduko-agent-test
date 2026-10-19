package io.github.greavemrsvg.sudoku.ui

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

/**
 * Regression test for the Erase label wrapping at 360 dp. Robolectric does not measure text realistically, so
 * this asserts the configured layout: the width left for the label inside each pad button.
 */
@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
@Config(application = TestSudokuApplication::class, qualifiers = "w360dp-h640dp-port-xhdpi")
class PadLabelLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val density get() = composeRule.activity.resources.displayMetrics.density

    private fun labelWidthDp(tag: String): Float {
        val width =
            composeRule
                .onNodeWithTag(tag)
                .fetchSemanticsNode()
                .size.width / density
        return width - PadContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr).value -
            PadContentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr).value
    }

    @Test
    fun NFR_pad_content_padding_is_compact() {
        assertTrue(PadContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) <= 8.dp)
        assertEquals(
            PadContentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            PadContentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        )
    }

    @Test
    fun NFR_erase_label_has_room_for_one_line_at_360dp() {
        val available = labelWidthDp("erase")
        assertTrue("Erase label width $available dp < $MIN_ERASE_LABEL_DP dp", available >= MIN_ERASE_LABEL_DP)
    }

    @Test
    fun NFR_digit_label_has_room_at_360dp() {
        for (digit in 1..GRID_SIZE) {
            val available = labelWidthDp("digit_$digit")
            assertTrue("digit $digit label width $available dp", available >= MIN_DIGIT_LABEL_DP)
        }
    }

    @Test
    fun NFR_pad_font_scale_is_capped() {
        assertTrue(MAX_PAD_FONT_SCALE <= 1.5f)
    }

    private companion object {
        /** "Erase" at the capped label size (14 sp x 1.3) needs about 50 dp. */
        const val MIN_ERASE_LABEL_DP = 50f
        const val MIN_DIGIT_LABEL_DP = 24f
    }
}
