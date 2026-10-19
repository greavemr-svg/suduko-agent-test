package io.github.greavemrsvg.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.sudoku.ui.GameRoute
import io.github.greavemrsvg.sudoku.ui.GameUiState
import io.github.greavemrsvg.sudoku.ui.GameViewModel
import io.github.greavemrsvg.sudoku.ui.theme.SudokuTheme

/** The single activity. Shows the game screen. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as SudokuApplication).container
        val viewModel =
            ViewModelProvider(
                this,
                viewModelFactory {
                    initializer {
                        GameViewModel(
                            container.createPuzzleGenerator(),
                            container.defaultDispatcher,
                            container.gameRepository,
                            container.gameSaver,
                        )
                    }
                },
            )[GameViewModel::class.java]
        setContent {
            val state by viewModel.uiState.collectAsState()
            ReportDrawnWhen { state is GameUiState.Ready }
            SudokuTheme {
                GameRoute(viewModel)
            }
        }
    }
}
