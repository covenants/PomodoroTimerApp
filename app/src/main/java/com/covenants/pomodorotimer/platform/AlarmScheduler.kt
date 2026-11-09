package com.covenants.pomodorotimer.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.covenants.pomodorotimer.core.domain.TimerEngine
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerSnapshot
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.core.time.TimeSource

/**
 * Schedules one platform alert for the upcoming stage end. The alarm is a side effect, never the
 * source of truth. If exact-alarm access is unavailable we degrade to an inexact alarm and the
 * UI tells the user alerts may be delayed.
 */
class AlarmScheduler(
    private val context: Context,
    private val clock: TimeSource,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(snapshot: TimerSnapshot, routine: RoutineConfig) {
        if (snapshot.status != TimerStatus.RUNNING || snapshot.mode == TimerMode.STOPWATCH) {
            cancel()
            return
        }
        val remaining = TimerEngine.remainingMs(snapshot, clock)
        if (remaining <= 0L) return
        val triggerAt = clock.wallClockMs() + remaining
        val operation = pendingIntent(snapshot)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and the call.
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        }
    }

    fun cancel() {
        runCatching { alarmManager.cancel(operationForCancel()) }
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(snapshot: TimerSnapshot): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STAGE_DUE
            putExtra(AlarmReceiver.EXTRA_SESSION_ID, snapshot.sessionId)
            putExtra(AlarmReceiver.EXTRA_STAGE_INDEX, snapshot.stageIndex)
            putExtra(AlarmReceiver.EXTRA_VERSION, snapshot.version)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun operationForCancel(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STAGE_DUE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val REQUEST_CODE = 4101
    }
}
