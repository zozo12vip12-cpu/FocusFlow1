package app.focusflow.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Tiny wrapper around two SharedPreferences files:
 *  - focus_settings : user settings (included in backups)
 *  - focus_timer    : live timer state (never backed up, see res/xml/*backup*.xml)
 */
class Prefs private constructor(ctx: Context) {

    private val settingsPrefs: SharedPreferences =
        ctx.getSharedPreferences("focus_settings", Context.MODE_PRIVATE)
    private val timerPrefs: SharedPreferences =
        ctx.getSharedPreferences("focus_timer", Context.MODE_PRIVATE)

    var settings: AppSettings
        get() {
            val p = settingsPrefs
            return AppSettings(
                focusMin = p.getInt("focusMin", 25),
                shortBreakMin = p.getInt("shortBreakMin", 5),
                longBreakMin = p.getInt("longBreakMin", 15),
                sessionsBeforeLong = p.getInt("sessionsBeforeLong", 4),
                dailyGoal = p.getInt("dailyGoal", 8),
                autoStart = p.getBoolean("autoStart", false),
                keepScreenOn = p.getBoolean("keepScreenOn", true),
                nudges = p.getBoolean("nudges", true),
                theme = p.getInt("theme", 0),
                dynamicColor = p.getBoolean("dynamicColor", false)
            )
        }
        set(v) {
            settingsPrefs.edit()
                .putInt("focusMin", v.focusMin)
                .putInt("shortBreakMin", v.shortBreakMin)
                .putInt("longBreakMin", v.longBreakMin)
                .putInt("sessionsBeforeLong", v.sessionsBeforeLong)
                .putInt("dailyGoal", v.dailyGoal)
                .putBoolean("autoStart", v.autoStart)
                .putBoolean("keepScreenOn", v.keepScreenOn)
                .putBoolean("nudges", v.nudges)
                .putInt("theme", v.theme)
                .putBoolean("dynamicColor", v.dynamicColor)
                .apply()
        }

    var timer: TimerState
        get() {
            val p = timerPrefs
            return TimerState(
                phase = Phase.entries.getOrElse(p.getInt("phase", 0)) { Phase.FOCUS },
                status = RunStatus.entries.getOrElse(p.getInt("status", 0)) { RunStatus.IDLE },
                endAt = p.getLong("endAt", 0L),
                remainingMs = p.getLong("remainingMs", 0L),
                totalMs = p.getLong("totalMs", 0L),
                cycle = p.getInt("cycle", 0),
                startedAt = p.getLong("startedAt", 0L),
                distractions = p.getInt("distractions", 0),
                leftAt = p.getLong("leftAt", 0L)
            )
        }
        set(v) {
            timerPrefs.edit()
                .putInt("phase", v.phase.ordinal)
                .putInt("status", v.status.ordinal)
                .putLong("endAt", v.endAt)
                .putLong("remainingMs", v.remainingMs)
                .putLong("totalMs", v.totalMs)
                .putInt("cycle", v.cycle)
                .putLong("startedAt", v.startedAt)
                .putInt("distractions", v.distractions)
                .putLong("leftAt", v.leftAt)
                .apply()
        }

    fun clearTimer() {
        timerPrefs.edit().clear().apply()
    }

    companion object {
        @Volatile
        private var instance: Prefs? = null

        fun get(ctx: Context): Prefs = instance ?: synchronized(this) {
            instance ?: Prefs(ctx.applicationContext).also { instance = it }
        }
    }
}
