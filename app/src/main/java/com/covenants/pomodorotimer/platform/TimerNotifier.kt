package com.covenants.pomodorotimer.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.covenants.pomodorotimer.MainActivity
import com.covenants.pomodorotimer.R
import com.covenants.pomodorotimer.core.domain.TimeFormat
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.ChimeChoice
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
                commandIntent(TimerCommandReceiver.ACTION_TOGGLE, 5101),
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

    /**
     * Posts the completion alert. Returns true when Android accepted the notification. When false,
     * the caller should play a fallback sound because the notification channel cannot do it.
     */
    fun postCompletion(
        result: CompletionResult,
        routine: RoutineConfig,
        settings: AppSettings,
    ): Boolean {
        val (title, text) = when {
            result.mode == TimerMode.COUNTDOWN -> "Countdown finished" to "Your timer is done."
            result.stage == FocusStage.WORK ->
                "Focus complete" to "You focused for ${routine.focusMinutes} minutes. ${breakName(result.nextStage)} is ready."
            result.stage == FocusStage.SHORT_BREAK || result.stage == FocusStage.LONG_BREAK ->
                "Break complete" to "Ready for your next focus session?"
            else -> "Session complete" to "Your timer is done."
        }
        val channel = if (settings.soundEnabled) soundChannel(settings.chime) else CHANNEL_SILENT
        val builder = NotificationCompat.Builder(context, channel)
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

        if (settings.fullScreenAlerts && canUseFullScreen()) {
            builder.setFullScreenIntent(fullScreenIntent(), true)
        }
        return notifySafely(COMPLETION_ID, builder.build())
    }

    fun cancelAll() {
        runCatching {
            manager.cancel(ACTIVE_ID)
            manager.cancel(COMPLETION_ID)
        }
    }

    fun canUseFullScreen(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.canUseFullScreenIntent()
    }

    private fun breakName(stage: FocusStage?): String = when (stage) {
        FocusStage.LONG_BREAK -> "A long break"
        else -> "A short break"
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun notifySafely(id: Int, notification: android.app.Notification): Boolean {
        if (!canNotify()) return false
        return runCatching {
            manager.notify(id, notification)
            true
        }.getOrDefault(false)
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

    private fun fullScreenIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_LAUNCH, true)
        }
        return PendingIntent.getActivity(
            context,
            5001,
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
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

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

        val silent = NotificationChannel(
            CHANNEL_SILENT,
            "Session alerts (silent)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Completion alerts with sound turned off in the app"
            setSound(null, null)
            enableVibration(false)
        }

        val gentle = NotificationChannel(
            CHANNEL_GENTLE,
            "Session alerts (gentle)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Soft completion alert"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
            enableVibration(false)
        }

        val chime = NotificationChannel(
            CHANNEL_CHIME,
            "Session alerts (chime)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Completion chime"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
            enableVibration(false)
        }

        val bell = NotificationChannel(
            CHANNEL_BELL,
            "Session alerts (bell)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Completion bell played on the alarm stream"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            enableVibration(false)
        }

        nm.createNotificationChannels(listOf(active, silent, gentle, chime, bell))
        // Remove the old silent channel from pre-release builds.
        runCatching { nm.deleteNotificationChannel("session_alerts") }
    }

    private fun soundChannel(chime: ChimeChoice): String = when (chime) {
        ChimeChoice.GENTLE -> CHANNEL_GENTLE
        ChimeChoice.CHIME -> CHANNEL_CHIME
        ChimeChoice.BELL -> CHANNEL_BELL
    }

    companion object {
        const val CHANNEL_ACTIVE = "active_timer"
        const val CHANNEL_SILENT = "session_alerts_silent"
        const val CHANNEL_GENTLE = "session_alerts_gentle"
        const val CHANNEL_CHIME = "session_alerts_chime"
        const val CHANNEL_BELL = "session_alerts_bell"
        const val ACTIVE_ID = 4201
        const val COMPLETION_ID = 4202
        const val EXTRA_ALARM_LAUNCH = "alarm_launch"
    }
}
