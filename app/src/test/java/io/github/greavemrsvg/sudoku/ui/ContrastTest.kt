package io.github.greavemrsvg.sudoku.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.greavemrsvg.sudoku.ui.theme.selectedCellTint
import org.junit.Assert.assertTrue
import org.junit.Test

/** Accessibility NFR: WCAG AA contrast for grid text on the cell backgrounds, in light and dark themes. */
@Suppress("ktlint:standard:function-naming")
class ContrastTest {
    private fun ratio(
        a: Color,
        b: Color,
    ): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun check(
        name: String,
        scheme: ColorScheme,
    ) {
        val backgrounds = mapOf("surface" to scheme.surface, "background" to scheme.background, "selected" to scheme.selectedCellTint())
        val texts = mapOf("given(onSurface)" to scheme.onSurface, "entry(primary)" to scheme.primary, "mistake(error)" to scheme.error)
        for ((bgName, bg) in backgrounds) {
            for ((textName, fg) in texts) {
                val r = ratio(fg, bg)
                assertTrue("$name $textName on $bgName contrast $r < 4.5", r >= 4.5)
            }
        }
        // Mistake bar and selected border are graphical objects: 3:1 against the cell background.
        assertTrue("$name mistake bar", ratio(scheme.error, scheme.selectedCellTint()) >= 3.0)
        assertTrue("$name selected border", ratio(scheme.primary, scheme.surface) >= 3.0)
        assertTrue("$name box line", ratio(scheme.onSurface, scheme.surface) >= 3.0)
    }

    @Test
    fun NFR_light_theme_grid_text_meets_wcag_aa_contrast() = check("light", lightColorScheme())

    @Test
    fun NFR_dark_theme_grid_text_meets_wcag_aa_contrast() = check("dark", darkColorScheme())
}
