package app.focusflow.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager when a session ends and the app is in the background or closed. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerController.complete(context)
    }
}

/** Fired shortly after the user left the app during a focus session. */
class NudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerController.nudgeIfAway(context)
    }
}
