package com.covenants.pomodorotimer.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.covenants.pomodorotimer.PomodoroApp
import kotlinx.coroutines.launch

/** Notification actions dispatch through the same repository command processor as the UI. */
class TimerCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? PomodoroApp ?: return
        val pendingResult = goAsync()
        app.container.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_TOGGLE -> app.container.repository.togglePause()
                    ACTION_SKIP -> app.container.repository.skip()
                    ACTION_STOP -> app.container.repository.stop()
                    ACTION_ADD_FIVE -> app.container.repository.addFiveMinutes()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.covenants.pomodorotimer.TOGGLE"
        const val ACTION_SKIP = "com.covenants.pomodorotimer.SKIP"
        const val ACTION_STOP = "com.covenants.pomodorotimer.STOP"
        const val ACTION_ADD_FIVE = "com.covenants.pomodorotimer.ADD_FIVE"
    }
}
