package io.github.greavemrsvg.sudoku.ui

import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.EMPTY
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import io.github.greavemrsvg.sudoku.domain.Puzzle
import io.github.greavemrsvg.sudoku.domain.PuzzleGenerator
import io.github.greavemrsvg.sudoku.domain.SudokuSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val seed = 7

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = GameViewModel(PuzzleGenerator(Random(seed), SudokuSolver()), UnconfinedTestDispatcher())

    private fun GameViewModel.ready() = uiState.value as GameUiState.Ready

    private fun GameViewModel.selectedIndexes() = ready().cells.indices.filter { ready().cells[it].isSelected }

    private fun puzzle(): Puzzle = PuzzleGenerator(Random(seed), SudokuSolver()).generate()

    private fun Puzzle.firstOpenIndex() = (0 until CELL_COUNT).first { !isGiven(it) }

    private fun Puzzle.wrongDigitAt(index: Int) = solution.get(index) % GRID_SIZE + 1

    @Test
    fun AC1_starts_loading_then_shows_generated_puzzle() =
        runTest {
            val vm = GameViewModel(PuzzleGenerator(Random(seed), SudokuSolver()), StandardTestDispatcher(testScheduler))
            assertSame(GameUiState.Loading, vm.uiState.value)
            testScheduler.advanceUntilIdle()
            val ready = vm.ready()
            assertEquals(CELL_COUNT, ready.cells.size)
            assertEquals(puzzle().givens.filledCount, ready.cells.count { it.digit != EMPTY })
            assertTrue(ready.cells.all { it.isGiven == (it.digit != EMPTY) })
            assertFalse(ready.isSolved)
        }

    @Test
    fun AC5_tapping_a_cell_selects_it_and_deselects_the_previous_one() {
        val vm = viewModel()
        val open = puzzle().firstOpenIndex()
        vm.onCellTapped(open)
        assertEquals(listOf(open), vm.selectedIndexes())
        vm.onCellTapped(open + 1)
        assertEquals(listOf(open + 1), vm.selectedIndexes())
    }

    @Test
    fun AC6_digit_appears_in_selected_cell() {
        val vm = viewModel()
        val open = puzzle().firstOpenIndex()
        vm.onCellTapped(open)
        vm.onDigit(5)
        assertEquals(5, vm.ready().cells[open].digit)
    }

    @Test
    fun AC9_wrong_digit_is_a_mistake_and_correct_digit_is_not() {
        val puzzle = puzzle()
        val vm = viewModel()
        val open = puzzle.firstOpenIndex()
        vm.onCellTapped(open)
        vm.onDigit(puzzle.wrongDigitAt(open))
        assertTrue(vm.ready().cells[open].isMistake)
        vm.onDigit(puzzle.solution.get(open))
        assertFalse(vm.ready().cells[open].isMistake)
    }

    @Test
    fun AC11_erase_clears_entry_and_mistake() {
        val puzzle = puzzle()
        val vm = viewModel()
        val open = puzzle.firstOpenIndex()
        vm.onCellTapped(open)
        vm.onDigit(puzzle.wrongDigitAt(open))
        vm.onErase()
        val cell = vm.ready().cells[open]
        assertEquals(EMPTY, cell.digit)
        assertFalse(cell.isMistake)
    }

    @Test
    fun AC8_digit_and_erase_on_a_given_cell_change_nothing() {
        val puzzle = puzzle()
        val vm = viewModel()
        val given = (0 until CELL_COUNT).first { puzzle.isGiven(it) }
        vm.onCellTapped(given)
        vm.onDigit(puzzle.givens.get(given) % GRID_SIZE + 1)
        vm.onErase()
        assertEquals(puzzle.givens.get(given), vm.ready().cells[given].digit)
    }

    @Test
    fun input_before_the_puzzle_is_ready_is_ignored() =
        runTest {
            val vm = GameViewModel(PuzzleGenerator(Random(seed), SudokuSolver()), StandardTestDispatcher(testScheduler))
            vm.onCellTapped(0)
            vm.onDigit(1)
            vm.onErase()
            assertSame(GameUiState.Loading, vm.uiState.value)
        }
}
