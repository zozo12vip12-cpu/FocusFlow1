package app.focusflow.data

import androidx.annotation.StringRes
import app.focusflow.R

/** The three phases of the work/break cycle. */
enum class Phase { FOCUS, SHORT_BREAK, LONG_BREAK }

enum class RunStatus { IDLE, RUNNING, PAUSED }

/** Calm background sounds. They are generated in code, so the app ships no audio files. */
enum class Sound(val key: String, @StringRes val label: Int) {
    RAIN("rain", R.string.sound_rain),
    WIND("wind", R.string.sound_wind),
    BROWN("brown", R.string.sound_brown),
    WHITE("white", R.string.sound_white);

    companion object {
        fun fromKey(key: String?): Sound? = Sound.entries.firstOrNull { it.key == key }
    }
}

/** User settings (stored in SharedPreferences, backed up with the database). */
data class AppSettings(
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val sessionsBeforeLong: Int = 4,
    val dailyGoal: Int = 8,
    val autoStart: Boolean = false,
    val keepScreenOn: Boolean = true,
    val nudges: Boolean = true,
    /** 0 = follow the system, 1 = light, 2 = dark */
    val theme: Int = 0,
    val dynamicColor: Boolean = false
)

/**
 * Everything the timer needs to survive process death.
 * While RUNNING the remaining time is derived from [endAt] (wall-clock), never from a counter.
 */
data class TimerState(
    val phase: Phase = Phase.FOCUS,
    val status: RunStatus = RunStatus.IDLE,
    /** Epoch millis when the running phase ends (only meaningful while RUNNING). */
    val endAt: Long = 0L,
    /** Remaining millis while PAUSED. */
    val remainingMs: Long = 0L,
    /** Planned length of the current phase in millis. */
    val totalMs: Long = 0L,
    /** Focus sessions completed since the last long break. */
    val cycle: Int = 0,
    /** When the current focus session was first started. */
    val startedAt: Long = 0L,
    /** How many times the user left the app during the current focus session. */
    val distractions: Int = 0,
    /** When the user left the app during focus (0 = not away). */
    val leftAt: Long = 0L
)

/** One completed focus session. */
data class Session(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSec: Int,
    val distractions: Int
)
