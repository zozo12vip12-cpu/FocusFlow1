package app.focusflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focusflow.R

/** Root of the UI: four tabs (timer, stats, achievements, settings). */
@Composable
fun FocusApp(vm: MainViewModel, ui: UiState) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }

    val items = listOf(
        R.string.tab_timer to Icons.Rounded.Timer,
        R.string.tab_stats to Icons.Rounded.BarChart,
        R.string.tab_awards to Icons.Rounded.EmojiEvents,
        R.string.tab_settings to Icons.Rounded.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when (tab) {
                0 -> TimerScreen(ui, stats, vm)
                1 -> StatsScreen(stats)
                2 -> AchievementsScreen(stats)
                else -> SettingsScreen(ui.settings, vm)
            }
        }
    }
}
