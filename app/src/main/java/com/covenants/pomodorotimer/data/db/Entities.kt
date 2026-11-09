package com.covenants.pomodorotimer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.FocusTask
import com.covenants.pomodorotimer.core.model.SessionHistory
import com.covenants.pomodorotimer.core.model.SessionOutcome
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerSnapshot
import com.covenants.pomodorotimer.core.model.TimerStatus

@Entity(tableName = "timer_session")
data class TimerSessionEntity(
    @PrimaryKey val sessionId: String,
    val mode: String,
    val status: String,
    val focusStage: String?,
    val stageIndex: Int,
    val totalDurationMs: Long,
    val remainingPausedMs: Long,
    val deadlineElapsedMs: Long?,
    val wallDeadlineMs: Long?,
    val bootMarker: String?,
    val taskId: String?,
    val taskTitle: String?,
    val startedWallMs: Long,
    val completedWallMs: Long?,
    val version: Long,
    val accumulatedActiveMs: Long,
    val activeStartedElapsedMs: Long?,
    val activeStartedWallMs: Long?,
    val completedWorkCount: Int,
)

@Entity(
    tableName = "session_history",
    indices = [Index("endedWallMs"), Index("mode"), Index("outcome")],
)
data class SessionHistoryEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val stage: String?,
    val outcome: String,
    val totalDurationMs: Long,
    val activeDurationMs: Long,
    val startedWallMs: Long,
    val endedWallMs: Long,
    val taskId: String?,
    val taskTitle: String?,
    val stageIndex: Int,
)

@Entity(
    tableName = "session_event",
    indices = [Index("sessionId")],
)
data class SessionEventEntity(
    @PrimaryKey val eventId: String,
    val sessionId: String,
    val stageIndex: Int,
    val type: String,
    val wallMs: Long,
    val elapsedMs: Long?,
)

@Entity(tableName = "focus_task")
data class FocusTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val done: Boolean,
    val estimateSessions: Int,
    val completedSessions: Int,
    val tag: String?,
    val createdAtMs: Long,
    val archivedAtMs: Long?,
)

fun TimerSessionEntity.toSnapshot(): TimerSnapshot = TimerSnapshot(
    sessionId = sessionId,
    mode = enumOr(TimerMode.FOCUS, mode),
    status = enumOr(TimerStatus.IDLE, status),
    stage = focusStage?.let { enumOr(FocusStage.WORK, it) },
    stageIndex = stageIndex,
    totalDurationMs = totalDurationMs,
    remainingWhenPausedMs = remainingPausedMs,
    endElapsedRealtimeMs = deadlineElapsedMs,
    expectedWallEndMs = wallDeadlineMs,
    bootMarker = bootMarker,
    accumulatedActiveMs = accumulatedActiveMs,
    activeStartedElapsedMs = activeStartedElapsedMs,
    activeStartedWallMs = activeStartedWallMs,
    taskId = taskId,
    taskTitle = taskTitle,
    startedWallMs = startedWallMs,
    version = version,
    completedWorkCount = completedWorkCount,
)

fun TimerSnapshot.toEntity(completedWallMs: Long? = null): TimerSessionEntity = TimerSessionEntity(
    sessionId = sessionId,
    mode = mode.name,
    status = status.name,
    focusStage = stage?.name,
    stageIndex = stageIndex,
    totalDurationMs = totalDurationMs,
    remainingPausedMs = remainingWhenPausedMs,
    deadlineElapsedMs = endElapsedRealtimeMs,
    wallDeadlineMs = expectedWallEndMs,
    bootMarker = bootMarker,
    taskId = taskId,
    taskTitle = taskTitle,
    startedWallMs = startedWallMs,
    completedWallMs = completedWallMs,
    version = version,
    accumulatedActiveMs = accumulatedActiveMs,
    activeStartedElapsedMs = activeStartedElapsedMs,
    activeStartedWallMs = activeStartedWallMs,
    completedWorkCount = completedWorkCount,
)

fun SessionHistoryEntity.toModel(): SessionHistory = SessionHistory(
    id = id,
    mode = enumOr(TimerMode.FOCUS, mode),
    stage = stage?.let { enumOr(FocusStage.WORK, it) },
    outcome = enumOr(SessionOutcome.STOPPED, outcome),
    totalDurationMs = totalDurationMs,
    activeDurationMs = activeDurationMs,
    startedWallMs = startedWallMs,
    endedWallMs = endedWallMs,
    taskId = taskId,
    taskTitle = taskTitle,
    stageIndex = stageIndex,
)

fun SessionHistory.toEntity(): SessionHistoryEntity = SessionHistoryEntity(
    id = id,
    mode = mode.name,
    stage = stage?.name,
    outcome = outcome.name,
    totalDurationMs = totalDurationMs,
    activeDurationMs = activeDurationMs,
    startedWallMs = startedWallMs,
    endedWallMs = endedWallMs,
    taskId = taskId,
    taskTitle = taskTitle,
    stageIndex = stageIndex,
)

fun FocusTaskEntity.toModel(): FocusTask = FocusTask(
    id = id,
    title = title,
    done = done,
    estimateSessions = estimateSessions,
    completedSessions = completedSessions,
    tag = tag,
    createdAtMs = createdAtMs,
)

fun FocusTask.toEntity(): FocusTaskEntity = FocusTaskEntity(
    id = id,
    title = title,
    done = done,
    estimateSessions = estimateSessions,
    completedSessions = completedSessions,
    tag = tag,
    createdAtMs = createdAtMs,
    archivedAtMs = null,
)

private inline fun <reified T : Enum<T>> enumOr(fallback: T, value: String): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
