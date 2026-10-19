package io.github.greavemrsvg.sudoku.domain

import io.github.greavemrsvg.sudoku.domain.TestPuzzles.GIVEN
import io.github.greavemrsvg.sudoku.domain.TestPuzzles.OPEN
import io.github.greavemrsvg.sudoku.domain.TestPuzzles.puzzle
import io.github.greavemrsvg.sudoku.domain.TestPuzzles.solution
import io.github.greavemrsvg.sudoku.domain.TestPuzzles.wrong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("ktlint:standard:function-naming")
class GameRulesTest {
    private fun fresh() = GameRules.newGame(puzzle)

    private fun enter(
        state: GameState,
        index: Int,
        digit: Int,
    ) = GameRules.enterDigit(GameRules.select(state, index), digit)

    private fun fillAllCorrect(): GameState {
        var s = fresh()
        for (i in 0 until CELL_COUNT) if (!puzzle.isGiven(i)) s = enter(s, i, solution.get(i))
        return s
    }

    @Test
    fun newGame_has_no_entries_and_no_selection() {
        val s = fresh()
        assertEquals(0, s.entries.filledCount)
        assertNull(s.selected)
        assertFalse(s.isSolved)
        assertFalse(s.isFull)
        assertEquals(solution.get(GIVEN), s.digitAt(GIVEN))
        assertEquals(EMPTY, s.digitAt(OPEN))
    }

    @Test
    fun AC7_entering_a_different_digit_replaces_the_old_one() {
        val first = enter(fresh(), OPEN, 4)
        assertEquals(4, first.digitAt(OPEN))
        val second = GameRules.enterDigit(first, 6)
        assertEquals(6, second.digitAt(OPEN))
        assertEquals(1, second.entries.filledCount)
    }

    @Test
    fun AC8_digit_on_a_given_cell_leaves_it_unchanged() {
        val s = GameRules.select(fresh(), GIVEN)
        assertEquals(GIVEN, s.selected)
        val after = GameRules.enterDigit(s, wrong(GIVEN))
        assertEquals(solution.get(GIVEN), after.digitAt(GIVEN))
        assertEquals(EMPTY, after.entries.get(GIVEN))
        assertEquals(s, after)
    }

    @Test
    fun AC8_erase_on_a_given_cell_leaves_it_unchanged() {
        val s = GameRules.select(fresh(), GIVEN)
        val after = GameRules.erase(s)
        assertEquals(solution.get(GIVEN), after.digitAt(GIVEN))
        assertEquals(s, after)
    }

    @Test
    fun AC10_entering_the_solution_digit_clears_a_mistake() {
        val mistaken = enter(fresh(), OPEN, wrong(OPEN))
        assertTrue(mistaken.isMistake(OPEN))
        val fixed = GameRules.enterDigit(mistaken, solution.get(OPEN))
        assertEquals(solution.get(OPEN), fixed.digitAt(OPEN))
        assertFalse(fixed.isMistake(OPEN))
    }

    @Test
    fun AC10_solution_digit_in_an_empty_cell_is_not_a_mistake() {
        val s = enter(fresh(), OPEN, solution.get(OPEN))
        assertFalse(s.isMistake(OPEN))
        assertFalse(fresh().isMistake(OPEN))
    }

    @Test
    fun AC13_full_board_with_a_mistake_is_not_solved_and_stays_editable() {
        var s = fresh()
        for (i in 0 until CELL_COUNT) {
            if (!puzzle.isGiven(i)) s = enter(s, i, if (i == OPEN) wrong(i) else solution.get(i))
        }
        assertTrue(s.isFull)
        assertFalse(s.isSolved)
        assertTrue(s.isMistake(OPEN))
        val erased = GameRules.erase(GameRules.select(s, OPEN))
        assertEquals(EMPTY, erased.digitAt(OPEN))
        val fixed = enter(s, OPEN, solution.get(OPEN))
        assertEquals(solution.get(OPEN), fixed.digitAt(OPEN))
        assertTrue(fixed.isSolved)
    }

    @Test
    fun AC7_replacing_a_wrong_digit_with_another_wrong_digit_keeps_it_a_mistake_and_replaces_it() {
        val a = wrong(OPEN)
        val b = (a % 9) + 1 // a different digit
        val first = enter(fresh(), OPEN, a)
        val second = GameRules.enterDigit(first, b)
        assertEquals(b, second.digitAt(OPEN))
        assertEquals(b != solution.get(OPEN), second.isMistake(OPEN))
    }

    @Test
    fun AC7_replacing_a_digit_only_changes_the_selected_cell() {
        val base = enter(enter(fresh(), 3, 5), OPEN, 4)
        val after = GameRules.enterDigit(base, 7)
        for (i in 0 until CELL_COUNT) {
            if (i != OPEN) assertEquals(base.digitAt(i), after.digitAt(i))
        }
        assertEquals(7, after.digitAt(OPEN))
    }

    @Test
    fun AC8_every_given_cell_rejects_digits_and_erase() {
        for (g in 0 until CELL_COUNT) {
            if (!puzzle.isGiven(g)) continue
            val s = GameRules.select(fresh(), g)
            for (d in 1..9) assertEquals(s, GameRules.enterDigit(s, d))
            assertEquals(s, GameRules.erase(s))
            assertEquals(solution.get(g), s.digitAt(g))
        }
    }

    @Test
    fun AC8_selecting_a_given_after_an_open_cell_does_not_edit_the_open_cell() {
        val base = enter(fresh(), OPEN, 4)
        val onGiven = GameRules.select(base, GIVEN)
        val after = GameRules.erase(GameRules.enterDigit(onGiven, 9))
        assertEquals(4, after.digitAt(OPEN))
        assertEquals(base.entries, after.entries)
    }

    @Test
    fun AC10_mistake_flag_is_independent_per_cell_when_one_is_corrected() {
        val third = 3
        var s = enter(fresh(), OPEN, wrong(OPEN))
        s = enter(s, third, wrong(third))
        s = GameRules.enterDigit(GameRules.select(s, OPEN), solution.get(OPEN))
        assertFalse(s.isMistake(OPEN))
        assertTrue(s.isMistake(third))
    }

    @Test
    fun AC10_every_wrong_digit_is_a_mistake_and_only_the_solution_digit_is_not() {
        for (d in 1..9) {
            val s = enter(fresh(), OPEN, d)
            assertEquals(d != solution.get(OPEN), s.isMistake(OPEN))
        }
    }

    @Test
    fun AC13_all_mistaken_cells_are_flagged_when_the_board_is_full() {
        val wrongCells = setOf(1, 5, 79)
        var s = fresh()
        for (i in 0 until CELL_COUNT) {
            if (!puzzle.isGiven(i)) s = enter(s, i, if (i in wrongCells) wrong(i) else solution.get(i))
        }
        assertTrue(s.isFull)
        assertFalse(s.isSolved)
        for (i in 0 until CELL_COUNT) assertEquals(i in wrongCells, s.isMistake(i))
    }

    @Test
    fun AC13_full_board_with_a_mistake_still_accepts_digit_entry_and_selection() {
        var s = fresh()
        for (i in 0 until CELL_COUNT) {
            if (!puzzle.isGiven(i)) s = enter(s, i, if (i == OPEN) wrong(i) else solution.get(i))
        }
        val other = enter(s, 3, wrong(3))
        assertEquals(wrong(3), other.digitAt(3))
        assertTrue(other.isMistake(3))
        assertFalse(other.isSolved)
        assertEquals(5, GameRules.select(s, 5).selected)
    }

    @Test
    fun solved_is_false_for_an_empty_board_and_a_board_missing_one_cell() {
        assertFalse(fresh().isSolved)
        val almost = GameRules.erase(GameRules.select(fillAllCorrect(), OPEN))
        // solved board is read-only, so build the almost-solved board directly
        assertTrue(almost.isSolved)
        var s = fresh()
        for (i in 0 until CELL_COUNT) if (!puzzle.isGiven(i) && i != OPEN) s = enter(s, i, solution.get(i))
        assertFalse(s.isSolved)
        assertFalse(s.isFull)
        assertTrue(enter(s, OPEN, solution.get(OPEN)).isSolved)
    }

    @Test
    fun select_does_not_change_entries() {
        val base = enter(fresh(), OPEN, 4)
        assertEquals(base.entries, GameRules.select(base, 7).entries)
    }

    @Test
    fun erase_clears_an_entry() {
        val s = GameRules.erase(enter(fresh(), OPEN, 5))
        assertEquals(EMPTY, s.digitAt(OPEN))
    }

    @Test
    fun enter_and_erase_are_no_ops_without_a_selection() {
        val s = fresh()
        assertSame(s, GameRules.enterDigit(s, 5))
        assertSame(s, GameRules.erase(s))
    }

    @Test
    fun solved_board_is_read_only() {
        val solved = fillAllCorrect()
        assertTrue(solved.isSolved)
        assertTrue(solved.isFull)
        val sel = GameRules.select(solved, OPEN)
        assertEquals(sel, GameRules.enterDigit(sel, wrong(OPEN)))
        assertEquals(sel, GameRules.erase(sel))
    }

    @Test
    fun select_works_on_any_cell() {
        assertEquals(GIVEN, GameRules.select(fresh(), GIVEN).selected)
        assertEquals(80, GameRules.select(fresh(), 80).selected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun select_rejects_negative_index() {
        GameRules.select(fresh(), -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun select_rejects_index_81() {
        GameRules.select(fresh(), CELL_COUNT)
    }

    @Test(expected = IllegalArgumentException::class)
    fun enterDigit_rejects_zero() {
        GameRules.enterDigit(fresh(), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun enterDigit_rejects_ten() {
        GameRules.enterDigit(fresh(), 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun gameState_rejects_entry_on_a_given() {
        GameState(puzzle, Grid(IntArray(CELL_COUNT)).with(GIVEN, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun gameState_rejects_out_of_range_selection() {
        GameState(puzzle, Grid(IntArray(CELL_COUNT)), 81)
    }
}
