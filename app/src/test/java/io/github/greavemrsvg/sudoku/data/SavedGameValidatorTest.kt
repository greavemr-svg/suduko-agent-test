@file:Suppress("ktlint:standard:function-naming")

package io.github.greavemrsvg.sudoku.data

import io.github.greavemrsvg.sudoku.data.SaveFixtures.withChar
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.SavedGameValidator
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
    fun AC18_rejects_given_that_differs_from_solution() {
        val other = if (dto.givens[givenIndex] == '1') '2' else '1'
        rejects(dto.copy(givens = dto.givens.withChar(givenIndex, other)))
    }

    @Test
    fun AC18_rejects_too_few_givens() {
        val few = solution.mapIndexed { i, c -> if (i < MIN_GIVEN_ROWS_CELLS - 1) c else '0' }.joinToString("")
        rejects(dto.copy(givens = few, entries = "0".repeat(CELL_COUNT), solved = false))
    }

    @Test
    fun AC18_rejects_too_many_givens() {
        val many = solution.mapIndexed { i, c -> if (i < MAX_GIVEN_CELLS + 1) c else '0' }.joinToString("")
        rejects(dto.copy(givens = many, entries = "0".repeat(CELL_COUNT), solved = false))
    }

    @Test
    fun AC18_rejects_givens_without_a_unique_solution() {
        // The first four rows (36 givens) leave the other five rows with many completions.
        val loose = solution.mapIndexed { i, c -> if (i < MIN_GIVEN_ROWS_CELLS) c else '0' }.joinToString("")
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

    private companion object {
        const val MIN_GIVEN_ROWS_CELLS = 36
        const val MAX_GIVEN_CELLS = 46
    }
}
