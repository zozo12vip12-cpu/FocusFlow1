package app.focusflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.focusflow.R
import app.focusflow.data.Phase

@Composable
fun phaseName(phase: Phase): String = stringResource(
    when (phase) {
        Phase.FOCUS -> R.string.phase_focus
        Phase.SHORT_BREAK -> R.string.phase_short
        Phase.LONG_BREAK -> R.string.phase_long
    }
)

@Composable
fun phaseChip(phase: Phase): String = stringResource(
    when (phase) {
        Phase.FOCUS -> R.string.chip_focus
        Phase.SHORT_BREAK -> R.string.chip_short
        Phase.LONG_BREAK -> R.string.chip_long
    }
)

/** 85 -> "1h 25m", 25 -> "25 min" (localized). */
@Composable
fun durationText(minutes: Int): String =
    if (minutes >= 60) {
        stringResource(R.string.fmt_hm, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.fmt_min, minutes)
    }

/** Scrollable page whose content is centered and limited in width, so it also looks good on tablets. */
@Composable
fun CenteredPage(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}
