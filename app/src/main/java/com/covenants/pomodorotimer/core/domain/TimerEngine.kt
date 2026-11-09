package com.covenants.pomodorotimer.core.domain

import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.NextStagePlan
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerSnapshot
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.core.time.TimeSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Pure timer transitions. Every platform surface (UI, notification action, alarm receiver)
 * funnels into the same repository which applies these functions inside a transaction.
 */
object TimerEngine {

    private const val MINUTE_MS = 60_000L

    fun newSession(
        sessionId: String,
        mode: TimerMode,
        stage: FocusStage?,
        stageIndex: Int,
        durationMs: Long,
        clock: TimeSource,
        completedWorkCount: Int = 0,
        taskId: String? = null,
        taskTitle: String? = null,
        startRunning: Boolean = true,
    ): TimerSnapshot {
        val nowElapsed = clock.elapsedRealtimeMs()
        val nowWall = clock.wallClockMs()
        val running = startRunning && mode != TimerMode.STOPWATCH
        return TimerSnapshot(
            sessionId = sessionId,
            mode = mode,
            status = if (running) TimerStatus.RUNNING else TimerStatus.IDLE,
            stage = stage,
            stageIndex = stageIndex,
            totalDurationMs = durationMs,
            remainingWhenPausedMs = durationMs,
            endElapsedRealtimeMs = if (running) nowElapsed + durationMs else null,
            expectedWallEndMs = if (running) nowWall + durationMs else null,
            bootMarker = if (running) clock.bootMarker() else null,
            accumulatedActiveMs = 0L,
            activeStartedElapsedMs = if (running) nowElapsed else null,
            taskId = taskId,
            taskTitle = taskTitle,
            startedWallMs = nowWall,
            version = 1L,
            completedWorkCount = completedWorkCount,
        )
    }

    fun newStopwatch(
        sessionId: String,
        clock: TimeSource,
        taskId: String? = null,
        taskTitle: String? = null,
    ): TimerSnapshot = TimerSnapshot(
        sessionId = sessionId,
        mode = TimerMode.STOPWATCH,
        status = TimerStatus.RUNNING,
        stage = null,
        stageIndex = 0,
        totalDurationMs = 0L,
        remainingWhenPausedMs = 0L,
        endElapsedRealtimeMs = null,
        expectedWallEndMs = null,
        bootMarker = clock.bootMarker(),
        accumulatedActiveMs = 0L,
        activeStartedElapsedMs = clock.elapsedRealtimeMs(),
        taskId = taskId,
        taskTitle = taskTitle,
        startedWallMs = clock.wallClockMs(),
        version = 1L,
        completedWorkCount = 0,
    )

    fun remainingMs(snapshot: TimerSnapshot, clock: TimeSource): Long = when (snapshot.status) {
        TimerStatus.RUNNING -> when (snapshot.mode) {
            TimerMode.STOPWATCH -> stopwatchElapsedMs(snapshot, clock)
            else -> {
                val deadline = snapshot.endElapsedRealtimeMs
                if (deadline != null && isSameBoot(snapshot.bootMarker, clock.bootMarker())) {
                    max(0L, deadline - clock.elapsedRealtimeMs())
                } else {
                    // Reboot or ambiguous clock identity: reconstruct from the wall anchor.
                    val wallDeadline = snapshot.expectedWallEndMs ?: clock.wallClockMs()
                    max(0L, wallDeadline - clock.wallClockMs())
                }
            }
        }
        TimerStatus.PAUSED -> when (snapshot.mode) {
            TimerMode.STOPWATCH -> stopwatchElapsedMs(snapshot, clock)
            else -> snapshot.remainingWhenPausedMs.coerceAtLeast(0L)
        }
        TimerStatus.IDLE -> snapshot.totalDurationMs
        TimerStatus.COMPLETED, TimerStatus.CANCELED -> 0L
    }

    fun stopwatchElapsedMs(snapshot: TimerSnapshot, clock: TimeSource): Long {
        val runningPart = if (snapshot.status == TimerStatus.RUNNING) {
            snapshot.activeStartedElapsedMs?.let { (clock.elapsedRealtimeMs() - it).coerceAtLeast(0L) } ?: 0L
        } else {
            0L
        }
        return snapshot.accumulatedActiveMs + runningPart
    }

    fun isDue(snapshot: TimerSnapshot, clock: TimeSource): Boolean =
        snapshot.mode != TimerMode.STOPWATCH &&
            snapshot.status == TimerStatus.RUNNING &&
            remainingMs(snapshot, clock) <= 0L

    fun progress(snapshot: TimerSnapshot, clock: TimeSource): Float {
        if (snapshot.mode == TimerMode.STOPWATCH) {
            val elapsed = stopwatchElapsedMs(snapshot, clock)
            return ((elapsed % MINUTE_MS).toFloat() / MINUTE_MS.toFloat()).coerceIn(0f, 1f)
        }
        if (snapshot.totalDurationMs <= 0L) return 0f
        val remaining = remainingMs(snapshot, clock)
        return ((snapshot.totalDurationMs - remaining).toFloat() / snapshot.totalDurationMs.toFloat())
            .coerceIn(0f, 1f)
    }

    fun pause(snapshot: TimerSnapshot, clock: TimeSource): TimerSnapshot {
        if (snapshot.status != TimerStatus.RUNNING) return snapshot
        val remaining = remainingMs(snapshot, clock)
        val extraActive = snapshot.activeStartedElapsedMs
            ?.let { (clock.elapsedRealtimeMs() - it).coerceAtLeast(0L) }
            ?: 0L
        return snapshot.copy(
            status = TimerStatus.PAUSED,
            remainingWhenPausedMs = if (snapshot.mode == TimerMode.STOPWATCH) {
                stopwatchElapsedMs(snapshot, clock)
            } else {
                remaining
            },
            endElapsedRealtimeMs = null,
            expectedWallEndMs = null,
            accumulatedActiveMs = snapshot.accumulatedActiveMs + extraActive,
            activeStartedElapsedMs = null,
            version = snapshot.version + 1,
        )
    }

    fun resume(snapshot: TimerSnapshot, clock: TimeSource): TimerSnapshot {
        if (snapshot.status != TimerStatus.PAUSED) return snapshot
        val nowElapsed = clock.elapsedRealtimeMs()
        val nowWall = clock.wallClockMs()
        val remaining = snapshot.remainingWhenPausedMs.coerceAtLeast(0L)
        return if (snapshot.mode == TimerMode.STOPWATCH) {
            snapshot.copy(
                status = TimerStatus.RUNNING,
                activeStartedElapsedMs = nowElapsed,
                version = snapshot.version + 1,
            )
        } else {
            snapshot.copy(
                status = TimerStatus.RUNNING,
                endElapsedRealtimeMs = nowElapsed + remaining,
                expectedWallEndMs = nowWall + remaining,
                bootMarker = clock.bootMarker(),
                activeStartedElapsedMs = nowElapsed,
                version = snapshot.version + 1,
            )
        }
    }

    fun extend(snapshot: TimerSnapshot, extraMs: Long, clock: TimeSource): TimerSnapshot {
        if (snapshot.mode == TimerMode.STOPWATCH || extraMs <= 0L) return snapshot
        return when (snapshot.status) {
            TimerStatus.RUNNING -> snapshot.copy(
                totalDurationMs = snapshot.totalDurationMs + extraMs,
                endElapsedRealtimeMs = (snapshot.endElapsedRealtimeMs ?: clock.elapsedRealtimeMs()) + extraMs,
                expectedWallEndMs = (snapshot.expectedWallEndMs ?: clock.wallClockMs()) + extraMs,
                version = snapshot.version + 1,
            )
            TimerStatus.PAUSED -> snapshot.copy(
                totalDurationMs = snapshot.totalDurationMs + extraMs,
                remainingWhenPausedMs = snapshot.remainingWhenPausedMs + extraMs,
                version = snapshot.version + 1,
            )
            else -> snapshot
        }
    }

    fun complete(snapshot: TimerSnapshot, clock: TimeSource): TimerSnapshot {
        if (snapshot.status != TimerStatus.RUNNING) return snapshot
        val extraActive = snapshot.activeStartedElapsedMs
            ?.let { (clock.elapsedRealtimeMs() - it).coerceAtLeast(0L) }
            ?: 0L
        return snapshot.copy(
            status = TimerStatus.COMPLETED,
            remainingWhenPausedMs = 0L,
            endElapsedRealtimeMs = null,
            expectedWallEndMs = null,
            accumulatedActiveMs = snapshot.accumulatedActiveMs + extraActive,
            activeStartedElapsedMs = null,
            version = snapshot.version + 1,
        )
    }

    fun stop(snapshot: TimerSnapshot, clock: TimeSource): TimerSnapshot {
        if (snapshot.status != TimerStatus.RUNNING && snapshot.status != TimerStatus.PAUSED) return snapshot
        val remaining = remainingMs(snapshot, clock)
        val extraActive = if (snapshot.status == TimerStatus.RUNNING) {
            snapshot.activeStartedElapsedMs
                ?.let { (clock.elapsedRealtimeMs() - it).coerceAtLeast(0L) }
                ?: 0L
        } else {
            0L
        }
        return snapshot.copy(
            status = TimerStatus.CANCELED,
            remainingWhenPausedMs = if (snapshot.mode == TimerMode.STOPWATCH) {
                stopwatchElapsedMs(snapshot, clock)
            } else {
                remaining
            },
            endElapsedRealtimeMs = null,
            expectedWallEndMs = null,
            accumulatedActiveMs = snapshot.accumulatedActiveMs + extraActive,
            activeStartedElapsedMs = null,
            version = snapshot.version + 1,
        )
    }

    /**
     * The stage after [snapshot]. A completed work stage increments the cycle count; a skipped
     * work stage does not. Only completed work stages can trigger a long break.
     */
    fun planNextStage(
        snapshot: TimerSnapshot,
        routine: RoutineConfig,
        completedWork: Boolean,
    ): NextStagePlan {
        val currentStage = snapshot.stage ?: FocusStage.WORK
        val nextIndex = snapshot.stageIndex + 1
        return if (currentStage == FocusStage.WORK) {
            val newCount = snapshot.completedWorkCount + if (completedWork) 1 else 0
            val longBreak = newCount > 0 && newCount % routine.longBreakEverySafe == 0
            val stage = if (longBreak) FocusStage.LONG_BREAK else FocusStage.SHORT_BREAK
            NextStagePlan(
                stage = stage,
                stageIndex = nextIndex,
                durationMs = routine.durationFor(stage),
                completedWorkCount = newCount,
                autoStart = routine.autoStartBreaks,
            )
        } else {
            NextStagePlan(
                stage = FocusStage.WORK,
                stageIndex = nextIndex,
                durationMs = routine.durationFor(FocusStage.WORK),
                completedWorkCount = snapshot.completedWorkCount,
                autoStart = routine.autoStartFocus,
            )
        }
    }

    fun activeDurationMs(snapshot: TimerSnapshot, clock: TimeSource): Long {
        if (snapshot.mode == TimerMode.STOPWATCH) return stopwatchElapsedMs(snapshot, clock)
        val live = if (snapshot.status == TimerStatus.RUNNING) {
            snapshot.activeStartedElapsedMs
                ?.let { (clock.elapsedRealtimeMs() - it).coerceAtLeast(0L) }
                ?: 0L
        } else {
            0L
        }
        return (snapshot.accumulatedActiveMs + live).coerceAtMost(snapshot.totalDurationMs)
    }

    private fun isSameBoot(persisted: String?, current: String?): Boolean {
        if (persisted == null || current == null) return false
        if (persisted == current) return true
        // Legacy/fallback offset markers tolerate sub-bucket drift.
        if (persisted.startsWith("offset:") && current.startsWith("offset:")) {
            val a = persisted.removePrefix("offset:").toLongOrNull() ?: return false
            val b = current.removePrefix("offset:").toLongOrNull() ?: return false
            return abs(a - b) <= 1L
        }
        return false
    }
}

object TimeFormat {
    private val finishFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    /** Countdown display rounds up so a fresh 25:00 session reads 25:00, not 24:59. */
    fun countdown(ms: Long): String {
        val totalSeconds = ceil(ms.coerceAtLeast(0L) / 1000.0).toLong()
        return clock(totalSeconds)
    }

    fun stopwatch(ms: Long): String = clock(ms.coerceAtLeast(0L) / 1000L)

    private fun clock(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun durationLabel(ms: Long): String {
        val totalMinutes = ms.coerceAtLeast(0L) / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun finishAt(epochMs: Long, zoneId: String): String =
        finishFormatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.of(zoneId)))

    fun eyebrowDate(zoneId: String): String {
        val now = Instant.now().atZone(ZoneId.of(zoneId))
        val day = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()).format(now).uppercase(Locale.getDefault())
        val date = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()).format(now).uppercase(Locale.getDefault())
        return "$day · $date"
    }
}
