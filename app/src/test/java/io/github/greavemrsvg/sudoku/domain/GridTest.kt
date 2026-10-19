package io.github.greavemrsvg.sudoku.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridTest {
    @Test
    fun valid_solution_is_accepted() {
        assertTrue(TestPuzzles.solution.isValidSolution())
    }

    @Test
    fun grid_with_empty_cell_is_not_a_solution() {
        assertFalse(TestPuzzles.solution.with(5, EMPTY).isValidSolution())
    }

    @Test
    fun row_duplicate_is_not_a_solution() {
        val g = TestPuzzles.solution.with(1, TestPuzzles.solution.get(0))
        assertFalse(g.isValidSolution())
    }

    @Test
    fun swapped_cells_break_columns_and_boxes() {
        val s = TestPuzzles.solution
        val swapped = s.with(0, s.get(1)).with(1, s.get(0))
        assertFalse(swapped.isValidSolution())
    }

    @Test
    fun with_returns_new_grid_and_leaves_original() {
        val g = Grid(IntArray(CELL_COUNT))
        val h = g.with(3, 7)
        assertEquals(0, g.get(3))
        assertEquals(7, h.get(3))
        assertEquals(1, h.filledCount)
        assertEquals(0, g.filledCount)
    }

    @Test
    fun constructor_copies_input() {
        val raw = IntArray(CELL_COUNT)
        val g = Grid(raw)
        raw[0] = 5
        assertEquals(0, g.get(0))
    }

    @Test
    fun equality_is_by_content() {
        assertEquals(Grid(IntArray(CELL_COUNT)), Grid(IntArray(CELL_COUNT)))
        assertEquals(Grid(IntArray(CELL_COUNT)).hashCode(), Grid(IntArray(CELL_COUNT)).hashCode())
        assertNotEquals(Grid(IntArray(CELL_COUNT)), Grid(IntArray(CELL_COUNT)).with(0, 1))
        assertEquals("0".repeat(CELL_COUNT), Grid(IntArray(CELL_COUNT)).toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrong_size_is_rejected() {
        Grid(IntArray(80))
    }

    @Test(expected = IllegalArgumentException::class)
    fun out_of_range_cell_value_is_rejected() {
        Grid(IntArray(CELL_COUNT) { 10 })
    }

    @Test(expected = IllegalArgumentException::class)
    fun get_rejects_negative_index() {
        TestPuzzles.solution.get(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun get_rejects_index_81() {
        TestPuzzles.solution.get(CELL_COUNT)
    }

    @Test(expected = IllegalArgumentException::class)
    fun with_rejects_digit_10() {
        TestPuzzles.solution.with(0, 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun with_rejects_bad_index() {
        TestPuzzles.solution.with(81, 1)
    }
}
