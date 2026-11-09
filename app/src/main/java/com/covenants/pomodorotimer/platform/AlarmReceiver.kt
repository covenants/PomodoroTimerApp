package com.covenants.pomodorotimer.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.covenants.pomodorotimer.PomodoroApp
import com.covenants.pomodorotimer.data.CompletionResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STAGE_DUE) return
        val app = context.applicationContext as? PomodoroApp ?: return
        val pendingResult = goAsync()
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val stageIndex = intent.getIntExtra(EXTRA_STAGE_INDEX, -1).takeIf { it >= 0 }
        val version = intent.getLongExtra(EXTRA_VERSION, -1L).takeIf { it >= 0L }

        app.container.appScope.launch {
            try {
                val result = app.container.repository.completeDue(sessionId, stageIndex, version)
                if (result != null) {
                    deliverCompletion(context, app, result)
                } else {
                    app.container.notifier.cancelActive()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_STAGE_DUE = "com.covenants.pomodorotimer.STAGE_DUE"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_STAGE_INDEX = "stage_index"
        const val EXTRA_VERSION = "version"

        suspend fun deliverCompletion(context: Context, app: PomodoroApp, result: CompletionResult) {
            val settings = app.container.settings.settings.first()
            val posted = app.container.notifier.postCompletion(
                result = result,
                routine = app.container.repository.routine.value,
                settings = settings,
            )
            CompletionAlarmPlayer.deliver(
                context = context,
                settings = settings,
                playSoundFallback = !posted,
            )
        }
    }
}
