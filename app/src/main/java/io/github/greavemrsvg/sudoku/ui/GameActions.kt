package io.github.greavemrsvg.sudoku.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.sudoku.R

private val NewGameMinWidth = 112.dp

/** Clears the selected player-entered cell. At least 48x48 dp. */
@Composable
fun EraseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PadContentPadding,
        modifier = modifier.heightIn(min = ControlHeight).testTag("erase"),
    ) {
        CappedFontScale {
            Text(text = stringResource(R.string.erase), maxLines = 1, softWrap = false)
        }
    }
}

/** Starts a different puzzle at once, with no confirmation (spec A6). At least 48x48 dp. */
@Composable
fun NewGameButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PadContentPadding,
        modifier = modifier.heightIn(min = ControlHeight).widthIn(min = NewGameMinWidth).testTag("new_game"),
    ) {
        CappedFontScale {
            Text(text = stringResource(R.string.new_game), maxLines = 1, softWrap = false)
        }
    }
}
