package app.focusflow.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.focusflow.R
import app.focusflow.data.Phase
import app.focusflow.data.RunStatus
import app.focusflow.data.Sound
import app.focusflow.data.StatsData
import app.focusflow.data.formatClock
import app.focusflow.timer.Alarms
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerScreen(ui: UiState, stats: StatsData, vm: MainViewModel) {
    val ctx = LocalContext.current
    val running = ui.timer.status == RunStatus.RUNNING
    val phase = ui.timer.phase
    val color = phaseColor(phase)

    // The notification permission is asked in context: when the user starts the first session.
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Keep the screen on only while a timer is running (and only if the user wants it).
    val view = LocalView.current
    val keepOn = running && ui.settings.keepScreenOn
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    CenteredPage(horizontalAlignment = Alignment.CenterHorizontally) {

        // Phase selector (disabled while the timer is running)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Phase.entries.forEach { p ->
                FilterChip(
                    selected = p == phase,
                    enabled = !running,
                    onClick = { vm.setPhase(p) },
                    label = {
                        Text(
                            phaseChip(p),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val sessionsBeforeLong = ui.settings.sessionsBeforeLong
        val caption = if (phase == Phase.FOCUS) {
            stringResource(
                R.string.session_of,
                (ui.timer.cycle + 1).coerceAtMost(sessionsBeforeLong),
                sessionsBeforeLong
            )
        } else {
            stringResource(R.string.break_hint)
        }
        TimerRing(
            progress = ui.progress,
            color = color,
            time = formatClock(ui.remainingMs),
            caption = caption
        )

        // One dot per focus session of the current cycle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(sessionsBeforeLong) { index ->
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            if (index < ui.timer.cycle) color else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = { vm.reset() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.cd_reset))
            }
            FilledIconButton(
                onClick = {
                    if (running) {
                        vm.pause()
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        vm.start()
                    }
                },
                modifier = Modifier.size(84.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = color,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (running) R.string.cd_pause else R.string.cd_start),
                    modifier = Modifier.size(44.dp)
                )
            }
            FilledTonalIconButton(onClick = { vm.skip() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.cd_skip))
            }
        }

        // Explains why the alert might be late when exact alarms are not allowed (Android 12+)
        if (!ui.exactAlarmOk) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.exact_alarm_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    TextButton(onClick = { Alarms.requestExactAlarmAccess(ctx) }) {
                        Text(stringResource(R.string.exact_alarm_btn))
                    }
                }
            }
        }

        // Focus sounds
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.sound_title), style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = ui.sound == null,
                    onClick = { vm.setSound(null) },
                    label = { Text(stringResource(R.string.sound_off)) }
                )
                Sound.entries.forEach { s ->
                    FilterChip(
                        selected = ui.sound == s,
                        onClick = { vm.setSound(s) },
                        label = { Text(stringResource(s.label)) }
                    )
                }
            }
        }

        // Today
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.today_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.sessions_progress, stats.todaySessions, ui.settings.dailyGoal),
                    style = MaterialTheme.typography.bodyLarge
                )
                LinearProgressIndicator(
                    progress = { (stats.todaySessions.toFloat() / ui.settings.dailyGoal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = color
                )
                Text(
                    durationText(stats.todayMin),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The big dial: a progress ring with 60 tick marks (like a kitchen timer) and the remaining time. */
@Composable
private fun TimerRing(progress: Float, color: Color, time: String, caption: String) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tick = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    Box(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 300.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val half = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(half, half)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outer = size.minDimension / 2f - stroke - 8.dp.toPx()
            for (i in 0 until 60) {
                val major = i % 5 == 0
                val len = if (major) 10.dp.toPx() else 5.dp.toPx()
                val angle = Math.toRadians(i * 6.0 - 90.0)
                val ca = cos(angle).toFloat()
                val sa = sin(angle).toFloat()
                drawLine(
                    color = tick,
                    start = Offset(cx + (outer - len) * ca, cy + (outer - len) * sa),
                    end = Offset(cx + outer * ca, cy + outer * sa),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                time,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                )
            )
            Text(
                caption,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 170.dp)
            )
        }
    }
}
