package com.covenants.pomodorotimer

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.covenants.pomodorotimer.core.domain.TimerEngine
import com.covenants.pomodorotimer.core.model.TimerMode
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    private val isForeground = MutableStateFlow(false)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isForeground.value = true
            }

            override fun onStop(owner: LifecycleOwner) {
                isForeground.value = false
            }
        })

        // Reconcile a due stage before the first frame.
        appScope.launch { reconcileAndDeliver() }

        // Update the ongoing notification only when timer state or settings change. The native
        // chronometer handles the per-second display without re-posting every second.
        appScope.launch {
            combine(
                repository.active,
                repository.routine,
                settings.settings,
            ) { active, routine, appSettings ->
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

        // Foreground watchdog only. Background completion is handled by the alarm receiver; this
        // avoids a per-second loop and a re-posted notification while the app is not visible.
        appScope.launch {
            combine(repository.active, isForeground) { active, foreground -> active to foreground }
                .collectLatest { (active, foreground) ->
                    if (!foreground ||
                        active == null ||
                        active.status != TimerStatus.RUNNING ||
                        active.mode == TimerMode.STOPWATCH
                    ) {
                        return@collectLatest
                    }
                    while (true) {
                        delay(1_000L)
                        val current = repository.active.value ?: break
                        if (!isForeground.value ||
                            current.status != TimerStatus.RUNNING ||
                            current.mode == TimerMode.STOPWATCH
                        ) {
                            break
                        }
                        if (TimerEngine.isDue(current, clock)) {
                            val result = repository.completeDue()
                            if (result != null) {
                                val appSettings = settings.settings.first()
                                val posted = notifier.postCompletion(
                                    result = result,
                                    routine = repository.routine.value,
                                    settings = appSettings,
                                )
                                CompletionAlarmPlayer.deliver(
                                    context = appContext,
                                    settings = appSettings,
                                    playSoundFallback = !posted,
                                )
                            }
                            break
                        }
                    }
                }
        }
    }

    /**
     * Reconciles persisted state and, when a stage was actually due, delivers the completion alert
     * through the same path used by the alarm receiver. Safe to call on every resume: the
     * repository completion is idempotent.
     */
    suspend fun reconcileAndDeliver() {
        val result = repository.reconcile() ?: return
        val appSettings = settings.settings.first()
        val posted = notifier.postCompletion(
            result = result,
            routine = repository.routine.value,
            settings = appSettings,
        )
        CompletionAlarmPlayer.deliver(
            context = appContext,
            settings = appSettings,
            playSoundFallback = !posted,
        )
    }
}
