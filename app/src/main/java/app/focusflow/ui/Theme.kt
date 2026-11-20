package app.focusflow.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.focusflow.data.Phase

// Brand palette: deep ink surfaces, tomato red for focus, mint for short breaks, blue for long breaks.
private val LightColors = lightColorScheme(
    primary = Color(0xFFD9392A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF0F7A64),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB6F2DF),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF2F5BD0),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF161A2B),
    surface = Color(0xFFF7F8FC),
    onSurface = Color(0xFF161A2B),
    surfaceVariant = Color(0xFFDDE2EF),
    onSurfaceVariant = Color(0xFF454B63),
    outline = Color(0xFF767C96),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3FA),
    surfaceContainer = Color(0xFFEBEEF7),
    surfaceContainerHigh = Color(0xFFE4E8F3),
    surfaceContainerHighest = Color(0xFFDDE2EF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8F7E),
    onPrimary = Color(0xFF5F0A00),
    primaryContainer = Color(0xFF8C1F12),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFF6ADBBD),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF005143),
    onSecondaryContainer = Color(0xFFB6F2DF),
    tertiary = Color(0xFFB3C5FF),
    background = Color(0xFF0E111E),
    onBackground = Color(0xFFE4E7F5),
    surface = Color(0xFF0E111E),
    onSurface = Color(0xFFE4E7F5),
    surfaceVariant = Color(0xFF2C3250),
    onSurfaceVariant = Color(0xFFBFC4DC),
    outline = Color(0xFF8A90AB),
    surfaceContainerLowest = Color(0xFF090B15),
    surfaceContainerLow = Color(0xFF141829),
    surfaceContainer = Color(0xFF191D30),
    surfaceContainerHigh = Color(0xFF222740),
    surfaceContainerHighest = Color(0xFF2C3250)
)

/** The accent color of each phase (ring, play button, dots). */
fun phaseColor(phase: Phase): Color = when (phase) {
    Phase.FOCUS -> Color(0xFFE0402B)
    Phase.SHORT_BREAK -> Color(0xFF1E9E78)
    Phase.LONG_BREAK -> Color(0xFF3D6BE0)
}

/** @param themeMode 0 = follow the system, 1 = light, 2 = dark */
@Composable
fun FocusTheme(themeMode: Int, dynamicColor: Boolean, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    // Make the status bar and navigation bar icons readable on the chosen theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
