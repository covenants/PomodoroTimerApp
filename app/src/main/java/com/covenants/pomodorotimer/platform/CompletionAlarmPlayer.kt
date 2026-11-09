package com.covenants.pomodorotimer.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.ChimeChoice

/**
 * Sound is normally played by the high-importance notification channel on the alarm stream.
 * This helper only plays a direct fallback when notifications are denied, and always handles the
 * optional haptic so the user's toggle stays truthful.
 */
object CompletionAlarmPlayer {

    fun deliver(context: Context, settings: AppSettings, playSoundFallback: Boolean) {
        if (playSoundFallback && settings.soundEnabled) {
            playSound(context, settings)
        }
        if (settings.hapticsEnabled) {
            vibrate(context)
        }
    }

    private fun playSound(context: Context, settings: AppSettings) {
        val type = when (settings.chime) {
            ChimeChoice.GENTLE -> RingtoneManager.TYPE_NOTIFICATION
            ChimeChoice.CHIME -> RingtoneManager.TYPE_NOTIFICATION
            ChimeChoice.BELL -> RingtoneManager.TYPE_ALARM
        }
        runCatching {
            val uri = RingtoneManager.getDefaultUri(type)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
        }
    }

    private fun vibrate(context: Context) {
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
