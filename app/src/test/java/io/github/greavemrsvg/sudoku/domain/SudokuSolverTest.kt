package io.github.greavemrsvg.sudoku.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuSolverTest {
    private val solver = SudokuSolver()

    @Test
    fun full_valid_grid_has_one_solution() {
        assertEquals(1, solver.countSolutions(TestPuzzles.solution))
    }

    @Test
    fun empty_grid_stops_at_limit() {
        val empty = Grid(IntArray(CELL_COUNT))
        assertEquals(2, solver.countSolutions(empty, 2))
        assertEquals(5, solver.countSolutions(empty, 5))
        assertTrue(solver.lastSearchNodes > 0)
    }

    @Test
    fun uniquely_solvable_puzzle_has_one_solution() {
        assertEquals(1, solver.countSolutions(Grid(IntArray(CELL_COUNT) { i -> if (i % 9 == 0) EMPTY else TestPuzzles.solution.get(i) })))
    }

    @Test
    fun ambiguous_puzzle_has_two_solutions() {
        val cells = IntArray(CELL_COUNT) { if (it < 27) EMPTY else TestPuzzles.solution.get(it) }
        assertEquals(2, solver.countSolutions(Grid(cells), 2))
    }

    @Test
    fun contradictory_givens_have_no_solution() {
        val cells = IntArray(CELL_COUNT)
        cells[0] = 5
        cells[1] = 5
        assertEquals(0, solver.countSolutions(Grid(cells)))
    }

    @Test
    fun dead_end_puzzle_has_no_solution() {
        // Cell 0 sees digits 1..8 in its row and 9 in its column, so it has no candidate.
        val cells = IntArray(CELL_COUNT)
        for (k in 1..8) cells[k] = k
        cells[9] = 9
        assertEquals(0, solver.countSolutions(Grid(cells)))
    }

    @Test
    fun node_counters_accumulate() {
        val fresh = SudokuSolver()
        fresh.countSolutions(TestPuzzles.solution)
        val first = fresh.lastSearchNodes
        fresh.countSolutions(TestPuzzles.solution)
        assertEquals(first * 2, fresh.totalSearchNodes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun limit_below_one_is_rejected() {
        solver.countSolutions(TestPuzzles.solution, 0)
    }
}
