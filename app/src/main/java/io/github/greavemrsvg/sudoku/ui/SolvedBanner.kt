package io.github.greavemrsvg.sudoku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.greavemrsvg.sudoku.R

/** Test tag of the solved message. */
const val SOLVED_BANNER_TAG = "solved_banner"

/** The "puzzle solved" message. A live region, so a screen reader announces it when it appears. */
@Composable
fun SolvedBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.solved_message),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.testTag(SOLVED_BANNER_TAG).semantics { liveRegion = LiveRegionMode.Polite },
    )
}
