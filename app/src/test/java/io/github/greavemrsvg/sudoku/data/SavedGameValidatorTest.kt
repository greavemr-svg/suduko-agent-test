@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.data.SaveFixtures.withChar
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.EMPTY
import io.github.greavemrsvg.sudoku.domain.Grid
import io.github.greavemrsvg.sudoku.domain.MAX_GIVENS
import io.github.greavemrsvg.sudoku.domain.MIN_GIVENS
import io.github.greavemrsvg.sudoku.domain.SavedGameValidator
import io.github.greavemrsvg.sudoku.domain.SudokuSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedGameValidatorTest {
    private val dto = SaveFixtures.dto
    private val puzzle = SaveFixtures.puzzle
    private val solution = dto.solution
    private val givenIndex = (0 until CELL_COUNT).first { puzzle.isGiven(it) }
    private val openIndex = SaveFixtures.openCells[2]

    private fun rejects(bad: SavedGameDto) = assertNull(SavedGameValidator.validate(bad))

    @Test
    fun AC15_valid_dto_restores_entries_and_mistakes() {
        assertEquals(SaveFixtures.inProgress, SavedGameValidator.validate(dto))
    }

    @Test
    fun AC16_solved_dto_restores_solved_state() {
        val restored = SavedGameValidator.validate(SavedGameCodec.toDto(SaveFixtures.solved))
        assertEquals(true, restored?.isSolved)
    }

    @Test
    fun AC18_rejects_wrong_version() {
        rejects(dto.copy(version = 0))
        rejects(dto.copy(version = 2))
    }

    @Test
    fun AC18_rejects_wrong_length_of_each_grid() {
        rejects(dto.copy(givens = dto.givens.dropLast(1)))
        rejects(dto.copy(givens = dto.givens + "0"))
        rejects(dto.copy(solution = solution.dropLast(1)))
        rejects(dto.copy(solution = solution + "1"))
        rejects(dto.copy(entries = dto.entries.dropLast(1)))
        rejects(dto.copy(entries = dto.entries + "0"))
        rejects(dto.copy(entries = ""))
    }

    @Test
    fun AC18_rejects_characters_outside_the_allowed_set() {
        rejects(dto.copy(givens = dto.givens.withChar(openIndex, 'x')))
        rejects(dto.copy(solution = solution.withChar(0, 'a')))
        rejects(dto.copy(entries = dto.entries.withChar(openIndex, '-')))
        rejects(dto.copy(entries = dto.entries.withChar(openIndex, ' ')))
        rejects(dto.copy(entries = dto.entries.withChar(openIndex, '٣')))
    }

    @Test
    fun AC18_rejects_solution_with_an_empty_cell() {
        rejects(dto.copy(solution = solution.withChar(openIndex, '0')))
    }

    @Test
    fun AC18_rejects_solution_that_is_not_a_valid_sudoku() {
        val a = solution[0]
        val b = solution[1]
        rejects(dto.copy(solution = solution.withChar(0, b).withChar(1, a)))
    }

    @Test
    fun AC18_rejects_invalid_solution_even_when_givens_agree_with_it() {
        // Swap two open cells of one row: the row stays valid, two columns break, givens still match.
        val i = SaveFixtures.openCells.first { it % 9 < 8 && !puzzle.isGiven(it + 1) && solution[it] != solution[it + 1] }
        val a = solution[i]
        val b = solution[i + 1]
        rejects(dto.copy(solution = solution.withChar(i, b).withChar(i + 1, a), entries = "0".repeat(CELL_COUNT)))
    }

    @Test
    fun AC18_rejects_solution_with_a_repeated_digit_in_a_row() {
        rejects(dto.copy(solution = solution.withChar(0, solution[1]), givens = dto.givens.withChar(0, '0')))
    }

    @Test
    fun AC18_rejects_given_that_differs_from_solution() {
        val other = if (dto.givens[givenIndex] == '1') '2' else '1'
        rejects(dto.copy(givens = dto.givens.withChar(givenIndex, other)))
    }

    @Test
    fun AC18_rejects_uniquely_solvable_givens_that_belong_to_a_different_solution() {
        // Swapping digits 1 and 2 in the givens gives a count-valid, uniquely solvable puzzle for another grid.
        val swapped = dto.givens.map { if (it == '1') '2' else if (it == '2') '1' else it }.joinToString("")
        assertEquals(1, SudokuSolver().countSolutions(Grid(IntArray(CELL_COUNT) { swapped[it] - '0' }), 2))
        rejects(dto.copy(givens = swapped, entries = "0".repeat(CELL_COUNT), solved = false))
    }

    @Test
    fun AC18_rejects_too_few_givens() {
        // Greedily blank solution cells while the puzzle stays uniquely solvable, stopping at one below the minimum.
        val cells = IntArray(CELL_COUNT) { solution[it] - '0' }
        var filled = CELL_COUNT
        for (i in 0 until CELL_COUNT) {
            if (filled == MIN_GIVENS - 1) break
            val kept = cells[i]
            cells[i] = EMPTY
            if (SudokuSolver().countSolutions(Grid(cells), 2) == 1) filled-- else cells[i] = kept
        }
        assertEquals(MIN_GIVENS - 1, filled)
        assertEquals(1, SudokuSolver().countSolutions(Grid(cells), 2))
        rejects(dto.copy(givens = cells.joinToString(""), entries = "0".repeat(CELL_COUNT), solved = false))
    }

    @Test
    fun AC18_rejects_too_many_givens() {
        // Fixture puzzle plus solution digits at open cells until one above the maximum; stays uniquely solvable.
        val cells = IntArray(CELL_COUNT) { dto.givens[it] - '0' }
        var filled = cells.count { it != EMPTY }
        for (i in SaveFixtures.openCells) {
            if (filled == MAX_GIVENS + 1) break
            cells[i] = solution[i] - '0'
            filled++
        }
        assertEquals(MAX_GIVENS + 1, filled)
        assertEquals(1, SudokuSolver().countSolutions(Grid(cells), 2))
        rejects(dto.copy(givens = cells.joinToString(""), entries = "0".repeat(CELL_COUNT), solved = false))
    }

    @Test
    fun AC18_rejects_givens_without_a_unique_solution() {
        // The first four rows (36 givens, inside the count range) leave the other five rows with many completions.
        val loose = solution.mapIndexed { i, c -> if (i < MIN_GIVENS) c else '0' }.joinToString("")
        assertEquals(2, SudokuSolver().countSolutions(Grid(IntArray(CELL_COUNT) { loose[it] - '0' }), 2))
        rejects(dto.copy(givens = loose, entries = "0".repeat(CELL_COUNT), solved = false))
    }

    @Test
    fun AC18_rejects_entry_on_a_given_cell() {
        rejects(dto.copy(entries = dto.entries.withChar(givenIndex, solution[givenIndex])))
    }

    @Test
    fun AC18_rejects_solved_true_when_not_solved() {
        rejects(dto.copy(solved = true))
    }

    @Test
    fun AC18_rejects_solved_false_when_solved() {
        rejects(SavedGameCodec.toDto(SaveFixtures.solved).copy(solved = false))
    }
}
