package app.focusflow.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.focusflow.MainActivity
import app.focusflow.R
import app.focusflow.data.Phase
import app.focusflow.data.TimerState
import app.focusflow.data.formatClock

/** All notifications of the app. Posting is silently ignored if the user denied the permission. */
object Notifier {
    const val CH_RUNNING = "running"
    const val CH_DONE = "done"
    const val CH_NUDGE = "nudge"
    const val CH_SOUND = "sound"

    const val ID_RUNNING = 1
    const val ID_DONE = 2
    const val ID_NUDGE = 3
    const val ID_SOUND = 4

    private fun manager(ctx: Context): NotificationManager =
        ctx.getSystemService(NotificationManager::class.java)

    fun ensureChannels(ctx: Context) {
        val nm = manager(ctx)
        nm.createNotificationChannel(
            NotificationChannel(CH_RUNNING, ctx.getString(R.string.ch_running), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, ctx.getString(R.string.ch_done), NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_NUDGE, ctx.getString(R.string.ch_nudge), NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SOUND, ctx.getString(R.string.ch_sound), NotificationManager.IMPORTANCE_LOW)
        )
    }

    fun phaseLabel(ctx: Context, phase: Phase): String = ctx.getString(
        when (phase) {
            Phase.FOCUS -> R.string.phase_focus
            Phase.SHORT_BREAK -> R.string.phase_short
            Phase.LONG_BREAK -> R.string.phase_long
        }
    )

    fun openAppIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Ongoing notification with a live countdown that the system itself draws (no service needed). */
    fun showRunning(ctx: Context, st: TimerState) {
        ensureChannels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(phaseLabel(ctx, st.phase))
            .setContentText(ctx.getString(R.string.notif_running))
            .setWhen(st.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppIntent(ctx))
            .build()
        manager(ctx).notify(ID_RUNNING, n)
    }

    fun showFinished(ctx: Context, finished: Phase, next: Phase) {
        ensureChannels(ctx)
        val title = if (finished == Phase.FOCUS) {
            ctx.getString(R.string.done_focus_title)
        } else {
            ctx.getString(R.string.done_break_title)
        }
        val n = NotificationCompat.Builder(ctx, CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(ctx.getString(R.string.done_text, phaseLabel(ctx, next)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        manager(ctx).notify(ID_DONE, n)
    }

    fun showNudge(ctx: Context, remainingMs: Long) {
        ensureChannels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_NUDGE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(ctx.getString(R.string.nudge_title))
            .setContentText(ctx.getString(R.string.nudge_text, formatClock(remainingMs)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        manager(ctx).notify(ID_NUDGE, n)
    }

    fun cancel(ctx: Context, id: Int) {
        manager(ctx).cancel(id)
    }
}
