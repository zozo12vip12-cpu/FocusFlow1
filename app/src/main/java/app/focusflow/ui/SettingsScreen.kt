package app.focusflow.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.focusflow.R
import app.focusflow.data.AppSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(s: AppSettings, vm: MainViewModel) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val policyUrl = stringResource(R.string.privacy_policy_url)

    CenteredPage {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        SectionTitle(stringResource(R.string.set_durations))
        SliderRow(stringResource(R.string.set_focus, s.focusMin), s.focusMin, 5, 90, 5) { v ->
            vm.update { it.copy(focusMin = v) }
        }
        SliderRow(stringResource(R.string.set_short, s.shortBreakMin), s.shortBreakMin, 1, 30, 1) { v ->
            vm.update { it.copy(shortBreakMin = v) }
        }
        SliderRow(stringResource(R.string.set_long, s.longBreakMin), s.longBreakMin, 5, 45, 5) { v ->
            vm.update { it.copy(longBreakMin = v) }
        }
        SliderRow(stringResource(R.string.set_cycle, s.sessionsBeforeLong), s.sessionsBeforeLong, 2, 8, 1) { v ->
            vm.update { it.copy(sessionsBeforeLong = v) }
        }
        SliderRow(stringResource(R.string.set_goal, s.dailyGoal), s.dailyGoal, 1, 16, 1) { v ->
            vm.update { it.copy(dailyGoal = v) }
        }

        SectionTitle(stringResource(R.string.set_behavior))
        SwitchRow(stringResource(R.string.set_autostart), s.autoStart) { on ->
            vm.update { it.copy(autoStart = on) }
        }
        SwitchRow(stringResource(R.string.set_keep_on), s.keepScreenOn) { on ->
            vm.update { it.copy(keepScreenOn = on) }
        }
        SwitchRow(stringResource(R.string.set_nudges), s.nudges) { on ->
            vm.update { it.copy(nudges = on) }
        }

        SectionTitle(stringResource(R.string.set_look))
        Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                0 to R.string.theme_system,
                1 to R.string.theme_light,
                2 to R.string.theme_dark
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = s.theme == mode,
                    onClick = { vm.update { it.copy(theme = mode) } },
                    label = {
                        Text(
                            stringResource(label),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SwitchRow(stringResource(R.string.set_dynamic), s.dynamicColor) { on ->
                vm.update { it.copy(dynamicColor = on) }
            }
        }

        SectionTitle(stringResource(R.string.set_privacy))
        Text(
            stringResource(R.string.privacy_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl)))
                } catch (e: ActivityNotFoundException) {
                    // No browser installed: nothing to do.
                }
            }
        ) {
            Text(stringResource(R.string.privacy_policy))
        }
        OutlinedButton(onClick = { confirmDelete = true }) {
            Text(stringResource(R.string.delete_data))
        }
        Text(
            stringResource(R.string.version_fmt, versionName(ctx)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.wipe()
                        confirmDelete = false
                    }
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onChange: (Int) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { raw -> onChange(((raw / step).roundToInt() * step).coerceIn(min, max)) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min) / step - 1
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Suppress("DEPRECATION")
private fun versionName(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}
