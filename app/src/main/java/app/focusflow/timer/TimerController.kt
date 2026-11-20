package app.focusflow.timer

import android.content.Context
import android.os.PowerManager
import app.focusflow.data.AppSettings
import app.focusflow.data.Phase
import app.focusflow.data.Prefs
import app.focusflow.data.RunStatus
import app.focusflow.data.Session
import app.focusflow.data.SessionDb
import app.focusflow.data.TimerState
import app.focusflow.sound.AmbientService

/**
 * The single place that changes the timer. It is called from the UI and from the broadcast
 * receivers, so it only works with persisted state (Prefs) and never keeps data in memory.
 */
object TimerController {

    /** A session that ended more than 10 minutes ago without us noticing is not counted. */
    private const val STALE_AFTER_MS = 10 * 60_000L

    /** How long after leaving the app the gentle reminder is shown. */
    private const val NUDGE_DELAY_MS = 45_000L

    /** Leaving the app for less than this does not count as an interruption. */
    private const val MIN_AWAY_MS = 5_000L

    fun durationMs(phase: Phase, s: AppSettings): Long {
        val minutes = when (phase) {
            Phase.FOCUS -> s.focusMin
            Phase.SHORT_BREAK -> s.shortBreakMin
            Phase.LONG_BREAK -> s.longBreakMin
        }
        return minutes * 60_000L
    }

    @Synchronized
    fun startOrResume(ctx: Context) {
        val prefs = Prefs.get(ctx)
        val st = prefs.timer
        if (st.status == RunStatus.RUNNING) return
        val now = System.currentTimeMillis()
        val fresh = st.status != RunStatus.PAUSED
        val total = if (fresh) durationMs(st.phase, prefs.settings) else st.totalMs
        val remaining = if (fresh) total else st.remainingMs
        val next = st.copy(
            status = RunStatus.RUNNING,
            endAt = now + remaining,
            remainingMs = remaining,
            totalMs = total,
            startedAt = if (fresh) now else st.startedAt,
            distractions = if (fresh) 0 else st.distractions,
            leftAt = 0L
        )
        prefs.timer = next
        Alarms.scheduleEnd(ctx, next.endAt)
        Notifier.showRunning(ctx, next)
    }

    @Synchronized
    fun pause(ctx: Context) {
        val prefs = Prefs.get(ctx)
        val st = prefs.timer
        if (st.status != RunStatus.RUNNING) return
        val remaining = (st.endAt - System.currentTimeMillis()).coerceAtLeast(1_000L)
        prefs.timer = st.copy(
            status = RunStatus.PAUSED,
            remainingMs = remaining,
            endAt = 0L,
            leftAt = 0L
        )
        cancelAlarmsAndNotifications(ctx)
    }

    /** Back to the start of the current phase. */
    @Synchronized
    fun reset(ctx: Context) {
        val prefs = Prefs.get(ctx)
        val st = prefs.timer
        prefs.timer = TimerState(phase = st.phase, cycle = st.cycle)
        cancelAlarmsAndNotifications(ctx)
    }

    /** Jump to the next phase without counting the current one. */
    @Synchronized
    fun skip(ctx: Context) {
        val st = Prefs.get(ctx).timer
        cancelAlarmsAndNotifications(ctx)
        moveToNext(ctx, st, completed = false)
    }

    @Synchronized
    fun setPhase(ctx: Context, phase: Phase) {
        val prefs = Prefs.get(ctx)
        val st = prefs.timer
        if (st.status == RunStatus.RUNNING) return
        prefs.timer = TimerState(phase = phase, cycle = st.cycle)
    }

    /**
     * Called when the countdown reaches zero: by the UI (app open) or by [TimerReceiver] (app closed).
     * It is safe to call it twice, the second call does nothing.
     */
    @Synchronized
    fun complete(ctx: Context) {
        val prefs = Prefs.get(ctx)
        val st = prefs.timer
        if (st.status != RunStatus.RUNNING) return
        val now = System.currentTimeMillis()
        if (now < st.endAt - 1_500L) return // not finished yet

        cancelAlarmsAndNotifications(ctx)

        if (now - st.endAt > STALE_AFTER_MS) {
            // The phone was off or the app was force-stopped: do not count this session.
            prefs.timer = TimerState(phase = st.phase, cycle = st.cycle)
            return
        }

        if (st.phase == Phase.FOCUS) {
            SessionDb.get(ctx).insert(
                Session(
                    id = 0L,
                    startedAt = st.startedAt,
                    endedAt = st.endAt,
                    durationSec = (st.totalMs / 1000L).toInt(),
                    distractions = st.distractions
                )
            )
            AmbientService.stop(ctx)
        }

        val next = moveToNext(ctx, st, completed = true)
        Notifier.showFinished(ctx, st.phase, next.phase)
        if (prefs.settings.autoStart) startOrResume(ctx)
    }

    /** Re-arms the alarm of a running timer (for example after the user allowed exact alarms). */
    fun ensureAlarm(ctx: Context) {
        val st = Prefs.get(ctx).timer
        if (st.status == RunStatus.RUNNING && st.endAt > System.currentTimeMillis()) {
            Alarms.scheduleEnd(ctx, st.endAt)
        }
    }

    // ------------------------------------------------------------------ gentle reminders

    /** The app went to the background. If a focus session is running, plan a gentle reminder. */
    fun onAppLeft(ctx: Context) {
        val prefs = Prefs.get(ctx)
        val st = prefs.timer
        if (st.status != RunStatus.RUNNING || st.phase != Phase.FOCUS || !prefs.settings.nudges) return
        val pm = ctx.getSystemService(PowerManager::class.java)
        if (!pm.isInteractive) return // the screen was switched off: that is not a distraction
        val now = System.currentTimeMillis()
        prefs.timer = st.copy(leftAt = now)
        Alarms.scheduleNudge(ctx, now + NUDGE_DELAY_MS)
    }

    /** The user came back: cancel the reminder and count the interruption if it was long enough. */
    fun onAppReturned(ctx: Context) {
        val prefs = Prefs.get(ctx)
        Alarms.cancelNudge(ctx)
        Notifier.cancel(ctx, Notifier.ID_NUDGE)
        val st = prefs.timer
        if (st.leftAt != 0L) {
            val away = System.currentTimeMillis() - st.leftAt
            val count = if (away >= MIN_AWAY_MS) st.distractions + 1 else st.distractions
            prefs.timer = st.copy(leftAt = 0L, distractions = count)
        }
    }

    fun nudgeIfAway(ctx: Context) {
        val st = Prefs.get(ctx).timer
        if (st.status == RunStatus.RUNNING && st.phase == Phase.FOCUS && st.leftAt != 0L) {
            Notifier.showNudge(ctx, (st.endAt - System.currentTimeMillis()).coerceAtLeast(0L))
        }
    }

    // ------------------------------------------------------------------ data

    /** Deletes everything the app stored: history, timer state and the running sound. */
    @Synchronized
    fun wipe(ctx: Context) {
        cancelAlarmsAndNotifications(ctx)
        AmbientService.stop(ctx)
        SessionDb.get(ctx).clear()
        Prefs.get(ctx).clearTimer()
    }

    // ------------------------------------------------------------------ internals

    private fun moveToNext(ctx: Context, finished: TimerState, completed: Boolean): TimerState {
        val prefs = Prefs.get(ctx)
        val s = prefs.settings
        val cycleAfter = when {
            finished.phase == Phase.FOCUS && completed -> finished.cycle + 1
            finished.phase == Phase.LONG_BREAK -> 0
            else -> finished.cycle
        }
        val next = when (finished.phase) {
            Phase.FOCUS ->
                if (completed && cycleAfter >= s.sessionsBeforeLong) Phase.LONG_BREAK else Phase.SHORT_BREAK
            else -> Phase.FOCUS
        }
        val state = TimerState(phase = next, cycle = cycleAfter)
        prefs.timer = state
        return state
    }

    private fun cancelAlarmsAndNotifications(ctx: Context) {
        Alarms.cancelEnd(ctx)
        Alarms.cancelNudge(ctx)
        Notifier.cancel(ctx, Notifier.ID_RUNNING)
        Notifier.cancel(ctx, Notifier.ID_NUDGE)
    }
}
