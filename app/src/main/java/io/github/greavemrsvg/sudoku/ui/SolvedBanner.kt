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
import androidx.compose.ui.text.style.TextOverflow
import io.github.greavemrsvg.sudoku.R

/** Test tag of the solved message. */
const val SOLVED_BANNER_TAG = "solved_banner"

/**
 * The "puzzle solved" message. The live-region node is always composed and empty until solved, so a screen
 * reader announces the change of its text (some TalkBack versions ignore live regions that only appear).
 */
@Composable
fun SolvedBanner(
    isSolved: Boolean,
    modifier: Modifier = Modifier,
) {
    CappedFontScale {
        Text(
            text = if (isSolved) stringResource(R.string.solved_message) else "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.testTag(SOLVED_BANNER_TAG).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
