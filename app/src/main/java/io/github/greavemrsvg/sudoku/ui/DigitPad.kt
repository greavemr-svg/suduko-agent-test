package io.github.greavemrsvg.sudoku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.sudoku.R
import io.github.greavemrsvg.sudoku.domain.GRID_SIZE

/** Height of the pad buttons and Erase. Above the 48 dp touch-target minimum (spec Accessibility NFR). */
internal val ControlHeight = 52.dp
private const val PAD_COLUMNS = 5

/** The 1-9 digit buttons, with Erase filling the last slot. */
@Composable
fun DigitPad(
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..GRID_SIZE).chunked(PAD_COLUMNS).forEach { digits ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                digits.forEach { digit ->
                    val description = stringResource(R.string.digit_button, digit)
                    Button(
                        onClick = { onDigit(digit) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(min = ControlHeight)
                                .testTag("digit_$digit")
                                .semantics { contentDescription = description },
                    ) {
                        Text(text = digit.toString())
                    }
                }
                if (digits.size < PAD_COLUMNS) {
                    EraseButton(onClick = onErase, modifier = Modifier.weight((PAD_COLUMNS - digits.size).toFloat()))
                }
            }
        }
    }
}
