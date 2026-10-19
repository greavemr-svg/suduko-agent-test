@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzleGeneratorTest {
    private val seeds = 1..SEED_COUNT

    private fun generate(seed: Int): Puzzle = PuzzleGenerator(Random(seed), SudokuSolver()).generate()

    @Test
    fun AC2_every_generated_puzzle_has_exactly_one_solution() {
        val checker = SudokuSolver()
        for (seed in seeds) {
            val puzzle = generate(seed)
            assertEquals("seed $seed", 1, checker.countSolutions(puzzle.givens, 2))
        }
    }

    @Test
    fun AC3_generated_puzzles_have_36_to_46_consistent_givens_and_valid_solution() {
        for (seed in seeds) {
            val puzzle = generate(seed)
            assertTrue("seed $seed solution invalid", puzzle.solution.isValidSolution())
            assertTrue("seed $seed givens ${puzzle.givens.filledCount}", puzzle.givens.filledCount in MIN_GIVENS..MAX_GIVENS)
            for (i in 0 until CELL_COUNT) {
                val given = puzzle.givens.get(i)
                assertTrue("seed $seed cell $i", given == EMPTY || given == puzzle.solution.get(i))
            }
        }
    }

    @Test
    fun same_seed_gives_same_puzzle_and_different_seeds_differ() {
        assertEquals(generate(7), generate(7))
        assertTrue(generate(7) != generate(8))
    }

    private companion object {
        const val SEED_COUNT = 120
    }
}
