package com.covenants.pomodorotimer.data

import androidx.room.withTransaction
import com.covenants.pomodorotimer.core.domain.TimerEngine
import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.FocusTask
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.SessionHistory
import com.covenants.pomodorotimer.core.model.SessionOutcome
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerSnapshot
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.core.time.TimeSource
import com.covenants.pomodorotimer.data.db.AppDatabase
import com.covenants.pomodorotimer.data.db.SessionEventEntity
import com.covenants.pomodorotimer.data.db.SessionHistoryEntity
import com.covenants.pomodorotimer.data.db.TaskDao
import com.covenants.pomodorotimer.data.db.TimerDao
import com.covenants.pomodorotimer.data.db.toEntity
import com.covenants.pomodorotimer.data.db.toModel
import com.covenants.pomodorotimer.data.db.toSnapshot
import com.covenants.pomodorotimer.platform.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed interface TimerEvent {
    data class StageCompleted(val stage: FocusStage?, val nextStage: FocusStage?, val autoStarted: Boolean) : TimerEvent
    data object CountdownCompleted : TimerEvent
    data object Stopped : TimerEvent
    data class Started(val mode: TimerMode) : TimerEvent
    data class Message(val text: String) : TimerEvent
}

data class CompletionResult(
    val mode: TimerMode,
    val stage: FocusStage?,
    val nextStage: FocusStage?,
    val autoStarted: Boolean,
)

/**
 * Single command processor for the whole app. UI, notifications and alarms all call these
 * suspend functions; all state changes are serialized and committed before platform side effects.
 */
class TimerRepository(
    private val db: AppDatabase,
    private val timerDao: TimerDao,
    private val taskDao: TaskDao,
    private val clock: TimeSource,
    private val scheduler: AlarmScheduler,
    private val settings: SettingsStore,
    scope: CoroutineScope,
) {
    private val mutex = Mutex()

    val active: StateFlow<TimerSnapshot?> = timerDao.observeActive()
        .map { it?.toSnapshot() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val routine: StateFlow<RoutineConfig> = settings.routine
        .stateIn(scope, SharingStarted.Eagerly, RoutineConfig())

    val history: StateFlow<List<SessionHistory>> = timerDao.observeHistory()
        .map { list -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val tasks: StateFlow<List<FocusTask>> = taskDao.observeTasks()
        .map { list -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<TimerEvent> = _events

    suspend fun startFocus(taskId: String? = null, taskTitle: String? = null): TimerSnapshot =
        mutex.withLock {
            terminateExistingLocked()
            val currentRoutine = settings.routine.first()
            val snapshot = TimerEngine.newSession(
                sessionId = UUID.randomUUID().toString(),
                mode = TimerMode.FOCUS,
                stage = FocusStage.WORK,
                stageIndex = 0,
                durationMs = currentRoutine.focusMs,
                clock = clock,
                completedWorkCount = 0,
                taskId = taskId,
                taskTitle = taskTitle,
            )
            persistLocked(snapshot)
            _events.tryEmit(TimerEvent.Started(TimerMode.FOCUS))
            snapshot
        }

    suspend fun startCountdown(durationMs: Long): TimerSnapshot = mutex.withLock {
        require(durationMs > 0) { "Countdown must be positive" }
        terminateExistingLocked()
        val snapshot = TimerEngine.newSession(
            sessionId = UUID.randomUUID().toString(),
            mode = TimerMode.COUNTDOWN,
            stage = null,
            stageIndex = 0,
            durationMs = durationMs,
            clock = clock,
        )
        persistLocked(snapshot)
        _events.tryEmit(TimerEvent.Started(TimerMode.COUNTDOWN))
        snapshot
    }

    suspend fun startStopwatch(): TimerSnapshot = mutex.withLock {
        terminateExistingLocked()
        val snapshot = TimerEngine.newStopwatch(UUID.randomUUID().toString(), clock)
        persistLocked(snapshot)
        _events.tryEmit(TimerEvent.Started(TimerMode.STOPWATCH))
        snapshot
    }

    /** Start the stage that is currently waiting in the IDLE state (a break or the next focus). */
    suspend fun startBreakNow(): TimerSnapshot? = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock null
        val stage = current.stage ?: return@withLock null
        if (current.status != TimerStatus.IDLE) return@withLock null
        val snapshot = TimerEngine.newSession(
            sessionId = UUID.randomUUID().toString(),
            mode = TimerMode.FOCUS,
            stage = stage,
            stageIndex = current.stageIndex,
            durationMs = current.totalDurationMs,
            clock = clock,
            completedWorkCount = current.completedWorkCount,
            taskId = current.taskId,
            taskTitle = current.taskTitle,
        )
        persistLocked(snapshot)
        snapshot
    }

    suspend fun pause(): TimerSnapshot? = mutex.withLock { pauseLocked() }

    suspend fun resume(): TimerSnapshot? = mutex.withLock { resumeLocked() }

    suspend fun togglePause(): TimerSnapshot? = mutex.withLock {
        // Read from the database inside the lock. The in-memory StateFlow may not have loaded
        // yet after Android restarts the process to deliver a notification action.
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock null
        when (current.status) {
            TimerStatus.RUNNING -> pauseLocked()
            TimerStatus.PAUSED -> resumeLocked()
            else -> current
        }
    }

    private suspend fun pauseLocked(): TimerSnapshot? {
        val current = timerDao.getActive()?.toSnapshot() ?: return null
        val paused = TimerEngine.pause(current, clock)
        if (paused == current) return current
        persistLocked(paused)
        return paused
    }

    private suspend fun resumeLocked(): TimerSnapshot? {
        val current = timerDao.getActive()?.toSnapshot() ?: return null
        val resumed = TimerEngine.resume(current, clock)
        if (resumed == current) return current
        persistLocked(resumed)
        return resumed
    }

    suspend fun addFiveMinutes(): TimerSnapshot? = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock null
        val extended = TimerEngine.extend(current, 5 * 60_000L, clock)
        if (extended == current) return@withLock current
        persistLocked(extended)
        extended
    }

    suspend fun stop(): TimerSnapshot? = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock null
        if (current.status != TimerStatus.RUNNING && current.status != TimerStatus.PAUSED) {
            return@withLock current
        }
        recordOutcomeLocked(current, SessionOutcome.STOPPED)
        timerDao.clearActive()
        scheduler.cancel()
        _events.tryEmit(TimerEvent.Stopped)
        null
    }

    suspend fun skip(): TimerSnapshot? = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock null
        if (current.status == TimerStatus.COMPLETED) {
            timerDao.clearActive()
            scheduler.cancel()
            return@withLock null
        }
        recordOutcomeLocked(current, SessionOutcome.SKIPPED)
        if (current.mode == TimerMode.FOCUS && current.stage != null) {
            val plan = TimerEngine.planNextStage(current, settings.routine.first(), completedWork = false)
            val next = TimerEngine.newSession(
                sessionId = UUID.randomUUID().toString(),
                mode = TimerMode.FOCUS,
                stage = plan.stage,
                stageIndex = plan.stageIndex,
                durationMs = plan.durationMs,
                clock = clock,
                completedWorkCount = plan.completedWorkCount,
                taskId = current.taskId,
                taskTitle = current.taskTitle,
                startRunning = plan.autoStart,
            )
            persistLocked(next)
            _events.tryEmit(
                TimerEvent.StageCompleted(current.stage, plan.stage, plan.autoStart)
            )
            next
        } else {
            timerDao.clearActive()
            scheduler.cancel()
            null
        }
    }

    /** Dismiss a waiting break without recording a skipped stage. */
    suspend fun dismissWaitingBreak() = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock
        if (current.status == TimerStatus.IDLE && current.stage != null && current.stage != FocusStage.WORK) {
            timerDao.clearActive()
            scheduler.cancel()
        }
    }

    suspend fun acknowledgeCompleted() = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock
        if (current.status == TimerStatus.COMPLETED || current.status == TimerStatus.CANCELED) {
            timerDao.clearActive()
            scheduler.cancel()
        }
    }

    /**
     * Called by the alarm receiver, app restore and notification actions. Idempotent: a replayed
     * alarm cannot create a second history row because the completion event id is deterministic.
     */
    suspend fun completeDue(
        sessionId: String? = null,
        stageIndex: Int? = null,
        version: Long? = null,
    ): CompletionResult? = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: return@withLock null
        if (sessionId != null && current.sessionId != sessionId) return@withLock null
        if (stageIndex != null && current.stageIndex != stageIndex) return@withLock null
        if (current.status != TimerStatus.RUNNING) {
            scheduler.cancel()
            return@withLock null
        }
        if (version != null && version != current.version) {
            // Stale alarm identity. The authoritative check below decides what happens.
            if (!TimerEngine.isDue(current, clock)) {
                scheduler.schedule(current, settings.routine.first())
                return@withLock null
            }
        }
        if (!TimerEngine.isDue(current, clock)) {
            scheduler.schedule(current, settings.routine.first())
            return@withLock null
        }
        handleCompletionLocked(current)
    }

    suspend fun reconcile(): CompletionResult? = mutex.withLock {
        val current = timerDao.getActive()?.toSnapshot() ?: run {
            scheduler.cancel()
            return@withLock null
        }
        when {
            current.status == TimerStatus.RUNNING && TimerEngine.isDue(current, clock) -> {
                handleCompletionLocked(current)
            }
            current.status == TimerStatus.RUNNING -> {
                scheduler.schedule(current, settings.routine.first())
                null
            }
            else -> {
                scheduler.cancel()
                null
            }
        }
    }

    suspend fun setLastMode(mode: TimerMode) = settings.setLastMode(mode)

    suspend fun addTask(title: String, tag: String?, estimateSessions: Int) {
        if (title.isBlank()) return
        val task = FocusTask(
            id = UUID.randomUUID().toString(),
            title = title.trim().take(120),
            done = false,
            estimateSessions = estimateSessions.coerceIn(1, 12),
            tag = tag?.trim()?.takeIf { it.isNotBlank() }?.take(24),
            createdAtMs = clock.wallClockMs(),
        )
        taskDao.upsert(task.toEntity())
    }

    suspend fun toggleTask(task: FocusTask) {
        taskDao.upsert(task.copy(done = !task.done).toEntity())
    }

    suspend fun deleteTask(task: FocusTask) {
        taskDao.delete(task.id)
    }

    suspend fun deleteAllHistory() {
        db.withTransaction {
            timerDao.deleteAllHistory()
            timerDao.deleteAllEvents()
        }
    }

    suspend fun allHistory(): List<SessionHistory> = timerDao.getAllHistory().map { it.toModel() }

    private suspend fun terminateExistingLocked() {
        val current = timerDao.getActive()?.toSnapshot() ?: return
        if (current.status == TimerStatus.RUNNING || current.status == TimerStatus.PAUSED) {
            recordOutcomeLocked(current, SessionOutcome.STOPPED)
        }
        timerDao.clearActive()
        scheduler.cancel()
    }

    private suspend fun handleCompletionLocked(current: TimerSnapshot): CompletionResult {
        val completed = TimerEngine.complete(current, clock)
        recordOutcomeLocked(completed, SessionOutcome.COMPLETED, useCompleted = true)
        return if (current.mode == TimerMode.FOCUS && current.stage != null) {
            val plan = TimerEngine.planNextStage(completed, settings.routine.first(), completedWork = true)
            val next = TimerEngine.newSession(
                sessionId = UUID.randomUUID().toString(),
                mode = TimerMode.FOCUS,
                stage = plan.stage,
                stageIndex = plan.stageIndex,
                durationMs = plan.durationMs,
                clock = clock,
                completedWorkCount = plan.completedWorkCount,
                taskId = completed.taskId,
                taskTitle = completed.taskTitle,
                startRunning = plan.autoStart,
            )
            persistLocked(next)
            _events.tryEmit(TimerEvent.StageCompleted(current.stage, plan.stage, plan.autoStart))
            CompletionResult(
                mode = TimerMode.FOCUS,
                stage = current.stage,
                nextStage = plan.stage,
                autoStarted = plan.autoStart,
            )
        } else {
            // Countdown: keep the completed snapshot so the UI can show the outcome. No next stage.
            db.withTransaction {
                timerDao.clearActive()
                timerDao.upsertSession(completed.toEntity(completedWallMs = clock.wallClockMs()))
            }
            scheduler.cancel()
            _events.tryEmit(TimerEvent.CountdownCompleted)
            CompletionResult(
                mode = current.mode,
                stage = current.stage,
                nextStage = null,
                autoStarted = false,
            )
        }
    }

    private suspend fun persistLocked(snapshot: TimerSnapshot) {
        db.withTransaction {
            timerDao.clearActive()
            timerDao.upsertSession(snapshot.toEntity())
        }
        when {
            snapshot.status == TimerStatus.RUNNING && snapshot.mode != TimerMode.STOPWATCH ->
                scheduler.schedule(snapshot, settings.routine.first())
            else -> scheduler.cancel()
        }
    }

    private suspend fun recordOutcomeLocked(
        snapshot: TimerSnapshot,
        outcome: SessionOutcome,
        useCompleted: Boolean = false,
    ) {
        val stageKey = snapshot.stageIndex
        val eventId = "${snapshot.sessionId}:$stageKey:${outcome.name}"
        val now = clock.wallClockMs()
        val activeMs = if (useCompleted) {
            snapshot.totalDurationMs
        } else {
            TimerEngine.activeDurationMs(snapshot, clock)
        }
        val totalMs = when (snapshot.mode) {
            TimerMode.STOPWATCH -> TimerEngine.stopwatchElapsedMs(snapshot, clock)
            else -> snapshot.totalDurationMs
        }
        val historyId = "${snapshot.sessionId}:$stageKey:${outcome.name}"
        val history = SessionHistory(
            id = historyId,
            mode = snapshot.mode,
            stage = snapshot.stage,
            outcome = outcome,
            totalDurationMs = totalMs,
            activeDurationMs = activeMs,
            startedWallMs = snapshot.startedWallMs,
            endedWallMs = now,
            taskId = snapshot.taskId,
            taskTitle = snapshot.taskTitle,
            stageIndex = stageKey,
        ).toEntity()
        db.withTransaction {
            val inserted = timerDao.insertEvent(
                SessionEventEntity(
                    eventId = eventId,
                    sessionId = snapshot.sessionId,
                    stageIndex = stageKey,
                    type = outcome.name,
                    wallMs = now,
                    elapsedMs = clock.elapsedRealtimeMs(),
                )
            )
            // -1 means the event already existed; the history write is therefore a duplicate.
            if (inserted != -1L) {
                timerDao.insertHistory(history)
                if (outcome == SessionOutcome.COMPLETED &&
                    snapshot.mode == TimerMode.FOCUS &&
                    snapshot.stage == FocusStage.WORK &&
                    snapshot.taskId != null
                ) {
                    taskDao.incrementCompletedSessions(snapshot.taskId)
                }
            }
        }
    }
}
