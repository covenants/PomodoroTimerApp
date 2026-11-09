package com.covenants.pomodorotimer.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.covenants.pomodorotimer.MainActivity
import com.covenants.pomodorotimer.R
import com.covenants.pomodorotimer.core.domain.TimeFormat
import com.covenants.pomodorotimer.core.domain.TimerEngine
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerSnapshot
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.data.CompletionResult

class TimerNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    fun updateActive(
        snapshot: TimerSnapshot?,
        routine: RoutineConfig,
        remainingMs: Long,
        settings: AppSettings,
    ) {
        if (snapshot == null || (snapshot.status != TimerStatus.RUNNING && snapshot.status != TimerStatus.PAUSED)) {
            cancelActive()
            return
        }
        if (!settings.notificationsEnabled) {
            cancelActive()
            return
        }
        val running = snapshot.status == TimerStatus.RUNNING
        val stageTitle = when (snapshot.mode) {
            TimerMode.FOCUS -> when (snapshot.stage) {
                FocusStage.WORK -> "Focus · ${routine.name}"
                FocusStage.SHORT_BREAK -> "Short break"
                FocusStage.LONG_BREAK -> "Long break"
                null -> "Focus"
            }
            TimerMode.COUNTDOWN -> "Countdown"
            TimerMode.STOPWATCH -> "Stopwatch"
        }
        val text = when {
            !running -> "Paused · ${TimeFormat.countdown(remainingMs)} remaining"
            snapshot.mode == TimerMode.STOPWATCH -> "Tap to return to your stopwatch"
            else -> "Finishes at ${TimeFormat.finishAt(System.currentTimeMillis() + remainingMs, java.util.TimeZone.getDefault().id)}"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(stageTitle)
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(running && snapshot.mode != TimerMode.STOPWATCH)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (running && snapshot.mode != TimerMode.STOPWATCH) {
            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + remainingMs)
        }

        if (snapshot.mode != TimerMode.STOPWATCH) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                if (running) "Pause" else "Resume",
                commandIntent(
                    if (running) TimerCommandReceiver.ACTION_TOGGLE else TimerCommandReceiver.ACTION_TOGGLE,
                    if (running) 5101 else 5102,
                ),
            )
            builder.addAction(
                android.R.drawable.ic_media_next,
                "Skip",
                commandIntent(TimerCommandReceiver.ACTION_SKIP, 5103),
            )
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            commandIntent(TimerCommandReceiver.ACTION_STOP, 5104),
        )

        notifySafely(ACTIVE_ID, builder.build())
    }

    fun cancelActive() {
        runCatching { manager.cancel(ACTIVE_ID) }
    }

    fun postCompletion(result: CompletionResult, routine: RoutineConfig) {
        val (title, text) = when {
            result.mode == TimerMode.COUNTDOWN -> "Countdown finished" to "Your timer is done."
            result.stage == FocusStage.WORK ->
                "Focus complete" to "You focused for ${routine.focusMinutes} minutes. ${breakName(result.nextStage)} is ready."
            result.stage == FocusStage.SHORT_BREAK || result.stage == FocusStage.LONG_BREAK ->
                "Break complete" to "Ready for your next focus session?"
            else -> "Session complete" to "Your timer is done."
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(0)
        notifySafely(COMPLETION_ID, builder.build())
    }

    fun cancelAll() {
        runCatching {
            manager.cancel(ACTIVE_ID)
            manager.cancel(COMPLETION_ID)
        }
    }

    private fun breakName(stage: FocusStage?): String = when (stage) {
        FocusStage.LONG_BREAK -> "A long break"
        else -> "A short break"
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun notifySafely(id: Int, notification: android.app.Notification) {
        // canNotify() checks POST_NOTIFICATIONS and the app-level notification toggle; the
        // runCatching is a second line of defence if permission is revoked concurrently.
        if (!canNotify()) return
        runCatching { manager.notify(id, notification) }
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return manager.areNotificationsEnabled()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            5000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun commandIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerCommandReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val active = NotificationChannel(
            CHANNEL_ACTIVE,
            context.getString(R.string.channel_active_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_active_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        val completion = NotificationChannel(
            CHANNEL_COMPLETION,
            context.getString(R.string.channel_completion_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_completion_description)
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannels(listOf(active, completion))
    }

    companion object {
        const val CHANNEL_ACTIVE = "active_timer"
        const val CHANNEL_COMPLETION = "session_alerts"
        const val ACTIVE_ID = 4201
        const val COMPLETION_ID = 4202
    }
}
