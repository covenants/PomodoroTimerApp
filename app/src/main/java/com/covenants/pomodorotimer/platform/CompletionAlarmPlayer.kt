package com.covenants.pomodorotimer.platform

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.ChimeChoice

/**
 * Completion sound and haptics are played by us rather than baked into the notification channel
 * so the in-app toggles remain truthful and work even when notifications are denied.
 */
object CompletionAlarmPlayer {

    fun play(context: Context, settings: AppSettings) {
        if (settings.soundEnabled) {
            val type = when (settings.chime) {
                ChimeChoice.GENTLE -> RingtoneManager.TYPE_NOTIFICATION
                ChimeChoice.CHIME -> RingtoneManager.TYPE_NOTIFICATION
                ChimeChoice.BELL -> RingtoneManager.TYPE_ALARM
            }
            runCatching {
                val uri = RingtoneManager.getDefaultUri(type)
                RingtoneManager.getRingtone(context.applicationContext, uri)?.play()
            }
        }
        if (settings.hapticsEnabled) {
            runCatching {
                val pattern = longArrayOf(0, 120, 90, 180)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    manager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        }
    }
}
