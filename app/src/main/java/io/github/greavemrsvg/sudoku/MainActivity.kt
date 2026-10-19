package io.github.greavemrsvg.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.greavemrsvg.sudoku.ui.GameRoute
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
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        GameViewModel(container.createPuzzleGenerator(), container.defaultDispatcher) as T
                },
            )[GameViewModel::class.java]
        setContent {
            SudokuTheme {
                GameRoute(viewModel)
            }
        }
    }
}
