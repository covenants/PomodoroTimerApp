package com.covenants.pomodorotimer

import android.app.Application
import android.content.Context
import com.covenants.pomodorotimer.core.domain.TimerEngine
import com.covenants.pomodorotimer.core.model.TimerStatus
import com.covenants.pomodorotimer.core.time.SystemTimeSource
import com.covenants.pomodorotimer.data.SettingsStore
import com.covenants.pomodorotimer.data.TimerRepository
import com.covenants.pomodorotimer.data.db.AppDatabase
import com.covenants.pomodorotimer.platform.AlarmScheduler
import com.covenants.pomodorotimer.platform.CompletionAlarmPlayer
import com.covenants.pomodorotimer.platform.TimerNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class PomodoroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Manual constructor injection keeps the dependency graph explicit and testable. Every dependency
 * has one owner for the process lifetime; there are no global mutable singletons.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val clock = SystemTimeSource(appContext)
    val database: AppDatabase = AppDatabase.build(appContext)
    val settings = SettingsStore(appContext)
    val scheduler = AlarmScheduler(appContext, clock)
    val notifier = TimerNotifier(appContext)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = TimerRepository(
        db = database,
        timerDao = database.timerDao(),
        taskDao = database.taskDao(),
        clock = clock,
        scheduler = scheduler,
        settings = settings,
        scope = appScope,
    )

    init {
        // Reconcile a due stage before the first frame, then keep the ongoing notification honest.
        appScope.launch { repository.reconcile() }
        appScope.launch {
            combine(
                repository.active,
                repository.routine,
                settings.settings,
                tickerFlow(),
            ) { active, routine, appSettings, _ ->
                Triple(active, routine, appSettings)
            }.collect { (active, routine, appSettings) ->
                notifier.updateActive(
                    snapshot = active,
                    routine = routine,
                    remainingMs = active?.let { TimerEngine.remainingMs(it, clock) } ?: 0L,
                    settings = appSettings,
                )
            }
        }
        appScope.launch {
            // Foreground watchdog: an inexact OS alarm may be delayed. The UI must not sit at
            // 00:00; completion still goes through the single idempotent repository command.
            while (true) {
                delay(1_000L)
                val active = repository.active.value ?: continue
                if (active.status == TimerStatus.RUNNING && TimerEngine.isDue(active, clock)) {
                    val result = repository.completeDue()
                    if (result != null) {
                        val appSettings = settings.settings.first()
                        notifier.postCompletion(result, repository.routine.value)
                        CompletionAlarmPlayer.play(appContext, appSettings)
                    }
                }
            }
        }
    }

    private fun tickerFlow() = flow {
        while (true) {
            emit(Unit)
            delay(1_000L)
        }
    }
}
