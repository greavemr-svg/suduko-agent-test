@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Checks that do not rely on the production validity helpers or on the generator's own solver instance. */
class GeneratorIndependentChecksTest {
    private fun generate(seed: Int): Puzzle = PuzzleGenerator(Random(seed), SudokuSolver()).generate()

    private fun units(): List<List<Int>> {
        val rows = (0 until 9).map { r -> (0 until 9).map { r * 9 + it } }
        val cols = (0 until 9).map { c -> (0 until 9).map { it * 9 + c } }
        val boxes =
            (0 until 9).map { b ->
                val r0 = b / 3 * 3
                val c0 = b % 3 * 3
                (0 until 9).map { (r0 + it / 3) * 9 + c0 + it % 3 }
            }
        return rows + cols + boxes
    }

    private fun isPermutation(
        grid: Grid,
        unit: List<Int>,
    ) = unit.map { grid.get(it) }.sorted() == (1..9).toList()

    @Test
    fun AC3_solution_rows_columns_and_boxes_are_each_a_permutation_of_1_to_9() {
        for (seed in 1..100) {
            val solution = generate(seed).solution
            for (unit in units()) assertTrue("seed $seed unit $unit", isPermutation(solution, unit))
        }
    }

    @Test
    fun AC3_given_count_in_range_and_every_given_equals_solution_digit() {
        for (seed in 1..100) {
            val puzzle = generate(seed)
            var count = 0
            for (i in 0 until 81) {
                val g = puzzle.givens.get(i)
                if (g != 0) {
                    count++
                    assertEquals("seed $seed cell $i", puzzle.solution.get(i), g)
                }
            }
            assertTrue("seed $seed givens $count", count in 36..46)
        }
    }

    @Test
    fun AC2_puzzle_has_exactly_one_solution_using_fresh_solver_and_it_is_the_stored_solution() {
        for (seed in 1..100) {
            val puzzle = generate(seed)
            val fresh = SudokuSolver()
            assertEquals("seed $seed", 1, fresh.countSolutions(puzzle.givens, 2))
            assertEquals("seed $seed", 1, fresh.countSolutions(puzzle.givens, 10))
            // The stored solution must be the one solution: it solves the puzzle uniquely.
            assertEquals(1, fresh.countSolutions(puzzle.solution, 2))
        }
    }

    @Test
    fun AC2_uniqueness_is_sensitive_removing_a_row_of_givens_makes_it_ambiguous() {
        val puzzle = generate(3)
        val cells = IntArray(81) { puzzle.solution.get(it) }
        for (i in 0 until 27) cells[i] = 0
        assertEquals(2, SudokuSolver().countSolutions(Grid(cells), 2))
    }

    @Test
    fun generation_is_deterministic_for_a_seed() {
        for (seed in listOf(1, 42, 2024)) {
            val a = generate(seed)
            val b = generate(seed)
            assertEquals(a.givens.toString(), b.givens.toString())
            assertEquals(a.solution.toString(), b.solution.toString())
        }
    }

    @Test
    fun solver_already_solved_grid_has_exactly_one_solution_for_any_limit() {
        assertEquals(1, SudokuSolver().countSolutions(TestPuzzles.solution, 1))
        assertEquals(1, SudokuSolver().countSolutions(TestPuzzles.solution, 50))
    }

    @Test
    fun solver_conflicting_givens_in_column_and_box_return_zero() {
        val col =
            IntArray(81).also {
                it[0] = 4
                it[9] = 4
            }
        val box =
            IntArray(81).also {
                it[0] = 4
                it[10] = 4
            }
        assertEquals(0, SudokuSolver().countSolutions(Grid(col)))
        assertEquals(0, SudokuSolver().countSolutions(Grid(box)))
    }

    @Test
    fun solver_limit_one_exits_early_with_fewer_nodes_than_a_larger_limit() {
        val empty = Grid(IntArray(81))
        val solver = SudokuSolver()
        assertEquals(1, solver.countSolutions(empty, 1))
        val one = solver.lastSearchNodes
        assertEquals(100, solver.countSolutions(empty, 100))
        assertTrue(one < solver.lastSearchNodes)
        // Early exit: finding one solution of an empty grid takes about one node per cell, not a full search.
        assertTrue("nodes $one", one <= 200)
    }

    @Test
    fun solver_does_not_mutate_the_input_grid() {
        val before = TestPuzzles.puzzle.givens.toString()
        SudokuSolver().countSolutions(TestPuzzles.puzzle.givens, 2)
        assertEquals(before, TestPuzzles.puzzle.givens.toString())
        assertFalse(
            TestPuzzles.puzzle.givens
                .toString()
                .isEmpty(),
        )
    }
}
