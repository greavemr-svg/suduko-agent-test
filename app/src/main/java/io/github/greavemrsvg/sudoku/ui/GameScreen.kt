package io.github.greavemrsvg.sudoku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Connects the [GameViewModel] to the stateless [GameScreen]. */
@Composable
fun GameRoute(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    GameScreen(
        state = state,
        onCellTapped = viewModel::onCellTapped,
        onDigit = viewModel::onDigit,
        onErase = viewModel::onErase,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    onCellTapped: (Int) -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state) {
            GameUiState.Loading ->
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is GameUiState.Ready ->
                Column(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    SudokuGrid(cells = state.cells, onCellTapped = onCellTapped)
                    Spacer(modifier = Modifier.height(16.dp))
                    DigitPad(onDigit = onDigit, onErase = onErase, modifier = Modifier.padding(horizontal = 8.dp))
                }
        }
    }
}
