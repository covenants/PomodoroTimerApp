package com.covenants.pomodorotimer.core.model

/**
 * The three first-class timing experiences. One active session exists at a time.
 */
enum class TimerMode { FOCUS, COUNTDOWN, STOPWATCH }

enum class TimerStatus { IDLE, RUNNING, PAUSED, COMPLETED, CANCELED }

enum class FocusStage { WORK, SHORT_BREAK, LONG_BREAK }

enum class SessionOutcome { COMPLETED, STOPPED, SKIPPED }

enum class ThemeChoice { MIDNIGHT, WARM_PAPER, AURORA }

enum class ChimeChoice { GENTLE, CHIME, BELL }

data class RoutineConfig(
    val name: String = "Deep Work",
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
    val haptics: Boolean = true,
) {
    val focusMs: Long get() = focusMinutes.coerceIn(1, 180) * 60_000L
    val shortBreakMs: Long get() = shortBreakMinutes.coerceIn(1, 60) * 60_000L
    val longBreakMs: Long get() = longBreakMinutes.coerceIn(1, 120) * 60_000L
    val longBreakEverySafe: Int get() = longBreakEvery.coerceIn(2, 12)

    fun durationFor(stage: FocusStage): Long = when (stage) {
        FocusStage.WORK -> focusMs
        FocusStage.SHORT_BREAK -> shortBreakMs
        FocusStage.LONG_BREAK -> longBreakMs
    }
}

/**
 * Authoritative in-memory representation of the persisted timer session.
 *
 * Countdown correctness is derived from [endElapsedRealtimeMs] (same boot) or
 * [expectedWallEndMs] (conservative fallback after reboot). Nothing decrements a counter.
 */
data class TimerSnapshot(
    val sessionId: String,
    val mode: TimerMode,
    val status: TimerStatus,
    val stage: FocusStage?,
    val stageIndex: Int,
    val totalDurationMs: Long,
    val remainingWhenPausedMs: Long,
    val endElapsedRealtimeMs: Long?,
    val expectedWallEndMs: Long?,
    val bootMarker: String?,
    val accumulatedActiveMs: Long,
    val activeStartedElapsedMs: Long?,
    val activeStartedWallMs: Long? = null,
    val taskId: String?,
    val taskTitle: String?,
    val startedWallMs: Long,
    val version: Long,
    val completedWorkCount: Int,
) {
    val isActive: Boolean
        get() = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED

    val isFocusStage: Boolean
        get() = mode == TimerMode.FOCUS && stage != null
}

data class FocusTask(
    val id: String,
    val title: String,
    val done: Boolean,
    val estimateSessions: Int = 1,
    val completedSessions: Int = 0,
    val tag: String? = null,
    val createdAtMs: Long,
)

data class SessionHistory(
    val id: String,
    val mode: TimerMode,
    val stage: FocusStage?,
    val outcome: SessionOutcome,
    val totalDurationMs: Long,
    val activeDurationMs: Long,
    val startedWallMs: Long,
    val endedWallMs: Long,
    val taskId: String?,
    val taskTitle: String?,
    val stageIndex: Int,
)

data class AppSettings(
    val theme: ThemeChoice = ThemeChoice.MIDNIGHT,
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val chime: ChimeChoice = ChimeChoice.CHIME,
    val hapticsEnabled: Boolean = true,
    val fullScreenAlerts: Boolean = false,
    val lastMode: TimerMode = TimerMode.FOCUS,
    val onboardingSeen: Boolean = false,
)

/** A plan for the stage that follows a completed or skipped stage. */
data class NextStagePlan(
    val stage: FocusStage?,
    val stageIndex: Int,
    val durationMs: Long,
    val completedWorkCount: Int,
    val autoStart: Boolean,
)
