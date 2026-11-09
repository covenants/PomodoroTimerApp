package com.covenants.pomodorotimer.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.covenants.pomodorotimer.core.domain.TimerEngine
import com.covenants.pomodorotimer.core.model.AppSettings
import com.covenants.pomodorotimer.core.model.ChimeChoice
import com.covenants.pomodorotimer.core.model.FocusStage
import com.covenants.pomodorotimer.core.model.FocusTask
import com.covenants.pomodorotimer.core.model.RoutineConfig
import com.covenants.pomodorotimer.core.model.SessionHistory
import com.covenants.pomodorotimer.core.model.SessionOutcome
import com.covenants.pomodorotimer.core.model.ThemeChoice
import com.covenants.pomodorotimer.core.model.TimerMode
import com.covenants.pomodorotimer.core.model.TimerSnapshot
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.core.time.TimeSource
import com.covenants.pomodorotimer.AppContainer
import com.covenants.pomodorotimer.data.SettingsStore
import com.covenants.pomodorotimer.data.TimerEvent
import com.covenants.pomodorotimer.data.TimerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class AppUiState(
    val active: TimerSnapshot? = null,
    val remainingMs: Long = 0L,
    val stopwatchMs: Long = 0L,
    val progress: Float = 0f,
    val routine: RoutineConfig = RoutineConfig(),
    val settings: AppSettings = AppSettings(),
    val history: List<SessionHistory> = emptyList(),
    val todayFocusMs: Long = 0L,
    val todaySessions: Int = 0,
    val weekFocusMs: Long = 0L,
    val weekSessions: Int = 0,
    val streakDays: Int = 0,
    val mostFocusedWindow: String = "—",
    val dailyMinutes: List<Int> = List(7) { 0 },
    val heatmapDays: List<Int> = List(28) { 0 },
    val allTimeSessions: Int = 0,
    val nowWallMs: Long = System.currentTimeMillis(),
) {
    val isRunning: Boolean get() = active?.status == TimerStatus.RUNNING
    val isPaused: Boolean get() = active?.status == TimerStatus.PAUSED
    val isCompleted: Boolean get() = active?.status == TimerStatus.COMPLETED
    val isWaitingBreak: Boolean
        get() = active?.status == TimerStatus.IDLE && active.stage != null && active.stage != FocusStage.WORK
    val isWaitingFocus: Boolean
        get() = active?.status == TimerStatus.IDLE && active.stage == FocusStage.WORK
    val isIdle: Boolean get() = active == null
    val hasActiveSession: Boolean get() = active != null && active.status != TimerStatus.CANCELED
}

class MainViewModel(
    private val repository: TimerRepository,
    private val settingsStore: SettingsStore,
    private val clock: TimeSource,
    private val context: Context,
    private val reconcileAction: suspend () -> Unit,
) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(500L)
        }
    }

    val uiState: StateFlow<AppUiState> = combine(
        repository.active,
        repository.routine,
        settingsStore.settings,
        repository.history,
        ticker,
    ) { active, routine, settings, history, _ ->
        buildState(active, routine, settings, history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    val tasks: StateFlow<List<FocusTask>> = repository.tasks
    val events: SharedFlow<TimerEvent> = repository.events

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    init {
        viewModelScope.launch { reconcileAction() }
    }

    fun reconcile() {
        viewModelScope.launch { reconcileAction() }
    }

    fun selectMode(mode: TimerMode) {
        viewModelScope.launch { settingsStore.setLastMode(mode) }
    }

    fun startFocus(task: FocusTask? = null) {
        viewModelScope.launch {
            repository.startFocus(task?.id, task?.title)
            settingsStore.setLastMode(TimerMode.FOCUS)
        }
    }

    fun startCountdownMs(durationMs: Long) {
        viewModelScope.launch {
            repository.startCountdown(durationMs)
            settingsStore.setLastMode(TimerMode.COUNTDOWN)
        }
    }

    fun startStopwatch() {
        viewModelScope.launch {
            repository.startStopwatch()
            settingsStore.setLastMode(TimerMode.STOPWATCH)
        }
    }

    fun startBreakNow() {
        viewModelScope.launch { repository.startBreakNow() }
    }

    fun pause() {
        viewModelScope.launch { repository.pause() }
    }

    fun resume() {
        viewModelScope.launch { repository.resume() }
    }

    fun togglePause() {
        viewModelScope.launch { repository.togglePause() }
    }

    fun addFiveMinutes() {
        viewModelScope.launch { repository.addFiveMinutes() }
    }

    fun stop() {
        viewModelScope.launch { repository.stop() }
    }

    fun skip() {
        viewModelScope.launch { repository.skip() }
    }

    fun dismissWaitingBreak() {
        viewModelScope.launch { repository.dismissWaitingBreak() }
    }

    fun acknowledgeCompleted() {
        viewModelScope.launch { repository.acknowledgeCompleted() }
    }

    fun saveRoutine(routine: RoutineConfig) {
        viewModelScope.launch {
            settingsStore.saveRoutine(routine)
            _messages.tryEmit("Routine saved")
        }
    }

    fun setTheme(theme: ThemeChoice) {
        viewModelScope.launch { settingsStore.setTheme(theme) }
    }

    fun setReduceMotion(value: Boolean) {
        viewModelScope.launch { settingsStore.setReduceMotion(value) }
    }

    fun setHighContrast(value: Boolean) {
        viewModelScope.launch { settingsStore.setHighContrast(value) }
    }

    fun setNotifications(value: Boolean) {
        viewModelScope.launch { settingsStore.setNotifications(value) }
    }

    fun setSound(value: Boolean) {
        viewModelScope.launch { settingsStore.setSound(value) }
    }

    fun setChime(value: ChimeChoice) {
        viewModelScope.launch { settingsStore.setChime(value) }
    }

    fun setHaptics(value: Boolean) {
        viewModelScope.launch { settingsStore.setHaptics(value) }
    }

    fun setFullScreenAlerts(value: Boolean) {
        viewModelScope.launch { settingsStore.setFullScreenAlerts(value) }
    }

    fun addTask(title: String, tag: String?, estimateSessions: Int) {
        viewModelScope.launch {
            repository.addTask(title, tag, estimateSessions)
            _messages.tryEmit("Task added")
        }
    }

    fun toggleTask(task: FocusTask) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun deleteTask(task: FocusTask) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
            _messages.tryEmit("Local history deleted")
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val history = repository.allHistory()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(
                        "id,mode,stage,outcome,total_seconds,active_seconds,started_utc,ended_utc,task_id,task_title,stage_index\n"
                    )
                    history.forEach { row ->
                        writer.write(
                            listOf(
                                row.id.csv(),
                                row.mode.name.csv(),
                                (row.stage?.name ?: "").csv(),
                                row.outcome.name.csv(),
                                (row.totalDurationMs / 1000L).toString(),
                                (row.activeDurationMs / 1000L).toString(),
                                Instant.ofEpochMilli(row.startedWallMs).toString().csv(),
                                Instant.ofEpochMilli(row.endedWallMs).toString().csv(),
                                (row.taskId ?: "").csv(),
                                (row.taskTitle ?: "").csv(),
                                row.stageIndex.toString(),
                            ).joinToString(",")
                        )
                        writer.write("\n")
                    }
                }
                _messages.tryEmit("Exported ${history.size} sessions")
            }.onFailure {
                _messages.tryEmit("Export failed. No data was changed.")
            }
        }
    }

    private fun buildState(
        active: TimerSnapshot?,
        routine: RoutineConfig,
        settings: AppSettings,
        history: List<SessionHistory>,
    ): AppUiState {
        val zone = runCatching { ZoneId.of(clock.zoneId()) }.getOrDefault(ZoneId.systemDefault())
        val today = Instant.ofEpochMilli(clock.wallClockMs()).atZone(zone).toLocalDate()
        val completedWork = history.filter {
            it.mode == TimerMode.FOCUS && it.stage == FocusStage.WORK && it.outcome == SessionOutcome.COMPLETED
        }
        val todayWork = completedWork.filter { it.endedWallMs.toLocalDate(zone) == today }
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekWork = completedWork.filter {
            val date = it.endedWallMs.toLocalDate(zone)
            !date.isBefore(weekStart) && date.isBefore(weekStart.plusDays(7))
        }
        val allDates = completedWork.map { it.endedWallMs.toLocalDate(zone) }.toSet()
        var cursor = if (allDates.contains(today)) today else today.minusDays(1)
        var streak = 0
        while (allDates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        val dailyMinutes = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            (completedWork.filter { it.endedWallMs.toLocalDate(zone) == date }
                .sumOf { it.activeDurationMs } / 60_000L).toInt()
        }
        val heatmap = (27 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            (completedWork.filter { it.endedWallMs.toLocalDate(zone) == date }
                .sumOf { it.activeDurationMs } / 60_000L).toInt()
        }
        return AppUiState(
            active = active,
            remainingMs = active?.let { TimerEngine.remainingMs(it, clock) } ?: 0L,
            stopwatchMs = active?.let { TimerEngine.stopwatchElapsedMs(it, clock) } ?: 0L,
            progress = active?.let { TimerEngine.progress(it, clock) } ?: 0f,
            routine = routine,
            settings = settings,
            history = history,
            todayFocusMs = todayWork.sumOf { it.activeDurationMs },
            todaySessions = todayWork.size,
            weekFocusMs = weekWork.sumOf { it.activeDurationMs },
            weekSessions = weekWork.size,
            streakDays = streak,
            mostFocusedWindow = mostFocusedWindow(completedWork, zone),
            dailyMinutes = dailyMinutes,
            heatmapDays = heatmap,
            allTimeSessions = completedWork.size,
            nowWallMs = clock.wallClockMs(),
        )
    }

    private fun mostFocusedWindow(history: List<SessionHistory>, zone: ZoneId): String {
        if (history.isEmpty()) return "—"
        val minutesByHour = IntArray(24)
        history.forEach { session ->
            val hour = Instant.ofEpochMilli(session.endedWallMs).atZone(zone).hour
            minutesByHour[hour] += (session.activeDurationMs / 60_000L).toInt()
        }
        var bestHour = 0
        var bestSum = -1
        for (hour in 0..22) {
            val sum = minutesByHour[hour] + minutesByHour[hour + 1]
            if (sum > bestSum) {
                bestSum = sum
                bestHour = hour
            }
        }
        if (bestSum <= 0) return "—"
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        val start = java.time.LocalTime.of(bestHour, 0)
        val end = java.time.LocalTime.of((bestHour + 2) % 24, 0)
        return "${formatter.format(start)}–${formatter.format(end)}"
    }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private fun String.csv(): String = "\"" + replace("\"", "\"\"") + "\""

    class Factory(private val container: AppContainer, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                repository = container.repository,
                settingsStore = container.settings,
                clock = container.clock,
                context = context.applicationContext,
                reconcileAction = container::reconcileAndDeliver,
            ) as T
        }
    }
}
