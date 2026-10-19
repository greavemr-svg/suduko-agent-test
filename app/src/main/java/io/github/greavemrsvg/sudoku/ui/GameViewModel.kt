package io.github.greavemrsvg.sudoku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.greavemrsvg.sudoku.domain.CELL_COUNT
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE
import io.github.greavemrsvg.sudoku.domain.GameRules
import io.github.greavemrsvg.sudoku.domain.GameState
import io.github.greavemrsvg.sudoku.domain.Puzzle
import io.github.greavemrsvg.sudoku.domain.PuzzleGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Most puzzles generated per New Game while looking for one that differs from the current one (design, risks). */
internal const val MAX_DIFFERENT_ATTEMPTS = 3

/**
 * Calls [generate] until it returns a puzzle whose givens differ from [previous]'s, at most [maxAttempts] times.
 * After the last attempt the result is returned even if it repeats [previous].
 */
internal fun generateDifferent(
    previous: Puzzle,
    maxAttempts: Int,
    generate: () -> Puzzle,
): Puzzle {
    var puzzle = generate()
    var attempts = 1
    while (puzzle.givens == previous.givens && attempts < maxAttempts) {
        puzzle = generate()
        attempts++
    }
    return puzzle
}

/**
 * Holds the current game in memory (persistence is added in a later task) and exposes it as
 * [GameUiState]. All rules live in the domain layer. The puzzle is generated on [defaultDispatcher].
 */
class GameViewModel(
    private val generator: PuzzleGenerator,
    private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var game: GameState? = null
    private var isGenerating = false
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val puzzle = withContext(defaultDispatcher) { generator.generate() }
            publish(GameRules.newGame(puzzle))
        }
    }

    fun onCellTapped(index: Int) = update { GameRules.select(it, index) }

    fun onDigit(digit: Int) = update { GameRules.enterDigit(it, digit) }

    fun onErase() = update { GameRules.erase(it) }

    /** Replaces the puzzle with a different one. No confirmation (spec A6). Ignored while one is being generated. */
    fun onNewGame() {
        val current = game ?: return
        if (isGenerating) return
        isGenerating = true
        publish(current)
        viewModelScope.launch {
            try {
                val puzzle =
                    withContext(defaultDispatcher) { generateDifferent(current.puzzle, MAX_DIFFERENT_ATTEMPTS, generator::generate) }
                game = GameRules.newGame(puzzle)
            } finally {
                isGenerating = false
                game?.let { publish(it) }
            }
        }
    }

    private fun update(transform: (GameState) -> GameState) {
        if (isGenerating) return
        val current = game ?: return
        publish(transform(current))
    }

    private fun publish(state: GameState) {
        game = state
        _uiState.value = state.toUi()
    }

    private fun GameState.toUi(): GameUiState.Ready =
        GameUiState.Ready(
            cells =
                List(CELL_COUNT) { i ->
                    CellUi(
                        row = i / GRID_SIZE,
                        column = i % GRID_SIZE,
                        digit = digitAt(i),
                        isGiven = puzzle.isGiven(i),
                        isMistake = isMistake(i),
                        isSelected = selected == i,
                    )
                },
            isSolved = isSolved,
            isGenerating = isGenerating,
        )
}
