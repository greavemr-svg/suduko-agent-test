package io.github.greavemrsvg.sudoku.domain

import kotlin.random.Random

/** Maximum number of full restarts before generation fails loudly. */
const val MAX_GENERATION_ATTEMPTS = 20

/** Generates easy puzzles with a unique solution and between [MIN_GIVENS] and [MAX_GIVENS] givens. */
class PuzzleGenerator(
    private val random: Random,
    private val solver: SudokuSolver,
) {
    fun generate(): Puzzle {
        repeat(MAX_GENERATION_ATTEMPTS) {
            val solution = fillGrid()
            val target = random.nextInt(MIN_GIVENS, MAX_GIVENS + 1)
            val givens = removeCells(solution, target)
            if (givens != null) return Puzzle(givens, Grid(solution))
        }
        throw IllegalStateException("Could not generate a puzzle in $MAX_GENERATION_ATTEMPTS attempts")
    }

    /** Fills a complete valid grid by randomised backtracking. */
    private fun fillGrid(): IntArray {
        val cells = IntArray(CELL_COUNT)
        check(fill(cells, 0)) { "Grid fill failed" }
        return cells
    }

    private fun fill(
        cells: IntArray,
        index: Int,
    ): Boolean {
        if (index == CELL_COUNT) return true
        val digits = (1..GRID_SIZE).shuffled(random)
        for (digit in digits) {
            if (!fits(cells, index, digit)) continue
            cells[index] = digit
            if (fill(cells, index + 1)) return true
            cells[index] = EMPTY
        }
        return false
    }

    private fun fits(
        cells: IntArray,
        index: Int,
        digit: Int,
    ): Boolean {
        val row = index / GRID_SIZE
        val col = index % GRID_SIZE
        for (k in 0 until GRID_SIZE) {
            if (cells[row * GRID_SIZE + k] == digit || cells[k * GRID_SIZE + col] == digit) return false
        }
        val boxRow = row / BOX_SIZE * BOX_SIZE
        val boxCol = col / BOX_SIZE * BOX_SIZE
        for (r in boxRow until boxRow + BOX_SIZE) {
            for (c in boxCol until boxCol + BOX_SIZE) {
                if (cells[r * GRID_SIZE + c] == digit) return false
            }
        }
        return true
    }

    /** Removes cells in random order while the solution stays unique; null if more than [MAX_GIVENS] givens remain. */
    private fun removeCells(
        solution: IntArray,
        target: Int,
    ): Grid? {
        val cells = solution.copyOf()
        var givens = CELL_COUNT
        for (index in (0 until CELL_COUNT).shuffled(random)) {
            if (givens == target) break
            val digit = cells[index]
            cells[index] = EMPTY
            if (solver.countSolutions(Grid(cells), 2) == 1) givens-- else cells[index] = digit
        }
        return if (givens <= MAX_GIVENS) Grid(cells) else null
    }
}
