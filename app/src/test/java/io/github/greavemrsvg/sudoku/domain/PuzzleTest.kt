package io.github.greavemrsvg.sudoku.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleTest {
    @Test
    fun isGiven_reflects_givens() {
        assertTrue(TestPuzzles.puzzle.isGiven(TestPuzzles.GIVEN))
        assertFalse(TestPuzzles.puzzle.isGiven(TestPuzzles.OPEN))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalid_solution_is_rejected() {
        Puzzle(Grid(IntArray(CELL_COUNT)), Grid(IntArray(CELL_COUNT)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun given_differing_from_solution_is_rejected() {
        val bad = Grid(IntArray(CELL_COUNT)).with(0, TestPuzzles.wrong(0))
        Puzzle(bad, TestPuzzles.solution)
    }

    @Test(expected = IllegalArgumentException::class)
    fun isGiven_rejects_bad_index() {
        TestPuzzles.puzzle.isGiven(CELL_COUNT)
    }
}
