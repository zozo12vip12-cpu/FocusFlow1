package app.focusflow.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Alarms are what wake the app when a session ends while the screen is off or the app is closed.
 *
 *  - End of session: exact alarm when the user allowed "Alarms & reminders" (Android 12+),
 *    otherwise a best-effort alarm that may arrive a little late.
 *  - Nudge: a normal (inexact) alarm, no special permission needed.
 */
object Alarms {
    private const val ACTION_END = "app.focusflow.action.TIMER_END"
    private const val ACTION_NUDGE = "app.focusflow.action.NUDGE"
    private const val RC_END = 10
    private const val RC_NUDGE = 12

    private fun alarmManager(ctx: Context): AlarmManager =
        ctx.getSystemService(AlarmManager::class.java)

    private fun broadcast(ctx: Context, cls: Class<*>, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            requestCode,
            Intent(ctx, cls).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** True if exact alarms are allowed (always true below Android 12). */
    fun canScheduleExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(ctx).canScheduleExactAlarms()

    fun scheduleEnd(ctx: Context, at: Long) {
        val am = alarmManager(ctx)
        val op = broadcast(ctx, TimerReceiver::class.java, ACTION_END, RC_END)
        try {
            if (canScheduleExact(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }

    fun cancelEnd(ctx: Context) {
        alarmManager(ctx).cancel(broadcast(ctx, TimerReceiver::class.java, ACTION_END, RC_END))
    }

    fun scheduleNudge(ctx: Context, at: Long) {
        alarmManager(ctx).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            at,
            broadcast(ctx, NudgeReceiver::class.java, ACTION_NUDGE, RC_NUDGE)
        )
    }

    fun cancelNudge(ctx: Context) {
        alarmManager(ctx).cancel(broadcast(ctx, NudgeReceiver::class.java, ACTION_NUDGE, RC_NUDGE))
    }

    /** Opens the system screen where the user can allow "Alarms & reminders" for this app. */
    fun requestExactAlarmAccess(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
                )
            } catch (e: Exception) {
                // The settings screen is not available on this device: nothing else to do.
            }
        }
    }
}
