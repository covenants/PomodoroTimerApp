package com.covenants.pomodorotimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.ChimeChoice
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.ThemeChoice
import com.covenants.pomodorotimer.core.model.TimerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "orbit_settings")

class SettingsStore(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private val safeData: Flow<Preferences> = store.data.catch { error ->
        if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
    }

    val settings: Flow<AppSettings> = safeData.map { prefs ->
        AppSettings(
            theme = prefs[Keys.THEME].toEnum(ThemeChoice.MIDNIGHT),
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            soundEnabled = prefs[Keys.SOUND] ?: true,
            chime = prefs[Keys.CHIME].toEnum(ChimeChoice.CHIME),
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            lastMode = prefs[Keys.LAST_MODE].toEnum(TimerMode.FOCUS),
            onboardingSeen = prefs[Keys.ONBOARDING] ?: false,
        )
    }

    val routine: Flow<RoutineConfig> = safeData.map { prefs ->
        RoutineConfig(
            name = prefs[Keys.ROUTINE_NAME] ?: "Deep Work",
            focusMinutes = prefs[Keys.FOCUS_MINUTES] ?: 25,
            shortBreakMinutes = prefs[Keys.SHORT_BREAK] ?: 5,
            longBreakMinutes = prefs[Keys.LONG_BREAK] ?: 15,
            longBreakEvery = prefs[Keys.LONG_EVERY] ?: 4,
            autoStartBreaks = prefs[Keys.AUTO_BREAKS] ?: false,
            autoStartFocus = prefs[Keys.AUTO_FOCUS] ?: false,
            haptics = prefs[Keys.ROUTINE_HAPTICS] ?: true,
        )
    }

    suspend fun setTheme(value: ThemeChoice) = put(Keys.THEME, value.name)
    suspend fun setReduceMotion(value: Boolean) = put(Keys.REDUCE_MOTION, value)
    suspend fun setHighContrast(value: Boolean) = put(Keys.HIGH_CONTRAST, value)
    suspend fun setNotifications(value: Boolean) = put(Keys.NOTIFICATIONS, value)
    suspend fun setSound(value: Boolean) = put(Keys.SOUND, value)
    suspend fun setChime(value: ChimeChoice) = put(Keys.CHIME, value.name)
    suspend fun setHaptics(value: Boolean) = put(Keys.HAPTICS, value)
    suspend fun setLastMode(value: TimerMode) = put(Keys.LAST_MODE, value.name)
    suspend fun setOnboardingSeen(value: Boolean) = put(Keys.ONBOARDING, value)

    suspend fun saveRoutine(routine: RoutineConfig) {
        store.edit { prefs ->
            prefs[Keys.ROUTINE_NAME] = routine.name.ifBlank { "Deep Work" }.take(40)
            prefs[Keys.FOCUS_MINUTES] = routine.focusMinutes.coerceIn(1, 180)
            prefs[Keys.SHORT_BREAK] = routine.shortBreakMinutes.coerceIn(1, 60)
            prefs[Keys.LONG_BREAK] = routine.longBreakMinutes.coerceIn(1, 120)
            prefs[Keys.LONG_EVERY] = routine.longBreakEvery.coerceIn(2, 12)
            prefs[Keys.AUTO_BREAKS] = routine.autoStartBreaks
            prefs[Keys.AUTO_FOCUS] = routine.autoStartFocus
            prefs[Keys.ROUTINE_HAPTICS] = routine.haptics
        }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val SOUND = booleanPreferencesKey("sound")
        val CHIME = stringPreferencesKey("chime")
        val HAPTICS = booleanPreferencesKey("haptics")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val ONBOARDING = booleanPreferencesKey("onboarding_seen")
        val ROUTINE_NAME = stringPreferencesKey("routine_name")
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val SHORT_BREAK = intPreferencesKey("short_break")
        val LONG_BREAK = intPreferencesKey("long_break")
        val LONG_EVERY = intPreferencesKey("long_every")
        val AUTO_BREAKS = booleanPreferencesKey("auto_breaks")
        val AUTO_FOCUS = booleanPreferencesKey("auto_focus")
        val ROUTINE_HAPTICS = booleanPreferencesKey("routine_haptics")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
