package com.covenants.pomodorotimer.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.covenants.pomodorotimer.PomodoroApp
import kotlinx.coroutines.launch

/**
 * After reboot the monotonic clock has reset. The repository reconciles from the persisted wall
 * anchor and reschedules the upcoming stage. A due stage completes exactly once.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val app = context.applicationContext as? PomodoroApp ?: return
        val pendingResult = goAsync()
        app.container.appScope.launch {
            try {
                val result = app.container.repository.reconcile()
                if (result != null) {
                    AlarmReceiver.deliverCompletion(context, app, result)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
