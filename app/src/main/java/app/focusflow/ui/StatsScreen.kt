package app.focusflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.focusflow.R
import app.focusflow.data.StatsData
import app.focusflow.data.localized
import java.time.LocalDate

@Composable
fun StatsScreen(stats: StatsData) {
    // Day-of-month under each bar (7 days, today is the last one).
    val labels = remember(stats) {
        val today = LocalDate.now()
        (6 downTo 0).map { back -> today.minusDays(back.toLong()).dayOfMonth.localized() }
    }

    CenteredPage {
        Text(
            stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (stats.totalSessions == 0) {
            Text(
                stringResource(R.string.stats_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                stringResource(R.string.today_title),
                durationText(stats.todayMin),
                Icons.Rounded.Today,
                Modifier.weight(1f)
            )
            StatCard(
                stringResource(R.string.stat_week),
                durationText(stats.weekMin),
                Icons.Rounded.DateRange,
                Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                stringResource(R.string.stat_total),
                durationText(stats.totalMin),
                Icons.Rounded.Schedule,
                Modifier.weight(1f)
            )
            StatCard(
                stringResource(R.string.stat_streak),
                stats.streak.localized(),
                Icons.Rounded.LocalFireDepartment,
                Modifier.weight(1f)
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.stats_chart_title),
                    style = MaterialTheme.typography.titleSmall
                )
                BarChart(
                    values = stats.last7Min,
                    labels = labels,
                    barColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (stats.totalSessions > 0) {
            Text(
                stringResource(R.string.stats_avg_distractions, stats.avgDistractions),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            stringResource(R.string.local_only),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Simple bar chart built from boxes (no chart library). The last bar (today) is highlighted. */
@Composable
private fun BarChart(values: List<Int>, labels: List<String>, barColor: Color) {
    val peak = maxOf(values.maxOrNull() ?: 0, 30).toFloat()
    val maxBar = 120.dp
    Row(
        Modifier
            .fillMaxWidth()
            .height(maxBar + 44.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, minutes ->
            val isToday = index == values.lastIndex
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    if (minutes > 0) minutes.localized() else "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(maxOf(maxBar * (minutes / peak), 4.dp))
                        .background(
                            if (isToday) barColor else barColor.copy(alpha = 0.45f),
                            RoundedCornerShape(6.dp)
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    labels[index],
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
